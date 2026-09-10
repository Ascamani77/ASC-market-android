package com.asc.markets.data

import java.util.UUID
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

enum class AppView {
    DASHBOARD, MARKET_WATCH, MARKETS, CHAT, ALERTS, NOTIFICATIONS, PUSH_SETTINGS,
    HOME_ALERTS, MY_ALERTS,
    NEWS, ANALYSIS_OPINION, CALENDAR, STREAM, MACRO_STREAM, SENTIMENT, EDUCATION, PROFILE, SETTINGS,
    ANALYSIS_RESULTS, TRADE, TRADING_ASSISTANT, LIQUIDITY_HUB,
    BACKTEST, MULTI_TIMEFRAME, FULL_CHART, DIAGNOSTICS,
    POST_MOVE_AUDIT, DATA_HUB, DATA_VAULT, PORTFOLIO_MANAGER, TRADE_RECONSTRUCTION, MARKET_VIEW,
    TRADE_DASHBOARD, SIDEBAR_PAGE, WATCHLIST, SIMULATION, MY_SIMULATION, AI_TERMINAL,
    PAPER_TRADING, QUOTES, MARKET_STATUS, CHART_ANALYSIS, SCALPING, CHART_DISPLAY_SETTINGS,
    ASSET_DETAIL, AUTO_TRADE, AI_SETTINGS, QUALIFIED_SETUP
}

fun AppView.toAiContextLabel(): String {
    return name.split('_').joinToString(" ") { segment ->
        when (segment.uppercase(Locale.US)) {
            "AI" -> "AI"
            "USD" -> "USD"
            "USDT" -> "USDT"
            "BTC" -> "BTC"
            "ETH" -> "ETH"
            "XAU" -> "XAU"
            "XAG" -> "XAG"
            "SPX" -> "SPX"
            "DXY" -> "DXY"
            else -> segment.lowercase(Locale.US).replaceFirstChar { ch ->
                ch.titlecase(Locale.US)
            }
        }
    }
}

fun buildAiPageAccessPermissions(): Map<String, Boolean> {
    return AppView.values().associate { it.toAiContextLabel() to true }
}

data class ForexPair(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val category: MarketCategory = MarketCategory.FOREX,
    val eaConfidence: Double = 0.0, // EA AI confidence score
    val regimeConfidence: String = "NONE", // EA regime confidence level
    val alignmentPercentage: Double = 0.0, // MTF alignment percentage from chart
    val eaDirection: String = "WAIT" // EA direction (BUY/SELL/WAIT)
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

enum class ImpactPriority { CRITICAL, HIGH, MEDIUM, LOW }

data class MacroEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val currency: String,
    val datetimeUtc: Long,
    val priority: ImpactPriority = ImpactPriority.MEDIUM,
    val status: MacroEventStatus = MacroEventStatus.UPCOMING,
    val source: String = "",
    val details: String = "",
    val actual: String = "",
    val forecast: String = "",
    val previous: String = ""
)

