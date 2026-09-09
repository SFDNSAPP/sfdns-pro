package com.sfdnsapp.pro

/**
 * Boot receiver to restore the DNS VPN after device startup when auto-connect is enabled.
 * On Android 12+ a foreground service can no longer be started directly from a
 * background broadcast, so we fall back to a tap-to-reconnect notification which
 * auto-starts the tunnel when the user taps it (a foreground, exempt context).
 */
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val autoConnect = prefs.getSafeBoolean(PrefKeys.KEY_AUTO_CONNECT, true)
        if (!autoConnect) return

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
            android.util.Log.w("BootReceiver", "Direct boot start blocked, showing reconnect notification", e)
            showReconnectNotification(context)
        }
    }

    private fun showReconnectNotification(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val channelId = "sfdns_boot_channel"
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "SFDNS Reconnect",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }

            val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
            val fa = prefs.getSafeString(PrefKeys.KEY_LANGUAGE, "fa") != "en"

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_AUTO_START, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val tapPendingIntent = PendingIntent.getActivity(
                context, 100, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_lightning)
                .setContentTitle(if (fa) "اتصال SFDNS قطع شد" else "SFDNS disconnected")
                .setContentText(
                    if (fa) "برای برقراری مجدد اتصال DNS ضربه بزنید"
                    else "Tap to restore your DNS connection"
                )
                .setContentIntent(tapPendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(100, notification)
        } catch (e: Exception) {
            android.util.Log.e("BootReceiver", "Failed to show reconnect notification", e)
        }
    }
}
