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
import com.trading.app.data.PepperstoneCTraderChartService
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.OHLCData

/**
 * Independent Pepperstone cTrader Chart Wrapper
 * This is a completely independent chart implementation that uses its own service instance
 * and state management, separate from the standard Pepperstone chart.
 */
@Composable
fun TradingChartPepperstoneCTrader(
    symbol: String,
    timeframe: String,
    content: @Composable (ChartFeedType, ProviderChartData) -> Unit
) {
    key(ChartFeedType.PEPPERSTONE_CTRADER, symbol, timeframe) {
        val context = LocalContext.current
        val cTraderHost = remember { NetworkConfig.cTraderHost(context) }
        val cTraderPort = remember { NetworkConfig.cTraderPort(context) }
        var candles by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
        var quote by remember { mutableStateOf<SymbolQuote?>(null) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var hasMoreHistory by remember { mutableStateOf(true) }
        val currentSymbol by rememberUpdatedState(symbol)

        // Create independent service instance
        val service = remember(cTraderHost, cTraderPort) {
            Log.d("TradingChartPepperstoneCTrader", "Creating new independent service instance")
            PepperstoneCTraderChartService(
                host = cTraderHost,
                port = cTraderPort,
                onQuoteUpdate = { incomingQuote ->
                    if (providerSymbolsMatch(incomingQuote.name, currentSymbol)) {
                        quote = providerDisplayQuote(incomingQuote, currentSymbol, candles)
                        Log.d("TradingChartPepperstoneCTrader", "Quote update: ${incomingQuote.name} @ ${incomingQuote.lastPrice}")
                    }
                },
                onHistoryUpdate = { receivedSymbol, history ->
                    if (providerSymbolsMatch(receivedSymbol, currentSymbol)) {
                        val merged = mergeProviderHistory(candles, history, isLoadingMore)
                        candles = merged
                        isLoadingMore = false
                        if (history.size < 500) hasMoreHistory = false
                        Log.d("TradingChartPepperstoneCTrader", "Loaded ${history.size} candles for $receivedSymbol (total: ${candles.size})")
                    }
                }
            )
        }

        LaunchedEffect(symbol, timeframe, cTraderHost, cTraderPort) {
            val streamSymbol = chartFeedSymbolFor(ChartFeedType.PEPPERSTONE_CTRADER, symbol)
            Log.d("TradingChartPepperstoneCTrader", "Starting stream for $streamSymbol, timeframe: $timeframe")
            candles = emptyList()
            quote = null
            isLoadingMore = false
            hasMoreHistory = true
            service.stopActiveStream()
            service.streamActiveSymbol(streamSymbol, timeframe, 500)
        }

        DisposableEffect(service) {
            onDispose {
                Log.d("TradingChartPepperstoneCTrader", "Disposing service")
                service.disconnect()
            }
        }

        content(
            ChartFeedType.PEPPERSTONE_CTRADER,
            ProviderChartData(
                candles = candles,
                quote = quote,
                isLoadingMore = isLoadingMore,
                hasMoreHistory = hasMoreHistory,
                onLoadMoreHistory = { endTime ->
                    if (!isLoadingMore && hasMoreHistory) {
                        isLoadingMore = true
                        val fetchSymbol = chartFeedSymbolFor(ChartFeedType.PEPPERSTONE_CTRADER, symbol)
                        Log.d("TradingChartPepperstoneCTrader", "Loading more history for $fetchSymbol, endTime: $endTime")
                        service.fetchHistory(fetchSymbol, timeframe, endTime, 500)
                    }
                }
            )
        )
    }
}
