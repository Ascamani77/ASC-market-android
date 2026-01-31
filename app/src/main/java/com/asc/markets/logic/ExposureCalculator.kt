
package com.asc.markets.logic

import com.asc.markets.data.AutomatedTrade
import kotlin.math.roundToInt

/**
 * PURE utility for net currency exposure calculation.
 * Logic Parity: BUY base = +, BUY quote = -. SELL reverses.
 */
object ExposureCalculator {
    fun calculate(trades: List<AutomatedTrade>): Map<String, Double> {
        val exposure = mutableMapOf<String, Double>()

        for (trade in trades) {
            val cleanSymbol = trade.pair.replace("/", "").replace("-", "").uppercase()
            if (cleanSymbol.length < 6) continue

            val base = cleanSymbol.substring(0, 3)
            val quote = cleanSymbol.substring(3, 6)
            
            // Standard size mapping parity
            val size = 1.0 
            val isBuy = trade.side.uppercase() == "BUY"

            val baseMultiplier = if (isBuy) 1.0 else -1.0
            val quoteMultiplier = if (isBuy) -1.0 else 1.0

            exposure[base] = (exposure[base] ?: 0.0) + (size * baseMultiplier)
            exposure[quote] = (exposure[quote] ?: 0.0) + (size * quoteMultiplier)
        }

        // Precision Parity: standard JS rounding to 8 decimals or fixed scaling
        return exposure.mapValues { (_, value) -> 
            (value * 100.0).roundToInt() / 100.0
        }
    }
}
