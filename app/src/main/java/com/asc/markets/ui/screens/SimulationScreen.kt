package com.asc.markets.ui.screens

import androidx.compose.runtime.*
import com.asc.markets.logic.ForexViewModel

@Composable
fun SimulationScreen(viewModel: ForexViewModel) {
    EASimulationPanel(viewModel = viewModel)
}

@Composable
fun MySimulationScreen(viewModel: ForexViewModel) {
    BacktestScreen(viewModel = viewModel)
}
