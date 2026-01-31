package com.asc.markets.ui.screens.dashboard

import androidx.compose.runtime.Composable
import com.asc.markets.data.ForexPair
import com.asc.markets.ui.screens.MarketChartsContent

@Composable
fun MarketOverviewTab(selectedPair: ForexPair) {
    // Delegate to MarketChartsContent which composes the full overview page (charts, depth ladder, macro grid, exposure hub, etc.)
    MarketChartsContent(selectedPair)
}
