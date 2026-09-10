package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.max
import kotlin.math.min

/**
 * Port of LuxAlgo "Supply and Demand Visible Range [LuxAlgo]" v5.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo
 * indicator("Supply and Demand Visible Range [LuxAlgo]", overlay=true, max_boxes_count=500, max_bars_back=500)
 *
 * Unlike the Daily version, zones span the VISIBLE chart range: max/min/volume are
 * accumulated from the left visible bar to the right visible bar and recomputed on
 * every pan/zoom, so the zones are fully dynamic.
 *
 * The Pine script loops intrabar data (request.security_lower_tf). We approximate
 * each chart bar as its own "intrabar" using high/low/volume - same approach used
 * by the Daily port. When a bridge sends no volume, equal bar weighting is used.
 */
data class SdVrColumn(
    val top: Float,
    val btm: Float,
    /** Width of the volume column in bars measured from the left visible bar. */
    val widthBars: Int
)

data class SdVrSide(
    val found: Boolean,
    val top: Float,
    val bottom: Float,
    val avg: Float,
    val wavg: Float,
    /** Histogram columns drawn until the threshold was reached. */
    val columns: List<SdVrColumn>
)

data class SdVrResult(
    /** Index of the left zone edge within the slice passed to calculate(). */
    val x1Index: Int,
    val supply: SdVrSide,
    val demand: SdVrSide,
    val equiAvg: Float,
    val equiWavg: Float
)

data class SupplyDemandVrSettings(
    val thresholdPercent: Float = 10f,
    val resolution: Int = 50,
    val showSupply: Boolean = true,
    val supplyColorHex: String = "#2157f3",
    val supplyArea: Boolean = true,
    val supplyAvg: Boolean = true,
    val supplyWavg: Boolean = true,
    val showEquilibrium: Boolean = true,
    val equilibriumColorHex: String = "#787b86",
    val equilibriumAvg: Boolean = true,
    val equilibriumWavg: Boolean = true,
    val showDemand: Boolean = true,
    val demandColorHex: String = "#ff5d00",
    val demandArea: Boolean = true,
    val demandAvg: Boolean = true,
    val demandWavg: Boolean = true,
    val alertsEnabled: Boolean = true
) {
    fun toJson(): String {
        val o = org.json.JSONObject()
        o.put("thresholdPercent", thresholdPercent.toDouble())
        o.put("resolution", resolution)
        o.put("showSupply", showSupply)
        o.put("supplyColorHex", supplyColorHex)
        o.put("supplyArea", supplyArea)
        o.put("supplyAvg", supplyAvg)
        o.put("supplyWavg", supplyWavg)
        o.put("showEquilibrium", showEquilibrium)
        o.put("equilibriumColorHex", equilibriumColorHex)
        o.put("equilibriumAvg", equilibriumAvg)
        o.put("equilibriumWavg", equilibriumWavg)
        o.put("showDemand", showDemand)
        o.put("demandColorHex", demandColorHex)
        o.put("demandArea", demandArea)
        o.put("demandAvg", demandAvg)
        o.put("demandWavg", demandWavg)
        o.put("alertsEnabled", alertsEnabled)
        return o.toString()
    }

    companion object {
        fun fromJson(raw: String?): SupplyDemandVrSettings {
            if (raw.isNullOrBlank()) return SupplyDemandVrSettings()
            return runCatching {
                val o = org.json.JSONObject(raw)
                SupplyDemandVrSettings(
                    thresholdPercent = o.optDouble("thresholdPercent", 10.0).toFloat(),
                    resolution = o.optInt("resolution", 50),
                    showSupply = o.optBoolean("showSupply", true),
                    supplyColorHex = o.optString("supplyColorHex", "#2157f3"),
                    supplyArea = o.optBoolean("supplyArea", true),
                    supplyAvg = o.optBoolean("supplyAvg", true),
                    supplyWavg = o.optBoolean("supplyWavg", true),
                    showEquilibrium = o.optBoolean("showEquilibrium", true),
                    equilibriumColorHex = o.optString("equilibriumColorHex", "#787b86"),
                    equilibriumAvg = o.optBoolean("equilibriumAvg", true),
                    equilibriumWavg = o.optBoolean("equilibriumWavg", true),
                    showDemand = o.optBoolean("showDemand", true),
                    demandColorHex = o.optString("demandColorHex", "#ff5d00"),
                    demandArea = o.optBoolean("demandArea", true),
                    demandAvg = o.optBoolean("demandAvg", true),
                    demandWavg = o.optBoolean("demandWavg", true),
                    alertsEnabled = o.optBoolean("alertsEnabled", true)
                )
            }.getOrDefault(SupplyDemandVrSettings())
        }
    }
}

object SupplyDemandVrIndicator {

