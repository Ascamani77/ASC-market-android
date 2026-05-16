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
import androidx.compose.ui.platform.LocalContext
import com.asc.markets.data.NetworkConfig
import com.trading.app.data.ChartFeedType
import com.trading.app.data.PepperstoneChartService
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.OHLCData

@Composable
fun TradingChartPepperstone(
    symbol: String,
    timeframe: String,
    content: @Composable (ChartFeedType, ProviderChartData) -> Unit
) {
    key(ChartFeedType.PEPPERSTONE, symbol, timeframe) {
        val context = LocalContext.current
        val cTraderHost = remember { NetworkConfig.cTraderHost(context) }
        val cTraderPort = remember { NetworkConfig.cTraderPort(context) }
        var candles by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
        var quote by remember { mutableStateOf<SymbolQuote?>(null) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var hasMoreHistory by remember { mutableStateOf(true) }
        val currentSymbol by rememberUpdatedState(symbol)

        val service = remember(cTraderHost, cTraderPort) {
            PepperstoneChartService(
                host = cTraderHost,
                port = cTraderPort,
                onQuoteUpdate = { incomingQuote ->
                    if (providerSymbolsMatch(incomingQuote.name, currentSymbol)) {
                        quote = providerDisplayQuote(incomingQuote, currentSymbol, candles)
                    }
                },
                onHistoryUpdate = { receivedSymbol, history ->
                    if (providerSymbolsMatch(receivedSymbol, currentSymbol)) {
                        val merged = mergeProviderHistory(candles, history, isLoadingMore)
                        candles = merged
                        isLoadingMore = false
                        if (history.size < 500) hasMoreHistory = false
                        Log.d("TradingChartPepperstone", "Loaded ${history.size} candles for $receivedSymbol")
                    }
                }
            )
        }

        LaunchedEffect(symbol, timeframe, cTraderHost, cTraderPort) {
            val streamSymbol = chartFeedSymbolFor(ChartFeedType.PEPPERSTONE, symbol)
            candles = emptyList()
            quote = null
            isLoadingMore = false
            hasMoreHistory = true
            service.stopActiveStream()
            service.streamActiveSymbol(streamSymbol, timeframe, 500)
        }

        DisposableEffect(service) {
            onDispose {
                service.disconnect()
            }
        }

        content(
            ChartFeedType.PEPPERSTONE,
            ProviderChartData(
                candles = candles,
                quote = quote,
                isLoadingMore = isLoadingMore,
                hasMoreHistory = hasMoreHistory,
                onLoadMoreHistory = { endTime ->
                    if (!isLoadingMore && hasMoreHistory) {
                        isLoadingMore = true
                        service.fetchHistory(chartFeedSymbolFor(ChartFeedType.PEPPERSTONE, symbol), timeframe, endTime, 500)
                    }
                }
            )
        )
    }
}
