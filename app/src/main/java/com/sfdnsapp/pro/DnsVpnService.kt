package com.sfdnsapp.pro

/**
 * Core VPN service implementing high-performance local DNS tunneling and DoH proxy.
 * Fixed bugs:
 * 1) Migrated all SharedPreferences to unified PrefKeys.
 * 2) Protected outgoing DoH sockets via ProtectedSocketFactory and DnsVpnService.protectSocket.
 * 3) Expanded resolveDohEndpointUrl to support both primary and secondary DNS, and fallback to plain DNS on 4xx/5xx errors.
 * 4) Hardened DoH packet parsing for IPv4 (fragments ignored) and IPv6 (extension headers traversal, IPv6 reply synthesis, mandatory UDP checksum).
 * 5) Safe stop handling and queries per second metric monitoring.
 */
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val channelId = "sfdns_connection_channel"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var speedJob: Job? = null
    private var drainJob: Job? = null
    private var dohProxyJob: Job? = null

    companion object {
        private const val TAG = "DnsVpnService"

        const val ACTION_START = "com.sfdnsapp.pro.securevpn.START"
        const val ACTION_STOP = "com.sfdnsapp.pro.securevpn.STOP"
        const val EXTRA_DNS_NAME = "dns_name"
        const val EXTRA_PRIMARY_DNS = "primary_dns"
        const val EXTRA_SECONDARY_DNS = "secondary_dns"

        @Volatile
        var isRunning = false
            private set

        @Volatile
        private var instance: DnsVpnService? = null

        val dohQueryCounter = AtomicLong(0)

        fun protectSocket(socket: DatagramSocket): Boolean {
            return instance?.protect(socket) ?: true
        }

        fun protectSocket(socket: Socket): Boolean {
            return instance?.protect(socket) ?: true
        }

        /**
         * Safe stop request usable from Activities, Tile, or Widgets even when background service starting is restricted.
         */
        fun requestStop() {
            instance?.stopVpn()
        }

        /**
         * Resolves the corresponding DoH endpoint URL for a given DNS server IP.
         * Supports both primary and secondary IP mappings.
         */
        fun resolveDohEndpointUrl(dnsIp: String): String {
            return when (dnsIp.trim()) {
                "1.1.1.1", "1.0.0.1" -> "https://1.1.1.1/dns-query"
                "8.8.8.8", "8.8.4.4" -> "https://dns.google/dns-query"
                "9.9.9.9", "149.112.112.112" -> "https://dns.quad9.net/dns-query"
                "178.22.122.100", "185.51.200.2" -> "https://free.shecan.ir/dns-query"
                "78.157.42.100", "78.157.42.101" -> "https://dns.electro.ir/dns-query"
                "10.201.201.201", "10.201.201.202" -> "https://dns.radar.game/dns-query"
                "10.202.10.202", "10.202.10.102" -> "https://dns.403.online/dns-query"
                "185.55.226.26", "185.55.225.25" -> "https://dns.begzar.ir/dns-query"
                "94.140.14.14", "94.140.15.15" -> "https://dns.adguard.com/dns-query"
                else -> "https://${dnsIp.trim()}/dns-query"
            }
        }
    }

    /**
     * Custom SSLSocketFactory that protects underlying TLS sockets with VpnService.protect()
     * preventing routing loops or traffic blockage under Kill Switch.
     */
    private class ProtectedSslSocketFactory : SSLSocketFactory() {
        private val defaultFactory: SSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()

        override fun getDefaultCipherSuites(): Array<String> = defaultFactory.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = defaultFactory.supportedCipherSuites

        private fun <T : Socket> protect(s: T): T {
            protectSocket(s)
            return s
        }

        override fun createSocket(): Socket {
            val s = Socket()
            protect(s)
            return s
        }

        override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
            protect(s)
            return defaultFactory.createSocket(s, host, port, autoClose)
        }

        override fun createSocket(host: String, port: Int): Socket {
            val s = Socket()
            protect(s)
            s.connect(InetSocketAddress(host, port), 1800)
            return defaultFactory.createSocket(s, host, port, true)
        }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
            val s = Socket()
            protect(s)
            s.bind(InetSocketAddress(localHost, localPort))
            s.connect(InetSocketAddress(host, port), 1800)
            return defaultFactory.createSocket(s, host, port, true)
        }

        override fun createSocket(host: InetAddress, port: Int): Socket {
            val s = Socket()
            protect(s)
            s.connect(InetSocketAddress(host, port), 1800)
            return defaultFactory.createSocket(s, host.hostAddress, port, true)
        }

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
            val s = Socket()
            protect(s)
            s.bind(InetSocketAddress(localAddress, localPort))
            s.connect(InetSocketAddress(address, port), 1800)
            return defaultFactory.createSocket(s, address.hostAddress, port, true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            val prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
            val dnsName = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "DNS")
            val primaryDns = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS, "178.22.122.100")
            val secondaryDns = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS, "185.51.200.2")
            val primaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6, "")
            val secondaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6, "")
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
                @Suppress("DEPRECATION")
                android.util.Patterns.IP_ADDRESS.matcher(clean).matches() || (clean.contains(":") && !clean.contains(" "))
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun startVpn(
        dnsName: String,
        rawPrimaryDns: String,
        rawSecondaryDns: String,
        primaryIpv6: String = "",
        secondaryIpv6: String = ""
    ) {
        cleanupVpnResources()

        val primaryDns = if (isValidIp(rawPrimaryDns)) rawPrimaryDns.trim() else "178.22.122.100"
        val secondaryDns = if (isValidIp(rawSecondaryDns)) rawSecondaryDns.trim() else "185.51.200.2"
        val validPrimaryIpv6 = if (isValidIp(primaryIpv6)) primaryIpv6.trim() else ""
        val validSecondaryIpv6 = if (isValidIp(secondaryIpv6)) secondaryIpv6.trim() else ""

        val prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
        val isDoh = prefs.getSafeBoolean(PrefKeys.KEY_DOH_ENABLED, false)

        isRunning = true
        createNotificationChannel()

        val displayDnsTitle = if (isDoh) "$dnsName (DoH)" else dnsName
        updateNotification(displayDnsTitle, primaryDns, "⚡ 0 Query/s")

        if (isDoh) {
            startDohProxy(primaryDns)
        }

        try {
            val builder = Builder()
            builder.setSession("SFDNS Pro")
            builder.addAddress("10.0.0.1", 24)

            // Local DNS-over-VPN Service: Allows non-DNS IP traffic to bypass the TUN interface
            // so latency and bandwidth are unaffected for apps & gaming.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowBypass()
            }

            val isIpv6 = prefs.getSafeBoolean(PrefKeys.KEY_IPV6_ENABLED, false)
            val killSwitch = prefs.getSafeBoolean(PrefKeys.KEY_KILL_SWITCH, false)
            val carrierOpt = prefs.getSafeString(PrefKeys.KEY_CARRIER_OPT, "auto")

            // Dynamic high performance MTU tuning
            val mtuVal = when (carrierOpt) {
                "mci" -> 1400
                "mtn" -> 1420
                "wifi" -> 1480
                else -> 1420
            }
            builder.setMtu(mtuVal)

            // Route IPv6 if globally enabled or explicitly specified
            val shouldAddIpv6Address = isIpv6 || validPrimaryIpv6.isNotEmpty() || validSecondaryIpv6.isNotEmpty() || primaryDns.contains(":") || secondaryDns.contains(":")

            if (shouldAddIpv6Address) {
                try {
                    builder.addAddress("fd00::1", 128)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add IPv6 interface address", e)
                }
            }

            // Primary and Secondary IPv4 DNS resolvers
            try {
                if (primaryDns.isNotEmpty()) builder.addDnsServer(primaryDns)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add primary IPv4 DNS: $primaryDns", e)
            }
            try {
                if (secondaryDns.isNotEmpty()) builder.addDnsServer(secondaryDns)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add secondary IPv4 DNS: $secondaryDns", e)
            }

            // Custom IPv6 DNS resolvers
            try {
                if (validPrimaryIpv6.isNotEmpty()) builder.addDnsServer(validPrimaryIpv6)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add primary IPv6 DNS: $validPrimaryIpv6", e)
            }
            try {
                if (validSecondaryIpv6.isNotEmpty()) builder.addDnsServer(validSecondaryIpv6)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add secondary IPv6 DNS: $validSecondaryIpv6", e)
            }

            // Fallback IPv6 resolvers for Cloudflare/Google if IPv6 is enabled globally
            if (isIpv6 && validPrimaryIpv6.isEmpty() && validSecondaryIpv6.isEmpty()) {
                if (primaryDns == "1.1.1.1" || primaryDns == "1.0.0.1") {
                    try { builder.addDnsServer("2606:4700:4700::1111") } catch (_: Exception) {}
                } else if (primaryDns == "8.8.8.8") {
                    try { builder.addDnsServer("2001:4860:4860::8888") } catch (_: Exception) {}
                }
            }

            // Enable Kill Switch blocking if configured
            if (killSwitch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setBlocking(true)
            }

            // Apply Per-App Split Tunneling Rules
            val splitTunnelEnabled = prefs.getSafeBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, false)
            val splitTunnelMode = prefs.getSafeString(PrefKeys.KEY_SPLIT_TUNNEL_MODE, "disallowed")
            val splitTunnelAppsStr = prefs.getSafeString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, "")

            if (splitTunnelEnabled && splitTunnelAppsStr.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val apps = splitTunnelAppsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (splitTunnelMode == "allowed") {
                    for (app in apps) {
                        try { builder.addAllowedApplication(app) } catch (_: Exception) {}
                    }
                } else {
                    for (app in apps) {
                        try { builder.addDisallowedApplication(app) } catch (_: Exception) {}
                    }
                }
            }

            // Route DNS IPs into TUN interface for DoH intercept when DoH is active
            if (isDoh) {
                try {
                    if (primaryDns.isNotEmpty()) builder.addRoute(primaryDns, 32)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add route for DoH: $primaryDns", e)
                }
                try {
                    if (secondaryDns.isNotEmpty()) builder.addRoute(secondaryDns, 32)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add route for DoH: $secondaryDns", e)
                }
            }

            try {
                vpnInterface = builder.establish()
            } catch (e: Exception) {
                Log.e(TAG, "builder.establish() failed: ${e.message}", e)
            }

            if (vpnInterface == null) {
                Log.w(TAG, "builder.establish() returned null. Stopping service.")
                stopVpn()
                return
            } else {
                val fd = vpnInterface?.fileDescriptor
                if (fd != null) {
                    drainJob = serviceScope.launch(Dispatchers.IO) {
                        try {
                            FileInputStream(fd).use { inputStream ->
                                FileOutputStream(fd).use { outputStream ->
                                    val buffer = ByteArray(32768)
                                    while (isRunning) {
                                        val read = inputStream.read(buffer)
                                        if (read <= 0) {
                                            delay(15)
                                            continue
                                        }

                                        if (isDoh && read >= 20) {
                                            val packetData = buffer.copyOf(read)
                                            serviceScope.launch(Dispatchers.IO) {
                                                processDohPacket(packetData, outputStream, primaryDns, secondaryDns)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Stream or file descriptor closed on VPN shutdown
                        }
                    }
                }
            }

            notifyVpnStatusChanged("connected")
            startSpeedMonitor(dnsName, primaryDns)

        } catch (e: Exception) {
            Log.e(TAG, "startVpn exception: ${e.message}", e)
            stopVpn()
        }
    }

    private fun cleanupVpnResources() {
        try {
            drainJob?.cancel()
            speedJob?.cancel()
            dohProxyJob?.cancel()
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun startSpeedMonitor(dnsName: String, primaryDns: String) {
        speedJob?.cancel()
        speedJob = serviceScope.launch {
            var lastQueries = dohQueryCounter.get()
            var lastTime = System.currentTimeMillis()

            while (isRunning) {
                delay(2000)
                val currentQueries = dohQueryCounter.get()
                val currentTime = System.currentTimeMillis()
                val timeDiff = (currentTime - lastTime) / 1000.0

                val qDiff = (currentQueries - lastQueries).coerceAtLeast(0)
                val qps = if (timeDiff > 0) (qDiff / timeDiff).toLong() else 0L

                val queryText = "$qps Query/s"
                updateNotification(dnsName, primaryDns, "⚡ $queryText")

                lastQueries = currentQueries
                lastTime = currentTime
            }
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

        val accentColor = 0xFF10B981.toInt()
        val titleText = "⚡ SFDNS Pro - تحریم‌شکن فعال"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(titleText)
            .setContentText("سرور: $dnsName  |  $speedInfo")
            .setSmallIcon(R.drawable.ic_lightning)
            .setColor(accentColor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
                } catch (_: Exception) {
                    startForeground(1, notification)
                }
            } else {
                startForeground(1, notification)
            }
        } catch (_: Exception) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(1, notification)
        }
    }

    /**
     * Fallback to plain DNS query if DoH endpoint returns 4xx/5xx errors or network fails.
     */
    private fun queryPlainDnsFallback(dnsQuery: ByteArray, dnsIp: String): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            protectSocket(socket)
            socket.soTimeout = 1500
            val targetAddr = InetAddress.getByName(dnsIp)
            val sendPacket = DatagramPacket(dnsQuery, dnsQuery.size, targetAddr, 53)
            socket.send(sendPacket)

            val recvBuf = ByteArray(4096)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
            recvPacket.data.copyOf(recvPacket.length)
        } catch (e: Exception) {
            Log.w(TAG, "Plain DNS fallback to $dnsIp failed: ${e.message}")
            null
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Executes DoH query with custom protected SSLSocketFactory and plain DNS fallback.
     */
    private fun executeDohQuery(dnsQuery: ByteArray, primaryDns: String, secondaryDns: String): ByteArray? {
        val dohUrlStr = resolveDohEndpointUrl(primaryDns)
        try {
            val url = URL(dohUrlStr)
            val conn = url.openConnection() as HttpsURLConnection
            conn.sslSocketFactory = ProtectedSslSocketFactory()
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

            val code = conn.responseCode
            if (code == 200) {
                return conn.inputStream.use { it.readBytes() }
            } else {
                Log.w(TAG, "DoH query returned non-200 code: $code. Falling back to plain DNS.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH request failed: ${e.message}. Falling back to plain DNS.")
        }

        // Fallback to plain DNS on primary or secondary
        val fallbackPrimary = queryPlainDnsFallback(dnsQuery, primaryDns)
        if (fallbackPrimary != null) return fallbackPrimary
        if (secondaryDns.isNotBlank() && secondaryDns != primaryDns) {
            return queryPlainDnsFallback(dnsQuery, secondaryDns)
        }
        return null
    }

    /**
     * Hardened DoH packet processor handling IPv4 and IPv6 without fragile hardcoded offsets.
     */
    private fun processDohPacket(
        packet: ByteArray,
        outputStream: FileOutputStream,
        primaryDns: String,
        secondaryDns: String
    ) {
        try {
            if (packet.size < 20) return
            val version = (packet[0].toInt() shr 4) and 0x0F

            if (version == 4) {
                val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
                if (ipHeaderLen < 20 || packet.size < ipHeaderLen + 8) return

                // Check for fragmentation: flags & fragment offset
                val fragFlagsOffset = ((packet[6].toInt() and 0xFF) shl 8) or (packet[7].toInt() and 0xFF)
                val mf = (fragFlagsOffset and 0x2000) != 0
                val fragOffset = fragFlagsOffset and 0x1FFF
                if (mf || fragOffset != 0) {
                    // Ignore fragmented packets
                    return
                }

                val protocol = packet[9].toInt() and 0xFF
                if (protocol != 17) return // Not UDP

                val srcPort = ((packet[ipHeaderLen].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 1].toInt() and 0xFF)
                val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 3].toInt() and 0xFF)
                if (dstPort != 53) return // Not DNS query

                val udpLength = ((packet[ipHeaderLen + 4].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 5].toInt() and 0xFF)
                val dnsOffset = ipHeaderLen + 8
                val dnsLen = minOf(udpLength - 8, packet.size - dnsOffset)
                if (dnsLen < 12) return // Invalid DNS message

                // Verify QR bit (bit 7 of byte 2 in DNS header). 0 = Query, 1 = Response.
                val qr = (packet[dnsOffset + 2].toInt() and 0x80) != 0
                if (qr) return // Ignore responses

                val dnsQuery = packet.copyOfRange(dnsOffset, dnsOffset + dnsLen)
                dohQueryCounter.incrementAndGet()

                val dnsResponse = executeDohQuery(dnsQuery, primaryDns, secondaryDns) ?: return

                // Synthesize IPv4 UDP Response packet
                val totalLen = 20 + 8 + dnsResponse.size
                val reply = ByteArray(totalLen)

                // IPv4 Header
                reply[0] = 0x45.toByte() // Version 4, IHL 5
                reply[1] = 0x00.toByte()
                reply[2] = ((totalLen shr 8) and 0xFF).toByte()
                reply[3] = (totalLen and 0xFF).toByte()
                reply[4] = 0x12.toByte()
                reply[5] = 0x34.toByte()
                reply[6] = 0x40.toByte() // DF flag
                reply[7] = 0x00.toByte()
                reply[8] = 64.toByte()   // TTL
                reply[9] = 17.toByte()   // Protocol UDP

                // Swap Source and Destination IPv4 addresses
                System.arraycopy(packet, 16, reply, 12, 4) // Src = Original Dst
                System.arraycopy(packet, 12, reply, 16, 4) // Dst = Original Src

                val ipChecksum = computeIpChecksum(reply, 20)
                reply[10] = ((ipChecksum shr 8) and 0xFF).toByte()
                reply[11] = (ipChecksum and 0xFF).toByte()

                // UDP Header: Swap Ports
                reply[20] = ((dstPort shr 8) and 0xFF).toByte() // Src Port = 53
                reply[21] = (dstPort and 0xFF).toByte()
                reply[22] = ((srcPort shr 8) and 0xFF).toByte() // Dst Port = original Src
                reply[23] = (srcPort and 0xFF).toByte()

                val udpLen = 8 + dnsResponse.size
                reply[24] = ((udpLen shr 8) and 0xFF).toByte()
                reply[25] = (udpLen and 0xFF).toByte()
                reply[26] = 0x00.toByte()
                reply[27] = 0x00.toByte()

                // DNS Payload
                System.arraycopy(dnsResponse, 0, reply, 28, dnsResponse.size)

                // Compute UDP Checksum
                val udpChecksum = computeUdpChecksumIpv4(
                    srcIp = reply.copyOfRange(12, 16),
                    dstIp = reply.copyOfRange(16, 20),
                    udpPacket = reply.copyOfRange(20, totalLen)
                )
                reply[26] = ((udpChecksum shr 8) and 0xFF).toByte()
                reply[27] = (udpChecksum and 0xFF).toByte()

                synchronized(outputStream) {
                    outputStream.write(reply)
                    outputStream.flush()
                }

            } else if (version == 6) {
                if (packet.size < 48) return

                var nextHeader = packet[6].toInt() and 0xFF
                var offset = 40

                // Traverse IPv6 extension headers to locate UDP payload
                while (offset < packet.size) {
                    if (nextHeader == 17) {
                        break // Reached UDP header
                    }
                    if (nextHeader == 0 || nextHeader == 43 || nextHeader == 60) {
                        // Hop-by-Hop (0), Routing (43), Destination Options (60)
                        if (offset + 2 > packet.size) return
                        val extLen = ((packet[offset + 1].toInt() and 0xFF) + 1) * 8
                        nextHeader = packet[offset].toInt() and 0xFF
                        offset += extLen
                    } else if (nextHeader == 44) {
                        // Fragment Header (44)
                        if (offset + 8 > packet.size) return
                        val fragOffset = ((packet[offset + 2].toInt() and 0xFF) shl 5) or ((packet[offset + 3].toInt() and 0xF8) shr 3)
                        val mFlag = (packet[offset + 3].toInt() and 0x01) != 0
                        if (mFlag || fragOffset != 0) {
                            // Ignore fragmented packet
                            return
                        }
                        nextHeader = packet[offset].toInt() and 0xFF
                        offset += 8
                    } else {
                        // Unsupported protocol/header
                        return
                    }
                }

                if (nextHeader != 17 || packet.size < offset + 8) return

                val srcPort = ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)
                val dstPort = ((packet[offset + 2].toInt() and 0xFF) shl 8) or (packet[offset + 3].toInt() and 0xFF)
                if (dstPort != 53) return

                val udpLength = ((packet[offset + 4].toInt() and 0xFF) shl 8) or (packet[offset + 5].toInt() and 0xFF)
                val dnsOffset = offset + 8
                val dnsLen = minOf(udpLength - 8, packet.size - dnsOffset)
                if (dnsLen < 12) return

                val qr = (packet[dnsOffset + 2].toInt() and 0x80) != 0
                if (qr) return

                val dnsQuery = packet.copyOfRange(dnsOffset, dnsOffset + dnsLen)
                dohQueryCounter.incrementAndGet()

                val dnsResponse = executeDohQuery(dnsQuery, primaryDns, secondaryDns) ?: return

                // Synthesize IPv6 UDP Response packet
                val payloadLen = 8 + dnsResponse.size
                val totalLen = 40 + payloadLen
                val reply = ByteArray(totalLen)

                // IPv6 Header (40 bytes)
                reply[0] = 0x60.toByte() // Version 6, Traffic Class 0
                reply[1] = 0x00.toByte()
                reply[2] = 0x00.toByte()
                reply[3] = 0x00.toByte()
                reply[4] = ((payloadLen shr 8) and 0xFF).toByte()
                reply[5] = (payloadLen and 0xFF).toByte()
                reply[6] = 17.toByte()   // Next Header = UDP
                reply[7] = 64.toByte()   // Hop Limit

                // Swap IPv6 Src and Dst (16 bytes each)
                System.arraycopy(packet, 24, reply, 8, 16)  // Src = Original Dst
                System.arraycopy(packet, 8, reply, 24, 16)  // Dst = Original Src

                // UDP Header (8 bytes)
                reply[40] = ((dstPort shr 8) and 0xFF).toByte()
                reply[41] = (dstPort and 0xFF).toByte()
                reply[42] = ((srcPort shr 8) and 0xFF).toByte()
                reply[43] = (srcPort and 0xFF).toByte()
                reply[44] = ((payloadLen shr 8) and 0xFF).toByte()
                reply[45] = (payloadLen and 0xFF).toByte()
                reply[46] = 0x00.toByte()
                reply[47] = 0x00.toByte()

                // DNS Payload
                System.arraycopy(dnsResponse, 0, reply, 48, dnsResponse.size)

                // Compute mandatory IPv6 UDP Checksum
                val udpChecksum = computeUdpChecksumIpv6(
                    srcIp = reply.copyOfRange(8, 24),
                    dstIp = reply.copyOfRange(24, 40),
                    udpPacket = reply.copyOfRange(40, totalLen)
                )
                reply[46] = ((udpChecksum shr 8) and 0xFF).toByte()
                reply[47] = (udpChecksum and 0xFF).toByte()

                synchronized(outputStream) {
                    outputStream.write(reply)
                    outputStream.flush()
                }
            }
        } catch (_: Exception) {
            // Ignore transient errors
        }
    }

    private fun computeIpChecksum(header: ByteArray, length: Int): Int {
        var sum = 0
        var i = 0
        while (i < length) {
            if (i != 10) {
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

    private fun computeUdpChecksumIpv4(srcIp: ByteArray, dstIp: ByteArray, udpPacket: ByteArray): Int {
        var sum = 0

        // Pseudo-header: Src IP (4 bytes), Dst IP (4 bytes), 0x00, Protocol (17), UDP Length (2 bytes)
        for (i in 0 until 4 step 2) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
        }
        sum += 17 // Protocol UDP
        sum += udpPacket.size

        // UDP packet bytes (with checksum field set to 0)
        var i = 0
        while (i < udpPacket.size - 1) {
            if (i != 6) { // Skip checksum at bytes 6-7
                sum += ((udpPacket[i].toInt() and 0xFF) shl 8) or (udpPacket[i + 1].toInt() and 0xFF)
            }
            i += 2
        }
        if (udpPacket.size % 2 != 0) {
            sum += (udpPacket[udpPacket.size - 1].toInt() and 0xFF) shl 8
        }

        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        return if (checksum == 0) 0xFFFF else checksum
    }

    private fun computeUdpChecksumIpv6(srcIp: ByteArray, dstIp: ByteArray, udpPacket: ByteArray): Int {
        var sum = 0

        // Pseudo-header: Src IP (16 bytes), Dst IP (16 bytes), UDP Length (32-bit), Next Header 17 (32-bit)
        for (i in 0 until 16 step 2) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
        }
        val udpLen = udpPacket.size
        sum += (udpLen shr 16) and 0xFFFF
        sum += udpLen and 0xFFFF
        sum += 17

        var i = 0
        while (i < udpPacket.size - 1) {
            if (i != 6) {
                sum += ((udpPacket[i].toInt() and 0xFF) shl 8) or (udpPacket[i + 1].toInt() and 0xFF)
            }
            i += 2
        }
        if (udpPacket.size % 2 != 0) {
            sum += (udpPacket[udpPacket.size - 1].toInt() and 0xFF) shl 8
        }

        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        return if (checksum == 0) 0xFFFF else checksum
    }

    private fun startDohProxy(primaryDns: String) {
        dohProxyJob?.cancel()
        dohProxyJob = serviceScope.launch(Dispatchers.IO) {
            val dohUrlStr = resolveDohEndpointUrl(primaryDns)

            var serverSocket: DatagramSocket? = null
            try {
                serverSocket = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
                protectSocket(serverSocket)
                serverSocket.soTimeout = 2000
                val receiveBuffer = ByteArray(4096)

                while (isRunning) {
                    try {
                        val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        serverSocket.receive(packet)
                        val queryBytes = packet.data.copyOf(packet.length)
                        val clientAddress = packet.address
                        val clientPort = packet.port

                        serviceScope.launch(Dispatchers.IO) {
                            try {
                                val url = URL(dohUrlStr)
                                val conn = url.openConnection() as HttpsURLConnection
                                conn.sslSocketFactory = ProtectedSslSocketFactory()
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
                                    val replyPacket = DatagramPacket(responseBytes, responseBytes.size, clientAddress, clientPort)
                                    serverSocket?.send(replyPacket)
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // Loop timeout
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "startDohProxy socket exception", e)
            } finally {
                try { serverSocket?.close() } catch (_: Exception) {}
            }
        }
    }

    private fun notifyVpnStatusChanged(status: String) {
        val statusIntent = Intent("$packageName.VPN_STATUS").apply {
            putExtra("status", status)
            setPackage(packageName)
        }
        sendBroadcast(statusIntent)

        try {
            DnsWidgetHelper.updateAllWidgets(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                TileService.requestListeningState(
                    this,
                    ComponentName(this, DnsTileService::class.java)
                )
            }
        } catch (_: Exception) {}
    }

    private fun stopVpn() {
        isRunning = false
        cleanupVpnResources()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()

        notifyVpnStatusChanged("disconnected")
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
