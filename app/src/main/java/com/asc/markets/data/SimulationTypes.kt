package com.asc.markets.data

import kotlinx.serialization.Serializable

@Serializable
data class SimulationTrade(
    val id: Int,
    val asset: String,
    val type: String, // "buy" | "sell"
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val exitPrice: Double? = null,
    val positionSize: Double,
    val timestamp: String,
    val status: String, // "open" | "closed"
    val result: String? = null, // "win" | "loss"
    val profitLoss: Double,
    val durationMinutes: Int? = null,
    val confidence: Double,
    val reasoning: String,
    val slAdjustmentReason: String? = null,
    val tpAdjustmentReason: String? = null
)

@Serializable
data class SimulationSettings(
    val mode: String, // "auto" | "prompt"
    val isRunning: Boolean,
    val isReplayMode: Boolean,
    val replaySpeed: Int,
    val killSwitchActive: Boolean,
    val maxDrawdownLimit: Double,
    val winRateThreshold: Double,
    val riskPerTrade: Double,
    val lookbackCandles: Int
)

@Serializable
data class SimulationSignal(
    val asset: String,
    val type: String, // "buy" | "sell"
    val entry: Double,
    val sl: Double,
    val tp: Double,
    val risk: Double,
    val confidence: Double,
    val reasoning: String
)

@Serializable
data class StrategyStats(
    val totalTrades: Int,
    val wins: Int,
    val totalPl: Double,
    val avgPl: Double,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val sharpeRatio: Double,
    val avgRr: Double,
    val equityCurve: List<EquityPoint>,
    val confidenceCalibration: List<CalibrationPoint>
)

@Serializable
data class EquityPoint(
    val timestamp: String,
    val balance: Double
)

@Serializable
data class CalibrationPoint(
    val confidenceBracket: Double,
    val actualWinRate: Double
)

@Serializable
data class SimulationCandle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val ma20: Double? = null,
    val ma50: Double? = null
)
