package com.asc.markets.data

import android.content.Context
import android.util.Log
import com.asc.markets.logic.PriceStreamManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Live WebSocket store for the ASC EA's chart write-ups.
 *
 * The MT5 bridge pushes the EA's latest signal (ai_signals_mq5.json) over the
 * WebSocket as it changes, so the app receives regime / confidence / MTF /
 * liquidity / entry / zone write-ups live without polling static files.
 */
object EASignalLiveStore {
    private const val TAG = "EASignalLiveStore"
    private const val WS_PORT = 8081

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _signal = MutableStateFlow<ASCSignalData?>(null)
    val signal: StateFlow<ASCSignalData?> = _signal

    private val _signalsByAsset = MutableStateFlow<Map<String, ASCSignalData>>(emptyMap())
    val signalsByAsset: StateFlow<Map<String, ASCSignalData>> = _signalsByAsset

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var scope: CoroutineScope? = null
    private var wsClient: OkHttpClient? = null
    private var ws: WebSocket? = null
    @Volatile
    private var manuallyDisconnected = false

    /** Last set of symbols pushed to the bridge as our watchlist. */
    private val _lastSentWatchlist = MutableStateFlow<Set<String>>(emptySet())

    fun start(context: Context) {
        if (scope != null) return
        val host = NetworkConfig.mt5Host(context)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        manuallyDisconnected = false
        connect("ws://$host:$WS_PORT")
        // Keep the bridge streaming ticks for every EA asset on the Markets page.
        scope?.launch {
            EALiveDataStore.liveAssets.collect { assets ->
                val symbols = assets.map { it.symbol }.filter { it.isNotBlank() }.toSet()
                if (symbols.isNotEmpty() && symbols != _lastSentWatchlist.value) {
                    _lastSentWatchlist.value = symbols
                    sendWatchlistUpdate(symbols)
                }
            }
        }
        Log.d(TAG, "EASignalLiveStore starting for $host:$WS_PORT")
    }

    /** Tell the bridge which symbols to stream real-time ticks for. */
    fun sendWatchlistUpdate(symbols: Set<String>) {
        val socket = ws
        if (socket == null) {
            Log.d(TAG, "sendWatchlistUpdate skipped (not connected): ${symbols.size} symbols")
            return
        }
        try {
            val arr = JSONArray()
            symbols.forEach { arr.put(it) }
            socket.send(
                JSONObject()
                    .put("action", "watchlist_update")
                    .put("symbols", arr)
                    .toString()
            )
            Log.d(TAG, "watchlist_update sent: ${symbols.size} symbols")
        } catch (e: Exception) {
            Log.e(TAG, "sendWatchlistUpdate error", e)
        }
    }

    /** Ask the bridge for the latest captured write-up of a specific asset. */
    fun requestSignal(asset: String) {
        val socket = ws
        if (socket == null) {
            Log.d(TAG, "requestSignal skipped (not connected) for $asset")
            return
        }
        try {
            socket.send(JSONObject().put("action", "get_ea_signal").put("asset", asset).toString())
        } catch (e: Exception) {
            Log.e(TAG, "requestSignal send error", e)
        }
    }

    private fun connect(url: String) {
        try {
            try { ws?.close(1000, "Reconnecting"); ws = null } catch (_: Exception) {}

            // No client ping: the bridge pings every 60s and OkHttp auto-pongs.
            // (Client-side pings were never ponged and just recycled a healthy
            // socket every ~30s, cutting ticks and signal answers mid-flight.)
            wsClient = OkHttpClient.Builder()
                .build()
            val request = Request.Builder().url(url).build()

            ws = wsClient!!.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _isConnected.value = true
                    Log.d(TAG, "Connected to $url")
                    // Request the latest EA signal snapshot immediately.
                    webSocket.send(JSONObject().put("action", "get_ea_signal").toString())
                    // Re-assert the watchlist so ticks stream for all EA assets again.
                    val cached = _lastSentWatchlist.value
                    if (cached.isNotEmpty()) {
                        try {
                            val arr = JSONArray()
                            cached.forEach { arr.put(it) }
                            webSocket.send(
                                JSONObject()
                                    .put("action", "watchlist_update")
                                    .put("symbols", arr)
                                    .toString()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "watchlist_update on reconnect error", e)
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val obj = JSONObject(text)
                        when (obj.optString("type", "").lowercase()) {
                            "ea_signal" -> handleEaSignal(obj)
                            "tick" -> handleTick(obj)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                    }
                }

                private fun handleEaSignal(obj: JSONObject) {
                    val signalObj = obj.optJSONObject("signal") ?: return
                    val parsed = json.decodeFromString<ASCSignalData>(signalObj.toString())
                    _signal.value = parsed
                    val key = normalizeSymbolForMatch(parsed.asset)
                    if (key.isNotEmpty()) {
                        _signalsByAsset.value = _signalsByAsset.value + (key to parsed)
                    }
                }

                private fun handleTick(obj: JSONObject) {
                    val symbol = obj.optString("symbol", "").ifBlank { return }
                    val lastPrice = obj.optDouble("lastPrice", Double.NaN)
                    if (!lastPrice.isFinite() || lastPrice <= 0.0) return
                    val change = obj.optDouble("change", 0.0)
                    val changePercent = obj.optDouble("changePercent", 0.0)

                    PriceStreamManager.updatePrice(symbol, lastPrice)

                    // Mirror onto the EA's canonical symbol (e.g. ETHUSDm) so the
                    // Markets page, which lists EA assets, can pick it up by key.
                    val eaSymbol = EALiveDataStore.liveAssets.value
                        .firstOrNull { MarketDataStore.matchesSymbol(it.symbol, symbol) }
                        ?.symbol
                    if (eaSymbol != null && eaSymbol != symbol) {
                        PriceStreamManager.updatePrice(eaSymbol, lastPrice)
                    }

                    // Keep MarketDataStore prices fresh too.
                    MarketDataStore.updatePair(
                        com.asc.markets.data.ForexPair(
                            symbol = symbol,
                            name = symbol,
                            price = lastPrice,
                            change = change,
                            changePercent = changePercent,
                            category = EALiveDataStore.inferCategory(symbol)
                        )
                    )
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${t.message}")
                    _isConnected.value = false
                    this@EASignalLiveStore.ws = null
                    scheduleReconnect(url)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(TAG, "WebSocket closed: $reason")
                    _isConnected.value = false
                    this@EASignalLiveStore.ws = null
                    scheduleReconnect(url)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
            _isConnected.value = false
        }
    }

    fun stop() {
        manuallyDisconnected = true
        try { ws?.close(1000, "Normal closure") } catch (_: Exception) {}
        ws = null
        _isConnected.value = false
    }

    private fun scheduleReconnect(url: String) {
        val current = scope ?: return
        current.launch {
            delay(5000)
            if (!manuallyDisconnected) connect(url)
        }
    }

    private fun normalizeSymbolForMatch(symbol: String): String = symbol
        .uppercase()
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
        .replace(".", "")
        .removeSuffix("M")
}
