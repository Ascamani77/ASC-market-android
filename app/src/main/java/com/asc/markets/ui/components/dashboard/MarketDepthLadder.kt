package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.ui.screens.dashboard.DepthExplanationMode
import com.asc.markets.ui.screens.dashboard.OrderBookMirror

// BINANCE REMOVED - EA ONLY
@Composable
fun MarketDepthLadder(symbol: String, price: Double) {
    val storePair = MarketDataStore.pairSnapshot(symbol)
    val safePrice = price.takeIf { it.isFinite() && it > 0.0 } ?: storePair?.price?.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    val pairFlow = MarketDataStore.pairFlow(symbol)
    val pair by pairFlow.collectAsState(initial = storePair ?: ForexPair(symbol, symbol, safePrice, 0.0, 0.0))

    OrderBookMirror(
        selectedPair = pair?.takeIf { it.price.isFinite() && it.price > 0.0 } ?: ForexPair(symbol, symbol, safePrice, 0.0, 0.0),
        modifier = Modifier.fillMaxWidth(),
        showExplanation = true,
        explanationMode = DepthExplanationMode.EXECUTION
    )
}
