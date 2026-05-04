package com.asc.markets.ui.screens.dashboard

import com.asc.markets.state.AssetContext
import com.asc.markets.data.ForexPair
import java.util.Locale

// Lightweight provider stubs for wiring the UniversalOverviewBox to a replaceable
// data layer. These are intentionally simple and deterministic so we can
// replace them later with calls to core fetchers that accept AssetContext.

fun getRegimeForContext(ctx: AssetContext, pair: ForexPair): String {
    val abs = kotlin.math.abs(pair.changePercent)
    return when {
        abs > 1.5 -> "Strong Trend"
        abs > 0.6 -> "Trending"
        abs > 0.25 -> "Transitional"
        else -> "Ranging"
    }
}

fun getVolatilityForPair(pair: ForexPair): String {
    val abs = kotlin.math.abs(pair.changePercent)
    return when {
        abs > 2.5 -> "Erratic"
        abs > 1.0 -> "Expanding"
        abs > 0.5 -> "Elevated"
        else -> "Compressed"
    }
}

fun getLiquidityConditionForContext(ctx: AssetContext): String = when (ctx) {
    AssetContext.FOREX -> "Deep"
    AssetContext.CRYPTO -> "Moderate"
    AssetContext.COMMODITIES -> "Variable"
    AssetContext.INDICES -> "Heavy"
    AssetContext.STOCKS -> "Market-hours"
    AssetContext.FUTURES -> "Variable"
    AssetContext.BONDS -> "Venue-dependent"
    AssetContext.ALL -> "Cross-asset"
}

fun getSessionSensitivityForContext(ctx: AssetContext): String = when (ctx) {
    AssetContext.FOREX -> "Asia / London / NY"
    AssetContext.COMMODITIES -> "London / NY"
    AssetContext.CRYPTO -> "24/7"
    AssetContext.INDICES -> "NY / London"
    AssetContext.STOCKS -> "Market Hours"
    AssetContext.FUTURES -> "Settlement-sensitive"
    AssetContext.BONDS -> "NY"
    AssetContext.ALL -> "Cross-asset"
}

fun getBiasLabelForPair(pair: ForexPair): String = when {
    pair.changePercent > 0.25 -> "Bullish (Conditional)"
    pair.changePercent < -0.25 -> "Bearish (Conditional)"
    else -> "Neutral"
}

fun getConfidenceForPair(pair: ForexPair): Int {
    val abs = kotlin.math.abs(pair.changePercent)
    return kotlin.math.min(95, (50 + (abs * 20)).toInt())
}

fun getKeyLevelsForPair(pair: ForexPair): Triple<String, String, String> {
    val level1 = String.format(Locale.US, "%.4f", pair.price * (1 + 0.01))
    val level2 = String.format(Locale.US, "%.4f", pair.price * (1 - 0.01))
    val level3 = String.format(Locale.US, "%.4f", pair.price)
    return Triple(level1, level2, level3)
}

fun getInvalidationLevelForPair(pair: ForexPair): String = String.format(Locale.US, "%.4f", pair.price * (1 - 0.005))

fun getMacroAlignmentForContext(ctx: AssetContext, pair: ForexPair): String = when {
    pair.changePercent > 0.0 -> "Risk-On"
    pair.changePercent < 0.0 -> "Risk-Off"
    else -> "Mixed"
}

fun getPlaybookReadinessForContext(ctx: AssetContext, pair: ForexPair): String {
    val vol = getVolatilityForPair(pair)
    val liq = getLiquidityConditionForContext(ctx)
    return when {
        vol == "Compressed" && liq.contains("Deep", true) -> "Breakout"
        vol == "Erratic" || liq.contains("Variable", true) -> "Wait"
        else -> "Mean Reversion"
    }
}

// --- NEW DATA PROVIDERS FOR ADVANCED OVERVIEW FEATURES ---

data class Opportunity(val title: String, val type: String, val confidence: Int)
fun getPreMoveOpportunities(ctx: AssetContext): List<Opportunity> = when (ctx) {
    AssetContext.FOREX -> listOf(Opportunity("EURUSD Range Break", "BREAKOUT", 82), Opportunity("GBPUSD Pullback", "TREND", 74))
    AssetContext.CRYPTO -> listOf(Opportunity("BTC Liquidity Sweep", "REVERSAL", 88))
    else -> listOf(Opportunity("Potential Volatility Expansion", "MOMENTUM", 65))
}

