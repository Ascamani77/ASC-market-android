package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.logic.ForexViewModel
import com.trading.app.components.PaperTradingPanel
import com.trading.app.data.PaperTradingSnapshotStore
import com.asc.markets.ui.theme.PureBlack

@Composable
fun PaperTradingScreen(
    viewModel: ForexViewModel = viewModel()
) {
    val snapshot = PaperTradingSnapshotStore.snapshot
    
    PaperTradingPanel(
        onClose = { viewModel.navigateTo(AppView.DASHBOARD) },
        positions = emptyList(), // These should ideally come from a store or viewModel
        orders = emptyList(),
        balance = snapshot.balance,
        accountInfo = null, // Or from mt5Service if accessible
        backgroundColor = PureBlack
    )
}
