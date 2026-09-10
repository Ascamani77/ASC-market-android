package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.abs

/**
 * Port of TradingView "Auto Fib Retracement" (Pine v6, TradingView/ZigZag lib 7).
 *
 * ZigZag: a bar becomes a new pivot when price deviates from the previous pivot
 * by more than threshold = ATR(10)/close*100 * Deviation (percent, dynamic),
 * with at least `depth` bars between pivots. Fib levels are anchored on the last
 * two pivots: ratio 0 at the newest pivot extreme, ratio 1 at the previous pivot.
 */
data class AutoFibLevel(
    val ratio: Float,
    val price: Float,
    val colorInt: Int
)

data class AutoFibData(
    val leftTime: Long,
    val leftPrice: Float,
    val rightTime: Long,
    val rightPrice: Float,
    val isUpSwing: Boolean,
    /** Shown levels ordered like Pine's processLevel chain (ascending ratio). */
    val levels: List<AutoFibLevel>
)

/** One editable fib level row (mirrors a Pine input triple show/value/color). */
data class AutoFibLevelState(
    var shown: Boolean,
    var ratio: Float,
    var colorInt: Int
)

data class AutoFibSettings(
    val deviation: Float = 3f,
    val depth: Int = 10,
    val reverse: Boolean = false,
    val extendLeft: Boolean = false,
    val extendRight: Boolean = true,
    val showPrices: Boolean = true,
    val showLevels: Boolean = true,
    val levelsFormatValues: Boolean = true,
    val labelsPositionLeft: Boolean = true,
    val backgroundTransparency: Int = 85,
    val levels: List<AutoFibLevelState> = AutoFibSettings.defaultLevels()
) {
    companion object {
        /**
         * Full Pine level table, in processLevel chain order (lineId0 -> lineId21):
         * show default / value / color exactly as the script inputs define them.
         */
        fun defaultLevels(): List<AutoFibLevelState> {
            fun lvl(shown: Boolean, ratio: Float, hex: String) =
                AutoFibLevelState(shown, ratio, android.graphics.Color.parseColor(hex))
            return listOf(
                lvl(false, -0.65f, "#009688"),
                lvl(false, -0.618f, "#009688"),
                lvl(false, -0.382f, "#81c784"),
                lvl(false, -0.236f, "#f44336"),
                lvl(true, 0f, "#787b86"),
                lvl(true, 0.236f, "#f44336"),
                lvl(true, 0.382f, "#81c784"),
                lvl(true, 0.5f, "#4caf50"),
                lvl(true, 0.618f, "#009688"),
                lvl(false, 0.65f, "#009688"),
                lvl(true, 0.786f, "#64b5f6"),
                lvl(true, 1f, "#787b86"),
                lvl(false, 1.272f, "#81c784"),
                lvl(false, 1.414f, "#f44336"),
                lvl(true, 1.618f, "#2962ff"),
                lvl(false, 1.65f, "#2962ff"),
                lvl(true, 2.618f, "#f44336"),
                lvl(false, 2.65f, "#f44336"),
                lvl(true, 3.618f, "#9c27b0"),
                lvl(false, 3.65f, "#9c27b0"),
                lvl(true, 4.236f, "#e91e63"),
                lvl(false, 4.618f, "#81c784")
            )
        }

        fun deepCopy(s: AutoFibSettings): AutoFibSettings = s.copy(
            levels = s.levels.map { AutoFibLevelState(it.shown, it.ratio, it.colorInt) }
        )
    }
}

class AutoFibRetracementIndicator(
    private val deviation: Float = 3f,
    private val depth: Int = 10,
    private val atrPeriod: Int = 10
) : TradingIndicator {
    override val id = "AUTO_FIB_RETRACEMENT"
    override val name = "Auto Fib Retracement"
    override val color = android.graphics.Color.parseColor("#787b86")

    override fun calculate(candles: List<OHLCData>): List<Float?> = List(candles.size) { null }

    private data class Pivot(val idx: Int, val price: Float)

    fun calculateAutoFib(candles: List<OHLCData>, settings: AutoFibSettings? = null): AutoFibData? {
        if (candles.size < depth + 2) return null
        val cfgDev = settings?.deviation ?: deviation
        val cfgDepth = maxOf(2, settings?.depth ?: depth)
        val atrSeries = AtrIndicator(atrPeriod).calculate(candles)

        var dir = 0
        var hiIdx = 0; var hiPrice = candles[0].high
        var loIdx = 0; var loPrice = candles[0].low
        var extIdx = 0; var extPrice = candles[0].close
        val pivots = ArrayList<Pivot>()

        for (i in 1 until candles.size) {
            val close = candles[i].close
            if (close <= 0f) continue
            val atrVal = atrSeries.getOrNull(i) ?: atrSeries.getOrNull(i - 1) ?: 0f
            if (atrVal <= 0f) continue
            val thrPct = atrVal / close * 100f * cfgDev

            if (dir == 0) {
                if (candles[i].high > hiPrice) { hiPrice = candles[i].high; hiIdx = i }
                if (candles[i].low < loPrice) { loPrice = candles[i].low; loIdx = i }
                val dropFromHigh = (hiPrice - candles[i].low) / hiPrice * 100f
                val riseFromLow = (candles[i].high - loPrice) / loPrice * 100f
                if (i - hiIdx >= cfgDepth && dropFromHigh >= thrPct) {
                    pivots.add(Pivot(hiIdx, hiPrice))
                    dir = -1; extIdx = i; extPrice = candles[i].low
                } else if (i - loIdx >= cfgDepth && riseFromLow >= thrPct) {
                    pivots.add(Pivot(loIdx, loPrice))
                    dir = 1; extIdx = i; extPrice = candles[i].high
                }
            } else if (dir == 1) {
                if (candles[i].high > extPrice) { extPrice = candles[i].high; extIdx = i }
                else {
                    val drop = (extPrice - candles[i].low) / extPrice * 100f
                    if (drop >= thrPct && i - extIdx >= cfgDepth) {
                        pivots.add(Pivot(extIdx, extPrice))
                        dir = -1; extIdx = i; extPrice = candles[i].low
                    }
                }
            } else {
                if (candles[i].low < extPrice) { extPrice = candles[i].low; extIdx = i }
                else {
                    val rise = (candles[i].high - extPrice) / extPrice * 100f
                    if (rise >= thrPct && i - extIdx >= cfgDepth) {
                        pivots.add(Pivot(extIdx, extPrice))
                        dir = 1; extIdx = i; extPrice = candles[i].high
                    }
                }
            }
        }
        pivots.add(Pivot(extIdx, extPrice))
        if (pivots.size < 2) return null

        var prev = pivots[pivots.size - 2]
        var last = pivots.last()
        if (settings?.reverse == true) {
            val t = prev; prev = last; last = t
        }

        // Pine: startPrice = anchor pivot (m=0), other pivot at m=1
        val startPrice = last.price
        val endPrice = prev.price
        val height = (if (startPrice > endPrice) -1f else 1f) * abs(startPrice - endPrice)
        if (height == 0f) return null

        val isUpSwing = startPrice > endPrice
        val levels = (settings?.levels ?: AutoFibSettings.defaultLevels())
            .filter { it.shown }
            .map { AutoFibLevel(ratio = it.ratio, price = startPrice + height * it.ratio, colorInt = it.colorInt) }
        if (levels.isEmpty()) return null

        return AutoFibData(
            leftTime = candles[prev.idx].time,
            leftPrice = prev.price,
            rightTime = candles[last.idx].time,
            rightPrice = last.price,
            isUpSwing = isUpSwing,
            levels = levels
        )
    }
}
