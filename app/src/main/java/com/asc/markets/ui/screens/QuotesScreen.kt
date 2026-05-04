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

@Composable
fun QuotesScreen(
    viewModel: ForexViewModel = viewModel()
) {
    val availableQuotes = remember {
        mutableStateListOf<SymbolInfo>().apply {
            addAll(defaultQuoteSymbols())
        }
    }
    val symbolQuotesByTicker = remember { mutableStateMapOf<String, SymbolQuote>() }

    Quotes(
        onClose = { viewModel.navigateTo(AppView.DASHBOARD) },
        quotes = availableQuotes,
        onQuoteSelect = { symbol ->
            viewModel.selectPairBySymbol(symbol)
            viewModel.navigateTo(AppView.STREAM)
        },
        quotesByTicker = symbolQuotesByTicker,
        onVisibleSymbolsChanged = { }
    )
}
