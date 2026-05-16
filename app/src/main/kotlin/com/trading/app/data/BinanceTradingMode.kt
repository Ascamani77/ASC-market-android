package com.trading.app.data

import android.content.Context
import com.asc.markets.data.NetworkConfig

enum class BinanceTradingMode(val prefValue: String, val displayName: String) {
    LIVE("live", "Live"),
    DEMO("demo", "Demo");

    companion object {
        const val PREF_KEY = "binance_trading_mode"

        fun fromPref(value: String?): BinanceTradingMode {
            return values().firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: LIVE
        }

        fun current(context: Context): BinanceTradingMode {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            return fromPref(prefs.getString(PREF_KEY, LIVE.prefValue))
        }
    }
}
