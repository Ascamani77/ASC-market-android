package com.intelligence.dashboard.repository

import com.intelligence.dashboard.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class IntelligenceRepository {
    
    fun getIntelligenceEvents(): Flow<List<IntelligenceEvent>> = flow {
        val now = System.currentTimeMillis()
        val events = mutableListOf<IntelligenceEvent>()
        
        // Initial mock data mirroring ASCEvent structure
        events.addAll(listOf(
            IntelligenceEvent(
                id = UUID.randomUUID().toString(),
                asset_class = AssetClass.macro,
                source = "FED-RESERVE-NY",
                source_type = SourceType.real,
                event_type = EventType.policy,
                timestamp_utc = now - 1200000, // 20 mins ago
                assets_affected = listOf("USD", "UST-10Y", "SPX"),
                title = "Emergency Rate Adjustment Protocol",
                actual = 5.25,
                estimate = 5.00,
                previous = 5.00,
                unit = "%",
                surprise_delta = 0.05,
                surprise_level = SurpriseLevel.medium,
                correlation_heat = 85,
                liquidity_depth = 45,
                strategy_context = StrategyEligibility(
                    bias = "short",
                    `class` = "macro",
                    risk_posture = "defensive",
                    rationale = "Unexpected hawkish tilt in liquidity provision requires immediate delta-neutral positioning."
                ),
                ebc = ExecutionBoundaryContract(
                    status = EBCStatus.BLOCKED,
                    friction_coefficient = 0.75,
                    alpha_erosion = 0.12,
                    violations = listOf("HARD_LOCK: Macro safety gate actively suppressing all entry.", "LIQUIDITY_GAP: Depth below threshold"),
                    is_observation_only = true
                ),
                transition_triggers = listOf(
                    TransitionTrigger("LIQUIDITY_THRESHOLD", "met"),
                    TransitionTrigger("YIELD_CURVE_STABILITY", "pending"),
                    TransitionTrigger("VOLATILITY_INDEX_DROP", "met")
                ),
                execution_regime = RegimeState.VOLATILE,
                visual_state = VisualState.TRANSITION_WATCH,
                persistence_count = 1,
                transition_status = TransitionStatus.active,
                volatility_confirmed = true,
                severity = Severity.critical,
                safety_gate = true,
                unlock_state = UnlockState.LOCKED,
                gate_release_time = now + 1800000, // 30 mins from now
                hard_unlock_time = now + 2700000,
                narrative_summary = "Federal Reserve New York branch initiates emergency liquidity withdrawal. Market depth collapsing across major pairs."
            ),
            IntelligenceEvent(
                id = UUID.randomUUID().toString(),
                asset_class = AssetClass.forex,
                source = "ECB-TERMINAL",
                source_type = SourceType.real,
                event_type = EventType.release,
                timestamp_utc = now - 3600000, // 1 hour ago
                assets_affected = listOf("EUR", "GER-30", "BUND"),
                title = "Eurozone CPI Flash Estimate",
                actual = 2.4,
                estimate = 2.6,
                previous = 2.8,
                unit = "%",
                surprise_delta = 0.08,
                surprise_level = SurpriseLevel.low,
                correlation_heat = 62,
                liquidity_depth = 78,
                strategy_context = StrategyEligibility(
                    bias = "long",
                    `class` = "forex",
                    risk_posture = "aggressive",
                    rationale = "Disinflationary trend accelerating beyond consensus. ECB pivot probability increasing."
                ),
                ebc = ExecutionBoundaryContract(
                    status = EBCStatus.PERMITTED,
                    friction_coefficient = 0.15,
                    alpha_erosion = 0.02,
                    violations = emptyList(),
                    is_observation_only = false
                ),
                transition_triggers = listOf(
                    TransitionTrigger("CPI_THRESHOLD", "met"),
                    TransitionTrigger("PIVOT_PROBABILITY", "met"),
                    TransitionTrigger("RECOVERY_SIGNAL", "pending")
                ),
                execution_regime = RegimeState.DOVISH,
                visual_state = VisualState.DIRECTIONAL_CONFIRMED,
                persistence_count = 2,
                transition_status = TransitionStatus.confirmed,
                volatility_confirmed = false,
                severity = Severity.normal,
                safety_gate = false,
                unlock_state = UnlockState.HARD_UNLOCK,
                narrative_summary = "Eurozone inflation cools faster than expected. Market pricing in June rate cut with 90% certainty."
            )
        ))
        
        emit(events.toList())
        
        while (true) {
            delay(30000)
            // Add more dynamic events if needed
        }
    }
}
