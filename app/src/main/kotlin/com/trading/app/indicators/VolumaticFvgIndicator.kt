package com.trading.app.indicators

import com.trading.app.models.OHLCData

data class VolumaticFvgSettings(
    val mitigationSrc: String = "close",
    val bullGaps: Boolean = true,
    val bearGaps: Boolean = true,
    val volumeBars: Boolean = true,
    val bullColorHex: String = "#1AC2D8",
    val bearColorHex: String = "#D8761A"
) {
    fun normalizedMitigation(): String = if (mitigationSrc == "high/low") "high/low" else "close"

    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("mitigationSrc", normalizedMitigation())
        o.put("bullGaps", bullGaps)
        o.put("bearGaps", bearGaps)
        o.put("volumeBars", volumeBars)
        o.put("bullColorHex", bullColorHex)
        o.put("bearColorHex", bearColorHex)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): VolumaticFvgSettings {
            if (raw.isNullOrBlank()) return VolumaticFvgSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                VolumaticFvgSettings(
                    mitigationSrc = if (o.optString("mitigationSrc", "close") == "high/low") "high/low" else "close",
                    bullGaps = o.optBoolean("bullGaps", true),
                    bearGaps = o.optBoolean("bearGaps", true),
                    volumeBars = o.optBoolean("volumeBars", true),
                    bullColorHex = o.optString("bullColorHex", "#1AC2D8"),
                    bearColorHex = o.optString("bearColorHex", "#D8761A")
                )
            }.getOrDefault(VolumaticFvgSettings())
        }
    }
}

data class VfvgItem(
    val leftTime: Long,
    val rightTime: Long,
    val top: Float,
    val bottom: Float,
    val isBull: Boolean,
    val bullPct: Int,
    val bearPct: Int,
    val bullExtSec: Long,
    val bearExtSec: Long,
    val totalVol: Float
)

data class VfvgResult(
    val items: List<VfvgItem>,
    val bullCount: Int,
    val bearCount: Int
)

/**
 * Port of BigBeluga "Volumatic Fair Value Gaps [BigBeluga]" v6.
 *
 * Notable adaptions to the lightweight-charts renderer:
 *  - ta.percentile_nearest_rank(diff, 1000, 100) is the rolling MAX of the last 1000
 *    diff values (100th percentile), tracked with a monotonic deque (O(1) per bar).
 *  - request.security_lower_tf volumes are approximated per chart bar with a
 *    body-length proxy: bull share = upBody / (high - low) of the PREVIOUS bar,
 *    matching the (sumBull[1] / totalVolume[1]) * 100 offset Pine uses.
 *  - Each body's right edge in Pine is kept at bar_index + 25 every bar, so all
 *    live FVGs render with rightTime = last-bar time + 25 * barWidth.
 *  - The dashboard table (barstate.islast) is returned as counts/sums in the result.
 */
object VolumaticFvgIndicator {

    // Pine: `diff` needs bars [k-2..k], skip early bars
    private class Fvg(
        var left: Int,
        var top: Float,
        var bottom: Float,
        var isBull: Boolean,
        var bullPct: Int,
        var bearPct: Int,
        var totalVol: Float
    )

