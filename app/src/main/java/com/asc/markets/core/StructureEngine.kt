package com.asc.markets.core

data class StructureState(val bias: String, val event: String)

object StructureEngine {
    fun analyze(data: List<Double>): StructureState {
        if (data.size < 10) return StructureState("RANGE", "NONE")
        
        val last = data.last()
        val high = data.maxOrNull() ?: 0.0
        val low = data.minOrNull() ?: 0.0
        
        return when {
            last > high * 0.99 -> StructureState("BULLISH", "BOS")
            last < low * 1.01 -> StructureState("BEARISH", "CHoCH")
            else -> StructureState("RANGE", "CONSOLIDATION")
        }
    }
}