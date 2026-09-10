package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import java.util.Calendar
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditSource
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.DeepAuditModal
import com.asc.markets.ui.components.DashboardTrade
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

@Composable
fun TradeLedgerScreen(viewModel: ForexViewModel = viewModel()) {
    var selectedCase by remember { mutableStateOf<PostMoveAuditCase?>(null) }
    var closedTrades by remember { mutableStateOf<List<TradeEntity>>(emptyList()) }
    var filterDirection by remember { mutableStateOf<String?>(null) } // null, "LONG", "SHORT"
    var filterOutcome by remember { mutableStateOf<String?>(null) } // null, "WIN", "LOSS"
    var showFilters by remember { mutableStateOf(false) }
    
    val auditRecords by viewModel.auditRecords.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
    // BINANCE AND FALLBACK REMOVED - EA ONLY
    val timedHistory = remember(marketTimedHistory) {
        marketTimedHistory
    }
    val cases = remember(closedTrades, auditRecords, candidates, timedHistory) {
        PostMoveAuditStore.buildCases(closedTrades, auditRecords, candidates, timedHistory)
    }
    val allLedgerCases = remember(cases) {
        cases.filter { it.source == PostMoveAuditSource.CLOSED_TRADE }
    }
    
    // Apply filters
    val ledgerCases = remember(allLedgerCases, filterDirection, filterOutcome) {
        allLedgerCases.filter { case ->
            val directionMatch = filterDirection == null || case.direction == filterDirection
            val outcomeMatch = filterOutcome == null || 
                (filterOutcome == "WIN" && case.win == true) ||
                (filterOutcome == "LOSS" && case.win == false)
            directionMatch && outcomeMatch
        }
    }
    
    // Calculate statistics
    val avgSlippage = PostMoveAuditStore.averageSlippage(ledgerCases)
    val efficiency = PostMoveAuditStore.outcomeEfficiency(ledgerCases)
    val totalTrades = ledgerCases.size
    val winningTrades = ledgerCases.count { it.win == true }
    val winRate = if (totalTrades > 0) (winningTrades.toDouble() / totalTrades * 100).toInt() else 0
    val avgPnl = ledgerCases.mapNotNull { it.pnl }.average().takeIf { !it.isNaN() }
    val bestTrade = ledgerCases.maxByOrNull { it.pnl ?: Double.MIN_VALUE }
    val worstTrade = ledgerCases.minByOrNull { it.pnl ?: Double.MAX_VALUE }

    LaunchedEffect(viewModel.tradeHistoryRepository) {
        closedTrades = withContext(Dispatchers.IO) {
            viewModel.tradeHistoryRepository?.getLast100Trades().orEmpty()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color.White.copy(alpha = 0.06f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CLOSED TRADE POST-MOVE LEDGER", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    imageVector = if (showFilters) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                    contentDescription = "Filter",
                    tint = if (filterDirection != null || filterOutcome != null) EmeraldSuccess else Color.White
                )
            }
        }
        
        // Filter Bar
        if (showFilters) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0A0A0A)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("FILTERS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            label = { Text("ALL") },
                            selected = filterDirection == null && filterOutcome == null,
                            onClick = {
                                filterDirection = null
                                filterOutcome = null
                            }
                        )
                        FilterChip(
                            label = { Text("LONG") },
                            selected = filterDirection == "LONG",
                            onClick = { filterDirection = if (filterDirection == "LONG") null else "LONG" }
                        )
                        FilterChip(
                            label = { Text("SHORT") },
                            selected = filterDirection == "SHORT",
                            onClick = { filterDirection = if (filterDirection == "SHORT") null else "SHORT" }
                        )
                        FilterChip(
                            label = { Text("WINS") },
                            selected = filterOutcome == "WIN",
                            onClick = { filterOutcome = if (filterOutcome == "WIN") null else "WIN" }
                        )
                        FilterChip(
                            label = { Text("LOSSES") },
                            selected = filterOutcome == "LOSS",
                            onClick = { filterOutcome = if (filterOutcome == "LOSS") null else "LOSS" }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepBlack)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color.White.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DeepBlack),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Statistics Summary Card
            item {
                InfoBox(minHeight = 220.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF101010)) {
                                Box(contentAlignment = Alignment.Center) { Text("🔒", color = Color.White, fontSize = 20.sp) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("EXECUTION", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                Text("QUALITY", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("POST-MOVE CLOSED TRADE LEDGER", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Row 1: Slippage and Efficiency
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AVG SLIPPAGE", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(
                                    avgSlippage?.let { String.format(Locale.US, "%.2f PIPS", it) } ?: "NOT CAPTURED", 
                                    color = if (avgSlippage == null) SlateText else if (avgSlippage > 2.0) RoseError else EmeraldSuccess, 
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text("EFFICIENCY", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(
                                    efficiency?.let { "$it%" } ?: "PENDING", 
                                    color = if (efficiency == null) SlateText else if (efficiency >= 70) EmeraldSuccess else if (efficiency >= 50) IndigoAccent else RoseError, 
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Row 2: Total Trades and Win Rate
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL TRADES", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text("$totalTrades", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text("WIN RATE", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(
                                    "$winRate%", 
                                    color = if (winRate >= 60) EmeraldSuccess else if (winRate >= 45) IndigoAccent else RoseError, 
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text("$winningTrades / $totalTrades", color = SlateText, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Row 3: Average PnL
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AVG PNL", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(
                                    avgPnl?.let { String.format(Locale.US, "%+.2f", it) } ?: "N/A", 
                                    color = if (avgPnl == null) SlateText else if (avgPnl >= 0) EmeraldSuccess else RoseError, 
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text("BEST / WORST", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        bestTrade?.pnl?.let { String.format(Locale.US, "+%.0f", it) } ?: "N/A",
                                        color = EmeraldSuccess,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("/", color = SlateText, fontSize = 14.sp)
                                    Text(
                                        worstTrade?.pnl?.let { String.format(Locale.US, "%.0f", it) } ?: "N/A",
                                        color = RoseError,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (ledgerCases.isEmpty()) {
                item {
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("NO CLOSED TRADE LEDGER RECORDS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text("The ledger will populate after confirmed fills are saved through TradeHistoryRepository or synchronized from a closed-order source.", color = SlateText, fontSize = 12.sp, lineHeight = 17.sp, fontFamily = InterFontFamily)
                        }
                    }
                }
            } else {
                items(
                    items = ledgerCases,
                    key = { it.id }
                ) { case ->
                    DashboardTrade(case) { selectedCase = it }
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }

    if (selectedCase != null) {
        DeepAuditModal(selectedCase!!) { selectedCase = null }
    }
}
