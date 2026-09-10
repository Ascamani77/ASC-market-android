package com.asc.markets.data

import android.content.Context
import com.asc.markets.BuildConfig

object NetworkConfig {
    const val PREFS_NAME = "asc_prefs"
    val DEFAULT_BACKEND_URL: String = BuildConfig.DEFAULT_BACKEND_URL.removeSuffix("/")
    val DEFAULT_HOST: String = DEFAULT_BACKEND_URL
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore(":")
    val DEFAULT_SCANNER_URL: String = "http://$DEFAULT_HOST:5000"
    const val DEFAULT_MT5_PORT = 8081

    private const val LEGACY_HOST = "10.95.77.133"
    private const val LEGACY_BACKEND_URL = "http://10.95.77.133:8000"
    private const val PREVIOUS_HOST = "10.151.58.104"
    private const val PREVIOUS_BACKEND_URL = "http://10.151.58.104:8000"
    private const val OLD_HOST = "192.168.1.199"
    private const val OLD_BACKEND_URL = "http://192.168.1.199:8001"

    fun backendUrl(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("backend_url", DEFAULT_BACKEND_URL)?.trim()?.removeSuffix("/")
        return when {
            savedUrl.isNullOrBlank() -> DEFAULT_BACKEND_URL
            savedUrl == LEGACY_BACKEND_URL -> DEFAULT_BACKEND_URL
            savedUrl == PREVIOUS_BACKEND_URL -> DEFAULT_BACKEND_URL
            savedUrl == OLD_BACKEND_URL -> DEFAULT_BACKEND_URL
            else -> savedUrl
        }
    }

    fun mt5Host(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedHost = prefs.getString("mt5_host", null)?.trim()
        if (!savedHost.isNullOrBlank()) {
            val result = normalizedHost(savedHost)
            android.util.Log.d("NetworkConfig", "mt5Host from saved prefs: $result (original: $savedHost)")
            return result
        }

        val legacyBridgeUrl = prefs.getString("mt5_bridge_url", null)?.trim()
        val legacyHost = legacyBridgeUrl
            ?.removePrefix("ws://")
            ?.removePrefix("http://")
            ?.removePrefix("https://")
            ?.substringBefore(":")
        val result = when {
            legacyHost.isNullOrBlank() -> DEFAULT_HOST
            else -> normalizedHost(legacyHost)
        }
        android.util.Log.d("NetworkConfig", "mt5Host from legacy bridge URL: $result (legacy: $legacyBridgeUrl)")
        return result
    }

    fun scannerBaseUrl(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("scanner_url", null)?.trim()?.removeSuffix("/")
        if (!saved.isNullOrBlank()) return saved
        val host = backendUrl(context)
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore(":")
        return "http://$host:5000"
    }

    fun mt5Port(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains("mt5_port")) {
            return prefs.getInt("mt5_port", DEFAULT_MT5_PORT)
        }

        val legacyBridgeUrl = prefs.getString("mt5_bridge_url", null)?.trim()
        return legacyBridgeUrl
            ?.removePrefix("ws://")
            ?.removePrefix("http://")
            ?.removePrefix("https://")
            ?.substringAfterLast(":", "")
            ?.toIntOrNull()
            ?: DEFAULT_MT5_PORT
    }

    fun mt5BridgeUrl(context: Context): String = "${mt5Host(context)}:${mt5Port(context)}"



    fun normalizedBackendUrl(value: String): String {
        val trimmed = value.trim().ifBlank { DEFAULT_BACKEND_URL }
        return when (trimmed.removeSuffix("/")) {
            LEGACY_BACKEND_URL, PREVIOUS_BACKEND_URL -> DEFAULT_BACKEND_URL
            else -> trimmed.removeSuffix("/")
        }
    }

    fun normalizedHost(value: String): String {
        return when (value.trim()) {
            LEGACY_HOST, PREVIOUS_HOST, OLD_HOST -> DEFAULT_HOST
            else -> value.trim().ifBlank { DEFAULT_HOST }
        }
    }
}
