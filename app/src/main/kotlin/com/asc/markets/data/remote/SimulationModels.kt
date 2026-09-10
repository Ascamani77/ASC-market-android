package com.asc.markets.data.remote

import com.google.gson.annotations.SerializedName

data class TradeSimulationRequest(
    val asset: String,
    val direction: String,
    @SerializedName("entry_price") val entryPrice: Double,
    @SerializedName("stop_loss") val stopLoss: Double,
    @SerializedName("take_profit") val takeProfit: Double
)

data class TradeSimulationResponse(
    @SerializedName("simulation_id") val simulationId: String,
    val asset: String,
    val direction: String,
    @SerializedName("entry_price") val entryPrice: Double,
    @SerializedName("stop_loss") val stopLoss: Double,
    @SerializedName("take_profit") val takeProfit: Double,
    @SerializedName("risk_reward_ratio") val riskRewardRatio: Double,
    
    @SerializedName("win_probability") val winProbability: Double,
    @SerializedName("expected_pnl_pips") val expectedPnlPips: Double,
    @SerializedName("outcome_prediction") val outcomePrediction: String,
    @SerializedName("confidence_score") val confidenceScore: Double,
    
    val recommendation: String,
    @SerializedName("position_size_suggestion") val positionSizeSuggestion: Double,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("risk_score") val riskScore: Double,
    val reasoning: String,
    
    val institutional: InstitutionalContext,
    @SerializedName("market_context") val marketContext: MarketContext,
    val timing: TimingEstimate? = null,
    val strengths: List<String>? = null,
    @SerializedName("risk_factors") val riskFactors: List<String>? = null,
    
    @SerializedName("simulation_time") val simulationTime: String? = null,
    @SerializedName("api_version") val apiVersion: String? = null,
    @SerializedName("processed_at") val processedAt: String? = null,
    val warning: String? = null
)

data class InstitutionalContext(
    @SerializedName("dispatch_state") val dispatchState: String,
    @SerializedName("dispatch_score") val dispatchScore: Double,
    @SerializedName("timing_convergence") val timingConvergence: String,
    @SerializedName("timing_alignment") val timingAlignment: Int,
    @SerializedName("market_pulse") val marketPulse: String,
    @SerializedName("pulse_compression") val pulseCompression: Double,
    @SerializedName("volatility_pulse") val volatilityPulse: String,
    @SerializedName("vol_eta_minutes") val volEtaMinutes: Int,
    @SerializedName("confluence_rating") val confluenceRating: String,
    @SerializedName("confluence_factors") val confluenceFactors: Int
)

data class MarketContext(
    @SerializedName("regime_state") val regimeState: String,
    @SerializedName("volatility_state") val volatilityState: String,
    val session: String,
    @SerializedName("mtf_alignment") val mtfAlignment: Int
)

data class TimingEstimate(
    @SerializedName("bars_to_tp_estimate") val barsToTpEstimate: Int,
    @SerializedName("bars_to_sl_estimate") val barsToSlEstimate: Int,
    @SerializedName("estimated_tp_time_minutes") val estimatedTpTimeMinutes: Int? = null,
    @SerializedName("estimated_sl_time_minutes") val estimatedSlTimeMinutes: Int? = null
)

data class SimulationStatusResponse(
    val status: String,
    val message: String,
    @SerializedName("simulation_available") val simulationAvailable: Boolean,
    @SerializedName("last_update_seconds_ago") val lastUpdateSecondsAgo: Int? = null
)


// Chart Display Settings Models
data class ChartDisplaySettingsRequest(
    val settings: Map<String, Boolean>
)

data class ChartDisplaySettingsResponse(
    val success: Boolean,
    val message: String? = null,
    val settings: Map<String, Boolean>? = null,
    @SerializedName("settings_count") val settingsCount: Int? = null,
    val timestamp: String? = null,
    val version: String? = null
)
