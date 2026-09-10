package com.asc.markets.data

import android.content.Context
import com.asc.markets.logic.ConnectionState
import com.asc.markets.logic.ConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object SystemLinkMonitor {
    data class Links(val app: Boolean, val ea: Boolean, val ai: Boolean) {
        val allConnected: Boolean get() = app && ea && ai
    }

    private val _state = MutableStateFlow(Links(app = false, ea = false, ai = false))
    val state = _state.asStateFlow()

    private val http = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            while (true) {
                _state.value = Links(
                    app = ConnectivityManager.state.value == ConnectionState.LIVE,
                    ea = EASignalLiveStore.isConnected.value || EALiveDataStore.isConnected.value,
                    ai = isAiReachable(aiServerUrl(appContext))
                )
                delay(5000)
            }
        }
    }

    private fun aiServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("backend_url", "http://${NetworkConfig.DEFAULT_HOST}:5000")
            ?: "http://${NetworkConfig.DEFAULT_HOST}:5000"
    }

    private fun isAiReachable(base: String): Boolean {
        return try {
            val url = base.removeSuffix("/") + "/signals"
            val request = Request.Builder().url(url).get().build()
            http.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}