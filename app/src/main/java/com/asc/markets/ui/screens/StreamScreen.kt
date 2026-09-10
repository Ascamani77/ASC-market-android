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
import com.asc.markets.data.NetworkConfig
import com.trading.app.TradingApp
import com.trading.app.data.ChartFeedType

@Composable
fun StreamScreen(initialSymbol: String? = null) {
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
            streamFeedType = streamFeedType,
            stateNamespace = "stream_${streamFeedType.prefValue}",
            initialSymbol = initialSymbol
        )
    }
}
