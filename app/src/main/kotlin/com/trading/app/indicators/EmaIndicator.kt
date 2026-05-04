package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color

class EmaIndicator(private val period: Int = 14) : TradingIndicator {
    override val id = "EMA_$period"
    override val name = "EMA $period"
    override val color = Color.parseColor("#2962FF") // Blue

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        if (candles.size < period) return List(candles.size) { null }

        val results = mutableListOf<Float?>()
        val multiplier = 2f / (period + 1)

        // Start with SMA for the first EMA value
        val initialSma = candles.take(period).map { it.close }.average().toFloat()
        
        for (i in 0 until period - 1) {
            results.add(null)
        }
        results.add(initialSma)

        var prevEma = initialSma
        for (i in period until candles.size) {
            val currentEma = (candles[i].close - prevEma) * multiplier + prevEma
            results.add(currentEma)
            prevEma = currentEma
        }

        return results
    }
}
