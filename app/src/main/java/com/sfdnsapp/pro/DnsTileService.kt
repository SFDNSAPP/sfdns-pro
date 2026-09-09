package com.sfdnsapp.pro

/**
 * Quick Settings Tile service for toggling SFDNS PRO.
 * Fixed: Migrated to unified PrefKeys and safe stop handling.
 */
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DnsTileService : TileService() {

    private fun isPersian(): Boolean {
        return try {
            getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .getSafeString(PrefKeys.KEY_LANGUAGE, "fa") != "en"
        } catch (_: Exception) {
            true
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isRunning = DnsVpnService.isRunning
        
        if (isRunning) {
            // Optimistically update UI to disconnected
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isPersian()) "در حال قطع..." else "Disconnecting..."
            }
            tile.updateTile()

            // Stop VPN safely
            val serviceIntent = Intent(this, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_STOP
            }
            try {
                startService(serviceIntent)
            } catch (e: Exception) {
                DnsVpnService.requestStop()
            }
        } else {
            // Optimistically update UI to connecting
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isPersian()) "در حال اتصال..." else "Connecting..."
            }
            tile.updateTile()

            // Start VPN with last saved DNS configuration
            val prefs = getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "Cloudflare")
            val primary = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS, "1.1.1.1")
            val secondary = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS, "1.0.0.1")
            val primaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_PRIMARY_DNS_IPV6, "")
            val secondaryIpv6 = prefs.getSafeString(PrefKeys.KEY_LAST_SECONDARY_DNS_IPV6, "")

            val serviceIntent = Intent(this, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_START
                putExtra(DnsVpnService.EXTRA_DNS_NAME, name)
                putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, primary)
                putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, secondary)
                putExtra("primary_dns_ipv6", primaryIpv6)
                putExtra("secondary_dns_ipv6", secondaryIpv6)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("DnsTileService", "Failed to start service from quick tile", e)
                // Don't leave the tile stuck on "Connecting...".
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (isPersian()) "خطا — برنامه را باز کنید" else "Failed — open app"
                }
                tile.updateTile()
            }
        }
        
        // Request listening refresh to sync UI states when state changes are processed
        requestListeningState(this, ComponentName(this, DnsTileService::class.java))
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = DnsVpnService.isRunning

        // Programmatically set the lightning icon to guarantee correct representation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tile.icon = Icon.createWithResource(this, R.drawable.ic_lightning)
        }

        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            val prefs = getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getSafeString(PrefKeys.KEY_LAST_DNS_NAME, "SFDNS")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = name
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isPersian()) "قطع اتصال" else "Disconnected"
            }
        }
        tile.updateTile()
    }
}
