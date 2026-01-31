
package com.asc.markets.logic

import com.asc.markets.data.AutomatedTrade
import kotlin.math.roundToInt

object RiskEngine {
    /**
     * Institutional Exposure Logic
     * Ported from utils/calculateNetExposure.ts
     */
    fun calculateExposure(activeTrades: List<AutomatedTrade>): Map<String, Double> {
        val exposure = mutableMapOf<String, Double>()

        for (trade in activeTrades) {
            val symbol = trade.pair.replace("/", "").uppercase()
            if (symbol.length < 6) continue

            val base = symbol.substring(0, 3)
            val quote = symbol.substring(3, 6)
            
            val size = 1.0 // Standardized lot unit
            val isBuy = trade.side.uppercase() == "BUY"

            val baseMult = if (isBuy) 1.0 else -1.0
            val quoteMult = if (isBuy) -1.0 else 1.0

            exposure[base] = (exposure[base] ?: 0.0) + (size * baseMult)
            exposure[quote] = (exposure[quote] ?: 0.0) + (size * quoteMult)
        }

        return exposure.mapValues { (_, v) -> (v * 100.0).roundToInt() / 100.0 }
    }

    data class ExposureProfile(
        val netUSD: Double,
        val netDirection: String,
        val clusters: Map<String, Double>,
        val totalActiveLots: Int
    )

    /**
     * Port of calculateExposure from services/riskEngine.ts
     */
    fun calculateExposureProfile(activeTrades: List<AutomatedTrade>): ExposureProfile {
        var netUSD = 0.0
        var totalActiveLots = 0
        val clusters = mutableMapOf<String, Double>()

        val openTrades = activeTrades.filter { it.status.uppercase() == "OPEN" }

        for (trade in openTrades) {
            val isBuy = trade.side.uppercase() == "BUY"
            val multiplier = if (isBuy) 1.0 else -1.0

            val size = 1.0 // assume 1 lot if not provided
            totalActiveLots += size.toInt()

            val pair = trade.pair.uppercase()
            if (pair.contains("USD")) {
                if (pair.startsWith("USD")) netUSD += multiplier * size
                else netUSD -= multiplier * size
            }

            clusters[pair] = (clusters[pair] ?: 0.0) + (multiplier * size)
        }

        val netDirection = when {
            netUSD > 0.0 -> "LONG"
            netUSD < 0.0 -> "SHORT"
            else -> "FLAT"
        }

        return ExposureProfile(
            netUSD = netUSD,
            netDirection = netDirection,
            clusters = clusters,
            totalActiveLots = totalActiveLots
        )
    }
}
