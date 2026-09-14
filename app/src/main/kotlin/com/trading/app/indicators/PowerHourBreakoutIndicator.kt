package com.trading.app.indicators

import com.trading.app.models.OHLCData
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

/**
 * Port of LuxAlgo "Power Hour Breakout [LuxAlgo]" v6.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo
 * indicator('Power Hour Breakout [LuxAlgo]', overlay = true,
 *   max_lines_count = 500, max_boxes_count = 500, max_labels_count = 500)
 *
 * Marks the configured New-York "Power Hour" session on the chart: a silver box
 * from session start to session end bounded by the session high/low, with top /
 * bottom level lines, optional percentage extensions (top + mult% of range,
 * bottom - mult% of range) shaded between level and extension, optional fibonacci
 * levels of the session range retraced from top (or from bottom when reversed),
 * and triangle markers on bars that close outside the session's high/low range.
 */
data class FiboLevelInput(
    val display: Boolean,
    val level: Float,
    val colorHex: String,
    val style: String,
    val price: Float = 0f // price-resolved level for rendering (0 = not yet resolved)
)

data class PowerHourBreakoutSettings(
    val powerHoursLength: Int = 10,
    val displayAll: Boolean = true,
    val session: String = "1500-1600",
    val showBreakouts: Boolean = true,
    val bullBreakColorHex: String = "#089981",
    val bearBreakColorHex: String = "#f23645",
    val topExtension: Boolean = true,
    val topMultiplierPct: Int = 50,
    val bottomExtension: Boolean = true,
    val bottomMultiplierPct: Int = 50,
    val showFibonacci: Boolean = true,
    val fiboReverse: Boolean = false,
    val fiboLevels: List<FiboLevelInput> = defaultFiboLevels(),
    val fibosLabels: Boolean = false,
    val fibosLabelSize: Int = 10,
    val topColorHex: String = "#089981",
    val bottomColorHex: String = "#f23645",
    val transparency: Int = 80,
    val sessionStartMarkers: Boolean = true,
    val backgroundColorHex: String = "#c0c0c0"
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("powerHoursLength", powerHoursLength)
        o.put("displayAll", displayAll)
        o.put("session", session)
        o.put("showBreakouts", showBreakouts)
        o.put("bullBreakColorHex", bullBreakColorHex)
        o.put("bearBreakColorHex", bearBreakColorHex)
        o.put("topExtension", topExtension)
        o.put("topMultiplierPct", topMultiplierPct)
        o.put("bottomExtension", bottomExtension)
        o.put("bottomMultiplierPct", bottomMultiplierPct)
        o.put("showFibonacci", showFibonacci)
        o.put("fiboReverse", fiboReverse)
        val arr = org.json.JSONArray()
        for (f in fiboLevels) {
            arr.put(org.json.JSONObject().put("display", f.display).put("level", f.level.toDouble()).put("colorHex", f.colorHex).put("style", f.style))
        }
        o.put("fiboLevels", arr)
        o.put("fibosLabels", fibosLabels)
        o.put("fibosLabelSize", fibosLabelSize)
        o.put("topColorHex", topColorHex)
        o.put("bottomColorHex", bottomColorHex)
        o.put("transparency", transparency)
        o.put("sessionStartMarkers", sessionStartMarkers)
        o.put("backgroundColorHex", backgroundColorHex)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): PowerHourBreakoutSettings {
            if (raw.isNullOrBlank()) return PowerHourBreakoutSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                val fibos = mutableListOf<FiboLevelInput>()
                val arr = o.optJSONArray("fiboLevels")
                if (arr != null && arr.length() == 5) {
                    for (i in 0 until 5) {
                        val f = arr.getJSONObject(i)
                        fibos.add(FiboLevelInput(
                            display = f.optBoolean("display", true),
                            level = f.optDouble("level", 0.5).toFloat(),
                            colorHex = f.optString("colorHex", "#c0c0c0"),
                            style = f.optString("style", "Dashed"),
                            price = 0f
                        ))
                    }
                } else fibos.addAll(defaultFiboLevels())
                PowerHourBreakoutSettings(
                    powerHoursLength = o.optInt("powerHoursLength", 10).coerceAtLeast(1),
                    displayAll = o.optBoolean("displayAll", true),
                    session = o.optString("session", "1500-1600"),
                    showBreakouts = o.optBoolean("showBreakouts", true),
                    bullBreakColorHex = o.optString("bullBreakColorHex", "#089981"),
                    bearBreakColorHex = o.optString("bearBreakColorHex", "#f23645"),
                    topExtension = o.optBoolean("topExtension", true),
                    topMultiplierPct = o.optInt("topMultiplierPct", 50).coerceAtLeast(1),
                    bottomExtension = o.optBoolean("bottomExtension", true),
                    bottomMultiplierPct = o.optInt("bottomMultiplierPct", 50).coerceAtLeast(1),
                    showFibonacci = o.optBoolean("showFibonacci", true),
                    fiboReverse = o.optBoolean("fiboReverse", false),
                    fiboLevels = fibos,
                    fibosLabels = o.optBoolean("fibosLabels", false),
                    fibosLabelSize = o.optInt("fibosLabelSize", 10).coerceIn(1, 50),
                    topColorHex = o.optString("topColorHex", "#089981"),
                    bottomColorHex = o.optString("bottomColorHex", "#f23645"),
                    transparency = o.optInt("transparency", 80).coerceIn(0, 100),
                    sessionStartMarkers = o.optBoolean("sessionStartMarkers", true),
                    backgroundColorHex = o.optString("backgroundColorHex", "#c0c0c0")
                )
            }.getOrDefault(PowerHourBreakoutSettings())
        }

        fun defaultFiboLevels(): List<FiboLevelInput> = listOf(
            FiboLevelInput(true, 0.786f, "#c0c0c0", "Dashed", 0f),
            FiboLevelInput(true, 0.618f, "#c0c0c0", "Dashed", 0f),
            FiboLevelInput(true, 0.500f, "#c0c0c0", "Dashed", 0f),
            FiboLevelInput(true, 0.382f, "#c0c0c0", "Dashed", 0f),
            FiboLevelInput(true, 0.236f, "#c0c0c0", "Dashed", 0f)
        )
    }
}

