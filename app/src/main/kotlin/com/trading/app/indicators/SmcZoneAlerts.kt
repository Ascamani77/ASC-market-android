package com.trading.app.indicators

import com.trading.app.models.OHLCData
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
    /** LuxAlgo Order & Breaker Blocks as (top, bottom) pairs with top >= bottom. */
    val orderBlocks: List<Pair<Float, Float>>,
    /** Merged multi-timeframe FVG zones. */
    val confluenceZones: List<ConfluenceFvgZone>,
    /** Visible-range supply/demand zones. */
    val sdResult: SdVrResult?,
    /** OTE 61.8-78.6% fib box. */
    val ote: OteVisibleChartData?,
    /** Premium & Discount SR bands (premium band + discount band). */
    val pd: PremiumDiscountData? = null,
    /** Liquidity Pools zones as (top, bottom) pairs. */
    val lpZones: List<Pair<Float, Float>> = emptyList(),
    /** EQH/EQL active zones as (top, bottom) pairs. */
    val eqZones: List<Pair<Float, Float>> = emptyList(),
    /** Liquidity Delta Profiler unswept zones as (top, bottom) pairs. */
    val ldpZones: List<Pair<Float, Float>> = emptyList(),
    /** Power Hour session boxes as (top, bottom) pairs. */
    val phBoxes: List<Pair<Float, Float>> = emptyList(),
    /** Volumatic FVG zones as (top, bottom) pairs. */
    val vfvgZones: List<Pair<Float, Float>> = emptyList(),
    /** Auto Fib retracement level prices. */
    val fibLevels: List<Float> = emptyList(),
    /** Trendline-Breakouts line values interpolated at the last bar. */
    val tbtValues: List<Float> = emptyList(),
    /** Trendline-Navigator line values interpolated at the last bar. */
    val tnavValues: List<Float> = emptyList()
)

object SmcZoneAlerts {

    /** Canonical zone keys used by UserAlert.smcZones. */
    val zoneKeys: List<String> = listOf(
        "FVG", "OB", "CFVG", "SD", "OTE", "PD",
        "LP", "EQ", "LDP", "PH", "VFVG", "FIB", "TBT", "TNAV"
    )

    /** Max SMC zones combinable in a single alert. */
    const val MAX_ZONES_PER_ALERT: Int = 4

    val zoneLabels: Map<String, String> = mapOf(
        "FVG" to "Fair Value Gap",
        "OB" to "Order Block",
        "CFVG" to "Confluence FVG",
        "SD" to "Supply & Demand",
        "OTE" to "OTE box",
        "PD" to "Premium / Discount",
        "LP" to "Liquidity Pools",
        "EQ" to "EQH / EQL Zones",
        "LDP" to "Liq. Delta Profiler",
        "PH" to "Power Hour Box",
        "VFVG" to "Volumatic FVG",
        "FIB" to "Auto Fib Levels",
        "TBT" to "Trendline Breakouts",
        "TNAV" to "Trendline Navigator"
    )

    private const val MAX_BARS = 400
    private const val SD_BARS = 200

