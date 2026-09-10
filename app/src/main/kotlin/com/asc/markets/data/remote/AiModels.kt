package com.asc.markets.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class LatestDeploymentsResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val last_updated: String? = null,
    val final_decision: List<FinalDecisionItem> = emptyList()
)

@Serializable
data class FinalDecisionItem(
    val asset_1: String? = null,
    val journal_direction: String? = null,
    val journal_label: String? = null,
    val journal_score: Double? = null,
    val journal_priority: String? = null,
    val pre_move_ai_score: Double? = null,
    val ignition_probability: Double? = null,
    val expansion_probability: Double? = null,
    val entry_window: String? = null,
    val portfolio_decision_label: String? = null,
    val portfolio_deployment_bucket: String? = null,
    val final_trade_state: String? = null,
    val feeder_volatility_score: Double? = null,
    val structural_pressure_score: Double? = null,
    val chart_context_score: Double? = null,
    val feeder_risk_state: String? = null,
    val feeder_risk_score: Double? = null,
    val monitoring_confidence: Double? = null,
    
    // Additional fields for UI compatibility
    val structure_score: Double? = null,
    val journal_timestamp: String? = null,
    val journal_timestamp_utc: Long? = null,
    val directional_score: Double? = null,
    val regime_state: String? = null,
    val live_tick_count: Int? = null,
    val direction_confidence: Double? = null,
    val trend_state: String? = null,
    val portfolio_decision_reason: String? = null,
    val chart_context_label: String? = null,
    val structure_label: String? = null,
    val final_trade_score: Double? = null,
    val final_trade_direction: String? = null,
    val final_trade_label: String? = null,
    val final_trade_confidence: String? = null,
    val final_trade_reason: String? = null,
    val confluence_score: Double? = null,
    val confluence_state: String? = null,
    val confluence_label: String? = null,
    val confluence_total: Int? = null,
    val confluence_count: Int? = null,
    val entry_state: String? = null,
    val entry_quality_score: Double? = null,
    val plan_state: String? = null,
    val exit_plan: String? = null,
    val execution_status: String? = null,
    val exit_pressure_score: Double? = null,
    val signal_quality_state: String? = null,
    val feeder_volatility_state: String? = null,
    val feeder_indicator_state: String? = null,
    val feeder_liquidity_state: String? = null,
    val feeder_indicator_bias: String? = null,
    val feeder_liquidity_bias: String? = null,
    val feeder_volatility_reason: String? = null,
    val final_position_scale: Double? = null,
    val recommended_position_scale: Double? = null,
    val final_risk_pct: Double? = null,
    val recommended_risk_pct: Double? = null,
    val final_risk_amount: Double? = null,
    val recommended_risk_amount: Double? = null,
    val pre_move_ai_phase: String? = null,
    val regime_persistence_score: Double? = null,
    val regime_transition_probability: Double? = null,
    val mtf_alignment_score: Double? = null,
    val ignition_decile: Int? = null,
    val live_tick_status: String? = null,
    val source_timeframe: String? = null,
    val atr_ratio: Double? = null,
    val vol_ratio: Double? = null,
    val burst_ratio: Double? = null,
    val generated_at: String? = null,
    val correlation_warning: String? = null,
    val correlation_regime: String? = null,
    val correlation_risk_score: Double? = null,
    val structural_pressure_label: String? = null,
    val structure_state: String? = null,
    val feeder_indicator_score: Double? = null,
    val feeder_liquidity_score: Double? = null,
    val immediate_momentum_pct: Double? = null,
    val short_momentum_pct: Double? = null,
    val medium_momentum_pct: Double? = null,
    val confluence_total_score: Double? = null,
    val confluence_count_active: Int? = null
)

@Serializable
data class HybridSignalsResponse(
    val signals: List<HybridSignalItem> = emptyList()
)

@Serializable
data class HybridSignalItem(
    val asset: String? = null,
    val direction: String? = null,
    val confidence: Double? = null,
    val p_trade: Double? = null,
    val combined: Double? = null,
    val ts: Double? = null
)

@Serializable
data class RunAiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val final_decision: List<FinalDecisionItem> = emptyList()
)

@Serializable
data class RunAiRequest(
    val user_query: String? = null,
    val persona: String? = null,
    val instruction: String? = null,
    val mode: String? = null
)

@Serializable
data class ScalpingSignal(
    val asset: String,
    val signal: String,
    val confidence: String,
    val timeframe: String,
    val strategy: String,
    val immediate_momentum_pct: Double? = null,
    val short_momentum_pct: Double? = null,
    val medium_momentum_pct: Double? = null,
    val feeder_volatility_score: Double? = null,
    val feeder_volatility_state: String? = null,
    val feeder_indicator_score: Double? = null,
    val feeder_indicator_bias: String? = null,
    val feeder_liquidity_score: Double? = null,
    val feeder_liquidity_bias: String? = null,
    val feeder_risk_score: Double? = null,
    val feeder_risk_state: String? = null,
    val confluence_score: Double? = null,
    val expansion_probability: Double? = null,
    val generated_at: String? = null,
    val volatility: Double? = null
)

@Serializable
data class ScalpingSignalsResponse(
    val success: Boolean = false,
    val message: String? = null,
    val signals: List<ScalpingSignal> = emptyList(),
    val error: String? = null
)

@Serializable
data class MarketAssetSnapshot(
    val price: Double,
    val timestamp: String,
    val bid: Double? = null,
    val ask: Double? = null,
    val volume: Double? = null
)

@Serializable
data class MarketUpdateRequest(
    val assets: Map<String, MarketAssetSnapshot>
)
