package com.asc.markets.ui.screens.tradeDashboard.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.asc.markets.ui.screens.tradeDashboard.model.*

/**
 * DashboardViewModel: Central state management for the Trade Dashboard
 */
class DashboardViewModel {
    // UI States
    var accountInfo by mutableStateOf<AccountInfo?>(null)
        private set

    var positions by mutableStateOf<List<Position>>(emptyList())
        private set

    var closedPositions by mutableStateOf<List<HistoricalTrade>>(emptyList())
        private set

    var currentPrice by mutableStateOf<PriceData?>(null)
        private set

    var candleData by mutableStateOf<List<CandleData>>(emptyList())
        private set

    var alerts by mutableStateOf<List<AIAlert>>(emptyList())
        private set

    var advisory by mutableStateOf<AIAdvisory?>(null)
        private set

    var marketIntel by mutableStateOf<AIMarketIntelligence?>(null)
        private set

    var calendarEvents by mutableStateOf<List<EconomicEvent>>(emptyList())
        private set

    var selectedSymbol by mutableStateOf("EURUSD")
        private set

    var selectedTimeframe by mutableStateOf("H1")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var aiSettings by mutableStateOf(AISettings())
        private set

    // Initialize with sample data
    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        isLoading = true
        
        accountInfo = AccountInfo(
            balance = 10000.0,
            equity = 10315.68,
            margin = 250.0,
            freeMargin = 10065.68,
            marginLevel = 4126.27,
            profit = 315.68
        )

        positions = listOf(
            Position(
                id = "pos_1",
                ticketId = "8842105",
                symbol = "EURUSD",
                type = TradeType.BUY,
                volume = 1.0,
                openPrice = 1.0850,
                currentPrice = 1.0875,
                tp = 1.0900,
                sl = 1.0800,
                swap = -12.45,
                commission = -7.80,
                profit = 234.53,
                healthScore = 100
            ),
            Position(
                id = "pos_2",
                ticketId = "8842112",
                symbol = "GBPUSD",
                type = TradeType.SELL,
                volume = 0.5,
                openPrice = 1.2700,
                currentPrice = 1.2680,
                tp = 1.2550,
                sl = 1.2850,
                swap = 4.20,
                commission = -3.50,
                profit = 76.36,
                healthScore = 100
            )
        )

        closedPositions = listOf(
            HistoricalTrade(
                id = "trade_1",
                ticketId = "789456",
                symbol = "XAUUSD",
                volume = 0.1,
                type = TradeType.BUY,
                openPrice = 2024.5,
                closePrice = 2035.2,
                openTime = "2026-02-27",
                closeTime = "2026-02-27 09:15",
                swap = 0.0,
                commission = -10.0,
                profit = 150.0
            ),
            HistoricalTrade(
                id = "trade_2",
                ticketId = "789457",
                symbol = "USDJPY",
                volume = 1.0,
                type = TradeType.SELL,
                openPrice = 150.45,
                closePrice = 150.12,
                openTime = "2026-02-27",
                closeTime = "2026-02-27 13:45",
                swap = 0.0,
                commission = -10.0,
                profit = 330.0
            )
        )

        // Generate data for initial symbol
        updateSelectedSymbol(selectedSymbol)

        alerts = listOf(
            AIAlert(
                id = "alert_1",
                timestamp = "05:30:12",
                message = "Bearish divergence noted on RSI (H1).",
                severity = AlertSeverity.WARNING,
                details = "RSI shows lower highs while price makes higher highs"
            )
        )

        advisory = AIAdvisory(
            bias = Bias.BEARISH,
            confidence = 65,
            suggestedSL = 1.26850,
            suggestedTP = 1.25900,
            riskLevel = RiskLevel.HIGH
        )

        marketIntel = AIMarketIntelligence(
            trendStrength = 75,
            volatilityScore = 42,
            momentumScore = 68,
            marketPhase = "DISTRIBUTION",
            timeframeTrends = TimeframeTrends(
                m5 = 85,
                m15 = 82,
                m30 = 78,
                h1 = 75,
                h4 = 65,
                d1 = 45
            ),
            volatilityDrivers = listOf("Central bank speech", "Geopolitical tensions", "Overbought RSI")
        )

        calendarEvents = listOf(
            EconomicEvent("e1", "13:30", "USD", "Core PCE Price Index (MoM)", Impact.HIGH, forecast = "0.3%", previous = "0.2%"),
            EconomicEvent("e2", "14:45", "USD", "Chicago PMI", Impact.MEDIUM, forecast = "48.0", previous = "46.0"),
            EconomicEvent("e3", "15:00", "USD", "Revised UoM Consumer Sentiment", Impact.LOW, forecast = "79.6", previous = "79.6")
        )

        isLoading = false
    }

    fun updateSelectedSymbol(symbol: String) { 
        selectedSymbol = symbol
        
        // Update currentPrice to match the selected symbol
        val basePrice = when {
            symbol.contains("JPY") -> 150.0
            symbol.contains("XAU") -> 2030.0
            symbol.contains("US30") -> 39000.0
            symbol.contains("NAS100") -> 18000.0
            symbol.startsWith("GBP") -> 1.26
            else -> 1.08 // EURUSD and others
        }

        currentPrice = PriceData(
            symbol = symbol,
            bid = basePrice + (Math.random() * basePrice * 0.001),
            ask = basePrice + (Math.random() * basePrice * 0.001) + 0.0002,
            spread = 1.5,
            change = (Math.random() * 2) - 1
        )

        // Generate new candle data for the symbol
        candleData = (1..20).map { i ->
            val candleBase = basePrice + (Math.random() * basePrice * 0.005)
            CandleData(
                time = System.currentTimeMillis() - (i * 3600000),
                open = candleBase,
                high = candleBase + (Math.random() * basePrice * 0.002),
                low = candleBase - (Math.random() * basePrice * 0.002),
                close = candleBase + (Math.random() * basePrice * 0.001),
                volume = 100000.0
            )
        }
    }

    fun onTimeframeSelected(timeframe: String) { selectedTimeframe = timeframe }
    fun adjustStopLoss(ticketId: String, newSL: Double) {
        positions = positions.map { if (it.ticketId == ticketId) it.copy(sl = newSL) else it }
    }
    fun adjustTakeProfit(ticketId: String, newTP: Double) {
        positions = positions.map { if (it.ticketId == ticketId) it.copy(tp = newTP) else it }
    }
    fun updateAISettings(settings: AISettings) { aiSettings = settings }
}
