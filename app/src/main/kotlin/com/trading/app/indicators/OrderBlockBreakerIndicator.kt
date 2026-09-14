package com.trading.app.indicators

import com.trading.app.models.OHLCData

data class OrderBlockBreakerSettings(
    val detection: String = "Intermediate Term",
    val showBull: Int = 3,
    val showBear: Int = 3,
    val useBody: Boolean = true,
    val showLabels: Boolean = true,
    val bullCssHex: String = "#2157f3",
    val bullBreakCssHex: String = "#ff1100",
    val bearCssHex: String = "#ff5d00",
    val bearBreakCssHex: String = "#0cb51a"
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("detection", detection)
        o.put("showBull", showBull)
        o.put("showBear", showBear)
        o.put("useBody", useBody)
        o.put("showLabels", showLabels)
        o.put("bullCssHex", bullCssHex)
        o.put("bullBreakCssHex", bullBreakCssHex)
        o.put("bearCssHex", bearCssHex)
        o.put("bearBreakCssHex", bearBreakCssHex)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): OrderBlockBreakerSettings {
            if (raw.isNullOrBlank()) return OrderBlockBreakerSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                OrderBlockBreakerSettings(
                    detection = normalizeDetection(o.optString("detection", "Intermediate Term")),
                    showBull = o.optInt("showBull", 3).coerceAtLeast(0),
                    showBear = o.optInt("showBear", 3).coerceAtLeast(0),
                    useBody = o.optBoolean("useBody", true),
                    showLabels = o.optBoolean("showLabels", true),
                    bullCssHex = o.optString("bullCssHex", "#2157f3"),
                    bullBreakCssHex = o.optString("bullBreakCssHex", "#ff1100"),
                    bearCssHex = o.optString("bearCssHex", "#ff5d00"),
                    bearBreakCssHex = o.optString("bearBreakCssHex", "#0cb51a")
                )
            }.getOrDefault(OrderBlockBreakerSettings())
        }

        fun normalizeDetection(raw: String): String = when {
            raw.equals("Short Term", ignoreCase = true) -> "Short Term"
            raw.equals("Long Term", ignoreCase = true) -> "Long Term"
            else -> "Intermediate Term"
        }
    }
}

data class ObDisplay(
    val locTime: Long,
    val rightTime: Long,
    val breaker: Boolean,
    val top: Float,
    val btm: Float,
    val bull: Boolean
)

data class ObLabel(
    val time: Long,
    val price: Float,
    val down: Boolean,
    val hex: String
)

data class ObBbResult(
    val bull: List<ObDisplay>,
    val bear: List<ObDisplay>,
    val labels: List<ObLabel>
)

/**
 * Port of LuxAlgo "Pure Price Action Order & Breaker Blocks [LuxAlgo]" v5.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo.
 *
 * Vector-based swing detection (depth per detection term) drives the last registered
 * bullish/bearish order blocks; OBs flip to "breaker" once price dips/invades them,
 * and are removed when reclaimed. Confirmation occurs when the newest swing lies inside
 * a broken OB, emitting polarity-change labels. Boxes/lines redraw only on the last bar.
 */
object OrderBlockBreakerIndicator {

    private class Swing(val y: Float, val x: Int) {
        var crossed: Boolean = false
    }

    private class Ob(val top: Float, val btm: Float, val loc: Long) {
        var breaker: Boolean = false
        var breakLoc: Long = 0L
    }

