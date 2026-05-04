package com.asc.markets.network

import android.content.Context
import android.util.Log
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.TimeUnit

class TiingoIexRestClient(
    private val apiKey: String,
    context: Context
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Volatile
    private var rateLimitedUntilMillis = prefs.getLong(KEY_RATE_LIMITED_UNTIL, 0L)

    @Volatile
    private var nextAllowedRequestMillis = prefs.getLong(KEY_NEXT_ALLOWED_REQUEST_AT, 0L)

    suspend fun fetchTopPairs(symbols: List<String>): List<ForexPair> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now < rateLimitedUntilMillis) {
            Log.w(TAG, "Skipping Tiingo IEX REST top; rate limited for ${(rateLimitedUntilMillis - now) / 1000}s")
            return@withContext emptyList()
        }
        if (now < nextAllowedRequestMillis) {
            Log.i(TAG, "Skipping Tiingo IEX REST top; cooldown for ${(nextAllowedRequestMillis - now) / 1000}s")
            return@withContext emptyList()
        }

        val tickers = symbols
            .asSequence()
            .map(::normalizeTicker)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        if (apiKey.isBlank() || tickers.isEmpty()) {
            return@withContext emptyList()
        }

        val url = TOP_URL.toHttpUrl()
            .newBuilder()
            .addQueryParameter("tickers", tickers.joinToString(","))
            .addQueryParameter("token", apiKey)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (response.code == 429) {
                        val retryAfterMillis = response.header("Retry-After")
                            ?.toLongOrNull()
                            ?.let { TimeUnit.SECONDS.toMillis(it) }
                            ?: RATE_LIMIT_BACKOFF_MS
                        rateLimitedUntilMillis = System.currentTimeMillis() + retryAfterMillis
                        prefs.edit().putLong(KEY_RATE_LIMITED_UNTIL, rateLimitedUntilMillis).apply()
                        Log.w(TAG, "Tiingo IEX REST rate limited; backing off for ${retryAfterMillis / 1000}s")
                    }
                    Log.w(TAG, "Tiingo IEX REST top failed code=${response.code} body=${body.take(240)}")
                    return@withContext emptyList()
                }

                val json = JSONArray(body)
                val pairs = buildList {
                    for (index in 0 until json.length()) {
                        val item = json.optJSONObject(index) ?: continue
                        val pair = parseTopItem(item)
                        if (pair != null) add(pair)
                    }
                }
                Log.i(TAG, "Tiingo IEX REST top returned ${pairs.size}/${tickers.size} prices")
                val nextAllowed = System.currentTimeMillis() + SUCCESS_COOLDOWN_MS
                nextAllowedRequestMillis = nextAllowed
                prefs.edit().putLong(KEY_NEXT_ALLOWED_REQUEST_AT, nextAllowed).apply()
                pairs
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tiingo IEX REST top request failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseTopItem(item: org.json.JSONObject): ForexPair? {
        val symbol = normalizeDisplaySymbol(item.optString("ticker"))
        if (symbol.isBlank()) return null

        val last = item.optDouble("last", Double.NaN)
        val tngoLast = item.optDouble("tngoLast", Double.NaN)
        val prevClose = item.optDouble("prevClose", Double.NaN)
        
        val price = when {
            last.isFinite() && last > 0.0 -> last
            tngoLast.isFinite() && tngoLast > 0.0 -> tngoLast
            prevClose.isFinite() && prevClose > 0.0 -> prevClose
            else -> return null
        }

        val existing = MarketDataStore.pairSnapshot(symbol)
        val previousPrice = existing?.price ?: price
        val change = price - previousPrice
        val changePercent = if (previousPrice != 0.0) (change / previousPrice) * 100.0 else 0.0
        val template = FOREX_PAIRS.firstOrNull {
            it.category == MarketCategory.STOCK &&
                it.symbol.replace("/", "").uppercase(Locale.US) == symbol
        }

        return ForexPair(
            symbol = template?.symbol ?: symbol,
            name = template?.name ?: existing?.name ?: symbol,
            price = price,
            change = change,
            changePercent = changePercent,
            category = MarketCategory.STOCK
        )
    }

    private fun normalizeTicker(symbol: String): String {
        return symbol
            .trim()
            .lowercase(Locale.US)
    }

    private fun normalizeDisplaySymbol(symbol: String): String {
        return symbol
            .trim()
            .uppercase(Locale.US)
    }

    private companion object {
        private const val TAG = "TiingoIexREST"
        private const val PREFS_NAME = "asc_prefs"
        private const val KEY_RATE_LIMITED_UNTIL = "tiingo_iex_rest_rate_limited_until"
        private const val KEY_NEXT_ALLOWED_REQUEST_AT = "tiingo_iex_rest_next_allowed_request_at"
        private const val TOP_URL = "https://api.tiingo.com/iex"
        private val RATE_LIMIT_BACKOFF_MS = TimeUnit.HOURS.toMillis(1)
        private val SUCCESS_COOLDOWN_MS = TimeUnit.HOURS.toMillis(1)
    }
}
