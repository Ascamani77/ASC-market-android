package com.asc.markets.logic

import com.asc.markets.data.ForexDataPoint

data class MarketState(
    val symbol: String,
    val chartData: List<ForexDataPoint>,
    val technicalBias: String, // "BULLISH" | "BEARISH" | "NEUTRAL"
    val safetyBlocked: Boolean,
    val confidence: Int // 0-100
)
