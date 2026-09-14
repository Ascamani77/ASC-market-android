package com.trading.app.indicators

import com.trading.app.models.OHLCData

data class LiquidityPoolsSettings(
    val cNum: Int = 2,
    val gapCount: Int = 5,
    val wait: Int = 10,
    val volTog: Boolean = true,
    val volSize: String = "Small",
    val bullColorHex: String = "#089981",
    val bearColorHex: String = "#f23645",
    val canTog: Boolean = false,
    val bullCanColorHex: String = "#089981",
    val bearCanColorHex: String = "#f23645"
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("cNum", cNum)
        o.put("gapCount", gapCount)
        o.put("wait", wait)
        o.put("volTog", volTog)
        o.put("volSize", volSize)
        o.put("bullColorHex", bullColorHex)
        o.put("bearColorHex", bearColorHex)
        o.put("canTog", canTog)
        o.put("bullCanColorHex", bullCanColorHex)
        o.put("bearCanColorHex", bearCanColorHex)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): LiquidityPoolsSettings {
            if (raw.isNullOrBlank()) return LiquidityPoolsSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                LiquidityPoolsSettings(
                    cNum = o.optInt("cNum", 2).coerceAtLeast(2),
                    gapCount = o.optInt("gapCount", 5).coerceAtLeast(0),
                    wait = o.optInt("wait", 10).coerceAtLeast(1),
                    volTog = o.optBoolean("volTog", true),
                    volSize = normalizeVolSize(o.optString("volSize", "Small")),
                    bullColorHex = o.optString("bullColorHex", "#089981"),
                    bearColorHex = o.optString("bearColorHex", "#f23645"),
                    canTog = o.optBoolean("canTog", false),
                    bullCanColorHex = o.optString("bullCanColorHex", "#089981"),
                    bearCanColorHex = o.optString("bearCanColorHex", "#f23645")
                )
            }.getOrDefault(LiquidityPoolsSettings())
        }

        fun normalizeVolSize(raw: String): String = when {
            raw.equals("Tiny", ignoreCase = true) -> "Tiny"
            raw.equals("Normal", ignoreCase = true) -> "Normal"
            raw.equals("Large", ignoreCase = true) -> "Large"
            raw.equals("Huge", ignoreCase = true) -> "Huge"
            else -> "Small"
        }

        fun formatVolume(v: Double): String = when {
            v >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", v / 1_000_000)
            v >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", v / 1_000)
            v > 0 -> String.format(java.util.Locale.US, "%.0f", v)
            else -> "0"
        }
    }
}

data class LpZone(
    val leftTime: Long,
    val boxRightTime: Long,
    val level: Float,
    val top: Float,
    val bottom: Float,
    val bull: Boolean,
    var vol: Double,
    val lineStartTime: Long,
    var lineEndTime: Long,
    var showLine: Boolean
)

data class LpVolumeTag(
    val time: Long,
    val price: Float,
    val text: String,
    val bull: Boolean
)

data class LpResult(
    val zones: List<LpZone>,
    val tags: List<LpVolumeTag>
)

/**
 * Port of LuxAlgo "Liquidity Pools [LuxAlgo]" v5.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo.
 *
 * Running body-based extremes (hst/lst) collect wick contacts spaced by `gapCount`;
 * zone volumes accumulate the proportion of each candle's volume beyond the level
 * (hi_vol/lo_vol) plus the inside-candle volume of the newest zone. A zone is created
 * once contacts reach cNum with a `wait`-bar confirmation crossover and close beyond
 * the level. Last-zone lines extend to the current close, older zones are hidden when
 * a newer zone supersedes them, and zones are deleted after two consecutive closes
 * breaching the far side. Boxes use `box.new(top, bottom)`; levels are body top/bottom.
 */
object LiquidityPoolsIndicator {

    private class Zn(
        val leftIdx: Int,
        var rightIdx: Int = 0,
        var top: Float,
        var bot: Float,
        val bull: Boolean,
        var vol: Double,
        var state: Int = 0,
        var lineX2: Int = 0,
        var showLine: Boolean = true
    )

