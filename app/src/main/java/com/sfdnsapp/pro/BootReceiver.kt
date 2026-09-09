package com.sfdnsapp.pro

/**
 * Boot receiver to start DNS VPN service on device startup if auto_connect is enabled.
 * Fixed: Migrated to unified PrefKeys.
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
            val autoConnect = prefs.getSafeBoolean(PrefKeys.KEY_AUTO_CONNECT, false)
            if (autoConnect) {
                val name = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "Cloudflare")
                val primary = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS, "1.1.1.1")
                val secondary = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS, "1.0.0.1")
                val primaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6, "")
                val secondaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6, "")

                val serviceIntent = Intent(context, DnsVpnService::class.java).apply {
                    action = DnsVpnService.ACTION_START
                    putExtra(DnsVpnService.EXTRA_DNS_NAME, name)
                    putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, primary)
                    putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, secondary)
                    putExtra("primary_dns_ipv6", primaryIpv6)
                    putExtra("secondary_dns_ipv6", secondaryIpv6)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BootReceiver", "Failed to auto-start DNS service on boot", e)
                }
            }
        }
    }
}
