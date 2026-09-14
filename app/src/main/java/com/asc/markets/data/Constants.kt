package com.asc.markets.data

import java.util.Locale

// ─── Canonical trained-asset registry ────────────────────────────────────────
// These are the only symbols the app should ever show (all screens).

val TRAINED_ASSET_SYMBOLS: Set<String> = setOf(
    // Crypto (8)
    "BTCUSD", "BTCUSDT", "BTCCNH", "BTCXAG", "BTCXAU",
    "ETHUSD", "ETHUSDT", "ETHBTC",
    // Forex / majors & metals (18)
    "AUDJPY", "AUDUSD", "DXY", "EURCAD", "EURCHF", "EURGBP", "EURJPY", "EURUSD",
    "GBPJPY", "GBPUSD", "NZDUSD", "USDCAD", "USDCHF", "USDCNH", "USDJPY",
    "XAGUSD", "XAUUSD", "XCUUSD",
    // Commodities / energy (3)
    "BRENTCMDUSD", "UKOIL", "USOIL",
    // Indices (9)
    "DE30", "JP225", "STOXX50", "UK100", "US30", "US500", "USTEC", "USTEC_x100", "SPCX",
    // Stocks (8)
    "AAPL", "AMZN", "META", "MSFT", "NFLX", "NVDA", "PYPL", "TSLA"
)

private val TRAINED_ASSET_KEYS: Set<String> =
    TRAINED_ASSET_SYMBOLS.map { normalizeTickerKey(it) }.toSet()

fun normalizeTickerKey(raw: String): String =
    raw.uppercase(Locale.US)
        .replace("/", "").replace("-", "").replace("_", "").replace(" ", "").replace(".", "")

/** Returns true when the raw symbol (possibly broker-suffixed e.g. "BTCUSDm") matches a trained asset. */
fun isTrainedAssetTicker(raw: String): Boolean {
    val key = normalizeTickerKey(raw).let { k ->
        // Strip broker suffix 'm'/'M' that Exness/Ctrader appends
        if (k.endsWith("M")) k.dropLast(1) else k
    }
    return key in TRAINED_ASSET_KEYS
}

private val TRAINED_CRYPTO_KEYS = setOf(
    "BTCUSD", "BTCUSDT", "BTCCNH", "BTCXAG", "BTCXAU", "ETHUSD", "ETHUSDT", "ETHBTC"
)
private val TRAINED_FOREX_KEYS = setOf(
    "AUDJPY", "AUDUSD", "DXY", "EURCAD", "EURCHF", "EURGBP", "EURJPY", "EURUSD",
    "GBPJPY", "GBPUSD", "NZDUSD", "USDCAD", "USDCHF", "USDCNH", "USDJPY",
    "XAGUSD", "XAUUSD", "XCUUSD"
)
private val TRAINED_COMMOD_KEYS = setOf("BRENTCMDUSD", "UKOIL", "USOIL")
private val TRAINED_INDICES_KEYS = setOf(
    "DE30", "JP225", "STOXX50", "UK100", "US30", "US500", "USTEC", "USTECX100", "SPCX"
)
private val TRAINED_STOCK_KEYS = setOf("AAPL", "AMZN", "META", "MSFT", "NFLX", "NVDA", "PYPL", "TSLA")

/** Classifies a trained asset into "crypto"/"forex"/"commodity"/"index"/"stock"; null if not trained. */
fun trainedAssetGroup(raw: String): String? {
    val key = normalizeTickerKey(raw).let { if (it.endsWith("M")) it.dropLast(1) else it }
    if (key !in TRAINED_ASSET_KEYS) return null
    return when {
        key in TRAINED_CRYPTO_KEYS -> "crypto"
        key in TRAINED_FOREX_KEYS -> "forex"
        key in TRAINED_COMMOD_KEYS -> "commodity"
        key in TRAINED_INDICES_KEYS -> "index"
        key in TRAINED_STOCK_KEYS -> "stock"
        else -> null
    }
}

