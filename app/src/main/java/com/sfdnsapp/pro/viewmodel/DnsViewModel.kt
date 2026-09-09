package com.sfdnsapp.pro.viewmodel

/**
 * ViewModel managing DNS selections, active VPN state, ping measurements, and system configuration.
 * Fully aligned with PrefKeys and hardened for Android 12+ and Android 14+.
 */
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sfdnsapp.pro.DnsVpnService
import com.sfdnsapp.pro.IpValidator
import com.sfdnsapp.pro.PrefKeys
import com.sfdnsapp.pro.data.AppInfo
import com.sfdnsapp.pro.data.DnsRepository
import com.sfdnsapp.pro.data.DnsServer
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
import org.json.JSONArray
import org.json.JSONObject

data class UiMetrics(
    val ping: String = "—",
    val downloadSpeed: String = "0 Q/s",
    val uploadSpeed: String = "—",
    val durationFormatted: String = "00:00:00"
)

data class AppSettings(
    val language: String = "fa", // "fa" or "en"
    val isDohEnabled: Boolean = false,
    val isIpv6Enabled: Boolean = false,
    val isAntiDpiEnabled: Boolean = false,
    val isAutoReconnect: Boolean = true,
    val isAutoConnectEnabled: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val isKillSwitchEnabled: Boolean = false,
    val carrierOpt: String = "auto", // "auto", "mci", "mtn", "wifi"
    val isSplitTunnelEnabled: Boolean = false,
    val splitTunnelMode: String = "disallowed", // "allowed", "disallowed"
    val isWidgetAutoSelectEnabled: Boolean = false
)

class DnsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

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

    init {
        loadPersistedData()
        scanInstalledApps()
        pingAllServers()
    }

    private fun loadPersistedData() {
        val lang = prefs.getSafeString(PrefKeys.KEY_LANGUAGE, "fa")
        val doh = prefs.getSafeBoolean(PrefKeys.KEY_DOH_ENABLED, false)
        val ipv6 = prefs.getSafeBoolean(PrefKeys.KEY_IPV6_ENABLED, false)
        val antiDpi = prefs.getSafeBoolean(PrefKeys.KEY_ANTI_DPI_ENABLED, false)
        val autoConnect = prefs.getSafeBoolean(PrefKeys.KEY_AUTO_CONNECT, true)
        val autoRec = prefs.getSafeBoolean(PrefKeys.KEY_AUTO_RECONNECT, autoConnect)
        val notif = prefs.getSafeBoolean(PrefKeys.KEY_NOTIFICATION_ENABLED, true)
        val killSwitch = prefs.getSafeBoolean(PrefKeys.KEY_KILL_SWITCH, false)
        val carrierOpt = prefs.getSafeString(PrefKeys.KEY_CARRIER_OPT, "auto")
        val splitEnabled = prefs.getSafeBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, false)
        val splitMode = prefs.getSafeString(PrefKeys.KEY_SPLIT_TUNNEL_MODE, "disallowed")
        val widgetAutoSelect = prefs.getSafeBoolean(PrefKeys.KEY_WIDGET_AUTO_SELECT, false)

        _settings.value = AppSettings(
            language = lang,
            isDohEnabled = doh,
            isIpv6Enabled = ipv6,
            isAntiDpiEnabled = antiDpi,
            isAutoReconnect = autoRec,
            isAutoConnectEnabled = autoConnect,
            isNotificationEnabled = notif,
            isKillSwitchEnabled = killSwitch,
            carrierOpt = carrierOpt,
            isSplitTunnelEnabled = splitEnabled,
            splitTunnelMode = splitMode,
            isWidgetAutoSelectEnabled = widgetAutoSelect
        )

        // Load custom DNS list
        val customJson = prefs.getSafeString(PrefKeys.KEY_CUSTOM_DNS_LIST, "[]")
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

        val lastDnsName = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "Shecan")
        val matched = allServers.find { it.name.equals(lastDnsName, ignoreCase = true) || it.faName.contains(lastDnsName) }
            ?: allServers.first()
        _selectedDns.value = matched

        // Load bypass apps
        val splitAppsStr = prefs.getSafeString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, "")
        val legacyBypassJson = prefs.getSafeString(PrefKeys.KEY_BYPASS_PACKAGES, "[]")
        val set = mutableSetOf<String>()

        if (splitAppsStr.isNotEmpty()) {
            set.addAll(splitAppsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
        } else {
            try {
                val arr = JSONArray(legacyBypassJson)
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
            } catch (_: Exception) {}
        }
        _bypassPackages.value = set

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

    /**
     * Persists the selected server's latest measured latency so home-screen
     * widgets can show a fresh value instead of a stale (or forever empty) one.
     */
    private fun persistSelectedPing(pingMs: Int) {
        prefs.edit().putString(PrefKeys.KEY_LAST_DNS_PING, "${pingMs}ms").apply()
    }

    fun selectDns(server: DnsServer) {
        _selectedDns.value = server
        prefs.edit()
            .putString(PrefKeys.KEY_LAST_DNS_NAME, server.name)
            .putString(PrefKeys.KEY_LAST_PRIMARY_DNS, server.primary)
            .putString(PrefKeys.KEY_LAST_SECONDARY_DNS, server.secondary)
            .putString(PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6, server.primaryV6)
            .putString(PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6, server.secondaryV6)
            .apply()

        // Probe latency immediately
        viewModelScope.launch {
            val p = DnsPingEngine.pingDnsIp(server.primary)
            if (p > 0) {
                _pingMap.value = _pingMap.value + (server.id to p)
                persistSelectedPing(p)
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
            val selected = _selectedDns.value
            launch {
                val p = DnsPingEngine.pingDnsIp(selected.primary)
                if (p > 0) {
                    _pingMap.value = _pingMap.value + (selected.id to p)
                    persistSelectedPing(p)
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
        val cleanPrimary = primary.trim()
        val cleanSecondary = secondary.trim()
        val cleanPrimaryV6 = primaryV6.trim()
        val cleanSecondaryV6 = secondaryV6.trim()

        if (!IpValidator.isValidIp(cleanPrimary)) return false
        if (cleanSecondary.isNotEmpty() && !IpValidator.isValidIp(cleanSecondary)) return false
        if (cleanPrimaryV6.isNotEmpty() && !IpValidator.isValidIpv6(cleanPrimaryV6)) return false
        if (cleanSecondaryV6.isNotEmpty() && !IpValidator.isValidIpv6(cleanSecondaryV6)) return false

        val newServer = DnsServer(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            faName = name.trim(),
            primary = cleanPrimary,
            secondary = cleanSecondary,
            primaryV6 = cleanPrimaryV6,
            secondaryV6 = cleanSecondaryV6,
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
        prefs.edit().putString(PrefKeys.KEY_CUSTOM_DNS_LIST, jsonArray.toString()).apply()
    }

    fun updateLanguage(lang: String) {
        _settings.value = _settings.value.copy(language = lang)
        prefs.edit().putString(PrefKeys.KEY_LANGUAGE, lang).apply()
    }

    fun toggleDoh(enabled: Boolean) {
        _settings.value = _settings.value.copy(isDohEnabled = enabled)
        prefs.edit().putBoolean(PrefKeys.KEY_DOH_ENABLED, enabled).apply()
    }

    fun toggleIpv6(enabled: Boolean) {
        _settings.value = _settings.value.copy(isIpv6Enabled = enabled)
        prefs.edit().putBoolean(PrefKeys.KEY_IPV6_ENABLED, enabled).apply()
    }

    fun toggleAntiDpi(enabled: Boolean) {
        _settings.value = _settings.value.copy(isAntiDpiEnabled = enabled)
        prefs.edit().putBoolean(PrefKeys.KEY_ANTI_DPI_ENABLED, enabled).apply()
    }

    fun toggleAutoConnect(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            isAutoConnectEnabled = enabled,
            isAutoReconnect = enabled
        )
        prefs.edit()
            .putBoolean(PrefKeys.KEY_AUTO_CONNECT, enabled)
            .putBoolean(PrefKeys.KEY_AUTO_RECONNECT, enabled)
            .apply()
    }

    fun toggleAutoReconnect(enabled: Boolean) {
        toggleAutoConnect(enabled)
    }

    fun toggleKillSwitch(enabled: Boolean) {
        _settings.value = _settings.value.copy(isKillSwitchEnabled = enabled)
        prefs.edit().putBoolean(PrefKeys.KEY_KILL_SWITCH, enabled).apply()
    }

    fun toggleWidgetAutoSelect(enabled: Boolean) {
        _settings.value = _settings.value.copy(isWidgetAutoSelectEnabled = enabled)
        prefs.edit().putBoolean(PrefKeys.KEY_WIDGET_AUTO_SELECT, enabled).apply()
    }

    fun setCarrierOpt(carrier: String) {
        _settings.value = _settings.value.copy(carrierOpt = carrier)
        prefs.edit().putString(PrefKeys.KEY_CARRIER_OPT, carrier).apply()
    }

    fun setSplitTunnel(enabled: Boolean, mode: String, apps: Set<String>) {
        _settings.value = _settings.value.copy(
            isSplitTunnelEnabled = enabled,
            splitTunnelMode = mode
        )
        _bypassPackages.value = apps
        val appsStr = apps.joinToString(",")
        val arr = JSONArray(apps)
        prefs.edit()
            .putBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, enabled)
            .putString(PrefKeys.KEY_SPLIT_TUNNEL_MODE, mode)
            .putString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, appsStr)
            .putString(PrefKeys.KEY_BYPASS_PACKAGES, arr.toString())
            .apply()
    }

    fun toggleBypassPackage(pkg: String) {
        val current = _bypassPackages.value.toMutableSet()
        if (current.contains(pkg)) {
            current.remove(pkg)
        } else {
            current.add(pkg)
        }
        _bypassPackages.value = current
        val isEnabled = current.isNotEmpty()
        _settings.value = _settings.value.copy(isSplitTunnelEnabled = isEnabled)
        val appsStr = current.joinToString(",")
        val arr = JSONArray(current)

        prefs.edit()
            .putBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, isEnabled)
            .putString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, appsStr)
            .putString(PrefKeys.KEY_BYPASS_PACKAGES, arr.toString())
            .apply()
    }

    private fun scanInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val appList = mutableListOf<AppInfo>()
            val gamePkgSet = mutableSetOf<String>()
            val seenPackages = mutableSetOf<String>()

            // 1. Query all launcher activities (guaranteed to find installed user-facing apps)
            try {
                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(launcherIntent, 0)
                }

                for (ri in resolveInfos) {
                    val pkg = ri.activityInfo?.packageName ?: continue
                    if (seenPackages.add(pkg)) {
                        val label = ri.loadLabel(pm).toString()
                        val isSys = (ri.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        appList.add(AppInfo(name = label, packageName = pkg, isSystemApp = isSys))
                    }
                }
            } catch (_: Exception) {}

            // 2. Also try getInstalledApplications for completeness
            try {
                val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(0)
                }

                packages.forEach { appInfo ->
                    val pkg = appInfo.packageName
                    if (seenPackages.add(pkg)) {
                        val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val label = pm.getApplicationLabel(appInfo).toString()
                        if (!isSys || pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("youtube") || pkg.contains("telegram")) {
                            appList.add(AppInfo(name = label, packageName = pkg, isSystemApp = isSys))
                        }
                    }
                }
            } catch (_: Exception) {}

            // Check game matches
            for (app in appList) {
                DnsRepository.popularGames.forEach { g ->
                    if (g.packageName.equals(app.packageName, ignoreCase = true)) {
                        gamePkgSet.add(app.packageName)
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

        durationJob = viewModelScope.launch {
            var lastQueries = DnsVpnService.dohQueryCounter.get()
            var lastTime = System.currentTimeMillis()

            while (_connectionState.value == "connected") {
                val now = System.currentTimeMillis()
                val elapsedSec = ((now - connectionStartTime) / 1000L).coerceAtLeast(0L)
                val hours = elapsedSec / 3600
                val minutes = (elapsedSec % 3600) / 60
                val seconds = elapsedSec % 60
                val durationFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                val timeDiffSec = ((now - lastTime) / 1000.0).coerceAtLeast(0.1)
                val currentQueries = DnsVpnService.dohQueryCounter.get()
                val qDiff = (currentQueries - lastQueries).coerceAtLeast(0)
                val qps = if (timeDiffSec > 0) (qDiff / timeDiffSec).toLong() else 0L

                lastQueries = currentQueries
                lastTime = now

                // Accurate active ping from cache, or "—" if not pinged yet (never hardcoded 18ms)
                val currentPingVal = _pingMap.value[_selectedDns.value.id]
                val pingStr = if (currentPingVal != null && currentPingVal > 0) "${currentPingVal}ms" else "—"
                val uploadMetric = if (currentQueries > 0) "$currentQueries Total" else "—"

                _metrics.value = UiMetrics(
                    ping = pingStr,
                    downloadSpeed = "$qps Q/s",
                    uploadSpeed = uploadMetric,
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
