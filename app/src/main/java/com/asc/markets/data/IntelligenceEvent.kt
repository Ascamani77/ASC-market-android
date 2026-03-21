package com.asc.markets.data

import kotlinx.serialization.Serializable

@Serializable
enum class AssetClass {
    macro, index, stock, commodity, forex, futures, bond
}

@Serializable
enum class IntelligenceEventType {
    release, earnings, policy, supply, risk, curve_shift
}

@Serializable
enum class IntelligenceSeverity {
    normal, high, critical
}

@Serializable
enum class SurpriseLevel {
    low, medium, high
}

@Serializable
enum class SourceType {
    real, derived
}

@Serializable
enum class ConvictionTier {
    high, moderate, informational
}

@Serializable
enum class CurveDirection {
    steepening, compression, flat
}

@Serializable
enum class TransitionStatus {
    active, aborted, confirmed
}

@Serializable
enum class RegimeState {
    RISK_ON, RISK_OFF, HAWKISH, DOVISH, VOLATILE, REGIME_NEUTRAL
}

@Serializable
enum class VisualState {
    NEUTRAL, TRANSITION_WATCH, DIRECTIONAL_CONFIRMED, HIGH_CONVICTION
}

@Serializable
enum class IntelligenceUnlockState {
    LOCKED, SOFT_UNLOCK, HARD_UNLOCK
}

@Serializable
enum class IntelligenceEBCStatus {
    PERMITTED, DEGRADED, BLOCKED
}

@Serializable
data class IntelligenceTransitionTrigger(
    val label: String,
    val status: String // 'met' | 'pending'
)

@Serializable
data class StrategyEligibility(
    val bias: String, // 'long' | 'short' | 'neutral' | 'bi-directional'
    val `class`: String,
    val risk_posture: String, // 'aggressive' | 'defensive' | 'halted'
    val rationale: String
)

@Serializable
data class IntelligenceExecutionBoundaryContract(
    val status: IntelligenceEBCStatus,
    val friction_coefficient: Double = 0.0,
    val alpha_erosion: Double = 0.0,
    val violations: List<String> = emptyList(),
    val is_observation_only: Boolean = false
)

@Serializable
data class IntelligenceEvent(
    val id: String,
    val asset_class: AssetClass,
    val source: String,
    val source_type: SourceType,
    val event_type: IntelligenceEventType,
    val timestamp_utc: Long,
    val assets_affected: List<String>,
    val title: String,
    
    val actual: Double? = null,
    val estimate: Double? = null,
    val previous: Double? = null,
    val unit: String? = null,
    val surprise_delta: Double? = null,
    val surprise_level: SurpriseLevel? = null,
    
    val sentiment_divergence: Boolean? = null,
    val correlation_heat: Int? = null,
    val liquidity_depth: Int? = null,
    
    val strategy_context: StrategyEligibility? = null,
    val ebc: IntelligenceExecutionBoundaryContract? = null,
    
    val short_rate: Double? = null,
    val long_rate: Double? = null,
    val curve_direction: CurveDirection? = null,
    
    val execution_regime: RegimeState,
    val structural_regime: RegimeState? = null,
    val visual_state: VisualState,
    
    val persistence_count: Int,
    val transition_status: TransitionStatus,
    val volatility_confirmed: Boolean,
    
    val severity: IntelligenceSeverity,
    val safety_gate: Boolean,
    val unlock_state: IntelligenceUnlockState,
    val gate_release_time: Long? = null,
    val hard_unlock_time: Long? = null,
    val is_stabilizing: Boolean? = null,
    
    val neutral_drivers: List<String>? = null,
    val transition_triggers: List<IntelligenceTransitionTrigger>? = null,
    val allowed_tactics: List<String>? = null,
    
    val confidence_score: Double? = null,
    val base_confidence: Double? = null,
    val conviction_tier: ConvictionTier? = null,
    
    val terminal_rate_repriced: Boolean? = null,
    val narrative_summary: String,
    val model_id: String? = null,
    val parent_event_id: String? = null
)
