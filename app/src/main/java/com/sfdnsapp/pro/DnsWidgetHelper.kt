package com.sfdnsapp.pro

/**
 * Helper utility for updating SFDNS widgets.
 * Fixed: Migrated to unified PrefKeys and removed hardcoded ping fallback.
 */
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

object DnsWidgetHelper {

    const val ACTION_TOGGLE_DNS = "com.sfdnsapp.pro.ACTION_TOGGLE_DNS"

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        // Update Quick Widget
        val quickComponent = ComponentName(context, DnsQuickWidgetProvider::class.java)
        val quickIds = appWidgetManager.getAppWidgetIds(quickComponent)
        if (quickIds.isNotEmpty()) {
            updateQuickWidgets(context, appWidgetManager, quickIds)
        }

        // Update Detail Widget
        val detailComponent = ComponentName(context, DnsDetailWidgetProvider::class.java)
        val detailIds = appWidgetManager.getAppWidgetIds(detailComponent)
        if (detailIds.isNotEmpty()) {
            updateDetailWidgets(context, appWidgetManager, detailIds)
        }
    }

    fun updateQuickWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val isRunning = DnsVpnService.isRunning
        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val fa = prefs.getSafeString(PrefKeys.KEY_LANGUAGE, "fa") != "en"
        val dnsName = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "SFDNS Pro")
        val pingVal = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_PING, "")
        val pingDisplay = if (pingVal.isNotEmpty()) "⚡ $pingVal" else "⚡ —"

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_dns_quick)

            // Setup Intent for clicking toggle button or widget
            val toggleIntent = Intent(context, DnsWidgetActionReceiver::class.java).apply {
                action = ACTION_TOGGLE_DNS
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_btn_toggle, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_quick_root, pendingIntent)

            views.setTextViewText(R.id.widget_dns_name, dnsName)

            if (isRunning) {
                views.setTextViewText(R.id.widget_status_text, if (fa) "متصل ($pingDisplay)" else "Connected ($pingDisplay)")
                views.setTextColor(R.id.widget_status_text, 0xFF10B981.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.bg_widget_badge_on)
                views.setTextViewText(R.id.widget_btn_toggle, if (fa) "قطع" else "Stop")
                views.setInt(R.id.widget_btn_toggle, "setBackgroundResource", R.drawable.bg_widget_button_connected)
            } else {
                views.setTextViewText(R.id.widget_status_text, if (fa) "قطع اتصال" else "Disconnected")
                views.setTextColor(R.id.widget_status_text, 0xFF94A3B8.toInt())
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.bg_widget_badge_off)
                views.setTextViewText(R.id.widget_btn_toggle, if (fa) "اتصال" else "Connect")
                views.setInt(R.id.widget_btn_toggle, "setBackgroundResource", R.drawable.bg_widget_button)
            }

            appWidgetManager.updateAppWidget(id, views)
        }
    }

    fun updateDetailWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val isRunning = DnsVpnService.isRunning
        val prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val fa = prefs.getSafeString(PrefKeys.KEY_LANGUAGE, "fa") != "en"
        val dnsName = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "SFDNS Pro")
        val primaryDns = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS, "178.22.122.100")
        val secondaryDns = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS, "185.51.200.2")
        val pingVal = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_PING, "")
        val pingDisplay = if (pingVal.isNotEmpty()) "⚡ $pingVal" else "⚡ —"

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_dns_detail)

            // Setup Intent for clicking toggle button
            val toggleIntent = Intent(context, DnsWidgetActionReceiver::class.java).apply {
                action = ACTION_TOGGLE_DNS
            }
            val pendingToggleIntent = PendingIntent.getBroadcast(
                context, 1, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Setup Intent for clicking root (open app)
            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingOpenAppIntent = PendingIntent.getActivity(
                context, 2, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_detail_btn_toggle, pendingToggleIntent)
            views.setOnClickPendingIntent(R.id.widget_detail_root, pendingOpenAppIntent)

            views.setTextViewText(R.id.widget_detail_dns_name, if (fa) "سرور: $dnsName" else "Server: $dnsName")
            views.setTextViewText(R.id.widget_detail_dns_ip, "IP: $primaryDns | $secondaryDns")

            if (isRunning) {
                views.setTextViewText(R.id.widget_detail_status_text, if (fa) "فعال ($pingDisplay)" else "Active ($pingDisplay)")
                views.setTextColor(R.id.widget_detail_status_text, 0xFF10B981.toInt())
                views.setImageViewResource(R.id.widget_detail_status_dot, R.drawable.bg_widget_badge_on)
                views.setTextViewText(R.id.widget_detail_btn_toggle, if (fa) "🔴 قطع اتصال سریع دی‌ان‌اس" else "🔴 Quick Stop DNS")
                views.setInt(R.id.widget_detail_btn_toggle, "setBackgroundResource", R.drawable.bg_widget_button_connected)
            } else {
                views.setTextViewText(R.id.widget_detail_status_text, if (fa) "غیرفعال" else "Inactive")
                views.setTextColor(R.id.widget_detail_status_text, 0xFFEF4444.toInt())
                views.setImageViewResource(R.id.widget_detail_status_dot, R.drawable.bg_widget_badge_off)
                views.setTextViewText(R.id.widget_detail_btn_toggle, if (fa) "⚡ اتصال سریع دی‌ان‌اس" else "⚡ Quick Connect DNS")
                views.setInt(R.id.widget_detail_btn_toggle, "setBackgroundResource", R.drawable.bg_widget_button)
            }

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