val FOREX_PAIRS = listOf(
    // FOREX (8) - Added EURGBP, EURJPY, USDCAD to match AI system
    ForexPair("EUR/USD", "Euro / US Dollar", 1.0845, 0.0012, 0.11, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("GBP/USD", "British Pound / US Dollar", 1.2634, -0.0021, -0.17, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/JPY", "US Dollar / Japanese Yen", 151.42, 0.34, 0.23, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/CHF", "US Dollar / Swiss Franc", 0.8812, 0.0008, 0.09, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("AUD/USD", "Australian Dollar / US Dollar", 0.6542, -0.0015, -0.23, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("EUR/GBP", "Euro / British Pound", 0.8585, 0.0008, 0.09, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("EUR/JPY", "Euro / Japanese Yen", 164.15, 0.42, 0.26, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/CAD", "US Dollar / Canadian Dollar", 1.3625, 0.0015, 0.11, com.asc.markets.data.MarketCategory.FOREX),

    // STOCKS (5)
    ForexPair("NVDA", "NVIDIA Corp.", 890.15, 23.80, 2.83, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("TSLA", "Tesla Inc.", 172.40, -4.10, -2.38, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("AAPL", "Apple Inc.", 185.12, 1.15, 0.62, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("MSFT", "Microsoft Corp.", 425.40, 3.10, 0.73, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("AMZN", "Amazon.com Inc.", 180.15, 2.48, 1.38, com.asc.markets.data.MarketCategory.STOCK),

    // COMMODITIES (4)
    ForexPair("XAU/USD", "Gold / US Dollar", 2342.50, 12.40, 0.53, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("XAG/USD", "Silver / US Dollar", 28.45, 0.65, 2.34, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("XCU/USD", "Copper / US Dollar", 4.528, 0.021, 0.47, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("Crude-F", "WTI Crude Oil", 82.14, -1.20, -1.44, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("Brent-F", "Brent Crude Oil", 85.42, -0.95, -1.10, com.asc.markets.data.MarketCategory.COMMODITIES),

    // CRYPTO (4)
    // Binance USDT pairs (for most of the app - Market Overview, Dashboard, etc.)
    ForexPair("BTC/USDT", "Bitcoin / Tether", 67432.50, 1240.20, 1.87, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ETH/USDT", "Ethereum / Tether", 3452.15, -45.20, -1.29, com.asc.markets.data.MarketCategory.CRYPTO),
    // MT5/Exness pairs (for Quote page and StreamScreen chart only)
    ForexPair("BTC/USD", "Bitcoin / US Dollar", 67425.00, 1232.50, 1.85, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ETH/USD", "Ethereum / US Dollar", 3450.80, -46.50, -1.33, com.asc.markets.data.MarketCategory.CRYPTO),

    // INDICES (4)
    ForexPair("DXY", "US Dollar Index", 104.28, 0.45, 0.43, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("NAS100", "Nasdaq 100", 18240.50, 142.30, 0.79, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("US30", "Dow Jones 30", 39120.00, 85.00, 0.22, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("SPX500", "S&P 500", 5210.45, 12.15, 0.23, com.asc.markets.data.MarketCategory.INDICES),

    // BONDS (2)
    ForexPair("US10Y", "US 10Y Treasury Yield", 4.256, 0.012, 0.28, com.asc.markets.data.MarketCategory.BONDS),
    ForexPair("US02Y", "US 2Y Treasury Yield", 4.624, -0.005, -0.11, com.asc.markets.data.MarketCategory.BONDS)
).filter { isTrainedAssetTicker(it.symbol) }

val MOCK_TRADES = listOf(
    AutomatedTrade(
        id = "T-842",
        pair = "EUR/USD",
        side = "BUY",
        status = "WON",
        entryPrice = "1.0842",
        exitPrice = "1.0885",
        pnl = "+43 Pips",
        pnlAmount = 430.0,
        reasoning = "Node detected institutional buy program following Asian low sweep. CHoCH confirmed on M15.",
        timestamp = System.currentTimeMillis() - 7200000,
        preTradeContext = "Market was in consolidation; Liquidity build-up at 1.0820.",
        postTradeOutcome = "Price reached TP1 within 4 hours. Institutional accumulation hold.",
        relayId = "PRIMARY-UK-L14",
        latencyMs = 0.02
    )
)
