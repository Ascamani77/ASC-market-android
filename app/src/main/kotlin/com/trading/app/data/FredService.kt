package com.trading.app.data

import android.util.Log
import com.asc.markets.BuildConfig
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException

class FredService {
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.FRED_API_KEY

    interface FredCallback {
        fun onSuccess(data: JSONObject)
        fun onError(error: String)
    }

    /**
     * Fetch series data from FRED.
     * Example series_id for bonds:
     * - DGS10: 10-Year Treasury Constant Maturity Rate
     * - DGS2: 2-Year Treasury Constant Maturity Rate
     * - DGS30: 30-Year Treasury Constant Maturity Rate
     */
    fun fetchSeriesObservations(seriesId: String, callback: FredCallback) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            callback.onError("FRED API key is missing")
            return
        }

        val url = OBSERVATIONS_URL.toHttpUrl()
            .newBuilder()
            .addQueryParameter("series_id", seriesId)
            .addQueryParameter("api_key", key)
            .addQueryParameter("file_type", "json")
            .addQueryParameter("sort_order", "desc")
            .addQueryParameter("limit", "120")
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FredService", "Failed to fetch FRED data: ${e.message}")
                callback.onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (it.isSuccessful && body.isNotBlank()) {
                        try {
                            val json = JSONObject(body)
                            callback.onSuccess(json)
                        } catch (e: Exception) {
                            callback.onError("Failed to parse FRED response")
                        }
                    } else {
                        callback.onError("HTTP Error: ${it.code} ${body.take(240)}")
                    }
                }
            }
        })
    }

    /**
     * Search for series in FRED.
     */
    fun searchSeries(searchText: String, callback: FredCallback) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            callback.onError("FRED API key is missing")
            return
        }

        val url = SEARCH_URL.toHttpUrl()
            .newBuilder()
            .addQueryParameter("api_key", key)
            .addQueryParameter("search_text", searchText)
            .addQueryParameter("file_type", "json")
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (it.isSuccessful && body.isNotBlank()) {
                        try {
                            val json = JSONObject(body)
                            callback.onSuccess(json)
                        } catch (e: Exception) {
                            callback.onError("Failed to parse FRED response")
                        }
                    } else {
                        callback.onError("HTTP Error: ${it.code} ${body.take(240)}")
                    }
                }
            }
        })
    }

    private companion object {
        private const val OBSERVATIONS_URL = "https://api.stlouisfed.org/fred/series/observations"
        private const val SEARCH_URL = "https://api.stlouisfed.org/fred/series/search"
    }
}
