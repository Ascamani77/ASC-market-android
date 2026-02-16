package com.asc.markets.data

val FOREX_PAIRS = listOf(
    // FOREX (5)
    ForexPair("EUR/USD", "Euro / US Dollar", 1.0845, 0.0012, 0.11, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("GBP/USD", "British Pound / US Dollar", 1.2634, -0.0021, -0.17, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/JPY", "US Dollar / Japanese Yen", 151.42, 0.34, 0.23, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/CHF", "US Dollar / Swiss Franc", 0.8812, 0.0008, 0.09, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("AUD/USD", "Australian Dollar / US Dollar", 0.6542, -0.0015, -0.23, com.asc.markets.data.MarketCategory.FOREX),

    // STOCKS (5)
    ForexPair("NVDA", "NVIDIA Corp.", 890.15, 23.80, 2.83, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("TSLA", "Tesla Inc.", 172.40, -4.10, -2.38, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("AAPL", "Apple Inc.", 185.12, 1.15, 0.62, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("MSFT", "Microsoft Corp.", 425.40, 3.10, 0.73, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("AMZN", "Amazon.com Inc.", 180.15, 2.48, 1.38, com.asc.markets.data.MarketCategory.STOCK),

    // COMMODITIES (2)
    ForexPair("XAU/USD", "Gold / US Dollar", 2342.50, 12.40, 0.53, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("USOIL", "WTI Crude Oil", 82.14, -1.20, -1.44, com.asc.markets.data.MarketCategory.COMMODITIES),

    // CRYPTO (2)
    ForexPair("BTC/USDT", "Bitcoin / Tether", 67432.50, 1240.20, 1.87, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ETH/USDT", "Ethereum / Tether", 3452.15, -45.20, -1.29, com.asc.markets.data.MarketCategory.CRYPTO),

    // INDICES (3)
    ForexPair("NAS100", "Nasdaq 100", 18240.50, 142.30, 0.79, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("US30", "Dow Jones 30", 39120.00, 85.00, 0.22, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("SPX500", "S&P 500", 5210.45, 12.15, 0.23, com.asc.markets.data.MarketCategory.INDICES),

    // BONDS (2)
    ForexPair("US10Y", "US 10Y Treasury Yield", 4.256, 0.012, 0.28, com.asc.markets.data.MarketCategory.BONDS),
    ForexPair("US02Y", "US 2Y Treasury Yield", 4.624, -0.005, -0.11, com.asc.markets.data.MarketCategory.BONDS),

    // FUTURES (2)
    ForexPair("ES1!", "S&P 500 Futures", 5245.25, 15.50, 0.30, com.asc.markets.data.MarketCategory.FUTURES),
    ForexPair("NQ1!", "Nasdaq 100 Futures", 18450.75, 119.00, 0.65, com.asc.markets.data.MarketCategory.FUTURES)
)

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