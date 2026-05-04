package com.trading.app.indicators

import com.trading.app.models.OHLCData
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class VwapData(
    val vwap: List<Float?>,
    val upperBand: List<Float?>,
    val lowerBand: List<Float?>
) {
    val latestVwap: Float?
        get() = vwap.lastOrNull { it != null }

    val latestUpperBand: Float?
        get() = upperBand.lastOrNull { it != null }

    val latestLowerBand: Float?
        get() = lowerBand.lastOrNull { it != null }
}

class VwapIndicator(
    private val bandMultiplier: Float = 1f,
    private val resetOnNewSession: Boolean = true
) : TradingIndicator {
    override val id = "VWAP"
    override val name = "VWAP"
    override val color = Color.parseColor("#2962FF") // Blue

    override fun calculate(candles: List<OHLCData>): List<Float?> {
        return calculateBands(candles).vwap
    }

    fun calculateBands(candles: List<OHLCData>): VwapData {
        if (candles.isEmpty()) {
            return VwapData(
                vwap = emptyList(),
                upperBand = emptyList(),
                lowerBand = emptyList()
            )
        }

        val vwapValues = MutableList<Float?>(candles.size) { null }
        val upperBandValues = MutableList<Float?>(candles.size) { null }
        val lowerBandValues = MutableList<Float?>(candles.size) { null }
        val hasRealVolume = candles.any { it.volume > 0f }

        var cumulativeWeightedPrice = 0.0
        var cumulativeWeightedSquaredPrice = 0.0
        var cumulativeVolume = 0.0
        var currentSessionKey = candles.first().time / 86_400L

        candles.forEachIndexed { index, candle ->
            val sessionKey = candle.time / 86_400L
            if (resetOnNewSession && sessionKey != currentSessionKey) {
                currentSessionKey = sessionKey
                cumulativeWeightedPrice = 0.0
                cumulativeWeightedSquaredPrice = 0.0
                cumulativeVolume = 0.0
            }

            val typicalPrice = ((candle.high + candle.low + candle.close) / 3f).toDouble()
            val weight = if (hasRealVolume) {
                candle.volume.toDouble().coerceAtLeast(0.0)
            } else {
                max(abs((candle.close - candle.open).toDouble()), (candle.high - candle.low).toDouble()).coerceAtLeast(0.0001)
            }

            cumulativeWeightedPrice += typicalPrice * weight
            cumulativeWeightedSquaredPrice += typicalPrice * typicalPrice * weight
            cumulativeVolume += weight

            if (cumulativeVolume > 0.0) {
                val vwap = cumulativeWeightedPrice / cumulativeVolume
                val variance = (cumulativeWeightedSquaredPrice / cumulativeVolume) - (vwap * vwap)
                val standardDeviation = sqrt(variance.coerceAtLeast(0.0))
                val bandDistance = standardDeviation * bandMultiplier

                vwapValues[index] = vwap.toFloat()
                upperBandValues[index] = (vwap + bandDistance).toFloat()
                lowerBandValues[index] = (vwap - bandDistance).toFloat()
            } else {
                vwapValues[index] = typicalPrice.toFloat()
                upperBandValues[index] = typicalPrice.toFloat()
                lowerBandValues[index] = typicalPrice.toFloat()
            }
        }

        return VwapData(
            vwap = vwapValues,
            upperBand = upperBandValues,
            lowerBand = lowerBandValues
        )
    }
}
