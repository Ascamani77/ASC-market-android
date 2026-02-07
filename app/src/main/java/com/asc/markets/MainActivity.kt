package com.asc.markets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.snapshotFlow
import com.asc.markets.data.AppView
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.IntegrityWatchdog
import com.asc.markets.ui.screens.*
import com.asc.markets.ui.components.*
import androidx.compose.runtime.CompositionLocalProvider
import com.asc.markets.ui.components.LocalShowMicrostructure
import com.asc.markets.ui.theme.*
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure system bars match app's dark background to remove gray seams
        try {
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            // Force opaque divider and ensure system bars use dark content coloring off
            try {
                window.navigationBarDividerColor = android.graphics.Color.BLACK
            } catch (t: Throwable) { /* API guard */ }

            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
        } catch (t: Throwable) {
            // ignore on older devices
        }
        Log.d("ASC", "MainActivity.onCreate() called")
        // start integrity watchdog to keep profiler metrics updated
        IntegrityWatchdog.start()
        setContent {
            AscTheme {
                Log.d("ASC", "AscTheme content block called")
                val viewModel: ForexViewModel = viewModel()
                val isInitializing by viewModel.isInitializing.collectAsState()
                val isRiskAccepted by viewModel.isRiskAccepted.collectAsState()
                val currentView by viewModel.currentView.collectAsState()
                val isSidebarCollapsed by viewModel.isSidebarCollapsed.collectAsState()
                val selectedPair by viewModel.selectedPair.collectAsState()
                val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
                val isCommandPaletteOpen by viewModel.isCommandPaletteOpen.collectAsState()
                val promoteMacro by viewModel.promoteMacroStream.collectAsState()
                val isHeaderVisible by viewModel.isGlobalHeaderVisible.collectAsState()

                if (isInitializing) {
                    SecureBootScreen()
                } else if (!isRiskAccepted) {
                    DisclaimerOverlay(onAccept = { viewModel.acceptRisk() })
                } else {
                    // Modal drawer overlay (hidden by default) to match JS hidden sidebar behavior
                    val scope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(initialValue = if (isDrawerOpen) DrawerValue.Open else DrawerValue.Closed)

                    // Sync ViewModel state -> drawerState and log changes for debugging
                    LaunchedEffect(isDrawerOpen) {
                        Log.d("ASC", "LaunchedEffect: isDrawerOpen=$isDrawerOpen")
                        if (isDrawerOpen) {
                            drawerState.open()
                            Log.d("ASC", "drawerState.open() called")
                        } else {
                            drawerState.close()
                            Log.d("ASC", "drawerState.close() called")
                        }
                    }

                    // Observe drawerState changes (e.g., scrim tap or swipe) and update ViewModel
                    LaunchedEffect(drawerState) {
                        snapshotFlow { drawerState.isOpen }.collect { open ->
                            Log.d("ASC", "snapshotFlow: drawerState.isOpen=$open, vmIsOpen=$isDrawerOpen")
                            if (open && !isDrawerOpen) {
                                viewModel.openDrawer()
                                Log.d("ASC", "viewModel.openDrawer() called from snapshotFlow")
                            } else if (!open && isDrawerOpen) {
                                viewModel.closeDrawer()
                                Log.d("ASC", "viewModel.closeDrawer() called from snapshotFlow")
                            }
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(modifier = Modifier.background(DeepBlack)) {
                                Surface(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .fillMaxHeight(),
                                    color = DeepBlack,
                                    tonalElevation = 0.dp
                                ) {
                                    AscSidebar(
                                        currentView = currentView,
                                        isCollapsed = false,
                                        promoteMacro = promoteMacro,
                                        onViewChange = { view ->
                                            viewModel.navigateTo(view)
                                            viewModel.closeDrawer()
                                        },
                                        onClose = { viewModel.closeDrawer() }
                                    )
                                }
                            }
                        },
                        scrimColor = PureBlack.copy(alpha = 0.6f),
                        gesturesEnabled = true
                    ) {
                        // Close drawer on back press first
                        BackHandler(enabled = isDrawerOpen) {
                            viewModel.closeDrawer()
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = PureBlack,
                            bottomBar = {
                                if (currentView != AppView.TRADING_ASSISTANT) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(65.dp),
                                        color = PureBlack,
                                        tonalElevation = 0.dp
                                    ) {
                                        NotchedBottomNav(
                                            currentView = currentView,
                                            onNavigate = { viewModel.navigateTo(it) },
                                            onHomeSelected = {
                                                viewModel.navigateTo(AppView.DASHBOARD)
                                                viewModel.setDashboardTab("MACRO_STREAM")
                                            }
                                        )
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
                                    AppView.DASHBOARD -> {
                                        if (isHeaderVisible) {
                                            GlobalHeader(
                                                currentView = currentView,
                                                selectedPair = selectedPair,
                                                onOpenDrawer = { viewModel.openDrawer() },
                                                onSearch = { viewModel.openCommandPalette() },
                                                onNotifications = { viewModel.navigateTo(AppView.DIAGNOSTICS) }
                                            )
                                        }
                                    }
                                    // Let the Post-Move Audit screen render its own top controls
                                    AppView.POST_MOVE_AUDIT -> {
                                        /* Intentionally no header here. PostMoveAuditScreen provides its own top control bar which should replace the app header. */
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
                                        AppView.MARKETS -> MarketsScreen { viewModel.selectPair(it) }
                                        AppView.CHAT -> ChatScreen(viewModel)
                                        AppView.ALERTS -> AlertsScreen(viewModel)
                                        AppView.BACKTEST -> BacktestScreen(viewModel)
                                        AppView.TRADING_ASSISTANT -> TerminalScreen(viewModel)
                                        AppView.MULTI_TIMEFRAME -> MultiTimeframeScreen(selectedPair.symbol)
                                        AppView.LIQUIDITY_HUB -> LiquidityHubScreen()
                                        AppView.TRADE -> TradeLedgerScreen()
                                        AppView.NEWS -> MacroIntelScreen()
                                        AppView.MACRO_STREAM -> {
                                            val events by viewModel.macroStreamEvents.collectAsState()
                                            CompositionLocalProvider(LocalShowMicrostructure provides false) {
                                                MacroStreamView(events = events, viewModel = viewModel)
                                            }
                                        }
                                        AppView.CALENDAR -> EconomicCalendarScreen()
                                        AppView.SENTIMENT -> SentimentScreen()
                                        AppView.EDUCATION -> EducationScreen()
                                        AppView.ANALYSIS_RESULTS -> AnalysisResultsScreen()
                                        AppView.DIAGNOSTICS -> DiagnosticsScreen()
                                        AppView.POST_MOVE_AUDIT -> PostMoveAuditScreen()
                                        AppView.PROFILE -> ProfileScreen()
                                        AppView.SETTINGS -> SettingsScreen(viewModel)
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
}

@Composable
fun SecureBootScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val infiniteTransition = rememberInfiniteTransition(label = "boot")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                label = "alpha"
            )
            
            Text("▲", color = Color.White.copy(alpha = alpha), fontSize = 48.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "INITIALIZING SECURE NODE", 
                color = Color.White.copy(alpha = 0.4f), 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Black, 
                letterSpacing = 4.sp
            )
        }
    }
}