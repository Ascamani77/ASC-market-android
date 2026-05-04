package com.trading.app.ui.chart

import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.LineData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndicatorCalculator @Inject constructor() {

    fun calculateRsi(data: List<CandlestickData>, period: Int = 14): List<LineData> {
        if (period <= 0 || data.size <= period) return emptyList()

        val results = mutableListOf<LineData>()
        var gainSum = 0.0
        var lossSum = 0.0

        for (index in 1..period) {
            val change = (data[index].close - data[index - 1].close).toDouble()
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

        results.add(LineData(data[period].time, calculateRsiValue(avgGain, avgLoss)))

        for (index in period + 1 until data.size) {
            val change = (data[index].close - data[index - 1].close).toDouble()
            val gain = if (change > 0.0) change else 0.0
            val loss = if (change < 0.0) -change else 0.0
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
            results.add(LineData(data[index].time, calculateRsiValue(avgGain, avgLoss)))
        }

        return results
    }

    fun calculateSma(data: List<LineData>, period: Int): List<LineData> {
        if (data.size < period) return emptyList()
        val results = mutableListOf<LineData>()
        for (i in period - 1 until data.size) {
            val sum = data.subList(i - period + 1, i + 1).map { it.value }.sum()
            results.add(LineData(data[i].time, sum / period))
        }
        return results
    }
}
