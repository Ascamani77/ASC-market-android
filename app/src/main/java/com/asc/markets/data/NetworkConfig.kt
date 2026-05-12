package com.asc.markets.data

import android.content.Context

object NetworkConfig {
    const val PREFS_NAME = "asc_prefs"
    const val DEFAULT_HOST = "10.164.138.133"
    const val DEFAULT_BACKEND_URL = "http://10.164.138.133:8000"
    const val DEFAULT_MT5_PORT = 8081
    const val DEFAULT_CTRADER_PORT = 8082
    private const val LEGACY_HOST = "10.95.77.133"
    private const val LEGACY_BACKEND_URL = "http://10.95.77.133:8000"
    private const val PREVIOUS_HOST = "10.151.58.104"
    private const val PREVIOUS_BACKEND_URL = "http://10.151.58.104:8000"

    fun backendUrl(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("backend_url", DEFAULT_BACKEND_URL)?.trim()?.removeSuffix("/")
        return when {
            savedUrl.isNullOrBlank() -> DEFAULT_BACKEND_URL
            savedUrl == LEGACY_BACKEND_URL -> DEFAULT_BACKEND_URL
            savedUrl == PREVIOUS_BACKEND_URL -> DEFAULT_BACKEND_URL
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

    fun cTraderHost(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return normalizedHost(prefs.getString("ctrader_host", DEFAULT_HOST)?.trim()?.ifBlank { DEFAULT_HOST } ?: DEFAULT_HOST)
    }

    fun cTraderPort(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("ctrader_port", DEFAULT_CTRADER_PORT)
    }

    fun cTraderBridgeUrl(context: Context): String = "${cTraderHost(context)}:${cTraderPort(context)}"

    fun normalizedBackendUrl(value: String): String {
        val trimmed = value.trim().ifBlank { DEFAULT_BACKEND_URL }
        return when (trimmed.removeSuffix("/")) {
            LEGACY_BACKEND_URL, PREVIOUS_BACKEND_URL -> DEFAULT_BACKEND_URL
            else -> trimmed.removeSuffix("/")
        }
    }

    fun normalizedHost(value: String): String {
        return when (value.trim()) {
            LEGACY_HOST, PREVIOUS_HOST -> DEFAULT_HOST
            else -> value.trim().ifBlank { DEFAULT_HOST }
        }
    }
}
