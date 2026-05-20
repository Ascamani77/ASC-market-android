package com.trading.app.data

import android.content.Context
import com.asc.markets.data.NetworkConfig
import com.trading.app.models.SymbolInfo
import java.util.Locale

enum class ChartFeedType(val prefValue: String, val displayName: String) {
    EXNESS("exness", "Exness"),
    PEPPERSTONE_CTRADER("pepperstone_ctrader", "Pepperstone cTrader"),
    BINANCE("binance", "Binance"),
    BINANCE_CONNECT("binance_connect", "Binance Connect");

    companion object {
        const val PREF_KEY = "chart_feed_type"
        const val STREAM_PREF_KEY = "stream_chart_feed_type"

        fun fromPref(value: String?): ChartFeedType {
            // Map old "pepperstone" to new "pepperstone_ctrader" for backward compatibility
            val normalizedValue = if (value?.equals("pepperstone", ignoreCase = true) == true) {
                "pepperstone_ctrader"
            } else {
                value
            }
            return values().firstOrNull { it.prefValue.equals(normalizedValue, ignoreCase = true) } ?: PEPPERSTONE_CTRADER
        }

        fun current(context: Context): ChartFeedType {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            return fromPref(prefs.getString(PREF_KEY, PEPPERSTONE_CTRADER.prefValue))
        }

        fun streamCurrent(context: Context): ChartFeedType {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            val streamPref = prefs.getString(STREAM_PREF_KEY, null)
            if (streamPref != null) {
                return fromPref(streamPref)
            }
            return fromPref(prefs.getString(PREF_KEY, PEPPERSTONE_CTRADER.prefValue))
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
        ChartFeedType.PEPPERSTONE_CTRADER -> listOf(
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
        ChartFeedType.BINANCE_CONNECT -> listOf(
            SymbolInfo("BTCUSDT", "Bitcoin / TetherUS", "Binance Connect", "spot crypto", "BTCUSDT"),
            SymbolInfo("ETHUSDT", "Ethereum / TetherUS", "Binance Connect", "spot crypto", "ETHUSDT"),
            SymbolInfo("BNBUSDT", "BNB / TetherUS", "Binance Connect", "spot crypto", "BNBUSDT"),
            SymbolInfo("SOLUSDT", "Solana / TetherUS", "Binance Connect", "spot crypto", "SOLUSDT"),
            SymbolInfo("XRPUSDT", "XRP / TetherUS", "Binance Connect", "spot crypto", "XRPUSDT"),
            SymbolInfo("ADAUSDT", "Cardano / TetherUS", "Binance Connect", "spot crypto", "ADAUSDT"),
            SymbolInfo("DOGEUSDT", "Dogecoin / TetherUS", "Binance Connect", "spot crypto", "DOGEUSDT"),
            SymbolInfo("AVAXUSDT", "Avalanche / TetherUS", "Binance Connect", "spot crypto", "AVAXUSDT"),
            SymbolInfo("LINKUSDT", "Chainlink / TetherUS", "Binance Connect", "spot crypto", "LINKUSDT"),
            SymbolInfo("DOTUSDT", "Polkadot / TetherUS", "Binance Connect", "spot crypto", "DOTUSDT")
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
        ChartFeedType.BINANCE_CONNECT -> normalized
        ChartFeedType.PEPPERSTONE_CTRADER -> normalized.removeSuffix("M")
        else -> normalized.removeSuffix("M")
    }
}
