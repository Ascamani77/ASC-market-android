package com.asc.markets.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized price broadcast system. All screens subscribe to price changes here.
 * When a price updates on the Tape, it instantly propagates to Dashboard, Chart, and all other listeners.
 */
object PriceStreamManager {
    // Map of pair symbol -> current price
    private val _priceUpdates = MutableStateFlow<Map<String, Double>>(
        mapOf(
            "EUR/USD" to 1.0945,
            "GBP/USD" to 1.2732,
            "DXY" to 104.35,
            "BTC/USD" to 42850.00
        )
    )

    // Public read-only access to price stream
    val priceUpdates: StateFlow<Map<String, Double>> = _priceUpdates.asStateFlow()

    /**
     * Broadcast a price update for a specific pair.
     * All subscribers (Dashboard, Chart, Tape, etc.) will receive this update instantly.
     */
    fun updatePrice(pair: String, newPrice: Double) {
        val current = _priceUpdates.value.toMutableMap()
        current[pair] = newPrice
        _priceUpdates.value = current
    }

    /**
     * Batch update multiple prices at once.
     */
    fun updatePrices(updates: Map<String, Double>) {
        val current = _priceUpdates.value.toMutableMap()
        current.putAll(updates)
        _priceUpdates.value = current
    }

    /**
     * Get current price for a pair (synchronous access)
     */
    fun getPrice(pair: String): Double? = _priceUpdates.value[pair]
}
