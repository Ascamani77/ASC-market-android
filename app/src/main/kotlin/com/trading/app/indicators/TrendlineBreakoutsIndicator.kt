package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Port of ChartPrime "Trendline Breakouts With Targets [Chartprime]" v5.
 * Licensed MPL 2.0 - ChartPrime.
 *
 * Pivot trendlines are built from confirmed pivots, extended forward by the selected
 * extension length, with parallel ATR-based bands. Breakouts are tested against fresh
 * pivot lines only. One trade/target state is simulated at a time.
 */
data class TrendlineBreakoutsSettings(
    val period: Int = 10,
    val trendType: String = "Wicks",
    val extensions: String = "25",
    val lineCol1Hex: String = "#6d6f6f",
    val showTargets: Boolean = true
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("period", period)
        o.put("trendType", trendType)
        o.put("extensions", extensions)
        o.put("lineCol1Hex", lineCol1Hex)
        o.put("showTargets", showTargets)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): TrendlineBreakoutsSettings {
            if (raw.isNullOrBlank()) return TrendlineBreakoutsSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                TrendlineBreakoutsSettings(
                    period = o.optInt("period", 10).coerceIn(2, 100),
                    trendType = normalizeType(o.optString("trendType", "Wicks")),
                    extensions = normalizeExtensions(o.optString("extensions", "25")),
                    lineCol1Hex = o.optString("lineCol1Hex", "#6d6f6f"),
                    showTargets = o.optBoolean("showTargets", true)
                )
            }.getOrDefault(TrendlineBreakoutsSettings())
        }

        fun normalizeType(raw: String): String =
            if (raw.equals("Body", ignoreCase = true)) "Body" else "Wicks"

        fun normalizeExtensions(raw: String): String {
            val digits = raw.filter(Char::isDigit)
            return when (digits) {
                "50" -> "50"
                "75" -> "75"
                else -> "25"
            }
        }
    }
}

/** Render-ready pivot trendline segment with a downward parallel band. */
data class TbtSegment(
    val startTime: Long,
    val startPrice: Float,
    val endTime: Long,
    val endPrice: Float,
    val band: Float,
    val support: Boolean
)

data class TbtSignal(
    val time: Long,
    val bull: Boolean
)

data class TbtTarget(
    val entryTime: Long,
    val entryPrice: Float,
    val tp: Float,
    val sl: Float,
    val exitTime: Long,
    val won: Boolean,
    val active: Boolean,
    val bull: Boolean
)

data class TbtResult(
    val segments: List<TbtSegment>,
    val signals: List<TbtSignal>,
    val targets: List<TbtTarget>
)

object TrendlineBreakoutsIndicator {
    const val MAX_SEGMENTS = 24
    const val MAX_SIGNALS = 120
    const val MAX_TARGETS = 24

    private data class PivotLine(
        val startTime: Long,
        val startPrice: Float,
        val slope: Float,
        val createdBar: Int,
        val hasSegment: Boolean
    )

    private data class ActiveTarget(
        val entryTime: Long,
        val entryPrice: Float,
        val tp: Float,
        val sl: Float,
        var exitTime: Long,
        val bull: Boolean
    )

    private fun extensionMultiplier(extensions: String): Int = when (extensions) {
        "50" -> 2
        "75" -> 3
        else -> 1
    }

    private fun isPivotHigh(src: FloatArray, j: Int, left: Int, right: Int): Boolean {
        if (j - left < 0 || j + right >= src.size) return false
        val v = src[j]
        if (!v.isFinite()) return false
        for (k in j - left until j) if (src[k] > v) return false
        for (k in j + 1..j + right) if (src[k] >= v) return false
        return true
    }

    private fun isPivotLow(src: FloatArray, j: Int, left: Int, right: Int): Boolean {
        if (j - left < 0 || j + right >= src.size) return false
        val v = src[j]
        if (!v.isFinite()) return false
        for (k in j - left until j) if (src[k] < v) return false
        for (k in j + 1..j + right) if (src[k] <= v) return false
        return true
    }

