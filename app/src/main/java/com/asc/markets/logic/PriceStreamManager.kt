package com.asc.markets.logic

import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.SystemTelemetry
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

/**
 * Centralized price broadcast system. All screens subscribe to price changes here.
 * When a price updates on the Tape, it instantly propagates to Dashboard, Chart, and all other listeners.
 */
object PriceStreamManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Observable map for Compose - extremely efficient key-based updates
    private val _prices = mutableStateMapOf<String, Double>()
    val prices: Map<String, Double> = _prices

    // For non-Compose collectors if any (deprecated, prefer using prices map)
    private val _priceUpdates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val priceUpdates = _priceUpdates.asStateFlow()

    init {
        scope.launch {
            MarketDataStore.allPairs.collect { pairs ->
                pairs.forEach { pair ->
                    _prices[pair.symbol] = pair.price
                    _prices[pair.symbol.replace("/", "")] = pair.price
                }
                _priceUpdates.value = _prices.toMap()
            }
        }
    }

    /**
     * Broadcast a price update for a specific pair.
     */
    fun updatePrice(pair: String, newPrice: Double) {
        // Find canonical symbol
        val currentPair = MarketDataStore.pairSnapshot(pair)
        
        val canonicalSymbol = currentPair?.symbol ?: pair
        
        // Update the observable map - Compose will only recompose rows observing these keys
        _prices[canonicalSymbol] = newPrice
        _prices[canonicalSymbol.replace("/", "")] = newPrice
        _prices[pair] = newPrice
        _prices[pair.replace("/", "")] = newPrice
        
        // Background telemetry
        if (System.currentTimeMillis() % 100 == 0L) {
             _priceUpdates.value = _prices.toMap()
        }
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
    fun getPrice(pair: String): Double? = _priceUpdates.value[pair] ?: _priceUpdates.value[pair.replace("/", "")]

    private fun isUsdtSymbol(symbol: String): Boolean {
        return symbol.replace("/", "").uppercase().endsWith("USDT")
    }

    private fun priceMapFor(pairs: List<ForexPair>): Map<String, Double> {
        val prices = mutableMapOf<String, Double>()
        pairs.forEach { pair ->
            prices[pair.symbol] = pair.price
            prices[pair.symbol.replace("/", "")] = pair.price
        }
        return prices
    }
}
