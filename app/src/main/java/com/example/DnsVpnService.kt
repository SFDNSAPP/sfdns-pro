package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val channelId = "sfdns_connection_channel"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var speedJob: Job? = null
    private var drainJob: Job? = null
    private var dohProxyJob: Job? = null

    companion object {
        const val ACTION_START = "com.sfdnspro.securevpn.START"
        const val ACTION_STOP = "com.sfdnspro.securevpn.STOP"
        const val EXTRA_DNS_NAME = "dns_name"
        const val EXTRA_PRIMARY_DNS = "primary_dns"
        const val EXTRA_SECONDARY_DNS = "secondary_dns"

        @Volatile
        var isRunning = false
            private set

        @Volatile
        private var instance: DnsVpnService? = null

        fun protectSocket(socket: java.net.DatagramSocket): Boolean {
            return instance?.protect(socket) ?: true
        }

        fun protectSocket(socket: java.net.Socket): Boolean {
            return instance?.protect(socket) ?: true
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
            val dnsName = prefs.getSafeString("last_dns_name", "DNS")
            val primaryDns = prefs.getSafeString("last_primary_dns", "178.22.122.100")
            val secondaryDns = prefs.getSafeString("last_secondary_dns", "185.51.200.2")
            val primaryIpv6 = prefs.getSafeString("last_primary_dns_ipv6", "")
            val secondaryIpv6 = prefs.getSafeString("last_secondary_dns_ipv6", "")
            startVpn(dnsName, primaryDns, secondaryDns, primaryIpv6, secondaryIpv6)
            return START_STICKY
        }

        when (intent.action) {
            ACTION_START -> {
                val dnsName = intent.getStringExtra(EXTRA_DNS_NAME) ?: "DNS"
                val primaryDns = intent.getStringExtra(EXTRA_PRIMARY_DNS) ?: "8.8.8.8"
                val secondaryDns = intent.getStringExtra(EXTRA_SECONDARY_DNS) ?: "8.8.4.4"
                val primaryIpv6 = intent.getStringExtra("primary_dns_ipv6") ?: ""
                val secondaryIpv6 = intent.getStringExtra("secondary_dns_ipv6") ?: ""
                startVpn(dnsName, primaryDns, secondaryDns, primaryIpv6, secondaryIpv6)
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun isValidIp(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val clean = ip.trim()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.net.InetAddresses.isNumericAddress(clean)
            } else {
                android.util.Patterns.IP_ADDRESS.matcher(clean).matches() || (clean.contains(":") && !clean.contains(" "))
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun startVpn(dnsName: String, rawPrimaryDns: String, rawSecondaryDns: String, primaryIpv6: String = "", secondaryIpv6: String = "") {
        try {
            drainJob?.cancel()
            speedJob?.cancel()
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null

        val primaryDns = if (isValidIp(rawPrimaryDns)) rawPrimaryDns.trim() else "178.22.122.100"
        val secondaryDns = if (isValidIp(rawSecondaryDns)) rawSecondaryDns.trim() else "185.51.200.2"
        val validPrimaryIpv6 = if (isValidIp(primaryIpv6)) primaryIpv6.trim() else ""
        val validSecondaryIpv6 = if (isValidIp(secondaryIpv6)) secondaryIpv6.trim() else ""

        val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
        val isDoh = prefs.getSafeBoolean("doh", false)

        isRunning = true
        createNotificationChannel()

        // Setup early notification
        val displayDnsTitle = if (isDoh) "$dnsName (DoH)" else dnsName
        updateNotification(displayDnsTitle, primaryDns, "⬇️ 0.0 KB/s  |  ⬆️ 0.0 KB/s")

        if (isDoh) {
            startDohProxy(primaryDns)
        }

        try {
            val builder = Builder()
            builder.setSession("SFDNS")
            builder.addAddress("10.0.0.1", 24)

            // Local DNS-over-VPN Service: Allows non-DNS traffic to bypass the TUN interface
            // so game and web IP packets flow directly through the primary network adapter,
            // while DNS resolution requests are directed to custom anti-sanction/low-latency DNS resolvers.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowBypass()
            }

            val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
            val isIpv6 = prefs.getSafeBoolean("ipv6", false)
            val killSwitch = prefs.getSafeBoolean("kill_switch", false)
            val carrierOpt = prefs.getSafeString("carrier_opt", "auto")

            // Dynamic high performance MTU tuning to eliminate package fragmentation & latency
            val mtuVal = when (carrierOpt) {
                "mci" -> 1400
                "mtn" -> 1420
                "wifi" -> 1480
                else -> 1420 // Optimal general standard for low latency
            }
            builder.setMtu(mtuVal)

            // If IPv6 is enabled globally, OR the DNS has valid IPv6 addresses provided, we route IPv6 traffic
            val shouldAddIpv6Address = isIpv6 || validPrimaryIpv6.isNotEmpty() || validSecondaryIpv6.isNotEmpty() || primaryDns.contains(":") || secondaryDns.contains(":")

            if (shouldAddIpv6Address) {
                try {
                    builder.addAddress("fd00::1", 128)
                } catch (e: Exception) {
                    android.util.Log.e("DnsVpnService", "Failed to add IPv6 address fd00::1 to VPN interface", e)
                }
            }

            // Direct device DNS queries to our selected high-performance servers
            try {
                if (primaryDns.isNotEmpty()) {
                    builder.addDnsServer(primaryDns)
                }
            } catch (e: Exception) {
                android.util.Log.e("DnsVpnService", "Failed to add primary IPv4 DNS: $primaryDns", e)
            }
            try {
                if (secondaryDns.isNotEmpty()) {
                    builder.addDnsServer(secondaryDns)
                }
            } catch (e: Exception) {
                android.util.Log.e("DnsVpnService", "Failed to add secondary IPv4 DNS: $secondaryDns", e)
            }

            // If we have explicit valid IPv6 DNS, add them!
            try {
                if (validPrimaryIpv6.isNotEmpty()) {
                    builder.addDnsServer(validPrimaryIpv6)
                }
            } catch (e: Exception) {
                android.util.Log.e("DnsVpnService", "Failed to add primary IPv6 DNS: $validPrimaryIpv6", e)
            }
            try {
                if (validSecondaryIpv6.isNotEmpty()) {
                    builder.addDnsServer(validSecondaryIpv6)
                }
            } catch (e: Exception) {
                android.util.Log.e("DnsVpnService", "Failed to add secondary IPv6 DNS: $validSecondaryIpv6", e)
            }

            // Fallback default IPv6 resolvers if user has IPv6 globally enabled but using default IPv4 DNS like Cloudflare/Google
            if (isIpv6 && validPrimaryIpv6.isEmpty() && validSecondaryIpv6.isEmpty()) {
                if (primaryDns == "1.1.1.1" || primaryDns == "1.0.0.1") {
                    try { builder.addDnsServer("2606:4700:4700::1111") } catch (e: Exception) {}
                } else if (primaryDns == "8.8.8.8") {
                    try { builder.addDnsServer("2001:4860:4860::8888") } catch (e: Exception) {}
                }
            }

            if (killSwitch) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setBlocking(true)
                }
            }

            // Apply Per-App Tunneling / Split Tunneling Rules
            val splitTunnelEnabled = prefs.getSafeBoolean("split_tunnel_enabled", false)
            val splitTunnelMode = prefs.getSafeString("split_tunnel_mode", "disallowed")
            val splitTunnelAppsStr = prefs.getSafeString("split_tunnel_apps", "")

            if (splitTunnelEnabled && splitTunnelAppsStr.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val apps = splitTunnelAppsStr.split(",").filter { it.isNotEmpty() }
                if (splitTunnelMode == "allowed") {
                    for (app in apps) {
                        try {
                            builder.addAllowedApplication(app)
                        } catch (e: Exception) {
                            // Package not found or uninstalled
                        }
                    }
                } else {
                    for (app in apps) {
                        try {
                            builder.addDisallowedApplication(app)
                        } catch (e: Exception) {
                            // Package not found or uninstalled
                        }
                    }
                }
            }

            // If DoH is enabled, route DNS traffic to primary/secondary DNS through TUN interface so DoH proxy intercepts and encrypts it
            if (isDoh) {
                try {
                    if (primaryDns.isNotEmpty()) {
                        builder.addRoute(primaryDns, 32)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DnsVpnService", "Failed to add route for DoH: $primaryDns", e)
                }
                try {
                    if (secondaryDns.isNotEmpty()) {
                        builder.addRoute(secondaryDns, 32)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DnsVpnService", "Failed to add route for DoH: $secondaryDns", e)
                }
            }

            try {
                vpnInterface = builder.establish()
            } catch (e: Exception) {
                android.util.Log.e("DnsVpnService", "builder.establish() exception: ${e.message}", e)
            }

            if (vpnInterface == null) {
                android.util.Log.w("DnsVpnService", "builder.establish() returned null (VPN permission denied or system unable to establish VPN). Stopping service.")
                stopVpn()
                return
            } else {
                // Start draining / processing the TUN interface
                val fd = vpnInterface?.fileDescriptor
                if (fd != null) {
                    drainJob = serviceScope.launch(Dispatchers.IO) {
                        val inputStream = java.io.FileInputStream(fd)
                        val outputStream = java.io.FileOutputStream(fd)
                        val buffer = ByteArray(32768)
                        try {
                            while (isRunning) {
                                val read = inputStream.read(buffer)
                                if (read <= 0) {
                                    delay(15)
                                    continue
                                }

                                // If DoH is active and packet is IPv4 UDP to port 53
                                if (isDoh && read >= 28 && buffer[9].toInt() == 17) {
                                    val destPort = ((buffer[22].toInt() and 0xFF) shl 8) or (buffer[23].toInt() and 0xFF)
                                    if (destPort == 53) {
                                        val packetData = buffer.copyOf(read)
                                        serviceScope.launch(Dispatchers.IO) {
                                            processDohPacket(packetData, outputStream, primaryDns)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore stream/socket closed exceptions
                        }
                    }
                }
            }

            // Broadcast status to UI (restricted to our own app package)
            val statusIntent = Intent("$packageName.VPN_STATUS").apply {
                putExtra("status", "connected")
                setPackage(packageName)
            }
            sendBroadcast(statusIntent)

            // Refresh Quick Settings Tile & Home Widgets state
            try {
                DnsWidgetHelper.updateAllWidgets(this@DnsVpnService)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    TileService.requestListeningState(
                        this@DnsVpnService,
                        ComponentName(this@DnsVpnService, DnsTileService::class.java)
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }

            // Launch periodic background speed monitoring safely
            try {
                startSpeedMonitor(dnsName, primaryDns)
            } catch (e: Exception) {
                android.util.Log.e("DnsVpnService", "Failed to start speed monitor", e)
            }

        } catch (e: Exception) {
            android.util.Log.e("DnsVpnService", "startVpn exception: ${e.message}", e)
            stopVpn()
        }
    }

    private fun startSpeedMonitor(dnsName: String, primaryDns: String) {
        speedJob?.cancel()
        speedJob = serviceScope.launch {
            val uid = android.os.Process.myUid()
            fun getRx(): Long {
                val r = TrafficStats.getUidRxBytes(uid)
                return if (r != TrafficStats.UNSUPPORTED.toLong()) r else TrafficStats.getTotalRxBytes()
            }
            fun getTx(): Long {
                val t = TrafficStats.getUidTxBytes(uid)
                return if (t != TrafficStats.UNSUPPORTED.toLong()) t else TrafficStats.getTotalTxBytes()
            }

            var lastRx = getRx()
            var lastTx = getTx()
            var lastTime = System.currentTimeMillis()

            while (isRunning) {
                delay(2000)
                val currentRx = getRx()
                val currentTx = getTx()
                val currentTime = System.currentTimeMillis()

                val timeDiff = (currentTime - lastTime) / 1000.0
                if (timeDiff > 0 && lastRx > 0 && lastTx > 0) {
                    val rxDiff = (currentRx - lastRx).coerceAtLeast(0)
                    val txDiff = (currentTx - lastTx).coerceAtLeast(0)

                    val rxSpeed = rxDiff / timeDiff
                    val txSpeed = txDiff / timeDiff

                    val downText = formatSpeed(rxSpeed)
                    val upText = formatSpeed(txSpeed)

                    updateNotification(dnsName, primaryDns, "⬇️ $downText | ⬆️ $upText")
                } else {
                    updateNotification(dnsName, primaryDns, "⬇️ 0.0 KB/s | ⬆️ 0.0 KB/s")
                }

                lastRx = currentRx
                lastTx = currentTx
                lastTime = currentTime
            }
        }
    }

    private fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec > 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec > 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024)
            else -> String.format("%.0f B/s", bytesPerSec)
        }
    }

    private fun updateNotification(dnsName: String, primaryDns: String, speedInfo: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, DnsVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Color highlight for a high-tech neon green look: #FF10B981
        val accentColor = 0xFF10B981.toInt()

        val titleText = "⚡ SFDNS Pro - تحریم‌شکن فعال"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(titleText)
            .setContentText("سرور: $dnsName  |  $speedInfo")
            .setSmallIcon(R.drawable.ic_lightning)
            .setColor(accentColor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Prevent annoying notification alerts/vibrations on speed update
            .setShowWhen(true)
            .setUsesChronometer(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "🔴 قطع اتصال (Disconnect)",
                stopPendingIntent
            )
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (e: Exception) {
                    startForeground(1, notification)
                }
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(1, notification)
        }
    }

    private fun processDohPacket(packet: ByteArray, outputStream: java.io.FileOutputStream, primaryDns: String) {
        try {
            val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
            val udpHeaderLen = 8
            val dnsOffset = ipHeaderLen + udpHeaderLen
            if (packet.size < dnsOffset) return

            val dnsQuery = packet.copyOfRange(dnsOffset, packet.size)

            val dohUrlStr = when {
                primaryDns == "1.1.1.1" || primaryDns == "1.0.0.1" -> "https://1.1.1.1/dns-query"
                primaryDns == "8.8.8.8" || primaryDns == "8.8.4.4" -> "https://dns.google/dns-query"
                primaryDns == "9.9.9.9" -> "https://dns.quad9.net/dns-query"
                primaryDns == "178.22.122.100" || primaryDns == "185.51.200.2" -> "https://free.shecan.ir/dns-query"
                primaryDns == "78.157.42.100" || primaryDns == "78.157.42.101" -> "https://dns.electro.ir/dns-query"
                primaryDns == "10.201.201.201" || primaryDns == "10.201.201.202" -> "https://dns.radar.game/dns-query"
                primaryDns == "10.202.10.202" || primaryDns == "10.202.10.102" -> "https://dns.403.online/dns-query"
                primaryDns == "185.55.226.26" || primaryDns == "185.55.225.25" -> "https://dns.begzar.ir/dns-query"
                else -> "https://1.1.1.1/dns-query"
            }

            val url = java.net.URL(dohUrlStr)
            val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/dns-message")
            conn.setRequestProperty("Accept", "application/dns-message")
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            conn.doOutput = true
            conn.doInput = true

            conn.outputStream.use { os ->
                os.write(dnsQuery)
                os.flush()
            }

            if (conn.responseCode == 200) {
                val dnsResponse = conn.inputStream.use { it.readBytes() }

                val totalLen = 20 + 8 + dnsResponse.size
                val reply = ByteArray(totalLen)

                // IPv4 Header
                reply[0] = 0x45.toByte() // Version 4, IHL 5
                reply[1] = 0x00.toByte()
                reply[2] = ((totalLen shr 8) and 0xFF).toByte()
                reply[3] = (totalLen and 0xFF).toByte()
                reply[4] = 0x12.toByte()
                reply[5] = 0x34.toByte()
                reply[6] = 0x40.toByte() // Don't Fragment
                reply[7] = 0x00.toByte()
                reply[8] = 64.toByte() // TTL
                reply[9] = 17.toByte() // UDP Protocol

                // Swap IPs: Src IP = original Dst IP (bytes 16..19), Dst IP = original Src IP (bytes 12..15)
                System.arraycopy(packet, 16, reply, 12, 4)
                System.arraycopy(packet, 12, reply, 16, 4)

                // Calculate IP Header Checksum
                val checksum = computeIpChecksum(reply, 20)
                reply[10] = ((checksum shr 8) and 0xFF).toByte()
                reply[11] = (checksum and 0xFF).toByte()

                // UDP Header: Swap Ports: Src Port = original Dst Port (bytes 22..23), Dst Port = original Src Port (bytes 20..21)
                reply[20] = packet[22]
                reply[21] = packet[23]
                reply[22] = packet[20]
                reply[23] = packet[21]

                val udpLen = 8 + dnsResponse.size
                reply[24] = ((udpLen shr 8) and 0xFF).toByte()
                reply[25] = (udpLen and 0xFF).toByte()
                reply[26] = 0x00.toByte() // Checksum optional in IPv4
                reply[27] = 0x00.toByte()

                // Payload
                System.arraycopy(dnsResponse, 0, reply, 28, dnsResponse.size)

                synchronized(outputStream) {
                    outputStream.write(reply)
                    outputStream.flush()
                }
            }
        } catch (e: Exception) {
            // Ignore transient DoH packet errors
        }
    }

    private fun computeIpChecksum(header: ByteArray, length: Int): Int {
        var sum = 0
        var i = 0
        while (i < length) {
            if (i != 10) { // skip checksum field itself
                val word = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
                sum += word
            }
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun startDohProxy(primaryDns: String) {
        dohProxyJob?.cancel()
        dohProxyJob = serviceScope.launch(Dispatchers.IO) {
            val dohUrlStr = when {
                primaryDns == "1.1.1.1" || primaryDns == "1.0.0.1" -> "https://1.1.1.1/dns-query"
                primaryDns == "8.8.8.8" || primaryDns == "8.8.4.4" -> "https://dns.google/dns-query"
                primaryDns == "9.9.9.9" -> "https://dns.quad9.net/dns-query"
                primaryDns == "178.22.122.100" || primaryDns == "185.51.200.2" -> "https://free.shecan.ir/dns-query"
                primaryDns == "78.157.42.100" || primaryDns == "78.157.42.101" -> "https://dns.electro.ir/dns-query"
                primaryDns == "10.201.201.201" || primaryDns == "10.201.201.202" -> "https://dns.radar.game/dns-query"
                primaryDns == "10.202.10.202" || primaryDns == "10.202.10.102" -> "https://dns.403.online/dns-query"
                primaryDns == "185.55.226.26" || primaryDns == "185.55.225.25" -> "https://dns.begzar.ir/dns-query"
                else -> "https://1.1.1.1/dns-query"
            }

            var serverSocket: java.net.DatagramSocket? = null
            try {
                serverSocket = java.net.DatagramSocket(0, java.net.InetAddress.getByName("127.0.0.1"))
                serverSocket.soTimeout = 2000
                val receiveBuffer = ByteArray(4096)

                while (isRunning) {
                    try {
                        val packet = java.net.DatagramPacket(receiveBuffer, receiveBuffer.size)
                        serverSocket.receive(packet)
                        val queryBytes = packet.data.copyOf(packet.length)
                        val clientAddress = packet.address
                        val clientPort = packet.port

                        serviceScope.launch(Dispatchers.IO) {
                            try {
                                val url = java.net.URL(dohUrlStr)
                                val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                                conn.requestMethod = "POST"
                                conn.setRequestProperty("Content-Type", "application/dns-message")
                                conn.setRequestProperty("Accept", "application/dns-message")
                                conn.connectTimeout = 1500
                                conn.readTimeout = 1500
                                conn.doOutput = true
                                conn.doInput = true

                                conn.outputStream.use { os ->
                                    os.write(queryBytes)
                                    os.flush()
                                }

                                if (conn.responseCode == 200) {
                                    val responseBytes = conn.inputStream.use { it.readBytes() }
                                    val replyPacket = java.net.DatagramPacket(responseBytes, responseBytes.size, clientAddress, clientPort)
                                    serverSocket?.send(replyPacket)
                                }
                            } catch (e: Exception) {
                                // Ignore transient DoH packet errors
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Timeout
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DnsVpnService", "startDohProxy error", e)
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    private fun stopVpn() {
        isRunning = false
        speedJob?.cancel()
        speedJob = null
        drainJob?.cancel()
        drainJob = null
        dohProxyJob?.cancel()
        dohProxyJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()

        val statusIntent = Intent("$packageName.VPN_STATUS").apply {
            putExtra("status", "disconnected")
            setPackage(packageName)
        }
        sendBroadcast(statusIntent)

        // Refresh Quick Settings Tile & Home Widgets state
        try {
            DnsWidgetHelper.updateAllWidgets(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                TileService.requestListeningState(
                    this,
                    ComponentName(this, DnsTileService::class.java)
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        stopVpn()
        serviceJob.cancelChildren()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SFDNS Connection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
