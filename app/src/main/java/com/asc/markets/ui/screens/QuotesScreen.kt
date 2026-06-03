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
import com.trading.app.models.SymbolInfo

import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.asc.markets.data.NetworkConfig
import com.trading.app.data.ChartFeedType

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

    Quotes(
        onClose = { viewModel.navigateTo(AppView.DASHBOARD) },
        quotes = availableQuotes,
        onQuoteSelect = { symbolInfo ->
            // Update the stream chart source based on the clicked item's exchange
            val prefs = context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            val feedType = when (symbolInfo.exchange) {
                "Binance" -> ChartFeedType.BINANCE
                "Exness" -> ChartFeedType.EXNESS
                "Pepperstone" -> ChartFeedType.PEPPERSTONE_CTRADER
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
