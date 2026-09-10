package com.trading.app.indicators

import com.trading.app.models.OHLCData
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

/**
 * Port of LuxAlgo "Supply and Demand Daily [LuxAlgo]" v5.
 * Licensed CC BY-NC-SA 4.0 - LuxAlgo
 * indicator("Supply and Demand Daily [LuxAlgo]", overlay=true, max_boxes_count=500, max_lines_count=500)
 * Simplified intrabar TF: uses daily candles volume profile (high/low bins) as approximation for request.security_lower_tf.
 */
data class SupplyDemandDailyData(
    val supplyTop: Float,
    val supplyBottom: Float,
    val supplyAvg: Float,
    val supplyWavg: Float,
    val supplyFound: Boolean,
    val demandTop: Float,
    val demandBottom: Float,
    val demandAvg: Float,
    val demandWavg: Float,
    val demandFound: Boolean,
    val x1Index: Int
)

class SupplyDemandDailyIndicator(
    private val per: Float = 10f,
    private val div: Int = 50
) : TradingIndicator {
    override val id = "SUPPLY_DEMAND_DAILY_LUXALGO"
    override val name = "Supply and Demand Daily [LuxAlgo]"
    override val color = android.graphics.Color.parseColor("#2157f3")

    override fun calculate(candles: List<OHLCData>): List<Float?> = List(candles.size) { null }

    fun calculateSupplyDemand(candles: List<OHLCData>): SupplyDemandDailyData? {
        if (candles.size < 5) return null
        // Find start of current day (x1) - Pine: dayofmonth != dayofmonth[1] resets
        // Use last day boundary: find most recent index where day changes
        fun dayOf(timeSec: Long): Int {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = timeSec * 1000L
            return cal.get(Calendar.DAY_OF_MONTH) + cal.get(Calendar.MONTH) * 31 + cal.get(Calendar.YEAR) * 366
        }
        var x1 = 0
        for (i in candles.size - 1 downTo 1) {
            if (dayOf(candles[i].time) != dayOf(candles[i - 1].time)) {
                x1 = i
                break
            }
        }
        if (x1 <= 0) {
            // fallback: last 1 day approx (use last 24h if intraday, else last div bars)
            x1 = max(0, candles.size - 50)
        }

        // Accumulate max/min/csum from x1 to size-2 (Pine uses high[1], volume[1])
        var maxP = Float.MIN_VALUE
        var minP = Float.MAX_VALUE
        var csum = 0f
        for (j in x1 until candles.size - 1) {
            maxP = max(maxP, candles[j].high)
            minP = min(minP, candles[j].low)
            csum += candles[j].volume
        }
        if (maxP == Float.MIN_VALUE || minP == Float.MAX_VALUE) return null
        // Some bridges don't send volume for forex/indices - fall back to equal
        // bar weighting so zones still form (share-of-bars instead of share-of-volume)
        var useCounts = false
        if (csum == 0f) {
            useCounts = true
            csum = (x1 until candles.size - 1).count().coerceAtLeast(1).toFloat()
        }
        val range = maxP - minP
        if (range <= 0f) return null
        val r = range / div.toFloat()

        // Supply/demand walk
        data class Bin(var lvl: Float, var prev: Float, var sum: Float = 0f, var prevSum: Float = 0f, var csumAcc: Float = 0f, var avgAcc: Float = 0f, var isReached: Boolean = false)

        val supply = Bin(lvl = maxP, prev = maxP)
        val demand = Bin(lvl = minP, prev = minP)

        var supplyResult: SupplyDemandDailyData? = null
        var demandResult: SupplyDemandDailyData? = null
        var supplyWavgVal = Float.NaN
        var demandWavgVal = Float.NaN

        // Loop intervals
        var foundSupply = false
        var foundDemand = false
        var supplyTop = maxP; var supplyBtm = maxP
        var demandTop = minP; var demandBtm = minP
        var supplyAvg = 0f; var demandAvg = 0f

        for (i in 0 until div) {
            supply.lvl -= r
            demand.lvl += r

            // accumulate volume per bar where high/low within interval (approx intrabar)
            for (j in x1 until candles.size - 1) {
                val h = candles[j].high
                val l = candles[j].low
                val v = if (useCounts) 1f else candles[j].volume
                // supply: h > lvl && h < prev
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
            }

            if (!foundSupply && csum > 0f && supply.sum / csum * 100f > per) {
                val avg = (maxP + supply.lvl) / 2f
                supplyWavgVal = if (supply.csumAcc != 0f) supply.avgAcc / supply.csumAcc else avg
                supplyTop = maxP
                supplyBtm = supply.lvl
                supplyAvg = avg
                foundSupply = true
                supply.isReached = true
            }
            if (!foundDemand && csum > 0f && demand.sum / csum * 100f > per) {
                val avg = (minP + demand.lvl) / 2f
                demandWavgVal = if (demand.csumAcc != 0f) demand.avgAcc / demand.csumAcc else avg
                demandTop = demand.lvl
                demandBtm = minP
                demandAvg = avg
                foundDemand = true
                demand.isReached = true
            }
            if (foundSupply && foundDemand) break
            supply.prev = supply.lvl
            demand.prev = demand.lvl
        }

        if (!foundSupply && !foundDemand) return null
        return SupplyDemandDailyData(
            supplyTop = supplyTop,
            supplyBottom = supplyBtm,
            supplyAvg = supplyAvg,
            supplyWavg = if (supplyWavgVal.isNaN()) supplyAvg else supplyWavgVal,
            supplyFound = foundSupply,
            demandTop = demandTop,
            demandBottom = demandBtm,
            demandAvg = demandAvg,
            demandWavg = if (demandWavgVal.isNaN()) demandAvg else demandWavgVal,
            demandFound = foundDemand,
            x1Index = x1
        )
    }
}
