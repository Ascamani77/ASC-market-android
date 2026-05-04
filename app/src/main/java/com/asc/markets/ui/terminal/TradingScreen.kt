package com.asc.markets.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.asc.markets.ui.terminal.components.*
import com.asc.markets.ui.terminal.models.ChartSettings
import com.asc.markets.ui.terminal.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.ui.terminal.viewmodels.ChartViewModel

@Composable
fun TradingScreen(
    viewModel: ChartViewModel = viewModel(),
    onSymbolSelected: (String) -> Unit = {}
) {
    val activeSymbol by viewModel.activeSymbol.collectAsState()
    val currentPair by viewModel.currentPair.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()
    val currentTimeframe by viewModel.timeframe.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val isMagnetMode by viewModel.isMagnetMode.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isCrosshairEnabled by viewModel.isCrosshairEnabled.collectAsState()
    val chartType by viewModel.chartType.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val indicators by viewModel.indicators.collectAsState()
    val activePositions by viewModel.activePositions.collectAsState()
    val selectedDrawingId by viewModel.selectedDrawingId.collectAsState()
    val selectedTimezone by viewModel.selectedTimezone.collectAsState()
    
    var activeTab by remember { mutableStateOf("Trading Panel") }
    
    // Modal States
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isSymbolSearchOpen by remember { mutableStateOf(false) }
    var isIndicatorsModalOpen by remember { mutableStateOf(false) }
    var isTimezoneModalOpen by remember { mutableStateOf(false) }
    
    // Right Panel States
    var isRightStripVisible by remember { mutableStateOf(false) }
    var isRightPanelExpanded by remember { mutableStateOf(false) }
    var activeUtility by remember { mutableStateOf<String?>("watchlist") }

    Scaffold(
        topBar = {
            TradingToolbar(
                activeSymbol = currentPair.symbol,
                currentTimeframe = currentTimeframe,
                currentStyle = chartType,
                onTimeframeChange = { viewModel.setTimeframe(it) },
                onStyleChange = { viewModel.setChartType(it) },
                onIndicatorsClick = { isIndicatorsModalOpen = true },
                onIndicatorToggle = { viewModel.toggleIndicator(it) },
                onReplayClick = { /* Toggle replay */ },
                onUndoClick = { /* Undo */ },
                onRedoClick = { /* Redo */ },
                onSettingsClick = { isSettingsOpen = true },
                onSymbolClick = { isSymbolSearchOpen = true },
                onTradeClick = { activeTab = "Trading Panel" },
                onSearchClick = { /* Search */ },
                onLayoutClick = { /* Change layout */ }
            )
        },
        bottomBar = {
            Column {
                BottomPanel(
                    activeTab = activeTab,
                    onTabChange = { activeTab = it }
                )
                StatusBar(
                    selectedTimezone = selectedTimezone,
                    onTimezoneClick = { isTimezoneModalOpen = true }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Trigger AI */ },
                containerColor = Color(0xFF2962FF),
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analyst")
            }
        },
        containerColor = if (theme == "dark") DarkBackground else Color.White
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Left Toolbar
            LeftToolbar(
                activeTool = activeTool,
                onToolClick = { viewModel.setActiveTool(it) }
            )
            
            // Main Content Area (Chart + Right Panel)
            Row(modifier = Modifier.weight(1f)) {
                // Chart Area
                Box(modifier = Modifier.weight(1f)) {
                    ChartContainer(
                        canvasSettings = settings.canvas,
                        viewModel = viewModel
                    )
                    
                    // Overlay Price Status Line
                    PriceStatusLine(
                        symbol = currentPair.symbol,
                        timeframe = currentTimeframe.replace("m", ""),
                        price = currentPair.price,
                        changePercent = currentPair.changePercent,
                        priceHistory = priceHistory,
                        indicators = indicators,
                        onIndicatorAction = { ind, action -> viewModel.toggleIndicator(ind) },
                        onBuy = { viewModel.handleTrade("buy") },
                        onSell = { viewModel.handleTrade("sell") },
                        isBuyEnabled = !activePositions.contains("${activeSymbol}_buy"),
                        isSellEnabled = !activePositions.contains("${activeSymbol}_sell"),
                        settings = settings.statusLine
                    )

                    // Active Positions Overlay (Top Left below Status Line)
                    Column(
                        modifier = Modifier
                            .padding(top = 100.dp, start = 16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        activePositions.filter { it.startsWith(activeSymbol) }.forEach { posKey ->
                            val type = if (posKey.endsWith("buy")) "buy" else "sell"
                            Card(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .width(160.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E222D).copy(alpha = 0.9f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF363A45)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    if (type == "buy") Color(0xFF2962FF) else Color(0xFFF23645),
                                                    androidx.compose.foundation.shape.CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${type.uppercase()} 40",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            Text(
                                                text = "+$12.40",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (type == "buy") Color(0xFF089981) else Color(0xFFF23645)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.closeTrade(type) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right Tool Panel (Overlay on chart)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    ) {
                        RightToolPanel(
                            isCrosshairEnabled = isCrosshairEnabled,
                            isLocked = isLocked,
                            isMagnetMode = isMagnetMode,
                            onCrosshairToggle = { viewModel.setCrosshairEnabled(it) },
                            onLockToggle = { viewModel.setLocked(it) },
                            onMagnetToggle = { viewModel.setMagnetMode(it) },
                            onResetZoom = { viewModel.resetZoom() }
                        )
                    }

                    // Floating Drawing Toolbar (Selected Object)
                    if (selectedDrawingId != null) {
                        DrawingToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 80.dp),
                            onDelete = { 
                                selectedDrawingId?.let { viewModel.removeDrawing(it) }
                            }
                        )
                    }
                }
                
                // Right Panel Implementation
                Box(modifier = Modifier.fillMaxHeight()) {
                    if (isRightStripVisible) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            // RightPanelContent migration needed if it exists
                            
                            RightSidebar(
                                activeUtility = if (isRightPanelExpanded) activeUtility else null,
                                onUtilityClick = { id ->
                                    if (isRightPanelExpanded && activeUtility == id) {
                                        isRightPanelExpanded = false
                                    } else {
                                        activeUtility = id
                                        isRightPanelExpanded = true
                                    }
                                },
                                onToggleExpand = { isRightStripVisible = false }
                            )
                        }
                    } else {
                        // Handle to show strip
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(12.dp)
                                .clickable { isRightStripVisible = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(40.dp)
                                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }

        // Modals
        SettingsModal(
            isOpen = isSettingsOpen,
            onClose = { isSettingsOpen = false },
            settings = settings,
            onSave = { viewModel.setSettings(it) },
            chartType = chartType,
            onChartTypeChange = { viewModel.setChartType(it) },
            theme = theme,
            onThemeChange = { viewModel.setTheme(it) },
            onResetDrawings = { viewModel.resetDrawings() }
        )
        
        SymbolSearchModal(
            isOpen = isSymbolSearchOpen,
            onClose = { isSymbolSearchOpen = false },
            onSelect = {
                viewModel.setSymbol(it)
                onSymbolSelected(it)
            }
        )
        
        IndicatorsModal(
            isOpen = isIndicatorsModalOpen,
            onClose = { isIndicatorsModalOpen = false },
            onSelectIndicator = { viewModel.toggleIndicator(it) }
        )

        TimezoneModal(
            isOpen = isTimezoneModalOpen,
            onClose = { isTimezoneModalOpen = false },
            selectedTimezone = selectedTimezone,
            onSelect = { viewModel.setTimezone(it) }
        )
    }
}
