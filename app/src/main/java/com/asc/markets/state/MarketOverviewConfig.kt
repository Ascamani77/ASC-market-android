package com.asc.markets.state

data class MetricConfig(
    val id: String,
    val label: String,
    val valueSource: String,
    val displayFormat: String = "default"
)

data class MarketOverviewConfig(
    val primarySentimentLabel: String,
    val metrics: List<MetricConfig>,
    val hiddenMetrics: List<String> = emptyList(),
    val explanatoryText: List<String> = emptyList()
)

object MarketOverviewConfigs {
    val FOREX = MarketOverviewConfig(
        primarySentimentLabel = "Risk-On / Risk-Off",
        metrics = listOf(
            MetricConfig("usd_strength", "USD Strength Index", "usd_strength_index", "0.00"),
            MetricConfig("fx_vol", "FX Volatility (ATR/VIX-FX)", "fx_volatility", "0.00"),
            MetricConfig("session_liquidity", "Session Liquidity", "session_liquidity", "numeric"),
            MetricConfig("top_gainer_pair", "Top Gainer Pair", "top_gainer_pair", "pair"),
            MetricConfig("top_loser_pair", "Top Loser Pair", "top_loser_pair", "pair")
        ),
        hiddenMetrics = listOf("market_cap", "dominance"),
        explanatoryText = listOf(
            "FX metrics focus on pair-based volatility and session liquidity.",
            "USD strength is shown as a weighted index across majors."
        )
    )

    val CRYPTO = MarketOverviewConfig(
        primarySentimentLabel = "Bullish / Bearish",
        metrics = listOf(
            MetricConfig("total_mcap", "Total Market Cap", "total_market_cap", "currency"),
            MetricConfig("btc_dom", "BTC Dominance", "btc_dominance", "percent"),
            MetricConfig("crypto_vol", "Crypto Volatility Index", "crypto_vol_index", "0.00"),
            MetricConfig("top_gainer_coin", "Top Gainer Coin", "top_gainer_coin", "asset"),
            MetricConfig("top_loser_coin", "Top Loser Coin", "top_loser_coin", "asset")
        ),
        hiddenMetrics = emptyList(),
        explanatoryText = listOf(
            "Crypto view surfaces market cap and dominance metrics for macro lensing.",
            "On-chain flows and derivative activity inform volatility indicators."
        )
    )

    val COMMODITIES = MarketOverviewConfig(
        primarySentimentLabel = "Supply Tight / Supply Loose",
        metrics = listOf(
            MetricConfig("inventory_change", "Inventory Change", "inventory_change", "percent"),
            MetricConfig("futures_curve", "Futures Curve (Contango/Back)", "futures_curve", "text"),
            MetricConfig("energy_vs_metals", "Energy vs Metals Strength", "energy_vs_metals", "ratio"),
            MetricConfig("top_gainer_contract", "Top Gainer Contract", "top_gainer_contract", "asset")
        ),
        hiddenMetrics = emptyList(),
        explanatoryText = listOf(
            "Commodities focus on inventory and curve dynamics rather than market cap.",
            "Futures curve signals supply/demand stress across maturities."
        )
    )

    val INDICES = MarketOverviewConfig(
        primarySentimentLabel = "Breadth Positive / Breadth Negative",
        metrics = listOf(
            MetricConfig("adv_decl", "Advance/Decline Ratio", "advance_decline_ratio", "ratio"),
            MetricConfig("sector_leader", "Sector Rotation Leader", "sector_rotation_leader", "text"),
            MetricConfig("index_vol", "Index Volatility", "index_volatility", "0.00")
        ),
        explanatoryText = listOf(
            "Index lens emphasizes breadth and sector rotation signals.",
            "Volatility here refers to index-level realised and implied moves."
        )
    )

    val BONDS = MarketOverviewConfig(
        primarySentimentLabel = "Risk Aversion / Risk Appetite",
        metrics = listOf(
            MetricConfig("curve_shape", "Yield Curve Shape", "yield_curve_shape", "text"),
            MetricConfig("10y_change", "10Y Yield Change", "yield_10y_change", "percent"),
            MetricConfig("real_yield", "Real Yield", "real_yield", "percent")
        ),
        explanatoryText = listOf(
            "Bonds lens surfaces yield curve moves and real yield dynamics.",
            "Curve shape captures slope and inversion risks relevant to macro."
        )
    )

    val configs: Map<AssetContext, MarketOverviewConfig> = mapOf(
        AssetContext.FOREX to FOREX,
        AssetContext.CRYPTO to CRYPTO,
        AssetContext.COMMODITIES to COMMODITIES,
        AssetContext.INDICES to INDICES,
        AssetContext.BONDS to BONDS
    )

    val default: MarketOverviewConfig = FOREX
}
