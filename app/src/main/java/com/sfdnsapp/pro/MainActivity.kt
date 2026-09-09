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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sfdnsapp.pro.ui.screens.MainScreen
import com.sfdnsapp.pro.ui.theme.MyApplicationTheme
import com.sfdnsapp.pro.viewmodel.DnsViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AUTO_START = "extra_auto_start"
    }

    private val viewModel: DnsViewModel by viewModels()
    private var isVpnStatusReceiverRegistered = false

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Log.w("MainActivity", "VPN prepare rejected by user: ${result.resultCode}")
            viewModel.setConnectionStatus("disconnected")
            val isPersian = viewModel.settings.value.language != "en"
            val errorMsg = if (isPersian) {
                "برای اتصال DNS نیاز به تایید مجوز VPN است. لطفاً دوباره تلاش کنید و اجازه دسترسی VPN را تایید نمایید."
            } else {
                "VPN permission is required to connect DNS. Please try again and accept the VPN permission prompt."
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
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

        // Handle tap-to-reconnect from the boot notification (Android 12+)
        handleAutoStart(intent)

        // Request notification permission on Android 13+
        requestNotificationPermission()

        // Apply server/setting changes immediately while connected: re-establish
        // the tunnel with the latest configuration (debounced for rapid toggles).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.restartRequests
                    .debounce(400)
                    .collect {
                        if (DnsVpnService.isRunning) {
                            Log.i("MainActivity", "Re-establishing VPN to apply updated configuration")
                            startVpnService()
                        }
                    }
            }
        }

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

    /**
     * Fixed: onResume preserves "connecting" state and doesn't incorrectly reset to "disconnected".
     */
    override fun onResume() {
        super.onResume()
        // Tile/Widget may have switched the active DNS while we were away.
        viewModel.syncSelectedDnsWithPrefs()
        val currentVmState = viewModel.connectionState.value
        if (DnsVpnService.isRunning) {
            viewModel.setConnectionStatus("connected")
        } else {
            // Ensure UI is not stuck in connecting if VPN permission was dismissed or service failed
            val needsVpnPrepare = VpnService.prepare(this) != null
            if (needsVpnPrepare || currentVmState == "connecting") {
                viewModel.setConnectionStatus("disconnected")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent.data)
        handleAutoStart(intent)
    }

    private fun handleAutoStart(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_AUTO_START, false) != true) return
        intent.removeExtra(EXTRA_AUTO_START)
        val prefs = getSharedPreferences(PrefKeys.PREFS_NAME, MODE_PRIVATE)
        val autoConnect = prefs.getSafeBoolean(PrefKeys.KEY_AUTO_CONNECT, true)
        if (autoConnect && !DnsVpnService.isRunning && viewModel.connectionState.value != "connecting") {
            Log.i("MainActivity", "Auto-starting VPN from boot notification tap")
            prepareAndStartVpn()
        }
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
                val success = viewModel.addCustomDns(name, primary, secondary, primaryV6, secondaryV6)
                if (success) {
                    Toast.makeText(this, "دی‌ان‌اس وارد شد: $name", Toast.LENGTH_SHORT).show()
                } else {
                    val isPersian = viewModel.settings.value.language != "en"
                    val err = if (isPersian) {
                        "آدرس DNS داخل لینک معتبر نیست"
                    } else {
                        "Invalid DNS address in link"
                    }
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show()
                }
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

    /**
     * Fixed: Safe stop handling on Android 12+ catching BackgroundServiceStartNotAllowedException
     * and providing fallback directly through companion instance.
     */
    private fun stopVpnService() {
        try {
            val intent = Intent(this, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_STOP
            }
            startService(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to stop VPN via startService, requesting stop directly", e)
            DnsVpnService.requestStop()
        }
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
