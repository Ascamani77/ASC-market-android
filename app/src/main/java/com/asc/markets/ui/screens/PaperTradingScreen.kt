package com.asc.markets.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.logic.ForexViewModel
import com.trading.app.TradingApp

@Composable
fun PaperTradingScreen(
    viewModel: ForexViewModel = viewModel()
) {
    TradingApp(
        startInPaperTradingPanel = true,
        onPaperTradingClose = { viewModel.navigateTo(AppView.STREAM) }
    )
}