    fun calculate(candles: List<OHLCData>, s: LiquidityPoolsSettings): LpResult {
        val n = candles.size
        if (n == 0) return LpResult(emptyList(), emptyList())

        // Running extremes (UDT 'data'): h/t/b/l/bi (high, body top, body bottom, low, barIdx)
        var hstH = candles[0].high
        var hstT = maxOf(candles[0].open, candles[0].close)
        var hstB = minOf(candles[0].open, candles[0].close)
        var hstL = candles[0].low
        var hstBi = 0
        var lstH = candles[0].high
        var lstT = maxOf(candles[0].open, candles[0].close)
        var lstB = minOf(candles[0].open, candles[0].close)
        var lstL = candles[0].low
        var lstBi = 0

        var hCount = 0
        var lCount = 0
        var lastHWick = 0
        var lastLwick = 0
        var hiVol = 0.0
        var loVol = 0.0
        var bsHw = 0
        var bsLw = 0

        // Per-bar snapshots for Pine [1]/[2] history references
        val hWickBar = BooleanArray(n)
        val lWickBar = BooleanArray(n)
        val hTBar = FloatArray(n)
        val lBBar = FloatArray(n)
        val bsHwBar = IntArray(n)
        val bsLwBar = IntArray(n)

        // Running zones (mirrors Pine h_zn / l_zn; pushed refs share the running object)
        var hZn: Zn? = null
        var lZn: Zn? = null

        val zoneList = mutableListOf<Zn>()

        for (i in 0 until n) {
            val high = candles[i].high
            val low = candles[i].low
            val ct = maxOf(candles[i].open, candles[i].close)
            val cb = minOf(candles[i].open, candles[i].close)
            val cc = candles[i].close

            // ---- Adjusting High and Low Check Boundaries ----
            if (high > hstH && (ct > hstH || ct < hstT)) {
                if (hCount > 1) {
                    lstH = high; lstT = ct; lstB = cb; lstL = low; lstBi = i
                    loVol = 0.0; lCount = 0
                }
                hstH = high; hstT = ct; hstB = cb; hstL = low; hstBi = i
                hiVol = 0.0; hCount = 1; lastHWick = i
            }
            if (low < lstL && (cb < lstL || cb > lstB)) {
                if (lCount > 1) {
                    hstH = high; hstT = ct; hstB = cb; hstL = low; hstBi = i
                    hiVol = 0.0; hCount = 0
                }
                lstH = high; lstT = ct; lstB = cb; lstL = low; lstBi = i
                loVol = 0.0; lCount = 1; lastLwick = i
            }

            // ---- Counting Contacts ----
            val hWick = high > hstT && ct <= hstT
            val lWick = low < lstB && cb >= lstB
            hWickBar[i] = hWick
            lWickBar[i] = lWick
            hTBar[i] = hstT
            lBBar[i] = lstB

            if (i >= 2) {
                if (hWickBar[i - 1] && hTBar[i - 1] == hTBar[i - 2] && bsHwBar[i - 1] > s.gapCount) {
                    hCount += 1
                    lastHWick = i - 1
                }
                if (lWickBar[i - 1] && lBBar[i - 1] == lBBar[i - 2] && bsLwBar[i - 1] > s.gapCount) {
                    lCount += 1
                    lastLwick = i - 1
                }
            }

            bsHw = kotlin.math.abs(lastHWick - i)
            bsLw = kotlin.math.abs(lastLwick - i)
            bsHwBar[i] = bsHw
            bsLwBar[i] = bsLw

            // ---- High/Low Tracking for Zone Outer Extremes ----
            if (high > hstH) hstH = high
            if (low < lstL) lstL = low

            // ---- Volume Tracking ----
            val range = high - low
            if (range > 0f) {
                hiVol += (maxOf(high - hstT, 0f) / range) * candles[i].volume
                loVol += (maxOf(lstB - low, 0f) / range) * candles[i].volume
            }

            // ---- Zone Management (Creation & Merging) ----
            val crossoverHw = i > 0 && bsHw > s.wait && bsHwBar[i - 1] <= s.wait
            if (hCount >= s.cNum && crossoverHw && cc < hstT) {
                val cur = hZn
                if (cur != null) {
                    val top = cur.top
                    when {
                        hstBi == cur.leftIdx -> {
                            cur.top = maxOf(hstH, top)
                            cur.bot = minOf(hstT, top)
                        }
                        hstH <= top && hstT >= top -> {
                            cur.rightIdx = i
                            cur.vol += hiVol
                            zoneList.add(cur)
                        }
                        hstH > top && hstT < top -> {
                            cur.top = maxOf(hstT, top)
                            cur.bot = minOf(hstH, top)
                            cur.rightIdx = i
                            cur.vol += hiVol
                            zoneList.add(cur)
                        }
                        else -> {
                            val nz = Zn(hstBi, i, hstH, hstT, false, hiVol)
                            hZn = nz
                            zoneList.add(nz)
                        }
                    }
                } else {
                    val nz = Zn(hstBi, i, hstH, hstT, false, hiVol)
                    hZn = nz
                    zoneList.add(nz)
                }
            }

            val crossoverLw = i > 0 && bsLw > s.wait && bsLwBar[i - 1] <= s.wait
            if (lCount >= s.cNum && crossoverLw && cc > lstB) {
                val cur = lZn
                if (cur != null) {
                    val top = cur.top
                    val bot = cur.bot
                    when {
                        lstBi == cur.leftIdx -> {
                            cur.top = maxOf(lstB, top)
                            cur.bot = minOf(lstL, bot)
                        }
                        lstB <= top && lstL >= bot -> {
                            cur.rightIdx = i
                            cur.vol += loVol
                            zoneList.add(cur)
                        }
                        (lstB > top && lstL < top) || (lstB > bot && lstL < bot) || (lstB > top && lstL < bot) -> {
                            cur.top = maxOf(lstB, top)
                            cur.bot = minOf(lstL, bot)
                            cur.rightIdx = i
                            cur.vol += loVol
                            zoneList.add(cur)
                        }
                        else -> {
                            val nz = Zn(lstBi, i, lstB, lstL, true, loVol)
                            lZn = nz
                            zoneList.add(nz)
                        }
                    }
                } else {
                    val nz = Zn(lstBi, i, lstB, lstL, true, loVol)
                    lZn = nz
                    zoneList.add(nz)
                }
            }

            // ---- Zone Management (Extension & Deletion) ----
            processZones(zoneList, i, cc, high, low, candles[i].volume.toDouble(), bull = true, lZn, hZn)
            processZones(zoneList, i, cc, high, low, candles[i].volume.toDouble(), bull = false, lZn, hZn)
        }

        val times = LongArray(n) { candles[it].time }
        // distinctBy uses reference identity (Zn has no equals override), collapsing the
        // same running-zone object pushed multiple times by the Pine merge branches.
        val outZones = zoneList.distinctBy { it }.filter { it.top > it.bot }.map { z ->
            LpZone(
                leftTime = times[z.leftIdx.coerceIn(0, n - 1)],
                boxRightTime = times[z.rightIdx.coerceIn(0, n - 1)],
                level = if (z.bull) z.top else z.bot,
                top = z.top,
                bottom = z.bot,
                bull = z.bull,
                vol = z.vol,
                lineStartTime = times[z.leftIdx.coerceIn(0, n - 1)],
                lineEndTime = times[z.lineX2.coerceIn(0, n - 1)],
                showLine = z.showLine && z.leftIdx < z.lineX2 && times[z.lineX2.coerceIn(0, n - 1)] > times[z.leftIdx.coerceIn(0, n - 1)]
            )
        }

        val tags = mutableListOf<LpVolumeTag>()
        if (s.volTog) {
            for (z in outZones) {
                if (z.vol > 0) {
                    tags.add(LpVolumeTag(z.lineEndTime, z.level, LiquidityPoolsSettings.formatVolume(z.vol), z.bull))
                }
            }
        }

        return LpResult(outZones, tags)
    }

