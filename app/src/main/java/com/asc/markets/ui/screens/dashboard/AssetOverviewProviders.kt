package com.asc.markets.ui.screens.dashboard

import com.asc.markets.state.AssetContext
import com.asc.markets.data.ForexPair

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
    val level1 = String.format("%.4f", pair.price * (1 + 0.01))
    val level2 = String.format("%.4f", pair.price * (1 - 0.01))
    val level3 = String.format("%.4f", pair.price)
    return Triple(level1, level2, level3)
}

fun getInvalidationLevelForPair(pair: ForexPair): String = String.format("%.4f", pair.price * (1 - 0.005))

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
    com.asc.markets.state.AssetContext.ALL -> provideForexExplore()
}

// Concrete explore providers (previously located in MarketOverviewTab.kt).
// These are kept lightweight and deterministic; replace with live data later.
fun provideForexExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.FOREX }

fun provideCryptoExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.CRYPTO }

fun provideCommoditiesExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.COMMODITIES }

fun provideIndicesExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.INDICES }

fun provideBondsExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.BONDS }

fun provideStocksExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.STOCK }

fun provideFuturesExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.FUTURES }


