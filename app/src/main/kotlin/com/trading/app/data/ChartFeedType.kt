package com.trading.app.data

import android.content.Context
import com.asc.markets.data.NetworkConfig
import com.trading.app.models.SymbolInfo
import java.util.Locale

enum class ChartFeedType(val prefValue: String, val displayName: String) {
    EXNESS("exness", "Exness MT5");

    companion object {
        const val PREF_KEY = "chart_feed_type"
        const val STREAM_PREF_KEY = "stream_chart_feed_type"

        // MT5-only: all legacy values map to EXNESS for backward compatibility
        fun fromPref(value: String?): ChartFeedType {
            return EXNESS
        }

        fun current(context: Context): ChartFeedType {
            // Read pref for migration but always return EXNESS
            context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_KEY, EXNESS.prefValue)
            return EXNESS
        }

        fun streamCurrent(context: Context): ChartFeedType {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            // migrate legacy stream pref if present
            prefs.getString(STREAM_PREF_KEY, null)
            prefs.getString(PREF_KEY, EXNESS.prefValue)
            return EXNESS
        }
    }
}

fun chartFeedQuotes(feedType: ChartFeedType): List<SymbolInfo> {
    // MT5-only catalog - broker symbols carry 'm' suffix as required by EA bridge
    return listOf(
        SymbolInfo("BTCUSD", "Bitcoin / U.S. Dollar", "Exness", "spot crypto", "BTCUSDm"),
        SymbolInfo("ETHUSD", "Ethereum / U.S. Dollar", "Exness", "spot crypto", "ETHUSDm"),
        SymbolInfo("EURUSD", "Euro / U.S. Dollar", "Exness", "forex", "EURUSDm"),
        SymbolInfo("GBPUSD", "British Pound / U.S. Dollar", "Exness", "forex", "GBPUSDm"),
        SymbolInfo("USDJPY", "U.S. Dollar / Japanese Yen", "Exness", "forex", "USDJPYm"),
        SymbolInfo("AUDUSD", "Australian Dollar / U.S. Dollar", "Exness", "forex", "AUDUSDm"),
        SymbolInfo("USDCAD", "U.S. Dollar / Canadian Dollar", "Exness", "forex", "USDCADm"),
        SymbolInfo("USDCHF", "U.S. Dollar / Swiss Franc", "Exness", "forex", "USDCHFm"),
        SymbolInfo("XAUUSD", "Gold / U.S. Dollar", "Exness", "commodity cfd", "XAUUSDm"),
        SymbolInfo("XAGUSD", "Silver / U.S. Dollar", "Exness", "commodity cfd", "XAGUSDm"),
        SymbolInfo("SPX", "S&P 500 Index", "Exness", "index", "US500m"),
        SymbolInfo("NASDAQ100", "Nasdaq 100 Index", "Exness", "index", "USTECm"),
        SymbolInfo("DJIA", "Dow Jones Industrial Average", "Exness", "index", "US30m")
    )
}

fun chartFeedSymbolFor(feedType: ChartFeedType, symbol: String): String {
    val normalized = symbol.trim().uppercase(Locale.US).replace("/", "").replace("_", "").replace(" ", "")
    if (normalized.isBlank()) return normalized
    val match = chartFeedQuotes(feedType).firstOrNull {
        it.ticker.equals(normalized, ignoreCase = true) || it.brokerSymbol.equals(normalized, ignoreCase = true)
    }
    return match?.ticker ?: normalized.removeSuffix("M")
}
