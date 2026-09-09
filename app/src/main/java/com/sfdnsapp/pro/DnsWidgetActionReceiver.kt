package com.sfdnsapp.pro

/**
 * Broadcast receiver for widget toggle actions.
 * Fixed: Migrated to unified PrefKeys and safe service stop handling.
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sfdnsapp.pro.data.DnsRepository
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
        // Synced directly with DnsRepository.defaultServers to guarantee consistent IPs and DoH mappings
        val DNS_CANDIDATES: List<WidgetDnsCandidate> = DnsRepository.defaultServers.map { server ->
            WidgetDnsCandidate(
                name = server.name,
                primary = server.primary,
                secondary = server.secondary,
                ipv6Primary = server.primaryV6,
                ipv6Secondary = server.secondaryV6
            )
        }

        private fun pingIp(ip: String, timeoutMs: Int = 500): Long {
            val start = System.nanoTime()
            return try {
                // use{}: a failed connect() must not leak the socket fd.
                Socket().use { socket ->
                    DnsVpnService.protectSocket(socket)
                    socket.connect(InetSocketAddress(ip, 53), timeoutMs)
                }
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
                // Use goAsync to allow background execution before starting VPN
                val pendingResult = goAsync()
                Thread {
                    try {
                        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
                        val smartAutoSelect = prefs.getSafeBoolean(PrefKeys.KEY_WIDGET_AUTO_SELECT, false)

                        val chosenName: String
                        val chosenPrimary: String
                        val chosenSecondary: String
                        val chosenPrimaryIpv6: String
                        val chosenSecondaryIpv6: String

                        if (smartAutoSelect) {
                            // Optional Smart Widget Auto-Select: Test fastest server and notify user
                            var bestCandidate: WidgetDnsCandidate? = null
                            var bestPing = 9999L

                            val executor = Executors.newFixedThreadPool(DNS_CANDIDATES.size.coerceAtLeast(1))
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

                                val lang = prefs.getSafeString(PrefKeys.KEY_LANGUAGE, "fa")
                                val toastMsg = if (lang == "fa") "DNS شما به $chosenName تغییر یافت" else "DNS switched to $chosenName"
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context.applicationContext, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                chosenName = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "Cloudflare")
                                chosenPrimary = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS, "1.1.1.1")
                                chosenSecondary = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS, "1.0.0.1")
                                chosenPrimaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6, "")
                                chosenSecondaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6, "")
                            }
                        } else {
                            // Default: Use user's last selected DNS configuration (like DnsTileService)
                            chosenName = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "Cloudflare")
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

