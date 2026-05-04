package com.asc.markets.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.Trade
import com.asc.markets.logic.PriceStreamManager
import com.trading.app.data.PaperTradingSnapshotStore
import java.util.*

@Composable
fun SimulationScreen(viewModel: ForexViewModel) {
    NewAISimulationScreen(viewModel = viewModel)
}

@Composable
fun MySimulationScreen(viewModel: ForexViewModel) {
    SimulationRouteScreen(
        viewModel = viewModel,
        pageMode = SimulationPageMode.MY
    )
}

@Composable
private fun SimulationRouteScreen(
    viewModel: ForexViewModel,
    pageMode: SimulationPageMode
) {
    val backgroundColor = Color.Black
    val accentColor = Color(0xFF10B981)
    
    var selectedTab by remember { mutableStateOf("DASHBOARD") }
    var engineEnabled by remember { mutableStateOf(false) }
    var tradingMode by remember { mutableStateOf("prompt") } // "auto" or "prompt"
    var chartExpanded by remember { mutableStateOf(false) }
    val simulationChartState = rememberEmbeddedSimulationChartState(symbol = "BTCUSD", timeframe = "1d")
    
    // Core state management for Simulation
    val openTrades = remember { mutableStateListOf<Trade>() }
    val closedTrades = remember { mutableStateListOf<Trade>() }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val snapshot = PaperTradingSnapshotStore.snapshot

    val listState = rememberLazyListState()
    val headerVisible by viewModel.isGlobalHeaderVisible.collectAsState()

    // Sync header visibility state with scroll AND chart expansion
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, chartExpanded) {
        if (chartExpanded) {
            viewModel.setGlobalHeaderVisible(false)
        } else {
            val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
            viewModel.setGlobalHeaderVisible(isAtTop)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (chartExpanded) {
            // FULL SCREEN CHART OVERLAY - No headers, no nav, just chart
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // Full chart
                Box(modifier = Modifier.fillMaxSize()) {
                    com.trading.app.components.TradingChart2(
                        symbol = simulationChartState.symbol,
                        timeframe = simulationChartState.timeframe,
                        style = simulationChartState.chartStyle,
                        chartSettings = simulationChartState.chartSettings,
                        drawings = simulationChartState.drawings,
                        onDrawingUpdate = { updatedDrawing ->
                            val existingIndex = simulationChartState.drawings.indexOfFirst { it.id == updatedDrawing.id }
                            if (existingIndex >= 0) {
                                simulationChartState.drawings[existingIndex] = updatedDrawing
                            } else {
                                simulationChartState.drawings.add(updatedDrawing)
                            }
                        },
                        activeTool = "cursor",
                        onToolReset = {},
                        showVolume = true,
                        showVolumeMa = false,
                        isLocked = false,
                        isVisible = true,
                        selectedCurrency = "USD",
                        onCurrencyClick = {},
                        onLongPress = { simulationChartState.showSettingsSheet = true },
                        onSettingsClick = { simulationChartState.showSettingsSheet = true },
                        selectedTimeZone = "UTC",
                        positions = simulationChartState.positions,
                        onPositionUpdate = {},
                        onPositionDelete = {},
                        selectedIndicatorId = simulationChartState.selectedIndicatorId,
                        onSelectedIndicatorIdChange = { simulationChartState.selectedIndicatorId = it }
                    )
                }

                // Footer / Info bar pinned to bottom (Stream page style)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = simulationChartState.symbol,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1D • REAL-TIME FEED",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { chartExpanded = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Subheader (Sticky)
                Surface(
                    color = backgroundColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val feedColor = if (snapshot.isConnected) accentColor else Color(0xFFEF4444)
                                val feedText = if (snapshot.isConnected) "LIVE" else "OFFLINE"
                                Surface(
                                    color = if (snapshot.isConnected) Color(0xFF142921).copy(alpha = 0.5f) else Color(0xFF2C0B0C).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (snapshot.isConnected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = feedColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("FEED $feedText", color = feedColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Surface(
                                    color = Color(0xFF18181B),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color(0xFF27272A))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if(engineEnabled) accentColor else Color.Gray))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if(engineEnabled) "Engine Active" else "Engine Idle", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    item {
                        SimulationDashboardContent(
                            pageMode = pageMode,
                            accentColor = accentColor,
                            engineEnabled = engineEnabled,
                            onEngineToggle = { engineEnabled = it },
                            tradingMode = tradingMode,
                            onModeToggle = { tradingMode = it },
                            isChartExpanded = chartExpanded,
                            onExpandToggle = { chartExpanded = it },
                            chartState = simulationChartState,
                            openTrades = openTrades.toList(),
                            closedTrades = closedTrades.toList(),
                            onExecuteManualTrade = { type, entry, sl, tp ->
                                val entryP = entry.toDoubleOrNull() ?: 0.0
                                val tpP = tp.toDoubleOrNull() ?: 0.0
                                val slP = sl.toDoubleOrNull() ?: 0.0
                                val profitLoss = if (type == "BUY") (tpP - entryP) * 100 else (entryP - tpP) * 100

                                openTrades.add(0, Trade(
                                    id = UUID.randomUUID().toString().take(8).uppercase(),
                                    asset = "BTC/USDT",
                                    type = type,
                                    entryPrice = entryP,
                                    stopLoss = slP,
                                    takeProfit = tpP,
                                    profitLoss = profitLoss,
                                    timestamp = System.currentTimeMillis()
                                ))
                            },
                            onCloseAllTrades = {
                                closedTrades.addAll(0, openTrades.toList())
                                openTrades.clear()
                            },
                            onCloseProfitableTrades = {
                                val currentPrice = PriceStreamManager.priceUpdates.value["BTC/USDT"]
                                    ?: PriceStreamManager.priceUpdates.value["BTC/USD"]
                                    ?: PriceStreamManager.priceUpdates.value["BTCUSDT"]
                                    ?: PriceStreamManager.priceUpdates.value["BTCUSD"]
                                val profitable = openTrades.filter { trade ->
                                    val direction = if (trade.type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
                                    val pnl = (currentPrice ?: trade.entryPrice - trade.entryPrice) * direction
                                    pnl > 0.0
                                }
                                closedTrades.addAll(0, profitable)
                                openTrades.removeAll { it in profitable }
                            },
                            onResetSimulation = {
                                openTrades.clear()
                                closedTrades.clear()
                                exportStatus = null
                            },
                            onExportPerformance = {
                                val allTrades = openTrades + closedTrades
                                val totalPnl = closedTrades.sumOf { it.profitLoss }
                                val wins = closedTrades.count { it.profitLoss > 0.0 }
                                val winRate = if (closedTrades.isNotEmpty()) (wins.toDouble() / closedTrades.size * 100.0) else 0.0
                                exportStatus = "Exported: ${allTrades.size} trades, PnL: $${String.format("%.2f", totalPnl)}, Win Rate: ${String.format("%.1f%%", winRate)}"
                            },
                            exportStatus = exportStatus,
                            onViewAllHistory = { }
                        )
                    }
                }
            }
        }
    }
}
