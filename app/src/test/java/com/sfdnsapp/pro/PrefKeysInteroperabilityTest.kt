package com.sfdnsapp.pro

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sfdnsapp.pro.viewmodel.DnsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `kill switch updates state and persists to SharedPreferences`() {
        assertFalse(viewModel.settings.value.isKillSwitchEnabled)
        viewModel.toggleKillSwitch(true)
        assertTrue(viewModel.settings.value.isKillSwitchEnabled)

        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefKeys.KEY_KILL_SWITCH, false))

        viewModel.toggleKillSwitch(false)
        assertFalse(viewModel.settings.value.isKillSwitchEnabled)
        assertFalse(prefs.getBoolean(PrefKeys.KEY_KILL_SWITCH, true))
    }

    @Test
    fun `carrier optimization updates state and persists to SharedPreferences`() {
        viewModel.setCarrierOpt("mci")
        assertEquals("mci", viewModel.settings.value.carrierOpt)

        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        assertEquals("mci", prefs.getString(PrefKeys.KEY_CARRIER_OPT, null))
    }

    @Test
    fun `split tunnel updates state and persists apps`() {
        val apps = setOf("com.example.game", "com.android.chrome")
        viewModel.setSplitTunnel(enabled = true, mode = "disallowed", apps = apps)

        assertTrue(viewModel.settings.value.isSplitTunnelEnabled)
        assertEquals("disallowed", viewModel.settings.value.splitTunnelMode)
        assertEquals(apps, viewModel.bypassPackages.value)

        val prefs = app.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, false))
        assertEquals("disallowed", prefs.getString(PrefKeys.KEY_SPLIT_TUNNEL_MODE, null))
        val savedApps = prefs.getString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, "") ?: ""
        assertTrue(savedApps.contains("com.example.game"))
        assertTrue(savedApps.contains("com.android.chrome"))
    }

    @Test
    fun `DoH endpoint resolution handles primary secondary and known DNS IPs`() {
        assertEquals("https://1.1.1.1/dns-query", DnsVpnService.resolveDohEndpointUrl("1.1.1.1"))
        assertEquals("https://1.1.1.1/dns-query", DnsVpnService.resolveDohEndpointUrl("1.0.0.1"))
        assertEquals("https://dns.google/dns-query", DnsVpnService.resolveDohEndpointUrl("8.8.8.8"))
        assertEquals("https://dns.google/dns-query", DnsVpnService.resolveDohEndpointUrl("8.8.4.4"))
        assertEquals("https://free.shecan.ir/dns-query", DnsVpnService.resolveDohEndpointUrl("178.22.122.100"))
        assertEquals("https://free.shecan.ir/dns-query", DnsVpnService.resolveDohEndpointUrl("185.51.200.2"))
        assertEquals("https://dns.electro.ir/dns-query", DnsVpnService.resolveDohEndpointUrl("78.157.42.100"))
        assertEquals("https://dns.radar.game/dns-query", DnsVpnService.resolveDohEndpointUrl("10.201.201.201"))
        assertEquals("https://dns.403.online/dns-query", DnsVpnService.resolveDohEndpointUrl("10.202.10.202"))
    }
}
