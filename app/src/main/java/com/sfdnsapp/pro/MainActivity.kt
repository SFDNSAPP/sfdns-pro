package com.sfdnsapp.pro

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sfdnsapp.pro.ui.screens.MainScreen
import com.sfdnsapp.pro.ui.theme.MyApplicationTheme
import com.sfdnsapp.pro.viewmodel.DnsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DnsViewModel by viewModels()
    private var isVpnStatusReceiverRegistered = false

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Log.w("MainActivity", "VPN prepare launcher canceled or failed: ${result.resultCode}")
            // Fallback attempt
            try {
                startVpnService()
            } catch (e: Exception) {
                viewModel.setConnectionStatus("disconnected")
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.i("MainActivity", "Notification permission granted: $isGranted")
    }

    private val vpnStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status") ?: "disconnected"
            viewModel.setConnectionStatus(status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Deep Link if present
        handleDeepLink(intent?.data)

        // Request notification permission on Android 13+
        requestNotificationPermission()

        setContent {
            MyApplicationTheme {
                MainScreen(
                    viewModel = viewModel,
                    onToggleConnect = { toggleVpnConnection() }
                )
            }
        }

        // Register VPN status broadcast receiver
        if (!isVpnStatusReceiverRegistered) {
            val filter = IntentFilter("$packageName.VPN_STATUS")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(vpnStatusReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(vpnStatusReceiver, filter)
            }
            isVpnStatusReceiverRegistered = true
        }
    }

    override fun onResume() {
        super.onResume()
        val currentStatus = if (DnsVpnService.isRunning) "connected" else "disconnected"
        viewModel.setConnectionStatus(currentStatus)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent.data)
    }

    private fun handleDeepLink(uri: Uri?) {
        if (uri == null || uri.scheme != "sfdns") return
        try {
            val name = uri.getQueryParameter("name") ?: uri.getQueryParameter("title") ?: "Imported DNS"
            val primary = uri.getQueryParameter("primary") ?: uri.getQueryParameter("p") ?: ""
            val secondary = uri.getQueryParameter("secondary") ?: uri.getQueryParameter("s") ?: ""
            val primaryV6 = uri.getQueryParameter("pv6") ?: ""
            val secondaryV6 = uri.getQueryParameter("sv6") ?: ""

            if (primary.isNotBlank()) {
                viewModel.addCustomDns(name, primary, secondary, primaryV6, secondaryV6)
                Toast.makeText(this, "دی‌ان‌اس وارد شد: $name", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling deep link", e)
        }
    }

    private fun toggleVpnConnection() {
        if (DnsVpnService.isRunning || viewModel.connectionState.value == "connected") {
            stopVpnService()
        } else {
            prepareAndStartVpn()
        }
    }

    private fun prepareAndStartVpn() {
        viewModel.setConnectionStatus("connecting")
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val selected = viewModel.selectedDns.value
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
            putExtra(DnsVpnService.EXTRA_DNS_NAME, selected.name)
            putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, selected.primary)
            putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, selected.secondary)
            putExtra("primary_dns_ipv6", selected.primaryV6)
            putExtra("secondary_dns_ipv6", selected.secondaryV6)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVpnService() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP
        }
        startService(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isVpnStatusReceiverRegistered) {
            try {
                unregisterReceiver(vpnStatusReceiver)
            } catch (e: Exception) {
                // Ignore
            }
            isVpnStatusReceiverRegistered = false
        }
    }
}
