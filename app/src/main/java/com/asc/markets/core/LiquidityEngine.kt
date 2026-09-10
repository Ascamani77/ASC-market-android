package com.asc.markets.core

object LiquidityEngine {
    fun detectSweep(data: List<Double>): Boolean {
        // Real sweep detection logic: check if price swept through recent high/low
        if (data.size < 5) return false
        
        val recentData = data.takeLast(5)
        val high = recentData.maxOrNull() ?: return false
        val low = recentData.minOrNull() ?: return false
        val current = data.last()
        
        // Detect sweep: price moved through a level and reversed
        // A sweep occurs when price briefly trades beyond a key level then reverses
        val range = high - low
        if (range == 0.0) return false
        
        // Check if current price is near the opposite extreme after touching the other
        val touchedHigh = recentData.any { it >= high * 0.999 }
        val touchedLow = recentData.any { it <= low * 1.001 }
        
        // If touched high and now near low, or touched low and now near high
        if (touchedHigh && current <= low + range * 0.1) return true
        if (touchedLow && current >= high - range * 0.1) return true
        
        return false
    }
    
    fun findPools(data: List<Double>): List<Double> {
        // Find equal highs/lows (liquidity pools)
        if (data.size < 10) return emptyList()
        
        val pools = mutableListOf<Double>()
        val tolerance = 0.001 // 0.1% tolerance for equal levels
        
        // Find levels that appear multiple times
        val levelCounts = data.groupBy { 
            (it / tolerance).toInt() * tolerance 
        }.filter { it.value.size >= 2 }
        
        levelCounts.forEach { (level, occurrences) ->
            if (occurrences.size >= 2) {
                pools.add(level)
            }
        }
        
        return pools.take(5) // Return top 5 liquidity pools
    }
}