    fun calculate(candles: List<OHLCData>, s: VolumaticFvgSettings): VfvgResult {
        val n = candles.size
        if (n < 4) return VfvgResult(emptyList(), 0, 0)
        val times = LongArray(n) { candles[it].time }
        val highs = FloatArray(n) { candles[it].high }
        val lows = FloatArray(n) { candles[it].low }
        val opens = FloatArray(n) { candles[it].open }
        val closes = FloatArray(n) { candles[it].close }
        val vols = FloatArray(n) { candles[it].volume }

        // rolling max of diff over the last 1000 bars (monotonic deque: (value, index))
        val deq = ArrayDeque<Pair<Float, Int>>(0)
        var maxDiff = 0f

        val fvgs = mutableListOf<Fvg>()

        // proxy for (sumBull/sumBear)/totalVolume*100 of a given bar
        fun percentSplit(k: Int): Pair<Int, Int> {
            val hi = highs[k]; val lo = lows[k]; val c = closes[k]; val o = opens[k]
            val range = if (hi > lo) hi - lo else 0f
            val up = if (c > o) c - o else 0f
            val bull = if (range > 0f) (up / range * 100f).toInt().coerceIn(0, 100) else 50
            return bull to (100 - bull)
        }

        for (k in 0 until n) {
            // -- finegrain-filter rolling max (Pine: ta.percentile_nearest_rank(diff, 1000, 100)) --
            val d = if (k < 2) 0f else
                if (closes[k - 1] > opens[k - 1]) (lows[k] - highs[k - 2]) / lows[k] * 100f
                else (lows[k - 2] - highs[k]) / highs[k] * 100f
            while (deq.isNotEmpty() && deq.last().first <= d) deq.removeLast()
            deq.addLast(d to k)
            while (deq.isNotEmpty() && deq.first().second < k - 999) deq.removeFirst()
            maxDiff = if (deq.isNotEmpty()) deq.first().first else 0f

            val sizeFVG = if (maxDiff != 0f) d / maxDiff * 100f else 0f
            val filterFVG = sizeFVG > 10f

            val isBull = k >= 2 && highs[k - 2] < lows[k] && highs[k - 2] < highs[k - 1] && lows[k - 2] < lows[k] && filterFVG
            val isBear = k >= 2 && lows[k - 2] > highs[k] && lows[k - 2] > lows[k - 1] && highs[k - 2] > highs[k] && filterFVG

            if (k > 100) {
                if (isBull && s.bullGaps) {
                    val prev = percentSplit(k - 1)
                    fvgs.add(Fvg(k - 1, lows[k], highs[k - 2], true, prev.first, prev.second, vols[k - 1]))
                }
                if (isBear && s.bearGaps) {
                    val prev = percentSplit(k - 1)
                    fvgs.add(Fvg(k - 1, lows[k - 2], highs[k], false, prev.first, prev.second, vols[k - 1]))
                }

                // Remove crossed FVGs (mitigated zones)
                var i = fvgs.size - 1
                while (i >= 0) {
                    val f = fvgs[i]
                    val src1 = if (s.normalizedMitigation() == "high/low") lows[k] else closes[k]
                    val src2 = if (s.normalizedMitigation() == "high/low") highs[k] else closes[k]
                    val crossed = if (f.isBull) src1 < f.bottom else src2 > f.top
                    if (crossed) fvgs.removeAt(i)
                    i--
                }

                // Remove overlapped FVGs: a box whose top edge falls strictly inside another's
                // vertical span removes the container (Pine uses for-loops over a mutating list;
                // reverse scan is deterministic and yields the same survivors)
                var oi = fvgs.size - 1
                while (oi >= 0) {
                    val f = fvgs[oi]
                    var removed = false
                    for (j in fvgs.indices) {
                        if (j == oi) continue
                        val l = fvgs[j]
                        if (l.top < f.top && l.top > f.bottom) { removed = true; break }
                    }
                    if (removed) fvgs.removeAt(oi) else oi--
                }

                // Controle size: keep the newest 10 (shift() drops the oldest = head)
                while (fvgs.size > 10) fvgs.removeAt(0)
            }
        }

        val lastK = n - 1
        val barWidth = (times[lastK] - times[lastK - 1]).coerceAtLeast(1L)
        val rightTime = times[lastK] + 25L * barWidth
        // Pine: size = int(right - left) / 200, with right - left always 26 -> 0.13 bars per percent
        val sizeStep = 26.0 / 200.0

        val items = mutableListOf<VfvgItem>()
        var bullCount = 0
        var bearCount = 0
        for (f in fvgs) {
            val leftTime = times[f.left.coerceIn(0, lastK)]
            val bullExtSec = (sizeStep * f.bullPct * barWidth).toLong()
            val bearExtSec = (sizeStep * f.bearPct * barWidth).toLong()
            if (f.isBull) bullCount++ else bearCount++
            items.add(
                VfvgItem(
                    leftTime = leftTime,
                    rightTime = rightTime,
                    top = f.top,
                    bottom = f.bottom,
                    isBull = f.isBull,
                    bullPct = f.bullPct,
                    bearPct = f.bearPct,
                    bullExtSec = bullExtSec,
                    bearExtSec = bearExtSec,
                    totalVol = f.totalVol
                )
            )
        }
        return VfvgResult(items, bullCount, bearCount)
    }
}