// Helper to produce a consistent display title: Full descriptive name followed by abbreviation in brackets.
fun MacroEvent.displayTitle(): String {
    // Known abbreviation -> full name map
    val known = mapOf(
        "NFP" to "Non-Farm Payrolls",
        "CPI" to "Consumer Price Index",
        "ECB" to "European Bank",
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
    var audited: Boolean = false,
    val direction: String? = null,
    val riskPct: Double? = null,
    val deploymentLabel: String? = null
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
        "ECB" to "European Bank",
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

data class NewsStory(
    val title: String = "",
    val source: String = "",
    val time: String = "",
    val impact: String = "MEDIUM",
    val category: String = "",
    val isSurprise: Boolean = false,
    // Alternative field names some servers might use
    val headline: String? = null,
    val author: String? = null,
    val publishedAt: String? = null,
    val url: String? = null,
    val description: String? = null
) {
    // Fallback to alternative field names if primary ones are empty
    fun getTitleOrDefault(): String = title.takeIf { it.isNotEmpty() } ?: (headline ?: "")
    fun getSourceOrDefault(): String = source.takeIf { it.isNotEmpty() } ?: (author ?: "")
    fun getTimeOrDefault(): String = time.takeIf { it.isNotEmpty() } ?: (publishedAt ?: "")
}

data class WatchlistItem(
    val id: String = UUID.randomUUID().toString(),
    val assetName: String,
    val status: String,
    val confidence: Double = 0.0,  // Changed to Double for ASC EA compatibility
    val newsRisk: String,
    val moveProbability: Int,
    val priority: Int,
    val preMoveSignal: String,
    val volatilityScore: Int,
    val triggerEvent: String = "",
    val timeToEvent: String = "",
    val price: Double = 0.0,
    val changePercent: Double = 0.0,
    val category: MarketCategory = MarketCategory.FOREX,
    val rationale: String = "",
    val isNew: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    
    // ASC EA Integration Fields
    val alignment_percentage: Double = 0.0,
    val structure_score: Double = 0.0,
    val regime_state: String = "",
    val trend_state: String = "",
    val volatility_state: String = "",
    val liquidity_bias: String = "",
    val structure_bias: String = "",
    val indicator_bias: String = "",
    
    // Zone Context Fields
    val zone_context_valid: Boolean = false,
    val zone_context_type: String = "",
    val zone_relationship: String = "",
    val current_zones: String = "",
    val target_zone: String = "",
    val zone_distance_pips: Double = 0.0,
    
    // Exhaustion Analysis Fields
    val exhaustion_detected: Boolean = false,
    val exhaustion_bias: String = "",
    val exhaustion_score: Double = 0.0,
    val rsi_value: Double = 50.0
)


// ============================================
// ASC EA DATA MODELS (ai_signals_mq5.json)
// ============================================

@Serializable
data class ASCSignalData(
    val timestamp: Long = 0,
    val asset: String = "",
    val direction: String = "WAIT",
    val confidence: Double = 0.0,
    val alignment_percentage: Double = 0.0,
    val regime: ASCRegimeData? = null,
    val volatility: ASCVolatilityData? = null,
    val liquidity: ASCLiquidityData? = null,
    val structure: ASCStructureData? = null,
    val indicators: ASCIndicatorsData? = null,
    val session: ASCSessionData? = null,
    val entry: ASCEntryData? = null,
    val trade_params: ASCTradeParams? = null,
    val pattern_detection: ASCPatternDetection? = null,
    val chart_panel: ASCChartPanelData? = null,
    val validation: ASCValidationData? = null,
    val quality: ASCQualityData? = null,
    val macro: ASCMacroData? = null,
    val htf_context: ASCHTFContextData? = null,
    val confluence: ASCConfluenceData? = null,
    val chart_panels: ASCChartPanelsData? = null,
    
    // Zone Context Fields
    val zone_context_valid: Boolean = false,
    val zone_context_type: String = "",
    val zone_relationship: String = "",
    val current_zones: String = "",
    val target_zone: String = "",
    val zone_distance_pips: Double = 0.0,
    
    // Exhaustion Fields
    val exhaustion_detected: Boolean = false,
    val exhaustion_bias: String = "",
    val exhaustion_score: Double = 0.0
)

@Serializable
data class ASCRegimeData(
    val state: String = "UNKNOWN",
    val trend: String = "NEUTRAL",
    val score: Double = 0.0,
    val confidence: String = "NONE",
    val reason: String = ""
)

@Serializable
data class ASCVolatilityData(
    val state: String = "NORMAL",
    val score: Double = 0.0,
    val atr_ratio: Double = 0.0,
    val bias: String = "NEUTRAL"
)

@Serializable
data class ASCLiquidityData(
    val state: String = "NEUTRAL",
    val bias: String = "NEUTRAL",
    val score: Double = 0.0,
    val fvg_bull: Boolean = false,
    val fvg_bear: Boolean = false,
    val sweep_high: Boolean = false,
    val sweep_low: Boolean = false,
    val bos_bull: Boolean = false,
    val bos_bear: Boolean = false
)

@Serializable
data class ASCStructureData(
    val bias: String = "NEUTRAL",
    val score: Double = 0.0,
    val quality: Double = 0.0
)

@Serializable
data class ASCIndicatorsData(
    val bias: String = "NEUTRAL",
    val score: Double = 0.0,
    val rsi: Double = 50.0,
    val macd_main: Double = 0.0,
    val macd_signal: Double = 0.0,
    val stochastic: Double = 50.0,
    val adx: Double = 0.0
)

@Serializable
data class ASCSessionData(
    val name: String = "OFF_HOURS",
    val score: Double = 0.0,
    val high_liquidity: Boolean = false
)

@Serializable
data class ASCEntryData(
    val state: String = "NO_ENTRY",
    val style: String = "NO_TRADE",
    val score: Double = 0.0,
    val confidence: Double = 0.0,
    val reason: String = "",
    val quality: String = "LOW"
)

@Serializable
data class ASCTradeParams(
    val stop_loss: Double = 0.0,
    val take_profit: Double = 0.0,
    val risk_pct: Double = 0.0
)

@Serializable
data class ASCPatternDetection(
    val detected_pattern: String = "",
    val pattern_confidence: Double = 0.0
)

@Serializable
data class ASCChartPanelData(
    val validator_active: Boolean = false,
    val validator_allowed: Boolean = false,
    val validator_direction: String = "",
    val validator_pwin: Double = 0.0,
    val validator_gate: Double = 0.0,
    val quality_tier: String = "NONE",
    val votes: ASCVotesData? = null,
    val confidence_trend: ASCConfidenceTrendData? = null
)

@Serializable
data class ASCVotesData(
    val smc_bull: Int = 0,
    val smc_bear: Int = 0,
    val ai_bull: Int = 0,
    val ai_bear: Int = 0,
    val total_bull: Int = 0,
    val total_bear: Int = 0,
    val win_pct: Double = 0.0,
    val direction: String = "NONE"
)

@Serializable
data class ASCConfidenceTrendData(
    val indicator: String = "STABLE",
    val strength: Double = 0.0
)

@Serializable
data class ASCValidationData(
    val state: String = "UNKNOWN",
    val score: Double = 0.0,
    val confidence: Double = 0.0,
    val spread_ok: Boolean = false,
    val timing_ok: Boolean = false,
    val rr_ok: Boolean = false,
    val market_ok: Boolean = false,
    val mtf_ok: Boolean = false
)

@Serializable
data class ASCQualityData(
    val grade: String = "F",
    val score: Double = 0.0,
    val confidence: Double = 0.0,
    val strengths: Int = 0,
    val weaknesses: Int = 0,
    val total_feeders: Int = 0,
    val top_strength: String = "",
    val top_weakness: String = ""
)

@Serializable
data class ASCMacroData(
    val trend: String = "UNKNOWN",
    val score: Double = 0.0,
    val confidence: Double = 0.0,
    val reason: String = "",
    val d1_trend_strength: Double = 0.0,
    val w1_trend_strength: Double = 0.0,
    val bullish_flag: Boolean = false,
    val bearish_flag: Boolean = false
)

@Serializable
data class ASCHTFContextData(
    val context: String = "UNKNOWN",
    val bias: String = "NEUTRAL",
    val score: Double = 0.0,
    val confidence: Double = 0.0,
    val reason: String = "",
    val support: Double = 0.0,
    val resistance: Double = 0.0,
    val near_support: Boolean = false,
    val near_resistance: Boolean = false
)

@Serializable
data class ASCConfluenceData(
    val state: String = "NO_CONFLUENCE",
    val score: Double = 0.0,
    val confidence: Double = 0.0,
    val reason: String = "",
    val bull_confluences: Int = 0,
    val bear_confluences: Int = 0
)

@Serializable
data class ASCChartPanelsData(
    val news_sentiment: String = "",
    val market_phase: String = "",
    val smc_details: String = "",
    val momentum: String = "",
    val market_structure: String = "",
    val time_filter: String = "",
    val win_probability: String = "",
    val risk_reward: String = "",
    val daily_pnl: String = "",
    val session_details: String = "",
    val spread_monitor: String = "",
    val correlation_alert: String = "",
    val cross_correlation: String = "",
    val session_intelligence: String = "",
    val portfolio_management: String = "",
    val criteria_score: String = "",
    val exhaustion_analysis: String = "",
    val zone_context: String = "",
    val equity_info: String = "",
    val risk_management: String = "",
    val smc_status: String = "",
    val module_reliability: String = ""
)

// Helper function to load ASC signal data from MT5 JSON file
fun loadASCSignalData(symbol: String): ASCSignalData? {
    return try {
        // Construct path to MT5 Files directory
        val userHome = System.getProperty("user.home")
        val mt5FilesPath = "$userHome\\AppData\\Roaming\\MetaQuotes\\Terminal\\D0E8209F77C8CF37AD8BF550E51FF075\\MQL5\\Files\\ai_signals_mq5.json"
        
        val file = java.io.File(mt5FilesPath)
        if (!file.exists()) {
            android.util.Log.w("ASC_EA", "Signal file not found: $mt5FilesPath")
            return null
        }
        
        val jsonString = file.readText()
        if (jsonString.isBlank()) {
            android.util.Log.w("ASC_EA", "Signal file is empty")
            return null
        }
        
        // Parse JSON using kotlinx.serialization
        val json = kotlinx.serialization.json.Json { 
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val signalData = json.decodeFromString<ASCSignalData>(jsonString)
        
        // Optional: Filter by symbol if needed (currently MT5 writes one signal per file)
        // For multi-symbol support, you'd need to modify MT5 EA to write array of signals
        
        android.util.Log.d("ASC_EA", "Loaded signal for ${signalData.asset}: ${signalData.direction} @ ${signalData.confidence}")
        return signalData
        
    } catch (e: Exception) {
        android.util.Log.e("ASC_EA", "Failed to load ASC signal data: ${e.message}", e)
        return null
    }
}


// Convert ASC Signal Data to WatchlistItem
fun ASCSignalData.toWatchlistItem(): WatchlistItem {
    return WatchlistItem(
        id = "${this.asset}_${this.timestamp}",
        assetName = this.asset,
        status = when (this.direction) {
            "BUY", "SELL" -> "READY"
            "WAIT" -> "WAITING"
            "NONE" -> "NO_SIGNAL"
            else -> "ANALYZING"
        },
        confidence = this.confidence,
        newsRisk = "Low", // TODO: Add news risk to ASC EA
        moveProbability = (this.confidence * 100).toInt(),
        priority = when {
            this.confidence >= 0.7 -> 1
            this.confidence >= 0.5 -> 2
            else -> 3
        },
        preMoveSignal = this.regime?.trend ?: "NEUTRAL",
        volatilityScore = ((this.volatility?.score ?: 0.0) * 100).toInt(),
        triggerEvent = this.pattern_detection?.detected_pattern ?: "",
        timeToEvent = when (this.entry?.state) {
            "OPTIMAL" -> "Now"
            "GOOD" -> "5m"
            "ACCEPTABLE" -> "15m"
            else -> "Wait"
        },
        price = 0.0, // TODO: Add current price to ASC EA
        changePercent = 0.0, // TODO: Add price change to ASC EA
        category = MarketCategory.FOREX, // TODO: Detect from asset name
        rationale = this.entry?.reason ?: "",
        isNew = (System.currentTimeMillis() - this.timestamp * 1000) < 300000, // New if < 5 minutes old
        
        // ASC EA Integration Fields
        alignment_percentage = this.alignment_percentage,
        structure_score = this.structure?.score ?: 0.0,
        regime_state = this.regime?.state ?: "",
        trend_state = this.regime?.trend ?: "",
        volatility_state = this.volatility?.state ?: "",
        liquidity_bias = this.liquidity?.bias ?: "",
        structure_bias = this.structure?.bias ?: "",
        indicator_bias = this.indicators?.bias ?: "",
        
        // Zone Context Fields
        zone_context_valid = this.zone_context_valid,
        zone_context_type = this.zone_context_type,
        zone_relationship = this.zone_relationship,
        current_zones = this.current_zones,
        target_zone = this.target_zone,
        zone_distance_pips = this.zone_distance_pips,
        
        // Exhaustion Analysis Fields
        exhaustion_detected = this.exhaustion_detected,
        exhaustion_bias = this.exhaustion_bias,
        exhaustion_score = this.exhaustion_score,
        rsi_value = this.indicators?.rsi ?: 50.0
    )
}

// Load all ASC signals and convert to WatchlistItems
fun loadWatchlistFromASCEA(): List<WatchlistItem> {
    // Currently MT5 EA writes single signal to ai_signals_mq5.json
    // TODO: Modify MT5 EA to write array of signals for multiple symbols
    val signalData = loadASCSignalData("current") ?: return emptyList()
    return listOf(signalData.toWatchlistItem())
}
