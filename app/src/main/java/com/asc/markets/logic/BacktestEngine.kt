package com.asc.markets.logic

import com.trading.app.indicators.PremiumDiscountData
import com.trading.app.indicators.PremiumDiscountIndicator
import com.trading.app.indicators.SmcZoneAlerts
import com.trading.app.indicators.SmcZoneSnapshot
import com.trading.app.models.OHLCData

/**
 * Historical SMC backtest over the same zone detection the stream chart uses.
 *
 * No look-ahead: at bar `i` the zone snapshot is built ONLY from bars [0..i],
 * so a signal uses nothing that happens after the bar it fires on. Entry is
 * taken at bar `i` close, exit is decided strictly by later bars.
 *
 * Direction bias per zone (mirrors the live SIM page):
 *  - FVG: bullish FVG -> LONG, bearish FVG -> SHORT
 *  - SD:  demand zone  -> LONG, supply zone -> SHORT
 *  - OTE: bull OTE box -> LONG, bear OTE box -> SHORT
 *  - PD:  premium box  -> SHORT (sell the premium), discount box -> LONG
 *  - OB / CFVG: level zones, neutral (no directional bias)
 */
data class BacktestTrade(
    val entryTime: Long,
    val exitTime: Long,
    val entry: Double,
    val exit: Double,
    val direction: String,
    val sl: Double,
    val tp: Double,
    val zone: String,
    val pnl: Double,
    val won: Boolean
)

data class BacktestResult(
    val trades: List<BacktestTrade> = emptyList(),
    val barsAnalyzed: Int = 0,
    val windowsStartTime: Long = 0L,
    val windowsEndTime: Long = 0L,
    val error: String? = null
) {
    val totalPnl: Double get() = trades.sumOf { it.pnl }
    val wins: Int get() = trades.count { it.won }
    val losses: Int get() = trades.size - wins
    val winRate: Double get() = if (trades.isEmpty()) 0.0 else wins.toDouble() / trades.size
    val profitFactor: Double get() {
        val grossWin = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val grossLoss = trades.filter { it.pnl < 0 }.sumOf { -it.pnl }
        return when {
            grossLoss <= 0.0 -> if (grossWin > 0) Double.MAX_VALUE else 0.0
            else -> grossWin / grossLoss
        }
    }
    val maxDrawdownPct: Double get() {
        var equity = 0.0
        var peak = Double.MIN_VALUE
        var dd = 0.0
        trades.forEach {
            equity += it.pnl
            peak = maxOf(peak, equity)
            if (peak > 0) dd = maxOf(dd, (peak - equity) / peak)
        }
        return dd
    }
    val zoneBreakdown: Map<String, Int> get() = trades.groupingBy { it.zone }.eachCount()
}

object BacktestEngine {

    private const val WARMUP = 200

    /**
     * @param candles ascending OHLC bars
     * @param tfSec timeframe in seconds (chart-timeframe for convergence grouping)
     * @param selectedSmcZones empty = any SMC zone triggers; else only these zones
     * @param direction "LONG"/"SHORT"/null. null = AUTO (bias derived from the zone)
     * @param slPct stop loss as fraction of entry (e.g. 0.005)
     * @param tpPct take profit as fraction of entry (e.g. 0.01)
     */
    fun run(
        candles: List<OHLCData>,
        tfSec: Long,
        selectedSmcZones: Set<String>,
        direction: String?,
        slPct: Double,
        tpPct: Double,
        size: Double = 1.0
    ): BacktestResult {
        if (candles.size < 4) return BacktestResult(error = "No candle data available.")
        val data = candles.sortedBy { it.time }
        val n = data.size
        val start = if (n > WARMUP + 3) WARMUP else 4
        if (n <= start + 3) return BacktestResult(error = "Backtest window too small (${n} bars) \u2014 need ${WARMUP + 4}+.")

        val pdIndicator = PremiumDiscountIndicator(50, 200)
        val trades = mutableListOf<BacktestTrade>()

        var i = start
        while (i < n - 3) {
            val window = data.subList(0, i + 1)
            val price = data[i].close.toDouble()

            val snap = runCatching { SmcZoneAlerts.snapshot(window, tfSec) }.getOrDefault(null)
                ?: run { i++; continue }
            val pd = runCatching { pdIndicator.calculatePremiumDiscount(window) }.getOrDefault(null)

            // zones active at this bar's close, plus each zone's directional bias
            val active = zoneBiasMap(snap, pd, price)
            val candidates = if (selectedSmcZones.isEmpty()) active
            else active.filterKeys { it in selectedSmcZones }
            if (candidates.isEmpty()) { i++; continue }

            // resolve direction
            val biases = candidates.values.filterNotNull().distinct()
            val tradeDir = when {
                !direction.isNullOrBlank() && biases.size > 1 -> null // conflicting SMC on bar
                !direction.isNullOrBlank() -> direction
                biases.size == 1 -> biases.first()
                else -> null // AUTO needs a biased zone
            }
            if (tradeDir == null) { i++; continue }

            val dirMult = if (tradeDir == "LONG") 1.0 else -1.0
            val sl = if (tradeDir == "LONG") price * (1 - slPct) else price * (1 + slPct)
            val tp = if (tradeDir == "LONG") price * (1 + tpPct) else price * (1 - tpPct)

            val zone = candidates.entries.firstOrNull { it.value == tradeDir }?.key
                ?: candidates.keys.first()

            // exit on first later bar that touches SL/TP (both in same bar -> SL, conservative)
            var exitPrice: Double? = null
            var exitTime: Long = 0L
            var won = false
            var j = i + 1
            while (j < n) {
                val bar = data[j]
                if (tradeDir == "LONG") {
                    if (bar.low.toDouble() <= sl) { exitPrice = sl; won = false }
                    else if (bar.high.toDouble() >= tp) { exitPrice = tp; won = true }
                } else {
                    if (bar.high.toDouble() >= sl) { exitPrice = sl; won = false }
                    else if (bar.low.toDouble() <= tp) { exitPrice = tp; won = true }
                }
                if (exitPrice != null) { exitTime = bar.time; break }
                j++
            }
            if (exitPrice == null) {
                exitPrice = data[n - 1].close.toDouble()
                exitTime = data[n - 1].time
                won = (exitPrice - price) * dirMult >= 0
            }

            trades.add(
                BacktestTrade(
                    entryTime = data[i].time,
                    exitTime = exitTime,
                    entry = price,
                    exit = exitPrice,
                    direction = tradeDir,
                    sl = sl,
                    tp = tp,
                    zone = zone,
                    pnl = (exitPrice - price) * dirMult * size,
                    won = won
                )
            )

            // trades do not overlap: resume scanning after the exit bar
            i = j
        }

        return BacktestResult(
            trades = trades,
            barsAnalyzed = n,
            windowsStartTime = data.first().time,
            windowsEndTime = data.last().time,
            error = if (trades.isEmpty()) "No SMC setups found in ${n} bars for the selected filters." else null
        )
    }

