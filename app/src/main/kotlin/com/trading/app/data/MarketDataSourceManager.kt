package com.trading.app.data

import android.util.Log
import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData

/**
 * MarketDataSourceManager - Manages multiple data sources with priority fallback
 * 
 * Priority order:
 * 1. Deriv - For BTC/USD, ETH/USD, and commodities (Gold, Silver, Oil)
 * 2. Binance - For crypto pairs ending in USDT
 * 3. MT5 - For forex and other instruments
 * 
 * This ensures we use the most reliable and low-latency source for each asset class
 */
class MarketDataSourceManager(
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val derivService = DerivService(
        onQuoteUpdate = { quote ->
            Log.d(TAG, "Deriv quote: ${quote.name} = ${quote.lastPrice}")
            onQuoteUpdate(quote)
        },
        onHistoryUpdate = { symbol, history ->
            Log.d(TAG, "Deriv history: $symbol (${history.size} candles)")
            onHistoryUpdate(symbol, history)
        }
    )
    
    private var binanceService: BinanceService? = null
    private var mt5Service: Mt5Service? = null
    
    companion object {
        private const val TAG = "MarketDataSourceManager"
        
        // Symbols supported by Deriv
        private val DERIV_SYMBOLS = setOf(
            "BTCUSD", "BTC/USD", "BTCUSDT",
            "ETHUSD", "ETH/USD", "ETHUSDT",
            "XAUUSD", "XAU/USD", "GOLD",  // Gold
            "XAGUSD", "XAG/USD", "SILVER", // Silver
            "BROUSD", "BRO/USD", "BRENT",  // Brent Crude
            "WTIUSD", "WTI/USD", "CRUDE"   // WTI Crude
        )
    }
    
    /**
     * Set the Binance service instance
     */
    fun setBinanceService(service: BinanceService) {
        this.binanceService = service
    }
    
    /**
     * Set the MT5 service instance
     */
    fun setMt5Service(service: Mt5Service) {
        this.mt5Service = service
    }
    
    /**
     * Determine which data source should be used for a symbol
     */
    fun getDataSourceForSymbol(symbol: String): DataSource {
        val normalized = normalizeSymbol(symbol)
        
        // Check if Deriv supports this symbol
        if (DERIV_SYMBOLS.any { normalizeSymbol(it) == normalized }) {
            return DataSource.DERIV
        }
        
        // Check if it's a Binance crypto pair
        if (normalized.endsWith("USDT")) {
            return DataSource.BINANCE
        }
        
        // Default to MT5 for forex and other instruments
        return DataSource.MT5
    }
    
    /**
     * Subscribe to a symbol using the appropriate data source
     */
    fun subscribe(symbol: String) {
        when (getDataSourceForSymbol(symbol)) {
            DataSource.DERIV -> {
                Log.i(TAG, "Subscribing to $symbol via Deriv")
                derivService.subscribe(symbol)
            }
            DataSource.BINANCE -> {
                Log.i(TAG, "Subscribing to $symbol via Binance")
                binanceService?.subscribe(symbol)
            }
            DataSource.MT5 -> {
                Log.i(TAG, "Subscribing to $symbol via MT5")
                // MT5 subscription handled separately
            }
        }
    }
    
    /**
     * Stream a single active symbol
     */
    fun streamActiveSymbol(symbol: String) {
        when (getDataSourceForSymbol(symbol)) {
            DataSource.DERIV -> {
                Log.i(TAG, "Streaming $symbol via Deriv")
                derivService.streamActiveSymbol(symbol)
            }
            DataSource.BINANCE -> {
                Log.i(TAG, "Streaming $symbol via Binance")
                binanceService?.streamActiveSymbol(symbol)
            }
            DataSource.MT5 -> {
                Log.i(TAG, "Streaming $symbol via MT5")
                // MT5 streaming handled separately
            }
        }
    }
    
    /**
     * Fetch historical data for a symbol
     */
    fun fetchHistory(symbol: String, timeframe: String, endTime: Long? = null) {
        when (getDataSourceForSymbol(symbol)) {
            DataSource.DERIV -> {
                Log.i(TAG, "Fetching history for $symbol via Deriv")
                derivService.fetchHistory(symbol, timeframe, endTime)
            }
            DataSource.BINANCE -> {
                Log.i(TAG, "Fetching history for $symbol via Binance")
                binanceService?.fetchHistory(symbol, timeframe, endTime)
            }
            DataSource.MT5 -> {
                Log.i(TAG, "Fetching history for $symbol via MT5")
                // MT5 history handled separately
            }
        }
    }
    
    /**
     * Stop streaming the active symbol
     */
    fun stopActiveStream() {
        derivService.stopActiveStream()
        binanceService?.stopActiveStream()
    }
    
    /**
     * Connect to Deriv (other services connect on demand)
     */
    fun connectDeriv() {
        derivService.connect()
    }
    
    /**
     * Disconnect all services
     */
    fun disconnectAll() {
        derivService.disconnect()
        binanceService?.disconnect()
        mt5Service?.disconnect()
    }
    
    /**
     * Get connection status for Deriv
     */
    fun isDerivConnected(): Boolean = derivService.isConnected()
    
    private fun normalizeSymbol(symbol: String): String {
        return symbol
            .uppercase()
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
            .removeSuffix(".M")
            .removeSuffix(".PRO")
            .removeSuffix("M")
    }
    
    enum class DataSource {
        DERIV,
        BINANCE,
        MT5
    }
}
