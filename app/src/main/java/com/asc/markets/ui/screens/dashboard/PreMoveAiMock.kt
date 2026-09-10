package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.asc.markets.data.ForexPair
import com.asc.markets.data.remote.FinalDecisionItem

@Composable
fun PreMoveAiMockImage(
    modifier: Modifier = Modifier,
    symbol: String = "",
    selectedPair: ForexPair? = null,
    livePairs: List<ForexPair> = emptyList(),
    priceHistory: Any? = null,
    timedPriceHistory: Any? = null,
    aiDecisions: List<FinalDecisionItem> = emptyList(),
    onAssetSelected: (ForexPair) -> Unit = {}
) {
    Box(modifier = modifier) {
        Text("AI Vision Preview: $symbol", color = Color.Gray)
    }
}
