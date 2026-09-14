package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.max
import kotlin.math.min

/**
 * Port of LuxAlgo "Trendline Breakout Navigator [LuxAlgo]" v5.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo.
 *
 * Three independent swing systems (Long/Medium/Short) track one evolving trendline each:
 * a higher-high starts a bullish horizontal line at the prior swing low (and vice versa),
 * then subsequent swings morph it into a sloped trendline. Wick breaks print dots.
 *
 * Mobile notes: multi-timeframe `request.security` is resolved on the chart timeframe
 * only; per-bar `barcolor`/`bgcolor` have no lightweight-charts equivalent and are skipped.
 * Labels are rendered as price-axis tags (the library has no in-chart text).
 */
data class TrendlineNavigatorSettings(
    val res: String = "",
    val showLong: Boolean = true,
    val showMedium: Boolean = true,
    val showShort: Boolean = true,
    val longLen: Int = 60,
    val mediumLen: Int = 30,
    val shortLen: Int = 10,
    val bullColorHex: String = "#089981",
    val bearColorHex: String = "#f23645",
    val wickBullColorHex: String = "#085def",
    val wickBearColorHex: String = "#ff5d00",
    val term: String = "Long",
    val hhll: String = "None",
    val background: Boolean = false,
    val barColor: Boolean = false
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("res", res)
        o.put("showLong", showLong)
        o.put("showMedium", showMedium)
        o.put("showShort", showShort)
        o.put("longLen", longLen)
        o.put("mediumLen", mediumLen)
        o.put("shortLen", shortLen)
        o.put("bullColorHex", bullColorHex)
        o.put("bearColorHex", bearColorHex)
        o.put("wickBullColorHex", wickBullColorHex)
        o.put("wickBearColorHex", wickBearColorHex)
        o.put("term", term)
        o.put("hhll", hhll)
        o.put("background", background)
        o.put("barColor", barColor)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): TrendlineNavigatorSettings {
            if (raw.isNullOrBlank()) return TrendlineNavigatorSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                TrendlineNavigatorSettings(
                    res = o.optString("res", ""),
                    showLong = o.optBoolean("showLong", true),
                    showMedium = o.optBoolean("showMedium", true),
                    showShort = o.optBoolean("showShort", true),
                    longLen = o.optInt("longLen", 60).coerceIn(2, 500),
                    mediumLen = o.optInt("mediumLen", 30).coerceIn(2, 500),
                    shortLen = o.optInt("shortLen", 10).coerceIn(2, 500),
                    bullColorHex = o.optString("bullColorHex", "#089981"),
                    bearColorHex = o.optString("bearColorHex", "#f23645"),
                    wickBullColorHex = o.optString("wickBullColorHex", "#085def"),
                    wickBearColorHex = o.optString("wickBearColorHex", "#ff5d00"),
                    term = normalizeTerm(o.optString("term", "Long")),
                    hhll = normalizeHhll(o.optString("hhll", "None")),
                    background = o.optBoolean("background", false),
                    barColor = o.optBoolean("barColor", false)
                )
            }.getOrDefault(TrendlineNavigatorSettings())
        }

        fun normalizeTerm(raw: String): String = when {
            raw.equals("Medium", ignoreCase = true) -> "Medium"
            raw.equals("Short", ignoreCase = true) -> "Short"
            else -> "Long"
        }

        fun normalizeHhll(raw: String): String = when {
            raw.equals("Only HH/LL", ignoreCase = true) -> "Only HH/LL"
            raw.startsWith("HH/LL", ignoreCase = true) -> "HH/LL & previous H/L"
            else -> "None"
        }

        fun termPos(term: String): Int = when (normalizeTerm(term)) {
            "Medium" -> 2
            "Short" -> 3
            else -> 1
        }
    }
}

