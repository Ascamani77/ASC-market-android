package com.asc.markets.logic

import android.content.Context
import android.util.Log
import com.asc.markets.data.ASCSignalData
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.data.remote.FinalDecisionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class AutoTradeSignalSource { EA, AI, COMBINED }

/**
 * Per-asset parameters, editable in the Auto Trade screen before the engine
 * triggers a trade. Replaces the global config for [AssetParams]' own fields;
 * engine-level limits (max positions/day) stay global.
 */
data class AssetParams(
    val entryModels: Set<String>,
    val smcTechniques: Set<String>,
    val sessions: Set<String>,
    val rrMultiplier: Double,
    val minEAScore: Int,
    val minAiScore: Int,
    val minConfidence: Int,
    val riskPerTradePct: Double,
    val newsFilter: Boolean,
    val spreadFilter: Boolean,
    val maxSpreadPips: Double
) {
    companion object {
        fun fromConfig(cfg: AutoTradeConfig) = AssetParams(
            entryModels = cfg.entryModels,
            smcTechniques = cfg.smcTechniques,
            sessions = cfg.sessions,
            rrMultiplier = cfg.rrMultiplier,
            minEAScore = cfg.minEAScore,
            minAiScore = cfg.minAiScore,
            minConfidence = cfg.minConfidence,
            riskPerTradePct = cfg.riskPerTradePct,
            newsFilter = cfg.newsFilter,
            spreadFilter = cfg.spreadFilter,
            maxSpreadPips = cfg.maxSpreadPips
        )
    }
}

data class AutoTradeConfig(
    val assets: Set<String>,
    val minEAScore: Int,
    val minAiScore: Int,
    val minConfidence: Int,
    val riskPerTradePct: Double,
    val maxDailyTrades: Int,
    val maxOpenPositions: Int,
    val newsFilter: Boolean,
    val spreadFilter: Boolean,
    val maxSpreadPips: Double,
    val entryModels: Set<String>,
    val smcTechniques: Set<String>,
    val sessions: Set<String>,
    val rrMultiplier: Double,
    val signalSource: AutoTradeSignalSource = AutoTradeSignalSource.COMBINED,
    val perAsset: Map<String, AssetParams> = emptyMap()
)

data class EngineAccount(
    val balance: Double = 0.0,
    val equity: Double = 0.0,
    val unrealizedPnl: Double = 0.0,
    val margin: Double = 0.0,
    val availableFunds: Double = 0.0
)

data class EnginePosition(
    val ticket: Long,
    val symbol: String,
    val type: String,
    val entryPrice: Double,
    val volume: Double,
    val tp: Double?,
    val sl: Double?,
    val profit: Double
)

data class EngineLogEntry(
    val time: Long,
    val message: String,
    val ok: Boolean
)

/**
 * Live auto-trade engine. Reads the EA's live signals from [EASignalLiveStore]
 * and/or the backend AI deployments, qualifies them against the active
 * [AutoTradeConfig], and executes market orders on the MT5 bridge WebSocket
 * (port 8081).
 *
 * Signal sources:
 *  - [AutoTradeSignalSource.EA]       → EA validator vote (chart panel) only.
 *  - [AutoTradeSignalSource.AI]       → Backend AI journal score only.
 *  - [AutoTradeSignalSource.COMBINED] → Both must be present, directions must
 *    agree, and the average score must pass the threshold.
 *
 * Order results come back as `order_result` messages; filled tickets are
 * tracked so the engine can report and (optionally) flatten its own exposure.
 */
object AutoTradeEngine {
    private const val TAG = "AutoTradeEngine"
    private const val WS_PORT = 8081
    private const val SCAN_INTERVAL_MS = 3000L
    private const val AI_POLL_INTERVAL_MS = 20_000L
    private const val MAX_SIGNAL_AGE_MS = 120_000L
    private const val MAX_AI_AGE_MS = 300_000L
    private const val ENTRY_COOLDOWN_MS = 60_000L
    private const val MAX_LOG_ENTRIES = 60

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeSymbols = MutableStateFlow<Set<String>>(emptySet())

