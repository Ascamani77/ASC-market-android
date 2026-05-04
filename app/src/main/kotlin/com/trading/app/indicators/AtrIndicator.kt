package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color
import kotlin.math.abs

class AtrIndicator(private val period: Int = 14) : TradingIndicator {
    override val id = "ATR"
    override val name = "ATR"
    override val color = Color.parseColor("#F23645") // Red

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        if (candles.size <= 1) return List(candles.size) { null }

        val tr = mutableListOf<Float>()
        for (i in 1 until candles.size) {
            val h = candles[i].high
            val l = candles[i].low
            val pc = candles[i - 1].close
            
            val trValue = maxOf(h - l, maxOf(abs(h - pc), abs(l - pc)))
            tr.add(trValue)
        }

        if (tr.size < period) return List(candles.size) { null }

        val results = mutableListOf<Float?>()
        for (i in 0 until period) {
            results.add(null)
        }

        // RMA Smoothing
        var currentAtr = tr.take(period).average().toFloat()
        results.add(currentAtr)

        for (i in period until tr.size) {
            currentAtr = (currentAtr * (period - 1) + tr[i]) / period
            results.add(currentAtr)
        }

        return results
    }
}
