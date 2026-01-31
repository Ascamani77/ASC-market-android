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
    // Parity: Mocking the same critical events as the TS version
    val mockEvents = listOf(
        EconomicEvent("Non-Farm Payrolls", "USD", "HIGH", "2024-12-01", "13:30", "UTC", "ForexFactory"),
        EconomicEvent("CPI YoY", "USD", "HIGH", "2024-12-01", "12:30", "UTC", "ForexFactory"),
        EconomicEvent("FOMC Rate Decision", "USD", "HIGH", "2024-12-01", "19:00", "UTC", "ForexFactory")
    )

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
     * Safety Gate Logic: Blocks dispatches in a 30-minute window of High-Impact news.
     */
    fun getTradingStatus(pair: String): TradingStatus {
        val now = System.currentTimeMillis()
        val currencies = pair.split("/")
        
        // Check for 30m proximity to High Impact news
        mockEvents.forEach { event ->
            if (currencies.contains(event.currency) && event.impact == "HIGH") {
                // Simplified time parsing for mock purposes
                val eventTime = System.currentTimeMillis() + (15 * 60 * 1000) // Mocked as 15 mins away
                val diffMins = (eventTime - now) / (1000 * 60)
                
                if (diffMins in -30..30) {
                    return TradingStatus(
                        pair = pair,
                        isBlocked = true,
                        reason = "SAFETY GATE: ${event.event} release window active (±30m).",
                        countdownMinutes = diffMins.toInt()
                    )
                }
            }
        }
        return TradingStatus(pair, false) 
    }
}