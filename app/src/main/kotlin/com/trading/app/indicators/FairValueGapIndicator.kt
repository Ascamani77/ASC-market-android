package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.max
import kotlin.math.min

/**
 * Port of LuxAlgo "Fair Value Gap [LuxAlgo]" v5.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo
 * indicator("Fair Value Gap [LuxAlgo]", overlay=true, max_lines_count=500, max_boxes_count=500)
 */
data class FvgRecord(
    val max: Float,
    val min: Float,
    val isBull: Boolean,
    val time: Long, // epoch seconds of detection bar (new_fvg.t)
    val index: Int // bar_index of detection (n)
)

data class FairValueGapData(
    val fvgs: List<FvgRecord>, // unmitigated only (after mitigation filtering)
    val unmitigatedLines: List<FvgRecord>, // limited by showLast for line drawing
    val mitigatedLines: List<FvgRecord>, // for mitigationLevels dashed lines
    val bullCount: Int,
    val bearCount: Int,
    val bullMitigated: Int,
    val bearMitigated: Int
)

data class FairValueGapSettings(
    val thresholdPer: Float = 0f,
    val auto: Boolean = false,
    val showLast: Int = 0,
    val mitigationLevels: Boolean = false,
    val timeframeMinutes: Int = 0, // 0 = Chart
    val extend: Int = 20,
    val dynamic: Boolean = false,
    val bullColorHex: String = "#089981",
    val bearColorHex: String = "#f23645",
    val showDashboard: Boolean = false,
    val dashboardLocation: String = "Top Right",
    val dashboardSize: String = "Small",
    // Style tab
    val plot1Enabled: Boolean = true,
    val plot2Enabled: Boolean = true,
    val plot3Enabled: Boolean = true,
    val plot4Enabled: Boolean = true,
    val styleBoxes: Boolean = true,
    val styleLines: Boolean = true,
    val styleTables: Boolean = true,
    val stylePrecision: String = "Default",
    val styleLabelsOnPriceScale: Boolean = true,
    val styleValuesInStatusLine: Boolean = true,
    val styleInputsInStatusLine: Boolean = true,
    // Visibility tab
    val visTicks: Boolean = true,
    val visSeconds: Boolean = true,
    val visMinutes: Boolean = true,
    val visHours: Boolean = true,
    val visDays: Boolean = true,
    val visWeeks: Boolean = true,
    val visMonths: Boolean = true,
    val visRanges: Boolean = true,
    val alertsEnabled: Boolean = true
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("thresholdPer", thresholdPer.toDouble())
        o.put("auto", auto)
        o.put("showLast", showLast)
        o.put("mitigationLevels", mitigationLevels)
        o.put("timeframeMinutes", timeframeMinutes)
        o.put("extend", extend)
        o.put("dynamic", dynamic)
        o.put("bullColorHex", bullColorHex)
        o.put("bearColorHex", bearColorHex)
        o.put("showDashboard", showDashboard)
        o.put("dashboardLocation", dashboardLocation)
        o.put("dashboardSize", dashboardSize)
        o.put("plot1Enabled", plot1Enabled)
        o.put("plot2Enabled", plot2Enabled)
        o.put("plot3Enabled", plot3Enabled)
        o.put("plot4Enabled", plot4Enabled)
        o.put("styleBoxes", styleBoxes)
        o.put("styleLines", styleLines)
        o.put("styleTables", styleTables)
        o.put("stylePrecision", stylePrecision)
        o.put("styleLabelsOnPriceScale", styleLabelsOnPriceScale)
        o.put("styleValuesInStatusLine", styleValuesInStatusLine)
        o.put("styleInputsInStatusLine", styleInputsInStatusLine)
        o.put("visTicks", visTicks)
        o.put("visSeconds", visSeconds)
        o.put("visMinutes", visMinutes)
        o.put("visHours", visHours)
        o.put("visDays", visDays)
        o.put("visWeeks", visWeeks)
        o.put("visMonths", visMonths)
        o.put("visRanges", visRanges)
        o.put("alertsEnabled", alertsEnabled)
        return o.toString()
    }
    companion object {
        fun fromJson(raw: String?): FairValueGapSettings {
            if (raw.isNullOrBlank()) return FairValueGapSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                FairValueGapSettings(
                    thresholdPer = o.optDouble("thresholdPer", 0.0).toFloat(),
                    auto = o.optBoolean("auto", false),
                    showLast = o.optInt("showLast", 0),
                    mitigationLevels = o.optBoolean("mitigationLevels", false),
                    timeframeMinutes = o.optInt("timeframeMinutes", 0),
                    extend = o.optInt("extend", 20),
                    dynamic = o.optBoolean("dynamic", false),
                    bullColorHex = o.optString("bullColorHex", "#089981"),
                    bearColorHex = o.optString("bearColorHex", "#f23645"),
                    showDashboard = o.optBoolean("showDashboard", false),
                    dashboardLocation = o.optString("dashboardLocation", "Top Right"),
                    dashboardSize = o.optString("dashboardSize", "Small"),
                    plot1Enabled = o.optBoolean("plot1Enabled", true),
                    plot2Enabled = o.optBoolean("plot2Enabled", true),
                    plot3Enabled = o.optBoolean("plot3Enabled", true),
                    plot4Enabled = o.optBoolean("plot4Enabled", true),
                    styleBoxes = o.optBoolean("styleBoxes", true),
                    styleLines = o.optBoolean("styleLines", true),
                    styleTables = o.optBoolean("styleTables", true),
                    stylePrecision = o.optString("stylePrecision", "Default"),
                    styleLabelsOnPriceScale = o.optBoolean("styleLabelsOnPriceScale", true),
                    styleValuesInStatusLine = o.optBoolean("styleValuesInStatusLine", true),
                    styleInputsInStatusLine = o.optBoolean("styleInputsInStatusLine", true),
                    visTicks = o.optBoolean("visTicks", true),
                    visSeconds = o.optBoolean("visSeconds", true),
                    visMinutes = o.optBoolean("visMinutes", true),
                    visHours = o.optBoolean("visHours", true),
                    visDays = o.optBoolean("visDays", true),
                    visWeeks = o.optBoolean("visWeeks", true),
                    visMonths = o.optBoolean("visMonths", true),
                    visRanges = o.optBoolean("visRanges", true),
                    alertsEnabled = o.optBoolean("alertsEnabled", true)
                )
            }.getOrDefault(FairValueGapSettings())
        }
    }
}

