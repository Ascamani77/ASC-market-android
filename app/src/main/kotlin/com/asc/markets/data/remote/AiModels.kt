package com.asc.markets.data.remote

data class RunAiRequest(
    val mode: String = "full"
)

data class FinalDecisionItem(
    val journal_timestamp: String? = null,
    val asset_1: String? = null,
    val journal_direction: String? = null,
    val journal_label: String? = null,
    val journal_priority: String? = null,
    val journal_score: Double? = null,
    val ignition_probability: Double? = null,
    val ignition_decile: Int? = null,
    val expansion_probability: Double? = null,
    val confluence_score: Double? = null,
    val confluence_count: Int? = null,
    val confluence_total: Int? = null,
    val entry_window: String? = null,
    val entry_quality_score: Double? = null,
    val exit_plan: String? = null,
    val exit_pressure_score: Double? = null,
    val portfolio_decision_label: String? = null,
    val portfolio_deployment_bucket: String? = null,
    val recommended_position_scale: Double? = null,
    val recommended_risk_pct: Double? = null,
    val recommended_risk_amount: Double? = null,
    val final_position_scale: Double? = null,
    val final_risk_pct: Double? = null,
    val final_risk_amount: Double? = null,
    val portfolio_decision_reason: String? = null,
    val correlation_regime: String? = null,
    val correlation_risk_score: Double? = null,
    val correlation_warning: String? = null,
    val regime_persistence_score: Double? = null,
    val regime_transition_probability: Double? = null,
    val structural_pressure_score: Double? = null,
    val structural_pressure_label: String? = null,
    val live_tick_status: String? = null,
    val live_tick_count: Int? = null,
    val directional_score: Double? = null,
    val direction_confidence: Double? = null,
    val source_timeframe: String? = null,
    val mtf_alignment_score: Double? = null,
    val regime_state: String? = null,
    val trend_state: String? = null,
    val feeder_volatility_state: String? = null,
    val feeder_volatility_score: Double? = null,
    val feeder_volatility_confidence: String? = null,
    val feeder_volatility_reason: String? = null,
    val atr_ratio: Double? = null,
    val vol_ratio: Double? = null,
    val burst_ratio: Double? = null,
    val confluence_state: String? = null,
    val confluence_label: String? = null,
    val confluence_confidence: String? = null,
    val confluence_bias: String? = null,
    val chart_context_state: String? = null,
    val chart_context_label: String? = null,
    val chart_context_score: Double? = null,
    val chart_context_confidence: String? = null,
    val chart_context_bias: String? = null,
    val structure_state: String? = null,
    val structure_label: String? = null,
    val structure_score: Double? = null,
    val structure_confidence: String? = null,
    val structure_bias: String? = null,
    val pre_move_ai_score: Double? = null,
    val pre_move_ai_phase: String? = null,
    val pre_move_ai_curve: List<Double>? = null,
    // Feeder Pipeline Status
    val entry_state: String? = null,
    val plan_state: String? = null,
    val execution_status: String? = null,
    val signal_quality_state: String? = null,
    val feeder_risk_state: String? = null,
    val feeder_liquidity_state: String? = null,
    val feeder_indicator_state: String? = null,
    // Final Trading AI fields
    val final_trade_direction: String? = null,
    val final_trade_state: String? = null,
    val final_trade_label: String? = null,
    val final_trade_confidence: String? = null,
    val final_trade_score: Double? = null,
    val final_trade_priority: String? = null,
    val final_trade_reason: String? = null
)

data class RunAiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val final_decision: List<FinalDecisionItem> = emptyList()
)

data class ChartAnalysisRequest(
    val image_base64: String,
    val metadata: Map<String, String> = emptyMap()
)

data class LatestDeploymentsResponse(
    val success: Boolean = false,
    val last_updated: String? = null,
    val count: Int = 0,
    val final_decision: List<FinalDecisionItem> = emptyList()
)

data class MarketUpdateRequest(
    val assets: Map<String, MarketAssetSnapshot>
)

data class MarketAssetSnapshot(
    val price: Double,
    val timestamp: String,
    val bid: Double? = null,
    val ask: Double? = null,
    val volume: Double? = null
)
