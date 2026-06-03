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
import com.trading.app.data.PepperstoneDemoChartService
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.OHLCData

/**
 * Independent Pepperstone Demo Chart Wrapper
 * This is a completely independent chart implementation that uses its own service instance
 * and state management, separate from the live Pepperstone chart.
 * Connects to demo bridge on port 8083.
 */
@Composable
fun TradingChartPepperstoneDemo(
    symbol: String,
    timeframe: String,
    content: @Composable (ChartFeedType, ProviderChartData) -> Unit
) {
    key(ChartFeedType.PEPPERSTONE_DEMO, symbol, timeframe) {
        val context = LocalContext.current
        val cTraderDemoHost = remember { NetworkConfig.cTraderDemoHost(context) }
        val cTraderDemoPort = remember { NetworkConfig.cTraderDemoPort(context) }
        var candles by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
        var quote by remember { mutableStateOf<SymbolQuote?>(null) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var hasMoreHistory by remember { mutableStateOf(true) }
        val currentSymbol by rememberUpdatedState(symbol)

        // Create independent demo service instance
        val service = remember(cTraderDemoHost, cTraderDemoPort) {
            Log.d("TradingChartPepperstoneDemo", "Creating new independent DEMO service instance")
            PepperstoneDemoChartService(
                host = cTraderDemoHost,
                port = cTraderDemoPort,
                redisHost = "192.168.1.198",
                redisPort = 6379,
                publishToRedis = true,
                onQuoteUpdate = { incomingQuote ->
                    if (providerSymbolsMatch(incomingQuote.name, currentSymbol)) {
                        quote = providerDisplayQuote(incomingQuote, currentSymbol, candles)
                        Log.d("TradingChartPepperstoneDemo", "DEMO Quote update: ${incomingQuote.name} @ ${incomingQuote.lastPrice}")
                    }
                },
                onHistoryUpdate = { receivedSymbol, history ->
                    if (providerSymbolsMatch(receivedSymbol, currentSymbol)) {
                        val merged = mergeProviderHistory(candles, history, isLoadingMore)
                        candles = merged
                        isLoadingMore = false
                        if (history.size < 500) hasMoreHistory = false
                        Log.d("TradingChartPepperstoneDemo", "DEMO Loaded ${history.size} candles for $receivedSymbol (total: ${candles.size})")
                    }
                }
            )
        }

        LaunchedEffect(symbol, timeframe, cTraderDemoHost, cTraderDemoPort) {
            val streamSymbol = chartFeedSymbolFor(ChartFeedType.PEPPERSTONE_DEMO, symbol)
            Log.d("TradingChartPepperstoneDemo", "Starting DEMO stream for $streamSymbol, timeframe: $timeframe")
            candles = emptyList()
            quote = null
            isLoadingMore = false
            hasMoreHistory = true
            service.stopActiveStream()
            service.streamActiveSymbol(streamSymbol, timeframe, 500)
        }

        DisposableEffect(service) {
            onDispose {
                Log.d("TradingChartPepperstoneDemo", "Disposing DEMO service")
                service.disconnect()
            }
        }

        content(
            ChartFeedType.PEPPERSTONE_DEMO,
            ProviderChartData(
                candles = candles,
                quote = quote,
                isLoadingMore = isLoadingMore,
                hasMoreHistory = hasMoreHistory,
                onLoadMoreHistory = { endTime ->
                    if (!isLoadingMore && hasMoreHistory) {
                        isLoadingMore = true
                        val fetchSymbol = chartFeedSymbolFor(ChartFeedType.PEPPERSTONE_DEMO, symbol)
                        Log.d("TradingChartPepperstoneDemo", "Loading more DEMO history for $fetchSymbol, endTime: $endTime")
                        service.fetchHistory(fetchSymbol, timeframe, endTime, 500)
                    }
                }
            )
        )
    }
}
