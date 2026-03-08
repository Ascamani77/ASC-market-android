package com.asc.markets.ui.screens.tradeDashboard.model

data class AccountInfo(
    val balance: Double,
    val equity: Double,
    val margin: Double,
    val freeMargin: Double,
    val marginLevel: Double,
    val profit: Double
)

enum class TradeType {
    BUY, SELL
}

data class Position(
    val id: String,
    val ticketId: String,
    val magicNumber: Int? = null,
    val symbol: String,
    val type: TradeType,
    val volume: Double,
    val openPrice: Double,
    val currentPrice: Double,
    val tp: Double? = null,
    val sl: Double? = null,
    val swap: Double,
    val commission: Double,
    val profit: Double,
    val healthScore: Int
)

data class HistoricalTrade(
    val id: String,
    val ticketId: String,
    val symbol: String,
    val type: TradeType,
    val volume: Double,
    val openPrice: Double,
    val closePrice: Double,
    val openTime: String,
    val closeTime: String,
    val swap: Double,
    val commission: Double,
    val profit: Double
)

data class PriceData(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val spread: Double,
    val change: Double
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

enum class Bias {
    BULLISH, BEARISH, NEUTRAL
}

data class AIAdvisory(
    val bias: Bias,
    val confidence: Int,
    val suggestedSL: Double,
    val suggestedTP: Double,
    val riskLevel: RiskLevel
)

enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}

data class AIAlert(
    val id: String,
    val timestamp: String,
    val message: String,
    val severity: AlertSeverity,
    val details: String? = null,
    val isCustom: Boolean = false
)

data class TimeframeTrends(
    val m5: Int,
    val m15: Int,
    val m30: Int,
    val h1: Int,
    val h4: Int,
    val d1: Int
)

data class AIMarketIntelligence(
    val trendStrength: Int,
    val volatilityScore: Int,
    val momentumScore: Int,
    val marketPhase: String,
    val phaseDescription: String? = null,
    val timeframeTrends: TimeframeTrends? = null,
    val volatilityDrivers: List<String> = emptyList()
)

data class CustomAlert(
    val id: String,
    val symbol: String,
    val type: CustomAlertType,
    val value: Double,
    val isActive: Boolean,
    val cooldownMinutes: Int,
    val lastTriggeredAt: String? = null,
    val createdAt: String
)

data class AISettings(
    val autoAdjustSLTP: Boolean = false,
    val autoExitMarket: Boolean = false,
    val maxRiskPerTrade: Double = 1.0,
    val minConfidenceThreshold: Int = 80,
    val allowedSymbols: List<String> = listOf("EURUSD", "GBPUSD", "XAUUSD"),
    val tradingHoursStart: String = "08:00",
    val tradingHoursEnd: String = "20:00",
    val maxDailyLoss: Double = 1000.0,
    val actionAreas: AIActionAreas = AIActionAreas()
)

data class AIActionAreas(
    val trendFollowing: Boolean = true,
    val counterTrend: Boolean = false,
    val newsTrading: Boolean = false
)

enum class CustomAlertType {
    PRICE_ABOVE, PRICE_BELOW, TREND_STRENGTH_ABOVE, VOLATILITY_ABOVE
}

data class CandleData(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class Impact {
    LOW, MEDIUM, HIGH
}

data class EconomicEvent(
    val id: String,
    val time: String,
    val currency: String,
    val event: String,
    val impact: Impact,
    val actual: String? = null,
    val forecast: String? = null,
    val previous: String? = null
)

data class RiskInfo(
    val position: Position,
    val reason: String,
    val advisory: AIAdvisory
)
