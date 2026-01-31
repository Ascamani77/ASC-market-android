package com.asc.markets.core

import com.asc.markets.data.ForexDataPoint
import kotlin.math.abs

/**
 * Port: supplyDemandEngine.ts
 * ERC (Extended Range Candle) detection parity.
 */
object SupplyDemandEngine {
    fun findZones(data: List<ForexDataPoint>): List<Zone> {
        val candles = data.takeLast(60)
        val zones = mutableListOf<Zone>()
        if (candles.size < 6) return zones

        for (i in 5 until candles.size - 1) {
            val prev = candles[i - 1]
            val curr = candles[i]
            val bodySize = abs(curr.close - curr.open)
            val avgBodySize = candles.subList(i - 5, i).map { abs(it.close - it.open) }.average()

            if (bodySize > avgBodySize * 2.5) {
                val type = if (curr.close > curr.open) "DEMAND" else "SUPPLY"
                zones.add(Zone(type, prev.low, prev.high, "FRESH"))
            }
        }
        return zones.takeLast(4)
    }
}

/**
 * Port: supportResistanceEngine.ts
 * Implements the 50-bin clustering algorithm for institucional level detection.
 */
object SupportResistanceEngine {
    fun findLevels(data: List<ForexDataPoint>): List<KeyLevel> {
        val candles = data.takeLast(100)
        val prices = candles.flatMap { listOf(it.high, it.low) }
        val currentPrice = candles.last().close
        
        val min = prices.minOrNull() ?: 0.0
        val max = prices.maxOrNull() ?: 1.0
        val binSize = (max - min) / 50.0
        
        val bins = prices.groupBy { (it / binSize).toInt() * binSize }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        return bins.map { (price, count) ->
            KeyLevel(
                type = if (price > currentPrice) "RESISTANCE" else "SUPPORT",
                price = price,
                strength = when { count > 10 -> "HIGH"; count > 5 -> "MEDIUM"; else -> "LOW" }
            )
        }
    }
}

/**
 * Port: volatilityEngine.ts
 * Efficiency Ratio (ER) and ATR variance parity.
 */
object VolatilityEngine {
    fun analyze(data: List<ForexDataPoint>): TrendCondition {
        val candles = data.takeLast(20)
        if (candles.isEmpty()) return TrendCondition("WEAK", "NORMAL")

        val ranges = candles.map { it.high - it.low }
        val avgRange = ranges.average()
        val lastRange = ranges.last()

        val volatility = when {
            lastRange > avgRange * 1.5 -> "HIGH"
            lastRange < avgRange * 0.5 -> "LOW"
            else -> "NORMAL"
        }

        val netMove = abs(candles.last().close - candles.first().close)
        val totalMove = ranges.sum()
        val efficiencyRatio = if (totalMove != 0.0) netMove / totalMove else 0.0

        return TrendCondition(
            strength = if (efficiencyRatio > 0.3) "STRONG" else "WEAK",
            volatility = volatility
        )
    }
}

data class Zone(val type: String, val low: Double, val high: Double, val freshness: String)
data class KeyLevel(val type: String, val price: Double, val strength: String)
data class TrendCondition(val strength: String, val volatility: String)