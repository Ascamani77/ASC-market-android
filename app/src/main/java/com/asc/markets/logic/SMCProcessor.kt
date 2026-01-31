package com.asc.markets.logic

import com.asc.markets.data.ForexPair

data class MarketStructure(
    val bias: String,
    val lastEvent: String,
    val confluenceScore: Int
)

object SMCProcessor {
    
    fun analyzeFractalStructure(pair: String, priceHistory: List<Double>): MarketStructure {
        if (priceHistory.size < 10) return MarketStructure("NEUTRAL", "NONE", 0)

        val lastPrice = priceHistory.last()
        val prevHigh = priceHistory.dropLast(1).maxOrNull() ?: 0.0
        val prevLow = priceHistory.dropLast(1).minOrNull() ?: 0.0

        return when {
            lastPrice > prevHigh -> MarketStructure("BULLISH", "BOS", 85)
            lastPrice < prevLow -> MarketStructure("BEARISH", "CHoCH", 90)
            else -> MarketStructure("RANGE", "NONE", 40)
        }
    }
}