package com.asc.markets.data

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

enum class AppView {
    DASHBOARD, MARKETS, MACRO_STREAM, CHAT, ALERTS, NOTIFICATIONS,
    NEWS, CALENDAR, SENTIMENT, EDUCATION, PROFILE, SETTINGS,
    ANALYSIS_RESULTS, TRADE, TRADING_ASSISTANT, LIQUIDITY_HUB,
    BACKTEST, MULTI_TIMEFRAME, FULL_CHART, DIAGNOSTICS,
    POST_MOVE_AUDIT
}

data class ForexPair(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val category: MarketCategory = MarketCategory.FOREX
)

enum class MarketCategory {
    FOREX, CRYPTO, COMMODITIES, INDICES, STOCK
}

// Infer category from symbol heuristics
fun ForexPair.category(): MarketCategory = this.category

@Serializable
data class ForexDataPoint(
    @SerialName("time") val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
)

data class NewsItem(
    val id: String = UUID.randomUUID().toString(),
    val headline: String,
    val source: String,
    val timestamp: Long,
    val url: String? = null,
    val impact: String? = null
)

data class MarketSignal(
    val pair: String,
    val direction: String,
    val status: String,
    val entry: String,
    val stopLoss: String,
    val takeProfits: List<String>,
    val riskReward: String,
    val timeframe: String,
    val signalType: String,
    val confidenceScore: Int,
    val reasoning: String,
    val confluence: List<String>,
    val liquidityEvent: String,
    val newsWarning: String? = null
)

data class AutomatedTrade(
    val id: String,
    val pair: String,
    val side: String,
    val status: String,
    val entryPrice: String,
    val exitPrice: String? = null,
    val pnl: String? = null,
    val pnlAmount: Double? = null,
    val reasoning: String,
    val timestamp: Long = System.currentTimeMillis(),
    val preTradeContext: String = "",
    val postTradeOutcome: String = "",
    // Relay identity used for the dispatch (e.g., PRIMARY-UK-L14)
    val relayId: String = "PRIMARY-UK-L14",
    // Measured action latency in milliseconds
    val latencyMs: Double = 0.02
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" | "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class EconomicEvent(
    val event: String,
    val currency: String,
    val impact: String,
    val date: String,
    val time: String,
    val actual: String? = null,
    val estimate: String? = null,
    val previous: String? = null
)

enum class MacroEventStatus { UPCOMING, CONFIRMED }

enum class ImpactPriority { CRITICAL, HIGH, MEDIUM }

data class MacroEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val currency: String,
    val datetimeUtc: Long,
    val priority: ImpactPriority = ImpactPriority.MEDIUM,
    val status: MacroEventStatus = MacroEventStatus.UPCOMING,
    val source: String = "",
    val details: String = ""
)

@kotlinx.serialization.Serializable
data class AuditRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val headline: String,
    val impact: String,
    val confidence: Int,
    val assets: String,
    val status: String,
    val timeUtc: Long = System.currentTimeMillis(),
    val reasoning: String = "",
    val nodeId: String = "L14-UK",
    val integrityHash: String = "",
    var audited: Boolean = false
)
