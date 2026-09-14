package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Port of LuxAlgo "Liquidity Delta Profiler [LuxAlgo]" v6.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo
 * indicator("Liquidity Delta Profiler [LuxAlgo]", overlay=true, max_boxes_count=500, max_labels_count=500)
 *
 * Detects BSL (buy-side liquidity) zones above price and SSL (sell-side liquidity)
 * zones below price from swing pivots, then tracks the volume delta traded inside
 * each zone's four quadrants, sweeps, zone decay (health %), and reversal signals
 * (ABS / EXH / DIV / REJ).
 */
data class LiquidityDeltaProfilerSettings(
    val pivotLength: Int = 15,
    val maxZones: Int = 10,
    val showSwept: Boolean = true,
    val filterOverlaps: Boolean = true,
    val showDecay: Boolean = true,
    val zoneCapacity: Float = 5f,
    val enableReversals: Boolean = true,
    val showDashboard: Boolean = true,
    val dashboardPosition: String = "Top Right",
    val dashboardSize: String = "Tiny",
    val dashboardWindow: Int = 10,
    val dashboardHold: Int = 3,
    val dashboardHighlight: Boolean = false,
    val bslColorHex: String = "#f23645",
    val sslColorHex: String = "#089981",
    val buyDeltaColorHex: String = "#089981",
    val sellDeltaColorHex: String = "#f23645"
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("pivotLength", pivotLength)
        o.put("maxZones", maxZones)
        o.put("showSwept", showSwept)
        o.put("filterOverlaps", filterOverlaps)
        o.put("showDecay", showDecay)
        o.put("zoneCapacity", zoneCapacity.toDouble())
        o.put("enableReversals", enableReversals)
        o.put("showDashboard", showDashboard)
        o.put("dashboardPosition", dashboardPosition)
        o.put("dashboardSize", dashboardSize)
        o.put("dashboardWindow", dashboardWindow)
        o.put("dashboardHold", dashboardHold)
        o.put("dashboardHighlight", dashboardHighlight)
        o.put("bslColorHex", bslColorHex)
        o.put("sslColorHex", sslColorHex)
        o.put("buyDeltaColorHex", buyDeltaColorHex)
        o.put("sellDeltaColorHex", sellDeltaColorHex)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): LiquidityDeltaProfilerSettings {
            if (raw.isNullOrBlank()) return LiquidityDeltaProfilerSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                LiquidityDeltaProfilerSettings(
                    pivotLength = o.optInt("pivotLength", 15).coerceAtLeast(2),
                    maxZones = o.optInt("maxZones", 10).coerceIn(1, 40),
                    showSwept = o.optBoolean("showSwept", true),
                    filterOverlaps = o.optBoolean("filterOverlaps", true),
                    showDecay = o.optBoolean("showDecay", true),
                    zoneCapacity = o.optDouble("zoneCapacity", 5.0).toFloat().coerceAtLeast(1f),
                    enableReversals = o.optBoolean("enableReversals", true),
                    showDashboard = o.optBoolean("showDashboard", true),
                    dashboardPosition = o.optString("dashboardPosition", "Top Right"),
                    dashboardSize = o.optString("dashboardSize", "Tiny"),
                    dashboardWindow = o.optInt("dashboardWindow", 10).coerceAtLeast(1),
                    dashboardHold = o.optInt("dashboardHold", 3).coerceAtLeast(1),
                    dashboardHighlight = o.optBoolean("dashboardHighlight", false),
                    bslColorHex = o.optString("bslColorHex", "#f23645"),
                    sslColorHex = o.optString("sslColorHex", "#089981"),
                    buyDeltaColorHex = o.optString("buyDeltaColorHex", "#089981"),
                    sellDeltaColorHex = o.optString("sellDeltaColorHex", "#f23645")
                )
            }.getOrDefault(LiquidityDeltaProfilerSettings())
        }
    }
}

data class LiquidityDeltaProfilerZone(
    val isBsl: Boolean,
    val top: Float,
    val bottom: Float,
    val leftIdx: Int,
    val rightIdx: Int,
    val swept: Boolean,
    val deltas: FloatArray,
    val volumeTraded: Float,
    val capacity: Float,
    /** 0-100 remaining zone health; -1 when swept (no label). */
    val healthPct: Int,
    val signaled: Boolean,
    val signalType: String,
    val signalBarIdx: Int,
    val signalPrice: Float
)

