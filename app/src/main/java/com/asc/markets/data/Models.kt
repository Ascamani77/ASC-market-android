package com.asc.markets.data

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

enum class AppView {
    DASHBOARD, MARKET_WATCH, MARKETS, MACRO_STREAM, INTELLIGENCE_STREAM, CHAT, ALERTS, NOTIFICATIONS,
    HOME_ALERTS,
    NEWS, CALENDAR, SENTIMENT, EDUCATION, PROFILE, SETTINGS,
    ANALYSIS_RESULTS, TRADE, TRADING_ASSISTANT, LIQUIDITY_HUB,
    BACKTEST, MULTI_TIMEFRAME, FULL_CHART, DIAGNOSTICS,
    POST_MOVE_AUDIT, DATA_HUB, DATA_VAULT, PORTFOLIO_MANAGER, TRADE_RECONSTRUCTION, MARKET_VIEW
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
    FOREX, CRYPTO, COMMODITIES, INDICES, STOCK, BONDS, FUTURES
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
    val id: String? = null,
    val title: String = "",
    val date: String = "",
    val asset_class: String = "",
    val event_type: String = "",
    val assets_affected: List<String> = emptyList(),
    val severity: String = "",
    val timestamp_utc: Long = 0,
    val source: String = "",
    val source_type: String = "",
    val actual: Any? = null,
    val estimate: Any? = null,
    val previous: Any? = null,
    val unit: String? = null,
    val surprise_level: String? = null,
    val surprise_delta: Double? = null,
    val execution_regime: String = "",
    val visual_state: String = "",
    val persistence_count: Int = 0,
    val transition_status: String = "",
    val volatility_confirmed: Boolean = false,
    val safety_gate: Boolean = false,
    val unlock_state: String = "",
    val gate_release_time: Long? = null,
    val hard_unlock_time: Long? = null,
    val narrative_summary: String? = null,
    val confidence_score: Int = 0,
    val base_confidence: Int = 0,
    val conviction_tier: String = "",
    val correlation_heat: Int = 0,
    val liquidity_depth: Int = 0,
    val sentiment_divergence: Boolean = false,
    val neutral_drivers: List<String> = emptyList(),
    val transition_triggers: List<TransitionTrigger> = emptyList(),
    val allowed_tactics: List<String> = emptyList(),
    val ebc_status: String = "",
    val friction_coefficient: Double = 0.0,
    val alpha_erosion: Double = 0.0,
    // Legacy fields for backward compatibility
    val event: String? = null,
    val currency: String? = null,
    val impact: String? = null,
    val time: String? = null,
    val type: String? = null,
    val asset: String? = null,
    val narrative: String? = null
)

data class TransitionTrigger(
    val label: String = "",
    val status: String = ""
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

// Helper to produce a consistent display title: Full descriptive name followed by abbreviation in brackets.
fun MacroEvent.displayTitle(): String {
    // Known abbreviation -> full name map
    val known = mapOf(
        "NFP" to "Non-Farm Payrolls",
        "CPI" to "Consumer Price Index",
        "ECB" to "European Central Bank",
        "PMI" to "Purchasing Managers' Index",
        "BLS" to "Bureau of Labor Statistics Employment Report",
        "PPI" to "Producer Price Index",
        "FED" to "Federal Reserve",
        "BOE" to "Bank of England",
        "ISM" to "ISM Services PMI",
        "GDP" to "Gross Domestic Product"
    )

    // If the title already contains brackets, assume it's formatted
    if (title.contains("[") && title.contains("]")) return title

    // Look for a token that is an uppercase abbreviation (2-4 letters)
    val tokens = title.split(Regex("""[ \-|,]+"""))
    val abbr = tokens.find { it.matches(Regex("^[A-Z]{2,4}")) }
    if (abbr != null) {
        val full = known[abbr] ?: tokens.filter { it != abbr }.joinToString(" ")
        // If full is the same as tokens without abbr and it's already descriptive, return "full [abbr]"
        return if (known.containsKey(abbr)) {
            // Prepend any geographic prefix (e.g., US) from the title
            val prefix = tokens.firstOrNull { it.matches(Regex("^[A-Z]{2}")) && it != abbr }
            if (prefix != null) "$prefix $full [$abbr]" else "$full [$abbr]"
        } else {
            // Fall back to original title with brackets
            "${title.replace(abbr, "").trim()} [$abbr]"
        }
    }

    // No abbreviation found — return title as-is
    return title
}

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

/**
 * Utility to normalize event titles into the format: "Full Descriptive Name [ABBR]".
 * If the title already contains bracketed text it is returned unchanged.
 */
fun formatEventTitle(raw: String): String {
    if (raw.contains("[")) return raw
    val mappings = mapOf(
        "NFP" to "Non-Farm Payrolls",
        "CPI" to "Consumer Price Index",
        "PMI" to "Purchasing Managers' Index",
        "ECB" to "European Central Bank",
        "ISM" to "ISM Services PMI",
        "GDP" to "Gross Domestic Product",
        "BLS" to "Bureau of Labor Statistics Employment Report",
        "PPI" to "Producer Price Index",
        "FED" to "Federal Reserve",
        "BOE" to "Bank of England"
    )

    val title = raw.trim()

    for ((abbr, full) in mappings) {
        // match abbreviation as a standalone word
        val abbrRegex = Regex("""\b${Regex.escape(abbr)}\b""", RegexOption.IGNORE_CASE)
        val fullRegex = Regex(Regex.escape(full), RegexOption.IGNORE_CASE)

        if (abbrRegex.containsMatchIn(title)) {
            // replace the abbreviation occurrence with the full name + [ABBR]
            return title.replace(abbrRegex, "$full [$abbr]")
        }

        if (fullRegex.containsMatchIn(title)) {
            // if full name exists but abbreviation not present, append abbreviation
            return if (title.contains("[")) title else "$title [$abbr]"
        }
    }

    // No mapping found — return as-is
    return title
}
