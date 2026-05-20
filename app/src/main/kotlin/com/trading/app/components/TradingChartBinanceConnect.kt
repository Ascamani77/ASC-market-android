package com.trading.app.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.trading.app.data.BinanceMarketType
import com.trading.app.data.BinanceConnectChartService
import com.trading.app.data.ChartFeedType
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.OHLCData

/**
 * Independent Binance Connect Chart Component
 * Uses public Binance WebSocket for real-time data
 * Completely independent from other chart implementations
 */
@Composable
fun TradingChartBinanceConnect(
    symbol: String,
    timeframe: String,
    content: @Composable (ChartFeedType, BinanceMarketType, ProviderChartData) -> Unit
) {
    key(ChartFeedType.BINANCE_CONNECT, symbol, timeframe) {
        var candles by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
        var quote by remember { mutableStateOf<SymbolQuote?>(null) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var hasMoreHistory by remember { mutableStateOf(true) }

        val service = remember {
            BinanceConnectChartService(
                onQuoteUpdate = { incomingQuote ->
                    quote = providerDisplayQuote(incomingQuote, symbol, candles)
                },
                onHistoryUpdate = { receivedSymbol, history ->
                    if (history.isEmpty()) {
                        isLoadingMore = false
                        hasMoreHistory = false
                        return@BinanceConnectChartService
                    }
                    if (!isLoadingMore && history.size == 1 && candles.isEmpty()) {
                        isLoadingMore = false
                        return@BinanceConnectChartService
                    }
                    val merged = if (!isLoadingMore && history.size == 1 && candles.isNotEmpty()) {
                        (candles + history)
                            .distinctBy(OHLCData::time)
                            .sortedBy(OHLCData::time)
                            .takeLast(10000)
                    } else {
                        mergeProviderHistory(candles, history, isLoadingMore)
                    }
                    candles = merged
                    isLoadingMore = false
                    if (history.size < 500 && history.size != 1) hasMoreHistory = false
                    Log.d("TradingChartBinanceConnect", "Loaded ${history.size} candles for $receivedSymbol")
                }
            )
        }

        LaunchedEffect(symbol, timeframe) {
            val streamSymbol = chartFeedSymbolFor(ChartFeedType.BINANCE_CONNECT, symbol)
            candles = emptyList()
            quote = null
            isLoadingMore = false
            hasMoreHistory = true
            service.stopActiveStream()
            service.streamActiveSymbol(streamSymbol, timeframe)
            service.fetchHistory(streamSymbol, timeframe, null)
        }

        DisposableEffect(service) {
            onDispose {
                service.disconnect()
            }
        }

        content(
            ChartFeedType.BINANCE_CONNECT,
            BinanceMarketType.SPOT,
            ProviderChartData(
                candles = candles,
                quote = quote,
                isLoadingMore = isLoadingMore,
                hasMoreHistory = hasMoreHistory,
                onLoadMoreHistory = { endTime ->
                    if (!isLoadingMore && hasMoreHistory && candles.isNotEmpty()) {
                        isLoadingMore = true
                        val streamSymbol = chartFeedSymbolFor(ChartFeedType.BINANCE_CONNECT, symbol)
                        service.fetchHistory(streamSymbol, timeframe, endTime)
                    }
                }
            )
        )
    }
}
