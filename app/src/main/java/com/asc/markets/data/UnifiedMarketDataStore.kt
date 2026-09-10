package com.asc.markets.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Unified Market Data Store - Single source of truth for all market data.
 * Uses ONLY MT5 EA data, no fallback.
 */
object UnifiedMarketDataStore {
    private const val TAG = "UnifiedMarketData"
    
    // Primary data source state
    private val _dataSource = MutableStateFlow(DataSource.LOADING)
    val dataSource: StateFlow<DataSource> = _dataSource
    
    // Unified pairs list
    private val _allPairs = MutableStateFlow<List<ForexPair>>(emptyList())
    val allPairs: StateFlow<List<ForexPair>> = _allPairs
    
    // Price history (symbol -> prices)
    private val _priceHistory = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val priceHistory: StateFlow<Map<String, List<Double>>> = _priceHistory
    
    // Timed price history (symbol -> timed prices)
    private val _timedPriceHistory = MutableStateFlow<Map<String, List<TimedPrice>>>(emptyMap())
    val timedPriceHistory: StateFlow<Map<String, List<TimedPrice>>> = _timedPriceHistory
    
    init {
        // Monitor EA connection state with data - NO FALLBACK
        CoroutineScope(Dispatchers.Default).launch {
            combine(
                EALiveDataStore.isConnected,
                EALiveDataStore.liveAssets
            ) { eaConnected, eaAssets ->
                Pair(eaConnected, eaAssets)
            }.collect { (eaConnected, eaAssets) ->
                if (eaConnected && eaAssets.isNotEmpty()) {
                    _dataSource.value = DataSource.MT5_EA
                    updateFromEA()
                    Log.d(TAG, "✅ Using MT5 EA data: ${eaAssets.size} assets")
                } else {
                    _dataSource.value = DataSource.LOADING
                    clearData()
                    Log.d(TAG, "⚠️ EA disconnected - no data available")
                }
            }
        }
    }
    
    private fun updateFromEA() {
        // Convert EA assets to ForexPair format
        val pairs = EALiveDataStore.toForexPairs()
        _allPairs.value = pairs
        
        // Get price history from EA
        _priceHistory.value = EALiveDataStore.getPriceHistory()
        _timedPriceHistory.value = EALiveDataStore.getTimedPriceHistory()
    }
    
    private fun clearData() {
        // Clear all data when EA is disconnected
        _allPairs.value = emptyList()
        _priceHistory.value = emptyMap()
        _timedPriceHistory.value = emptyMap()
    }
    
    /**
     * Get live price for a specific symbol
     */
    fun getLivePrice(symbol: String): Double? {
        return _allPairs.value.find { it.symbol == symbol }?.price
    }
    
    /**
     * Get all symbols for a category
     */
    fun getSymbolsByCategory(category: MarketCategory): List<ForexPair> {
        return _allPairs.value.filter { it.category == category }
    }
}

enum class DataSource {
    LOADING,
    MT5_EA
}