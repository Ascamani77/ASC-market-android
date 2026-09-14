package com.asc.markets

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.AppView
import com.asc.markets.data.BiometricAuthManager
import com.asc.markets.ui.screens.*
import com.asc.markets.ui.components.*
import com.asc.markets.ui.ai.AiScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.asc.markets.ui.theme.*
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.asc.markets.data.NetworkConfig
import com.asc.markets.ui.terminal.viewmodels.ChartViewModel
import com.researchcenter.ui.screens.AnalysisOpinionScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import com.trading.app.data.ChartFeedType

class MainActivity : FragmentActivity() {
    var notificationTypeToOpen by mutableStateOf<String?>(null)
    var notificationSymbol by mutableStateOf<String?>(null)
    var notificationIdToDismiss by mutableStateOf<Int?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    persistFcmToken(token)
                    android.util.Log.d("FCM", "Device token: $token")
                }
            }
        }
    }

    private fun persistFcmToken(token: String) {
        applicationContext.getSharedPreferences("asc_prefs", 0)
            .edit().putString("fcm_token", token).apply()
    }

    /**
     * One-time ask to exempt the app from battery Doze so the foreground
     * monitor's socket isn't frozen — that freeze is why alerts could be
     * delayed until the app was reopened.
     */
    private fun maybeRequestBatteryExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val prefs = getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("battery_exempt_asked", false)) return
        prefs.edit().putBoolean("battery_exempt_asked", true).apply()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return

        AlertDialog.Builder(this)
            .setTitle("Background alerts")
            .setMessage("Allow ASC Market to keep listening for EA signals in the background? Battery optimization can freeze the live connection and delay alerts.")
            .setPositiveButton("Allow") { _, _ ->
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.asc.markets.data.SystemLinkMonitor.start(this)
        maybeRequestBatteryExemption()
        notificationTypeToOpen = intent?.getStringExtra("notification_type")
        notificationSymbol = intent?.getStringExtra("notification_symbol")
        notificationIdToDismiss = intent?.getIntExtra("notification_id", -1)?.takeIf { it > 0 }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    persistFcmToken(token)
                    android.util.Log.d("FCM", "Device token: $token")
                }
            }
        }
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("asc_prefs", 0) }
            val themeModeStr by remember { mutableStateOf(prefs.getString("theme_mode", "DARK") ?: "DARK") }
            
            val ascThemeMode = when (themeModeStr) {
                "LIGHT" -> com.asc.markets.ui.theme.AscThemeMode.LIGHT
                "SYSTEM" -> com.asc.markets.ui.theme.AscThemeMode.SYSTEM
                else -> com.asc.markets.ui.theme.AscThemeMode.DARK
            }
            AscTheme(themeMode = ascThemeMode) {
                val activity = this@MainActivity
                var biometricPassed by remember { mutableStateOf(false) }
                var biometricPrompted by remember { mutableStateOf(false) }
                var bootSplashDone by remember { mutableStateOf(false) }
                var lastUnlockedAt by remember { mutableStateOf(System.currentTimeMillis()) }

                fun rearmAuth() {
                    val enabled = BiometricAuthManager.isEnabled(activity)
                    val can = BiometricAuthManager.canAuthenticate(activity)
                    if (!enabled || !can) return
                    val prefs = activity.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
                    val timeoutMins = prefs.getInt("session_timeout_mins", 15)
                    val idleMs = System.currentTimeMillis() - lastUnlockedAt
                    if (idleMs >= timeoutMins * 60_000L) {
                        try {
                            BiometricAuthManager.showPrompt(
                                activity = activity,
                                onSuccess = { lastUnlockedAt = System.currentTimeMillis() },
                                onFallback = { lastUnlockedAt = System.currentTimeMillis() },
                                onError = { lastUnlockedAt = System.currentTimeMillis() }
                            )
                        } catch (_: Exception) {
                            lastUnlockedAt = System.currentTimeMillis()
                        }
                    }
                }

                DisposableEffect(activity) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) rearmAuth()
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(Unit) {
                    // Failsafe first: the gate must open even if the prompt API
                    // throws synchronously (no enrolled biometrics, hw busy,
                    // wrong lifecycle state) — otherwise the startup spinner
                    // hangs forever on a black screen.
                    try {
                        val biometricEnabled = BiometricAuthManager.isEnabled(activity)
                        val canBiometric = BiometricAuthManager.canAuthenticate(activity)
                        if (biometricEnabled && canBiometric && !biometricPassed) {
                            biometricPrompted = true
                            BiometricAuthManager.showPrompt(
                                activity = activity,
                                onSuccess = { biometricPassed = true; lastUnlockedAt = System.currentTimeMillis() },
                                onFallback = { biometricPassed = true; lastUnlockedAt = System.currentTimeMillis() },
                                onError = { biometricPassed = true; lastUnlockedAt = System.currentTimeMillis() }
                            )
                        } else {
                            biometricPassed = true
                            lastUnlockedAt = System.currentTimeMillis()
                        }
                    } catch (_: Exception) {
                        biometricPassed = true
                        lastUnlockedAt = System.currentTimeMillis()
                    }
                }

                LaunchedEffect(Unit) {
                    delay(2200)
                    bootSplashDone = true
                }

                if (!biometricPassed || !bootSplashDone) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(PureBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = 72.dp)
                    }
                } else {

                val viewModel: ForexViewModel = viewModel()
                val chartViewModel: ChartViewModel = viewModel()
                val currentView by viewModel.currentView.collectAsState()
                val isInitializing by viewModel.isInitializing.collectAsState()
                val isRiskAccepted by viewModel.isRiskAccepted.collectAsState()
                val showRiskDisclosure by viewModel.showRiskDisclosure.collectAsState()
                val selectedPairSymbol by remember(viewModel) {
                    viewModel.selectedPair.map { it.symbol }.distinctUntilChanged()
                }.collectAsState(initial = "")
                val chartActiveSymbol by chartViewModel.activeSymbol.collectAsState()
                val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
                val promoteMacro by viewModel.promoteMacroStream.collectAsState()
                val isCommandPaletteOpen by viewModel.isCommandPaletteOpen.collectAsState()
                val linkedOrderFlowSymbol = remember(selectedPairSymbol, chartActiveSymbol) {
                    resolveLinkedOrderFlowSymbol(
                        selectedPairSymbol = selectedPairSymbol,
                        chartSymbol = chartActiveSymbol
                    )
                }

                // Feed fired vigilance alerts into the in-app NOTIFICATIONS inbox
                // (tapping one opens the asset's qualified setup page).
                LaunchedEffect(Unit) {
                    com.asc.markets.logic.VigilanceMonitor.onTriggered = { alert: com.asc.markets.logic.TriggeredAlert ->
                        viewModel.pushInAppNotification(
                            com.asc.markets.data.NotificationModel(
                                id = alert.id,
                                type = "vigilance",
                                msg = "${alert.title} — ${alert.body}",
                                time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(alert.timestamp)),
                                severity = "WARNING",
                                seen = false,
                                symbol = alert.pair,
                                timeframe = "LIVE",
                                targetView = AppView.QUALIFIED_SETUP.name
                            )
                        )
                        viewModel.incrementAlertNotificationCount()
                    }
                }

                // System-notification deep link: tapping a "vigilance" notification
                // must land on the alert page. Driven by activity-level Compose state
                // so both cold start (onCreate) and warm re-open (onNewIntent) work.
                LaunchedEffect(notificationTypeToOpen, notificationIdToDismiss) {
                    notificationIdToDismiss?.let { id ->
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .cancel(id)
                        notificationIdToDismiss = null
                    }
                    when (notificationTypeToOpen) {
                        "vigilance" -> viewModel.navigateTo(AppView.MY_ALERTS)
                        "price_alert" -> viewModel.navigateTo(AppView.STREAM)
                        else -> if (notificationTypeToOpen != null) viewModel.navigateTo(AppView.MY_ALERTS)
                    }
                }

                if (isInitializing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PureBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = 72.dp)
                    }
                } else if (!isRiskAccepted && showRiskDisclosure) {
                    DisclaimerOverlay(onAccept = { viewModel.acceptRisk() })
                } else {
                    // Modal drawer has been replaced by SIDEBAR_PAGE for a full-screen menu experience.
                    // The swipe-to-open gesture is disabled by removing the ModalNavigationDrawer wrapper.

                    // Global back handler for general navigation (Markets, Chat, etc.)
                    // Exclude DASHBOARD (should exit app) and ANALYSIS_OPINION (has its own internal BackHandler)
                    BackHandler(enabled = currentView != AppView.DASHBOARD && currentView != AppView.ANALYSIS_OPINION) {
                        if (currentView == AppView.ASSET_DETAIL) {
                            // Back from an asset's detail page returns to where it was
                            // opened from (MY ALERTS, MARKETS) rather than the dashboard.
                            viewModel.onAssetDetailBack()
                        } else {
                            viewModel.navigateBack()
                        }
                    }

                    val headerVisible by viewModel.isGlobalHeaderVisible.collectAsState(initial = true)

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = PureBlack,
                        bottomBar = {
                            when {
                                currentView == AppView.CHAT || currentView == AppView.ANALYSIS_OPINION -> {
                                    // No bottom bar for chat or when in Analysis & Opinion view.
                                }
                                currentView == AppView.SIMULATION || currentView == AppView.MY_SIMULATION -> {
                                    // Animated bottom bar for Simulation based on header visibility
                                    AnimatedVisibility(
                                        visible = headerVisible,
                                        enter = slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = tween(durationMillis = 300)
                                        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                                        exit = slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(durationMillis = 300)
                                        ) + fadeOut(animationSpec = tween(durationMillis = 300))
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(AppBottomNavHeight),
                                            color = PureBlack,
                                            tonalElevation = 0.dp
                                        ) {
                                            NotchedBottomNav(
                                                currentView = currentView,
                                                onNavigate = { viewModel.navigateTo(it) },
                                                onHomeSelected = {
                                                    viewModel.navigateTo(AppView.DASHBOARD)
                                                    viewModel.setDashboardTab("COMMAND_CENTER")
                                                },
                                                onMenuClick = { viewModel.navigateTo(AppView.SIDEBAR_PAGE) }
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(AppBottomNavHeight),
                                        color = PureBlack,
                                        tonalElevation = 0.dp
                                    ) {
                                        NotchedBottomNav(
                                            currentView = currentView,
                                            onNavigate = { viewModel.navigateTo(it) },
                                            onHomeSelected = {
                                                viewModel.navigateTo(AppView.DASHBOARD)
                                                viewModel.setDashboardTab("COMMAND_CENTER")
                                            },
                                            // Open the sidebar as a full page
                                            onMenuClick = { viewModel.navigateTo(AppView.SIDEBAR_PAGE) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // THE TRICK: Swap header based on state.
                            // The ASC MARKET header collapses in height as the dashboard
                            // list scrolls (quantized to ~5% steps). The content area below
                            // is a regular weight(1f) Box that simply grows, so the list
                            // always extends to the bottom — no translated drawer gaps.
                            val dashCollapse by viewModel.globalHeaderCollapse.collectAsState(initial = 0f)
                            when (currentView) {
                                // Dashboard: the ASC MARKET header slides away as its box
                                // shrinks while the dashboard (pinned tabs + content) grows
                                // into the freed space, so the pinned Home/Signals/AI bar
                                // ends up as the main header. Because the content area is a
                                // real layout slot (weight 1f), the LazyColumn always fills
                                // down to the bottom nav — no black gap while scrolling.
                                AppView.DASHBOARD -> {
                                    val unread: Int by viewModel.unreadCount.collectAsState(initial = 0)
                                    val headerVisible by viewModel.isGlobalHeaderVisible.collectAsState(initial = true)
                                    val effCollapse = if (headerVisible) dashCollapse else 1f
                                    val headerDensity = LocalDensity.current
                                    val headerPx = with(headerDensity) { 72.dp.toPx() }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp * (1f - effCollapse))
                                            .clipToBounds()
                                    ) {
                                        if (headerVisible) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        translationY = -effCollapse * headerPx
                                                    }
                                            ) {
                                                GlobalHeader(
                                                    currentView = currentView,
                                                    onOpenDrawer = { viewModel.navigateTo(AppView.SIDEBAR_PAGE) },
                                                    onSearch = { viewModel.openCommandPalette() },
                                                    onNotifications = { viewModel.navigateTo(AppView.CALENDAR) },
                                                    unreadCount = unread
                                                )
                                            }
                                        }
                                    }
                                }
                                // Let screens that provide their own header render without the global NavHeader
                                AppView.POST_MOVE_AUDIT, AppView.HOME_ALERTS, AppView.ANALYSIS_OPINION, AppView.ANALYSIS_RESULTS, AppView.SIDEBAR_PAGE, AppView.SIMULATION, AppView.MY_SIMULATION, AppView.STREAM, AppView.PAPER_TRADING, AppView.SETTINGS, AppView.ALERTS, AppView.NOTIFICATIONS, AppView.PUSH_SETTINGS, AppView.MY_ALERTS, AppView.WATCHLIST, AppView.SCALPING, AppView.CHART_DISPLAY_SETTINGS, AppView.ASSET_DETAIL, AppView.AUTO_TRADE, AppView.PROFILE, AppView.TRADE, AppView.TRADE_RECONSTRUCTION, AppView.AI_SETTINGS, AppView.CALENDAR, AppView.QUALIFIED_SETUP, AppView.MARKETS -> {
                                    /* Intentionally no header here. The screen provides its own top control bar which should replace the app header. */
                                }
                                else -> {
                                    if (currentView == AppView.CHAT) {
                                        val messages by viewModel.ascChatMessages.collectAsState()
                                        NavHeader(
                                            title = currentView.name.replace("_", " "),
                                            onBack = { viewModel.navigateBack() },
                                            onSearch = { viewModel.openCommandPalette() },
                                            isChatScreen = true,
                                            onDelete = { viewModel.clearAscChatMessages() },
                                            showDelete = messages.isNotEmpty()
                                        )
                                    } else {
                                        NavHeader(
                                            title = currentView.name.replace("_", " "),
                                            onBack = { viewModel.navigateBack() },
                                            onSearch = { viewModel.openCommandPalette() },
                                            isChatScreen = false
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (currentView) {
                                    AppView.DASHBOARD -> DashboardScreen(viewModel)
                                    AppView.MARKETS -> MarketsScreen({ viewModel.selectPair(it) }, viewModel)
                                    AppView.ASSET_DETAIL -> AssetDetailScreen(viewModel)
                                    AppView.ALERTS -> AlertsScreen(viewModel)
                                    AppView.MY_ALERTS -> MyAlertsScreen(
                                        viewModel = viewModel,
                                        onOpenInbox = { viewModel.navigateTo(AppView.NOTIFICATIONS) },
                                        onOpenPushSettings = { viewModel.navigateTo(AppView.PUSH_SETTINGS) }
                                    )
                                    AppView.NOTIFICATIONS -> NotificationsScreen(viewModel)
                                    AppView.PUSH_SETTINGS -> PushSettingsScreen(viewModel)
                                    AppView.BACKTEST -> BacktestScreen(viewModel)
                                    AppView.MULTI_TIMEFRAME -> MultiTimeframeAnalysisScreen()
                                    AppView.LIQUIDITY_HUB -> LiquidityHubScreen()
                                    AppView.TRADE -> TradeLedgerScreen()
                                    AppView.TRADE_DASHBOARD -> TradeDashboardScreen()
                                    AppView.SIMULATION -> SimulationScreen(viewModel)
                                    AppView.MY_SIMULATION -> MySimulationScreen(viewModel)
                                    AppView.ANALYSIS_OPINION -> {
                                        val watchlistItems by viewModel.watchlistItems.collectAsState()
                                        val activeAssets = remember(watchlistItems) {
                                            watchlistItems.map { it.assetName }.toSet()
                                        }
                                        AnalysisOpinionScreen(
                                            onBackToApp = { viewModel.navigateTo(AppView.DASHBOARD) },
                                            activeAssets = activeAssets
                                        )
                                    }
                                    AppView.HOME_ALERTS -> HomeAlertsScreen(viewModel)
                                    AppView.CALENDAR -> CalendarScreen()
                                    AppView.STREAM -> StreamScreen(initialSymbol = notificationSymbol)
                                    AppView.SENTIMENT -> SentimentScreen(viewModel)
                                    AppView.EDUCATION -> EducationScreen()
                                    AppView.ANALYSIS_RESULTS -> AnalysisResultsScreen()
                                    AppView.QUALIFIED_SETUP -> QualifiedSetupScreen(viewModel)
                                    AppView.WATCHLIST -> {
                                        val context = LocalContext.current
                                        WatchlistScreen(
                                            viewModel = viewModel,
                                            onViewChart = { symbol ->
                                                viewModel.selectPairBySymbolNoNavigate(symbol)
                                                // Force Exness (Live) when navigating from Watchlist
                                                val prefs = context.getSharedPreferences(NetworkConfig.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                                                prefs.edit().putString(ChartFeedType.STREAM_PREF_KEY, ChartFeedType.EXNESS.prefValue).apply()
                                                
                                                viewModel.navigateTo(AppView.STREAM)
                                            },
                                            onSetAlert = { symbol ->
                                                viewModel.selectPairBySymbolNoNavigate(symbol)
                                                viewModel.navigateTo(AppView.ALERTS)
                                            },
                                            onDeepDive = { symbol ->
                                                viewModel.selectPairBySymbolNoNavigate(symbol)
                                                viewModel.navigateTo(AppView.QUALIFIED_SETUP)
                                            }
                                        )
                                    }
                                    AppView.DIAGNOSTICS -> DiagnosticsScreen()
                                    AppView.POST_MOVE_AUDIT -> PostMoveAuditScreen()
                                    AppView.DATA_HUB -> DataHubScreen()
                                    AppView.DATA_VAULT -> DataVaultScreen()
                                    AppView.PORTFOLIO_MANAGER -> PortfolioManagerScreen()
                                    AppView.TRADE_RECONSTRUCTION -> TradeReconstructionScreen()
                                    AppView.PROFILE -> ProfileScreen(viewModel)
                                    AppView.MARKET_VIEW -> MarketViewScreen()
                                    AppView.SCALPING -> ScalpingScreen(viewModel)
                                    AppView.CHART_DISPLAY_SETTINGS -> ChartDisplaySettingsScreen(viewModel)
                                    AppView.SETTINGS -> SettingsScreen(viewModel)
                                    AppView.PAPER_TRADING -> PaperTradingScreen(viewModel)
                                    AppView.QUOTES -> QuotesScreen()
                                    AppView.MARKET_STATUS -> MarketStatusScreen()
                                    AppView.CHART_ANALYSIS -> ChartAnalysisScreen()
                                    AppView.SIDEBAR_PAGE -> {
                                        // Render sidebar contents as a full page (replicates modal drawer content)
                                        val unreadAlertNotifications by viewModel.alertNotificationCount.collectAsState(initial = 0)
                                        val activeAlertNodes by com.asc.markets.logic.VigilanceNodeEngine.activeNodeCount.collectAsState(initial = 0)
                                        AscSidebar(
                                            currentView = currentView,
                                            isCollapsed = false,
                                            promoteMacro = promoteMacro,
                                            alertBadgeCount = unreadAlertNotifications + activeAlertNodes,
onViewChange = { view ->
                                            viewModel.navigateFromSidebar(view)
                                        },
                                            onClose = { viewModel.navigateBack() }
                                        )
                                    }
AppView.AI_TERMINAL -> TerminalScreen(viewModel)
                                     AppView.AUTO_TRADE -> AutoTradeScreen(viewModel)
                                     AppView.AI_SETTINGS -> AiSettingsScreen(viewModel)
                                     else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("NODE_ACCESS_RESTRICED: ${currentView.name}", color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }

                    // Command Palette overlay (search)
                    if (isCommandPaletteOpen) {
                        CommandPalette(
                            onDismiss = { viewModel.closeCommandPalette() },
                            onNavigate = { viewModel.navigateTo(it); viewModel.closeCommandPalette() },
                            onSelectAsset = { viewModel.selectPairBySymbol(it) }
                        )
                    }
                    // Safety Gate: Controls opt-in modal
                    val execOptInRequested by viewModel.executionOptInRequested.collectAsState()
                    if (execOptInRequested) {
                        AlertDialog(
                            onDismissRequest = { viewModel.cancelExecutionOptIn() },
                            title = { Text("Controls — Safety Gate", color = Color.White) },
                            text = {
                                Text(
                                    "You are attempting to access trading controls while the app is in Surveillance‑First mode.\n\n" +
                                            "These controls expose sensitive capabilities. Confirm you understand the risks and that your session is authorized to proceed.",
                                    color = Color.LightGray
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { viewModel.confirmExecutionOptIn() }) {
                                    Text("Confirm — Enable Controls", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.cancelExecutionOptIn() }) {
                                    Text("Cancel", color = Color.LightGray)
                                }
                            }
                        )
                    }
                    // Periodic ingestion: poll VigilanceNodeEngine and feed MacroStream with mapped macro events
                    LaunchedEffect(Unit) {
                        while (true) {
                            try {
                                val events = com.asc.markets.logic.VigilanceNodeEngine.toMacroEvents()
                                if (events.isNotEmpty()) {
                                    viewModel.ingestMacroEventsFromSources(events)
                                }
                            } catch (t: Throwable) {
                                android.util.Log.e("ASC", "Error ingesting vigilance nodes: ${t.message}")
                            }
                            kotlinx.coroutines.delay(15_000)
                        }
                    }
                }
                } // end biometric gate
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTypeToOpen = intent.getStringExtra("notification_type")
        notificationSymbol = intent.getStringExtra("notification_symbol")
        notificationIdToDismiss = intent.getIntExtra("notification_id", -1)?.takeIf { it > 0 }
    }

}

private fun resolveLinkedOrderFlowSymbol(
    selectedPairSymbol: String,
    chartSymbol: String
): String {
    val normalizedSelected = normalizeLinkedOrderFlowSymbol(selectedPairSymbol)
    val normalizedChart = normalizeLinkedOrderFlowSymbol(chartSymbol)
    val defaultCrypto = "BTCUSDT"

    return when {
        normalizedSelected.isNotBlank() && normalizedSelected != defaultCrypto -> selectedPairSymbol
        normalizedChart.isNotBlank() && normalizedChart != defaultCrypto -> chartSymbol
        else -> selectedPairSymbol
    }
}

private fun normalizeLinkedOrderFlowSymbol(symbol: String): String {
    return symbol
        .uppercase()
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
}
