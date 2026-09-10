package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Port of "Confluence FVG Finder" (Pine v6).
 *
 * Multi-timeframe Fair Value Gap confluence: detects FVGs on configurable
 * timeframes (synthesized by aggregating chart candles - equivalent of
 * request.security), merges zones whose midpoints sit within Proximity x ATR of
 * each other, requires >= minConfluence timeframes, rates strength 0-10 from
 * gap/ATR ratio, tags the forming session, normalizes zone heights (ATR based or
 * fixed %), applies gap filters, mitigation modes and optional mitigated display.
 */
data class ConfluenceFvgZone(
    val top: Float,
    val bot: Float,
    val bullish: Boolean,
    val session: String,
    val formTime: Long,
    val strength: Float,
    val confluenceCount: Int,
    val pips: Int,
    val distancePips: Int,
    val mitigated: Boolean = false
)

data class ConfluenceFvgData(
    val zones: List<ConfluenceFvgZone>,
    val refAtr: Float
)

data class ConfluenceFvgSettings(
    // Universal Zone Settings
    val useNormalizedZones: Boolean = true,
    val zoneHeightMethod: String = "atr",      // "atr" | "percent"
    val zoneHeightAtrMult: Float = 0.75f,
    val zoneHeightPercent: Float = 0.3f,
    // FVG Detection
    val showBullish: Boolean = true,
    val showBearish: Boolean = true,
    val maxZonesPerSide: Int = 8,
    // Multi-Timeframe Confluence
    val tf1Min: Int = 60,
    val tf2Min: Int = 120,
    val tf3Min: Int = 240,
    val minConfluence: Int = 2,
    val proximityAtrMult: Float = 5f,
    // Strength Rating
    val enableStrengthRating: Boolean = true,
    val minStrengthFilter: Float = 3f,
    val confluenceBonus: Float = 0.5f,
    // FVG Styling
    val bullishColorHex: String = "#089981",
    val bearishColorHex: String = "#f23645",
    val bullishBorderHex: String = "#089981",
    val bearishBorderHex: String = "#f23645",
    val borderWidth: Int = 2,
    val showDirectionLabels: Boolean = true,
    // Extended Info Labels
    val showExtendedInfo: Boolean = true,
    val infoTextSize: String = "Large",       // Tiny/Small/Normal/Large/Huge
    // Mitigation Settings
    val mitigationType: String = "fifty",      // "touch" | "full" | "fifty"
    val showMitigated: Boolean = false,
    val mitigatedColorHex: String = "#787b86",
    // Filter Settings
    val minGapSize: Float = 0f,
    val useAtrFilter: Boolean = true,
    val atrMultiplier: Float = 0.5f,
    val atrLength: Int = 14,
    // Alarms
    val alertsEnabled: Boolean = true,
    // Style tab
    val styleBoxes: Boolean = true,
    val stylePaneLabels: Boolean = true,
    val stylePrecision: String = "Default",
    val styleInputsInStatusLine: Boolean = true,
    // Visibility tab
    val visTicks: Boolean = true,
    val visSeconds: Boolean = true,
    val visMinutes: Boolean = true,
    val visHours: Boolean = true,
    val visDays: Boolean = true,
    val visWeeks: Boolean = true,
    val visMonths: Boolean = true,
    val visRanges: Boolean = true
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("useNormalizedZones", useNormalizedZones)
        o.put("zoneHeightMethod", zoneHeightMethod)
        o.put("zoneHeightAtrMult", zoneHeightAtrMult.toDouble())
        o.put("zoneHeightPercent", zoneHeightPercent.toDouble())
        o.put("showBullish", showBullish)
        o.put("showBearish", showBearish)
        o.put("maxZonesPerSide", maxZonesPerSide)
        o.put("tf1Min", tf1Min)
        o.put("tf2Min", tf2Min)
        o.put("tf3Min", tf3Min)
        o.put("minConfluence", minConfluence)
        o.put("proximityAtrMult", proximityAtrMult.toDouble())
        o.put("enableStrengthRating", enableStrengthRating)
        o.put("minStrengthFilter", minStrengthFilter.toDouble())
        o.put("confluenceBonus", confluenceBonus.toDouble())
        o.put("bullishColorHex", bullishColorHex)
        o.put("bearishColorHex", bearishColorHex)
        o.put("bullishBorderHex", bullishBorderHex)
        o.put("bearishBorderHex", bearishBorderHex)
        o.put("borderWidth", borderWidth)
        o.put("showDirectionLabels", showDirectionLabels)
        o.put("showExtendedInfo", showExtendedInfo)
        o.put("infoTextSize", infoTextSize)
        o.put("mitigationType", mitigationType)
        o.put("showMitigated", showMitigated)
        o.put("mitigatedColorHex", mitigatedColorHex)
        o.put("minGapSize", minGapSize.toDouble())
        o.put("useAtrFilter", useAtrFilter)
        o.put("atrMultiplier", atrMultiplier.toDouble())
        o.put("atrLength", atrLength)
        o.put("alertsEnabled", alertsEnabled)
        o.put("styleBoxes", styleBoxes)
        o.put("stylePaneLabels", stylePaneLabels)
        o.put("stylePrecision", stylePrecision)
        o.put("styleInputsInStatusLine", styleInputsInStatusLine)
        o.put("visTicks", visTicks)
        o.put("visSeconds", visSeconds)
        o.put("visMinutes", visMinutes)
        o.put("visHours", visHours)
        o.put("visDays", visDays)
        o.put("visWeeks", visWeeks)
        o.put("visMonths", visMonths)
        o.put("visRanges", visRanges)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): ConfluenceFvgSettings {
            if (raw.isNullOrBlank()) return ConfluenceFvgSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                ConfluenceFvgSettings(
                    useNormalizedZones = o.optBoolean("useNormalizedZones", true),
                    zoneHeightMethod = o.optString("zoneHeightMethod", "atr"),
                    zoneHeightAtrMult = o.optDouble("zoneHeightAtrMult", 0.75).toFloat(),
                    zoneHeightPercent = o.optDouble("zoneHeightPercent", 0.3).toFloat(),
                    showBullish = o.optBoolean("showBullish", true),
                    showBearish = o.optBoolean("showBearish", true),
                    maxZonesPerSide = o.optInt("maxZonesPerSide", 8),
                    tf1Min = o.optInt("tf1Min", 60),
                    tf2Min = o.optInt("tf2Min", 120),
                    tf3Min = o.optInt("tf3Min", 240),
                    minConfluence = o.optInt("minConfluence", 2),
                    proximityAtrMult = o.optDouble("proximityAtrMult", 5.0).toFloat(),
                    enableStrengthRating = o.optBoolean("enableStrengthRating", true),
                    minStrengthFilter = o.optDouble("minStrengthFilter", 3.0).toFloat(),
                    confluenceBonus = o.optDouble("confluenceBonus", 0.5).toFloat(),
                    bullishColorHex = o.optString("bullishColorHex", "#089981"),
                    bearishColorHex = o.optString("bearishColorHex", "#f23645"),
                    bullishBorderHex = o.optString("bullishBorderHex", "#089981"),
                    bearishBorderHex = o.optString("bearishBorderHex", "#f23645"),
                    borderWidth = o.optInt("borderWidth", 2),
                    showDirectionLabels = o.optBoolean("showDirectionLabels", true),
                    showExtendedInfo = o.optBoolean("showExtendedInfo", true),
                    infoTextSize = o.optString("infoTextSize", "Large"),
                    mitigationType = o.optString("mitigationType", "fifty"),
                    showMitigated = o.optBoolean("showMitigated", false),
                    mitigatedColorHex = o.optString("mitigatedColorHex", "#787b86"),
                    minGapSize = o.optDouble("minGapSize", 0.0).toFloat(),
                    useAtrFilter = o.optBoolean("useAtrFilter", true),
                    atrMultiplier = o.optDouble("atrMultiplier", 0.5).toFloat(),
                    atrLength = o.optInt("atrLength", 14),
                    alertsEnabled = o.optBoolean("alertsEnabled", true),
                    styleBoxes = o.optBoolean("styleBoxes", true),
                    stylePaneLabels = o.optBoolean("stylePaneLabels", true),
                    stylePrecision = o.optString("stylePrecision", "Default"),
                    styleInputsInStatusLine = o.optBoolean("styleInputsInStatusLine", true),
                    visTicks = o.optBoolean("visTicks", true),
                    visSeconds = o.optBoolean("visSeconds", true),
                    visMinutes = o.optBoolean("visMinutes", true),
                    visHours = o.optBoolean("visHours", true),
                    visDays = o.optBoolean("visDays", true),
                    visWeeks = o.optBoolean("visWeeks", true),
                    visMonths = o.optBoolean("visMonths", true),
                    visRanges = o.optBoolean("visRanges", true)
                )
            }.getOrDefault(ConfluenceFvgSettings())
        }
    }
}

