package com.asc.markets.ui.screens.tradeDashboard.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.asc.markets.data.BinanceDataStore
import com.asc.markets.data.CombinedFallbackDataStore
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.ui.screens.tradeDashboard.model.*
import com.trading.app.data.PaperTradingAccountSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var accountInfo by mutableStateOf<AccountInfo?>(null)
        private set

    var positions by mutableStateOf<List<Position>>(emptyList())
        private set

    var closedPositions by mutableStateOf<List<HistoricalTrade>>(emptyList())
        private set

    var currentPrice by mutableStateOf<PriceData?>(null)
        private set

    var candleData by mutableStateOf<List<CandleData>>(emptyList())
        private set

    var alerts by mutableStateOf<List<AIAlert>>(emptyList())
        private set

    var advisory by mutableStateOf<AIAdvisory?>(null)
        private set

    var marketIntel by mutableStateOf<AIMarketIntelligence?>(null)
        private set

    var calendarEvents by mutableStateOf<List<EconomicEvent>>(emptyList())
        private set

    var selectedSymbol by mutableStateOf("EURUSD")
        private set

    var selectedTimeframe by mutableStateOf("H1")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var aiSettings by mutableStateOf(AISettings())
        private set

    var decisionNarrative by mutableStateOf<String?>(null)
        private set

    init {
        observeMarketData()
    }

    fun updateAccountSnapshot(snapshot: PaperTradingAccountSnapshot) {
        val hasAccount = snapshot.hasLiveAccountData ||
            snapshot.balance != 0.0 ||
            snapshot.equity != 0.0 ||
            snapshot.margin != 0.0 ||
            snapshot.freeMargin != 0.0

        accountInfo = if (hasAccount || snapshot.activeTrades > 0 || snapshot.activeOrders > 0) {
            AccountInfo(
                balance = snapshot.balance,
                equity = snapshot.equity,
                margin = snapshot.margin + snapshot.ordersMargin,
                freeMargin = snapshot.freeMargin,
                marginLevel = snapshot.marginLevel,
                profit = snapshot.floatingPnl + snapshot.realizedPnl
            )
        } else {
            null
        }

        positions = snapshot.toOpenPositions()
        snapshot.currentTradeSymbol?.takeIf { it.isNotBlank() }?.let { updateSelectedSymbol(it) }
    }

    fun updateClosedTrades(trades: List<TradeEntity>) {
        closedPositions = trades.map { it.toHistoricalTrade() }
    }

    fun updateDeployments(decisions: List<FinalDecisionItem>) {
        val actionable = decisions.filter { it.asset_1?.isNotBlank() == true }
        alerts = actionable.take(6).mapIndexed { index, item -> item.toAlert(index) }
        advisory = actionable.firstOrNull()?.toAdvisory()
        marketIntel = actionable.firstOrNull()?.toMarketIntel()
        decisionNarrative = actionable.firstOrNull()?.toNarrative()
    }

    fun updateSelectedSymbol(symbol: String) {
        val normalized = symbol.ifBlank { selectedSymbol }
        selectedSymbol = normalized

        val pair = livePairSnapshot(normalized)
        currentPrice = if (pair != null && pair.price.isFinite() && pair.price > 0.0) {
            PriceData(
                symbol = pair.symbol.replace("/", ""),
                bid = pair.price,
                ask = pair.price,
                spread = 0.0,
                change = pair.changePercent
            )
        } else {
            null
        }

        val history = (if (BinanceDataStore.isUsdtSymbol(normalized)) {
            BinanceDataStore.historySnapshot(normalized)
        } else {
            MarketDataStore.historySnapshot(normalized)
        })
            .filter { it.isFinite() && it > 0.0 }
            .takeLast(80)

        candleData = if (history.size >= 2) {
            val step = timeframeToMillis(selectedTimeframe)
            val now = System.currentTimeMillis()
            history.mapIndexed { index, close ->
                val open = if (index == 0) history.first() else history[index - 1]
                CandleData(
                    time = now - ((history.lastIndex - index).toLong() * step),
                    open = open,
                    high = max(open, close),
                    low = min(open, close),
                    close = close,
                    volume = 0.0
                )
            }
        } else {
            emptyList()
        }
    }

    fun onTimeframeSelected(timeframe: String) {
        selectedTimeframe = timeframe
        updateSelectedSymbol(selectedSymbol)
    }

    fun adjustStopLoss(ticketId: String, newSL: Double) {
        positions = positions.map { if (it.ticketId == ticketId) it.copy(sl = newSL) else it }
    }

    fun adjustTakeProfit(ticketId: String, newTP: Double) {
        positions = positions.map { if (it.ticketId == ticketId) it.copy(tp = newTP) else it }
    }

    fun updateAISettings(settings: AISettings) {
        aiSettings = settings
    }

    private fun observeMarketData() {
        scope.launch {
            combine(
                MarketDataStore.allPairs,
                BinanceDataStore.allPairs,
                CombinedFallbackDataStore.allPairs
            ) { marketPairs, binancePairs, fallbackPairs ->
                marketPairs + binancePairs + fallbackPairs
            }.collect { pairs ->
                val livePair = pairs.firstOrNull { it.price.isFinite() && it.price > 0.0 }
                if (currentPrice == null && livePair != null) {
                    updateSelectedSymbol(livePair.symbol)
                } else {
                    updateSelectedSymbol(selectedSymbol)
                }
            }
        }
    }

    private fun PaperTradingAccountSnapshot.toOpenPositions(): List<Position> {
        val symbol = currentTradeSymbol?.takeIf { it.isNotBlank() } ?: return emptyList()
        val entry = currentTradeEntryPrice ?: return emptyList()
        val price = currentTradePrice ?: livePairSnapshot(symbol)?.price ?: entry
        val side = currentTradeSide.orEmpty().uppercase(Locale.US)
        val type = if (side.contains("SELL") || side.contains("SHORT")) TradeType.SELL else TradeType.BUY
        val profit = currentTradePnl ?: 0.0
        val volume = currentTradeVolume ?: 0.0

        return listOf(
            Position(
                id = "live-$symbol",
                ticketId = "LIVE",
                symbol = symbol,
                type = type,
                volume = volume,
                openPrice = entry,
                currentPrice = price,
                tp = null,
                sl = null,
                swap = 0.0,
                commission = 0.0,
                profit = profit,
                healthScore = healthScore(profit, openRisk)
            )
        )
    }

    private fun livePairSnapshot(symbol: String) =
        BinanceDataStore.pairSnapshot(symbol) ?: MarketDataStore.pairSnapshot(symbol)

    private fun TradeEntity.toHistoricalTrade(): HistoricalTrade {
        val type = if (direction.uppercase(Locale.US).contains("SELL") || direction.uppercase(Locale.US).contains("SHORT")) {
            TradeType.SELL
        } else {
            TradeType.BUY
        }
        val time = formatTime(timestamp)
        return HistoricalTrade(
            id = id.toString(),
            ticketId = id.toString(),
            symbol = asset,
            type = type,
            volume = 0.0,
            openPrice = entryPrice,
            closePrice = exitPrice,
            openTime = time,
            closeTime = time,
            swap = 0.0,
            commission = 0.0,
            profit = pnl
        )
    }

    private fun FinalDecisionItem.toAlert(index: Int): AIAlert {
        val risk = final_risk_pct ?: recommended_risk_pct ?: 0.0
        val severity = when {
            risk >= 2.0 || correlation_warning?.isNotBlank() == true -> AlertSeverity.CRITICAL
            risk >= 1.0 || journal_priority?.equals("HIGH", ignoreCase = true) == true -> AlertSeverity.WARNING
            else -> AlertSeverity.INFO
        }
        val direction = journal_direction ?: "UNSPECIFIED"
        val label = portfolio_decision_label ?: journal_label ?: "ASC DECISION"
        return AIAlert(
            id = "${asset_1.orEmpty()}-$index",
            timestamp = journal_timestamp?.takeLast(8) ?: "--:--:--",
            message = "${asset_1.orEmpty()} $direction • $label",
            severity = severity,
            details = portfolio_decision_reason ?: correlation_warning ?: confluence_label
        )
    }

    private fun FinalDecisionItem.toAdvisory(): AIAdvisory {
        val direction = journal_direction.orEmpty().uppercase(Locale.US)
        val confidence = percentInt(direction_confidence ?: journal_score ?: confluence_score ?: pre_move_ai_score)
        val risk = final_risk_pct ?: recommended_risk_pct ?: 0.0
        return AIAdvisory(
            bias = when {
                direction.contains("BUY") || direction.contains("LONG") -> Bias.BULLISH
                direction.contains("SELL") || direction.contains("SHORT") -> Bias.BEARISH
                else -> Bias.NEUTRAL
            },
            confidence = confidence,
            suggestedSL = 0.0,
            suggestedTP = 0.0,
            riskLevel = when {
                risk >= 2.0 -> RiskLevel.HIGH
                risk >= 1.0 -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }
        )
    }

    private fun FinalDecisionItem.toMarketIntel(): AIMarketIntelligence {
        val drivers = listOfNotNull(
            correlation_warning,
            feeder_volatility_reason,
            structure_label,
            chart_context_label,
            confluence_label
        ).filter { it.isNotBlank() }

        return AIMarketIntelligence(
            trendStrength = percentInt(mtf_alignment_score ?: directional_score ?: trendScoreFromState(trend_state)),
            volatilityScore = percentInt(feeder_volatility_score ?: atr_ratio ?: vol_ratio ?: burst_ratio),
            momentumScore = percentInt(ignition_probability ?: expansion_probability ?: entry_quality_score),
            marketPhase = pre_move_ai_phase ?: regime_state ?: trend_state ?: "WAITING",
            phaseDescription = portfolio_decision_reason,
            timeframeTrends = null,
            volatilityDrivers = drivers
        )
    }

    private fun FinalDecisionItem.toNarrative(): String {
        val lines = listOfNotNull(
            asset_1?.let { "Asset: $it" },
            journal_direction?.let { "Direction: $it" },
            portfolio_decision_label?.let { "Decision: $it" },
            portfolio_deployment_bucket?.let { "Bucket: $it" },
            final_risk_pct?.let { "Final risk: ${String.format(Locale.US, "%.2f", it)}%" },
            final_position_scale?.let { "Position scale: ${String.format(Locale.US, "%.2f", it)}" },
            pre_move_ai_phase?.let { "Pre-move phase: $it" },
            portfolio_decision_reason
        )
        return lines.joinToString("\n")
    }

    private fun healthScore(profit: Double, openRisk: Double): Int {
        val denominator = openRisk.takeIf { it > 0.0 } ?: abs(profit).coerceAtLeast(1.0)
        return ((1.0 + (profit / denominator)).coerceIn(0.0, 1.0) * 100.0).toInt()
    }

    private fun percentInt(value: Double?): Int {
        val raw = value ?: return 0
        val normalized = if (raw <= 1.0) raw * 100.0 else raw
        return normalized.toInt().coerceIn(0, 100)
    }

    private fun trendScoreFromState(state: String?): Double? {
        return when (state?.uppercase(Locale.US)) {
            "UPTREND", "DOWNTREND", "TRENDING" -> 75.0
            "SIDEWAYS", "RANGE" -> 40.0
            else -> null
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestamp))
    }

    private fun timeframeToMillis(timeframe: String): Long {
        return when (timeframe) {
            "M1" -> 60_000L
            "M5" -> 300_000L
            "M15" -> 900_000L
            "H4" -> 14_400_000L
            "D1" -> 86_400_000L
            else -> 3_600_000L
        }
    }
}