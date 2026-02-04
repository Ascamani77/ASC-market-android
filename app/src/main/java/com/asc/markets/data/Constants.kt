package com.asc.markets.data

val FOREX_PAIRS = listOf(
    // Forex Majors
    ForexPair("EUR/USD", "Euro / US Dollar", 1.0845, 0.0012, 0.11, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("GBP/USD", "British Pound / US Dollar", 1.2634, -0.0021, -0.17, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/JPY", "US Dollar / Japanese Yen", 151.42, 0.34, 0.23, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("AUD/USD", "Australian Dollar / US Dollar", 0.6512, 0.0005, 0.08, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/CAD", "US Dollar / Canadian Dollar", 1.3542, 0.0015, 0.11, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("USD/CHF", "US Dollar / Swiss Franc", 0.9021, -0.0011, -0.12, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("NZD/USD", "New Zealand Dollar / US Dollar", 0.5985, 0.0002, 0.03, com.asc.markets.data.MarketCategory.FOREX),

    // Minors & Crosses
    ForexPair("EUR/GBP", "Euro / British Pound", 0.8582, 0.0005, 0.06, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("EUR/JPY", "Euro / Japanese Yen", 164.25, 0.12, 0.07, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("GBP/JPY", "British Pound / Japanese Yen", 191.35, 0.45, 0.24, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("AUD/JPY", "Australian Dollar / Japanese Yen", 98.65, 0.15, 0.15, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("EUR/AUD", "Euro / Australian Dollar", 1.6652, -0.0012, -0.07, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("EUR/CAD", "Euro / Canadian Dollar", 1.4685, 0.0008, 0.05, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("GBP/CAD", "British Pound / Canadian Dollar", 1.7112, -0.0015, -0.09, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("AUD/NZD", "Australian Dollar / New Zealand Dollar", 1.0882, 0.0004, 0.04, com.asc.markets.data.MarketCategory.FOREX),
    ForexPair("CAD/JPY", "Canadian Dollar / Japanese Yen", 111.82, 0.08, 0.07, com.asc.markets.data.MarketCategory.FOREX),

    // Crypto (expanded)
    ForexPair("BTC/USD", "Bitcoin / US Dollar", 67432.50, 1240.20, 1.87, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ETH/USD", "Ethereum / US Dollar", 3452.15, -45.20, -1.29, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("SOL/USD", "Solana / US Dollar", 145.85, 8.42, 6.12, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("LTC/USD", "Litecoin / US Dollar", 88.12, 1.25, 1.44, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("XRP/USD", "Ripple / US Dollar", 0.62, -0.01, -1.59, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ADA/USD", "Cardano / US Dollar", 0.92, 0.03, 3.37, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("DOGE/USD", "Dogecoin / US Dollar", 0.18, 0.005, 2.86, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("DOT/USD", "Polkadot / US Dollar", 6.42, -0.12, -1.84, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("AVAX/USD", "Avalanche / US Dollar", 23.50, 0.78, 3.43, com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("LINK/USD", "Chainlink / US Dollar", 12.05, -0.20, -1.63, com.asc.markets.data.MarketCategory.CRYPTO),

    // Commodities (expanded)
    ForexPair("XAU/USD", "Gold / US Dollar", 2342.50, 12.40, 0.53, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("XAG/USD", "Silver / US Dollar", 28.15, 0.42, 1.52, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("USOIL", "WTI Crude Oil", 82.14, -1.20, -1.44, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("BRENT", "Brent Crude", 88.30, -0.95, -1.06, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("NATGAS", "Natural Gas", 3.45, 0.05, 1.47, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("COPPER", "Copper", 4.05, 0.02, 0.50, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("CORN", "Corn", 5.95, -0.04, -0.67, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("SOYB", "Soybeans", 12.40, 0.10, 0.81, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("PLAT", "Platinum", 920.50, 5.20, 0.57, com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("PALL", "Palladium", 1840.25, -10.30, -0.56, com.asc.markets.data.MarketCategory.COMMODITIES),

    // Indexes (expanded)
    ForexPair("NAS100", "Nasdaq 100", 18240.50, 142.30, 0.79, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("US30", "Dow Jones 30", 39120.00, 85.00, 0.22, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("SPX500", "S&P 500", 5210.45, 12.15, 0.23, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("FTSE100", "FTSE 100", 7500.25, 22.10, 0.30, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("DAX30", "DAX 30", 15640.10, -10.50, -0.07, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("NIKKEI225", "Nikkei 225", 31900.75, 120.45, 0.38, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("HSI", "Hang Seng Index", 21050.00, -45.00, -0.21, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("STOXX50", "STOXX Europe 50", 4500.60, 8.20, 0.18, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("TSX60", "S&P/TSX 60", 1900.40, 6.80, 0.36, com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("ASX200", "ASX 200", 7200.90, 12.30, 0.17, com.asc.markets.data.MarketCategory.INDICES),

    // Stocks (expanded)
    ForexPair("AAPL", "Apple Inc.", 173.22, 1.52, 0.88, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("MSFT", "Microsoft Corp.", 334.12, -0.84, -0.25, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("GOOGL", "Alphabet Inc.", 142.55, 2.10, 1.50, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("AMZN", "Amazon.com Inc.", 138.40, -0.90, -0.65, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("TSLA", "Tesla Inc.", 220.10, 3.15, 1.45, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("META", "Meta Platforms", 315.50, -2.40, -0.76, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("NVDA", "NVIDIA Corp.", 420.75, 5.20, 1.25, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("JPM", "JPMorgan Chase", 156.30, 0.80, 0.51, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("BABA", "Alibaba Group", 95.40, -1.10, -1.14, com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("ORCL", "Oracle Corp.", 85.22, 0.60, 0.71, com.asc.markets.data.MarketCategory.STOCK)
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