
package com.asc.markets.logic

import com.asc.markets.data.AutomatedTrade
import org.junit.Assert.assertEquals
import org.junit.Test

class ParityTests {

    @Test
    fun testExposureParity() {
        // Fixture 1: Standard Major Pair
        val trades = listOf(
            AutomatedTrade(id = "1", pair = "EUR/USD", side = "BUY", status = "OPEN", entryPrice = "1.0", reasoning = "fixture"),
            AutomatedTrade(id = "2", pair = "GBP/USD", side = "SELL", status = "OPEN", entryPrice = "1.0", reasoning = "fixture")
        )
        
        val exposure = ExposureCalculator.calculate(trades)
        
        // TS Logic: BUY EUR/USD (+1 EUR, -1 USD); SELL GBP/USD (-1 GBP, +1 USD)
        // Expected USD Net: 0.0
        assertEquals(1.0, exposure["EUR"]!!, 0.001)
        assertEquals(-1.0, exposure["GBP"]!!, 0.001)
        assertEquals(0.0, exposure["USD"] ?: 0.0, 0.001)
    }

    @Test
    fun testSafetyGateParity() {
        // Test proximity blocking logic
        val status = CalendarService.getTradingStatus("EUR/USD")
        // Mock current time matches news release window in CalendarService
        assertEquals(true, status.isBlocked)
        assert(status.reason?.contains("SAFETY GATE") == true)
    }
}
