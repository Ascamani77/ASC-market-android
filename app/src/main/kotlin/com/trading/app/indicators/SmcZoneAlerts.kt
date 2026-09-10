package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * SMC zone detection used by stream-chart alerts.
 *
 * A snapshot bundles the price zones the user cares about (Fair Value Gap,
 * Order Block, Confluence FVG, Supply & Demand, OTE box) so the alert loop can
 * answer "which zones is the current price touching right now?" without
 * depending on which indicators happen to be drawn on the chart.
 */
data class SmcZoneSnapshot(
    /** Unmitigated FVGs (recent bars only). */
    val fvgs: List<FvgRecord>,
    /** Order Block zones as (top, bottom) pairs with top >= bottom. */
    val orderBlocks: List<Pair<Float, Float>>,
    /** Merged multi-timeframe FVG zones. */
    val confluenceZones: List<ConfluenceFvgZone>,
    /** Visible-range supply/demand zones. */
    val sdResult: SdVrResult?,
    /** OTE 61.8-78.6% fib box. */
    val ote: OteVisibleChartData?
)

object SmcZoneAlerts {

    /** Canonical zone keys used by UserAlert.smcZones. */
    val zoneKeys: List<String> = listOf("FVG", "OB", "CFVG", "SD", "OTE")

    val zoneLabels: Map<String, String> = mapOf(
        "FVG" to "Fair Value Gap",
        "OB" to "Order Block",
        "CFVG" to "Confluence FVG",
        "SD" to "Supply & Demand",
        "OTE" to "OTE box"
    )

    private const val MAX_BARS = 400
    private const val SD_BARS = 200

    /** Builds a zone snapshot from the current chart candles. */
    fun snapshot(
        candles: List<OHLCData>,
        chartTfSec: Long,
        cfvgSettings: ConfluenceFvgSettings = ConfluenceFvgSettings()
    ): SmcZoneSnapshot {
        val recent = candles.takeLast(MAX_BARS)

        val fvgs = runCatching {
            FairValueGapIndicator().calculateFvg(recent, FairValueGapSettings(), chartTfSec).fvgs
        }.getOrDefault(emptyList())

        val orderBlocks = runCatching {
            detectOrderBlocks(recent, lookback = 120)
        }.getOrDefault(emptyList())

        val confluence = runCatching {
            val lastClose = recent.lastOrNull()?.close ?: 0f
            ConfluenceFvgIndicator().calculateZones(recent, chartTfSec, lastClose, cfvgSettings)?.zones ?: emptyList()
        }.getOrDefault(emptyList())

        val sd = runCatching {
            SupplyDemandVrIndicator.calculate(
                recent.takeLast(SD_BARS),
                SupplyDemandVrSettings().thresholdPercent,
                SupplyDemandVrSettings().resolution
            )
        }.getOrDefault(null)

        val ote = runCatching { OteVisibleChartIndicator().calculateOte(recent) }.getOrDefault(null)

        return SmcZoneSnapshot(fvgs, orderBlocks, confluence, sd, ote)
    }

    /**
     * Returns the set of zone keys whose range currently contains [price].
     * A small touch buffer (~1 pip scale) lets "touching" a zone fire at its
     * boundary, not only strictly inside it.
     */
    fun touchedZones(s: SmcZoneSnapshot, price: Float): Set<String> {
        val out = mutableSetOf<String>()
        if (price <= 0f) return out
        val buf = max(price * 0.0001f, 0.00005f)

        if (s.fvgs.any { price >= min(it.min, it.max) - buf && price <= max(it.min, it.max) + buf }) {
            out.add("FVG")
        }
        if (s.orderBlocks.any {
                price >= it.second - buf && price <= it.first + buf
            }
        ) {
            out.add("OB")
        }
        if (s.confluenceZones.any { price >= min(it.bot, it.top) - buf && price <= max(it.bot, it.top) + buf }) {
            out.add("CFVG")
        }
        val sd = s.sdResult
        if (sd != null) {
            val supplyTouched = sd.supply.found &&
                price >= min(sd.supply.top, sd.supply.bottom) - buf &&
                price <= max(sd.supply.top, sd.supply.bottom) + buf
            val demandTouched = sd.demand.found &&
                price >= min(sd.demand.top, sd.demand.bottom) - buf &&
                price <= max(sd.demand.top, sd.demand.bottom) + buf
            if (supplyTouched || demandTouched) out.add("SD")
        }
        val ote = s.ote
        if (ote != null) {
            val hi = max(ote.boxTop, ote.boxBottom)
            val lo = min(ote.boxTop, ote.boxBottom)
            if (price >= lo - buf && price <= hi + buf) out.add("OTE")
        }
        return out
    }

    /**
     * Lightweight Order Block detector: a zone is the last opposite-coloured
     * candle immediately before a strong impulse candle (body >= avgBody * 2).
     */
    fun detectOrderBlocks(candles: List<OHLCData>, lookback: Int = 120): List<Pair<Float, Float>> {
        if (candles.size < 40) return emptyList()
        val n = candles.size
        val start = max(0, n - lookback)

        val bodies = FloatArray(n)
        var bodySum = 0f
        var cnt = 0
        for (i in start until n) {
            val b = abs(candles[i].close - candles[i].open)
            bodies[i] = b
            bodySum += b
            cnt++
        }
        if (cnt == 0) return emptyList()
        val avgBody = bodySum / cnt
        if (avgBody <= 0f) return emptyList()
        val impMult = 2.0f

        val zones = mutableListOf<Pair<Float, Float>>()
        for (i in start + 2 until n) {
            val c = candles[i]
            if (bodies[i] < avgBody * impMult) continue
            val bullishImpulse = c.close > c.open
            var j = i - 1
            while (j >= start && candles[j].close == candles[j].open) j--
            if (j < start) continue
            val prev = candles[j]
            val prevBull = prev.close > prev.open
            val obOpposite = if (bullishImpulse) !prevBull else prevBull
            if (obOpposite) zones.add(Pair(max(prev.high, prev.low), min(prev.high, prev.low)))
        }
        return zones.takeLast(30)
    }
}