package com.asc.markets.data

data class Trade(
    val id: String,
    val asset: String,
    val type: String, // "BUY" or "SELL"
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val profitLoss: Double,
    val timestamp: Long,
    val reasoning: String? = null
)
