package com.asc.markets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.EventStreamViewModel
import com.asc.markets.data.AppView
import com.asc.markets.ui.screens.*
import com.asc.markets.ui.components.*
import com.asc.markets.ui.ai.AiScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.asc.markets.ui.theme.*
import android.util.Log
import com.asc.markets.ui.terminal.viewmodels.ChartViewModel
import com.researchcenter.ui.screens.AnalysisOpinionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AscTheme {
                val viewModel: ForexViewModel = viewModel()
                val chartViewModel: ChartViewModel = viewModel()
                val currentView by viewModel.currentView.collectAsState()
                val isInitializing by viewModel.isInitializing.collectAsState()
                val isRiskAccepted by viewModel.isRiskAccepted.collectAsState()
                val showRiskDisclosure by viewModel.showRiskDisclosure.collectAsState()
                val selectedPair by viewModel.selectedPair.collectAsState()
                val chartActiveSymbol by chartViewModel.activeSymbol.collectAsState()
                val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
                val promoteMacro by viewModel.promoteMacroStream.collectAsState()
                val isCommandPaletteOpen by viewModel.isCommandPaletteOpen.collectAsState()
                val linkedOrderFlowSymbol = remember(selectedPair.symbol, chartActiveSymbol) {
                    resolveLinkedOrderFlowSymbol(
                        selectedPairSymbol = selectedPair.symbol,
                        chartSymbol = chartActiveSymbol
                    )
                }

                if (isInitializing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PureBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = IndigoAccent)
                    }
                } else if (!isRiskAccepted && showRiskDisclosure) {
                    DisclaimerOverlay(onAccept = { viewModel.acceptRisk() })
                } else {
                    // Modal drawer has been replaced by SIDEBAR_PAGE for a full-screen menu experience.
                    // The swipe-to-open gesture is disabled by removing the ModalNavigationDrawer wrapper.

                    // Global back handler for general navigation (Markets, Chat, etc.)
                    // Exclude DASHBOARD (should exit app) and ANALYSIS_OPINION (has its own internal BackHandler)
                    BackHandler(enabled = currentView != AppView.DASHBOARD && currentView != AppView.ANALYSIS_OPINION) {
                        viewModel.navigateBack()
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
                            // THE TRICK: Swap header based on state
                            when (currentView) {
                                AppView.DASHBOARD, AppView.MARKETS, AppView.CALENDAR, AppView.INTELLIGENCE_STREAM -> {
                                    val unread: Int by viewModel.unreadCount.collectAsState(initial = 0)
                                    val collapseProgress by if (currentView == AppView.INTELLIGENCE_STREAM) {
                                        val intelViewModel: EventStreamViewModel = viewModel()
                                        intelViewModel.globalHeaderCollapse.collectAsState(initial = 0f)
                                    } else {
                                        viewModel.globalHeaderCollapse.collectAsState(initial = 0f)
                                    }

                                    // Animate header height (72.dp -> 0.dp) based on collapse progress
                                    val targetHeight = if (headerVisible) (72.dp * (1f - collapseProgress)) else 0.dp
                                    val headerHeight by animateDpAsState(targetValue = targetHeight)

                                    Box(modifier = Modifier.fillMaxWidth().height(headerHeight)) {
                                        GlobalHeader(
                                            currentView = currentView,
                                            selectedPair = selectedPair,
                                            onOpenDrawer = { viewModel.navigateTo(AppView.SIDEBAR_PAGE) },
                                            onSearch = { viewModel.openCommandPalette() },
                                            onNotifications = { viewModel.navigateTo(AppView.CALENDAR) },
                                            unreadCount = unread
                                        )
                                    }
                                }
                                // Let screens that provide their own header render without the global NavHeader
                                AppView.POST_MOVE_AUDIT, AppView.HOME_ALERTS, AppView.ANALYSIS_OPINION, AppView.ANALYSIS_RESULTS, AppView.SIDEBAR_PAGE, AppView.SIMULATION, AppView.MY_SIMULATION, AppView.STREAM, AppView.PAPER_TRADING, AppView.SETTINGS, AppView.ALERTS, AppView.CREATE_ALERT, AppView.NOTIFICATIONS, AppView.PUSH_SETTINGS, AppView.MY_ALERTS, AppView.WATCHLIST -> {
                                    /* Intentionally no header here. The screen provides its own top control bar which should replace the app header. */
                                }
                                else -> {
                                        NavHeader(
                                            title = currentView.name.replace("_", " "),
                                            onBack = { viewModel.navigateBack() },
                                            onSearch = { viewModel.openCommandPalette() }
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (currentView) {
                                    AppView.DASHBOARD -> DashboardScreen(viewModel)
                                    AppView.MARKETS -> MarketsScreen({ viewModel.selectPair(it) }, viewModel)
                                    AppView.CHAT -> ChatScreen(viewModel)
                                    AppView.ALERTS -> AlertsScreen(viewModel)
                                    AppView.CREATE_ALERT -> CreateAlertScreen(viewModel)
                                    AppView.MY_ALERTS -> MyAlertsScreen(
                                        viewModel = viewModel,
                                        onCreateAlertClick = { viewModel.navigateTo(AppView.CREATE_ALERT) },
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
                                    AppView.INTELLIGENCE_STREAM -> EventStreamScreen()
                                    AppView.CALENDAR -> CalendarScreen()
                                    AppView.STREAM -> StreamScreen()
                                    AppView.SENTIMENT -> SentimentScreen(viewModel)
                                    AppView.EDUCATION -> EducationScreen()
                                    AppView.ANALYSIS_RESULTS -> AnalysisResultsScreen()
                                    AppView.WATCHLIST -> WatchlistScreen(
                                        viewModel = viewModel,
                                        onViewChart = { symbol ->
                                            viewModel.selectPairBySymbol(symbol)
                                            viewModel.navigateTo(AppView.STREAM)
                                        },
                                        onSetAlert = { symbol ->
                                            viewModel.selectPairBySymbol(symbol)
                                            viewModel.navigateTo(AppView.CREATE_ALERT)
                                        },
                                        onDeepDive = { symbol ->
                                            viewModel.selectPairBySymbol(symbol)
                                            viewModel.navigateTo(AppView.ANALYSIS_RESULTS)
                                        }
                                    )
                                    AppView.DIAGNOSTICS -> DiagnosticsScreen()
                                    AppView.MARKET_WATCH -> MarketWatchScreen()
                                    AppView.POST_MOVE_AUDIT -> PostMoveAuditScreen()
                                    AppView.DATA_HUB -> DataHubScreen()
                                    AppView.DATA_VAULT -> DataVaultScreen()
                                    AppView.PORTFOLIO_MANAGER -> PortfolioManagerScreen()
                                    AppView.TRADE_RECONSTRUCTION -> TradeReconstructionScreen()
                                    AppView.PROFILE -> ProfileScreen()
                                    AppView.MARKET_VIEW -> MarketViewScreen()
                                    AppView.SETTINGS -> SettingsScreen(viewModel)
                                    AppView.PAPER_TRADING -> PaperTradingScreen(viewModel)
                                    AppView.QUOTES -> QuotesScreen(viewModel)
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
                                                viewModel.navigateTo(view)
                                            },
                                            onClose = { viewModel.navigateBack() }
                                        )
                                    }
                                    AppView.AI_TERMINAL -> TerminalScreen(viewModel)
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
            }
        }
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
