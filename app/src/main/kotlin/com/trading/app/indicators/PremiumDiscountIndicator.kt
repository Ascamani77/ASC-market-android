package com.trading.app.indicators

import com.trading.app.models.OHLCData
import kotlin.math.max
import kotlin.math.min

/**
 * Port of BigBeluga "Premium & Discount Delta Volume" v6.
 * Licensed CC BY-NC-SA 4.0 - BigBeluga
 * @version 6 - indicator('Premium & Discount Delta Volume [BigBeluga]', overlay=true)
 */
data class PremiumDiscountData(
    val srUpperTop: Float,
    val srUpperBottom: Float,
    val srLowerTop: Float,
    val srLowerBottom: Float,
    val macroUpperTop: Float,
    val macroUpperBottom: Float,
    val macroLowerTop: Float,
    val macroLowerBottom: Float,
    val equilibrium: Float,
    val macroEquilibrium: Float,
    val deltaVolSR: Float,
    val deltaVolMacro: Float,
    val posVolSRSum: Float,
    val negVolSRSum: Float,
    val posVolMacroSum: Float,
    val negVolMacroSum: Float,
    val atr: Float
)

class PremiumDiscountIndicator(
    private val srPeriod: Int = 50,
    private val macroPeriod: Int = 200,
    private val atrPeriod: Int = 200,
    private val atrMult: Float = 0.8f
) : TradingIndicator {
    override val id = "PREMIUM_DISCOUNT"
    override val name = "Premium & Discount Delta Volume [BigBeluga]"
    override val color = android.graphics.Color.parseColor("#79c1f1")

    override fun calculate(candles: List<OHLCData>): List<Float?> = List(candles.size) { null }

    fun calculatePremiumDiscount(candles: List<OHLCData>): PremiumDiscountData? {
        if (candles.size < 2) return null
        val srCount = min(srPeriod, candles.size)
        val macroCount = min(macroPeriod, candles.size)
        val srSlice = candles.takeLast(srCount)
        val macroSlice = candles.takeLast(macroCount)

        // SR highs/lows
        val srHighs = srSlice.map { it.high }
        val srLows = srSlice.map { it.low }
        val macroHighs = macroSlice.map { it.high }
        val macroLows = macroSlice.map { it.low }

        // ATR 200 *0.8
        val atr = AtrIndicator(atrPeriod).calculate(candles).lastOrNull { it != null } ?: 0f
        val atrValue = atr * atrMult

        // Delta volume: pos where close>open => volume, neg where close<open => -volume
        fun deltaFor(slice: List<OHLCData>): Triple<Float, Float, Float> {
            val pos = mutableListOf<Float>()
            val neg = mutableListOf<Float>()
            var posSum = 0f
            var negSum = 0f
            for (c in slice) {
                if (c.close > c.open) {
                    pos.add(c.volume); posSum += c.volume
                    neg.add(0f)
                } else if (c.close < c.open) {
                    neg.add(-c.volume); negSum += -c.volume // negative
                    pos.add(0f)
                } else {
                    pos.add(0f); neg.add(0f)
                }
            }
            val posAvg = if (pos.isEmpty()) 0f else pos.average().toFloat()
            val negAvg = if (neg.isEmpty()) 0f else neg.average().toFloat()
            val delta = if (posAvg == 0f) 0f else ((negAvg / posAvg) + 1f) * 100f
            val capped = delta.coerceIn(-100f, 100f)
            return Triple(capped, posSum, negSum)
        }

        val (deltaSR, posSRSum, negSRSum) = deltaFor(srSlice)
        val (deltaMacro, posMacroSum, negMacroSum) = deltaFor(macroSlice)

        val srUpperTop = (srHighs.maxOrNull() ?: 0f) + atrValue
        val srUpperBottom = srHighs.maxOrNull() ?: 0f
        val srLowerTop = srLows.minOrNull() ?: 0f
        val srLowerBottom = srLowerTop - atrValue

        val macroUpperTop = (macroHighs.maxOrNull() ?: 0f) + atrValue
        val macroUpperBottom = macroHighs.maxOrNull() ?: 0f
        val macroLowerTop = macroLows.minOrNull() ?: 0f
        val macroLowerBottom = macroLowerTop - atrValue

        val equilibrium = (srLowerTop + srUpperBottom) / 2f
        val macroEquilibrium = (macroLowerTop + macroUpperBottom) / 2f

        return PremiumDiscountData(
            srUpperTop = srUpperTop,
            srUpperBottom = srUpperBottom,
            srLowerTop = srLowerTop,
            srLowerBottom = srLowerBottom,
            macroUpperTop = macroUpperTop,
            macroUpperBottom = macroUpperBottom,
            macroLowerTop = macroLowerTop,
            macroLowerBottom = macroLowerBottom,
            equilibrium = equilibrium,
            macroEquilibrium = macroEquilibrium,
            deltaVolSR = deltaSR,
            deltaVolMacro = deltaMacro,
            posVolSRSum = posSRSum,
            negVolSRSum = negSRSum,
            posVolMacroSum = posMacroSum,
            negVolMacroSum = negMacroSum,
            atr = atrValue
        )
    }
}
