
package com.asc.markets.logic

import com.asc.markets.data.AutomatedTrade
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Acceptance Tests: Logic Parity with TS codebase.
 */
class EngineParityTests {

    @Test
    fun testExposureCalculationParity() {
        // Fixture 1: Correlated Major Pair
        val trades = listOf(
            AutomatedTrade(id = "1", pair = "EUR/USD", side = "BUY", status = "OPEN", entryPrice = "1.0842", reasoning = "fixture"),
            AutomatedTrade(id = "2", pair = "GBP/USD", side = "SELL", status = "OPEN", entryPrice = "1.2630", reasoning = "fixture")
        )
        
        val exposure = ExposureCalculator.calculate(trades)
        
        // TS Logic: BUY EUR/USD (+1 EUR, -1 USD); SELL GBP/USD (-1 GBP, +1 USD)
        // Expected Net USD: 0.0
        assertEquals(1.0, exposure["EUR"] ?: 0.0, 0.001)
        assertEquals(-1.0, exposure["GBP"] ?: 0.0, 0.001)
        assertEquals(0.0, exposure["USD"] ?: 0.0, 0.001)
    }

    @Test
    fun testSafetyGateProximityParity() {
        // Fixture 2: High impact window
        // CalendarService mock state should replicate the ±30m news window
        val status = CalendarService.getTradingStatus("EUR/USD")
        
        // Assert that the safety gate blocks execution as it does in TS
        assertEquals(true, status.isBlocked)
        assert(status.reason?.contains("SAFETY GATE") == true)
    }
}
