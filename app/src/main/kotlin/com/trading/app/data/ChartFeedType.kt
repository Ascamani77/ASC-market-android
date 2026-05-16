package com.trading.app.data

import android.content.Context
import com.asc.markets.data.NetworkConfig
import com.trading.app.models.SymbolInfo
import java.util.Locale

enum class ChartFeedType(val prefValue: String, val displayName: String) {
    EXNESS("exness", "Exness"),
    PEPPERSTONE("pepperstone", "Pepperstone"),
    BINANCE("binance", "Binance");

    companion object {
        const val PREF_KEY = "chart_feed_type"
        const val STREAM_PREF_KEY = "stream_chart_feed_type"

        fun fromPref(value: String?): ChartFeedType {
            return values().firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: PEPPERSTONE
        }

        fun current(context: Context): ChartFeedType {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            return fromPref(prefs.getString(PREF_KEY, PEPPERSTONE.prefValue))
        }

        fun streamCurrent(context: Context): ChartFeedType {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            val streamPref = prefs.getString(STREAM_PREF_KEY, null)
            if (streamPref != null) {
                return fromPref(streamPref)
            }
            return fromPref(prefs.getString(PREF_KEY, PEPPERSTONE.prefValue))
        }
    }
}

fun chartFeedQuotes(feedType: ChartFeedType): List<SymbolInfo> {
    return when (feedType) {
        ChartFeedType.EXNESS -> listOf(
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
            SymbolInfo("USOIL", "WTI Crude Oil", "Exness", "commodity cfd", "USOILm"),
            SymbolInfo("BRENTOIL", "Brent Crude Oil", "Exness", "commodity cfd", "UKOILm"),
            SymbolInfo("SPX", "S&P 500 Index", "Exness", "index", "US500m"),
            SymbolInfo("NASDAQ100", "Nasdaq 100 Index", "Exness", "index", "USTECm"),
            SymbolInfo("DJIA", "Dow Jones Industrial Average", "Exness", "index", "US30m")
        )
        ChartFeedType.PEPPERSTONE -> listOf(
            SymbolInfo("EURUSD", "Euro / U.S. Dollar", "Pepperstone", "forex", "EURUSD"),
            SymbolInfo("GBPUSD", "British Pound / U.S. Dollar", "Pepperstone", "forex", "GBPUSD"),
            SymbolInfo("USDJPY", "U.S. Dollar / Japanese Yen", "Pepperstone", "forex", "USDJPY"),
            SymbolInfo("AUDUSD", "Australian Dollar / U.S. Dollar", "Pepperstone", "forex", "AUDUSD"),
            SymbolInfo("USDCAD", "U.S. Dollar / Canadian Dollar", "Pepperstone", "forex", "USDCAD"),
            SymbolInfo("NZDUSD", "New Zealand Dollar / U.S. Dollar", "Pepperstone", "forex", "NZDUSD"),
            SymbolInfo("USDCHF", "U.S. Dollar / Swiss Franc", "Pepperstone", "forex", "USDCHF"),
            SymbolInfo("XAUUSD", "Gold / U.S. Dollar", "Pepperstone", "commodity cfd", "XAUUSD"),
            SymbolInfo("XAGUSD", "Silver / U.S. Dollar", "Pepperstone", "commodity cfd", "XAGUSD"),
            SymbolInfo("USOIL", "WTI Crude Oil", "Pepperstone", "commodity cfd", "USOIL"),
            SymbolInfo("BTCUSD", "Bitcoin / U.S. Dollar", "Pepperstone", "crypto cfd", "BTCUSD"),
            SymbolInfo("ETHUSD", "Ethereum / U.S. Dollar", "Pepperstone", "crypto cfd", "ETHUSD"),
            SymbolInfo("NAS100", "Nasdaq 100 Index", "Pepperstone", "index", "NAS100"),
            SymbolInfo("US30", "Dow Jones Industrial Average", "Pepperstone", "index", "US30"),
            SymbolInfo("SPX500", "S&P 500 Index", "Pepperstone", "index", "SPX500")
        )
        ChartFeedType.BINANCE -> listOf(
            SymbolInfo("BTCUSDT", "Bitcoin / TetherUS", "Binance", "spot crypto", "BTCUSDT"),
            SymbolInfo("ETHUSDT", "Ethereum / TetherUS", "Binance", "spot crypto", "ETHUSDT"),
            SymbolInfo("BNBUSDT", "BNB / TetherUS", "Binance", "spot crypto", "BNBUSDT"),
            SymbolInfo("SOLUSDT", "Solana / TetherUS", "Binance", "spot crypto", "SOLUSDT"),
            SymbolInfo("XRPUSDT", "XRP / TetherUS", "Binance", "spot crypto", "XRPUSDT"),
            SymbolInfo("ADAUSDT", "Cardano / TetherUS", "Binance", "spot crypto", "ADAUSDT"),
            SymbolInfo("DOGEUSDT", "Dogecoin / TetherUS", "Binance", "spot crypto", "DOGEUSDT"),
            SymbolInfo("AVAXUSDT", "Avalanche / TetherUS", "Binance", "spot crypto", "AVAXUSDT"),
            SymbolInfo("LINKUSDT", "Chainlink / TetherUS", "Binance", "spot crypto", "LINKUSDT"),
            SymbolInfo("DOTUSDT", "Polkadot / TetherUS", "Binance", "spot crypto", "DOTUSDT")
        )
    }
}

fun chartFeedSymbolFor(feedType: ChartFeedType, symbol: String): String {
    val normalized = symbol.trim().uppercase(Locale.US).replace("/", "").replace("_", "").replace(" ", "")
    if (normalized.isBlank()) return normalized
    val match = chartFeedQuotes(feedType).firstOrNull {
        it.ticker.equals(normalized, ignoreCase = true) || it.brokerSymbol.equals(normalized, ignoreCase = true)
    }
    return match?.ticker ?: when (feedType) {
        ChartFeedType.BINANCE -> normalized
        else -> normalized.removeSuffix("M")
    }
}
