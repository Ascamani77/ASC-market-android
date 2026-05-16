package com.trading.app.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.trading.app.data.BinanceMarketType
import com.trading.app.data.BinanceTradingMode
import com.trading.app.data.BinanceChartService
import com.trading.app.data.ChartFeedType
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.OHLCData

@Composable
fun TradingChartBinance(
    symbol: String,
    timeframe: String,
    tradingMode: BinanceTradingMode,
    marketType: BinanceMarketType,
    content: @Composable (ChartFeedType, BinanceMarketType, ProviderChartData) -> Unit
) {
    key(ChartFeedType.BINANCE, symbol, timeframe, tradingMode, marketType) {
        var candles by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
        var quote by remember { mutableStateOf<SymbolQuote?>(null) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var hasMoreHistory by remember { mutableStateOf(true) }

        val service = remember(tradingMode, marketType) {
            BinanceChartService(
                tradingMode = tradingMode,
                marketType = marketType,
                onQuoteUpdate = { incomingQuote ->
                    quote = providerDisplayQuote(incomingQuote, symbol, candles)
                },
                onHistoryUpdate = { receivedSymbol, history ->
                    if (history.isEmpty()) {
                        isLoadingMore = false
                        hasMoreHistory = false
                        return@BinanceChartService
                    }
                    val merged = mergeProviderHistory(candles, history, isLoadingMore)
                    candles = merged
                    isLoadingMore = false
                    if (history.size < 500) hasMoreHistory = false
                    Log.d("TradingChartBinance", "Loaded ${history.size} candles for $receivedSymbol")
                }
            )
        }

        LaunchedEffect(symbol, timeframe, tradingMode, marketType) {
            val streamSymbol = chartFeedSymbolFor(ChartFeedType.BINANCE, symbol)
            candles = emptyList()
            quote = null
            isLoadingMore = false
            hasMoreHistory = true
            service.stopActiveStream()
            service.streamActiveSymbol(streamSymbol)
            service.fetchHistory(streamSymbol, timeframe, null)
        }

        DisposableEffect(service) {
            onDispose {
                service.disconnect()
            }
        }

        content(
            ChartFeedType.BINANCE,
            marketType,
            ProviderChartData(
                candles = candles,
                quote = quote,
                isLoadingMore = isLoadingMore,
                hasMoreHistory = hasMoreHistory,
                onLoadMoreHistory = { endTime ->
                    if (!isLoadingMore && hasMoreHistory) {
                        isLoadingMore = true
                        service.fetchHistory(chartFeedSymbolFor(ChartFeedType.BINANCE, symbol), timeframe, endTime)
                    }
                }
            )
        )
    }
}