data class LiquidityDeltaProfilerResult(
    val bslZones: List<LiquidityDeltaProfilerZone>,
    val sslZones: List<LiquidityDeltaProfilerZone>,
    val absTotal: Int,
    val absWins: Int,
    val exhTotal: Int,
    val exhWins: Int,
    val divTotal: Int,
    val divWins: Int,
    val rejTotal: Int,
    val rejWins: Int
)

object LiquidityDeltaProfilerIndicator {

    private class MutableZone(
        val isBsl: Boolean,
        var top: Float,
        var bottom: Float,
        val left: Int,
        var right: Int,
        var swept: Boolean = false,
        var signaled: Boolean = false,
        val deltas: FloatArray = FloatArray(4),
        var volumeTraded: Float = 0f,
        val capacity: Float,
        var wasHit: Boolean = false,
        var signalType: String = "",
        var signalBarIdx: Int = -1,
        var signalPrice: Float = 0f
    )

    private class Trade(
        val type: String,
        val dir: Int,
        val entry: Float,
        val entryBar: Int,
        var active: Boolean,
        var won: Boolean,
        var consecBars: Int
    )

    fun calculate(candles: List<OHLCData>, s: LiquidityDeltaProfilerSettings): LiquidityDeltaProfilerResult? {
        val n = candles.size
        val len = s.pivotLength.coerceAtLeast(2)
        if (n < len * 2 + 2) return null

        val high = FloatArray(n) { candles[it].high }
        val low = FloatArray(n) { candles[it].low }
        val closeArr = FloatArray(n) { candles[it].close }
        val openArr = FloatArray(n) { candles[it].open }
        val vol = FloatArray(n) { candles[it].volume }

        // ATR(14) - index aligned with the candles (nulls for the warm-up bars)
        val atr = AtrIndicator(14).calculate(candles).map { it ?: 0f }.toFloatArray()

        // Swing pivots, Pine-exact (TV's real tie rule): a pivot sits at the
        // RIGHTMOST occurrence of the max in the window [p-L .. p+L], i.e. equal
        // highs to the LEFT are tolerated (>=) but any reach to the RIGHT rejects
        // it (> strict). ph[p]/pl[p] mark the PIVOT bar itself; the main loop
        // below tests ph[i - len] at confirmation bar i.
        val ph = BooleanArray(n)
        val pl = BooleanArray(n)
        for (p in len until n - len) {
            var maxLeft = Float.NEGATIVE_INFINITY
            for (j in p - len until p) if (high[j] > maxLeft) maxLeft = high[j]
            var maxRight = Float.NEGATIVE_INFINITY
            for (j in p + 1..p + len) if (high[j] > maxRight) maxRight = high[j]
            ph[p] = high[p] >= maxLeft && high[p] > maxRight

            var minLeft = Float.POSITIVE_INFINITY
            for (j in p - len until p) if (low[j] < minLeft) minLeft = low[j]
            var minRight = Float.POSITIVE_INFINITY
            for (j in p + 1..p + len) if (low[j] < minRight) minRight = low[j]
            pl[p] = low[p] <= minLeft && low[p] < minRight
        }

        val bsl = mutableListOf<MutableZone>()
        val ssl = mutableListOf<MutableZone>()
        val trades = mutableListOf<Trade>()
        var absTotal = 0; var absWins = 0
        var exhTotal = 0; var exhWins = 0
        var divTotal = 0; var divWins = 0
        var rejTotal = 0; var rejWins = 0

        var volSum = 0f
        for (i in 0 until n) {
            volSum += vol[i]
            if (i >= len) volSum -= vol[i - len]
            val avgVol = if (i >= len) volSum / len else volSum / max(1, i + 1)
            val cap = (if (avgVol == 0f) 1f else avgVol) * s.zoneCapacity

            // --- Pivot events create new zones (mirror Pine order: before updates) ---
            // Pine tests `not na(ph)` at confirmation bar i; the pivot bar is i - len.
            if (i >= len && ph[i - len]) {
                val pivotIdx = i - len
                var pHigh = high[pivotIdx]
                var pBot = max(closeArr[pivotIdx], openArr[pivotIdx])
                val atrP = atr[pivotIdx]
                if (pHigh - pBot < atrP * 0.1f) pBot = pHigh - atrP * 0.1f

                var skip = false
                if (s.filterOverlaps && bsl.isNotEmpty()) {
                    for (k in bsl.size - 1 downTo 0) {
                        if (k >= bsl.size) break
                        val ex = bsl[k]
                        if (!ex.swept) {
                            val overlaps = max(pBot, ex.bottom) <= min(pHigh, ex.top)
                            if (overlaps) {
                                if (pHigh > ex.top) {
                                    bsl.removeAt(k)
                                } else {
                                    skip = true
                                    break
                                }
                            }
                        }
                    }
                }
                if (!skip && pHigh - pBot > 0f) {
                    bsl.add(0, MutableZone(true, pHigh, pBot, pivotIdx, pivotIdx, capacity = cap))
                    while (bsl.size > s.maxZones) bsl.removeAt(bsl.size - 1)
                }
            }

            if (i >= len && pl[i - len]) {
                val pivotIdx = i - len
                var pLow = low[pivotIdx]
                var pTop = min(closeArr[pivotIdx], openArr[pivotIdx])
                val atrP = atr[pivotIdx]
                if (pTop - pLow < atrP * 0.1f) pTop = pLow + atrP * 0.1f

                var skip = false
                if (s.filterOverlaps && ssl.isNotEmpty()) {
                    for (k in ssl.size - 1 downTo 0) {
                        if (k >= ssl.size) break
                        val ex = ssl[k]
                        if (!ex.swept) {
                            val overlaps = max(pLow, ex.bottom) <= min(pTop, ex.top)
                            if (overlaps) {
                                if (pLow < ex.bottom) {
                                    ssl.removeAt(k)
                                } else {
                                    skip = true
                                    break
                                }
                            }
                        }
                    }
                }
                if (!skip && pTop - pLow > 0f) {
                    ssl.add(0, MutableZone(false, pTop, pLow, pivotIdx, pivotIdx, capacity = cap))
                    while (ssl.size > s.maxZones) ssl.removeAt(ssl.size - 1)
                }
            }

            val totalRange = high[i] - low[i]
            val barDelta = if (totalRange == 0f) 0f else vol[i] * (closeArr[i] - openArr[i]) / totalRange

            // --- Track volume delta through active zones ---
            updateZones(bsl, true, i, high, low, closeArr, openArr, vol, barDelta, totalRange, s, trades)
            updateZones(ssl, false, i, high, low, closeArr, openArr, vol, barDelta, totalRange, s, trades)

            // --- Reversal trade evaluation (dashboard stats) ---
            if (trades.isNotEmpty()) {
                for (k in trades.size - 1 downTo 0) {
                    val t = trades[k]
                    if (!t.active) continue
                    if (i > t.entryBar) {
                        val inProfit = (t.dir == 1 && closeArr[i] > t.entry) || (t.dir == -1 && closeArr[i] < t.entry)
                        if (inProfit) t.consecBars += 1 else t.consecBars = 0
                        if (t.consecBars >= s.dashboardHold) {
                            t.active = false
                            t.won = true
                        } else if (i - t.entryBar >= s.dashboardWindow) {
                            t.active = false
                        }
                        if (!t.active) {
                            when (t.type) {
                                "ABS" -> { absTotal++; if (t.won) absWins++ }
                                "EXH" -> { exhTotal++; if (t.won) exhWins++ }
                                "DIV" -> { divTotal++; if (t.won) divWins++ }
                                "REJ" -> { rejTotal++; if (t.won) rejWins++ }
                            }
                            trades.removeAt(k)
                        }
                    }
                }
            }
        }

        fun toZone(z: MutableZone): LiquidityDeltaProfilerZone {
            val healthPct = if (z.swept) -1 else {
                (100f - z.volumeTraded / z.capacity * 100f).coerceAtLeast(0f).toInt()
            }
            return LiquidityDeltaProfilerZone(
                isBsl = z.isBsl,
                top = z.top,
                bottom = z.bottom,
                leftIdx = z.left,
                rightIdx = z.right,
                swept = z.swept,
                deltas = z.deltas.copyOf(),
                volumeTraded = z.volumeTraded,
                capacity = z.capacity,
                healthPct = healthPct,
                signaled = z.signaled,
                signalType = z.signalType,
                signalBarIdx = z.signalBarIdx,
                signalPrice = z.signalPrice
            )
        }

        return LiquidityDeltaProfilerResult(
            bslZones = bsl.map { toZone(it) },
            sslZones = ssl.map { toZone(it) },
            absTotal = absTotal, absWins = absWins,
            exhTotal = exhTotal, exhWins = exhWins,
            divTotal = divTotal, divWins = divWins,
            rejTotal = rejTotal, rejWins = rejWins
        )
    }

