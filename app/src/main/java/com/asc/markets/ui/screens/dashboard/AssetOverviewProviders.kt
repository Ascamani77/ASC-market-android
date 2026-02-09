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
fun provideForexExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS

fun provideCryptoExplore(): List<ForexPair> = listOf(
    ForexPair("BTC/USD", "Bitcoin", 76762.0, 1200.0, 1.59, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ETH/USD", "Ethereum", 3000.0, 150.0, 5.26, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("SOL/USD", "Solana", 120.0, 8.0, 7.14, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("BNB/USD", "BNB", 420.0, -5.0, -1.17, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ADA/USD", "Cardano", 0.45, 0.02, 4.65, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("XRP/USD", "XRP", 0.62, -0.01, -1.59, category = com.asc.markets.data.MarketCategory.CRYPTO)
)

fun provideCommoditiesExplore(): List<ForexPair> = listOf(
    ForexPair("XAU/USD", "Gold", 2087.5, 38.0, 1.85, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("WTI", "Crude WTI", 76.45, -1.02, -1.32, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("NG", "Natural Gas", 2.856, 0.09, 3.21, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("XAG/USD", "Silver", 25.3, 0.4, 1.61, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("COPPER", "Copper", 4.32, 0.05, 1.17, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("PLAT", "Platinum", 980.0, -10.0, -1.01, category = com.asc.markets.data.MarketCategory.COMMODITIES)
)

fun provideIndicesExplore(): List<ForexPair> = listOf(
    ForexPair("SPX", "S&P 500", 6939.02, 60.0, 0.87, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("NDX", "Nasdaq 100", 25552.39, 358.0, 1.42, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("DAX", "DAX", 24538.81, 229.0, 0.94, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("FTSE", "FTSE 100", 10223.54, 52.0, 0.51, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("NI225", "Japan 225", 53322.8, 1100.0, 2.10, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("SSE", "SSE Comp", 4117.95, -40.0, -0.96, category = com.asc.markets.data.MarketCategory.INDICES)
)

fun provideBondsExplore(): List<ForexPair> = listOf(
    ForexPair("US10Y", "US 10Y", 102.5, 0.2, 0.20, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("US2Y", "US 2Y", 98.3, -0.1, -0.10, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("UK10Y", "UK 10Y", 101.2, 0.3, 0.30, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("GER10Y", "Germany 10Y", 89.7, 0.4, 0.45, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("JPN10Y", "Japan 10Y", 26.5, 0.0, 0.00, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("AUS10Y", "Australia 10Y", 105.4, 0.5, 0.48, category = com.asc.markets.data.MarketCategory.STOCK)
)

fun provideStocksExplore(): List<ForexPair> = com.asc.markets.data.FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.STOCK }

fun provideFuturesExplore(): List<ForexPair> = provideIndicesExplore()


