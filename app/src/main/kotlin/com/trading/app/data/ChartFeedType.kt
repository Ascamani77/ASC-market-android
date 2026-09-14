package com.trading.app.data

import android.content.Context
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.TRAINED_ASSET_SYMBOLS
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
    // The stream quotes page mirrors the Market Overview page: every trained asset
    // appears here. Broker symbols carry the 'm' suffix as required by the EA bridge.
    val legacy = listOf(
        SymbolInfo("BTCUSD", "Bitcoin / US Dollar", "Exness", "spot crypto", "BTCUSDm"),
        SymbolInfo("ETHUSD", "Ethereum / US Dollar", "Exness", "spot crypto", "ETHUSDm"),
        SymbolInfo("EURUSD", "Euro / US Dollar", "Exness", "forex", "EURUSDm"),
        SymbolInfo("GBPUSD", "British Pound / US Dollar", "Exness", "forex", "GBPUSDm"),
        SymbolInfo("USDJPY", "US Dollar / Japanese Yen", "Exness", "forex", "USDJPYm"),
        SymbolInfo("AUDUSD", "Australian Dollar / US Dollar", "Exness", "forex", "AUDUSDm"),
        SymbolInfo("USDCAD", "US Dollar / Canadian Dollar", "Exness", "forex", "USDCADm"),
        SymbolInfo("USDCHF", "US Dollar / Swiss Franc", "Exness", "forex", "USDCHFm"),
        SymbolInfo("XAUUSD", "Gold / US Dollar", "Exness", "commodity cfd", "XAUUSDm"),
        SymbolInfo("XAGUSD", "Silver / US Dollar", "Exness", "commodity cfd", "XAGUSDm"),
        SymbolInfo("US500", "US S&P 500", "Exness", "index", "US500m"),
        SymbolInfo("USTEC", "US Tech 100", "Exness", "index", "USTECm"),
        SymbolInfo("US30", "Dow Jones 30", "Exness", "index", "US30m")
    )
    val seen = legacy.map { it.ticker.uppercase(Locale.US) }.toSet()
    val extra = TRAINED_ASSET_SYMBOLS
        .filter { it.uppercase(Locale.US) !in seen }
        .map { symbol ->
            SymbolInfo(
                ticker = symbol,
                name = trainedName(symbol),
                exchange = "Exness",
                type = trainedType(symbol),
                brokerSymbol = trainedBroker(symbol)
            )
        }
    return legacy + extra
}

/** Human display name for a trained asset (matches the Market Overview page). */
private fun trainedName(symbol: String): String = when (symbol) {
    "BTCUSDT" -> "Bitcoin / Tether"
    "BTCCNH" -> "Bitcoin / CNH"
    "BTCXAG" -> "Bitcoin / Silver"
    "BTCXAU" -> "Bitcoin / Gold"
    "ETHUSDT" -> "Ethereum / Tether"
    "ETHBTC" -> "Ethereum / Bitcoin"
    "AUDJPY" -> "Aussie / Yen"
    "DXY" -> "US Dollar Index"
    "EURCAD" -> "Euro / Canadian Dollar"
    "EURCHF" -> "Euro / Swiss Franc"
    "EURGBP" -> "Euro / British Pound"
    "EURJPY" -> "Euro / Japanese Yen"
    "GBPJPY" -> "British Pound / Yen"
    "NZDUSD" -> "Kiwi / US Dollar"
    "USDCNH" -> "US Dollar / Chinese Yuan"
    "XCUUSD" -> "Copper / US Dollar"
    "BRENTCMDUSD" -> "Brent Crude"
    "UKOIL" -> "UK Brent Oil"
    "USOIL" -> "WTI Crude Oil"
    "DE30" -> "Germany DAX 30"
    "JP225" -> "Japan Nikkei 225"
    "STOXX50" -> "Euro Stoxx 50"
    "UK100" -> "UK FTSE 100"
    "USTEC_x100" -> "US Tech 100 x100"
    "SPCX" -> "S&P Composite"
    "AAPL" -> "Apple Inc."
    "AMZN" -> "Amazon.com Inc."
    "META" -> "Meta Platforms"
    "MSFT" -> "Microsoft Corp."
    "NFLX" -> "Netflix Inc."
    "NVDA" -> "NVIDIA Corp."
    "PYPL" -> "PayPal Holdings"
    "TSLA" -> "Tesla Inc."
    else -> symbol
}

private fun trainedType(symbol: String): String = when {
    symbol in CRYPTO_TRAINED -> "spot crypto"
    symbol in INDICES_TRAINED -> "index"
    symbol in COMMOD_TRAINED -> "commodity cfd"
    symbol in STOCK_TRAINED -> "stock"
    else -> "forex"
}

private fun trainedBroker(symbol: String): String = when {
    symbol == "USTEC_x100" -> "USTECx100m"
    symbol in INDICES_TRAINED -> "${symbol}m"
    symbol in COMMOD_TRAINED -> "${symbol}m"
    symbol in CRYPTO_TRAINED && symbol.endsWith("USD", ignoreCase = true) -> "${symbol}m"
    else -> symbol
}

private val CRYPTO_TRAINED = setOf(
    "BTCUSD", "BTCUSDT", "BTCCNH", "BTCXAG", "BTCXAU",
    "ETHUSD", "ETHUSDT", "ETHBTC"
)
private val COMMOD_TRAINED = setOf("BRENTCMDUSD", "UKOIL", "USOIL")
private val INDICES_TRAINED = setOf(
    "DE30", "JP225", "STOXX50", "UK100", "US30", "US500", "USTEC", "USTEC_x100", "SPCX"
)
private val STOCK_TRAINED = setOf(
    "AAPL", "AMZN", "META", "MSFT", "NFLX", "NVDA", "PYPL", "TSLA"
)

fun chartFeedSymbolFor(feedType: ChartFeedType, symbol: String): String {
    val normalized = symbol.trim().uppercase(Locale.US).replace("/", "").replace("_", "").replace(" ", "")
    if (normalized.isBlank()) return normalized
    val match = chartFeedQuotes(feedType).firstOrNull {
        it.ticker.equals(normalized, ignoreCase = true) || it.brokerSymbol.equals(normalized, ignoreCase = true)
    }
    return match?.ticker ?: normalized.removeSuffix("M")
}
