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

@Composable
fun MarketDepthLadder(symbol: String, price: Double) {
    val pair by MarketDataStore.pairFlow(symbol)
        .collectAsState(initial = MarketDataStore.pairSnapshot(symbol) ?: ForexPair(symbol, symbol, price, 0.0, 0.0))

    OrderBookMirror(
        selectedPair = pair ?: ForexPair(symbol, symbol, price, 0.0, 0.0),
        modifier = Modifier.fillMaxWidth(),
        showExplanation = true,
        explanationMode = DepthExplanationMode.EXECUTION
    )
}
