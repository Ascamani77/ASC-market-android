package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.data.ForexPair
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.screens.dashboard.MarketOverviewTab
import com.asc.markets.ui.theme.PureBlack

@Composable
fun MarketsScreen(onSelectPair: (ForexPair) -> Unit, viewModel: ForexViewModel = viewModel()) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    
    Surface(
        color = PureBlack,
        modifier = Modifier.fillMaxSize()
    ) {
        // Now containing the Market Overview Tab content from the dashboard
        MarketOverviewTab(
            selectedPair = selectedPair,
            onAssetClick = { pair ->
                viewModel.selectPair(pair)
                viewModel.navigateTo(AppView.TRADING_ASSISTANT)
            },
            viewModel = viewModel
        )
    }
}
