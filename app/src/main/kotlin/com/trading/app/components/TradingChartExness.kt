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
import com.trading.app.data.Mt5ReverseBridge
import com.trading.app.data.Mt5Service
import com.trading.app.data.chartFeedQuotes
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.BalanceRecord
import com.trading.app.models.EconomicCalendarPayload
import com.trading.app.models.OHLCData
import com.trading.app.models.Order
import com.trading.app.models.Position
import com.trading.app.models.SymbolInfo
import kotlinx.coroutines.delay

@Composable
fun TradingChartExness(
    symbol: String,
    timeframe: String,
    reverseBridge: Mt5ReverseBridge?,
    isCalendarVisible: Boolean,
    calendarRequestDateIso: String?,
    calendarRequestVersion: Int,
    isNewsVisible: Boolean,
    onAccountUpdate: (Mt5Service.AccountInfo) -> Unit = {},
    onPositionsUpdate: (List<Position>) -> Unit = {},
    onOrdersUpdate: (List<Order>) -> Unit = {},
    onHistoryOrdersUpdate: (List<Order>) -> Unit = {},
    onBalanceHistoryUpdate: (List<BalanceRecord>) -> Unit = {},
    onCalendarUpdate: (EconomicCalendarPayload) -> Unit = {},
    onNewsUpdate: (com.trading.app.models.NewsPayload) -> Unit = {},
    onSymbolsUpdate: (List<SymbolInfo>) -> Unit = {},
    content: @Composable (ChartFeedType, ProviderChartData) -> Unit
) {
    key(ChartFeedType.EXNESS) {
        val context = LocalContext.current
        val mt5Host = remember { NetworkConfig.mt5Host(context) }
        val mt5Port = remember { NetworkConfig.mt5Port(context) }
        var candles by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
        var quote by remember { mutableStateOf<SymbolQuote?>(null) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var hasMoreHistory by remember { mutableStateOf(true) }
        val currentSymbol by rememberUpdatedState(symbol)
        val currentTimeframe by rememberUpdatedState(timeframe)

        // Resolve the broker symbol (with 'm' suffix) deterministically from the static
        // catalog first - the runtime symbol map may not be populated yet for indices
        // like SPX -> US500m.
        fun resolveStreamSymbol(requested: String): String {
            val normalized = chartFeedSymbolFor(ChartFeedType.EXNESS, requested)
            val catalogMatch = chartFeedQuotes(ChartFeedType.EXNESS).firstOrNull {
                it.ticker.equals(normalized, ignoreCase = true) ||
                    it.brokerSymbol.equals(normalized, ignoreCase = true) ||
                    it.ticker.equals(requested, ignoreCase = true) ||
                    it.brokerSymbol.equals(requested, ignoreCase = true)
            }
            return catalogMatch?.brokerSymbol?.ifBlank { catalogMatch.ticker } ?: normalized
        }
        val currentOnAccountUpdate by rememberUpdatedState(onAccountUpdate)
        val currentOnPositionsUpdate by rememberUpdatedState(onPositionsUpdate)
        val currentOnOrdersUpdate by rememberUpdatedState(onOrdersUpdate)
        val currentOnHistoryOrdersUpdate by rememberUpdatedState(onHistoryOrdersUpdate)
        val currentOnBalanceHistoryUpdate by rememberUpdatedState(onBalanceHistoryUpdate)
        val currentOnCalendarUpdate by rememberUpdatedState(onCalendarUpdate)
        val currentOnNewsUpdate by rememberUpdatedState(onNewsUpdate)
        val currentOnSymbolsUpdate by rememberUpdatedState(onSymbolsUpdate)

        val service = remember(mt5Host, mt5Port) {
            Mt5Service(
                pcIpAddress = mt5Host,
                port = mt5Port,
                onHistoryUpdate = { receivedSymbol, history ->
                    val accepted = receivedSymbol.isEmpty() ||
                        providerSymbolsMatch(receivedSymbol, currentSymbol) ||
                        providerSymbolsMatch(receivedSymbol, resolveStreamSymbol(currentSymbol))
                    if (accepted) {
                        val merged = mergeProviderHistory(candles, history, isLoadingMore)
                        candles = merged
                        isLoadingMore = false
                        if (history.size < 500) hasMoreHistory = false
                        Log.d("TradingChartExness", "Loaded ${history.size} candles for $receivedSymbol")
                    }
                },
                onQuoteUpdate = { incomingQuote ->
                    val accepted = providerSymbolsMatch(incomingQuote.name, currentSymbol) ||
                        providerSymbolsMatch(incomingQuote.name, resolveStreamSymbol(currentSymbol))
                    if (accepted) {
                        quote = providerDisplayQuote(incomingQuote, currentSymbol, candles)
                    }
                },
                onAccountUpdate = { currentOnAccountUpdate(it) },
                onPositionsUpdate = { currentOnPositionsUpdate(it) },
                onOrdersUpdate = { currentOnOrdersUpdate(it) },
                onHistoryOrdersUpdate = { currentOnHistoryOrdersUpdate(it) },
                onBalanceHistoryUpdate = { currentOnBalanceHistoryUpdate(it) },
                onCalendarUpdate = { currentOnCalendarUpdate(it) },
                onNewsUpdate = { currentOnNewsUpdate(it) },
                onSymbolsUpdate = { currentOnSymbolsUpdate(it) }
            )
        }

        LaunchedEffect(mt5Host, mt5Port) {
            service.connect()
            service.requestSymbols()
            reverseBridge?.connect()
        }

        LaunchedEffect(symbol, timeframe, mt5Host, mt5Port) {
            candles = emptyList()
            quote = null
            isLoadingMore = false
            hasMoreHistory = true
            service.stopActiveStream()
            service.streamActiveSymbol(resolveStreamSymbol(symbol), timeframe, 500)

            // Watchdog: if the bridge never answers with history (lost subscribe,
            // half-dead socket, unknown symbol), force-retry a few times so the
            // chart never stays stuck on the previous asset.
            val stampAtRequest = service.lastHistoryTimestampMs
            var attempt = 0
            while (attempt < 3 && service.lastHistoryTimestampMs == stampAtRequest) {
                delay(2500L)
                if (service.lastHistoryTimestampMs == stampAtRequest) {
                    attempt++
                    Log.w("TradingChartExness", "No history for $symbol ($timeframe) yet - forcing re-subscribe (attempt $attempt)")
                    service.streamActiveSymbol(resolveStreamSymbol(symbol), timeframe, 500, force = true)
                }
            }
        }

        LaunchedEffect(isCalendarVisible, calendarRequestDateIso, calendarRequestVersion) {
            if (isCalendarVisible) {
                service.requestCalendar(calendarRequestDateIso)
            }
        }

        LaunchedEffect(isNewsVisible) {
            if (isNewsVisible) {
                service.requestNews()
            }
        }

        DisposableEffect(service, reverseBridge) {
            onDispose {
                service.disconnect()
                reverseBridge?.disconnect()
            }
        }

        content(
            ChartFeedType.EXNESS,
            ProviderChartData(
                candles = candles,
                quote = quote,
                isLoadingMore = isLoadingMore,
                hasMoreHistory = hasMoreHistory,
                onLoadMoreHistory = { endTime ->
                    if (!isLoadingMore && hasMoreHistory) {
                        isLoadingMore = true
                        service.subscribe(resolveStreamSymbol(currentSymbol), currentTimeframe, endTime, 500)
                    }
                }
            )
        )
    }
}
