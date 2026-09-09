package com.sfdnsapp.pro

/**
 * Broadcast receiver for widget toggle actions.
 * Fixed: Migrated to unified PrefKeys and safe service stop handling.
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DnsWidgetActionReceiver : BroadcastReceiver() {

    data class WidgetDnsCandidate(
        val name: String,
        val primary: String,
        val secondary: String,
        val ipv6Primary: String = "",
        val ipv6Secondary: String = ""
    )

    companion object {
        private val DNS_CANDIDATES = listOf(
            WidgetDnsCandidate("Electro (Gaming)", "78.157.42.100", "78.157.42.101"),
            WidgetDnsCandidate("Shecan (Bypass)", "185.51.200.2", "178.22.122.100"),
            WidgetDnsCandidate("Radar Game (Gaming)", "10.202.10.11", "10.202.10.10"),
            WidgetDnsCandidate("PUBG Fast 2 (Gaming)", "50.3.121.54", "78.157.42.101"),
            WidgetDnsCandidate("403 Online (Bypass)", "10.202.10.202", "10.202.10.102"),
            WidgetDnsCandidate("Cloudflare (Public)", "1.1.1.1", "1.0.0.1"),
            WidgetDnsCandidate("Google (Public)", "8.8.8.8", "8.8.4.4"),
            WidgetDnsCandidate("Shatel (Gaming)", "85.15.1.15", "85.15.1.14")
        )

        private fun pingIp(ip: String, timeoutMs: Int = 500): Long {
            val start = System.nanoTime()
            return try {
                val socket = Socket()
                DnsVpnService.protectSocket(socket)
                socket.connect(InetSocketAddress(ip, 53), timeoutMs)
                socket.close()
                val diff = (System.nanoTime() - start) / 1_000_000
                if (diff > 0) diff else 1L
            } catch (e: Exception) {
                9999L
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DnsWidgetHelper.ACTION_TOGGLE_DNS) {
            val isRunning = DnsVpnService.isRunning

            if (isRunning) {
                // Stop VPN Service safely
                val stopIntent = Intent(context, DnsVpnService::class.java).apply {
                    action = DnsVpnService.ACTION_STOP
                }
                try {
                    context.startService(stopIntent)
                } catch (e: Exception) {
                    DnsVpnService.requestStop()
                }
                DnsWidgetHelper.updateAllWidgets(context)
            } else {
                // Use goAsync to allow quick parallel DNS ping testing before starting VPN
                val pendingResult = goAsync()
                Thread {
                    try {
                        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

                        // Run parallel ping scan on candidates to select the absolute fastest server live
                        var bestCandidate: WidgetDnsCandidate? = null
                        var bestPing = 9999L

                        val executor = Executors.newFixedThreadPool(DNS_CANDIDATES.size)
                        val futures = DNS_CANDIDATES.map { candidate ->
                            executor.submit<Pair<WidgetDnsCandidate, Long>> {
                                val ping = pingIp(candidate.primary)
                                Pair(candidate, ping)
                            }
                        }
                        executor.shutdown()
                        try {
                            executor.awaitTermination(1200, TimeUnit.MILLISECONDS)
                        } catch (e: Exception) {
                            // Timeout
                        }

                        for (future in futures) {
                            try {
                                if (future.isDone) {
                                    val pair = future.get()
                                    if (pair.second < bestPing) {
                                        bestPing = pair.second
                                        bestCandidate = pair.first
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }

                        // Fallback to last saved or default if all candidates timed out
                        val chosenName: String
                        val chosenPrimary: String
                        val chosenSecondary: String
                        val chosenPrimaryIpv6: String
                        val chosenSecondaryIpv6: String

                        if (bestCandidate != null && bestPing < 9999L) {
                            chosenName = bestCandidate.name
                            chosenPrimary = bestCandidate.primary
                            chosenSecondary = bestCandidate.secondary
                            chosenPrimaryIpv6 = bestCandidate.ipv6Primary
                            chosenSecondaryIpv6 = bestCandidate.ipv6Secondary

                            prefs.edit().apply {
                                putString(PrefKeys.KEY_LAST_DNS_NAME, chosenName)
                                putString(PrefKeys.KEY_LAST_PRIMARY_DNS, chosenPrimary)
                                putString(PrefKeys.KEY_LAST_SECONDARY_DNS, chosenSecondary)
                                putString(PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6, chosenPrimaryIpv6)
                                putString(PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6, chosenSecondaryIpv6)
                                putString(PrefKeys.KEY_LAST_DNS_PING, "${bestPing}ms")
                                apply()
                            }
                        } else {
                            chosenName = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "Cloudflare (Public)")
                            chosenPrimary = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS, "1.1.1.1")
                            chosenSecondary = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS, "1.0.0.1")
                            chosenPrimaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6, "")
                            chosenSecondaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6, "")
                        }

                        val splitTunnelEnabled = prefs.getSafeBoolean(PrefKeys.KEY_SPLIT_TUNNEL_ENABLED, false)
                        val splitTunnelMode = prefs.getSafeString(PrefKeys.KEY_SPLIT_TUNNEL_MODE, "disallowed")
                        val splitTunnelApps = prefs.getSafeString(PrefKeys.KEY_SPLIT_TUNNEL_APPS, "")

                        val startIntent = Intent(context, DnsVpnService::class.java).apply {
                            action = DnsVpnService.ACTION_START
                            putExtra(DnsVpnService.EXTRA_DNS_NAME, chosenName)
                            putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, chosenPrimary)
                            putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, chosenSecondary)
                            putExtra("primary_dns_ipv6", chosenPrimaryIpv6)
                            putExtra("secondary_dns_ipv6", chosenSecondaryIpv6)
                            putExtra("split_tunnel_enabled", splitTunnelEnabled)
                            putExtra("split_tunnel_mode", splitTunnelMode)
                            putExtra("split_tunnel_apps", splitTunnelApps)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(startIntent)
                        } else {
                            context.startService(startIntent)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DnsWidgetActionReceiver", "Failed to start service from widget", e)
                    } finally {
                        DnsWidgetHelper.updateAllWidgets(context)
                        pendingResult.finish()
                    }
                }.start()
            }
        }
    }
}

