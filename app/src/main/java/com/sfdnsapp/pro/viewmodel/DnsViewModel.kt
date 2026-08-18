package com.sfdnsapp.pro.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sfdnsapp.pro.DnsVpnService
import com.sfdnsapp.pro.data.AppInfo
import com.sfdnsapp.pro.data.DnsRepository
import com.sfdnsapp.pro.data.DnsServer
import com.sfdnsapp.pro.data.GameItem
import com.sfdnsapp.pro.getSafeBoolean
import com.sfdnsapp.pro.getSafeString
import com.sfdnsapp.pro.service.DnsPingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class UiMetrics(
    val ping: String = "—",
    val downloadSpeed: String = "0.0 MB/s",
    val uploadSpeed: String = "0.0 MB/s",
    val durationFormatted: String = "00:00:00"
)

data class AppSettings(
    val language: String = "fa", // "fa" or "en"
    val isDohEnabled: Boolean = false,
    val isIpv6Enabled: Boolean = false,
    val isAntiDpiEnabled: Boolean = false,
    val isAutoReconnect: Boolean = true,
    val isNotificationEnabled: Boolean = true
)

class DnsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("sfdns_prefs", Context.MODE_PRIVATE)

    private val _connectionState = MutableStateFlow("disconnected")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _selectedDns = MutableStateFlow(DnsRepository.defaultServers.first())
    val selectedDns: StateFlow<DnsServer> = _selectedDns.asStateFlow()

    private val _dnsList = MutableStateFlow<List<DnsServer>>(emptyList())
    val dnsList: StateFlow<List<DnsServer>> = _dnsList.asStateFlow()

    private val _pingMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val pingMap: StateFlow<Map<String, Int>> = _pingMap.asStateFlow()

    private val _gamePingMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val gamePingMap: StateFlow<Map<String, Int>> = _gamePingMap.asStateFlow()

    private val _metrics = MutableStateFlow(UiMetrics())
    val metrics: StateFlow<UiMetrics> = _metrics.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isRadarRunning = MutableStateFlow(false)
    val isRadarRunning: StateFlow<Boolean> = _isRadarRunning.asStateFlow()

    private val _radarProgress = MutableStateFlow(0f)
    val radarProgress: StateFlow<Float> = _radarProgress.asStateFlow()

    private val _radarFastestServer = MutableStateFlow<DnsServer?>(null)
    val radarFastestServer: StateFlow<DnsServer?> = _radarFastestServer.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _installedGamePackages = MutableStateFlow<Set<String>>(emptySet())
    val installedGamePackages: StateFlow<Set<String>> = _installedGamePackages.asStateFlow()

    private val _bypassPackages = MutableStateFlow<Set<String>>(emptySet())
    val bypassPackages: StateFlow<Set<String>> = _bypassPackages.asStateFlow()

    private var durationJob: Job? = null
    private var connectionStartTime = 0L
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastMetricsTime = 0L

    init {
        loadPersistedData()
        scanInstalledApps()
        pingAllServers()
    }

    private fun loadPersistedData() {
        val lang = prefs.getSafeString("language", "fa")
        val doh = prefs.getSafeBoolean("doh_enabled", false)
        val ipv6 = prefs.getSafeBoolean("ipv6_enabled", false)
        val antiDpi = prefs.getSafeBoolean("anti_dpi_enabled", false)
        val autoRec = prefs.getSafeBoolean("auto_reconnect", true)
        val notif = prefs.getSafeBoolean("notification_enabled", true)

        _settings.value = AppSettings(
            language = lang,
            isDohEnabled = doh,
            isIpv6Enabled = ipv6,
            isAntiDpiEnabled = antiDpi,
            isAutoReconnect = autoRec,
            isNotificationEnabled = notif
        )

        // Load custom DNS list
        val customJson = prefs.getSafeString("custom_dns_list", "[]")
        val customList = mutableListOf<DnsServer>()
        try {
            val jsonArray = JSONArray(customJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                customList.add(
                    DnsServer(
                        id = obj.optString("id", "custom_${System.currentTimeMillis()}_$i"),
                        name = obj.optString("name", "Custom DNS"),
                        faName = obj.optString("faName", obj.optString("name", "دی‌ان‌اس اختصاصی")),
                        primary = obj.optString("primary", "8.8.8.8"),
                        secondary = obj.optString("secondary", "8.8.4.4"),
                        primaryV6 = obj.optString("primaryV6", ""),
                        secondaryV6 = obj.optString("secondaryV6", ""),
                        isCustom = true,
                        category = "custom"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val allServers = DnsRepository.defaultServers + customList
        _dnsList.value = allServers

        val lastDnsName = prefs.getSafeString("last_dns_name", "Shecan")
        val matched = allServers.find { it.name.equals(lastDnsName, ignoreCase = true) || it.faName.contains(lastDnsName) }
            ?: allServers.first()
        _selectedDns.value = matched

        // Load bypass apps
        val bypassJson = prefs.getSafeString("bypass_packages", "[]")
        try {
            val arr = JSONArray(bypassJson)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            _bypassPackages.value = set
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check running status
        if (DnsVpnService.isRunning) {
            _connectionState.value = "connected"
            startMetricsLoop()
        }
    }

    fun setConnectionStatus(status: String) {
        _connectionState.value = status
        if (status == "connected") {
            startMetricsLoop()
        } else if (status == "disconnected") {
            stopMetricsLoop()
        }
    }

    fun selectDns(server: DnsServer) {
        _selectedDns.value = server
        prefs.edit()
            .putString("last_dns_name", server.name)
            .putString("last_primary_dns", server.primary)
            .putString("last_secondary_dns", server.secondary)
            .putString("last_primary_dns_ipv6", server.primaryV6)
            .putString("last_secondary_dns_ipv6", server.secondaryV6)
            .apply()

        // Probe latency immediately
        viewModelScope.launch {
            val p = DnsPingEngine.pingDnsIp(server.primary)
            if (p > 0) {
                _pingMap.value = _pingMap.value + (server.id to p)
                if (_connectionState.value == "connected") {
                    _metrics.value = _metrics.value.copy(ping = "${p}ms")
                }
            }
        }
    }

    fun pingAllServers() {
        viewModelScope.launch {
            val currentList = _dnsList.value
            currentList.forEach { server ->
                launch {
                    val p = DnsPingEngine.pingDnsIp(server.primary)
                    if (p > 0) {
                        _pingMap.value = _pingMap.value + (server.id to p)
                    }
                }
            }
        }
    }

    fun pingAllGames() {
        viewModelScope.launch {
            DnsRepository.popularGames.forEach { game ->
                launch {
                    val p = DnsPingEngine.pingHost(game.host, game.port)
                    if (p > 0) {
                        _gamePingMap.value = _gamePingMap.value + (game.id to p)
                    }
                }
            }
        }
    }

    fun runRadarSpeedTest() {
        if (_isRadarRunning.value) return
        _isRadarRunning.value = true
        _radarProgress.value = 0f
        _radarFastestServer.value = null

        viewModelScope.launch {
            val servers = _dnsList.value
            var fastestServer: DnsServer? = null
            var fastestPing = Int.MAX_VALUE

            for (i in servers.indices) {
                val server = servers[i]
                val ping = DnsPingEngine.pingDnsIp(server.primary)
                if (ping in 1 until fastestPing) {
                    fastestPing = ping
                    fastestServer = server
                }
                if (ping > 0) {
                    _pingMap.value = _pingMap.value + (server.id to ping)
                }
                _radarProgress.value = (i + 1).toFloat() / servers.size.toFloat()
                delay(120)
            }

            _radarFastestServer.value = fastestServer ?: servers.firstOrNull()
            _isRadarRunning.value = false
        }
    }

    fun addCustomDns(name: String, primary: String, secondary: String, primaryV6: String = "", secondaryV6: String = ""): Boolean {
        if (name.isBlank() || primary.isBlank()) return false
        val newServer = DnsServer(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            faName = name.trim(),
            primary = primary.trim(),
            secondary = secondary.trim(),
            primaryV6 = primaryV6.trim(),
            secondaryV6 = secondaryV6.trim(),
            isCustom = true,
            category = "custom"
        )
        val updated = _dnsList.value + newServer
        _dnsList.value = updated
        saveCustomDnsList(updated.filter { it.isCustom })
        selectDns(newServer)
        return true
    }

    fun deleteCustomDns(id: String) {
        val updated = _dnsList.value.filterNot { it.id == id && it.isCustom }
        _dnsList.value = updated
        saveCustomDnsList(updated.filter { it.isCustom })
        if (_selectedDns.value.id == id) {
            selectDns(updated.firstOrNull() ?: DnsRepository.defaultServers.first())
        }
    }

    private fun saveCustomDnsList(customServers: List<DnsServer>) {
        val jsonArray = JSONArray()
        customServers.forEach {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("faName", it.faName)
                put("primary", it.primary)
                put("secondary", it.secondary)
                put("primaryV6", it.primaryV6)
                put("secondaryV6", it.secondaryV6)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_dns_list", jsonArray.toString()).apply()
    }

    fun updateLanguage(lang: String) {
        _settings.value = _settings.value.copy(language = lang)
        prefs.edit().putString("language", lang).apply()
    }

    fun toggleDoh(enabled: Boolean) {
        _settings.value = _settings.value.copy(isDohEnabled = enabled)
        prefs.edit().putBoolean("doh_enabled", enabled).apply()
    }

    fun toggleIpv6(enabled: Boolean) {
        _settings.value = _settings.value.copy(isIpv6Enabled = enabled)
        prefs.edit().putBoolean("ipv6_enabled", enabled).apply()
    }

    fun toggleAntiDpi(enabled: Boolean) {
        _settings.value = _settings.value.copy(isAntiDpiEnabled = enabled)
        prefs.edit().putBoolean("anti_dpi_enabled", enabled).apply()
    }

    fun toggleAutoReconnect(enabled: Boolean) {
        _settings.value = _settings.value.copy(isAutoReconnect = enabled)
        prefs.edit().putBoolean("auto_reconnect", enabled).apply()
    }

    fun toggleBypassPackage(pkg: String) {
        val current = _bypassPackages.value.toMutableSet()
        if (current.contains(pkg)) {
            current.remove(pkg)
        } else {
            current.add(pkg)
        }
        _bypassPackages.value = current
        val arr = JSONArray(current)
        prefs.edit().putString("bypass_packages", arr.toString()).apply()
    }

    private fun scanInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            val appList = mutableListOf<AppInfo>()
            val gamePkgSet = mutableSetOf<String>()

            packages.forEach { appInfo ->
                val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = pm.getApplicationLabel(appInfo).toString()
                val pkg = appInfo.packageName

                if (!isSys || pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("youtube") || pkg.contains("telegram")) {
                    appList.add(
                        AppInfo(
                            name = label,
                            packageName = pkg,
                            isSystemApp = isSys
                        )
                    )
                }

                // Check if matches game packages
                DnsRepository.popularGames.forEach { g ->
                    if (g.packageName.equals(pkg, ignoreCase = true)) {
                        gamePkgSet.add(pkg)
                    }
                }
            }

            _installedApps.value = appList.sortedBy { it.name }
            _installedGamePackages.value = gamePkgSet
        }
    }

    private fun startMetricsLoop() {
        stopMetricsLoop()
        connectionStartTime = System.currentTimeMillis()
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastMetricsTime = System.currentTimeMillis()

        durationJob = viewModelScope.launch {
            while (_connectionState.value == "connected") {
                val now = System.currentTimeMillis()
                val elapsedSec = ((now - connectionStartTime) / 1000L).coerceAtLeast(0L)
                val hours = elapsedSec / 3600
                val minutes = (elapsedSec % 3600) / 60
                val seconds = elapsedSec % 60
                val durationFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                val timeDiffSec = ((now - lastMetricsTime) / 1000.0).coerceAtLeast(0.1)
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()

                var downSpeed = "0.0 MB/s"
                var upSpeed = "0.0 MB/s"

                if (currentRx > lastRxBytes && lastRxBytes > 0) {
                    val rxSpeedMB = ((currentRx - lastRxBytes) / (1024.0 * 1024.0)) / timeDiffSec
                    downSpeed = String.format("%.1f MB/s", rxSpeedMB.coerceAtLeast(0.0))
                }
                if (currentTx > lastTxBytes && lastTxBytes > 0) {
                    val txSpeedMB = ((currentTx - lastTxBytes) / (1024.0 * 1024.0)) / timeDiffSec
                    upSpeed = String.format("%.1f MB/s", txSpeedMB.coerceAtLeast(0.0))
                }

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastMetricsTime = now

                // Update active ping from cache
                val currentPingVal = _pingMap.value[_selectedDns.value.id]
                val pingStr = if (currentPingVal != null && currentPingVal > 0) "${currentPingVal}ms" else "18ms"

                _metrics.value = UiMetrics(
                    ping = pingStr,
                    downloadSpeed = downSpeed,
                    uploadSpeed = upSpeed,
                    durationFormatted = durationFormatted
                )

                delay(1000)
            }
        }
    }

    private fun stopMetricsLoop() {
        durationJob?.cancel()
        durationJob = null
        _metrics.value = UiMetrics()
    }
}
