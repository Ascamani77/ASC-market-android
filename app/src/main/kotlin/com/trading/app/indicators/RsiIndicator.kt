package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color

class RsiIndicator(private val period: Int = 14, private val maPeriod: Int = 14) : TradingIndicator {
    override val id = "RSI"
    override val name = "RSI"
    override val color = Color.parseColor("#7E57C2") // Purple

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        if (period <= 0 || candles.size <= period) return List(candles.size) { null }

        val rsiValues = MutableList<Float?>(candles.size) { null }
        var gainSum = 0.0
        var lossSum = 0.0

        for (index in 1..period) {
            val change = (candles[index].close - candles[index - 1].close).toDouble()
            if (change > 0.0) {
                gainSum += change
            } else {
                lossSum -= change
            }
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        fun calculateRsiValue(gain: Double, loss: Double): Float {
            return when {
                gain == 0.0 && loss == 0.0 -> 50f
                loss == 0.0 -> 100f
                gain == 0.0 -> 0f
                else -> (100.0 - (100.0 / (1.0 + gain / loss))).toFloat()
            }
        }

        rsiValues[period] = calculateRsiValue(avgGain, avgLoss)

        for (index in period + 1 until candles.size) {
            val change = (candles[index].close - candles[index - 1].close).toDouble()
            val gain = if (change > 0.0) change else 0.0
            val loss = if (change < 0.0) -change else 0.0
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
            rsiValues[index] = calculateRsiValue(avgGain, avgLoss)
        }

        return rsiValues
    }

    fun calculateMa(rsiValues: List<Float?>): List<Float?> {
        val maValues = mutableListOf<Float?>()
        for (i in rsiValues.indices) {
            if (i < maPeriod - 1) {
                maValues.add(null)
                continue
            }
            
            val subList = rsiValues.subList(i - maPeriod + 1, i + 1)
            if (subList.any { it == null }) {
                maValues.add(null)
            } else {
                maValues.add(subList.filterNotNull().average().toFloat())
            }
        }
        return maValues
    }
}
