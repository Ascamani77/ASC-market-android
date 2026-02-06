package com.asc.markets.data

import android.content.Context
import android.util.Log
import com.asc.markets.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"

    // last fetch metadata for UI/status
    @Volatile
    var lastFetchMillis: Long = 0L
        private set

    @Volatile
    var lastFetchSuccess: Boolean = false
        private set

    /**
     * Fetch remote config JSON from the endpoint defined in BuildConfig.REMOTE_CONFIG_URL.
     * Returns a JsonObject or null on failure / no endpoint.
     */
    fun fetchRemoteConfig(): JsonObject? {
        val endpoint = try {
            val bcClass = com.asc.markets.BuildConfig::class.java
            val f = bcClass.getDeclaredField("REMOTE_CONFIG_URL")
            val v = f.get(null)
            when (v) {
                is String -> v
                else -> ""
            }
        } catch (t: Throwable) {
            ""
        }
        if (endpoint.isBlank()) return null

        try {
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "remote config fetch failed: HTTP $code")
                lastFetchMillis = System.currentTimeMillis()
                lastFetchSuccess = false
                return null
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = Json.parseToJsonElement(body).jsonObject
            lastFetchMillis = System.currentTimeMillis()
            lastFetchSuccess = true
            return parsed
        } catch (t: Throwable) {
            Log.w(TAG, "fetchRemoteConfig error: ${t.message}")
            lastFetchMillis = System.currentTimeMillis()
            lastFetchSuccess = false
            return null
        }
    }
}