    /**
     * @param slice candles covering the visible range; x1 is always index 0 here
     *              (the Pine script anchors on chart.left_visible_bar_time).
     */
    fun calculate(slice: List<OHLCData>, perIn: Float, divIn: Int): SdVrResult? {
        if (slice.size < 5) return null
        val per = perIn.coerceIn(0f, 100f)
        val div = divIn.coerceIn(2, 500)

        // Accumulate over bars [0 .. size-2] (Pine uses up to n-1 like the daily port)
        var maxP = Float.MIN_VALUE
        var minP = Float.MAX_VALUE
        var csum = 0f
        for (j in 0 until slice.size - 1) {
            maxP = max(maxP, slice[j].high)
            minP = min(minP, slice[j].low)
            csum += slice[j].volume
        }
        if (maxP == Float.MIN_VALUE || minP == Float.MAX_VALUE) return null

        var useCounts = false
        if (csum == 0f) {
            useCounts = true
            csum = (slice.size - 1).coerceAtLeast(1).toFloat()
        }
        val range = maxP - minP
        if (range <= 0f) return null
        val r = range / div.toFloat()
        val totalWidthBars = (slice.size - 1).coerceAtLeast(1)

        // Bin walk state (mirrors Pine's `bin` UDT)
        class Bin(var lvl: Float, var prev: Float) {
            var sum = 0f
            var prevSum = 0f
            var avgAcc = 0f
            var csumAcc = 0f
            var isreached = false
            val columns = mutableListOf<SdVrColumn>()
        }

        val supply = Bin(maxP, maxP)
        val demand = Bin(minP, minP)

        var supplyFound = false
        var demandFound = false
        var supplyTop = maxP; var supplyBtm = maxP
        var demandTop = minP; var demandBtm = minP
        var supplyAvg = 0f; var demandAvg = 0f
        var supplyWavgVal = Float.NaN
        var demandWavgVal = Float.NaN

        for (i in 0 until div) {
            supply.lvl -= r
            demand.lvl += r

            // Accumulated volume column drawn BEFORE this interval accumulates,
            // exactly like the Pine box.new placement inside the interval loop.
            if (!supply.isreached && supply.sum > 0f) {
                supply.columns += SdVrColumn(supply.prev, supply.lvl, (supply.sum / csum * totalWidthBars).toInt())
            }
            if (!demand.isreached && demand.sum > 0f) {
                demand.columns += SdVrColumn(demand.lvl, demand.prev, (demand.sum / csum * totalWidthBars).toInt())
            }

            // Loop through bars (each chart bar acts as one intrabar sample)
            for (j in 0 until slice.size - 1) {
                val h = slice[j].high
                val l = slice[j].low
                val v = if (useCounts) 1f else slice[j].volume

                if (h > supply.lvl && h < supply.prev) {
                    supply.sum += v
                    supply.avgAcc += supply.lvl * (supply.sum - supply.prevSum)
                    supply.csumAcc += supply.sum - supply.prevSum
                    supply.prevSum = supply.sum
                }
                if (l < demand.lvl && l > demand.prev) {
                    demand.sum += v
                    demand.avgAcc += demand.lvl * (demand.sum - demand.prevSum)
                    demand.csumAcc += demand.sum - demand.prevSum
                    demand.prevSum = demand.sum
                }

                if (csum > 0f && !supply.isreached && supply.sum / csum * 100f > per) {
                    val avg = (maxP + supply.lvl) / 2f
                    supplyWavgVal = if (supply.csumAcc != 0f) supply.avgAcc / supply.csumAcc else avg
                    supplyTop = maxP
                    supplyBtm = supply.lvl
                    supplyAvg = avg
                    supply.isreached = true
                    supplyFound = true
                }
                if (csum > 0f && !demand.isreached && demand.sum / csum * 100f > per) {
                    val avg = (minP + demand.lvl) / 2f
                    demandWavgVal = if (demand.csumAcc != 0f) demand.avgAcc / demand.csumAcc else avg
                    demandTop = demand.lvl
                    demandBtm = minP
                    demandAvg = avg
                    demand.isreached = true
                    demandFound = true
                }
                if (supply.isreached && demand.isreached) break
            }

            if (supply.isreached && demand.isreached) break
            supply.prev = supply.lvl
            demand.prev = demand.lvl
        }

        if (!supplyFound && !demandFound) return null

        // Equilibrium only when BOTH sides were found (matches the Pine guard)
        val equiAvg = (maxP + minP) / 2f
        val sW = if (supplyWavgVal.isNaN()) supplyAvg else supplyWavgVal
        val dW = if (demandWavgVal.isNaN()) demandAvg else demandWavgVal
        val equiWavg = (sW + dW) / 2f

        return SdVrResult(
            x1Index = 0,
            supply = SdVrSide(supplyFound, supplyTop, supplyBtm, supplyAvg, sW, supply.columns),
            demand = SdVrSide(demandFound, demandTop, demandBtm, demandAvg, dW, demand.columns),
            equiAvg = equiAvg,
            equiWavg = equiWavg
        )
    }
}
