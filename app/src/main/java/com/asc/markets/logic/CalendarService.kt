package com.asc.markets.logic

import com.asc.markets.data.EconomicEvent
import java.util.*
import kotlin.math.abs

data class SurpriseMetadata(
    val detected: Boolean,
    val level: String, // "low", "medium", "high"
    val delta: Double
)

data class TradingStatus(
    val pair: String,
    val isBlocked: Boolean,
    val reason: String? = null,
    val resumeTime: String? = null,
    val countdownMinutes: Int? = null
)

object CalendarService {
    // All data comes from the API - no mock events

    /**
     * Surprise Magnitude Engine: Calculates deviation magnitude.
     * Parity with TS: computeSurprise()
     */
    fun calculateSurprise(actual: Double?, estimate: Double?): SurpriseMetadata? {
        if (actual == null || estimate == null || estimate == 0.0) return null
        val delta = actual - estimate
        val strength = abs(delta) / abs(estimate)
        
        return SurpriseMetadata(
            detected = actual != estimate,
            delta = delta,
            level = when {
                strength >= 0.25 -> "high"
                strength >= 0.10 -> "medium"
                else -> "low"
            }
        )
    }

    /**
     * Safety Gate Logic: All trading status checks are now handled via the API
     * Real data flows from the backend - no local trading status evaluation
     */
    fun getTradingStatus(pair: String): TradingStatus {
        // Trading status now comes from API-provided ebc_status and unlock_state
        return TradingStatus(pair, false)
    }
}