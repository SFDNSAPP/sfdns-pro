package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DnsTileService : TileService() {

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
                tile.subtitle = "Disconnecting..."
            }
            tile.updateTile()

            // Stop VPN
            val serviceIntent = Intent(this, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_STOP
            }
            startService(serviceIntent)
        } else {
            // Optimistically update UI to connecting
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Connecting..."
            }
            tile.updateTile()

            // Start VPN with last saved DNS configuration
            val prefs = getSharedPreferences("sfdns_prefs", Context.MODE_PRIVATE)
            val name = prefs.getSafeString("last_dns_name", "Cloudflare")
            val primary = prefs.getSafeString("last_primary_dns", "1.1.1.1")
            val secondary = prefs.getSafeString("last_secondary_dns", "1.0.0.1")
            val primaryIpv6 = prefs.getSafeString("last_primary_dns_ipv6", "")
            val secondaryIpv6 = prefs.getSafeString("last_secondary_dns_ipv6", "")

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
            val prefs = getSharedPreferences("sfdns_prefs", Context.MODE_PRIVATE)
            val name = prefs.getSafeString("last_dns_name", "SFDNS")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = name
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Disconnected"
            }
        }
        tile.updateTile()
    }
}
