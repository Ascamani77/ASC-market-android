package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.max
import kotlin.math.min

/**
 * Port of twingall "OTE visible chart" (23 Jan'23) v5.
 * indicator("OTE visible chart", overlay=true)
 * Based on PineCoders/VisibleChart/4 BasicVisibleChart lib (visible range high/low).
 * Fib box OTE 61.8% - 78.6%, extensions, anchor wick/body.
 */
data class OteVisibleChartData(
    val chartHigh: Float,
    val chartLow: Float,
    val highTime: Long,
    val lowTime: Long,
    val leftTime: Long,
    val rightTime: Long,
    val isBull: Boolean,
    val levels: List<OteLevel>,
    val boxTop: Float, // 78.6
    val boxBottom: Float, // 61.8
    val extensions: List<OteLevel>,
    val useBodies: Boolean
)

data class OteLevel(
    val label: String,
    val fibPercent: Float,
    val price: Float,
    val color: Int,
    val visible: Boolean
)

class OteVisibleChartIndicator(
    private val useBodies: Boolean = false,
    private val showFibBox: Boolean = true,
    private val showHighLowLines: Boolean = true,
    private val showMidline: Boolean = true,
    private val show61eight: Boolean = false,
    private val show78six: Boolean = false,
    private val show88six: Boolean = false,
    private val showFibExt1: Boolean = true,
    private val fibExt1: Float = 1.618f,
    private val showFibExt2: Boolean = true,
    private val fibExt2: Float = 2.0f,
    private val showFibExt3: Boolean = false,
    private val fibExt3: Float = 1.5f,
    private val showFibExt4: Boolean = false,
    private val fibExt4: Float = 2.5f,
    private val showFibExt5: Boolean = false,
    private val fibExt5: Float = 3.0f,
    private val showFibExt6: Boolean = false,
    private val fibExt6: Float = 3.5f,
    private val flipInvertExts: Boolean = false
) : TradingIndicator {
    override val id = "OTE_VISIBLE_CHART_TWINGALL"
    override val name = "OTE visible chart [twingall]"
    override val color = android.graphics.Color.parseColor("#D94CD9")

    override fun calculate(candles: List<OHLCData>): List<Float?> = List(candles.size) { null }

    fun calculateOte(candles: List<OHLCData>): OteVisibleChartData? {
        if (candles.size < 3) return null
        // VisibleChart approximation: use all candles (or last 300 visible). Pine uses visible range only.
        // useBodies: body high = max(open,close), body low = min(open,close)
        var chartHigh = Float.MIN_VALUE
        var chartLow = Float.MAX_VALUE
        var highTime: Long = candles.last().time
        var lowTime: Long = candles.last().time
        for (c in candles) {
            val h = if (useBodies) max(c.open, c.close) else c.high
            val l = if (useBodies) min(c.open, c.close) else c.low
            if (h > chartHigh) { chartHigh = h; highTime = c.time }
            if (l < chartLow) { chartLow = l; lowTime = c.time }
        }
        if (chartHigh == Float.MIN_VALUE || chartLow == Float.MAX_VALUE) return null
        val leftTime = min(highTime, lowTime)
        val rightTime = max(highTime, lowTime)
        val isBull = lowTime < highTime
        val range = chartHigh - chartLow
        if (range == 0f) return null

        fun fibPrice(levelPercent: Float): Float {
            val fibRatio = 1f - (levelPercent / 100f)
            return if (isBull) chartLow + (range * fibRatio) else chartHigh - (range * fibRatio)
        }
        fun extPrice(extLevel: Float): Float {
            // extLevel passed as 1.618*100 etc. Pine fibExt called with (fibExt*100) then fibRatio = ext/100
            // For flipInvert logic: handled outside, here just raw extension price
            val fibRatio = extLevel / 100f
            return if (isBull) chartLow - (range * fibRatio) else chartHigh + (range * fibRatio)
        }

        // Core retracements
        val levels = mutableListOf<OteLevel>()
        if (showHighLowLines) {
            levels.add(OteLevel("100", 100f, fibPrice(100f), android.graphics.Color.parseColor("#D94CD9"), true))
            levels.add(OteLevel("0", 0f, fibPrice(0f), android.graphics.Color.parseColor("#D94CD9"), true))
        }
        if (showMidline) {
            levels.add(OteLevel("50", 50f, fibPrice(50f), android.graphics.Color.GRAY, true))
        }
        if (show61eight) levels.add(OteLevel("61.8", 61.8f, fibPrice(61.8f), android.graphics.Color.parseColor("#00C853"), true))
        if (show78six) levels.add(OteLevel("78.6", 78.6f, fibPrice(78.6f), android.graphics.Color.parseColor("#00C853"), true))
        if (show88six) levels.add(OteLevel("88.6", 88.6f, fibPrice(88.6f), android.graphics.Color.parseColor("#00C853"), true))
        // Always include 61.8/78.6 for box even if not shown as lines (box uses them)
        if (!show61eight && !show78six && showFibBox) {
            // ensure box levels exist even if lines hidden
        }

        val boxTop = fibPrice(78.6f)
        val boxBottom = fibPrice(61.8f)

        // Extensions
        val exts = mutableListOf<OteLevel>()
        fun addExt(show: Boolean, ext: Float, label: String) {
            if (!show) return
            val raw = if (flipInvertExts) (ext * 100f) - 100f else -(ext * 100f)
            // mirror Pine: if flipInvert then (ext*100)-100 else -(ext*100)
            val price = extPrice(raw)
            exts.add(OteLevel(label, ext, price, android.graphics.Color.RED, true))
        }
        addExt(showFibExt1, fibExt1, "Ext#1 ${fibExt1}")
        addExt(showFibExt2, fibExt2, "Ext#2 ${fibExt2}")
        addExt(showFibExt3, fibExt3, "Ext#3 ${fibExt3}")
        addExt(showFibExt4, fibExt4, "Ext#4 ${fibExt4}")
        addExt(showFibExt5, fibExt5, "Ext#5 ${fibExt5}")
        addExt(showFibExt6, fibExt6, "Ext#6 ${fibExt6}")

        return OteVisibleChartData(
            chartHigh = chartHigh,
            chartLow = chartLow,
            highTime = highTime,
            lowTime = lowTime,
            leftTime = leftTime,
            rightTime = rightTime,
            isBull = isBull,
            levels = levels,
            boxTop = boxTop,
            boxBottom = boxBottom,
            extensions = exts,
            useBodies = useBodies
        )
    }
}