/** Render-ready navigator trendline (times are chart epoch seconds). */
data class TnavSegment(
    val startTime: Long,
    val startPrice: Float,
    val endTime: Long,
    val endPrice: Float,
    val pos: Int, // 1 solid/width2, 2 dashed, 3 dotted
    val bull: Boolean // high-system (cBull) vs low-system (cBear)
)

data class TnavDot(
    val time: Long,
    val price: Float,
    val bull: Boolean
)

data class TnavTag(
    val time: Long,
    val price: Float,
    val text: String
)

data class TnavResult(
    val segments: List<TnavSegment>,
    val dots: List<TnavDot>,
    val tags: List<TnavTag>
)

object TrendlineNavigatorIndicator {
    const val MAX_SEGMENTS = 48
    const val MAX_DOTS = 120
    const val MAX_TAGS = 60
    private const val MAX_LINE_BARS = 5000

    private data class ActiveLine(
        var x1: Int,
        var y1: Float,
        var x2: Int,
        var y2: Float,
        var slope: Float, // price per bar
        var cpIdx: Int,
        var cpPrice: Float,
        var active: Boolean,
        var bull: Boolean
    )

    private fun priceAt(line: ActiveLine, k: Int): Float {
        val dx = (line.x2 - line.x1).toDouble()
        if (dx == 0.0) return line.y2
        return (line.y1 + (line.y2 - line.y1) * (k - line.x1) / dx).toFloat()
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

    fun calculate(candles: List<OHLCData>, s: TrendlineNavigatorSettings): TnavResult {
        val n = candles.size
        val empty = TnavResult(emptyList(), emptyList(), emptyList())
        if (n == 0) return empty

        val high = FloatArray(n) { candles[it].high }
        val low = FloatArray(n) { candles[it].low }
        val close = FloatArray(n) { candles[it].close }
        val times = LongArray(n) { candles[it].time }

        data class Sys(val pos: Int, val toggle: Boolean, val left: Int)
        val systems = listOf(
            Sys(1, s.showLong, s.longLen.coerceIn(2, 500)),
            Sys(2, s.showMedium, s.mediumLen.coerceIn(2, 500)),
            Sys(3, s.showShort, s.shortLen.coerceIn(2, 500))
        ).filter { it.toggle }
        if (systems.isEmpty()) return empty

        val ps = TrendlineNavigatorSettings.termPos(s.term)
        val showTags = !s.hhll.equals("None", ignoreCase = true)
        val showPrevDots = s.hhll.equals("HH/LL & previous H/L", ignoreCase = true)

        val segments = mutableListOf<TnavSegment>()
        val dots = mutableListOf<TnavDot>()
        val tags = mutableListOf<TnavTag>()

        fun emit(line: ActiveLine, bull: Boolean, pos: Int) {
            if (line.x2 <= line.x1) return
            if (line.x2 - line.x1 > MAX_LINE_BARS) return
            if (!line.y1.isFinite() || !line.y2.isFinite()) return
            segments.add(
                TnavSegment(
                    startTime = times[line.x1],
                    startPrice = line.y1,
                    endTime = times[line.x2],
                    endPrice = line.y2,
                    pos = pos,
                    bull = bull
                )
            )
        }

        for (sys in systems) {
            val left = sys.left
            val right = 1
            if (n < left + right + 1) continue

            var trend = 0
            var prevPhIdx = -1
            var prevPhPrice = Float.NaN
            var prevPlIdx = -1
            var prevPlPrice = Float.NaN
            var bn: ActiveLine? = null
            var carriedPh: Float? = null
            var carriedPl: Float? = null

            for (bar in 0 until n) {
                // Extend the active navigator line one bar forward (Pine: set_xy2 every bar).
                val cur = bn
                if (cur != null && cur.active) {
                    if (cur.x2 - cur.x1 > MAX_LINE_BARS) {
                        cur.active = false
                    } else {
                        cur.x2 = bar
                        cur.y2 = cur.y2 + cur.slope
                    }
                }

                var cH = false
                var cL = false
                var tH = 0L
                var ph = Float.NaN
                var tL = 0L
                var pl = Float.NaN
                if (bar >= right) {
                    val j = bar - right
                    if (isPivotHigh(high, j, left, right)) {
                        ph = high[j]
                        tH = times[j]
                        if (carriedPh != null && carriedPh != ph) cH = true
                        carriedPh = ph
                    }
                    if (isPivotLow(low, j, left, right)) {
                        pl = low[j]
                        tL = times[j]
                        if (carriedPl != null && carriedPl != pl) cL = true
                        carriedPl = pl
                    }
                }
                val chH = cH && !cL
                val chL = cL && !cH

                if (chH) {
                    // Highest high between the current bar and the pivot bar (most recent wins ties).
                    val pivotIdx = bar - right
                    var v = Float.NEGATIVE_INFINITY
                    var x = pivotIdx
                    var j = bar
                    while (j >= pivotIdx) {
                        if (high[j] > v) {
                            v = high[j]
                            x = j
                        }
                        j--
                    }
                    val c = close[x]
                    if (trend < 1 && prevPhIdx >= 0 && prevPlIdx >= 0 &&
                        v > prevPhPrice && x - prevPhIdx > 5 && bar - prevPlIdx < MAX_LINE_BARS
                    ) {
                        if (sys.pos == ps && showTags) {
                            tags.add(TnavTag(times[x], v, "HH"))
                            if (showPrevDots) tags.add(TnavTag(times[prevPhIdx], prevPhPrice, "●"))
                        }
                        trend = 1
                        val old = bn
                        if (old != null) {
                            emit(old, old.bull, sys.pos)
                        }
                        bn = ActiveLine(prevPlIdx, prevPlPrice, bar, prevPlPrice, 0f, prevPlIdx, prevPlPrice, active = true, bull = true)
                    } else {
                        val line = bn
                        if (line != null && line.active && x != line.cpIdx) {
                            val denom = (x - line.cpIdx).toFloat()
                            if (denom != 0f) {
                                val slope = (v - line.cpPrice) / denom
                                if (v < line.y1 + slope && (v > line.y2 + slope || line.slope == 0f)) {
                                    val priceLin = priceAt(line, x)
                                    if (c < priceLin) {
                                        if (line.slope != 0f) {
                                            dots.add(TnavDot(times[x], priceLin, bull = false))
                                        }
                                        line.x2 = bar
                                        line.y2 = v + slope * (bar - x).toFloat()
                                        if (line.slope == 0f) {
                                            var guard = 0
                                            while (guard++ < 200) {
                                                var best = Float.NEGATIVE_INFINITY
                                                var bestK = -1
                                                var k = 0
                                                while (k <= bar - line.x1) {
                                                    val diff = priceAt(line, bar - k) - close[bar - k]
                                                    if (diff > best) {
                                                        best = diff
                                                        bestK = k
                                                    }
                                                    k++
                                                }
                                                if (best > 0f && bestK >= 0) {
                                                    val nx1 = bar - bestK
                                                    val ny1 = high[nx1]
                                                    line.cpIdx = nx1
                                                    line.cpPrice = ny1
                                                    val d2 = (x - nx1).toFloat()
                                                    if (d2 == 0f) break
                                                    val s2 = (v - ny1) / d2
                                                    line.x1 = nx1
                                                    line.y1 = ny1
                                                    line.x2 = bar
                                                    line.y2 = v + s2 * (bar - x).toFloat()
                                                    line.slope = s2
                                                } else {
                                                    line.x2 = bar
                                                    line.y2 = v + slope * (bar - x).toFloat()
                                                    line.slope = slope
                                                    break
                                                }
                                            }
                                        } else {
                                            line.slope = slope
                                        }
                                    } else {
                                        line.active = false
                                        emit(line, line.bull, sys.pos)
                                    }
                                }
                            }
                        }
                    }
                    prevPhIdx = x
                    prevPhPrice = v
                } else {
                    val line = bn
                    if (trend < 1 && line != null && line.active && close[bar].isFinite() && close[bar] > line.y2) {
                        line.active = false
                        emit(line, line.bull, sys.pos)
                    }
                }

                if (chL) {
                    val pivotIdx = bar - right
                    var v = Float.POSITIVE_INFINITY
                    var x = pivotIdx
                    var j = bar
                    while (j >= pivotIdx) {
                        if (low[j] < v) {
                            v = low[j]
                            x = j
                        }
                        j--
                    }
                    val c = close[x]
                    if (trend > -1 && prevPlIdx >= 0 && prevPhIdx >= 0 &&
                        v < prevPlPrice && x - prevPlIdx > 5 && bar - prevPhIdx < MAX_LINE_BARS
                    ) {
                        if (sys.pos == ps && showTags) {
                            tags.add(TnavTag(times[x], v, "LL"))
                            if (showPrevDots) tags.add(TnavTag(times[prevPlIdx], prevPlPrice, "●"))
                        }
                        trend = -1
                        val old = bn
                        if (old != null) {
                            emit(old, old.bull, sys.pos)
                        }
                        bn = ActiveLine(prevPhIdx, prevPhPrice, bar, prevPhPrice, 0f, prevPhIdx, prevPhPrice, active = true, bull = false)
                    } else {
                        val line = bn
                        if (line != null && line.active && x != line.cpIdx) {
                            val denom = (x - line.cpIdx).toFloat()
                            if (denom != 0f) {
                                val slope = (v - line.cpPrice) / denom
                                if (v > line.y1 + slope && (v < line.y2 + slope || line.slope == 0f)) {
                                    val priceLin = priceAt(line, x)
                                    if (c > priceLin) {
                                        if (line.slope != 0f) {
                                            dots.add(TnavDot(times[x], priceLin, bull = true))
                                        }
                                        line.x2 = bar
                                        line.y2 = v + slope * (bar - x).toFloat()
                                        if (line.slope == 0f) {
                                            var guard = 0
                                            while (guard++ < 200) {
                                                var best = Float.NEGATIVE_INFINITY
                                                var bestK = -1
                                                var k = 0
                                                while (k <= bar - line.x1) {
                                                    val diff = priceAt(line, bar - k) - close[bar - k]
                                                    if (diff > best) {
                                                        best = diff
                                                        bestK = k
                                                    }
                                                    k++
                                                }
                                                if (best > 0f && bestK >= 0) {
                                                    val nx1 = bar - bestK
                                                    val ny1 = low[nx1]
                                                    line.cpIdx = nx1
                                                    line.cpPrice = ny1
                                                    val d2 = (x - nx1).toFloat()
                                                    if (d2 == 0f) break
                                                    val s2 = (v - ny1) / d2
                                                    line.x1 = nx1
                                                    line.y1 = ny1
                                                    line.x2 = bar
                                                    line.y2 = v + s2 * (bar - x).toFloat()
                                                    line.slope = s2
                                                } else {
                                                    line.x2 = bar
                                                    line.y2 = v + slope * (bar - x).toFloat()
                                                    line.slope = slope
                                                    break
                                                }
                                            }
                                        } else {
                                            line.slope = slope
                                        }
                                    } else {
                                        line.active = false
                                        emit(line, line.bull, sys.pos)
                                    }
                                }
                            }
                        }
                    }
                    prevPlIdx = x
                    prevPlPrice = v
                } else {
                    val line = bn
                    if (trend > -1 && line != null && line.active && close[bar].isFinite() && close[bar] < line.y2) {
                        line.active = false
                        emit(line, line.bull, sys.pos)
                    }
                }
            }

            val tail = bn
            if (tail != null) {
                emit(tail, tail.bull, sys.pos)
            }
        }

        return TnavResult(
            segments.takeLast(MAX_SEGMENTS),
            dots.takeLast(MAX_DOTS),
            tags.takeLast(MAX_TAGS)
        )
    }
}