package com.asc.markets.core

object ConfluenceEngine {
    fun calculateScore(structure: String, liquidity: Boolean, volatility: String): Int {
        var points = 40
        if (structure != "RANGE") points += 20
        if (liquidity) points += 20
        if (volatility == "NORMAL") points += 20
        return points.coerceIn(0, 100)
    }
}