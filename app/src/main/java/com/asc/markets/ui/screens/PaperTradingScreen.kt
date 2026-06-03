package com.asc.markets.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.data.NetworkConfig
import com.asc.markets.logic.ForexViewModel
import com.trading.app.TradingApp
import com.trading.app.data.ChartFeedType

@Composable
fun PaperTradingScreen(
    viewModel: ForexViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var streamFeedType by remember {
        mutableStateOf(ChartFeedType.streamCurrent(context))
    }

    DisposableEffect(prefs, context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ChartFeedType.STREAM_PREF_KEY || key == ChartFeedType.PREF_KEY) {
                streamFeedType = ChartFeedType.streamCurrent(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    key(streamFeedType) {
        TradingApp(
            startInPaperTradingPanel = true,
            onPaperTradingClose = { viewModel.navigateTo(AppView.STREAM) },
            streamFeedType = streamFeedType,
            stateNamespace = "stream_${streamFeedType.prefValue}"
        )
    }
}
