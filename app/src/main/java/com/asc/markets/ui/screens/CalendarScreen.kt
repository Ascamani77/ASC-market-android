package com.asc.markets.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.logic.ForexViewModel
import com.trading.app.components.CalendarPage
import com.trading.app.data.CalendarSnapshotStore

@Composable
fun CalendarScreen(
    viewModel: ForexViewModel = viewModel()
) {
    val payload = CalendarSnapshotStore.latestDisplayPayload
    
    CalendarPage(
        payload = payload,
        isLoading = false,
        onBack = { viewModel.navigateTo(AppView.DASHBOARD) },
        onRefresh = { },
        onSelectDate = { },
        onPreviousMonth = { },
        onNextMonth = { }
    )
}