    private fun updateZones(
        zones: MutableList<MutableZone>,
        isBsl: Boolean,
        idx: Int,
        high: FloatArray,
        low: FloatArray,
        closeArr: FloatArray,
        openArr: FloatArray,
        vol: FloatArray,
        barDelta: Float,
        totalRange: Float,
        s: LiquidityDeltaProfilerSettings,
        trades: MutableList<Trade>
    ) {
        if (zones.isEmpty()) return
        for (k in zones.size - 1 downTo 0) {
            val z = zones[k]
            if (z.swept) continue
            z.right = idx
            var hit = false
            val step = (z.top - z.bottom) / 4f
            if (step <= 0f) continue
            for (j in 0..3) {
                val qBot = z.bottom + j * step
                val qTop = z.bottom + (j + 1) * step
                val oTop = min(high[idx], qTop)
                val oBot = max(low[idx], qBot)
                val overlap = if (oTop > oBot) oTop - oBot else 0f
                if (overlap > 0f) {
                    hit = true
                    val overlapRatio = if (totalRange == 0f) 0f else overlap / totalRange
                    z.deltas[j] += barDelta * overlapRatio
                    z.volumeTraded += vol[idx] * overlapRatio
                }
            }
            if (isBsl) {
                if (high[idx] > z.top) z.swept = true
            } else {
                if (low[idx] < z.bottom) z.swept = true
            }
            z.wasHit = hit
            if (hit || z.swept) {
                evaluateReversals(z, isBsl, high[idx], low[idx], closeArr[idx], openArr[idx], barDelta, vol[idx], idx, s, trades)
            }
        }
    }

