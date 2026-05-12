package com.asc.markets.data

data class PendingOrder(
    val id: String,
    val symbol: String,
    val type: OrderType,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val lotSize: Double,
    val riskPercent: Double,
    val status: OrderStatus,
    val timestamp: Long,
    val triggerDistance: Double = 0.0
)

enum class OrderType { BUY, SELL }
enum class OrderStatus { WAITING, NEAR_TRIGGER, FILLED, CANCELLED }

data class SMCZone(
    val id: String,
    val type: SMCType,
    val priceLevel: Double,
    val strength: Float,
    val touched: Boolean = false
)

enum class SMCType { BOS, CHOCH, ORDER_BLOCK, FVG, LIQUIDITY_SWEEP, PREMIUM_ZONE, DISCOUNT_ZONE }

data class SupplyDemandZone(
    val id: String,
    val type: ZoneType,
    val upperPrice: Double,
    val lowerPrice: Double,
    val strength: Float,
    val detectionLength: Int,
    val touchRule: TouchRule,
    val touches: Int = 0
)

enum class ZoneType { SUPPLY, DEMAND }
enum class TouchRule { SINGLE_TOUCH, MULTI_TOUCH, NO_TOUCH }

data class SupportResistanceLevel(
    val id: String,
    val price: Double,
    val type: SRType,
    val strength: Float,
    val isDynamic: Boolean = false
)

enum class SRType { SUPPORT, RESISTANCE }

data class ExecutionSettings(
    val slippage: Float,
    val spreadType: SpreadType,
    val latency: Int,
    val fillType: FillType
)

enum class SpreadType { FIXED, VARIABLE, NONE }
enum class FillType { INSTANT, DELAYED, PARTIAL }

data class SimulationPerformance(
    val winRate: Float,
    val lossRate: Float,
    val profitFactor: Float,
    val drawdown: Float,
    val totalTrades: Int,
    val equityCurve: List<Double>
)

data class SimulationControlState(
    val isRunning: Boolean,
    val isPaused: Boolean,
    val speed: SimulationSpeed,
    val currentBarIndex: Int = 0,
    val totalBars: Int = 0
)

enum class SimulationSpeed { NORMAL, FAST, VERY_FAST }

enum class MarketScenarioCondition { CONSOLIDATING, TRENDING_UP, TRENDING_DOWN, REVERSAL_SETUP }
enum class MarketPressure { BULLISH, BEARISH, NEUTRAL }
enum class VolumeSignal { LOW, NORMAL, RISING, CLIMAX }
enum class StrategySimulationMode { BREAKOUT, BOS_RETEST, RANGE_BOUNCE, REVERSAL_CONFIRMATION }
enum class MarketProjection { BULLISH_BREAKOUT, BEARISH_BREAKDOWN, RANGE_CONTINUATION, REVERSAL_UP, REVERSAL_DOWN, WAIT_FOR_CONFIRMATION }

data class ScenarioSimulationInput(
    val symbol: String,
    val timeframe: String,
    val currentPrice: Double,
    val supportPrice: Double,
    val resistancePrice: Double,
    val condition: MarketScenarioCondition,
    val pressure: MarketPressure,
    val volume: VolumeSignal,
    val strategy: StrategySimulationMode,
    val bosEnabled: Boolean,
    val chochEnabled: Boolean,
    val orderBlocksEnabled: Boolean,
    val fvgEnabled: Boolean,
    val liquiditySweepEnabled: Boolean,
    val premiumDiscountEnabled: Boolean,
    val zoneStrength: Float,
    val touchRule: TouchRule,
    val riskPercent: Double
)

data class StrategySimulationResult(
    val projection: MarketProjection,
    val confidence: Float,
    val expectedMovePips: Int,
    val suggestedAction: String,
    val bestStrategy: StrategySimulationMode,
    val entryPlan: String,
    val invalidation: String,
    val riskPlan: String,
    val expectedWinRate: Float,
    val profitFactor: Float,
    val expectedDrawdown: Float,
    val simulatedTrades: Int,
    val reasoning: List<String>
)