/** One computed power-hour frame ready for the renderer. */
data class PowerHourFrame(
    val startTime: Long,      // bar open time of first inside bar (Pine: time on powerHourStart)
    val endTime: Long,        // bar open time of last inside bar (Pine: endTime updated while inside)
    val endSession: Long,     // bar open time of last bar processed while frame was current (Pine: endSession updated every bar)
    val top: Float,           // session high
    val bottom: Float,        // session low
    val topExt: Float?,       // top + mult * range (null if disabled)
    val bottomExt: Float?,    // bottom - mult * range (null if disabled)
    val fibos: List<FiboLevelInput> // price-resolved fibo levels for this frame
)

data class PowerHourBreakout(
    val time: Long,
    val bull: Boolean
)

data class PowerHourBreakoutResult(
    val frames: List<PowerHourFrame>,
    val breakouts: List<PowerHourBreakout>
)

object PowerHourBreakoutIndicator {

    /** Mobile GPU cap on rendered sessions (Pine max_boxes_count = 500). */
    const val MAX_RENDER = 40
    /** Cap on breakout markers rendered. */
    const val MAX_BREAKOUT_MARKERS = 120

    private val NY = ZoneId.of("America/New_York")

    /** Parse a Pine session string "HHMM-HHMM" into [startMin, endMin] (wrap-safe). */
    private fun parseSession(session: String): Pair<Int, Int> {
        val parts = session.trim().split("-")
        fun toMin(s: String): Int {
            val t = s.trim()
            if (t.length != 4) return 0
            val h = t.substring(0, 2).toIntOrNull() ?: 0
            val m = t.substring(2, 4).toIntOrNull() ?: 0
            return h.coerceIn(0, 23) * 60 + m.coerceIn(0, 59)
        }
        val start = parts.getOrNull(0)?.let { toMin(it) } ?: 900
        val end = parts.getOrNull(1)?.let { toMin(it) } ?: start
        return start to end
    }

    /** Chart timestamps are epoch seconds; normalize any legacy millisecond values first. */
    private fun toEpochSeconds(chartTime: Long): Long =
        if (chartTime >= 1_000_000_000_000L) chartTime / 1000L else chartTime

