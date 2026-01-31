package com.asc.markets.data

val FOREX_PAIRS = listOf(
    // Forex Majors
    ForexPair("EUR/USD", "Euro / US Dollar", 1.0845, 0.0012, 0.11),
    ForexPair("GBP/USD", "British Pound / US Dollar", 1.2634, -0.0021, -0.17),
    ForexPair("USD/JPY", "US Dollar / Japanese Yen", 151.42, 0.34, 0.23),
    ForexPair("AUD/USD", "Australian Dollar / US Dollar", 0.6512, 0.0005, 0.08),
    ForexPair("USD/CAD", "US Dollar / Canadian Dollar", 1.3542, 0.0015, 0.11),
    ForexPair("USD/CHF", "US Dollar / Swiss Franc", 0.9021, -0.0011, -0.12),
    ForexPair("NZD/USD", "New Zealand Dollar / US Dollar", 0.5985, 0.0002, 0.03),

    // Minors & Crosses
    ForexPair("EUR/GBP", "Euro / British Pound", 0.8582, 0.0005, 0.06),
    ForexPair("EUR/JPY", "Euro / Japanese Yen", 164.25, 0.12, 0.07),
    ForexPair("GBP/JPY", "British Pound / Japanese Yen", 191.35, 0.45, 0.24),
    ForexPair("AUD/JPY", "Australian Dollar / Japanese Yen", 98.65, 0.15, 0.15),
    ForexPair("EUR/AUD", "Euro / Australian Dollar", 1.6652, -0.0012, -0.07),
    ForexPair("EUR/CAD", "Euro / Canadian Dollar", 1.4685, 0.0008, 0.05),
    ForexPair("GBP/CAD", "British Pound / Canadian Dollar", 1.7112, -0.0015, -0.09),
    ForexPair("AUD/NZD", "Australian Dollar / New Zealand Dollar", 1.0882, 0.0004, 0.04),
    ForexPair("CAD/JPY", "Canadian Dollar / Japanese Yen", 111.82, 0.08, 0.07),

    // Crypto
    ForexPair("BTC/USD", "Bitcoin / US Dollar", 67432.50, 1240.20, 1.87),
    ForexPair("ETH/USD", "Ethereum / US Dollar", 3452.15, -45.20, -1.29),
    ForexPair("SOL/USD", "Solana / US Dollar", 145.85, 8.42, 6.12),

    // Commodities
    ForexPair("XAU/USD", "Gold / US Dollar", 2342.50, 12.40, 0.53),
    ForexPair("XAG/USD", "Silver / US Dollar", 28.15, 0.42, 1.52),
    ForexPair("USOIL", "WTI Crude Oil", 82.14, -1.20, -1.44),

    // Indexes
    ForexPair("NAS100", "Nasdaq 100", 18240.50, 142.30, 0.79),
    ForexPair("US30", "Dow Jones 30", 39120.00, 85.00, 0.22),
    ForexPair("SPX500", "S&P 500", 5210.45, 12.15, 0.23)
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
        postTradeOutcome = "Price reached TP1 within 4 hours. Institutional accumulation hold."
    )
)