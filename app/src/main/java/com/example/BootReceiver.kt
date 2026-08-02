package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("sfdns_prefs", Context.MODE_PRIVATE)
            val autoConnect = prefs.getSafeBoolean("auto_connect", false)
            if (autoConnect) {
                val name = prefs.getSafeString("last_dns_name", "Cloudflare")
                val primary = prefs.getSafeString("last_primary_dns", "1.1.1.1")
                val secondary = prefs.getSafeString("last_secondary_dns", "1.0.0.1")
                val primaryIpv6 = prefs.getSafeString("last_primary_dns_ipv6", "")
                val secondaryIpv6 = prefs.getSafeString("last_secondary_dns_ipv6", "")

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