    fun calculate(candles: List<OHLCData>, s: OrderBlockBreakerSettings): ObBbResult {
        val n = candles.size
        if (n == 0) return ObBbResult(emptyList(), emptyList(), emptyList())

        val times = LongArray(n) { candles[it].time }
        val maxArr = FloatArray(n) { if (s.useBody) maxOf(candles[it].open, candles[it].close) else candles[it].high }
        val minArr = FloatArray(n) { if (s.useBody) minOf(candles[it].open, candles[it].close) else candles[it].low }
        val closeArr = FloatArray(n) { candles[it].close }
        val openArr = FloatArray(n) { candles[it].open }

        val depth = when (s.detection) {
            "Short Term" -> 1
            "Long Term" -> 3
            else -> 2
        }

        val fh = MutableList(depth) { mutableListOf<Swing>() }
        val fl = MutableList(depth) { mutableListOf<Swing>() }

        val bullishOb = mutableListOf<Ob>()
        val bearishOb = mutableListOf<Ob>()
        val labels = mutableListOf<ObLabel>()
        var prevBullConf = 0
        var prevBearConf = 0
        // Pine `var swingLevel` inside detect() persists across bars: swing candidates
        // stay live until a NEW confirmed swing replaces them (kept only on a deep fire).
        var persistedTop: Swing? = null
        var persistedBtm: Swing? = null

        for (k in 0 until n) {
            fh[0].add(0, Swing(candles[k].high, k))
            if (fh[0].size > 3) fh[0].removeAt(fh[0].size - 1)
            fl[0].add(0, Swing(candles[k].low, k))
            if (fl[0].size > 3) fl[0].removeAt(fl[0].size - 1)

            detect(fh, bull = true, depth)?.let { persistedTop = it }
            detect(fl, bull = false, depth)?.let { persistedBtm = it }
            val top = persistedTop
            val btm = persistedBtm
            val topX = top?.x
            val topY = top?.y
            val btmX = btm?.x
            val btmY = btm?.y

            var bullConf = 0
            var bearConf = 0

            if (topY != null && closeArr[k] > topY && !top!!.crossed) {
                top.crossed = true
                var minima = maxArr.getOrElse(k - 1) { maxArr[0] }
                var maxima = minArr.getOrElse(k - 1) { minArr[0] }
                var loc = times.getOrElse(k - 1) { times[0] }
                val limit = kotlin.math.min(k - top!!.x - 1, k - 1)
                for (i in 1..limit) {
                    val idx = k - i
                    val mi = minArr[idx]
                    minima = minOf(mi, minima)
                    if (minima == mi) { maxima = maxArr[idx]; loc = times[idx] }
                }
                bullishOb.add(0, Ob(maxima, minima, loc))
            }

            if (bullishOb.isNotEmpty()) {
                for (i in bullishOb.size - 1 downTo 0) {
                    val e = bullishOb[i]
                    if (!e.breaker) {
                        if (minOf(closeArr[k], openArr[k]) < e.btm) {
                            e.breaker = true
                            e.breakLoc = times[k]
                        }
                    } else {
                        if (closeArr[k] > e.top) bullishOb.removeAt(i)
                        else if (i < s.showBull && topY != null && topY < e.top && topY > e.btm) bullConf = 1
                    }
                }
            }
            if (bullConf > prevBullConf && s.showLabels && topX != null && topY != null) {
                labels.add(ObLabel(times[topX], topY, down = true, s.bearCssHex))
            }
            prevBullConf = bullConf

            if (btmY != null && closeArr[k] < btmY && !btm!!.crossed) {
                btm.crossed = true
                var minima = minArr.getOrElse(k - 1) { minArr[0] }
                var maxima = maxArr.getOrElse(k - 1) { maxArr[0] }
                var loc = times.getOrElse(k - 1) { times[0] }
                val limit = kotlin.math.min(k - btm!!.x - 1, k - 1)
                for (i in 1..limit) {
                    val idx = k - i
                    val ma = maxArr[idx]
                    maxima = maxOf(ma, maxima)
                    if (maxima == ma) { minima = minArr[idx]; loc = times[idx] }
                }
                bearishOb.add(0, Ob(maxima, minima, loc))
            }

            if (bearishOb.isNotEmpty()) {
                for (i in bearishOb.size - 1 downTo 0) {
                    val e = bearishOb[i]
                    if (!e.breaker) {
                        if (maxOf(closeArr[k], openArr[k]) > e.top) {
                            e.breaker = true
                            e.breakLoc = times[k]
                        }
                    } else {
                        if (closeArr[k] < e.btm) bearishOb.removeAt(i)
                        else if (i < s.showBear && btmY != null && btmY > e.btm && btmY < e.top) bearConf = 1
                    }
                }
            }
            if (bearConf > prevBearConf && s.showLabels && btmX != null && btmY != null) {
                labels.add(ObLabel(times[btmX], btmY, down = false, s.bullCssHex))
            }
            prevBearConf = bearConf
        }

        val lastTime = times[n - 1]
        fun toDisplay(ob: Ob, bull: Boolean): ObDisplay = ObDisplay(
            locTime = ob.loc,
            rightTime = if (ob.breaker) ob.breakLoc else lastTime,
            breaker = ob.breaker,
            top = ob.top,
            btm = ob.btm,
            bull = bull
        )
        val bullOut = bullishOb.take(s.showBull.coerceAtLeast(0)).map { toDisplay(it, true) }
        val bearOut = bearishOb.take(s.showBear.coerceAtLeast(0)).map { toDisplay(it, false) }
        return ObBbResult(bullOut, bearOut, labels)
    }

    private fun detect(id: MutableList<MutableList<Swing>>, bull: Boolean, depth: Int): Swing? {
        var swingLevel: Swing? = null
        for (i in 0 until depth) {
            val v = id[i]
            if (v.size == 3) {
                val pivot = if (bull) maxOf(v[0].y, v[1].y, v[2].y) else minOf(v[0].y, v[1].y, v[2].y)
                if (pivot == v[1].y) {
                    if (i < depth - 1) {
                        id[i + 1].add(0, v[1])
                        if (id[i + 1].size > 3) id[i + 1].removeAt(id[i + 1].size - 1)
                    } else {
                        swingLevel = Swing(v[1].y, v[1].x)
                    }
                    v.removeAt(v.size - 1)
                    v.removeAt(v.size - 1)
                }
            }
        }
        return swingLevel
    }
}