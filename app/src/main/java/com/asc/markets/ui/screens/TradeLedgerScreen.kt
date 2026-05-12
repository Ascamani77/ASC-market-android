package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.BinanceDataStore
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

@Composable
fun TradeLedgerScreen(viewModel: ForexViewModel = viewModel()) {
    var selectedCase by remember { mutableStateOf<PostMoveAuditCase?>(null) }
    var closedTrades by remember { mutableStateOf<List<TradeEntity>>(emptyList()) }
    val auditRecords by viewModel.auditRecords.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
    val binanceTimedHistory by BinanceDataStore.timedPriceHistory.collectAsState()
    val fallbackTimedHistory by com.asc.markets.data.CombinedFallbackDataStore.timedPriceHistory.collectAsState()
    val timedHistory = remember(marketTimedHistory, binanceTimedHistory, fallbackTimedHistory) {
        marketTimedHistory + binanceTimedHistory + fallbackTimedHistory
    }
    val cases = remember(closedTrades, auditRecords, candidates, timedHistory) {
        PostMoveAuditStore.buildCases(closedTrades, auditRecords, candidates, timedHistory)
    }
    val ledgerCases = remember(cases) {
        cases.filter { it.source == PostMoveAuditSource.CLOSED_TRADE }
    }
    val avgSlippage = PostMoveAuditStore.averageSlippage(ledgerCases)
    val efficiency = PostMoveAuditStore.outcomeEfficiency(ledgerCases)

    LaunchedEffect(viewModel.tradeHistoryRepository) {
        closedTrades = withContext(Dispatchers.IO) {
            viewModel.tradeHistoryRepository?.getLast100Trades().orEmpty()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color.White.copy(alpha = 0.06f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("CLOSED TRADE POST-MOVE LEDGER", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("AVG SLIPPAGE", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(avgSlippage?.let { String.format("%.2f PIPS", it) } ?: "NOT CAPTURED", color = if (avgSlippage == null) SlateText else EmeraldSuccess, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("OUTCOME EFFICIENCY", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(efficiency?.let { "$it% SCORED" } ?: "PENDING", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
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
                items(ledgerCases, key = { it.id }) { case ->
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
