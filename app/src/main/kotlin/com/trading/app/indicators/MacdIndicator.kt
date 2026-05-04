package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color

class MacdIndicator(
    private val fastPeriod: Int = 12,
    private val slowPeriod: Int = 26,
    private val signalPeriod: Int = 9
) : TradingIndicator {
    override val id = "MACD"
    override val name = "MACD"
    override val color = Color.parseColor("#2962FF") // Blue

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        // This method will now return the MACD line as primary value
        // To support all three lines in the UI, we might need a more complex return type
        // or multiple calculations. For now, let's keep it simple.
        return calculateMacdLine(candles)
    }

    fun calculateMacdLine(candles: List<OHLCData>): List<Float?> {
        if (candles.size < slowPeriod) return List(candles.size) { null }

        val fastEma = calculateEma(candles.map { it.close }, fastPeriod)
        val slowEma = calculateEma(candles.map { it.close }, slowPeriod)

        val macdLine = mutableListOf<Float?>()
        for (i in candles.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                macdLine.add(f - s)
            } else {
                macdLine.add(null)
            }
        }
        return macdLine
    }

    fun calculateSignalLine(macdLine: List<Float?>): List<Float?> {
        val nonNullMacd = macdLine.filterNotNull()
        if (nonNullMacd.size < signalPeriod) return List(macdLine.size) { null }

        val multiplier = 2f / (signalPeriod + 1)
        val initialSma = nonNullMacd.take(signalPeriod).average().toFloat()

        val signalLine = mutableListOf<Float?>()
        val firstNonNullIndex = macdLine.indexOfFirst { it != null }
        
        for (i in 0 until firstNonNullIndex + signalPeriod - 1) {
            signalLine.add(null)
        }
        signalLine.add(initialSma)

        var prevEma = initialSma
        for (i in (firstNonNullIndex + signalPeriod) until macdLine.size) {
            val currentMacd = macdLine[i]
            if (currentMacd != null) {
                val currentEma = (currentMacd - prevEma) * multiplier + prevEma
                signalLine.add(currentEma)
                prevEma = currentEma
            } else {
                signalLine.add(null)
            }
        }
        return signalLine
    }

    fun calculateHistogram(macdLine: List<Float?>, signalLine: List<Float?>): List<Float?> {
        val histogram = mutableListOf<Float?>()
        for (i in macdLine.indices) {
            val m = macdLine[i]
            val s = signalLine[i]
            if (m != null && s != null) {
                histogram.add(m - s)
            } else {
                histogram.add(null)
            }
        }
        return histogram
    }

    private fun calculateEma(values: List<Float>, period: Int): List<Float?> {
        val results = mutableListOf<Float?>()
        val multiplier = 2f / (period + 1)
        val initialSma = values.take(period).average().toFloat()

        for (i in 0 until period - 1) {
            results.add(null)
        }
        results.add(initialSma)

        var prevEma = initialSma
        for (i in period until values.size) {
            val currentEma = (values[i] - prevEma) * multiplier + prevEma
            results.add(currentEma)
            prevEma = currentEma
        }
        return results
    }
}