data class VolatilityRadarItem(val asset: String, val score: Float, val trend: String)
fun getVolatilityRadar(ctx: AssetContext): List<VolatilityRadarItem> = listOf(
    VolatilityRadarItem("EURUSD", 0.82f, "EXPANDING"),
    VolatilityRadarItem("BTCUSD", 0.95f, "HIGH"),
    VolatilityRadarItem("XAUUSD", 0.45f, "COMPRESSED")
)

fun getTradeReadinessScore(ctx: AssetContext): Int = 78 // Mock score 0-100

data class MarketTension(val asset: String, val tension: Float) // 0.0 to 1.0
fun getMarketTensionMetrics(ctx: AssetContext): List<MarketTension> = listOf(
    MarketTension("USD Index", 0.85f),
    MarketTension("S&P 500", 0.42f),
    MarketTension("Gold", 0.68f)
)

data class UpcomingEvent(val name: String, val timeUtc: Long)
fun getUpcomingEvents(ctx: AssetContext): List<UpcomingEvent> {
    val now = System.currentTimeMillis()
    return listOf(
        UpcomingEvent("US Core CPI", now + 3600000), // 1h
        UpcomingEvent("ECB Press Conf", now + 7200000) // 2h
    )
}

fun getBacktestSimulationSummary(ctx: AssetContext): String = "Win Rate: 64.2% | Profit Factor: 1.84 | Max DD: 4.2%"

// Provide explore items per AssetContext. This delegates to the specific
// providers declared in MarketOverviewTab.kt so callers elsewhere can stay
// context-aware without depending on low-level constants.
fun getExploreItemsForContext(ctx: com.asc.markets.state.AssetContext): List<ForexPair> = when (ctx) {
    com.asc.markets.state.AssetContext.FOREX -> provideForexExplore()
    com.asc.markets.state.AssetContext.CRYPTO -> provideCryptoExplore()
    com.asc.markets.state.AssetContext.COMMODITIES -> provideCommoditiesExplore()
    com.asc.markets.state.AssetContext.INDICES -> provideIndicesExplore()
    com.asc.markets.state.AssetContext.STOCKS -> provideStocksExplore()
    com.asc.markets.state.AssetContext.FUTURES -> provideFuturesExplore()
    com.asc.markets.state.AssetContext.BONDS -> provideBondsExplore()
    com.asc.markets.state.AssetContext.ALL -> provideAllExplore()
}

// Concrete explore providers (previously located in MarketOverviewTab.kt).
// These are kept lightweight and deterministic; replace with live data later.
fun provideForexExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.FOREX }

fun provideCryptoExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.CRYPTO }

fun provideCommoditiesExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.COMMODITIES }

fun provideIndicesExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.INDICES }

fun provideBondsExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.BONDS }

fun provideStocksExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.STOCK }

fun provideFuturesExplore(): List<ForexPair> {
    val liveFutures = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.FUTURES }
    if (liveFutures.isNotEmpty()) return liveFutures

    return listOf(
        ForexPair("ES1!", "E-mini S&P 500", 5218.25, 14.50, 0.28, com.asc.markets.data.MarketCategory.FUTURES),
        ForexPair("NQ1!", "Nasdaq 100 E-mini", 18322.75, 96.25, 0.53, com.asc.markets.data.MarketCategory.FUTURES),
        ForexPair("CL1!", "Crude Oil Futures", 81.74, -0.84, -1.02, com.asc.markets.data.MarketCategory.FUTURES),
        ForexPair("GC1!", "Gold Futures", 2351.80, 11.20, 0.48, com.asc.markets.data.MarketCategory.FUTURES)
    )
}

fun provideAllExplore(): List<ForexPair> = buildList {
    addAll(provideStocksExplore())
    addAll(provideCryptoExplore())
    addAll(provideForexExplore())
    addAll(provideCommoditiesExplore())
    addAll(provideIndicesExplore())
    addAll(provideBondsExplore())
    addAll(provideFuturesExplore())
}
