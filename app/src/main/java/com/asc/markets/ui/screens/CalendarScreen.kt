package com.asc.markets.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.data.NetworkConfig
import com.asc.markets.logic.ForexViewModel
import com.trading.app.components.CalendarPage
import com.trading.app.data.CalendarSnapshotStore
import com.trading.app.data.Mt5Service
import java.time.LocalDate

@Composable
fun CalendarScreen(
    viewModel: ForexViewModel = viewModel()
) {
    val context = LocalContext.current
    var payload by remember { mutableStateOf(CalendarSnapshotStore.latestDisplayPayload) }
    var isLoading by remember { mutableStateOf(payload == null) }
    var selectedDateIso by remember { mutableStateOf(payload?.selectedDateIso ?: LocalDate.now().toString()) }
    val mt5Service = remember {
        Mt5Service(
            pcIpAddress = NetworkConfig.mt5Host(context),
            port = NetworkConfig.mt5Port(context),
            onHistoryUpdate = { _, _ -> },
            onQuoteUpdate = { _ -> },
            onCalendarUpdate = { calendarPayload ->
                payload = calendarPayload.display
                selectedDateIso = calendarPayload.display.selectedDateIso
                isLoading = false
                CalendarSnapshotStore.latestDisplayPayload = calendarPayload.display
                CalendarSnapshotStore.latestAiPayload = calendarPayload.ai
            },
            onConnectionStatusUpdate = { connected ->
                if (connected) {
                    isLoading = true
                }
            }
        )
    }

    DisposableEffect(mt5Service) {
        mt5Service.connect()
        onDispose {
            mt5Service.disconnect()
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        mt5Service.requestCalendar(selectedDateIso)
    }

    CalendarPage(
        payload = payload,
        isLoading = isLoading,
        onBack = { viewModel.navigateTo(AppView.DASHBOARD) },
        onRefresh = {
            isLoading = true
            mt5Service.requestCalendar(selectedDateIso)
        },
        onSelectDate = { isoDate ->
            selectedDateIso = isoDate
            isLoading = true
            mt5Service.requestCalendar(isoDate)
        },
        onPreviousMonth = {
            selectedDateIso = LocalDate.parse(selectedDateIso).minusMonths(1).toString()
            isLoading = true
            mt5Service.requestCalendar(selectedDateIso)
        },
        onNextMonth = {
            selectedDateIso = LocalDate.parse(selectedDateIso).plusMonths(1).toString()
            isLoading = true
            mt5Service.requestCalendar(selectedDateIso)
        }
    )
}