    /** True when the bar's open time falls inside the NY session window. */
    private fun insideSession(chartTime: Long, startMin: Int, endMin: Int): Boolean {
        val zdt = Instant.ofEpochSecond(toEpochSeconds(chartTime)).atZone(NY)
        val mins = zdt.hour * 60 + zdt.minute
        return if (endMin > startMin) mins in startMin until endMin
        else mins >= startMin || mins < endMin
    }

    fun calculate(candles: List<OHLCData>, s: PowerHourBreakoutSettings): PowerHourBreakoutResult {
        val n = candles.size
        val empty = PowerHourBreakoutResult(emptyList(), emptyList())
        if (n == 0) return empty

        val (startMin, endMin) = parseSession(s.session)
        val high = FloatArray(n) { candles[it].high }
        val low = FloatArray(n) { candles[it].low }
        val close = FloatArray(n) { candles[it].close }
        val times = LongArray(n) { candles[it].time }

        // Pine: insidePowerHour = not na(time(timeframe.period, session, 'America/New_York'))
        // powerHourStart = insidePowerHour and not insidePowerHour[1]
        val inside = BooleanArray(n)
        for (i in 0 until n) inside[i] = insideSession(times[i], startMin, endMin)

        data class LiveFrame(
            var startTime: Long, var endTime: Long, var endSession: Long,
            var top: Float, var bottom: Float
        )
        val frames = mutableListOf<LiveFrame>()
        val breakouts = mutableListOf<PowerHourBreakout>()

        for (i in 0 until n) {
            val t = times[i]
            // gatherData()
            if (inside[i] && (i == 0 || !inside[i - 1])) {
                // powerHourStart: new frame at this bar's open time
                frames.add(LiveFrame(t, t, t, high[i], low[i]))
            }
            if (frames.isNotEmpty()) {
                val cur = frames[frames.size - 1]
                // Pine: currentPowerHour.endSession := time (every bar)
                cur.endSession = t
                if (inside[i]) {
                    // Pine: only while inside
                    cur.endTime = t
                    cur.top = max(cur.top, high[i])
                    cur.bottom = min(cur.bottom, low[i])
                }
            }

            // breakouts() - only when outside the power hour, against the LAST frame
            if (s.showBreakouts && frames.isNotEmpty() && !inside[i] && i > 0) {
                val last = frames[frames.size - 1]
                val bull = close[i - 1] < last.top && close[i] > last.top
                val bear = close[i - 1] > last.bottom && close[i] < last.bottom
                if (bull) breakouts.add(PowerHourBreakout(t, true))
                else if (bear) breakouts.add(PowerHourBreakout(t, false))
            }
        }

        // drawPowerHours(length): slice(size - (displayAll ? size : length), size)
        val from = if (s.displayAll) 0 else (frames.size - s.powerHoursLength.coerceAtLeast(1)).coerceAtLeast(0)
        // Mobile cap on total rendered sessions
        val kept = frames.subList(from, frames.size).let { if (it.size > MAX_RENDER) it.takeLast(MAX_RENDER) else it }

        val outFrames = mutableListOf<PowerHourFrame>()
        val topMult = s.topMultiplierPct.coerceAtLeast(1) / 100f
        val bottomMult = s.bottomMultiplierPct.coerceAtLeast(1) / 100f
        for (f in kept) {
            val range = (f.top - f.bottom).coerceAtLeast(0f)
            var fibos = listOf<FiboLevelInput>()
            if (s.showFibonacci) {
                fibos = s.fiboLevels.map { fl ->
                    val fiboRange = fl.level * range
                    val price = if (s.fiboReverse) f.bottom + fiboRange else f.top - fiboRange
                    FiboLevelInput(fl.display, fl.level, fl.colorHex, fl.style, price)
                }
            }
            outFrames.add(
                PowerHourFrame(
                    startTime = f.startTime,
                    endTime = f.endTime,
                    endSession = f.endSession,
                    top = f.top,
                    bottom = f.bottom,
                    topExt = if (s.topExtension) f.top + topMult * range else null,
                    bottomExt = if (s.bottomExtension) f.bottom - bottomMult * range else null,
                    fibos = fibos
                )
            )
        }

        // Cap breakout markers (all are historical; stagger is left to renderer)
        val mk = if (breakouts.size > MAX_BREAKOUT_MARKERS) breakouts.takeLast(MAX_BREAKOUT_MARKERS) else breakouts

        return PowerHourBreakoutResult(outFrames, mk)
    }
}