    private fun evaluateReversals(
        z: MutableZone,
        isBsl: Boolean,
        barH: Float,
        barL: Float,
        barC: Float,
        barO: Float,
        barDelta: Float,
        barVol: Float,
        barIdx: Int,
        s: LiquidityDeltaProfilerSettings,
        trades: MutableList<Trade>
    ) {
        if (!s.enableReversals || z.signaled) return
        var absTotalD = 0f
        for (d in z.deltas) absTotalD += abs(d)
        if (absTotalD <= 0f) return

        val outerD = z.deltas[if (isBsl) 3 else 0]
        val isSweeping = if (isBsl) barH > z.top else barL < z.bottom
        val closesInside = if (isBsl) (barC <= z.top && barC >= z.bottom) else (barC >= z.bottom && barC <= z.top)
        val mid = (z.top + z.bottom) / 2f
        val closesRejecting = if (isBsl) barC < mid else barC > mid

        var signalType = ""
        var significance = 0f
        val ratio = abs(outerD) / (absTotalD + 0.0001f)

        // 1. Absorption at the Extreme (Trap)
        if (isSweeping && ((isBsl && outerD < 0f) || (!isBsl && outerD > 0f))) {
            if (ratio > 0.2f) {
                signalType = "ABS"
                significance = ratio * 2f
            }
        }
        // 2. Exhaustion (Dry Sweep)
        if (signalType.isEmpty() && isSweeping) {
            if (ratio < 0.1f) {
                signalType = "EXH"
                significance = 1f - ratio * 5f
            }
        }
        // 3. Delta Divergence (FOMO)
        if (signalType.isEmpty() && closesInside) {
            if (ratio > 0.6f && ((isBsl && outerD > 0f) || (!isBsl && outerD < 0f))) {
                signalType = "DIV"
                significance = ratio
            }
        }
        // 4. Snapback (Climax + Rejection)
        if (signalType.isEmpty() && isSweeping && closesRejecting && barVol > 0f) {
            val barRatio = abs(barDelta) / (barVol + 0.0001f)
            if (((isBsl && barDelta < 0f) || (!isBsl && barDelta > 0f)) && barRatio > 0.2f) {
                signalType = "REJ"
                significance = barRatio * 2f
            }
        }

        if (signalType.isNotEmpty()) {
            z.signaled = true
            z.signalType = signalType
            z.signalBarIdx = barIdx
            z.signalPrice = if (isBsl) barH else barL
            trades.add(Trade(signalType, if (isBsl) -1 else 1, barC, barIdx, true, false, 0))
        }
    }
}