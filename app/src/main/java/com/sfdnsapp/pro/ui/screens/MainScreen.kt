package com.sfdnsapp.pro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sfdnsapp.pro.data.DnsServer
import com.sfdnsapp.pro.ui.components.ActiveServerCard
import com.sfdnsapp.pro.ui.components.ChangelogModal
import com.sfdnsapp.pro.ui.components.CustomDnsDialog
import com.sfdnsapp.pro.ui.components.CyberConnectButton
import com.sfdnsapp.pro.ui.components.CyberHeader
import com.sfdnsapp.pro.ui.components.GamingHubDialog
import com.sfdnsapp.pro.ui.components.MetricsDashboard
import com.sfdnsapp.pro.ui.components.QuickActionHub
import com.sfdnsapp.pro.ui.components.RadarSpeedTestDialog
import com.sfdnsapp.pro.ui.components.ServerListBottomSheet
import com.sfdnsapp.pro.ui.components.SettingsDialog
import com.sfdnsapp.pro.ui.components.SplitTunnelDialog
import com.sfdnsapp.pro.ui.components.SupportModal
import com.sfdnsapp.pro.ui.components.VipModal
import com.sfdnsapp.pro.ui.theme.CyberBg
import com.sfdnsapp.pro.ui.theme.NeonCyan
import com.sfdnsapp.pro.ui.theme.NeonGreen
import com.sfdnsapp.pro.viewmodel.DnsViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DnsViewModel,
    onToggleConnect: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedDns by viewModel.selectedDns.collectAsState()
    val dnsList by viewModel.dnsList.collectAsState()
    val pingMap by viewModel.pingMap.collectAsState()
    val gamePingMap by viewModel.gamePingMap.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isRadarRunning by viewModel.isRadarRunning.collectAsState()
    val radarProgress by viewModel.radarProgress.collectAsState()
    val radarFastestServer by viewModel.radarFastestServer.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val installedGamePackages by viewModel.installedGamePackages.collectAsState()
    val bypassPackages by viewModel.bypassPackages.collectAsState()

    val isPersian = settings.language == "fa"
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Modal dialog visibility states
    var showServerList by remember { mutableStateOf(false) }
    var showRadar by remember { mutableStateOf(false) }
    var showGamingHub by remember { mutableStateOf(false) }
    var showCustomDns by remember { mutableStateOf(false) }
    var showSplitTunnel by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showVip by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Feedback when a server/setting change triggers an in-place tunnel restart.
    LaunchedEffect(viewModel) {
        viewModel.restartRequests.collect {
            val fa = viewModel.settings.value.language != "en"
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (fa) "⏳ در حال اعمال و اتصال مجدد..." else "⏳ Applying & reconnecting..."
                )
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = CyberBg,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(CyberBg)
            ) {
                // Cyber background elements
                CyberBackgroundCanvas()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    CyberHeader(
                        isPersian = isPersian,
                        onOpenVip = { showVip = true },
                        onOpenChangelog = { showChangelog = true },
                        onOpenSupport = { showSupport = true },
                        onOpenSettings = { showSettings = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Server Selector Card
                    ActiveServerCard(
                        server = selectedDns,
                        livePing = pingMap[selectedDns.id],
                        isPersian = isPersian,
                        onClick = { showServerList = true }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Center Cyber Power Connect Button
                    CyberConnectButton(
                        connectionState = connectionState,
                        isPersian = isPersian,
                        onClick = onToggleConnect
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Metrics Dashboard (Ping, Duration, Download, Upload)
                    MetricsDashboard(
                        metrics = metrics,
                        isPersian = isPersian
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Hub (Radar, Gaming, Bypass, Custom DNS)
                    QuickActionHub(
                        isPersian = isPersian,
                        onOpenRadar = {
                            showRadar = true
                            viewModel.runRadarSpeedTest()
                        },
                        onOpenGaming = {
                            showGamingHub = true
                            viewModel.pingAllGames()
                        },
                        onOpenSplitTunnel = { showSplitTunnel = true },
                        onOpenCustomDns = { showCustomDns = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Modal Dialogs
                if (showServerList) {
                    ServerListBottomSheet(
                        servers = dnsList,
                        selectedServer = selectedDns,
                        pingMap = pingMap,
                        isPersian = isPersian,
                        onSelectServer = { s ->
                            viewModel.selectDns(s)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isPersian) "سرور ${s.faName} انتخاب شد" else "Selected ${s.name}"
                                )
                            }
                        },
                        onPingAll = { viewModel.pingAllServers() },
                        // Close the list first: two stacked bottom sheets break
                        // back-press/scrim behavior and confuse navigation.
                        onOpenAddCustom = { showServerList = false; showCustomDns = true },
                        onDeleteCustom = { id -> viewModel.deleteCustomDns(id) },
                        onDismiss = { showServerList = false }
                    )
                }

                if (showRadar) {
                    RadarSpeedTestDialog(
                        isScanning = isRadarRunning,
                        progress = radarProgress,
                        fastestServer = radarFastestServer,
                        servers = dnsList,
                        pingMap = pingMap,
                        isPersian = isPersian,
                        onStartScan = { viewModel.runRadarSpeedTest() },
                        onConnectFastest = { s ->
                            viewModel.selectDns(s)
                            if (connectionState != "connected") {
                                onToggleConnect()
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isPersian) "متصل به سریع‌ترین سرور: ${s.faName} 🚀" else "Connected to fastest server: ${s.name} 🚀"
                                )
                            }
                        },
                        onDismiss = { showRadar = false }
                    )
                }

                if (showGamingHub) {
                    GamingHubDialog(
                        gamePingMap = gamePingMap,
                        installedPackages = installedGamePackages,
                        isPersian = isPersian,
                        onPingAllGames = { viewModel.pingAllGames() },
                        onDismiss = { showGamingHub = false }
                    )
                }

                if (showCustomDns) {
                    CustomDnsDialog(
                        isPersian = isPersian,
                        onSave = { name, p, s, pv6, sv6 ->
                            val success = viewModel.addCustomDns(name, p, s, pv6, sv6)
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isPersian) "دی‌ان‌اس اختصاصی با موفقیت افزوده شد" else "Custom DNS added successfully"
                                    )
                                }
                            }
                        },
                        onDismiss = { showCustomDns = false }
                    )
                }

                if (showSplitTunnel) {
                    SplitTunnelDialog(
                        installedApps = installedApps,
                        bypassPackages = bypassPackages,
                        splitMode = settings.splitTunnelMode,
                        isPersian = isPersian,
                        onToggleApp = { pkg -> viewModel.toggleBypassPackage(pkg) },
                        onSelectMode = { mode ->
                            viewModel.setSplitTunnel(bypassPackages.isNotEmpty(), mode, bypassPackages)
                        },
                        onDismiss = { showSplitTunnel = false }
                    )
                }

                if (showSettings) {
                    SettingsDialog(
                        settings = settings,
                        isPersian = isPersian,
                        onToggleDoh = { viewModel.toggleDoh(it) },
                        onToggleIpv6 = { viewModel.toggleIpv6(it) },
                        onToggleAntiDpi = { viewModel.toggleAntiDpi(it) },
                        onToggleAutoConnect = { viewModel.toggleAutoConnect(it) },
                        onToggleKillSwitch = { viewModel.toggleKillSwitch(it) },
                        onToggleWidgetAutoSelect = { viewModel.toggleWidgetAutoSelect(it) },
                        onSelectCarrier = { viewModel.setCarrierOpt(it) },
                        onSelectLanguage = { viewModel.updateLanguage(it) },
                        onDismiss = { showSettings = false }
                    )
                }

                if (showVip) {
                    VipModal(isPersian = isPersian, onDismiss = { showVip = false })
                }

                if (showSupport) {
                    SupportModal(isPersian = isPersian, onDismiss = { showSupport = false })
                }

                if (showChangelog) {
                    ChangelogModal(isPersian = isPersian, onDismiss = { showChangelog = false })
                }
            }
        }
    }
}