    /** Returns the SMC zones the CURRENT [price] actually sits inside, with each zone's
     *  directional bias (null = neutral level zone such as OB/CFVG). Used by the live SIM
     *  page so a selected pattern only counts when the price really touches that zone. */
    fun zonesAtPrice(candles: List<OHLCData>, tfSec: Long, price: Double): Map<String, String?> {
        if (candles.size < 4 || price <= 0) return emptyMap()
        val snap = runCatching { SmcZoneAlerts.snapshot(candles, tfSec) }.getOrDefault(null)
            ?: return emptyMap()
        val pd = runCatching { PremiumDiscountIndicator(50, 200).calculatePremiumDiscount(candles) }.getOrDefault(null)
        return zoneBiasMap(snap, pd, price)
    }

    /** Returns the zones containing [price] with their directional bias (null = neutral zone). */
    private fun zoneBiasMap(snap: SmcZoneSnapshot, pd: PremiumDiscountData?, price: Double): Map<String, String?> {
        val out = mutableMapOf<String, String?>()
        val buf = maxOf(price * 0.0001, 0.00005)

        // FVG — bias from touched records
        val fvgs = snap.fvgs.filter {
            price >= minOf(it.min, it.max).toDouble() - buf && price <= maxOf(it.min, it.max).toDouble() + buf
        }
        if (fvgs.isNotEmpty()) {
            out["FVG"] = when {
                fvgs.any { it.isBull } && fvgs.none { !it.isBull } -> "LONG"
                fvgs.any { !it.isBull } && fvgs.none { it.isBull } -> "SHORT"
                else -> null
            }
        }

        // SD — supply/demand sides
        val sd = snap.sdResult
        if (sd != null) {
            val supplyTouch = sd.supply.found &&
                price >= minOf(sd.supply.top, sd.supply.bottom).toDouble() - buf &&
                price <= maxOf(sd.supply.top, sd.supply.bottom).toDouble() + buf
            val demandTouch = sd.demand.found &&
                price >= minOf(sd.demand.top, sd.demand.bottom).toDouble() - buf &&
                price <= maxOf(sd.demand.top, sd.demand.bottom).toDouble() + buf
            if (supplyTouch || demandTouch) {
                out["SD"] = when {
                    supplyTouch && demandTouch -> null
                    supplyTouch -> "SHORT"
                    else -> "LONG"
                }
            }
        }

        // OTE box
        val ote = snap.ote
        if (ote != null) {
            val hi = maxOf(ote.boxTop, ote.boxBottom).toDouble()
            val lo = minOf(ote.boxTop, ote.boxBottom).toDouble()
            if (price >= lo - buf && price <= hi + buf) {
                out["OTE"] = if (ote.isBull) "LONG" else "SHORT"
            }
        }

        // OB (neutral) — opposite-candle zones from the snapshot
        if (snap.orderBlocks.any {
                price >= minOf(it.first, it.second).toDouble() - buf && price <= maxOf(it.first, it.second).toDouble() + buf
            }
        ) {
            out["OB"] = null
        }

        // CFVG (neutral)
        if (snap.confluenceZones.any {
                price >= minOf(it.bot, it.top).toDouble() - buf && price <= maxOf(it.bot, it.top).toDouble() + buf
            }
        ) {
            out["CFVG"] = null
        }

        // PD — price inside an SR premium (upper) or discount (lower) box
        if (pd != null) {
            val upperTop = maxOf(pd.srUpperTop, pd.srUpperBottom).toDouble()
            val upperBot = minOf(pd.srUpperTop, pd.srUpperBottom).toDouble()
            val lowerTop = maxOf(pd.srLowerTop, pd.srLowerBottom).toDouble()
            val lowerBot = minOf(pd.srLowerTop, pd.srLowerBottom).toDouble()
            val inPremium = price in upperBot..upperTop
            val inDiscount = price in lowerBot..lowerTop
            if (inPremium || inDiscount) {
                out["PD"] = when {
                    inPremium && inDiscount -> null
                    inPremium -> "SHORT"
                    else -> "LONG"
                }
            }
        }

        return out
    }
}