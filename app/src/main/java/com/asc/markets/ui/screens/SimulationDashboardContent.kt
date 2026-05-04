package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.Trade
import com.asc.markets.logic.PriceStreamManager
import com.trading.app.data.BinanceService
import com.trading.app.data.PaperTradingAccountSnapshot
import com.trading.app.data.PaperTradingSnapshotStore
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

enum class SimulationPageMode {
    AI,
    MY
}

private const val SimulationStartingBalance = 10000.0
private const val SimulationTradeSizeBtc = 0.01

private enum class PnlDetailTab(val label: String) {
    ACCOUNT("Account"),
    CLOSED_ORDERS("Closed Orders"),
    OPEN_ORDERS("Open Orders"),
    DEPOSIT_WITHDRAWAL("Deposit & Withdrawal")
}

data class SimulationDisplayTrade(
    val symbol: String,
    val side: String,
    val entryPrice: Double?,
    val currentPrice: Double?,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val volume: Double?,
    val leverage: String,
    val pnl: Double?,
    val pnlPct: Double?
)

@Composable
fun SimulationDashboardContent(
    pageMode: SimulationPageMode,
    accentColor: Color,
    engineEnabled: Boolean,
    onEngineToggle: (Boolean) -> Unit,
    tradingMode: String,
    onModeToggle: (String) -> Unit,
    onExecuteManualTrade: (type: String, entry: String, sl: String, tp: String) -> Unit,
    onViewAllHistory: () -> Unit = {},
    isChartExpanded: Boolean = false,
    onExpandToggle: (Boolean) -> Unit = {},
    chartState: SimulationEmbeddedChartState = rememberEmbeddedSimulationChartState(symbol = "BTCUSD", timeframe = "1d"),
    openTrades: List<Trade> = emptyList(),
    closedTrades: List<Trade> = emptyList(),
    onCloseAllTrades: () -> Unit = {},
    onCloseProfitableTrades: () -> Unit = {},
    onResetSimulation: () -> Unit = {},
    onExportPerformance: () -> Unit = {},
    exportStatus: String? = null
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "STRATEGY EVALUATION ENGINE", 
            color = Color.Gray, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Box(modifier = Modifier.size(6.dp).background(accentColor, androidx.compose.foundation.shape.CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REAL-TIME ANALYSIS", 
                color = accentColor, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (pageMode == SimulationPageMode.AI) {
            // Chart header with expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CHART VIEW", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onExpandToggle(true) }) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Expand",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXPAND", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            EmbeddedSimulationChartSection(chartState = chartState)
            Spacer(modifier = Modifier.height(24.dp))

            // AI Controls only
            SimulationControlsSectionV2(
                engineEnabled = engineEnabled,
                onEngineToggle = onEngineToggle,
                tradingMode = tradingMode,
                onModeToggle = onModeToggle,
                accentColor = accentColor
            )
        } else {
            MySimulationDashboard(
                accentColor = accentColor,
                onExpandChart = { onExpandToggle(true) },
                openTrades = openTrades,
                closedTrades = closedTrades,
                onExecuteManualTrade = onExecuteManualTrade,
                onCloseAllTrades = onCloseAllTrades,
                onCloseProfitableTrades = onCloseProfitableTrades,
                onResetSimulation = onResetSimulation,
                onExportPerformance = onExportPerformance,
                exportStatus = exportStatus
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        RiskManagementSectionV2()
        Spacer(modifier = Modifier.height(24.dp))
        RecentHistorySection(onViewAll = onViewAllHistory, trades = closedTrades)
    }
}

@Composable
fun MySimulationDashboard(
    accentColor: Color,
    onExpandChart: () -> Unit,
    openTrades: List<Trade>,
    closedTrades: List<Trade>,
    onExecuteManualTrade: (type: String, entry: String, sl: String, tp: String) -> Unit,
    onCloseAllTrades: () -> Unit,
    onCloseProfitableTrades: () -> Unit,
    onResetSimulation: () -> Unit,
    onExportPerformance: () -> Unit,
    exportStatus: String?
) {
    val snapshot = PaperTradingSnapshotStore.snapshot
    val currentPrice = rememberBtcLivePrice()
    val activeTrade = remember(snapshot, openTrades, currentPrice) {
        simulationDisplayTrade(snapshot, openTrades, currentPrice)
    }
    val allTrades = openTrades + closedTrades
    val wins = closedTrades.count { it.profitLoss > 0.0 }
    val winRate = if (closedTrades.isNotEmpty()) wins.toDouble() / closedTrades.size else 0.0
    val avgWin = closedTrades.filter { it.profitLoss > 0.0 }.map { it.profitLoss }.average().takeIf { it.isFinite() } ?: 0.0
    val avgLoss = closedTrades.filter { it.profitLoss < 0.0 }.map { it.profitLoss }.average().takeIf { it.isFinite() } ?: 0.0
    val bestTrade = closedTrades.maxOfOrNull { it.profitLoss } ?: 0.0
    val worstTrade = closedTrades.minOfOrNull { it.profitLoss } ?: 0.0

    Column {
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            PnlPositionOverviewCard(
                accentColor = accentColor,
                openTrades = openTrades,
                closedTrades = closedTrades,
                onExpand = onExpandChart
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ActiveTradeBox(accentColor = accentColor, title = "ACTIVE TRADE", trade = activeTrade)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("QUICK ACTIONS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val entry = currentPrice ?: return@Button
                            onExecuteManualTrade("BUY", rawPriceText(entry), rawPriceText(entry * 0.99), rawPriceText(entry * 1.02))
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                        enabled = currentPrice != null
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OPEN BUY", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val entry = currentPrice ?: return@Button
                            onExecuteManualTrade("SELL", rawPriceText(entry), rawPriceText(entry * 1.01), rawPriceText(entry * 0.98))
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C0B0C)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                        enabled = currentPrice != null
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OPEN SELL", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCloseAllTrades,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C0B0C)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
                        enabled = openTrades.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CLOSE ALL", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = onCloseProfitableTrades,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                        enabled = openTrades.any { (calculateTradePnl(it, currentPrice) ?: 0.0) > 0.0 }
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CLOSE +", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onExportPerformance,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    enabled = allTrades.isNotEmpty()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT PERFORMANCE DATA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                exportStatus?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = accentColor, fontSize = 10.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onResetSimulation,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C0B0C)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
                    enabled = allTrades.isNotEmpty()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESET SIMULATION", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("WIN RATE & LOSS RATE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(accentColor, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Win Rate", color = Color.Gray, fontSize = 9.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loss Rate", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                WinLossRateChart(accentColor = accentColor, closedTrades = closedTrades)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MY PERFORMANCE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Row 1: Total Trades, Win Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PerformanceMeter(
                        label = "TOTAL TRADES",
                        value = allTrades.size.toString(),
                        progress = (allTrades.size / 20f).coerceIn(0f, 1f),
                        accentColor = Color(0xFF6366F1),
                        startLabel = "0",
                        endLabel = "20+",
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceMeter(
                        label = "WIN RATE",
                        value = formatPercent(winRate * 100.0),
                        progress = winRate.toFloat(),
                        accentColor = accentColor,
                        startLabel = "0%",
                        endLabel = "100%",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Row 2: Avg Win, Avg Loss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PerformanceMeter(
                        label = "AVG WIN",
                        value = formatSignedMoney(avgWin),
                        progress = (abs(avgWin) / 250.0).toFloat().coerceIn(0f, 1f),
                        accentColor = accentColor,
                        startLabel = "Low",
                        endLabel = "High",
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceMeter(
                        label = "AVG LOSS",
                        value = formatSignedMoney(avgLoss),
                        progress = (abs(avgLoss) / 250.0).toFloat().coerceIn(0f, 1f),
                        accentColor = Color(0xFFEF4444),
                        startLabel = "Low",
                        endLabel = "High",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Row 3: Best Trade, Worst Trade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PerformanceMeter(
                        label = "BEST TRADE",
                        value = formatSignedMoney(bestTrade),
                        progress = (abs(bestTrade) / 250.0).toFloat().coerceIn(0f, 1f),
                        accentColor = accentColor,
                        startLabel = "Low",
                        endLabel = "High",
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceMeter(
                        label = "WORST TRADE",
                        value = formatSignedMoney(worstTrade),
                        progress = (abs(worstTrade) / 250.0).toFloat().coerceIn(0f, 1f),
                        accentColor = Color(0xFFEF4444),
                        startLabel = "Low",
                        endLabel = "High",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PnlPositionOverviewCard(
    accentColor: Color,
    openTrades: List<Trade> = emptyList(),
    closedTrades: List<Trade> = emptyList(),
    onExpand: () -> Unit = {}
) {
    val miniInfoBoxColor = Color.White.copy(alpha = 0.035f)
    val snapshot = PaperTradingSnapshotStore.snapshot
    val currentPrice = rememberBtcLivePrice()
    var selectedTab by remember { mutableStateOf(PnlDetailTab.ACCOUNT) }
    val openPnl = openTrades.sumOf { calculateTradePnl(it, currentPrice) ?: 0.0 }
    val realizedPnl = closedTrades.sumOf { it.profitLoss }
    val localBalance = SimulationStartingBalance + realizedPnl
    val usesLiveSnapshot = snapshot.hasLiveAccountData || snapshot.hasLiveTradeData
    val totalPnl = if (usesLiveSnapshot) snapshot.floatingPnl + snapshot.realizedPnl else openPnl + realizedPnl
    val balance = if (snapshot.hasLiveAccountData) snapshot.balance else localBalance
    val equity = if (usesLiveSnapshot) snapshot.equity else balance + openPnl
    val margin = if (usesLiveSnapshot) snapshot.margin else openTrades.sumOf { ((it.entryPrice * SimulationTradeSizeBtc) / 10.0) }
    val activeTrade = remember(snapshot, openTrades, currentPrice) { simulationDisplayTrade(snapshot, openTrades, currentPrice) }
    val positionValue = activeTrade?.let { (it.currentPrice ?: it.entryPrice ?: 0.0) * (it.volume ?: SimulationTradeSizeBtc) } ?: 0.0
    val now = System.currentTimeMillis()
    val dayPnl = realizedPnlForWindow(closedTrades, 24L * 60L * 60L * 1000L, now) + openPnl
    val weekPnl = realizedPnlForWindow(closedTrades, 7L * 24L * 60L * 60L * 1000L, now) + openPnl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(0.dp)
    ) {
        Surface(
            color = miniInfoBoxColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PnlSummaryItem("Total Pnl", formatSignedMoney(totalPnl), pnlColor(totalPnl, accentColor), Modifier.weight(1f))
                PnlSummaryItem("24-Hour PnL", formatSignedMoney(dayPnl), pnlColor(dayPnl, accentColor), Modifier.weight(1f))
                PnlSummaryItem("7-Day PnL", formatSignedMoney(weekPnl), pnlColor(weekPnl, accentColor), Modifier.weight(1f))
                PnlSummaryItem("Balance", formatMoney(balance), accentColor, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        LiveBtcUsdChartCard(accentColor = accentColor, onExpand = onExpand)
        Spacer(modifier = Modifier.height(22.dp))

        Text(
            "Perps Position Value ${formatMoney(positionValue)}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(miniInfoBoxColor)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PnlDetailTab.values().forEach { tab ->
                val label = if (tab == PnlDetailTab.OPEN_ORDERS) "Open Orders(${openTrades.size})" else tab.label
                PnlTabItem(label, selectedTab == tab) { selectedTab = tab }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (selectedTab) {
                PnlDetailTab.ACCOUNT -> AccountTabContent(
                    activeTrade = activeTrade,
                    equity = equity,
                    margin = margin,
                    openPnl = if (usesLiveSnapshot) snapshot.floatingPnl else openPnl,
                    accentColor = accentColor
                )
                PnlDetailTab.CLOSED_ORDERS -> TradesListContent(
                    trades = closedTrades,
                    emptyText = "No closed orders yet.",
                    currentPrice = currentPrice,
                    accentColor = accentColor,
                    closed = true
                )
                PnlDetailTab.OPEN_ORDERS -> TradesListContent(
                    trades = openTrades,
                    emptyText = "No open orders.",
                    currentPrice = currentPrice,
                    accentColor = accentColor,
                    closed = false
                )
                PnlDetailTab.DEPOSIT_WITHDRAWAL -> DepositWithdrawalContent(
                    startingBalance = SimulationStartingBalance,
                    realizedPnl = realizedPnl,
                    balance = balance,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun AccountTabContent(
    activeTrade: SimulationDisplayTrade?,
    equity: Double,
    margin: Double,
    openPnl: Double,
    accentColor: Color
) {
    Column(modifier = Modifier.padding(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PnlPositionMetric("Unrealized PNL", formatSignedMoney(openPnl), pnlColor(openPnl, accentColor), Modifier.weight(1f))
            PnlPositionMetric("Leverage", activeTrade?.leverage ?: "---", Color.White, Modifier.weight(1f), Alignment.End)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PnlPositionMetric("Amount", activeTrade?.volume?.let { "${String.format(Locale.US, "%.4f", it)} BTC" } ?: "---", Color.White, Modifier.weight(1f))
            PnlPositionMetric("Value", activeTrade?.let { formatMoney((it.currentPrice ?: it.entryPrice ?: 0.0) * (it.volume ?: 0.0)) } ?: "---", Color.White, Modifier.weight(1f), Alignment.CenterHorizontally)
            PnlPositionMetric("Equity", formatMoney(equity), accentColor, Modifier.weight(1f), Alignment.End)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PnlPositionMetric("Entry Price", formatNullablePrice(activeTrade?.entryPrice), Color.White, Modifier.weight(1f))
            PnlPositionMetric("Margin", formatMoney(margin), Color.White, Modifier.weight(1f), Alignment.CenterHorizontally)
            PnlPositionMetric("Liq.Price", liquidationEstimate(activeTrade), Color.Gray, Modifier.weight(1f), Alignment.End)
        }
    }
}

@Composable
private fun TradesListContent(
    trades: List<Trade>,
    emptyText: String,
    currentPrice: Double?,
    accentColor: Color,
    closed: Boolean
) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (trades.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                Text(emptyText, color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            trades.take(5).forEach { trade ->
                val pnl = if (closed) trade.profitLoss else calculateTradePnl(trade, currentPrice)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${trade.asset} ${trade.type}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Entry ${formatPrice(trade.entryPrice)}", color = Color.Gray, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(pnl?.let { formatSignedMoney(it) } ?: "---", color = pnlColor(pnl ?: 0.0, accentColor), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (closed) "CLOSED" else "OPEN", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DepositWithdrawalContent(
    startingBalance: Double,
    realizedPnl: Double,
    balance: Double,
    accentColor: Color
) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LedgerRow("Initial Deposit", formatMoney(startingBalance), accentColor)
        LedgerRow("Realized Settlement", formatSignedMoney(realizedPnl), pnlColor(realizedPnl, accentColor))
        LedgerRow("Current Balance", formatMoney(balance), accentColor)
    }
}

@Composable
private fun LedgerRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActiveTradeBox(
    accentColor: Color,
    title: String,
    trade: SimulationDisplayTrade? = null
) {
    val fallbackTrade = simulationDisplayTrade(PaperTradingSnapshotStore.snapshot, emptyList(), rememberBtcLivePrice())
    val displayTrade = trade ?: fallbackTrade
    val pnl = displayTrade?.pnl
    val tradeColor = if ((pnl ?: 0.0) >= 0.0) accentColor else Color(0xFFEF4444)
    Surface(
        color = Color.Black,
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
                Text(title, color = Color.Gray, fontSize = 10.sp)
                Surface(
                    color = if (displayTrade == null) Color(0xFF18181B) else tradeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (displayTrade == null) "NO POSITION" else displayTrade.side.uppercase(Locale.US),
                        color = if (displayTrade == null) Color.Gray else tradeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(displayTrade?.symbol ?: "---", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(pnl?.let { formatSignedMoney(it) } ?: "---", color = if (displayTrade == null) Color.Gray else tradeColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY", color = Color.Gray, fontSize = 10.sp)
                    Text(formatNullablePrice(displayTrade?.entryPrice), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("CURRENT", color = Color.Gray, fontSize = 10.sp)
                    Text(formatNullablePrice(displayTrade?.currentPrice), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("STOP LOSS", color = Color.Gray, fontSize = 10.sp)
                    Text(formatNullablePrice(displayTrade?.stopLoss), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("TAKE PROFIT", color = Color.Gray, fontSize = 10.sp)
                    Text(formatNullablePrice(displayTrade?.takeProfit), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PnlSummaryItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun rememberBtcLivePrice(): Double? {
    val prices by PriceStreamManager.priceUpdates.collectAsState()
    val pair by MarketDataStore.pairFlow("BTCUSD").collectAsState(initial = MarketDataStore.pairSnapshot("BTCUSD"))
    return prices["BTC/USDT"]
        ?: prices["BTC/USD"]
        ?: prices["BTCUSDT"]
        ?: prices["BTCUSD"]
        ?: pair?.price
}

fun simulationDisplayTrade(
    snapshot: PaperTradingAccountSnapshot,
    openTrades: List<Trade>,
    currentPrice: Double?
): SimulationDisplayTrade? {
    if (snapshot.currentTradeSymbol != null || snapshot.hasLiveTradeData) {
        return SimulationDisplayTrade(
            symbol = snapshot.currentTradeSymbol ?: "LIVE TRADE",
            side = snapshot.currentTradeSide ?: "OPEN",
            entryPrice = snapshot.currentTradeEntryPrice,
            currentPrice = snapshot.currentTradePrice ?: currentPrice,
            stopLoss = null,
            takeProfit = null,
            volume = snapshot.currentTradeVolume,
            leverage = "LIVE",
            pnl = snapshot.currentTradePnl,
            pnlPct = snapshot.currentTradePnlPct
        )
    }

    val trade = openTrades.firstOrNull() ?: return null
    return SimulationDisplayTrade(
        symbol = trade.asset,
        side = trade.type,
        entryPrice = trade.entryPrice,
        currentPrice = currentPrice,
        stopLoss = trade.stopLoss,
        takeProfit = trade.takeProfit,
        volume = SimulationTradeSizeBtc,
        leverage = "10x",
        pnl = calculateTradePnl(trade, currentPrice),
        pnlPct = calculateTradePnlPct(trade, currentPrice)
    )
}

fun calculateTradePnl(trade: Trade, currentPrice: Double?): Double? {
    val price = currentPrice ?: return null
    val direction = if (trade.type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
    return (price - trade.entryPrice) * SimulationTradeSizeBtc * direction
}

fun calculateTradePnlPct(trade: Trade, currentPrice: Double?): Double? {
    val price = currentPrice ?: return null
    if (trade.entryPrice == 0.0) return null
    val direction = if (trade.type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
    return ((price - trade.entryPrice) / trade.entryPrice) * 100.0 * direction
}

private fun realizedPnlForWindow(trades: List<Trade>, windowMillis: Long, nowMillis: Long): Double {
    return trades.filter { nowMillis - it.timestamp <= windowMillis }.sumOf { it.profitLoss }
}

fun formatMoney(value: Double): String = String.format(Locale.US, "$%,.2f", value)

fun formatSignedMoney(value: Double): String {
    val sign = if (value >= 0.0) "+" else "-"
    return "$sign${formatMoney(abs(value))}"
}

fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

fun formatPrice(value: Double): String = String.format(Locale.US, "%,.2f", value)

fun rawPriceText(value: Double): String = String.format(Locale.US, "%.2f", value)

fun formatNullablePrice(value: Double?): String = value?.let { formatPrice(it) } ?: "---"

fun pnlColor(value: Double, accentColor: Color): Color = if (value >= 0.0) accentColor else Color(0xFFEF4444)

private fun liquidationEstimate(trade: SimulationDisplayTrade?): String {
    val entry = trade?.entryPrice ?: return "---"
    val side = trade.side.uppercase(Locale.US)
    val estimate = if (side.contains("BUY") || side.contains("LONG")) entry * 0.9 else entry * 1.1
    return formatPrice(estimate)
}

@Composable
fun LiveBtcUsdChartCard(accentColor: Color, onExpand: () -> Unit = {}) {
    val pair by MarketDataStore.pairFlow("BTCUSD").collectAsState(initial = MarketDataStore.pairSnapshot("BTCUSD"))
    val rawHistory by MarketDataStore.historyFlow("BTCUSD").collectAsState(initial = MarketDataStore.historySnapshot("BTCUSD"))
    val marketOverviewPrices by PriceStreamManager.priceUpdates.collectAsState()
    var candleHistory by remember { mutableStateOf<List<Double>>(emptyList()) }
    val binanceService = remember {
        BinanceService(
            onQuoteUpdate = {},
            onHistoryUpdate = { _, history ->
                candleHistory = history
                    .sortedBy { it.time }
                    .map { it.close.toDouble() }
                    .filter { it.isFinite() && it > 0.0 }
                    .takeLast(100)
            }
        )
    }

    LaunchedEffect(binanceService) {
        binanceService.fetchHistory("BTCUSDT", "1h", null)
    }

    DisposableEffect(binanceService) {
        onDispose {
            binanceService.disconnect()
        }
    }

    val marketOverviewLivePrice = marketOverviewPrices["BTC/USDT"]
        ?: marketOverviewPrices["BTC/USD"]
        ?: marketOverviewPrices["BTCUSDT"]
        ?: marketOverviewPrices["BTCUSD"]
        ?: pair?.price
    val fallbackHistory = rawHistory.filter { it.isFinite() && it > 0.0 }.takeLast(100)
    val sourceHistory = if (candleHistory.size >= 2) candleHistory else fallbackHistory
    val liveHistory = if (sourceHistory.isNotEmpty() && marketOverviewLivePrice != null) {
        (sourceHistory.dropLast(1) + marketOverviewLivePrice).takeLast(100)
    } else {
        sourceHistory.takeLast(100)
    }
    val latestPrice = liveHistory.lastOrNull() ?: pair?.price
    val previousClose = liveHistory.getOrNull(liveHistory.lastIndex - 1)
    val changePercent = if (latestPrice != null && previousClose != null && previousClose > 0.0) {
        ((latestPrice - previousClose) / previousClose) * 100.0
    } else {
        pair?.changePercent
    }
    val hasLiveData = liveHistory.isNotEmpty()
    val priceText = latestPrice?.let { String.format(Locale.US, "%,.0f", it) } ?: "---"
    val changeText = if (hasLiveData && changePercent != null && changePercent.isFinite()) {
        String.format(Locale.US, "%+.2f%%", changePercent)
    } else {
        "---"
    }
    val changeColor = if ((changePercent ?: 0.0) >= 0.0) accentColor else Color(0xFFEF4444)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .border(1.dp, Color(0xFF1C1C1E), RoundedCornerShape(10.dp))
            .background(Color.Black, RoundedCornerShape(10.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFF59E0B), shape = CircleShape) {
                            Text("₿", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BTCUSD", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(priceText, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("USD", color = Color.LightGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(changeText, color = changeColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Expand",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (liveHistory.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val minPrice = liveHistory.minOrNull() ?: return@Canvas
                        val maxPrice = liveHistory.maxOrNull() ?: return@Canvas
                        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0
                        val topPadding = 10f
                        val bottomPadding = 8f
                        val chartHeight = size.height - topPadding - bottomPadding

                        fun xFor(index: Int): Float = size.width * index / (liveHistory.size - 1)
                        fun yFor(price: Double): Float = topPadding + chartHeight - (((price - minPrice) / range).toFloat() * chartHeight)

                        val linePath = Path()
                        val areaPath = Path()
                        liveHistory.forEachIndexed { index, price ->
                            val x = xFor(index)
                            val y = yFor(price)
                            if (index == 0) {
                                linePath.moveTo(x, y)
                                areaPath.moveTo(x, size.height)
                                areaPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                areaPath.lineTo(x, y)
                            }
                        }
                        areaPath.lineTo(size.width, size.height)
                        areaPath.close()

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(accentColor.copy(alpha = 0.42f), Color.Transparent),
                                startY = 0f,
                                endY = size.height
                            )
                        )
                        drawPath(linePath, color = accentColor.copy(alpha = 0.92f), style = Stroke(width = 4f))
                        drawCircle(accentColor, radius = 8f, center = Offset(size.width, yFor(liveHistory.last())))
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Waiting for live BTCUSD data", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PnlTabItem(label: String, selected: Boolean, onClick: () -> Unit = {}) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.035f) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            maxLines = 1
        )
    }
}

@Composable
fun PnlPositionMetric(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PerformanceMeter(
    label: String,
    value: String,
    progress: Float,
    accentColor: Color,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .background(accentColor, RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .offset(x = ((clamped * 300).dp).coerceAtMost(300.dp))
                    .size(8.dp)
                    .background(Color.White, CircleShape)
                    .align(Alignment.CenterStart)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, color = Color.Gray, fontSize = 9.sp)
            Text(endLabel, color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun WinLossRateChart(accentColor: Color, closedTrades: List<Trade>) {
    val winRateData = if (closedTrades.isEmpty()) {
        listOf(0f, 45f, 52f, 58f, 55f, 62f, 68f, 65f, 72f, 75f, 78f)
    } else {
        val wins = closedTrades.count { it.profitLoss > 0.0 }
        val total = closedTrades.size
        val currentWinRate = if (total > 0) (wins.toFloat() / total * 100f) else 0f
        val currentLossRate = 100f - currentWinRate
        val steps = 11
        val stepSize = total.toFloat() / (steps - 1).coerceAtLeast(1)
        (0 until steps).map { i ->
            val atTrade = (i * stepSize).toInt().coerceAtMost(total)
            val tradesUpTo = closedTrades.take(atTrade)
            val winsUpTo = tradesUpTo.count { it.profitLoss > 0.0 }
            if (tradesUpTo.isNotEmpty()) (winsUpTo.toFloat() / tradesUpTo.size * 100f) else 0f
        }
    }
    val lossRateData = if (closedTrades.isEmpty()) {
        listOf(0f, 55f, 48f, 42f, 45f, 38f, 32f, 35f, 28f, 25f, 22f)
    } else {
        winRateData.map { 100f - it }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val padding = 40f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2
            
            // Draw grid lines
            val gridColor = Color.White.copy(alpha = 0.1f)
            for (i in 0..5) {
                val y = padding + (chartHeight * i / 5f)
                drawLine(
                    gridColor,
                    start = Offset(padding, y),
                    end = Offset(size.width - padding, y),
                    strokeWidth = 1f
                )
            }
            
            // Draw win rate line (green)
            val winPath = Path()
            winRateData.forEachIndexed { index, rate ->
                val x = padding + (chartWidth * index / (winRateData.size - 1))
                val y = padding + chartHeight - (chartHeight * rate / 100f)
                if (index == 0) {
                    winPath.moveTo(x, y)
                } else {
                    winPath.lineTo(x, y)
                }
            }
            drawPath(winPath, color = accentColor, style = Stroke(width = 2f))
            
            // Draw win rate points
            winRateData.forEachIndexed { index, rate ->
                val x = padding + (chartWidth * index / (winRateData.size - 1))
                val y = padding + chartHeight - (chartHeight * rate / 100f)
                drawCircle(accentColor, radius = 4f, center = Offset(x, y))
            }
            
            // Draw loss rate line (red)
            val lossPath = Path()
            lossRateData.forEachIndexed { index, rate ->
                val x = padding + (chartWidth * index / (lossRateData.size - 1))
                val y = padding + chartHeight - (chartHeight * rate / 100f)
                if (index == 0) {
                    lossPath.moveTo(x, y)
                } else {
                    lossPath.lineTo(x, y)
                }
            }
            drawPath(lossPath, color = Color(0xFFEF4444), style = Stroke(width = 2f))
            
            // Draw loss rate points
            lossRateData.forEachIndexed { index, rate ->
                val x = padding + (chartWidth * index / (lossRateData.size - 1))
                val y = padding + chartHeight - (chartHeight * rate / 100f)
                drawCircle(Color(0xFFEF4444), radius = 4f, center = Offset(x, y))
            }
        }
        
        // Y-axis labels on the left
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("100%", color = Color.Gray, fontSize = 9.sp)
            Text("80%", color = Color.Gray, fontSize = 9.sp)
            Text("60%", color = Color.Gray, fontSize = 9.sp)
            Text("40%", color = Color.Gray, fontSize = 9.sp)
            Text("20%", color = Color.Gray, fontSize = 9.sp)
            Text("0%", color = Color.Gray, fontSize = 9.sp)
        }

        Text(
            "Rate (%)",
            color = Color.Gray,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-24).dp)
                .rotate(-90f)
        )
        
        // X-axis labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(9.dp))
                Text("20", color = Color.Gray, fontSize = 9.sp)
                Text("40", color = Color.Gray, fontSize = 9.sp)
                Text("60", color = Color.Gray, fontSize = 9.sp)
                Text("80", color = Color.Gray, fontSize = 9.sp)
                Text("100", color = Color.Gray, fontSize = 9.sp)
            }
        }
        
        // X-axis label at bottom center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp)
                .padding(top = 206.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text("Number of Trades", color = Color.Gray, fontSize = 9.sp)
        }
    }
}