@Composable
private fun CyberBackgroundCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "bgAnim")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgGlow"
    )

    val particleOffset1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )

    val particleOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Top Neon Aurora / Cyan Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.14f * alphaAnim),
                    NeonGreen.copy(alpha = 0.05f * alphaAnim),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, 0f),
                radius = width * 0.95f
            )
        )

        // Bottom faint violet glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.04f * alphaAnim),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, height),
                radius = width * 0.8f
            )
        )

        // Subtle tech grid lines
        val step = 42.dp.toPx()
        var x = 0f
        while (x < width) {
            drawLine(
                color = Color(0xFF161E30).copy(alpha = 0.35f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 0.8f
            )
            x += step
        }

        var y = 0f
        while (y < height) {
            drawLine(
                color = Color(0xFF161E30).copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.8f
            )
            y += step
        }

        // Floating cyber bubbles / glowing particles
        val bubbles = listOf(
            Triple(0.18f, particleOffset1, 5.dp.toPx()),
            Triple(0.35f, (particleOffset1 + 0.5f) % 1f, 3.5.dp.toPx()),
            Triple(0.72f, particleOffset2, 6.dp.toPx()),
            Triple(0.85f, (particleOffset2 + 0.4f) % 1f, 4.dp.toPx()),
            Triple(0.52f, (particleOffset1 + 0.25f) % 1f, 4.5.dp.toPx())
        )

        for ((posX, progress, radius) in bubbles) {
            val curY = height * progress
            val alpha = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0.1f, 0.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = alpha * 0.4f), Color.Transparent),
                    center = Offset(width * posX, curY),
                    radius = radius * 2.5f
                ),
                radius = radius * 2.5f,
                center = Offset(width * posX, curY)
            )
            drawCircle(
                color = NeonCyan.copy(alpha = alpha * 0.8f),
                radius = radius * 0.5f,
                center = Offset(width * posX, curY)
            )
        }
    }
}
