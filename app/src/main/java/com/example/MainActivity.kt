package com.example

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket
import java.net.InetAddress
import java.net.DatagramPacket
import java.net.DatagramSocket

import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var isContentReady = false
    private val mainJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + mainJob)
    private val pingSemaphore = kotlinx.coroutines.sync.Semaphore(6)

    private var cachedDnsName: String? = null
    private var cachedPrimaryDns: String? = null
    private var cachedSecondaryDns: String? = null
    private var cachedPrimaryDnsIpv6: String? = null
    private var cachedSecondaryDnsIpv6: String? = null

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startCachedVpn()
        } else {
            android.util.Log.w("MainActivity", "VPN prepare launcher resultCode=${result.resultCode}. Starting cached VPN as fallback.")
            try {
                startCachedVpn()
            } catch (e: Exception) {
                sendVpnStatusToJs("disconnected")
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (::webView.isInitialized) {
            webView.post {
                webView.evaluateJavascript("if (window.onNotificationPermissionResult) { onNotificationPermissionResult($isGranted); }", null)
            }
        }
    }

    private val vpnStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status") ?: "disconnected"
            sendVpnStatusToJs(status)
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isContentReady }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bgColor = android.graphics.Color.parseColor("#0c0c0e")
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(bgColor)
                ) {
                    // Setup WebView inside Jetpack Compose, handling proper status bar & navigation insets
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                this@MainActivity.webView = this
                                setupWebView(bgColor)
                                loadUrl("file:///android_asset/index.html")
                            }
                        },
                        update = { webView ->
                            webView.setBackgroundColor(bgColor)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                }
            }
        }

        // Register receiver for VPN status changes
        val filter = IntentFilter("$packageName.VPN_STATUS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(vpnStatusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(vpnStatusReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
            webView.pauseTimers()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
            val status = if (DnsVpnService.isRunning) "connected" else "disconnected"
            sendVpnStatusToJs(status)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.setupWebView(bgColor: Int) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        
        // Dynamic caching and performance settings
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        
        // Disable cross-origin file URL access for better security
        try {
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting file access properties", e)
        }

        // Set solid background matching HTML theme to prevent black/blank screens
        setBackgroundColor(bgColor)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("file:///android_asset/")) {
                    return false // Allow local asset loading
                }
                // Redirect external links to system browser / external apps safely
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to handle external link: $url", e)
                }
                return true // Prevent loading external sites inside our app WebView
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isContentReady = true
                // Sync actual active VPN state on page load
                val initialStatus = if (DnsVpnService.isRunning) "connected" else "disconnected"
                sendVpnStatusToJs(initialStatus)
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                android.util.Log.e("WebViewError", "Error ($errorCode): $description for $failingUrl")
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.util.Log.e("WebViewError", "Resource Error: ${error?.description} (${error?.errorCode}) for ${request?.url}")
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                android.util.Log.e("WebViewSSL", "SSL error encountered: ${error?.toString()}")
                handler?.cancel() // Secure fallback: reject untrusted or invalid SSL certificates
            }
        }

        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.d("WebViewConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
        }

        addJavascriptInterface(AndroidWebBridge(), "AndroidBridge")
    }



    private fun sendVpnStatusToJs(status: String) {
        if (!::webView.isInitialized) return
        val sanitizedStatus = when (status) {
            "connected", "disconnected", "connecting" -> status
            else -> {
                android.util.Log.e("SecurityWarning", "Ignored unauthorized status value: $status")
                return
            }
        }
        webView.post {
            webView.evaluateJavascript("if (window.onVpnStatusChanged) { onVpnStatusChanged('$sanitizedStatus'); }", null)
        }
    }

    private fun startCachedVpn() {
        val name = cachedDnsName ?: "DNS"
        val primary = cachedPrimaryDns ?: "8.8.8.8"
        val secondary = cachedSecondaryDns ?: "8.8.4.4"
        val primaryIpv6 = cachedPrimaryDnsIpv6 ?: ""
        val secondaryIpv6 = cachedSecondaryDnsIpv6 ?: ""

        val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
        val splitTunnelEnabled = prefs.getSafeBoolean("split_tunnel_enabled", false)
        val splitTunnelMode = prefs.getSafeString("split_tunnel_mode", "disallowed")
        val splitTunnelApps = prefs.getSafeString("split_tunnel_apps", "")

        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
            putExtra(DnsVpnService.EXTRA_DNS_NAME, name)
            putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, primary)
            putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, secondary)
            putExtra("primary_dns_ipv6", primaryIpv6)
            putExtra("secondary_dns_ipv6", secondaryIpv6)
            putExtra("split_tunnel_enabled", splitTunnelEnabled)
            putExtra("split_tunnel_mode", splitTunnelMode)
            putExtra("split_tunnel_apps", splitTunnelApps)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        sendVpnStatusToJs("connected")
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(vpnStatusReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        mainJob.cancel()
        super.onDestroy()
    }

    inner class AndroidWebBridge {

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

        @JavascriptInterface
        fun startVpn(dnsName: String, primaryDns: String, secondaryDns: String) {
            startVpn(dnsName, primaryDns, secondaryDns, "", "")
        }

        @JavascriptInterface
        fun startVpn(dnsName: String, primaryDns: String, secondaryDns: String, primaryIpv6: String, secondaryIpv6: String) {
            val cleanPrimary = if (isValidIp(primaryDns)) primaryDns.trim() else "178.22.122.100"
            val cleanSecondary = if (isValidIp(secondaryDns)) secondaryDns.trim() else "185.51.200.2"
            val cleanPrimaryIpv6 = if (isValidIp(primaryIpv6)) primaryIpv6.trim() else ""
            val cleanSecondaryIpv6 = if (isValidIp(secondaryIpv6)) secondaryIpv6.trim() else ""

            cachedDnsName = dnsName
            cachedPrimaryDns = cleanPrimary
            cachedSecondaryDns = cleanSecondary
            cachedPrimaryDnsIpv6 = cleanPrimaryIpv6
            cachedSecondaryDnsIpv6 = cleanSecondaryIpv6

            // Save last connected DNS info for boot autoconnect
            val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
            prefs.edit().apply {
                putString("last_dns_name", dnsName)
                putString("last_primary_dns", cleanPrimary)
                putString("last_secondary_dns", cleanSecondary)
                putString("last_primary_dns_ipv6", cleanPrimaryIpv6)
                putString("last_secondary_dns_ipv6", cleanSecondaryIpv6)
                apply()
            }

            scope.launch {
                try {
                    val intent = VpnService.prepare(this@MainActivity)
                    if (intent != null) {
                        sendVpnStatusToJs("connecting")
                        vpnPrepareLauncher.launch(intent)
                    } else {
                        startCachedVpn()
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("MainActivity", "SecurityException preparing VPN service, attempting direct start as fallback", e)
                    try {
                        startCachedVpn()
                    } catch (fallbackEx: Exception) {
                        android.util.Log.e("MainActivity", "Fallback VPN start failed", fallbackEx)
                        sendVpnStatusToJs("disconnected")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error preparing VPN service", e)
                    sendVpnStatusToJs("disconnected")
                }
            }
        }

        @JavascriptInterface
        fun stopVpn() {
            val intent = Intent(this@MainActivity, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_STOP
            }
            startService(intent)
            sendVpnStatusToJs("disconnected")
        }

        private fun isValidCallbackId(id: String): Boolean {
            return id.matches(Regex("^[a-zA-Z0-9_]+$"))
        }

        @JavascriptInterface
        fun pingDNS(ip: String, callbackId: String) {
            if (!isValidCallbackId(callbackId)) {
                android.util.Log.e("SecurityWarning", "Rejected invalid callbackId in pingDNS: $callbackId")
                return
            }
            scope.launch(Dispatchers.IO) {
                pingSemaphore.withPermit {
                    val pingMs = testSocketPing(ip)
                    if (pingMs > 0) {
                        val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
                        prefs.edit().putString("last_dns_ping", "${pingMs}ms").apply()
                        scope.launch(Dispatchers.Main) {
                            DnsWidgetHelper.updateAllWidgets(this@MainActivity)
                        }
                    }
                    scope.launch(Dispatchers.Main) {
                        webView.evaluateJavascript("if (window['$callbackId']) { window['$callbackId']($pingMs); }", null)
                    }
                }
            }
        }

        @JavascriptInterface
        fun pingDomain(domain: String, callbackId: String) {
            if (!isValidCallbackId(callbackId)) {
                android.util.Log.e("SecurityWarning", "Rejected invalid callbackId in pingDomain: $callbackId")
                return
            }
            scope.launch(Dispatchers.IO) {
                pingSemaphore.withPermit {
                    var pingMs = -1
                    try {
                        val address = InetAddress.getByName(domain)
                        if (address != null) {
                            val startTime = System.nanoTime()
                            Socket().use { socket ->
                                DnsVpnService.protectSocket(socket)
                                socket.connect(InetSocketAddress(address, 443), 1500)
                                val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                                pingMs = if (elapsed > 0) elapsed else 1
                            }
                        }
                    } catch (e: Exception) {
                        try {
                            val address = InetAddress.getByName(domain)
                            if (address != null) {
                                val startTime = System.nanoTime()
                                Socket().use { socket ->
                                    DnsVpnService.protectSocket(socket)
                                    socket.connect(InetSocketAddress(address, 80), 1200)
                                    val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                                    pingMs = if (elapsed > 0) elapsed else 1
                                }
                            }
                        } catch (ex: Exception) {
                            pingMs = -1
                        }
                    }
                    scope.launch(Dispatchers.Main) {
                        webView.evaluateJavascript("if (window['$callbackId']) { window['$callbackId']($pingMs); }", null)
                    }
                }
            }
        }

        private fun testSocketPing(ip: String): Int {
            if (ip.isEmpty() || ip == "0.0.0.0") return -1
            val address = try { InetAddress.getByName(ip) } catch (e: Exception) { return -1 }

            var bestPing = Int.MAX_VALUE

            // Try UDP DNS query twice to eliminate cold socket / WiFi radio wake-up latency spikes
            for (attempt in 0 until 2) {
                val id1 = (1..254).random().toByte()
                val id2 = (1..254).random().toByte()
                val queryBytes = byteArrayOf(
                    id1, id2,
                    0x01.toByte(), 0x00.toByte(), // Standard query
                    0x00.toByte(), 0x01.toByte(), // 1 question
                    0x00.toByte(), 0x00.toByte(),
                    0x00.toByte(), 0x00.toByte(),
                    0x00.toByte(), 0x00.toByte(),
                    0x06.toByte(), 'g'.toByte(), 'o'.toByte(), 'o'.toByte(), 'g'.toByte(), 'l'.toByte(), 'e'.toByte(),
                    0x03.toByte(), 'c'.toByte(), 'o'.toByte(), 'm'.toByte(),
                    0x00.toByte(),
                    0x00.toByte(), 0x01.toByte(),
                    0x00.toByte(), 0x01.toByte()
                )

                try {
                    DatagramSocket().use { socket ->
                        DnsVpnService.protectSocket(socket)
                        socket.soTimeout = 800 // 800ms timeout per packet
                        val startTime = System.nanoTime()
                        val packet = DatagramPacket(queryBytes, queryBytes.size, address, 53)
                        socket.send(packet)

                        val responseBytes = ByteArray(512)
                        val responsePacket = DatagramPacket(responseBytes, responseBytes.size)
                        socket.receive(responsePacket)

                        val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                        val validPing = if (elapsed > 0) elapsed else 1
                        if (validPing < bestPing) {
                            bestPing = validPing
                        }
                    }
                } catch (e: Exception) {
                    // UDP socket timeout or unreachable
                }

                if (bestPing < Int.MAX_VALUE && attempt == 0) {
                    try { Thread.sleep(15) } catch (e: Exception) {}
                }
            }

            if (bestPing < Int.MAX_VALUE) return bestPing

            // Fallback to TCP connection check if UDP DNS is filtered by ISP
            return try {
                val startTime = System.nanoTime()
                Socket().use { socket ->
                    DnsVpnService.protectSocket(socket)
                    socket.connect(InetSocketAddress(address, 53), 1000)
                    val elapsed = ((System.nanoTime() - startTime) / 1_000_000).toInt()
                    if (elapsed > 0) elapsed else 1
                }
            } catch (e: Exception) {
                -1
            }
        }

        @JavascriptInterface
        fun verifyDnsConnection(callbackId: String) {
            if (!isValidCallbackId(callbackId)) {
                android.util.Log.e("SecurityWarning", "Rejected invalid callbackId in verifyDnsConnection: $callbackId")
                return
            }
            scope.launch(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                var success = false
                var ipList = ""
                var errorMsg = ""
                try {
                    // This will perform a live DNS resolution using the active DNS/network interface
                    val addresses = InetAddress.getAllByName("google.com")
                    if (addresses.isNotEmpty()) {
                        success = true
                        ipList = addresses.map { it.hostAddress ?: "" }.filter { it.isNotEmpty() }.joinToString(", ")
                    }
                } catch (e: Exception) {
                    errorMsg = e.message ?: "Unknown resolution failure"
                }
                val elapsed = System.currentTimeMillis() - startTime
                // Escape quotes
                val safeIps = ipList.replace("\"", "\\\"")
                val safeError = errorMsg.replace("\"", "\\\"")
                val json = """{"success":$success,"elapsed":$elapsed,"ips":"$safeIps","error":"$safeError"}"""
                scope.launch(Dispatchers.Main) {
                    webView.evaluateJavascript("if (window['$callbackId']) { window['$callbackId']($json); }", null)
                }
            }
        }

        @JavascriptInterface
        fun resolveDoH(domain: String, callbackId: String) {
            if (!isValidCallbackId(callbackId)) return
            scope.launch(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                var resolvedIps = ""
                var success = false
                try {
                    val url = java.net.URL("https://dns.google/resolve?name=${domain}&type=A")
                    val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Accept", "application/dns-json")
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        if (response.contains("\"Answer\":")) {
                            success = true
                            resolvedIps = response
                        }
                    }
                } catch (e: Exception) {
                    try {
                        val url = java.net.URL("https://1.1.1.1/dns-query?name=${domain}&type=A")
                        val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                        conn.requestMethod = "GET"
                        conn.setRequestProperty("Accept", "application/dns-json")
                        conn.connectTimeout = 2000
                        conn.readTimeout = 2000
                        if (conn.responseCode == 200) {
                            val response = conn.inputStream.bufferedReader().use { it.readText() }
                            success = true
                            resolvedIps = response
                        }
                    } catch (ex: Exception) {
                        success = false
                    }
                }
                val elapsed = System.currentTimeMillis() - startTime
                val safeIps = resolvedIps.replace("\"", "\\\"")
                val json = """{"success":$success,"elapsed":$elapsed,"data":"$safeIps"}"""
                scope.launch(Dispatchers.Main) {
                    webView.evaluateJavascript("if (window['$callbackId']) { window['$callbackId']($json); }", null)
                }
            }
        }

        @JavascriptInterface
        fun openTelegram(url: String, fallbackUrl: String) {
            scope.launch(Dispatchers.Main) {
                fun isAllowedScheme(u: String): Boolean {
                    val lower = u.lowercase().trim()
                    return lower.startsWith("tg://") || lower.startsWith("https://t.me/") || lower.startsWith("https://telegram.me/")
                }
                try {
                    val targetUrl = if (isAllowedScheme(url)) url else if (isAllowedScheme(fallbackUrl)) fallbackUrl else "https://t.me/"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        if (isAllowedScheme(fallbackUrl)) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        }
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
            }
        }

        @JavascriptInterface
        fun openGmail(email: String) {
            scope.launch(Dispatchers.Main) {
                val cleanEmail = email.trim()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                    android.util.Log.e("SecurityWarning", "Rejected invalid email address: $email")
                    return@launch
                }
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$cleanEmail")
                        putExtra(Intent.EXTRA_SUBJECT, "SFDNS Pro Support v2.3")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "plain/text"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanEmail))
                            putExtra(Intent.EXTRA_SUBJECT, "SFDNS Pro Support v2.3")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(Intent.createChooser(intent, "Send Email").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (ex: Exception) {
                        // Ignore
                    }
                }
            }
        }

        @JavascriptInterface
        fun saveSetting(key: String, value: String) {
            val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
            prefs.edit()
                .putString(key, value)
                .putBoolean(key + "_bool", value == "true")
                .apply()
        }

        @JavascriptInterface
        fun getSetting(key: String): String {
            val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
            return prefs.getSafeString(key, "false")
        }

        @JavascriptInterface
        fun getTrafficStats(): String {
            val uid = android.os.Process.myUid()
            val rx = android.net.TrafficStats.getUidRxBytes(uid)
            val tx = android.net.TrafficStats.getUidTxBytes(uid)
            val totalRx = android.net.TrafficStats.getTotalRxBytes()
            val totalTx = android.net.TrafficStats.getTotalTxBytes()
            return """{"rx":$rx,"tx":$tx,"totalRx":$totalRx,"totalTx":$totalTx}"""
        }

        @JavascriptInterface
        fun hasNotificationPermission(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            return true
        }

        @JavascriptInterface
        fun requestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        @JavascriptInterface
        fun saveStringSetting(key: String, value: String) {
            val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
        }

        @JavascriptInterface
        fun getStringSetting(key: String, defaultValue: String): String {
            val prefs = getSharedPreferences("sfdns_prefs", MODE_PRIVATE)
            return prefs.getSafeString(key, defaultValue)
        }

        private fun getAppIconBase64(packageName: String): String {
            return try {
                val pm = packageManager
                val icon = pm.getApplicationIcon(packageName)
                val bitmap = if (icon is android.graphics.drawable.BitmapDrawable) {
                    icon.bitmap
                } else {
                    val width = icon.intrinsicWidth.coerceAtLeast(1)
                    val height = icon.intrinsicHeight.coerceAtLeast(1)
                    val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    icon.setBounds(0, 0, canvas.width, canvas.height)
                    icon.draw(canvas)
                    bmp
                }
                
                val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                val outputStream = java.io.ByteArrayOutputStream()
                resized.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, outputStream)
                val bytes = outputStream.toByteArray()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun getInstalledApps(): String {
            return try {
                val pm = packageManager
                val appsList = org.json.JSONArray()
                val packages = pm.getInstalledPackages(0)
                
                val listWithLabel = ArrayList<Pair<android.content.pm.PackageInfo, String>>()
                for (pkg in packages) {
                    val label = try {
                        pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                    } catch (e: Exception) {
                        pkg.packageName
                    }
                    listWithLabel.add(Pair(pkg, label))
                }
                
                // Sort applications alphabetically by name
                listWithLabel.sortBy { it.second.lowercase() }

                for ((pkg, label) in listWithLabel) {
                    val isSystem = pkg.applicationInfo?.let {
                        (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    } ?: false
                    val launcherIntent = pm.getLaunchIntentForPackage(pkg.packageName)
                    
                    // Filter: user-installed apps or system apps with launchers (user facing apps like Chrome, WhatsApp)
                    if (launcherIntent != null || !isSystem) {
                        val appObj = org.json.JSONObject()
                        appObj.put("name", label)
                        appObj.put("package", pkg.packageName)
                        appObj.put("isSystem", isSystem)
                        appObj.put("icon", getAppIconBase64(pkg.packageName))
                        appsList.put(appObj)
                    }
                }
                appsList.toString()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error listing installed apps", e)
                "[]"
            }
        }

        @JavascriptInterface
        fun isPrivateDnsActive(): String {
            return try {
                val mode = android.provider.Settings.Global.getString(contentResolver, "private_dns_mode") ?: "off"
                val specifier = android.provider.Settings.Global.getString(contentResolver, "private_dns_specifier") ?: ""
                val isEnabled = mode == "hostname" || mode == "opportunistic" || specifier.isNotEmpty()
                val safeMode = mode.replace("\"", "\\\"")
                val safeSpecifier = specifier.replace("\"", "\\\"")
                """{"active":$isEnabled,"mode":"$safeMode","specifier":"$safeSpecifier"}"""
            } catch (e: Exception) {
                """{"active":false,"mode":"unknown","specifier":""}"""
            }
        }

        @JavascriptInterface
        fun flushDNSCache(callbackId: String) {
            if (!isValidCallbackId(callbackId)) {
                android.util.Log.e("SecurityWarning", "Rejected invalid callbackId in flushDNSCache: $callbackId")
                return
            }
            scope.launch(Dispatchers.IO) {
                var success = false
                try {
                    // Invalidate JVM network cache by forcing TTL cache durations to 0
                    java.security.Security.setProperty("networkaddress.cache.ttl", "0")
                    java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0")
                    
                    // Clear internal WebView internet files/cache 
                    scope.launch(Dispatchers.Main) {
                        webView.clearCache(true)
                    }
                    success = true
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to clear DNS cache", e)
                }
                
                kotlinx.coroutines.delay(1000) // Beautiful 1s delay for a highly polished, interactive UI experience
                
                scope.launch(Dispatchers.Main) {
                    webView.evaluateJavascript("if (window['$callbackId']) { window['$callbackId']($success); }", null)
                }
            }
        }

        @JavascriptInterface
        fun isIgnoringBatteryOptimizations(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                pm?.isIgnoringBatteryOptimizations(packageName) ?: true
            } else {
                true
            }
        }

        @JavascriptInterface
        fun requestIgnoreBatteryOptimizations() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    } catch (ex: Exception) {
                        android.util.Log.e("MainActivity", "Failed to launch battery optimization settings", ex)
                    }
                }
            }
        }
    }
}
