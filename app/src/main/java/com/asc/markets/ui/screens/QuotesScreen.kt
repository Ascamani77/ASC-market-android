package com.asc.markets.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.logic.ForexViewModel
import com.trading.app.components.Quotes
import com.trading.app.components.SymbolQuote
import com.trading.app.components.defaultQuoteSymbols
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import com.trading.app.models.SymbolInfo

import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.asc.markets.data.NetworkConfig
import com.trading.app.data.ChartFeedType
import com.asc.markets.data.MarketDataStore
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun QuotesScreen(
    viewModel: ForexViewModel = viewModel()
) {
    val context = LocalContext.current
    val availableQuotes = remember {
        mutableStateListOf<SymbolInfo>().apply {
            addAll(defaultQuoteSymbols())
        }
    }
    val symbolQuotesByTicker = remember { mutableStateMapOf<String, SymbolQuote>() }

    // Collect prices in a LaunchedEffect to avoid recomposing the entire screen on every tick
    LaunchedEffect(Unit) {
        launch {
            MarketDataStore.allPairs.collect { pairs ->
                pairs.forEach { updateQuoteMap(symbolQuotesByTicker, it) }
            }
        }
    }

    Quotes(
        onClose = { viewModel.navigateTo(AppView.DASHBOARD) },
        quotes = availableQuotes,
        onQuoteSelect = { symbolInfo ->
            // Update the stream chart source based on the clicked item's exchange
            val prefs = context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            val feedType = when (symbolInfo.exchange) {
                "Exness" -> ChartFeedType.EXNESS
                else -> ChartFeedType.fromPref(prefs.getString(ChartFeedType.PREF_KEY, null))
            }
            prefs.edit().putString(ChartFeedType.STREAM_PREF_KEY, feedType.prefValue).apply()

            viewModel.selectPairBySymbol(symbolInfo.ticker)
            viewModel.navigateTo(AppView.STREAM)
        },
        quotesByTicker = symbolQuotesByTicker,
        onVisibleSymbolsChanged = { }
    )
}

private fun updateQuoteMap(map: MutableMap<String, SymbolQuote>, pair: com.asc.markets.data.ForexPair) {
    val ticker = pair.symbol
        .uppercase(Locale.US)
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
        .removeSuffix("M")
        .removeSuffix(".M")
        .removeSuffix(".PRO")
        .removeSuffix(".ECN")
        .removeSuffix(".S")
        .removeSuffix(".SPOT")
        .removeSuffix("+")
        .removeSuffix(".P")

    val quote = SymbolQuote(
        name = pair.name,
        lastPrice = pair.price.toFloat(),
        change = pair.change.toFloat(),
        changePercent = pair.changePercent.toFloat(),
        open = 0f,
        high = 0f,
        low = 0f,
        prevClose = 0f,
        bid = pair.price.toFloat(),
        ask = pair.price.toFloat(),
        volume = 0f
    )
    
    map[ticker] = quote
    
    // Explicit mappings for common ticker discrepancies
    if (ticker == "NAS100") map["NASDAQ100"] = quote
    if (ticker == "SPX500") map["SPX"] = quote
    if (ticker == "US30") map["DJIA"] = quote
    if (ticker == "CRUDE-F") map["CRUDE-F"] = quote
    if (ticker == "BRENT-F") map["BRENT-F"] = quote
}
