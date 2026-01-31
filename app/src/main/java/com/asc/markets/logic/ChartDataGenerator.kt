package com.asc.markets.logic

import com.asc.markets.data.ForexDataPoint
import kotlin.random.Random

fun generateMockChartData(symbol: String): List<ForexDataPoint> {
    val data = mutableListOf<ForexDataPoint>()
    var currentPrice = when {
        symbol.contains("BTC", ignoreCase = true) -> 67000.0
        symbol.contains("XAU", ignoreCase = true) -> 2300.0
        symbol.contains("EUR", ignoreCase = true) -> 1.0845
        else -> 100.0
    }

    val now = System.currentTimeMillis() / 1000L
    val fifteenMinsInSecs = (15 * 60).toLong()

    for (i in 500 downTo 0) {
        val timestamp = now - (i.toLong() * fifteenMinsInSecs)
        val open = currentPrice

        val volMult = if (symbol.contains("BTC", ignoreCase = true)) 0.015 else 0.003
        val volatility = currentPrice * volMult

        val close = open + (Random.nextDouble() - 0.5) * volatility
        val high = maxOf(open, close) + Random.nextDouble() * (volatility * 0.5)
        val low = minOf(open, close) - Random.nextDouble() * (volatility * 0.5)

        data.add(
            ForexDataPoint(
                timestamp = timestamp,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = Random.nextLong(1000, 6000).toDouble()
            )
        )

        currentPrice = close
    }

    return data
}
