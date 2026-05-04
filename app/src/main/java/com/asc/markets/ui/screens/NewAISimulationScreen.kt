package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.trading.app.data.PaperTradingSnapshotStore

@Composable
fun NewAISimulationScreen(viewModel: ForexViewModel) {
    val backgroundColor = Color.Black
    val cardColor = Color.Black
    val accentColor = Color(0xFF10B981) // Green

    var engineEnabled by remember { mutableStateOf(false) }
    var chartExpanded by remember { mutableStateOf(false) }
    val aiChartState = rememberEmbeddedSimulationChartState(symbol = "BTCUSD", timeframe = "1h")
    val listState = rememberLazyListState()
    val snapshot = PaperTradingSnapshotStore.snapshot

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, chartExpanded) {
        if (chartExpanded) {
            viewModel.setGlobalHeaderVisible(false)
        } else {
            val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
            viewModel.setGlobalHeaderVisible(isAtTop)
        }
    }

    // Full screen chart overlay - no box wrapper, chart fills entire screen
    if (chartExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // TradingChart2 directly without any Surface wrapper
            Box(modifier = Modifier.fillMaxSize()) {
                com.trading.app.components.TradingChart2(
                    symbol = aiChartState.symbol,
                    timeframe = aiChartState.timeframe,
                    style = aiChartState.chartStyle,
                    chartSettings = aiChartState.chartSettings,
                    drawings = aiChartState.drawings,
                    onDrawingUpdate = { updatedDrawing ->
                        val existingIndex = aiChartState.drawings.indexOfFirst { it.id == updatedDrawing.id }
                        if (existingIndex >= 0) {
                            aiChartState.drawings[existingIndex] = updatedDrawing
                        } else {
                            aiChartState.drawings.add(updatedDrawing)
                        }
                    },
                    activeTool = "cursor",
                    onToolReset = {},
                    showVolume = true,
                    showVolumeMa = false,
                    isLocked = false,
                    isVisible = true,
                    selectedCurrency = when {
                        aiChartState.symbol.uppercase().endsWith("USDT") -> "USDT"
                        aiChartState.symbol.uppercase().endsWith("JPY") -> "JPY"
                        aiChartState.symbol.uppercase().endsWith("GBP") -> "GBP"
                        aiChartState.symbol.uppercase().endsWith("EUR") -> "EUR"
                        else -> "USD"
                    },
                    onCurrencyClick = {},
                    onLongPress = {},
                    onSettingsClick = {},
                    selectedTimeZone = "UTC",
                    positions = aiChartState.positions,
                    onPositionUpdate = {},
                    onPositionDelete = {},
                    selectedIndicatorId = aiChartState.selectedIndicatorId,
                    onSelectedIndicatorIdChange = { aiChartState.selectedIndicatorId = it }
                )
            }
            
            // X button to exit - positioned on top
            IconButton(
                onClick = { chartExpanded = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ASC",
                            color = accentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SIMULATION",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                val feedColor = if (snapshot.isConnected) accentColor else Color(0xFFEF4444)
                val feedText = if (snapshot.isConnected) "LIVE" else "OFFLINE"
                Surface(
                    color = if (snapshot.isConnected) Color(0xFF142921).copy(alpha = 0.5f) else Color(0xFF2C0B0C).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (snapshot.isConnected) accentColor.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = feedColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("FEED $feedText", color = feedColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFF142921).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accentColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI ENGINE ACTIVE", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Icon(Icons.Default.NotificationsNone, contentDescription = "Alerts", tint = Color.White)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current State
            item {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radar icon
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CURRENT STATE", color = Color.Gray, fontSize = 10.sp)
                            val stateText = if (snapshot.currentTradeSymbol != null) "POSITION OPEN" else if (snapshot.hasLiveTradeData) "SCANNING" else "WAITING FOR TRIGGER"
                            Text(stateText, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val descText = if (snapshot.currentTradeSymbol != null) "Managing active ${snapshot.currentTradeSymbol} position." else "AI is scanning the market for high-probability opportunities."
                            Text(descText, color = Color.Gray, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("MODE", color = Color.Gray, fontSize = 10.sp)
                            Text("AUTONOMOUS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("CONFIDENCE", color = Color.Gray, fontSize = 10.sp)
                            val confidence = if (snapshot.hasLiveTradeData) "85%" else "82%"
                            Text(confidence, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Chart Section - Compact Mini Chart
            item {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PnlPositionOverviewCard(accentColor = accentColor, onExpand = { chartExpanded = true })
                }
            }

            // Active AI Trade (if any)
            item {
                val activeTrade = remember(snapshot) {
                    simulationDisplayTrade(snapshot, emptyList(), null)
                }
                ActiveTradeBox(accentColor = accentColor, title = "ACTIVE AI TRADE", trade = activeTrade)
            }

            // AI Performance Stats - Detailed
            item {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TRADE STATISTICS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            val totalPnl = snapshot.realizedPnl + snapshot.floatingPnl
                            val pnlColor = if (totalPnl >= 0.0) accentColor else Color(0xFFEF4444)
                            Text("$${String.format("%.2f", totalPnl)}", color = pnlColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TradeStatBox("TOTAL", "${snapshot.activeTrades + snapshot.activeOrders}", Color.White)
                            TradeStatBox("WINS", "${snapshot.activeTrades}", accentColor)
                            TradeStatBox("LOSSES", "0", Color(0xFFEF4444))
                            TradeStatBox("UNFILLED", "${snapshot.activeOrders}", Color.Gray)
                        }
                    }
                }
            }

            // AI Reasoning - Decision Process
            item {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color(0xFF9333EA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI DECISION PROCESS", color = Color(0xFF9333EA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                color = Color(0xFF142921),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val confidence = if (snapshot.hasLiveTradeData) "85%" else "82%"
                                Text("CONFIDENCE $confidence", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val marketStatus = if (snapshot.currentTradeSymbol != null) "IN TRADE" else if (snapshot.hasLiveTradeData) "SCAN" else "WAIT"
                            val marketProgress = if (snapshot.currentTradeSymbol != null) 1.0f else if (snapshot.hasLiveTradeData) 0.72f else 0.0f
                            AiDecisionVisualCard("MARKET", marketStatus, marketProgress, accentColor, Modifier.weight(1f))
                            val entryStatus = if (snapshot.currentTradeSymbol != null) snapshot.currentTradeSide?.uppercase() ?: "OPEN" else "WAIT"
                            val entryProgress = if (snapshot.currentTradeSymbol != null) 1.0f else 0.46f
                            AiDecisionVisualCard("ENTRY", entryStatus, entryProgress, Color(0xFF3B82F6), Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val riskLevel = if (snapshot.openRiskPct >= 5.0) "HIGH" else "SAFE"
                            val riskProgress = (snapshot.openRiskPct / 10.0).toFloat().coerceIn(0f, 1f)
                            val riskColor = if (snapshot.openRiskPct >= 5.0) Color(0xFFEF4444) else Color(0xFFF59E0B)
                            AiDecisionVisualCard("RISK", riskLevel, riskProgress, riskColor, Modifier.weight(1f))
                            val score = if (snapshot.hasLiveTradeData) "85" else "---"
                            val scoreProgress = if (snapshot.hasLiveTradeData) 0.85f else 0f
                            AiDecisionVisualCard("SCORE", score, scoreProgress, Color(0xFF9333EA), Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = Color.White.copy(alpha = 0.035f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val decisionColor = if (snapshot.currentTradeSymbol != null) accentColor else Color.Gray
                                    Box(modifier = Modifier.size(8.dp).background(decisionColor, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DECISION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                val decisionText = if (snapshot.currentTradeSymbol != null) "POSITION ACTIVE" else "WAITING FOR TRIGGER"
                                Text(decisionText, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { engineEnabled = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START ENGINE", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { engineEnabled = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F).copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PAUSE ENGINE", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { /* manual override */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PanTool, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MANUAL OVERRIDE", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeStatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AiDecisionVisualCard(
    label: String,
    value: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(8.dp).background(accentColor, CircleShape))
        }
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedProgress)
                    .background(accentColor, RoundedCornerShape(999.dp))
            )
        }
    }
}