    /**
     * Pine iterates bull_zones/bear_zones backwards; newest zone (array last) gets line
     * extension + volume; older zones are hidden when the running zone supersedes them;
     * zones delete after two consecutive closes breaching the far side.
     */
    private fun processZones(
        zoneList: MutableList<Zn>, i: Int, cc: Float, high: Float, low: Float,
        volume: Double, bull: Boolean, lZn: Zn?, hZn: Zn?
    ) {
        if (zoneList.isEmpty()) return
        val idxs = zoneList.indices.filter { zoneList[it].bull == bull }
        if (idxs.isEmpty()) return
        for (k in idxs.indices.reversed()) {
            val idx = idxs[k]
            val z = zoneList[idx]
            val isLast = k == idxs.size - 1
            if (isLast) {
                if (bull) {
                    if (cc > z.top) { z.lineX2 = i; z.showLine = true }
                } else {
                    if (cc < z.bot) { z.lineX2 = i; z.showLine = true }
                }
                if ((high < z.top && high > z.bot) || (low < z.top && low > z.bot) || (high >= z.top && low <= z.bot)) {
                    z.vol += civ(high, low, volume, z.top, z.bot)
                }
            } else if (bull) {
                val r = lZn
                if (r != null && r.bot > z.bot) z.showLine = false
            } else {
                val r = hZn
                if (r != null && r.top < z.bot) z.showLine = false
            }
            if (bull) {
                if (cc < z.bot) {
                    if (z.state < 0) { zoneList.removeAt(idx); continue }
                    z.state -= 1
                } else {
                    z.state = 0
                }
            } else {
                if (cc > z.top) {
                    if (z.state < 0) { zoneList.removeAt(idx); continue }
                    z.state -= 1
                } else {
                    z.state = 0
                }
            }
        }
    }

    // get_civ: portion of the candle's range inside the box, times volume
    private fun civ(high: Float, low: Float, volume: Double, top: Float, bot: Float): Double {
        val r = high - low
        if (r <= 0f) return 0.0
        val h2 = if (high > top) top else high
        val l2 = if (low < bot) bot else low
        return ((h2 - l2) / r) * volume
    }
}