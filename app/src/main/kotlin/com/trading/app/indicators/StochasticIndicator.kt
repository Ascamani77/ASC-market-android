package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color

class StochasticIndicator(private val period: Int = 14) : TradingIndicator {
    override val id = "STOCH"
    override val name = "Stochastic"
    override val color = Color.parseColor("#7E57C2") // Purple

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        if (candles.size < period) return List(candles.size) { null }

        val results = mutableListOf<Float?>()
        for (i in 0 until period - 1) {
            results.add(null)
        }

        for (i in period - 1 until candles.size) {
            val window = candles.subList(i - period + 1, i + 1)
            val high = window.maxOf { it.high }
            val low = window.minOf { it.low }
            val close = candles[i].close

            if (high - low != 0f) {
                val kLine = ((close - low) / (high - low)) * 100f
                results.add(kLine)
            } else {
                results.add(50f)
            }
        }

        return results
    }
}
