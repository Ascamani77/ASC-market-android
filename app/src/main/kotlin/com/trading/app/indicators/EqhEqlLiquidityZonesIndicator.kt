package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Port of LuxAlgo "EQH/EQL Liquidity Zones [LuxAlgo]" v6.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo
 * indicator("EQH/EQL Liquidity Zones [LuxAlgo]", overlay=true,
 *   max_lines_count=500, max_labels_count=500, max_boxes_count=500)
 *
 * Detects equal highs (EQH) / equal lows (EQL) from asymmetric swing pivots,
 * draws a liquidity box between the two matching pivots, extends it bar by bar
 * until price sweeps through the level, then either deletes it or fades it to
 * a grey "Swept" state. Cluster labels ("2x EQH (1.2K)") aggregate nearby
 * unswept zones the same way the Pine f_consolidateLabels() does.
 */
data class EqhEqlLiquidityZonesSettings(
    val pivotLeft: Int = 10,
    val pivotRight: Int = 2,
    val thresholdPct: Float = 0.03f,
    val maxZones: Int = 60,
    val bullColorHex: String = "#089981",
    val bearColorHex: String = "#f23645",
    val zoneTransp: Int = 85,
    val showMidline: Boolean = false,
    val midlineColorHex: String = "#787b86",
    val showVolume: Boolean = true,
    val showLabels: Boolean = true,
    val deleteOnSweep: Boolean = false
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("pivotLeft", pivotLeft)
        o.put("pivotRight", pivotRight)
        o.put("thresholdPct", thresholdPct.toDouble())
        o.put("maxZones", maxZones)
        o.put("bullColorHex", bullColorHex)
        o.put("bearColorHex", bearColorHex)
        o.put("zoneTransp", zoneTransp)
        o.put("showMidline", showMidline)
        o.put("midlineColorHex", midlineColorHex)
        o.put("showVolume", showVolume)
        o.put("showLabels", showLabels)
        o.put("deleteOnSweep", deleteOnSweep)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): EqhEqlLiquidityZonesSettings {
            if (raw.isNullOrBlank()) return EqhEqlLiquidityZonesSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                EqhEqlLiquidityZonesSettings(
                    pivotLeft = o.optInt("pivotLeft", 10).coerceAtLeast(1),
                    pivotRight = o.optInt("pivotRight", 2).coerceAtLeast(1),
                    thresholdPct = o.optDouble("thresholdPct", 0.03).toFloat().coerceAtLeast(0f),
                    maxZones = o.optInt("maxZones", 60).coerceIn(1, 100),
                    bullColorHex = o.optString("bullColorHex", "#089981"),
                    bearColorHex = o.optString("bearColorHex", "#f23645"),
                    zoneTransp = o.optInt("zoneTransp", 85).coerceIn(0, 100),
                    showMidline = o.optBoolean("showMidline", false),
                    midlineColorHex = o.optString("midlineColorHex", "#787b86"),
                    showVolume = o.optBoolean("showVolume", true),
                    showLabels = o.optBoolean("showLabels", true),
                    deleteOnSweep = o.optBoolean("deleteOnSweep", false)
                )
            }.getOrDefault(EqhEqlLiquidityZonesSettings())
        }
    }
}

/** Immutable snapshot of one liquidity zone for the chart renderer. */
data class EqhEqlZone(
    val isHigh: Boolean,
    val top: Float,
    val bottom: Float,
    val mid: Float,
    val leftIdx: Int,
    val rightIdx: Int,
    val sweepLevel: Float,
    val totalVol: Float,
    val swept: Boolean,
    /** Consolidated cluster label ("2x EQH (1.2K)") or "" when this zone is not a cluster base. */
    val clusterLabel: String
)

data class EqhEqlLiquidityZonesResult(
    val active: List<EqhEqlZone>,
    val swept: List<EqhEqlZone>
)

object EqhEqlLiquidityZonesIndicator {

    /** Cap on rendered swept zones (Pine keeps every swept box; mobile GPUs do not). */
    const val MAX_SWEPT_RENDER = 24

    private class PivotPoint(val price: Float, val idx: Int, val vol: Float)

    private class MutableZone(
        val isHigh: Boolean,
        val top: Float,
        val bottom: Float,
        val mid: Float,
        val left: Int,
        var right: Int,
        val sweepLevel: Float,
        val totalVol: Float,
        val createdIdx: Int,
        var clusterLabel: String = ""
    )

    /** Pine f_formatVol(): 1.2M / 3.4K / integer. */
    fun formatVol(v: Float): String = when {
        v >= 1_000_000f -> String.format(java.util.Locale.US, "%.1fM", v / 1_000_000f)
        v >= 1_000f -> String.format(java.util.Locale.US, "%.1fK", v / 1_000f)
        else -> Math.round(v).toString()
    }

