package com.sfdnsapp.pro

/**
 * Unified SharedPreferences constants for SFDNS PRO.
 * Fixes key desynchronization between DnsViewModel, DnsVpnService, and system components.
 */
object PrefKeys {
    const val PREFS_NAME = "sfdns_prefs"

    // Connection & DNS
    const val KEY_LAST_DNS_NAME = "last_dns_name"
    const val KEY_LAST_PRIMARY_DNS = "last_primary_dns"
    const val KEY_LAST_SECONDARY_DNS = "last_secondary_dns"
    const val KEY_LAST_PRIMARY_DNS_IPV6 = "last_primary_dns_ipv6"
    const val KEY_LAST_SECONDARY_DNS_IPV6 = "last_secondary_dns_ipv6"
    const val KEY_LAST_DNS_PING = "last_dns_ping"
    const val KEY_CUSTOM_DNS_LIST = "custom_dns_list"

    // Feature flags (یکسان برای ViewModel و Service)
    const val KEY_DOH_ENABLED = "doh_enabled"
    const val KEY_IPV6_ENABLED = "ipv6_enabled"
    const val KEY_KILL_SWITCH = "kill_switch"
    const val KEY_ANTI_DPI_ENABLED = "anti_dpi_enabled"
    const val KEY_AUTO_CONNECT = "auto_connect"          // برای BootReceiver
    const val KEY_AUTO_RECONNECT = "auto_reconnect"      // برای UI
    const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    const val KEY_LANGUAGE = "language"
    const val KEY_CARRIER_OPT = "carrier_opt"            // "auto" | "mci" | "mtn" | "wifi"
    const val KEY_WIDGET_AUTO_SELECT = "widget_auto_select" // Smart auto-select fastest server on widget tap

    // Split Tunnel
    const val KEY_SPLIT_TUNNEL_ENABLED = "split_tunnel_enabled"
    const val KEY_SPLIT_TUNNEL_MODE = "split_tunnel_mode" // "allowed" | "disallowed"
    const val KEY_SPLIT_TUNNEL_APPS = "split_tunnel_apps" // کاما جدا شده
    const val KEY_BYPASS_PACKAGES = "bypass_packages"     // برای سازگاری موقت با UI فعلی
}