    /** Normalized key(s) the auto-trade engine is currently monitoring. */
    val activeSymbols: StateFlow<Set<String>> = _activeSymbols.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _account = MutableStateFlow(EngineAccount())
    val account: StateFlow<EngineAccount> = _account.asStateFlow()

    private val _positions = MutableStateFlow<List<EnginePosition>>(emptyList())
    val positions: StateFlow<List<EnginePosition>> = _positions.asStateFlow()

    private val _autoOpenTickets = MutableStateFlow<Set<Long>>(emptySet())
    val autoOpenTickets: StateFlow<Set<Long>> = _autoOpenTickets.asStateFlow()

    private val _tradesToday = MutableStateFlow(0)
    val tradesToday: StateFlow<Int> = _tradesToday.asStateFlow()

    private val _aiDecisions = MutableStateFlow<Map<String, FinalDecisionItem>>(emptyMap())
    val aiDecisions: StateFlow<Map<String, FinalDecisionItem>> = _aiDecisions.asStateFlow()

    private val _log = MutableStateFlow<List<EngineLogEntry>>(emptyList())
    val log: StateFlow<List<EngineLogEntry>> = _log.asStateFlow()

    @Volatile
    private var config: AutoTradeConfig? = null

    private val autoTickets = mutableSetOf<Long>()
    private val cooldowns = mutableMapOf<String, Long>()
    private val inFlight = mutableSetOf<String>()
    private var dayStamp = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    private var wsClient: OkHttpClient? = null
    private var ws: WebSocket? = null
    private var host: String = ""
    private var aiPollerJob: Job? = null
    private var reconnectDelayMs = 3000L

    fun start(context: Context, config: AutoTradeConfig) {
        if (config.assets.isEmpty()) return
        this.config = config
        host = NetworkConfig.mt5Host(context)
        AiRetrofitClient.configure(NetworkConfig.scannerBaseUrl(context))
        connect()
        if (!_isRunning.value) {
            _isRunning.value = true
            _activeSymbols.value = config.assets.map { normalizeKey(it) }.toSet()
            pushAutoTradeState(config.assets.toList())
            val mode = when (config.signalSource) {
                AutoTradeSignalSource.EA -> "EA"
                AutoTradeSignalSource.AI -> "AI"
                AutoTradeSignalSource.COMBINED -> "EA+AI COMBINED"
            }
            log("Engine started — $mode · ${config.assets.size} asset(s), EA ≥ ${config.minEAScore}% / AI ≥ ${config.minAiScore}% · conf ≥ ${config.minConfidence}%", true)
            startAiPoller(config.signalSource)
            scope.launch {
                while (_isRunning.value && isActive) {
                    try {
                        monitorCycle()
                    } catch (e: Exception) {
                        Log.e(TAG, "Monitor cycle error", e)
                    }
                    delay(SCAN_INTERVAL_MS)
                }
            }
        }
    }

    fun stop() {
        if (!_isRunning.value) return
        _isRunning.value = false
        _activeSymbols.value = emptySet()
        pushAutoTradeState(emptyList(), enabled = false)
        aiPollerJob?.cancel()
        aiPollerJob = null
        config = null
        inFlight.clear()
        log("Engine stopped — no new entries. Open positions remain.", true)
    }

    /** Flatten every position this engine opened in this session. */
    fun closeAllAuto() {
        val tickets = autoTickets.toList()
        if (tickets.isEmpty()) {
            log("No tracked auto positions to close.", false)
            return
        }
        tickets.forEach { sendClose(it) }
        log("Closing ${tickets.size} auto position(s)...", true)
    }

    // ─── BRIDGE WEBSOCKET ───────────────────────────────────────────────────

    private fun connect() {
        try {
            runCatching { wsClient?.dispatcher?.executorService?.shutdown() }
            runCatching { ws?.close(1000, null) }
            ws = null
            if (_connected.value) _connected.value = false
            val client = OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .build()
            wsClient = client
            val request = Request.Builder().url("ws://$host:$WS_PORT").build()
            ws = client.newWebSocket(request, bridgeListener)
        } catch (e: Exception) {
            Log.e(TAG, "connect error", e)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(reconnectDelayMs)
            if (_isRunning.value) {
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
                connect()
            }
        }
    }

