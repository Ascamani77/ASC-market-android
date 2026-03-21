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
import java.util.*

@Composable
fun SimulationScreen(viewModel: ForexViewModel) {
    val backgroundColor = Color(0xFF09090B)
    val accentColor = Color(0xFF10B981)
    
    var selectedTab by remember { mutableStateOf("DASHBOARD") }
    var engineEnabled by remember { mutableStateOf(false) }
    var tradingMode by remember { mutableStateOf("prompt") } // "auto" or "prompt"
    
    // Core state management for Simulation
    val trades = remember { mutableStateListOf<Trade>() }
    
    val listState = rememberLazyListState()
    val headerVisible by viewModel.isGlobalHeaderVisible.collectAsState()

    // Sync header visibility state with scroll
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
        viewModel.setGlobalHeaderVisible(isAtTop)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Main Header (Tabs) - Animated visibility
        AnimatedVisibility(
            visible = headerVisible,
            enter = expandVertically() + fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = shrinkVertically() + fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf("DASHBOARD", "BRAIN AUDIT", "HISTORY").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = { selectedTab = tab },
                        color = if (isSelected) Color(0xFF1D1D1F) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = if (!isSelected) BorderStroke(1.dp, Color(0xFF1C1C1E)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgedBox(
                                badge = {
                                    if (tab == "HISTORY" && trades.isNotEmpty()) {
                                        Badge(containerColor = accentColor) {
                                            Text(trades.size.toString(), color = Color.Black)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = when(tab) {
                                        "DASHBOARD" -> Icons.Default.GridView
                                        "BRAIN AUDIT" -> Icons.Default.Psychology
                                        else -> Icons.Default.History
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Subheader (Sticky)
        Surface(
            color = backgroundColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF142921).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LIVE FEED", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("BALANCE: ", color = Color.Gray, fontSize = 13.sp)
                        Text("$10,000.00", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            when (selectedTab) {
                "DASHBOARD" -> {
                    item { MarketTickerHeaderV4() }
                    item {
                        SimulationDashboardContent(
                            accentColor = accentColor,
                            engineEnabled = engineEnabled,
                            onEngineToggle = { engineEnabled = it },
                            tradingMode = tradingMode,
                            onModeToggle = { tradingMode = it },
                            onExecuteManualTrade = { type, entry, sl, tp ->
                                val entryP = entry.toDoubleOrNull() ?: 0.0
                                val tpP = tp.toDoubleOrNull() ?: 0.0
                                val slP = sl.toDoubleOrNull() ?: 0.0
                                val profitLoss = if (type == "BUY") (tpP - entryP) * 100 else (entryP - tpP) * 100
                                
                                trades.add(0, Trade(
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
                            onViewAllHistory = { selectedTab = "HISTORY" }
                        )
                    }
                }
                "BRAIN AUDIT" -> {
                    item { SimulationBrainAuditContent() }
                }
                "HISTORY" -> {
                    item { SimulationHistoryContent(trades = trades, onBack = { selectedTab = "DASHBOARD" }) }
                }
            }
        }
    }
}