class ConfluenceFvgIndicator(
    private val isGoldSymbol: Boolean = false
) : TradingIndicator {
    override val id = "CONFLUENCE_FVG"
    override val name = "Confluence FVG Finder"
    override val color = android.graphics.Color.parseColor("#089981")

    override fun calculate(candles: List<OHLCData>): List<Float?> = List(candles.size) { null }

    /** One active-zone lifetime on a single timeframe. */
    private data class Active(
        val top: Float,
        val bot: Float,
        val strength: Float,
        val sessionInt: Int,
        val startTime: Long,
        val count: Int,
        var endExclusive: Long = Long.MAX_VALUE
    )

    // Pine session windows (GMT minutes), same precedence order as the script
    private fun sessionOf(epochSec: Long): Int {
        val minutes = ((epochSec % 86400L) / 60L).toInt()
        return when {
            minutes in 480 until 1020 -> 1 // London
            minutes in 780 until 1320 -> 2 // NY
            minutes in 0 until 540 -> 3    // Asian
            else -> 0                      // Other
        }
    }

    private fun sessionStr(i: Int) = when (i) {
        1 -> "London"; 2 -> "NY"; 3 -> "Asian"; else -> "Other"
    }

    private fun strengthOf(gap: Float, atrVal: Float, s: ConfluenceFvgSettings): Float {
        if (!s.enableStrengthRating) return 5f
        if (atrVal <= 0f) return 3f
        val ratio = gap / atrVal
        return when {
            ratio >= 1.5f -> 8f
            ratio >= 1.0f -> 6f
            ratio >= 0.75f -> 4.5f
            ratio >= 0.5f -> 3f
            else -> 1.5f
        }
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

    /** Walk a grouped series collecting every distinct active-zone period (Pine detect_tf_*). */
    private fun detectEvents(series: List<OHLCData>, bullish: Boolean, s: ConfluenceFvgSettings): List<Active> {
        if (series.size < 4) return emptyList()
        val atrSeries = AtrIndicator(s.atrLength).calculate(series)
        val events = ArrayList<Active>()
        var active: Active? = null

        for (i in 3 until series.size) {
            val atrv = atrSeries.getOrNull(i - 1) ?: atrSeries.getOrNull(i) ?: 0f
            val prevLow = series[i - 1].low
            val prevHigh = series[i - 1].high
            val thirdBackHigh = series[i - 3].high
            val thirdBackLow = series[i - 3].low

            val signal: Pair<Float, Float>? = if (bullish) {
                if (prevLow > thirdBackHigh) prevLow to thirdBackHigh else null
            } else {
                if (prevHigh < thirdBackLow) thirdBackLow to prevHigh else null
            }

            if (signal != null) {
                val (gapTop, gapBot) = signal
                val gap = gapTop - gapBot
                val minGap = if (s.useAtrFilter) atrv * s.atrMultiplier else s.minGapSize
                if (gap >= minGap && gap > 0f) {
                    // new signal replaces the current active zone (Pine var-overwrite)
                    active?.let { it.endExclusive = series[i - 1].time; events.add(it) }
                    active = Active(
                        top = gapTop, bot = gapBot,
                        strength = strengthOf(gap, atrv, s),
                        sessionInt = sessionOf(series[i - 1].time),
                        startTime = series[i - 1].time,
                        count = 1
                    )
                    continue
                }
            }

            // invalidation: close[1] beyond the far side of the zone
            val a = active ?: continue
            val prevClose = series[i - 1].close
            val invalidated = if (bullish) prevClose < a.bot else prevClose > a.top
            if (invalidated) {
                a.endExclusive = series[i - 1].time
                active = null
            }
        }
        active?.let { events.add(it) }
        return events
    }

    private fun overlaps(a: Active, b: Active) = a.startTime < b.endExclusive && b.startTime < a.endExclusive

    private fun pipsOf(diff: Float): Int {
        val pipValue = if (isGoldSymbol) 0.01f else 0.0001f
        return if (pipValue <= 0f) 0 else (diff / pipValue).roundToInt()
    }

    /** Pine mitigation modes against chart candles after zone formation. */
    private fun isMitigated(z: Active, bullish: Boolean, candles: List<OHLCData>, mode: String): Boolean {
        val mid = (z.top + z.bot) / 2f
        for (c in candles) {
            if (c.time <= z.startTime) continue
            val hit = if (bullish) {
                when (mode) {
                    "touch" -> c.low <= z.top
                    "full" -> c.low <= z.bot
                    else -> c.low <= mid          // "50% Fill"
                }
            } else {
                when (mode) {
                    "touch" -> c.high >= z.bot
                    "full" -> c.high >= z.top
                    else -> c.high >= mid         // "50% Fill"
                }
            }
            if (hit) return true
        }
        return false
    }

    fun calculateZones(
        candles: List<OHLCData>,
        chartTfSec: Long,
        lastPrice: Float,
        settings: ConfluenceFvgSettings = ConfluenceFvgSettings()
    ): ConfluenceFvgData? {
        val s = settings
        if (candles.size < 20 || lastPrice <= 0f) return null
        if (!s.showBullish && !s.showBearish) return null
        val tfsRaw = listOf(s.tf1Min, s.tf2Min, s.tf3Min).map { it * 60L }
        val tfs = tfsRaw.filter { it >= chartTfSec && it % max(chartTfSec, 60L) == 0L }
        if (tfs.isEmpty()) return null

        val grouped = tfs.map { tf -> group(candles, tf) }
        if (grouped.any { it.size < 6 }) return null
        val refSeries = grouped.first()
        val refAtr = AtrIndicator(s.atrLength).calculate(refSeries).lastOrNull { it != null && it > 0f } ?: return null
        val tolerance = refAtr * s.proximityAtrMult

        val result = ArrayList<ConfluenceFvgZone>()
        for (bullish in listOf(true, false)) {
            if (bullish && !s.showBullish) continue
            if (!bullish && !s.showBearish) continue

            val perTfEvents = grouped.map { g -> detectEvents(g, bullish, s) }
            val candidates = ArrayList<Active>()

            for (anchor in perTfEvents.first()) {
                var top = anchor.top
                var bot = anchor.bot
                var str = anchor.strength
                var cnt = 1
                var formTime = anchor.startTime
                for (otherIdx in 1 until perTfEvents.size) {
                    val match = perTfEvents[otherIdx].firstOrNull { other ->
                        overlaps(anchor, other) &&
                            abs((top + bot) / 2f - (other.top + other.bot) / 2f) <= tolerance
                    } ?: continue
                    top = max(top, match.top)
                    bot = min(bot, match.bot)
                    str = max(str, match.strength)
                    cnt += 1
                    formTime = max(formTime, match.startTime)
                }
                if (cnt >= s.minConfluence.coerceIn(1, 3)) {
                    val mergedStrength = min(str + max(cnt - 1, 0) * s.confluenceBonus, 10f)
                    if (mergedStrength >= s.minStrengthFilter) {
                        candidates.add(
                            Active(top, bot, mergedStrength, anchor.sessionInt, formTime, cnt)
                        )
                    }
                }
            }

            // dedupe near-identical mids (< 0.5 ATR), keep earliest/strongest first
            val deduped = ArrayList<Active>()
            for (c in candidates.sortedWith(compareBy({ it.startTime }, { -it.strength }))) {
                val mid = (c.top + c.bot) / 2f
                if (deduped.none { abs((it.top + it.bot) / 2f - mid) < refAtr * 0.5f }) deduped.add(c)
            }
            val kept = deduped.takeLast(s.maxZonesPerSide.coerceIn(1, 50))

            for (z in kept) {
                val mitigated = isMitigated(z, bullish, candles, s.mitigationType)
                if (mitigated && !s.showMitigated) continue

                val mid = (z.top + z.bot) / 2f
                val finalTop: Float; val finalBot: Float
                if (s.useNormalizedZones) {
                    val target = if (s.zoneHeightMethod == "percent") {
                        lastPrice * (s.zoneHeightPercent / 100f)
                    } else {
                        refAtr * s.zoneHeightAtrMult
                    }.coerceAtLeast(1e-6f)
                    finalTop = mid + target / 2f
                    finalBot = mid - target / 2f
                } else {
                    finalTop = z.top; finalBot = z.bot
                }
                result.add(
                    ConfluenceFvgZone(
                        top = finalTop, bot = finalBot,
                        bullish = bullish,
                        session = sessionStr(z.sessionInt),
                        formTime = z.startTime,
                        strength = z.strength,
                        confluenceCount = z.count,
                        pips = pipsOf(finalTop - finalBot),
                        distancePips = pipsOf(abs(lastPrice - mid)),
                        mitigated = mitigated
                    )
                )
            }
        }
        if (result.isEmpty()) return null
        return ConfluenceFvgData(result.sortedBy { it.formTime }, refAtr)
    }
}