    private val bridgeListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connected.value = true
            reconnectDelayMs = 3000L
            log("MT5 bridge connected", true)
            send(JSONObject().put("action", "get_account"))
            if (_isRunning.value) config?.assets?.toList()?.let { pushAutoTradeState(it) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                handleMessage(text)
            } catch (e: Exception) {
                Log.e(TAG, "message parse error: ${e.message}")
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connected.value = false
            log("MT5 bridge disconnected ($code ${reason.takeIf { it.isNotBlank() } ?: ""})", false)
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connected.value = false
            log("MT5 bridge disconnected — ${t.message}", false)
            Log.e(TAG, "bridge failure: ${t.message}")
            scheduleReconnect()
        }
    }

    private fun handleMessage(text: String) {
        val json = JSONObject(text)
        if (json.has("auto_trade_ack")) {
            log("Auto-trade state synced to MT5 bridge (${json.optString("enabled")} / ${json.optString("assets")})", true)
            return
        }
        if (json.has("order_result")) {
            handleOrderResult(json.getJSONObject("order_result"))
            return
        }
        if (json.has("close_result")) {
            handleCloseResult(json.getJSONObject("close_result"))
            return
        }
        when (json.optString("type", "")) {
            "account" -> handleAccount(json)
            "positions" -> handlePositions(json.getJSONArray("data"))
        }
    }

    private fun handleAccount(acc: JSONObject) {
        val balance = acc.optDouble("balance", 0.0)
        val equity = acc.optDouble("equity", balance)
        val margin = acc.optDouble("margin", 0.0)
        val free = acc.optDouble("availableFunds", 0.0)
        val unrealized = acc.optDouble("unrealizedPnl", 0.0)
        _account.value = EngineAccount(
            balance = balance,
            equity = equity,
            unrealizedPnl = unrealized,
            margin = margin,
            availableFunds = if (free != 0.0) free else equity - margin
        )
    }

    private fun handlePositions(arr: JSONArray) {
        val list = mutableListOf<EnginePosition>()
        val liveTickets = mutableSetOf<Long>()
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val ticket = p.optLong("ticket", 0L)
            if (ticket == 0L) continue
            liveTickets.add(ticket)
            val type = p.optString("type", "buy")
            val tp = p.optDouble("tp", 0.0).takeIf { it > 0 }
            val sl = p.optDouble("sl", 0.0).takeIf { it > 0 }
            list += EnginePosition(
                ticket = ticket,
                symbol = p.optString("symbol", ""),
                type = if (type == "0" || type.equals("buy", true)) "buy" else "sell",
                entryPrice = p.optDouble("price_open", 0.0),
                volume = p.optDouble("volume_current", 0.0),
                tp = tp,
                sl = sl,
                profit = p.optDouble("profit", 0.0)
            )
        }
        _positions.value = list
        _autoOpenTickets.value = autoTickets.intersect(liveTickets)

        val closedExternally = autoTickets.filter { it !in liveTickets }
        if (closedExternally.isNotEmpty()) {
            autoTickets.removeAll(closedExternally.toSet())
            log("Position closed externally — ticket #${closedExternally.first()}", true)
        }
    }

    private fun handleOrderResult(res: JSONObject) {
        val status = res.optString("status", "failed")
        if (status == "success") {
            val ticket = res.optLong("ticket", 0L)
            autoTickets.add(ticket)
            _tradesToday.value = _tradesToday.value + 1
            inFlight.clear()
            log("Order filled — ticket #$ticket @ ${res.optDouble("price", 0.0).format5()} (vol ${res.optDouble("volume", 0.0).format2()})", true)
        } else {
            inFlight.clear()
            log("Order rejected by bridge: ${res.optString("error", "unknown")}", false)
        }
    }

    private fun handleCloseResult(res: JSONObject) {
        val ticket = res.optLong("ticket", 0L)
        autoTickets.remove(ticket)
        val status = res.optString("status", "failed")
        log(if (status == "success") "Closed position #$ticket" else "Close failed for #$ticket: ${res.optString("error", "")}", status == "success")
    }

    // ─── SIGNAL SOURCES ──────────────────────────────────────────────────────

    private fun startAiPoller(source: AutoTradeSignalSource) {
        if (source == AutoTradeSignalSource.EA) return
        aiPollerJob = scope.launch {
            while (_isRunning.value && isActive) {
                try {
                    val response = AiRetrofitClient.api.getHybridSignals()
                    if (response.signals.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        _aiDecisions.value = response.signals
                            .filter { !it.asset.isNullOrBlank() && it.ts != null }
                            .map { s ->
                                val dir = s.direction?.trim()?.uppercase() ?: "NONE"
                                FinalDecisionItem(
                                    asset_1 = s.asset,
                                    journal_direction = dir,
                                    journal_score = (s.confidence ?: 0.0) * 100.0,
                                    journal_timestamp_utc = (s.ts!! * 1000.0).toLong().coerceAtMost(now),
                                    final_trade_state = if (dir == "NONE") "STANDBY" else "TRADE",
                                    entry_state = "HIGH",
                                    entry_quality_score = (s.p_trade ?: 0.0) * 100.0,
                                    monitoring_confidence = s.combined ?: 0.0,
                                    generated_at = java.text.SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US
                                    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(now)
                                )
                            }
                            .associateBy { normalizeKey(it.asset_1!!) }
                        log("AI feed: ${_aiDecisions.value.size} asset(s) · ${_aiDecisions.value.keys.joinToString(", ")}", true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AI poll error: ${e.message}")
                }
                delay(AI_POLL_INTERVAL_MS)
            }
        }
    }

    // ─── SCAN LOOP ───────────────────────────────────────────────────────────

    private fun monitorCycle() {
        val cfg = config ?: return
        val signalMap = EASignalLiveStore.signalsByAsset.value
        val openKeys = _positions.value.mapTo(mutableSetOf()) { normalizeKey(it.symbol) }
        val now = System.currentTimeMillis()
        resetDailyCounterIfNeeded()

        for (asset in cfg.assets) {
            val key = normalizeKey(asset)
            val params = cfg.perAsset[asset] ?: AssetParams.fromConfig(cfg)
            if (!preChecks(cfg, key, openKeys, now, params)) continue
            when (cfg.signalSource) {
                AutoTradeSignalSource.EA -> tryEaEntry(cfg, params, asset, key, signalMap[key], now)
                AutoTradeSignalSource.AI -> tryAiEntry(cfg, params, asset, key, now)
                AutoTradeSignalSource.COMBINED -> tryCombinedEntry(cfg, params, asset, key, signalMap[key], now)
            }
        }
    }

    private fun preChecks(cfg: AutoTradeConfig, key: String, openKeys: Set<String>, now: Long, params: AssetParams): Boolean {
        if (!_isRunning.value) return false
        if (key in cooldowns && now - (cooldowns[key] ?: 0) < ENTRY_COOLDOWN_MS) return false
        if (key in inFlight) return false
        if (key in openKeys) return false
        if (_positions.value.size >= cfg.maxOpenPositions) return false
        if (_tradesToday.value >= cfg.maxDailyTrades) return false
        if (!sessionAllows(params.sessions)) return false
        return true
    }

    // ─── EA PATH ─────────────────────────────────────────────────────────────

    private fun tryEaEntry(cfg: AutoTradeConfig, params: AssetParams, asset: String, key: String, signal: ASCSignalData?, now: Long) {
        if (signal == null) return
        if (now - signal.timestamp > MAX_SIGNAL_AGE_MS) return

        val panel = signal.chart_panel ?: return
        if (!panel.validator_active) return
        val side = eaSide(panel.validator_direction) ?: return

        val votePct = (panel.votes?.win_pct ?: 0.0).let { if (it > 1.0) it else it * 100.0 }
        if (votePct < params.minEAScore) {
            log("$asset — EA vote ${votePct.round1()}% < ${params.minEAScore}% (skip)", false)
            return
        }
        val confPct = (signal.validation?.confidence ?: signal.confidence).let { it * 100.0 }
        if (confPct < params.minConfidence) {
            log("$asset — EA confidence ${confPct.round1()}% < ${params.minConfidence}% (skip)", false)
            return
        }
        if (!entryStateAllowed(signal.entry?.state)) return
        if (!filtersPass(params, asset, signal.validation)) return

        val sl = signal.trade_params?.stop_loss ?: 0.0
        var tp = signal.trade_params?.take_profit ?: 0.0
        val price = resolveLivePrice(asset, key, signal.asset)
        if (price <= 0.0) return
        if (sl <= 0.0) return
        if (!saneLevels(side, price, sl, tp)) return
        if (tp <= 0.0) tp = deriveTp(params, side, price, sl)

        launchOrder(cfg, params, key, signal.asset.ifBlank { asset }, side, price, sl, tp, "EA", votePct, confPct, "ASC EA AUTO")
    }

    // ─── AI PATH ─────────────────────────────────────────────────────────────

    private fun tryAiEntry(cfg: AutoTradeConfig, params: AssetParams, asset: String, key: String, now: Long) {
        val ai = _aiDecisions.value[key] ?: return
        if (now - aiAgeMs(ai) > MAX_AI_AGE_MS) return
        if ((ai.final_trade_state ?: "").equals("REJECTED", true)) return

        val side = aiSide(ai.journal_direction) ?: return
        val score = ai.journal_score ?: 0.0
        if (score < params.minAiScore) {
            log("$asset — AI score ${score.round1()}% < ${params.minAiScore}% (skip)", false)
            return
        }
        if (!entryStateAllowed(ai.entry_state)) return
        // Ignore news/spread gates for AI-only: they need EA validation data.
        if (params.newsFilter || params.spreadFilter) {
            log("$asset — news/spread filters require EA validation (skip AI-only)", false)
            return
        }

        val price = resolveLivePrice(asset, key, asset)
        if (price <= 0.0) return
        val fallback = fallbackLevels(side, price)
        val sl = fallback.first
        var tp = fallback.second
        if (tp <= 0.0) tp = deriveTp(params, side, price, sl)

        launchOrder(cfg, params, key, asset, side, price, sl, tp, "AI", score, score, "ASC AI AUTO")
    }

    // ─── COMBINED PATH ───────────────────────────────────────────────────────

    private fun tryCombinedEntry(cfg: AutoTradeConfig, params: AssetParams, asset: String, key: String, signal: ASCSignalData?, now: Long) {
        val ai = _aiDecisions.value[key] ?: return
        if (signal == null) return
        if (now - signal.timestamp > MAX_SIGNAL_AGE_MS) return
        if (now - aiAgeMs(ai) > MAX_AI_AGE_MS) return
        if ((ai.final_trade_state ?: "").equals("REJECTED", true)) return

        val panel = signal.chart_panel ?: return
        if (!panel.validator_active) return
        val eaBuy = eaSide(panel.validator_direction)
        val aiBuy = aiSide(ai.journal_direction)
        if (eaBuy == null || aiBuy == null || eaBuy != aiBuy) {
            log("$asset — EA ${panel.validator_direction} vs AI ${ai.journal_direction} (veto, skip)", false)
            return
        }
        val side = eaBuy

        val votePct = (panel.votes?.win_pct ?: 0.0).let { if (it > 1.0) it else it * 100.0 }
        val aiScore = ai.journal_score ?: 0.0
        val combined = (votePct + aiScore) / 2.0
        if (votePct < params.minEAScore || aiScore < params.minAiScore) {
            log("$asset — gates not met: EA ${votePct.round1()}% (≥${params.minEAScore}%) / AI ${aiScore.round1()}% (≥${params.minAiScore}%) — combined ${combined.round1()}% (skip)", false)
            return
        }
        val confPct = (signal.validation?.confidence ?: signal.confidence).let { it * 100.0 }
        if (confPct < params.minConfidence) {
            log("$asset — confidence ${confPct.round1()}% < ${params.minConfidence}% (skip)", false)
            return
        }
        if (!entryStateAllowed(signal.entry?.state)) return
        if (!filtersPass(params, asset, signal.validation)) return

        val sl = signal.trade_params?.stop_loss ?: 0.0
        var tp = signal.trade_params?.take_profit ?: 0.0
        val price = resolveLivePrice(asset, key, signal.asset)
        if (price <= 0.0) return

        val finalSl = if (sl > 0.0) sl else fallbackLevels(side, price).first
        if (finalSl <= 0.0) return
        if (!saneLevels(side, price, finalSl, tp)) return
        if (tp <= 0.0) tp = deriveTp(params, side, price, finalSl)

        launchOrder(cfg, params, key, signal.asset.ifBlank { asset }, side, price, finalSl, tp, "EA+AI", combined, confPct, "ASC EA+AI AUTO")
    }

    // ─── ORDER EXECUTION ─────────────────────────────────────────────────────

    private fun launchOrder(
        cfg: AutoTradeConfig,
        params: AssetParams,
        key: String,
        brokerSymbol: String,
        side: String,
        price: Double,
        sl: Double,
        tp: Double,
        sourceLabel: String,
        score: Double,
        conf: Double,
        comment: String
    ) {
        val account = _account.value
        val base = if (account.equity > 0) account.equity else account.balance
        val volume = riskVolume(base, params.riskPerTradePct, price, sl, brokerSymbol)
        if (volume <= 0.0) return

        val payload = JSONObject()
            .put("action", "place_order")
            .put("symbol", brokerSymbol)
            .put("type", side)
            .put("orderType", "market")
            .put("volume", volume)
            .put("comment", comment)
        if (sl > 0) payload.put("sl", sl)
        if (tp > 0) payload.put("tp", tp)

        val sent = send(payload)
        if (sent) {
            inFlight.add(key)
            cooldowns[key] = System.currentTimeMillis()
            log("$key → ${side.uppercase()} ${volume.format2()} lots @ ${price.format5()} | SL ${sl.format5()} TP ${tp.format5()} | [$sourceLabel] ${score.round1()}% conf ${conf.round1()}%", true)
        } else {
            log("$key — bridge not connected, order dropped", false)
        }
    }

    private fun sendClose(ticket: Long) {
        if (ws == null) return
        try {
            ws?.send(JSONObject().put("action", "close_position").put("ticket", ticket).toString())
        } catch (e: Exception) {
            Log.e(TAG, "close_position send error", e)
        }
    }

    private fun send(payload: JSONObject): Boolean {
        val socket = ws
        if (socket == null) return false
        return try {
            socket.send(payload.toString())
        } catch (e: Exception) {
            Log.e(TAG, "send error", e)
            false
        }
    }

    /** Tell the MT5 bridge / EA which assets this app is auto-trading. */
    private fun pushAutoTradeState(assets: List<String>, enabled: Boolean = _isRunning.value) {
        send(
            JSONObject()
                .put("action", "auto_trade_state")
                .put("enabled", enabled)
                .put("assets", assets)
        )
    }

    // ─── QUALIFIER HELPERS ───────────────────────────────────────────────────

    private fun eaSide(direction: String): String? = when (direction.trim().uppercase()) {
        "LONG", "BUY", "BULLISH" -> "buy"
        "SHORT", "SELL", "BEARISH" -> "sell"
        else -> null
    }

    private fun aiSide(direction: String?): String? = when (direction?.trim()?.uppercase()) {
        "LONG", "BUY", "BULLISH" -> "buy"
        "SHORT", "SELL", "BEARISH" -> "sell"
        else -> null
    }

    private fun entryStateAllowed(state: String?): Boolean = when (state?.uppercase() ?: "") {
        "OPTIMAL", "GOOD", "HIGH", "STRONG" -> true
        else -> false
    }

    private fun filtersPass(params: AssetParams, asset: String, validation: com.asc.markets.data.ASCValidationData?): Boolean {
        if (params.spreadFilter) {
            if (validation == null || !validation.spread_ok) {
                log("$asset — spread filter active but validation not OK (skip)", false)
                return false
            }
        }
        if (params.newsFilter) {
            if (validation == null || !validation.timing_ok) {
                log("$asset — news filter active but validation not OK (skip)", false)
                return false
            }
        }
        return true
    }

    private fun saneLevels(side: String, price: Double, sl: Double, tp: Double): Boolean =
        if (side == "buy") sl < price && (tp <= 0.0 || tp > price)
        else sl > price && (tp <= 0.0 || tp < price)

    private fun deriveTp(params: AssetParams, side: String, price: Double, sl: Double): Double {
        if (params.rrMultiplier <= 0 || params.entryModels.contains("Custom")) return 0.0
        return if (side == "buy") price + (price - sl) * params.rrMultiplier
        else price - (sl - price) * params.rrMultiplier
    }

    private fun fallbackLevels(side: String, price: Double): Pair<Double, Double> {
        val stopPct = fallbackStopPct()
        val sl = if (side == "buy") price * (1 - stopPct) else price * (1 + stopPct)
        return sl to 0.0
    }

    /** AI-only has no SL/TP; use a conservative percentage stop. */
    private fun fallbackStopPct(): Double = 0.004

    private fun aiAgeMs(ai: FinalDecisionItem): Long {
        ai.journal_timestamp_utc?.let { if (it > 0) return it }
        val generated = ai.generated_at ?: ""
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(generated)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun resolveLivePrice(display: String, key: String, brokerSymbol: String): Double {
        val candidates = listOfNotNull(display, display.replace("/", ""), brokerSymbol, key + "M", key)
        for (c in candidates) {
            PriceStreamManager.getPrice(c)?.let { if (it > 0.0) return it }
        }
        return 0.0
    }

    /** Approximate USD risk per 1.0 price-point move for one lot. */
    private fun contractNotional(symbol: String): Double {
        val s = symbol.uppercase()
        return when {
            s.contains("BTC") || s.contains("ETH") || s.contains("USDT") -> 1.0
            s.contains("XAU") -> 100.0
            s.contains("XAG") || s.contains("WTICO") || s.contains("OIL") || s.contains("NGAS") -> 1000.0
            s.contains("USD") && s.length <= 7 -> 100_000.0
            s.contains("US30") || s.contains("US500") || s.contains("NAS100") || s.contains("GER40") || s.contains("UK100") || s.contains("JP225") -> 10.0
            else -> 100.0
        }
    }

    private fun riskVolume(base: Double, riskPct: Double, price: Double, sl: Double, symbol: String): Double {
        if (base <= 0) return 0.0
        if (riskPct <= 0) return 0.01
        val riskAmount = base * (riskPct / 100.0)
        val distance = abs(price - sl)
        if (distance <= 0) return 0.01
        val notional = contractNotional(symbol)
        val raw = riskAmount / (distance * notional)
        val clamped = raw.coerceIn(0.01, 50.0)
        return Math.round(clamped * 100.0) / 100.0
    }

    private fun sessionAllows(sessions: Set<String>): Boolean {
        if (sessions.isEmpty()) return true
        val names = sessions.map { it.uppercase() }
        if (names.any { it.contains("ALL") || it.contains("CUSTOM") }) return true
        val hour = Calendar.getInstance().apply { timeZone = TimeZone.getTimeZone("UTC") }.get(Calendar.HOUR_OF_DAY)
        val inLondon = hour in 7..15 && "LONDON" in names
        val inNy = hour in 13..21 && names.any { it.contains("NEW YORK") || it == "NEW_YORK" }
        val inAsia = (hour >= 23 || hour <= 7) && "ASIA" in names
        val inSydney = (hour >= 22 || hour <= 6) && "SYDNEY" in names
        return inLondon || inNy || inAsia || inSydney
    }

    private fun resetDailyCounterIfNeeded() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (today != dayStamp) {
            dayStamp = today
            _tradesToday.value = 0
        }
    }

    fun normalizeKey(symbol: String): String = symbol
        .uppercase()
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
        .replace(".", "")
        .removeSuffix("M")

    private fun log(message: String, ok: Boolean) {
        _log.value = (listOf(EngineLogEntry(System.currentTimeMillis(), message, ok)) + _log.value).take(MAX_LOG_ENTRIES)
        Log.d(TAG, message)
    }

    private fun Double.round1(): String = String.format("%.1f", this)
    private fun Double.format2(): String = String.format("%.2f", this)
    private fun Double.format5(): String = String.format("%.5f", this)
}