    fun calculate(candles: List<OHLCData>, s: TrendlineBreakoutsSettings): TbtResult {
        val n = candles.size
        val empty = TbtResult(emptyList(), emptyList(), emptyList())
        if (n == 0) return empty

        val period = s.period.coerceIn(2, 100)
        val right = (period / 2).coerceAtLeast(1)
        val left = period
        if (n < left + right + 1) return empty

        val useBody = s.trendType.equals("Body", ignoreCase = true)
        val highSrc = FloatArray(n) { i ->
            if (useBody) max(candles[i].open, candles[i].close) else candles[i].high
        }
        val lowSrc = FloatArray(n) { i ->
            if (useBody) min(candles[i].open, candles[i].close) else candles[i].low
        }
        val high = FloatArray(n) { candles[it].high }
        val low = FloatArray(n) { candles[it].low }
        val close = FloatArray(n) { candles[it].close }
        val times = LongArray(n) { candles[it].time }

        val atr = AtrIndicator(30).calculate(candles)
        val zband = FloatArray(n) { 0f }
        for (i in 20 until n) {
            val a = atr.getOrNull(i - 20)
            if (a != null && a.isFinite() && a > 0f) {
                val c = close[i - 20]
                if (c.isFinite() && c > 0f) {
                    zband[i] = min(a * 0.3f, c * 0.003f) / 2f
                }
            }
        }

        var lastInterval = 3600L
        fun intervalAt(i: Int): Long {
            if (i > 0) {
                val d = times[i] - times[i - 1]
                if (d > 0) lastInterval = d
            }
            return lastInterval.coerceAtLeast(1L)
        }

        fun lineValue(line: PivotLine, atTime: Long): Float =
            line.startPrice + ((atTime - line.startTime).toFloat() * line.slope)

        fun checkCross(line: PivotLine?, i: Int): Int {
            if (line == null || !line.hasSegment) return 0
            if (i <= 0 || i - line.createdBar >= period) return 0
            val zb = zband[i]
            if (!zb.isFinite() || zb <= 0f) return 0
            val interval = intervalAt(i)
            val current = lineValue(line, times[i])
            val previous = lineValue(line, times[i] - interval)
            if (!current.isFinite() || !previous.isFinite()) return 0
            val c0 = close[i]
            val c1 = close[i - 1]
            if (!c0.isFinite() || !c1.isFinite()) return 0
            if (c1 < previous && c0 > current) return 1
            if (c1 > previous - zb * 0.1f && c0 < current - zb * 0.1f) return -1
            return 0
        }

        val segments = mutableListOf<TbtSegment>()
        val signals = mutableListOf<TbtSignal>()
        val targets = mutableListOf<TbtTarget>()

        var prevHighTime = 0L
        var prevHighPrice = Float.NaN
        var lineHigh: PivotLine? = null
        var prevLowTime = 0L
        var prevLowPrice = Float.NaN
        var lineLow: PivotLine? = null

        var tradeOn = false
        var longTrade = false
        var shortTrade = false
        var active: ActiveTarget? = null

        val extBars = extensionMultiplier(s.extensions) * 25L

        for (i in 0 until n) {
            val enteringTradeOn = tradeOn
            val zb = zband[i]
            val interval = intervalAt(i)

            // Confirmed pivots: pivot bar is `right` bars back, like ta.pivothigh/low.
            if (i >= right) {
                val j = i - right
                if (isPivotHigh(highSrc, j, left, right)) {
                    val pivotTime = times[j]
                    val pivotPrice = highSrc[j]
                    if (prevHighTime > 0L && pivotTime != prevHighTime) {
                        val denom = (pivotTime - prevHighTime).toFloat()
                        val slope = if (denom != 0f) (pivotPrice - prevHighPrice) / denom else 0f
                        lineHigh = PivotLine(prevHighTime, prevHighPrice, slope, i, hasSegment = true)
                        if (!enteringTradeOn && zb > 0f && slope < 0f) {
                            val extSec = extBars * interval
                            segments.add(
                                TbtSegment(
                                    startTime = prevHighTime,
                                    startPrice = prevHighPrice,
                                    endTime = pivotTime + extSec,
                                    endPrice = pivotPrice + extSec.toFloat() * slope,
                                    band = zb,
                                    support = false
                                )
                            )
                        }
                    } else if (prevHighTime <= 0L) {
                        lineHigh = PivotLine(pivotTime, pivotPrice, 0f, i, hasSegment = false)
                    }
                    prevHighTime = pivotTime
                    prevHighPrice = pivotPrice
                }
                if (isPivotLow(lowSrc, j, left, right)) {
                    val pivotTime = times[j]
                    val pivotPrice = lowSrc[j]
                    if (prevLowTime > 0L && pivotTime != prevLowTime) {
                        val denom = (pivotTime - prevLowTime).toFloat()
                        val slope = if (denom != 0f) (pivotPrice - prevLowPrice) / denom else 0f
                        lineLow = PivotLine(prevLowTime, prevLowPrice, slope, i, hasSegment = true)
                        if (!enteringTradeOn && zb > 0f && slope >= 0f) {
                            val extSec = extBars * interval
                            segments.add(
                                TbtSegment(
                                    startTime = prevLowTime,
                                    startPrice = prevLowPrice,
                                    endTime = pivotTime + extSec,
                                    endPrice = pivotPrice + extSec.toFloat() * slope,
                                    band = zb,
                                    support = true
                                )
                            )
                        }
                    } else if (prevLowTime <= 0L) {
                        lineLow = PivotLine(pivotTime, pivotPrice, 0f, i, hasSegment = false)
                    }
                    prevLowTime = pivotTime
                    prevLowPrice = pivotPrice
                }
            }

            val longSignal = if (!enteringTradeOn && i > 0) {
                val line = lineHigh
                line != null && line.slope <= 0f && checkCross(line, i) == 1
            } else false
            val shortSignal = if (!enteringTradeOn && i > 0) {
                val line = lineLow
                line != null && line.slope >= 0f && checkCross(line, i) == -1
            } else false

            if (longSignal || shortSignal) signals.add(TbtSignal(times[i], longSignal))

            if ((longSignal || shortSignal) && !enteringTradeOn && zb > 0f) {
                val bull = longSignal
                val entry = if (bull) high[i] else low[i]
                val tp = if (bull) high[i] + zb * 20f else low[i] - zb * 20f
                val sl = if (bull) low[i] - zb * 20f else high[i] + zb * 20f
                if (entry.isFinite() && tp.isFinite() && sl.isFinite()) {
                    longTrade = bull
                    shortTrade = !bull
                    tradeOn = true
                    active = ActiveTarget(times[i], entry, tp, sl, times[i], bull)
                }
            }

            val cur = active
            if (tradeOn && cur != null) {
                cur.exitTime = times[i]
                if (longTrade) {
                    if (high[i] >= cur.tp) {
                        targets.add(TbtTarget(cur.entryTime, cur.entryPrice, cur.tp, cur.sl, times[i], won = true, active = false, bull = true))
                        tradeOn = false
                        longTrade = false
                        shortTrade = false
                        active = null
                    } else if (close[i] <= cur.sl) {
                        targets.add(TbtTarget(cur.entryTime, cur.entryPrice, cur.tp, cur.sl, times[i], won = false, active = false, bull = true))
                        tradeOn = false
                        longTrade = false
                        shortTrade = false
                        active = null
                    }
                } else if (shortTrade) {
                    if (low[i] <= cur.tp) {
                        targets.add(TbtTarget(cur.entryTime, cur.entryPrice, cur.tp, cur.sl, times[i], won = true, active = false, bull = false))
                        tradeOn = false
                        longTrade = false
                        shortTrade = false
                        active = null
                    } else if (close[i] >= cur.sl) {
                        targets.add(TbtTarget(cur.entryTime, cur.entryPrice, cur.tp, cur.sl, times[i], won = false, active = false, bull = false))
                        tradeOn = false
                        longTrade = false
                        shortTrade = false
                        active = null
                    }
                }
            }
        }

        val cur = active
        if (tradeOn && cur != null) {
            targets.add(TbtTarget(cur.entryTime, cur.entryPrice, cur.tp, cur.sl, times[n - 1], won = false, active = true, bull = cur.bull))
        }

        return TbtResult(
            segments.takeLast(MAX_SEGMENTS),
            signals.takeLast(MAX_SIGNALS),
            targets.takeLast(MAX_TARGETS)
        )
    }
}