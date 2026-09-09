package com.sfdnsapp.pro

/**
 * Localized strings for background notifications and foreground service.
 */
object NotificationStrings {

    fun getTitle(lang: String): String {
        return if (lang.equals("en", ignoreCase = true)) {
            "⚡ SFDNS Pro - Active"
        } else {
            "⚡ SFDNS Pro - تحریم‌شکن فعال"
        }
    }

    fun getContent(lang: String, dnsName: String, speedInfo: String): String {
        return if (lang.equals("en", ignoreCase = true)) {
            "Server: $dnsName  |  $speedInfo"
        } else {
            "سرور: $dnsName  |  $speedInfo"
        }
    }

    fun getDisconnectButton(lang: String): String {
        return if (lang.equals("en", ignoreCase = true)) {
            "🔴 Disconnect"
        } else {
            "🔴 قطع اتصال (Disconnect)"
        }
    }
}
