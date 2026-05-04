package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color
import kotlin.math.sqrt

data class BbandsData(
    val upperBand: List<Float?>,
    val middleBand: List<Float?>,
    val lowerBand: List<Float?>
) {
    val latestUpperBand: Float?
        get() = upperBand.lastOrNull { it != null }

    val latestMiddleBand: Float?
        get() = middleBand.lastOrNull { it != null }

    val latestLowerBand: Float?
        get() = lowerBand.lastOrNull { it != null }
}

class BbandsIndicator(private val period: Int = 20, private val stdDev: Float = 2f) : TradingIndicator {
    override val id = "BBANDS"
    override val name = "Bollinger Bands"
    override val color = Color.parseColor("#2962FF") // Blue

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        return calculateBands(candles).middleBand
    }

    fun calculateBands(candles: List<OHLCData>): BbandsData {
        if (candles.size < period || period <= 0) {
            val empty = List(candles.size) { null }
            return BbandsData(
                upperBand = empty,
                middleBand = empty,
                lowerBand = empty
            )
        }

        val upperBand = MutableList<Float?>(candles.size) { null }
        val middleBand = MutableList<Float?>(candles.size) { null }
        val lowerBand = MutableList<Float?>(candles.size) { null }
        val closes = candles.map { it.close }

        for (i in period - 1 until closes.size) {
            val window = closes.subList(i - period + 1, i + 1)
            val sma = window.average().toFloat()
            val variance = window.map { (it - sma) * (it - sma) }.average().toFloat()
            val deviation = sqrt(variance) * stdDev

            middleBand[i] = sma
            upperBand[i] = sma + deviation
            lowerBand[i] = sma - deviation
        }

        return BbandsData(
            upperBand = upperBand,
            middleBand = middleBand,
            lowerBand = lowerBand
        )
    }
}
