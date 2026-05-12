package com.asc.markets.logic

import com.asc.markets.data.BinanceDataStore
import com.asc.markets.data.CombinedFallbackDataStore
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.SystemTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Centralized price broadcast system. All screens subscribe to price changes here.
 * When a price updates on the Tape, it instantly propagates to Dashboard, Chart, and all other listeners.
 */
object PriceStreamManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Map of pair symbol -> current price
    private val _priceUpdates = MutableStateFlow(
        priceMapFor(MarketDataStore.allPairs.value + BinanceDataStore.allPairs.value + CombinedFallbackDataStore.allPairs.value)
    )

    // Public read-only access to price stream
    val priceUpdates: StateFlow<Map<String, Double>> = _priceUpdates.asStateFlow()

    init {
        scope.launch {
            combine(
                MarketDataStore.allPairs,
                BinanceDataStore.allPairs,
                CombinedFallbackDataStore.allPairs
            ) { marketPairs, binancePairs, fallbackPairs ->
                marketPairs + binancePairs + fallbackPairs
            }.collect { pairs ->
                _priceUpdates.value = priceMapFor(pairs)
            }
        }
    }

    /**
     * Broadcast a price update for a specific pair.
     * All subscribers (Dashboard, Chart, Tape, etc.) will receive this update instantly.
     */
    fun updatePrice(pair: String, newPrice: Double) {
        val telemetrySource = when {
            pair.contains("USDT", ignoreCase = true) -> "BINANCE"
            pair.contains("BTC", ignoreCase = true) || pair.contains("ETH", ignoreCase = true) -> "CTRADER"
            else -> "MT5"
        }
        SystemTelemetry.recordTick(telemetrySource, 1.0)

        // Use pairSnapshot to find the canonical ForexPair even if symbol is "BTCUSD" vs "BTC/USDT"
        val currentPair = if (isUsdtSymbol(pair)) {
            BinanceDataStore.pairSnapshot(pair)
        } else {
            MarketDataStore.pairSnapshot(pair) ?: CombinedFallbackDataStore.pairSnapshot(pair)
        }
        
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
        
        _priceUpdates.value = _priceUpdates.value.toMutableMap().apply {
            put(currentPair.symbol, newPrice)
            put(currentPair.symbol.replace("/", ""), newPrice)
            put(pair, newPrice)
            put(pair.replace("/", ""), newPrice)
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