class FairValueGapIndicator(
    private val thresholdPer: Float = 0f,
    private val auto: Boolean = false,
    private val extend: Int = 20
) : TradingIndicator {
    override val id = "FVG_LUXALGO"
    override val name = "Fair Value Gap [LuxAlgo]"
    override val color = android.graphics.Color.parseColor("#089981")

    override fun calculate(candles: List<OHLCData>): List<Float?> = List(candles.size) { null }

    fun calculateFvg(candles: List<OHLCData>): FairValueGapData {
        return calculateFvg(candles, FairValueGapSettings(thresholdPer, auto, extend = extend))
    }

    fun calculateFvg(
        candles: List<OHLCData>,
        settings: FairValueGapSettings,
        chartTfSec: Long = 60L
    ): FairValueGapData {
        if (candles.size < 3) return FairValueGapData(emptyList(), emptyList(), emptyList(), 0, 0, 0, 0)

        // Determine detection candles (grouped if timeframe override)
        val detectCandles: List<OHLCData>
        val detectTfSec: Long
        if (settings.timeframeMinutes != 0) {
            val tfSec = settings.timeframeMinutes * 60L
            detectTfSec = tfSec
            detectCandles = group(candles, tfSec)
            if (detectCandles.size < 3) return FairValueGapData(emptyList(), emptyList(), emptyList(), 0, 0, 0, 0)
        } else {
            detectCandles = candles
            detectTfSec = chartTfSec
        }

        val threshold: Float = if (settings.auto) {
            var cum = 0.0
            for (c in detectCandles) {
                if (c.low != 0f) cum += (c.high - c.low).toDouble() / c.low.toDouble()
            }
            (cum / max(1, detectCandles.size)).toFloat()
        } else {
            settings.thresholdPer / 100f
        }

        // For dynamic tracking (Pine dynamic mode keeps one global bull/bear level)
        var dynBullMax = Float.NaN
        var dynBullMin = Float.NaN
        var dynBearMax = Float.NaN
        var dynBearMin = Float.NaN

        val fvgRecords = mutableListOf<FvgRecord>()
        var bullCount = 0
        var bearCount = 0
        var tSeen = Long.MIN_VALUE

        for (i in 2 until detectCandles.size) {
            val high2 = detectCandles[i - 2].high
            val low2 = detectCandles[i - 2].low
            val close1 = detectCandles[i - 1].close
            val low = detectCandles[i].low
            val high = detectCandles[i].high

            val bullFvg = low > high2 && close1 > high2 && if (high2 != 0f) (low - high2) / high2 > threshold else false
            val bearFvg = high < low2 && close1 < low2 && if (high != 0f) (low2 - high) / high > threshold else false

            val t = detectCandles[i].time
            // Pine dedup: new_fvg.t != t
            if (t == tSeen) continue
            if (bullFvg) {
                val rec = FvgRecord(max = low, min = high2, isBull = true, time = t, index = i)
                fvgRecords.add(rec)
                bullCount++
                tSeen = t
                if (settings.dynamic) {
                    dynBullMax = low
                    dynBullMin = high2
                }
                continue
            } else if (bearFvg) {
                val rec = FvgRecord(max = low2, min = high, isBull = false, time = t, index = i)
                fvgRecords.add(rec)
                bearCount++
                tSeen = t
                if (settings.dynamic) {
                    dynBearMax = low2
                    dynBearMin = high
                }
                continue
            }
            // dynamic clamp when no new FVG: max_bull := max(min(close, max), min)
            if (settings.dynamic) {
                val close = detectCandles[i].close
                if (!dynBullMax.isNaN()) {
                    dynBullMax = max(min(close, dynBullMax), dynBullMin)
                }
                if (!dynBearMin.isNaN()) {
                    dynBearMin = min(max(close, dynBearMin), dynBearMax)
                }
            }
        }

        // Mitigation: check chart candles after detection bar
        val mitigatedList = mutableListOf<FvgRecord>()
        val unmitigated = mutableListOf<FvgRecord>()
        var bullMitigated = 0
        var bearMitigated = 0
        for (rec in fvgRecords) {
            // Find chart index for this detection time
            val chartStartIdx = candles.indexOfFirst { it.time > rec.time }.let { if (it == -1) candles.size else it }
            if (chartStartIdx >= candles.size) {
                unmitigated.add(rec)
                continue
            }
            val crossed = candles.subList(chartStartIdx, candles.size).any { c ->
                if (rec.isBull) c.close < rec.min else c.close > rec.max
            }
            if (crossed) {
                mitigatedList.add(rec)
                if (rec.isBull) bullMitigated++ else bearMitigated++
            } else {
                unmitigated.add(rec)
            }
        }

        // Unmitigated lines limited by showLast (0 = show all unmitigated boxes, lines only if >0)
        val unmitigatedLines: List<FvgRecord> = if (settings.showLast > 0) {
            // newest N
            unmitigated.takeLast(settings.showLast.coerceAtLeast(1))
        } else {
            emptyList()
        }

        // dynamic mode: if enabled, the single dynamic level is the current FVG extent
        // For rendering we keep unmitigated boxes; dynamic flag only suppresses boxes and shows dynamic lines
        // Mitigated dashed lines are drawn only if mitigationLevels enabled

        return FairValueGapData(
            fvgs = unmitigated,
            unmitigatedLines = unmitigatedLines,
            mitigatedLines = if (settings.mitigationLevels) mitigatedList else emptyList(),
            bullCount = bullCount,
            bearCount = bearCount,
            bullMitigated = bullMitigated,
            bearMitigated = bearMitigated
        )
    }

    private fun group(candles: List<OHLCData>, htfSec: Long): List<OHLCData> {
        if (htfSec <= 0) return emptyList()
        val out = ArrayList<OHLCData>()
        var curKey = Long.MIN_VALUE
        var open = 0f; var close = 0f; var high = -Float.MAX_VALUE; var low = Float.MAX_VALUE
        var vol = 0f; var t = 0L
        for (c in candles) {
            val key = c.time - c.time % htfSec
            if (key != curKey) {
                if (curKey != Long.MIN_VALUE) out.add(OHLCData(t, open, high, low, close, vol))
                curKey = key; open = c.open; close = c.close; high = c.high; low = c.low; vol = c.volume; t = key
            } else {
                high = max(high, c.high); low = min(low, c.low)
                close = c.close; vol += c.volume
            }
        }
        if (curKey != Long.MIN_VALUE) out.add(OHLCData(t, open, high, low, close, vol))
        return out
    }
}