    fun calculate(candles: List<OHLCData>, s: EqhEqlLiquidityZonesSettings): EqhEqlLiquidityZonesResult {
        val n = candles.size
        val empty = EqhEqlLiquidityZonesResult(emptyList(), emptyList())
        if (n == 0) return empty
        val left = s.pivotLeft.coerceAtLeast(1)
        val right = s.pivotRight.coerceAtLeast(1)
        if (n < left + right + 1) return empty

        val high = FloatArray(n) { candles[it].high }
        val low = FloatArray(n) { candles[it].low }
        val vol = FloatArray(n) { candles[it].volume }

        // Asymmetric pivots, Pine-exact (validated against TV exports + the canonical
        // array.lastindexof(max) implementation of ta.pivothigh): bar p is a pivot when
        // its high is the highest in the whole window [p-L .. p+R] AND the pivot sits at
        // the RIGHTMOST occurrence of that max. So equal highs to the LEFT are tolerated
        // (>=), but any bar to the RIGHT that reaches it rejects the pivot (> strict).
        // On a flat-top / double-top run this places the pivot on the 2nd (rightmost)
        // equal high -- which is what lets a fresh W/M at the current price form zones.
        val isPh = BooleanArray(n)
        val isPl = BooleanArray(n)
        for (p in left until n - right) {
            var maxLeft = Float.NEGATIVE_INFINITY
            for (j in p - left until p) if (high[j] > maxLeft) maxLeft = high[j]
            var maxRight = Float.NEGATIVE_INFINITY
            for (j in p + 1..p + right) if (high[j] > maxRight) maxRight = high[j]
            isPh[p] = high[p] >= maxLeft && high[p] > maxRight

            var minLeft = Float.POSITIVE_INFINITY
            for (j in p - left until p) if (low[j] < minLeft) minLeft = low[j]
            var minRight = Float.POSITIVE_INFINITY
            for (j in p + 1..p + right) if (low[j] < minRight) minRight = low[j]
            isPl[p] = low[p] <= minLeft && low[p] < minRight
        }

        val histHighs = mutableListOf<PivotPoint>() // newest first (unshift), cap 50
        val histLows = mutableListOf<PivotPoint>()
        val active = mutableListOf<MutableZone>() // oldest first (push), cap maxZones
        val sweptOut = mutableListOf<MutableZone>()

        for (i in 0 until n) {
            // --- Pivot confirmations create zones (mirror Pine order: before updates) ---
            val confP = i - right
            if (confP >= 0) {
                if (isPh[confP]) {
                    val pH = high[confP]
                    val curVol = vol[confP]
                    for (prev in histHighs) {
                        val diff = abs(pH - prev.price) / prev.price * 100f
                        if (diff <= s.thresholdPct) {
                            val top = max(pH, prev.price)
                            val bottom = min(pH, prev.price)
                            active.add(
                                MutableZone(
                                    isHigh = true, top = top, bottom = bottom, mid = (top + bottom) / 2f,
                                    left = prev.idx, right = i, sweepLevel = top,
                                    totalVol = prev.vol + curVol, createdIdx = i
                                )
                            )
                            break
                        }
                    }
                    histHighs.add(0, PivotPoint(pH, confP, curVol))
                    if (histHighs.size > 50) histHighs.removeAt(histHighs.size - 1)
                }
                if (isPl[confP]) {
                    val pL = low[confP]
                    val curVol = vol[confP]
                    for (prev in histLows) {
                        val diff = abs(pL - prev.price) / prev.price * 100f
                        if (diff <= s.thresholdPct) {
                            val top = max(pL, prev.price)
                            val bottom = min(pL, prev.price)
                            active.add(
                                MutableZone(
                                    isHigh = false, top = top, bottom = bottom, mid = (top + bottom) / 2f,
                                    left = prev.idx, right = i, sweepLevel = bottom,
                                    totalVol = prev.vol + curVol, createdIdx = i
                                )
                            )
                            break
                        }
                    }
                    histLows.add(0, PivotPoint(pL, confP, curVol))
                    if (histLows.size > 50) histLows.removeAt(histLows.size - 1)
                }
            }

            // --- Extend + sweep check (sweeps only after the confirmation bar) ---
            for (k in active.size - 1 downTo 0) {
                val z = active[k]
                z.right = i
                if (i > z.createdIdx) {
                    val sweptNow = if (z.isHigh) high[i] > z.sweepLevel else low[i] < z.sweepLevel
                    if (sweptNow) {
                        active.removeAt(k)
                        if (!s.deleteOnSweep) sweptOut.add(z)
                    }
                }
            }

            // --- Enforce limit on unswept zones (oldest first, like Pine remove(0)) ---
            while (active.size > s.maxZones.coerceAtLeast(1)) active.removeAt(0)
        }

        // --- Consolidate cluster labels over the final live set (Pine f_consolidateLabels) ---
        val processed = BooleanArray(active.size)
        for (a in active.indices) {
            if (processed[a]) continue
            val base = active[a]
            processed[a] = true
            var clusterVol = base.totalVol
            var clusterCount = 1
            for (b in a + 1 until active.size) {
                if (processed[b]) continue
                val comp = active[b]
                if (base.isHigh == comp.isHigh &&
                    abs(base.sweepLevel - comp.sweepLevel) / base.sweepLevel * 100f <= s.thresholdPct * 3f
                ) {
                    clusterVol += comp.totalVol
                    clusterCount += 1
                    processed[b] = true
                }
            }
            val typeStr = if (base.isHigh) "EQH" else "EQL"
            val countStr = if (clusterCount > 1) "${clusterCount}x " else ""
            val volStr = if (s.showVolume) " (${formatVol(clusterVol)})" else ""
            base.clusterLabel = countStr + typeStr + volStr
        }

        fun snap(z: MutableZone, swept: Boolean): EqhEqlZone {
            val label = if (swept) {
                val volStr = if (s.showVolume) " (${formatVol(z.totalVol)})" else ""
                "Swept " + (if (z.isHigh) "EQH" else "EQL") + volStr
            } else z.clusterLabel
            return EqhEqlZone(
                isHigh = z.isHigh, top = z.top, bottom = z.bottom, mid = z.mid,
                leftIdx = z.left, rightIdx = z.right, sweepLevel = z.sweepLevel,
                totalVol = z.totalVol, swept = swept, clusterLabel = label
            )
        }

        val sweptKept = if (sweptOut.size > MAX_SWEPT_RENDER) sweptOut.takeLast(MAX_SWEPT_RENDER) else sweptOut
        return EqhEqlLiquidityZonesResult(
            active = active.map { snap(it, false) },
            swept = sweptKept.map { snap(it, true) }
        )
    }
}
