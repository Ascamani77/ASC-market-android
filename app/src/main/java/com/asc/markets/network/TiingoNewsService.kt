package com.asc.markets.network

import android.util.Log
import com.asc.markets.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TiingoNewsArticle(
    val id: Int,
    val title: String,
    val url: String,
    val description: String,
    val publishedDate: String,
    val crawlDate: String,
    val source: String,
    val tickers: List<String>,
    val tags: List<String>
)

class TiingoNewsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun fetchLatestNews(tickers: List<String>? = null, tags: List<String>? = null, limit: Int = 20): List<TiingoNewsArticle> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.TIINGO_API_KEY
        Log.i(TAG, "Fetching Tiingo news - API key present: ${apiKey.isNotBlank()}")
        if (apiKey.isBlank()) {
            Log.w(TAG, "Tiingo API key is missing")
            return@withContext emptyList()
        }

        val urlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host("api.tiingo.com")
            .addPathSegments("tiingo/news")
            .addQueryParameter("token", apiKey)

        tickers?.takeIf { it.isNotEmpty() }?.let {
            urlBuilder.addQueryParameter("tickers", it.joinToString(","))
        }

        tags?.takeIf { it.isNotEmpty() }?.let {
            urlBuilder.addQueryParameter("tags", it.joinToString(","))
        }

        val url = urlBuilder.build()
        Log.i(TAG, "Fetching from URL: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.i(TAG, "Response code: ${response.code}, body length: ${body.length}")
                if (!response.isSuccessful) {
                    Log.w(TAG, "Tiingo news fetch failed: ${response.code} ${body.take(240)}")
                    return@withContext emptyList()
                }

                Log.i(TAG, "Response body preview: ${body.take(500)}")
                val json = JSONArray(body)
                val articles = buildList {
                    for (i in 0 until minOf(json.length(), limit)) {
                        val item = json.optJSONObject(i) ?: continue
                        val article = parseNewsArticle(item)
                        if (article != null) add(article)
                    }
                }
                Log.i(TAG, "Tiingo news fetched ${articles.size} articles")
                articles
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tiingo news fetch error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseNewsArticle(json: JSONObject): TiingoNewsArticle? {
        return try {
            val id = json.optInt("id", 0)
            val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null
            val url = json.optString("url").takeIf { it.isNotBlank() } ?: return null
            val description = json.optString("description").orEmpty()
            val publishedDate = json.optString("publishedDate").orEmpty()
            val crawlDate = json.optString("crawlDate").orEmpty()
            val source = json.optString("source").orEmpty()
            
            val tickersArray = json.optJSONArray("tickers")
            val tickers = mutableListOf<String>()
            if (tickersArray != null) {
                for (i in 0 until tickersArray.length()) {
                    tickers.add(tickersArray.optString(i))
                }
            }
            
            val tagsArray = json.optJSONArray("tags")
            val tags = mutableListOf<String>()
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tags.add(tagsArray.optString(i))
                }
            }

            TiingoNewsArticle(
                id = id,
                title = title,
                url = url,
                description = description,
                publishedDate = publishedDate,
                crawlDate = crawlDate,
                source = source,
                tickers = tickers,
                tags = tags
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse news article: ${e.message}")
            null
        }
    }

    private companion object {
        private const val TAG = "TiingoNews"
    }
}
