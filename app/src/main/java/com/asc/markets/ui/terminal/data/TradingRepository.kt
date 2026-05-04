package com.asc.markets.ui.terminal.data

import com.asc.markets.data.MarketDataStore
import com.asc.markets.ui.terminal.models.Candle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class TradingRepository(private val webSocketManager: WebSocketManager = WebSocketManager()) {
    suspend fun getInitialCandles(symbol: String, timeframe: String): List<Candle> {
        // API call to fetch historical data
        return emptyList()
    }

    fun getLivePriceUpdates(symbol: String): Flow<Double> {
        return webSocketManager.priceUpdates(symbol)
    }
}

class WebSocketManager {
    fun priceUpdates(symbol: String): Flow<Double> = MarketDataStore
        .pairFlow(symbol)
        .map { it?.price }
        .filterNotNull()
        .distinctUntilChanged()
}
