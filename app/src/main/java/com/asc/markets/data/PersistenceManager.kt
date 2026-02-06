package com.asc.markets.data

import android.content.Context
import android.content.SharedPreferences
import com.asc.markets.security.SecurityManager

/**
 * Institutional Secure Storage Bridge.
 * Parity with persistenceManager.ts keys and logic.
 */
class PersistenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("asc_secure_prefs", Context.MODE_PRIVATE)

    fun secureSave(key: String, value: String) {
        // Defensive guard: block attempts to persist potential API keys or tokens via this generic secure API.
        val forbidden = Regex("(?i)(openai|api[_-]?key|api-?key|secret|token)")
        if (forbidden.containsMatchIn(key)) {
            android.util.Log.w("PersistenceManager", "Blocked attempt to save potential API key into secure prefs for key='${key}'")
            return
        }

        val encrypted = SecurityManager.encrypt(value)
        prefs.edit().putString(key, encrypted).apply()
    }

    fun secureLoad(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return SecurityManager.decrypt(encrypted)
    }

    // Parity: Key format asc_state_${symbol.replace('/', '_')}
    fun getChartStateKey(symbol: String): String {
        return "asc_state_${symbol.replace("/", "_")}"
    }

    fun saveChartState(symbol: String, jsonState: String) {
        secureSave(getChartStateKey(symbol), jsonState)
    }

    fun loadChartState(symbol: String): String? {
        return secureLoad(getChartStateKey(symbol))
    }
}