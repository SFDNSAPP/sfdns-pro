package com.sfdnsapp.pro

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sfdnsapp.pro.viewmodel.DnsViewModel
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrefKeysInteroperabilityTest {

    private lateinit var app: Application
    private lateinit var viewModel: DnsViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        viewModel = DnsViewModel(app)
    }

    @Test
    fun `PrefKeys constants match expected unified keys`() {
        assertEquals("sfdns_prefs", PrefKeys.PREFS_NAME)
        assertEquals("last_dns_name", PrefKeys.KEY_LAST_DNS_NAME)
        assertEquals("last_primary_dns", PrefKeys.KEY_LAST_PRIMARY_DNS)
        assertEquals("last_secondary_dns", PrefKeys.KEY_LAST_SECONDARY_DNS)
        assertEquals("last_primary_dns_ipv6", PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6)
        assertEquals("last_secondary_dns_ipv6", PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6)
        assertEquals("language", PrefKeys.KEY_LANGUAGE)
        assertEquals("doh_enabled", PrefKeys.KEY_DOH_ENABLED)
        assertEquals("ipv6_enabled", PrefKeys.KEY_IPV6_ENABLED)
        assertEquals("anti_dpi_enabled", PrefKeys.KEY_ANTI_DPI_ENABLED)
        assertEquals("auto_connect", PrefKeys.KEY_AUTO_CONNECT)
        assertEquals("auto_reconnect", PrefKeys.KEY_AUTO_RECONNECT)
        assertEquals("notification_enabled", PrefKeys.KEY_NOTIFICATION_ENABLED)
        assertEquals("kill_switch", PrefKeys.KEY_KILL_SWITCH)
        assertEquals("carrier_opt", PrefKeys.KEY_CARRIER_OPT)
        assertEquals("split_tunnel_enabled", PrefKeys.KEY_SPLIT_TUNNEL_ENABLED)
        assertEquals("split_tunnel_mode", PrefKeys.KEY_SPLIT_TUNNEL_MODE)
        assertEquals("split_tunnel_apps", PrefKeys.KEY_SPLIT_TUNNEL_APPS)
        assertEquals("custom_dns_list", PrefKeys.KEY_CUSTOM_DNS_LIST)
        assertEquals("bypass_packages", PrefKeys.KEY_BYPASS_PACKAGES)
    }

    @Test
    fun `writing KEY_DOH_ENABLED via ViewModel reflects identically in SharedPreferences for VpnService`() {
        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

        viewModel.toggleDoh(true)
        assertTrue(viewModel.settings.value.isDohEnabled)
        val readFromPrefsTrue = prefs.getBoolean(PrefKeys.KEY_DOH_ENABLED, false)
        assertEquals(true, readFromPrefsTrue)

        viewModel.toggleDoh(false)
        assertFalse(viewModel.settings.value.isDohEnabled)
        val readFromPrefsFalse = prefs.getBoolean(PrefKeys.KEY_DOH_ENABLED, true)
        assertEquals(false, readFromPrefsFalse)
    }

    @Test
    fun `writing KEY_IPV6_ENABLED via ViewModel reflects identically in SharedPreferences for VpnService`() {
        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

        viewModel.toggleIpv6(true)
        assertTrue(viewModel.settings.value.isIpv6Enabled)
        val readFromPrefsTrue = prefs.getBoolean(PrefKeys.KEY_IPV6_ENABLED, false)
        assertEquals(true, readFromPrefsTrue)

        viewModel.toggleIpv6(false)
        assertFalse(viewModel.settings.value.isIpv6Enabled)
        val readFromPrefsFalse = prefs.getBoolean(PrefKeys.KEY_IPV6_ENABLED, true)
        assertEquals(false, readFromPrefsFalse)
    }

    @Test
    fun `writing KEY_KILL_SWITCH via ViewModel reflects identically in SharedPreferences for VpnService`() {
        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

        assertFalse(viewModel.settings.value.isKillSwitchEnabled)
        viewModel.toggleKillSwitch(true)
        assertTrue(viewModel.settings.value.isKillSwitchEnabled)
        val readFromPrefsTrue = prefs.getBoolean(PrefKeys.KEY_KILL_SWITCH, false)
        assertEquals(true, readFromPrefsTrue)

        viewModel.toggleKillSwitch(false)
        assertFalse(viewModel.settings.value.isKillSwitchEnabled)
        val readFromPrefsFalse = prefs.getBoolean(PrefKeys.KEY_KILL_SWITCH, true)
        assertEquals(false, readFromPrefsFalse)
    }

    @Test
    fun `writing KEY_CARRIER_OPT via ViewModel reflects identically in SharedPreferences for VpnService`() {
        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

        viewModel.setCarrierOpt("mci")
        assertEquals("mci", viewModel.settings.value.carrierOpt)
        val readMci = prefs.getString(PrefKeys.KEY_CARRIER_OPT, null)
        assertEquals("mci", readMci)

        viewModel.setCarrierOpt("mtn")
        assertEquals("mtn", viewModel.settings.value.carrierOpt)
        val readMtn = prefs.getString(PrefKeys.KEY_CARRIER_OPT, null)
        assertEquals("mtn", readMtn)

        viewModel.setCarrierOpt("wifi")
        assertEquals("wifi", viewModel.settings.value.carrierOpt)
        val readWifi = prefs.getString(PrefKeys.KEY_CARRIER_OPT, null)
        assertEquals("wifi", readWifi)
    }

    @Test
    fun `writing split tunnel configuration writes exact keys read by DnsVpnService`() {
        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val testApps = setOf("com.supercell.clashofclans", "com.dts.freefireth")

        viewModel.setSplitTunnel(enabled = true, mode = "allowed", apps = testApps)

        // Assert ViewModel state
        assertTrue(viewModel.settings.value.isSplitTunnelEnabled)
        assertEquals("allowed", viewModel.settings.value.splitTunnelMode)
        assertEquals(testApps, viewModel.bypassPackages.value)

        // Read keys exactly as DnsVpnService does
        val readEnabled = prefs.getBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, false)
        val readMode = prefs.getString(PrefKeys.KEY_SPLIT_TUNNEL_MODE, null)
        val readAppsStr = prefs.getString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, "") ?: ""

        assertEquals(true, readEnabled)
        assertEquals("allowed", readMode)
        assertTrue(readAppsStr.contains("com.supercell.clashofclans"))
        assertTrue(readAppsStr.contains("com.dts.freefireth"))

        // Disable split tunnel
        viewModel.setSplitTunnel(enabled = false, mode = "disallowed", apps = emptySet())
        assertEquals(false, prefs.getBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, true))
        assertEquals("disallowed", prefs.getString(PrefKeys.KEY_SPLIT_TUNNEL_MODE, null))
        assertEquals("", prefs.getString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, null))
    }

    @Test
    fun `mapping test for KEY_BYPASS_PACKAGES to KEY_SPLIT_TUNNEL_APPS backward compatibility`() {
        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

        // Simulate legacy storage containing ONLY KEY_BYPASS_PACKAGES as JSON array
        val legacyPackages = JSONArray().apply {
            put("com.example.legacy1")
            put("com.example.legacy2")
        }
        prefs.edit()
            .putString(PrefKeys.KEY_BYPASS_PACKAGES, legacyPackages.toString())
            .remove(PrefKeys.KEY_SPLIT_TUNNEL_APPS)
            .commit()

        // Create a new ViewModel instance to simulate cold load of persisted legacy data
        val newViewModel = DnsViewModel(app)
        val loadedPackages = newViewModel.bypassPackages.value
        assertEquals(2, loadedPackages.size)
        assertTrue(loadedPackages.contains("com.example.legacy1"))
        assertTrue(loadedPackages.contains("com.example.legacy2"))

        // Test updating writes to both KEY_SPLIT_TUNNEL_APPS and KEY_BYPASS_PACKAGES
        val updatedSet = setOf("com.example.migrated.app")
        newViewModel.setSplitTunnel(enabled = true, mode = "disallowed", apps = updatedSet)

        val splitAppsStr = prefs.getString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, "") ?: ""
        val bypassJson = prefs.getString(PrefKeys.KEY_BYPASS_PACKAGES, "") ?: ""

        assertEquals("com.example.migrated.app", splitAppsStr)
        assertTrue(bypassJson.contains("com.example.migrated.app"))
    }

    @Test
    fun `auto connect writes both KEY_AUTO_CONNECT and KEY_AUTO_RECONNECT`() {
        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

        viewModel.toggleAutoConnect(true)
        assertTrue(viewModel.settings.value.isAutoConnectEnabled)
        assertTrue(viewModel.settings.value.isAutoReconnect)
        assertEquals(true, prefs.getBoolean(PrefKeys.KEY_AUTO_CONNECT, false))
        assertEquals(true, prefs.getBoolean(PrefKeys.KEY_AUTO_RECONNECT, false))

        viewModel.toggleAutoConnect(false)
        assertFalse(viewModel.settings.value.isAutoConnectEnabled)
        assertFalse(viewModel.settings.value.isAutoReconnect)
        assertEquals(false, prefs.getBoolean(PrefKeys.KEY_AUTO_CONNECT, true))
        assertEquals(false, prefs.getBoolean(PrefKeys.KEY_AUTO_RECONNECT, true))
    }

    @Test
    fun `DoH endpoint resolution handles primary secondary and known DNS IPs`() {
        assertEquals("https://cloudflare-dns.com/dns-query", DnsVpnService.resolveDohEndpointUrl("1.1.1.1"))
        assertEquals("https://cloudflare-dns.com/dns-query", DnsVpnService.resolveDohEndpointUrl("1.0.0.1"))
        assertEquals("https://dns.google/dns-query", DnsVpnService.resolveDohEndpointUrl("8.8.8.8"))
        assertEquals("https://dns.google/dns-query", DnsVpnService.resolveDohEndpointUrl("8.8.4.4"))
        assertEquals("https://free.shecan.ir/dns-query", DnsVpnService.resolveDohEndpointUrl("178.22.122.100"))
        assertEquals("https://free.shecan.ir/dns-query", DnsVpnService.resolveDohEndpointUrl("185.51.200.2"))
        assertEquals("https://dns.electro.ir/dns-query", DnsVpnService.resolveDohEndpointUrl("78.157.42.100"))
        assertEquals("https://dns.radar.game/dns-query", DnsVpnService.resolveDohEndpointUrl("10.201.201.201"))
        assertEquals("https://dns.403.online/dns-query", DnsVpnService.resolveDohEndpointUrl("10.202.10.202"))
        assertEquals("https://doh.opendns.com/dns-query", DnsVpnService.resolveDohEndpointUrl("208.67.222.222"))
    }

    @Test
    fun `DoH endpoint resolution returns null for unsupported and custom IPs`() {
        // Unknown/private IPs must NOT produce a guessed IP-literal URL that would
        // always fail TLS hostname verification — callers fall back to plain DNS.
        assertNull(DnsVpnService.resolveDohEndpointUrl("4.2.2.4"))
        assertNull(DnsVpnService.resolveDohEndpointUrl("192.168.1.1"))
        assertNull(DnsVpnService.resolveDohEndpointUrl(""))
        assertNull(DnsVpnService.resolveDohEndpointUrl("not-an-ip"))
    }
}
