package com.trading.app.data

import android.content.Context
import com.asc.markets.data.NetworkConfig

enum class BinanceMarketType(val prefValue: String, val displayName: String) {
    FUTURES("futures", "Futures");

    companion object {
        const val PREF_KEY = "binance_market_type"

        fun fromPref(value: String?): BinanceMarketType {
            return values().firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: FUTURES
        }

        fun current(context: Context): BinanceMarketType {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            return fromPref(prefs.getString(PREF_KEY, FUTURES.prefValue))
        }
    }
}
