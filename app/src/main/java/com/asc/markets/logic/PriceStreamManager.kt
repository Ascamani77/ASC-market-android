package com.asc.markets.logic

import com.asc.markets.data.MarketDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized price broadcast system. All screens subscribe to price changes here.
 * When a price updates on the Tape, it instantly propagates to Dashboard, Chart, and all other listeners.
 */
object PriceStreamManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Map of pair symbol -> current price
    private val _priceUpdates = MutableStateFlow(
        MarketDataStore.allPairs.value.associate { pair -> pair.symbol to pair.price }
    )

    // Public read-only access to price stream
    val priceUpdates: StateFlow<Map<String, Double>> = _priceUpdates.asStateFlow()

    init {
        scope.launch {
            MarketDataStore.allPairs.collect { pairs ->
                _priceUpdates.value = pairs.associate { pair -> pair.symbol to pair.price }
            }
        }
    }

    /**
     * Broadcast a price update for a specific pair.
     * All subscribers (Dashboard, Chart, Tape, etc.) will receive this update instantly.
     */
    fun updatePrice(pair: String, newPrice: Double) {
        // Use pairSnapshot to find the canonical ForexPair even if symbol is "BTCUSD" vs "BTC/USDT"
        val currentPair = MarketDataStore.pairSnapshot(pair)
        
        if (currentPair == null) {
            android.util.Log.d("PriceStream", "No match found for incoming symbol: $pair")
            return
        }
        
        android.util.Log.v("PriceStream", "Updating ${currentPair.symbol} with price $newPrice (from $pair)")
        
        val prevPrice = currentPair.price
        val change = newPrice - prevPrice
        val changePercent = if (prevPrice != 0.0) {
            (change / prevPrice) * 100.0
        } else {
            0.0
        }
        
        // Update the canonical pair in the store
        MarketDataStore.updatePair(
            currentPair.copy(
                price = newPrice,
                change = change,
                changePercent = changePercent
            )
        )
    }

    /**
     * Batch update multiple prices at once.
     */
    fun updatePrices(updates: Map<String, Double>) {
        updates.forEach { (pair, price) ->
            updatePrice(pair, price)
        }
    }

    /**
     * Get current price for a pair (synchronous access)
     */
    fun getPrice(pair: String): Double? = _priceUpdates.value[pair]
}
