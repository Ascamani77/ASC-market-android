package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color

class VolumeSpikeIndicator(private val multiplier: Float = 2.0f) : TradingIndicator {
    override val id = "VOL_SPIKE"
    override val name = "Volume Spike"
    override val color = Color.parseColor("#FFD700") // Gold

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        if (candles.size < 20) return List(candles.size) { null }
        
        val results = mutableListOf<Float?>()
        // Fill initial nulls
        for (i in 0 until 19) {
            results.add(null)
        }

        for (i in 19 until candles.size) {
            val window = candles.subList(i - 19, i)
            val avgVol = window.map { it.volume }.average().toFloat()
            
            // Return 1.0f if spike, 0.0f if normal
            if (candles[i].volume >= avgVol * multiplier) {
                results.add(1.0f)
            } else {
                results.add(0.0f)
            }
        }
        
        return results
    }
}