    /** Builds a zone snapshot from the current chart candles. Only [keys] are computed. */
    fun snapshot(
        candles: List<OHLCData>,
        chartTfSec: Long,
        keys: Set<String> = zoneKeys.toSet(),
        cfvgSettings: ConfluenceFvgSettings = ConfluenceFvgSettings()
    ): SmcZoneSnapshot {
        val recent = candles.takeLast(MAX_BARS)

        val fvgs = if ("FVG" in keys) runCatching {
            FairValueGapIndicator().calculateFvg(recent, FairValueGapSettings(), chartTfSec).fvgs
        }.getOrDefault(emptyList()) else emptyList()

        // OB = the LuxAlgo Order & Breaker Blocks engine (same boxes as the overlay)
        val orderBlocks = if ("OB" in keys) runCatching {
            val obb = OrderBlockBreakerIndicator.calculate(recent, OrderBlockBreakerSettings())
            (obb.bull + obb.bear).map { d -> max(d.top, d.btm) to min(d.top, d.btm) }
        }.getOrDefault(emptyList()) else emptyList()

        val confluence = if ("CFVG" in keys) runCatching {
            val lastClose = recent.lastOrNull()?.close ?: 0f
            ConfluenceFvgIndicator().calculateZones(recent, chartTfSec, lastClose, cfvgSettings)?.zones ?: emptyList()
        }.getOrDefault(emptyList()) else emptyList()

        val sd = if ("SD" in keys) runCatching {
            SupplyDemandVrIndicator.calculate(
                recent.takeLast(SD_BARS),
                SupplyDemandVrSettings().thresholdPercent,
                SupplyDemandVrSettings().resolution
            )
        }.getOrDefault(null) else null

        val ote = if ("OTE" in keys) runCatching { OteVisibleChartIndicator().calculateOte(recent) }.getOrDefault(null) else null

        val pd = if ("PD" in keys) runCatching { PremiumDiscountIndicator().calculatePremiumDiscount(recent) }.getOrDefault(null) else null

        val lpZones = if ("LP" in keys) runCatching {
            LiquidityPoolsIndicator.calculate(recent, LiquidityPoolsSettings()).zones
                .map { z -> max(z.top, z.bottom) to min(z.top, z.bottom) }
        }.getOrDefault(emptyList()) else emptyList()

        val eqZones = if ("EQ" in keys) runCatching {
            EqhEqlLiquidityZonesIndicator.calculate(recent, EqhEqlLiquidityZonesSettings()).active
                .map { z -> max(z.top, z.bottom) to min(z.top, z.bottom) }
        }.getOrDefault(emptyList()) else emptyList()

        val ldpZones = if ("LDP" in keys) runCatching {
            val r = LiquidityDeltaProfilerIndicator.calculate(recent, LiquidityDeltaProfilerSettings())
            ((r?.bslZones ?: emptyList()) + (r?.sslZones ?: emptyList())).filter { !it.swept }
                .map { z -> max(z.top, z.bottom) to min(z.top, z.bottom) }
        }.getOrDefault(emptyList()) else emptyList()

        val phBoxes = if ("PH" in keys) runCatching {
            PowerHourBreakoutIndicator.calculate(recent, PowerHourBreakoutSettings()).frames
                .map { f -> max(f.top, f.bottom) to min(f.top, f.bottom) }
        }.getOrDefault(emptyList()) else emptyList()

        val vfvgZones = if ("VFVG" in keys) runCatching {
            VolumaticFvgIndicator.calculate(recent, VolumaticFvgSettings()).items
                .map { d -> max(d.top, d.bottom) to min(d.top, d.bottom) }
        }.getOrDefault(emptyList()) else emptyList()

        val fibLevels = if ("FIB" in keys) runCatching {
            AutoFibRetracementIndicator().calculateAutoFib(recent)?.levels?.map { it.price } ?: emptyList()
        }.getOrDefault(emptyList()) else emptyList()

        val lastTime = recent.lastOrNull()?.time
        val tbtValues = if ("TBT" in keys && lastTime != null) runCatching {
            TrendlineBreakoutsIndicator.calculate(recent, TrendlineBreakoutsSettings()).segments.mapNotNull { sg ->
                lineValueAt(sg.startTime, sg.startPrice, sg.endTime, sg.endPrice, lastTime)
            }
        }.getOrDefault(emptyList()) else emptyList()

        val tnavValues = if ("TNAV" in keys && lastTime != null) runCatching {
            TrendlineNavigatorIndicator.calculate(recent, TrendlineNavigatorSettings()).segments.mapNotNull { sg ->
                lineValueAt(sg.startTime, sg.startPrice, sg.endTime, sg.endPrice, lastTime)
            }
        }.getOrDefault(emptyList()) else emptyList()

        return SmcZoneSnapshot(
            fvgs, orderBlocks, confluence, sd, ote, pd,
            lpZones, eqZones, ldpZones, phBoxes, vfvgZones, fibLevels, tbtValues, tnavValues
        )
    }

    /** Interpolated diagonal-line price at bar time [t]; null when [t] is outside the drawn segment. */
    private fun lineValueAt(startTime: Long, startPrice: Float, endTime: Long, endPrice: Float, t: Long): Float? {
        if (endTime <= startTime || t < startTime || t > endTime) return null
        val f = (t - startTime).toFloat() / (endTime - startTime).toFloat()
        return startPrice + (endPrice - startPrice) * f
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
        val pd = s.pd
        if (pd != null) {
            val premTouched = price >= min(pd.srUpperBottom, pd.srUpperTop) - buf &&
                price <= max(pd.srUpperBottom, pd.srUpperTop) + buf
            val discTouched = price >= min(pd.srLowerBottom, pd.srLowerTop) - buf &&
                price <= max(pd.srLowerBottom, pd.srLowerTop) + buf
            if (premTouched || discTouched) out.add("PD")
        }
        fun bandTouched(zones: List<Pair<Float, Float>>): Boolean =
            zones.any { price >= it.second - buf && price <= it.first + buf }
        if (bandTouched(s.lpZones)) out.add("LP")
        if (bandTouched(s.eqZones)) out.add("EQ")
        if (bandTouched(s.ldpZones)) out.add("LDP")
        if (bandTouched(s.phBoxes)) out.add("PH")
        if (bandTouched(s.vfvgZones)) out.add("VFVG")
        if (s.fibLevels.any { kotlin.math.abs(price - it) <= buf }) out.add("FIB")
        if (s.tbtValues.any { kotlin.math.abs(price - it) <= buf }) out.add("TBT")
        if (s.tnavValues.any { kotlin.math.abs(price - it) <= buf }) out.add("TNAV")
        return out
    }
}