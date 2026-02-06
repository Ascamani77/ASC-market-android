package com.asc.markets.logic

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.api.ForexAnalysisEngine
import com.asc.markets.data.*
import com.asc.markets.data.ForexDataPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.launch
import com.asc.markets.data.MacroEvent
import com.asc.markets.data.PersistenceManager
import com.asc.markets.data.AuditRecord
    
import com.asc.markets.data.TelemetryManager
import com.asc.markets.data.MacroEventStatus
import com.asc.markets.data.ImpactPriority
import com.asc.markets.BuildConfig

class ForexViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentView = MutableStateFlow(AppView.DASHBOARD)
    val currentView = _currentView.asStateFlow()

    // Track previous view to allow back-navigation from screens like POST_MOVE_AUDIT
    private val _previousView = MutableStateFlow<AppView?>(null)
    val previousView = _previousView.asStateFlow()

    private val _isSidebarCollapsed = MutableStateFlow(false)
    val isSidebarCollapsed = _isSidebarCollapsed.asStateFlow()
    
    // Drawer state for modal (hidden-by-default) sidebar on mobile
    // Start closed by default so a single explicit open action reliably opens the drawer
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen = _isDrawerOpen.asStateFlow()

    private val _isCommandPaletteOpen = MutableStateFlow(false)
    val isCommandPaletteOpen = _isCommandPaletteOpen.asStateFlow()

    private val _marketState = MutableStateFlow<MarketState?>(null)
    val marketState = _marketState.asStateFlow()

    private val _isRiskAccepted = MutableStateFlow(false)
    val isRiskAccepted = _isRiskAccepted.asStateFlow()

    private val _selectedPair = MutableStateFlow(FOREX_PAIRS[0])
    val selectedPair = _selectedPair.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    private val _isArmed = MutableStateFlow(false)
    val isArmed = _isArmed.asStateFlow()

    // Feature flag: when true, Macro Intelligence Stream is promoted as a landing/highlight view
    private val _promoteMacroStream = MutableStateFlow(false)
    val promoteMacroStream = _promoteMacroStream.asStateFlow()

    // Dashboard tab target (string name of DashboardTab) allows external callers to set which
    // top-tab the Dashboard should show when navigated to (e.g., Home button -> MACRO_STREAM)
    private val _dashboardTabTarget = MutableStateFlow("MACRO_STREAM")
    val dashboardTabTarget = _dashboardTabTarget.asStateFlow()

    private val _activeAlgo = MutableStateFlow("MARKET")
    val activeAlgo = _activeAlgo.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(role = "model", content = "[SYSTEM BOOT] Local Analytical Node initialized. Protocol L14 active.")
    ))
    val terminalLogs = _terminalLogs.asStateFlow()

    // Macro events source (can be updated by network or local ingestion)
    private val _allMacroEvents = MutableStateFlow<List<MacroEvent>>(com.asc.markets.data.sampleMacroEvents())
    val allMacroEvents = _allMacroEvents.asStateFlow()

    // Filtered list intended for the MacroStream view — ensure ~90% UPCOMING vs CONFIRMED
    private val _macroStreamEvents = MutableStateFlow<List<MacroEvent>>(computeMacroStreamList(_allMacroEvents.value))
    val macroStreamEvents = _macroStreamEvents.asStateFlow()

    // Basic telemetry/metrics for rollout monitoring
    private val _sessionLandingCount = MutableStateFlow(0)
    val sessionLandingCount = _sessionLandingCount.asStateFlow()

    private val _clicksToExecutionCount = MutableStateFlow(0)
    val clicksToExecutionCount = _clicksToExecutionCount.asStateFlow()

    private val _ingestionDroppedCount = MutableStateFlow(0)
    val ingestionDroppedCount = _ingestionDroppedCount.asStateFlow()

    // Post-move audit ledger
    private val _auditLog = MutableStateFlow<List<com.asc.markets.data.AuditRecord>>(listOf())
    val auditLog = _auditLog.asStateFlow()

    private val _userOverrideCount = MutableStateFlow(0)
    val userOverrideCount = _userOverrideCount.asStateFlow()

    // Execution opt-in flow: UI shows education modal when this is true
    private val _executionOptInRequested = MutableStateFlow(false)
    val executionOptInRequested = _executionOptInRequested.asStateFlow()

    // Hold the target view the user attempted to access so we can navigate after opt-in
    private val _pendingExecutionTarget = MutableStateFlow<AppView?>(null)
    val pendingExecutionTarget = _pendingExecutionTarget.asStateFlow()

    // Pattern Sensitivity: persisted calibration value (0-100)
    private val _patternSensitivity = MutableStateFlow(50f)
    val patternSensitivity = _patternSensitivity.asStateFlow()

    // Remote config control: when true, always apply remote flag immediately (force remote override).
    private val _forceRemoteOverride = MutableStateFlow(false)
    val forceRemoteOverride = _forceRemoteOverride.asStateFlow()

    // Poll interval for remote config (ms). Can be updated from Settings and persisted.
    private val _remotePollIntervalMs = MutableStateFlow(10_000L)
    val remotePollIntervalMs = _remotePollIntervalMs.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1200)
            _isInitializing.value = false
            // ensure initial stream list is computed
            _macroStreamEvents.value = computeMacroStreamList(_allMacroEvents.value)

            // Read persisted preferences first so user toggles survive restarts
            try {
                val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
                val persisted = prefs.getBoolean("promote_macro_stream", false)
                _promoteMacroStream.value = persisted
                if (persisted) {
                    _currentView.value = AppView.MACRO_STREAM
                    _sessionLandingCount.value = _sessionLandingCount.value + 1
                }
                // Load persisted pattern sensitivity (0..100)
                val persistedPattern = prefs.getFloat("pattern_sensitivity", 50f)
                _patternSensitivity.value = persistedPattern

                // Load persisted remote-mode override (force remote) if present
                if (prefs.contains("force_remote_override")) {
                    _forceRemoteOverride.value = prefs.getBoolean("force_remote_override", false)
                } else {
                    // fallback to BuildConfig.DEFAULT_FORCE_REMOTE if available via reflection
                    try {
                        val bcClass = com.asc.markets.BuildConfig::class.java
                        val f = bcClass.getDeclaredField("DEFAULT_FORCE_REMOTE")
                        val v = f.get(null)
                        if (v is Boolean) _forceRemoteOverride.value = v
                        if (v is String) _forceRemoteOverride.value = v.toBoolean()
                    } catch (_: Exception) { }
                }

                // Load persisted poll interval (ms)
                if (prefs.contains("remote_poll_ms")) {
                    _remotePollIntervalMs.value = prefs.getLong("remote_poll_ms", 10_000L)
                } else {
                    // fallback to BuildConfig.DEFAULT_REMOTE_POLL_MS via reflection
                    try {
                        val bcClass = com.asc.markets.BuildConfig::class.java
                        val f = bcClass.getDeclaredField("DEFAULT_REMOTE_POLL_MS")
                        val v = f.get(null)
                        when (v) {
                            is Number -> _remotePollIntervalMs.value = v.toLong()
                            is String -> _remotePollIntervalMs.value = v.toLongOrNull() ?: 10_000L
                        }
                    } catch (_: Exception) { }
                }

            } catch (_: Exception) {
                // ignore and fall through to BuildConfig reflection fallback
            }

            // Initialize telemetry sink (app-private storage)
            try {
                TelemetryManager.init(getApplication())
            } catch (_: Exception) {
                // ignore
            }

            // Load persisted audit records into memory
            try {
                val pm = PersistenceManager(getApplication())
                val raw = pm.loadAllAuditRecords()
                val parsed = raw.mapNotNull { r ->
                    try { Json.decodeFromString<AuditRecord>(r) } catch (_: Exception) { null }
                }
                _auditRecords.value = parsed
            } catch (_: Exception) { }
            // Start continuous polling of remote config; poll interval and mode are configurable.
            viewModelScope.launch(Dispatchers.IO) {
                var backoffMs = 10_000L
                while (isActive) {
                    val interval = _remotePollIntervalMs.value
                    var nextDelay = interval
                    try {
                        val remote = RemoteConfigManager.fetchRemoteConfig()
                        if (remote != null) {
                            val promoteRemote = remote["promote_macro_stream"]?.jsonPrimitive?.booleanOrNull
                            if (promoteRemote != null) {
                                if (_forceRemoteOverride.value) {
                                    // Force-apply remote value (overrides any local setting)
                                    val current = _promoteMacroStream.value
                                    if (promoteRemote != current) {
                                        setPromoteMacroStream(promoteRemote)
                                        TelemetryManager.recordEvent("remote_config_override_applied", mapOf("promote_macro_stream" to promoteRemote))
                                    } else {
                                        TelemetryManager.recordEvent("remote_config_no_change", mapOf("promote_macro_stream" to promoteRemote))
                                    }
                                } else {
                                    // Respect-local mode: only apply remote when no explicit local preference exists.
                                    val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
                                    val hasLocal = prefs.contains("promote_macro_stream")
                                    if (!hasLocal) {
                                        setPromoteMacroStream(promoteRemote)
                                        TelemetryManager.recordEvent("remote_config_applied_respect_local", mapOf("promote_macro_stream" to promoteRemote))
                                    } else {
                                        TelemetryManager.recordEvent("remote_config_skipped_respect_local", mapOf("promote_macro_stream" to promoteRemote))
                                    }
                                }
                            }
                        }
                        // success -> reset backoff
                        backoffMs = 10_000L
                    } catch (t: Throwable) {
                        TelemetryManager.recordEvent("remote_config_error", mapOf("error" to (t.message ?: "unknown")))
                        backoffMs = (backoffMs * 2).coerceAtMost(120_000L)
                        nextDelay = backoffMs
                    }
                    delay(nextDelay)
                }
            }

            // Fallback: read optional BuildConfig default for promoting MacroStream via reflection
            if (!_promoteMacroStream.value) {
                try {
                    val bcClass = com.asc.markets.BuildConfig::class.java
                    val field = bcClass.getDeclaredField("DEFAULT_PROMOTE_MACRO_STREAM")
                    val valObj = field.get(null)
                    val promoteDefault = when (valObj) {
                        is Boolean -> valObj
                        is String -> valObj.toBoolean()
                        else -> false
                    }
                    _promoteMacroStream.value = promoteDefault
                    if (promoteDefault) {
                        _currentView.value = AppView.MACRO_STREAM
                        _sessionLandingCount.value = _sessionLandingCount.value + 1
                    }
                } catch (_: Exception) {
                    // no default defined; leave runtime flag as-is (false)
                }
            }

            // Load persisted audit log
            try {
                val pm = PersistenceManager(getApplication())
                val raw = pm.secureLoad("audit_log")
                if (!raw.isNullOrBlank()) {
                    val list = Json.decodeFromString<List<com.asc.markets.data.AuditRecord>>(raw)
                    _auditLog.value = list
                }
            } catch (_: Exception) { }
        }
    }

    // Audit ledger state
    private val _auditRecords = MutableStateFlow<List<AuditRecord>>(emptyList())
    val auditRecords = _auditRecords.asStateFlow()

    // Append and persist an audit record
    fun appendAuditRecord(record: AuditRecord) {
        try {
            val pm = PersistenceManager(getApplication())
            val key = "audit_${record.id}"
            val json = Json.encodeToString(record)
            pm.saveAuditRecord(key, json)
            _auditRecords.value = listOf(record) + _auditRecords.value
            TelemetryManager.recordEvent("audit_record_appended", mapOf("id" to record.id))
        } catch (_: Exception) { }
    }

    fun markAuditRecordAudited(id: String) {
        try {
            val current = _auditRecords.value.toMutableList()
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val updated = current[idx].copy(audited = true)
                current[idx] = updated
                _auditRecords.value = current
                val pm = PersistenceManager(getApplication())
                val key = "audit_${id}"
                pm.saveAuditRecord(key, Json.encodeToString(updated))
                TelemetryManager.recordEvent("audit_record_marked_audited", mapOf("id" to id))
            }
        } catch (_: Exception) { }
    }

    fun clearAuditLedger() {
        try {
            val pm = PersistenceManager(getApplication())
            pm.clearAuditRecords()
            _auditRecords.value = emptyList()
            TelemetryManager.recordEvent("audit_ledger_cleared", emptyMap())
        } catch (_: Exception) { }
    }

    /**
     * Mark all in-memory audit records as audited and persist each.
     */
    fun markAllAuditRecordsAudited() {
        try {
            val current = _auditRecords.value.map { it.copy(audited = true) }
            _auditRecords.value = current
            val pm = PersistenceManager(getApplication())
            current.forEach { rec ->
                val key = "audit_${rec.id}"
                try { pm.saveAuditRecord(key, Json.encodeToString(rec)) } catch (_: Throwable) {}
            }
            TelemetryManager.recordEvent("audit_mark_all_records_audited", mapOf("count" to current.size))
        } catch (_: Exception) { }
    }

    // Audit log APIs
    fun addAuditRecord(record: com.asc.markets.data.AuditRecord) {
        val updated = listOf(record) + _auditLog.value
        _auditLog.value = updated
        persistAuditLog(updated)
    }

    fun markAudited(id: String) {
        val updated = _auditLog.value.map { if (it.id == id) it.copy(audited = true) else it }
        _auditLog.value = updated
        persistAuditLog(updated)
    }

    fun clearAuditLog() {
        _auditLog.value = listOf()
        try { PersistenceManager(getApplication()).secureSave("audit_log", "[]") } catch (_: Exception) { }
    }

    fun markAllAudited() {
        val updated = _auditLog.value.map { it.copy(audited = true) }
        _auditLog.value = updated
        persistAuditLog(updated)
    }

    private fun persistAuditLog(list: List<com.asc.markets.data.AuditRecord>) {
        try {
            val pm = PersistenceManager(getApplication())
            val raw = Json.encodeToString(list)
            pm.secureSave("audit_log", raw)
        } catch (_: Exception) { }
    }

    private fun computeMacroStreamList(all: List<MacroEvent>): List<MacroEvent> {
        // target max items to display in stream view
        val maxItems = 50
        val upcoming = all.filter { it.status == MacroEventStatus.UPCOMING }.sortedBy { it.datetimeUtc }
        val confirmed = all.filter { it.status == MacroEventStatus.CONFIRMED }.sortedByDescending { it.datetimeUtc }

        val takeUpcoming = minOf(upcoming.size, (maxItems * 9) / 10)
        val remaining = maxItems - takeUpcoming
        val takeConfirmed = minOf(confirmed.size, maxOf(1, remaining))

        val list = mutableListOf<MacroEvent>()
        list.addAll(upcoming.take(takeUpcoming))
        list.addAll(confirmed.take(takeConfirmed))

        // If not enough items to reach max, append more upcoming
        if (list.size < maxItems) {
            val extra = upcoming.drop(takeUpcoming).take(maxItems - list.size)
            list.addAll(extra)
        }

        return list
    }

    // Allow external updates to events (e.g., network ingestion)
    fun updateMacroEvents(events: List<MacroEvent>) {
        _allMacroEvents.value = events
        _macroStreamEvents.value = computeMacroStreamList(events)
    }

    /**
     * Ingest macro events from external sources, filtering out microstructure-origin events.
     * This enforces that MacroStream only receives macro-level signals.
     */
    fun ingestMacroEventsFromSources(events: List<MacroEvent>) {
        val filtered = events.filter { ev ->
            val src = ev.source?.lowercase() ?: ""
            // reject obvious microstructure sources
            val microKeywords = listOf("tick", "trade", "spread", "orderbook", "dom", "depth", "l2", "fill", "execution", "latency")
            microKeywords.none { kw -> src.contains(kw) }
        }

        // Count how many events were dropped due to microstructure filtering
        val dropped = events.size - filtered.size
        if (dropped > 0) {
            _ingestionDroppedCount.value = _ingestionDroppedCount.value + dropped
            // Record telemetry for ingestion drops
            try {
                TelemetryManager.recordEvent("ingestion_dropped", mapOf("dropped" to dropped, "sourceCount" to events.size))
            } catch (_: Exception) { }
        }

        if (filtered.isEmpty()) return

        // merge with existing allMacroEvents, newest first
        val merged = (filtered + _allMacroEvents.value).distinctBy { it.title + it.datetimeUtc }
        _allMacroEvents.value = merged
        _macroStreamEvents.value = computeMacroStreamList(merged)
        try { TelemetryManager.recordEvent("macrostream_update", mapOf("newTotal" to merged.size)) } catch (_: Exception) { }
    }

    fun navigateTo(view: AppView) {
        // Track user attempts to open execution surfaces
        if (view == AppView.TRADE || view == AppView.TRADING_ASSISTANT || view == AppView.LIQUIDITY_HUB) {
            _clicksToExecutionCount.value = _clicksToExecutionCount.value + 1
            try { TelemetryManager.recordEvent("clicks_to_execution", mapOf("count" to _clicksToExecutionCount.value, "target" to view.name)) } catch (_: Exception) { }
            if (_promoteMacroStream.value) {
                // Require explicit opt-in before exposing execution screens when surveillance is promoted
                requestExecutionOptIn(view)
                return
            }
        }

        // record previous view for back navigation (unless navigating to same view)
        if (_currentView.value != view) {
            _previousView.value = _currentView.value
        }
        _currentView.value = view
    }

    /**
     * Navigate back to the previously recorded view. Falls back to DASHBOARD when none recorded.
     */
    fun navigateBack() {
        val prev = _previousView.value ?: AppView.DASHBOARD
        _currentView.value = prev
        _previousView.value = null
    }
    fun toggleSidebar() { _isSidebarCollapsed.value = !_isSidebarCollapsed.value }
    fun toggleDrawer() { _isDrawerOpen.value = !_isDrawerOpen.value }
    fun openDrawer() { _isDrawerOpen.value = true }
    fun closeDrawer() { _isDrawerOpen.value = false }
    fun openCommandPalette() { _isCommandPaletteOpen.value = true }
    fun closeCommandPalette() { _isCommandPaletteOpen.value = false }
    fun acceptRisk() { _isRiskAccepted.value = true }
    fun selectPair(pair: ForexPair) { 
        _selectedPair.value = pair 
        _currentView.value = AppView.DASHBOARD
    }

    fun selectPairBySymbol(symbol: String) {
        val pair = FOREX_PAIRS.find { it.symbol == symbol }
        pair?.let {
            selectPair(it)
        }
    }

    /**
     * Select a pair without changing the current view (no navigation).
     * Use this when updating selection from within a modal or settings screen.
     */
    fun selectPairBySymbolNoNavigate(symbol: String) {
        val pair = FOREX_PAIRS.find { it.symbol == symbol }
        pair?.let {
            _selectedPair.value = it
        }
    }

    fun computeMarketState(symbol: String, data: List<ForexDataPoint>) {
        viewModelScope.launch {
            val last20 = data.takeLast(20).map { it.close }
            val mean20 = if (last20.isNotEmpty()) last20.average() else 0.0
            val lastClose = data.lastOrNull()?.close ?: 0.0
            val technicalBias = when {
                lastClose > mean20 * 1.002 -> "BULLISH"
                lastClose < mean20 * 0.998 -> "BEARISH"
                else -> "NEUTRAL"
            }

            val safetyBlocked = false

            val confidence = (
                (kotlin.math.abs(lastClose - mean20) / (mean20.takeIf { it != 0.0 } ?: 1.0))
                * 100
            ).coerceIn(0.0, 100.0).toInt()

            _marketState.emit(
                MarketState(
                    symbol = symbol,
                    chartData = data,
                    technicalBias = technicalBias,
                    safetyBlocked = safetyBlocked,
                    confidence = confidence
                )
            )
        }
    }

    fun sendCommand(cmd: String) {
        val userMsg = ChatMessage(role = "user", content = cmd.uppercase())
        _terminalLogs.value = listOf(userMsg) + _terminalLogs.value
        
        viewModelScope.launch {
            delay(300)
            val response = if (cmd.uppercase() == "ARM") {
                _isArmed.value = true
                "[SECURITY] PIPELINE ARMED via Local Node."
            } else {
                ForexAnalysisEngine.getAnalystResponse(cmd, "Core_Engine")
            }
            _terminalLogs.value = listOf(ChatMessage(role = "model", content = response)) + _terminalLogs.value
        }
    }

    fun toggleArm() { _isArmed.value = !_isArmed.value }

    // Trigger a UI education modal requiring explicit opt-in before exposing execution controls
    fun requestExecutionOptIn(target: AppView) {
        _pendingExecutionTarget.value = target
        _executionOptInRequested.value = true
        _terminalLogs.value = listOf(
            ChatMessage(role = "model", content = "[AUDIT] Execution opt-in requested for ${target.name} at ${System.currentTimeMillis()}")
        ) + _terminalLogs.value
    }

    // Called by UI when user confirms they understand the risks and explicitly opts into execution
    fun confirmExecutionOptIn() {
        val target = _pendingExecutionTarget.value
        _pendingExecutionTarget.value = null
        _executionOptInRequested.value = false
        _userOverrideCount.value = _userOverrideCount.value + 1
        _terminalLogs.value = listOf(
            ChatMessage(role = "model", content = "[AUDIT] Execution opt-in confirmed at ${System.currentTimeMillis()}")
        ) + _terminalLogs.value

        // Persist an encrypted audit record for the opt-in event
        try {
            val pm = PersistenceManager(getApplication())
            val ts = System.currentTimeMillis()
            val key = "audit_$ts"
            val json = "{\"event\":\"execution_opt_in_confirmed\",\"target\":\"${target?.name}\",\"timestamp\":$ts,\"sessionLanding\":${_sessionLandingCount.value}}"
            pm.secureSave(key, json)
            try { TelemetryManager.recordEvent("execution_opt_in_confirmed", mapOf("target" to target?.name, "auditKey" to key)) } catch (_: Exception) { }
        } catch (_: Exception) {
            // ignore persistence failure but keep audit in terminal logs
        }

        // Navigate to the requested execution view after explicit confirmation
        target?.let {
            _currentView.value = it
        }
    }

    fun cancelExecutionOptIn() {
        _pendingExecutionTarget.value = null
        _executionOptInRequested.value = false
        _terminalLogs.value = listOf(
            ChatMessage(role = "model", content = "[AUDIT] Execution opt-in cancelled at ${System.currentTimeMillis()}")
        ) + _terminalLogs.value
    }

    // API: enable/disable promotion of MacroStream (non-destructive)
    fun setPromoteMacroStream(enabled: Boolean) {
        _promoteMacroStream.value = enabled
        try {
            val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("promote_macro_stream", enabled).apply()
            if (enabled) {
                _sessionLandingCount.value = _sessionLandingCount.value + 1
            } else {
                _userOverrideCount.value = _userOverrideCount.value + 1
            }
        } catch (_: Exception) {
            // ignore persistence failure — runtime flag still set
        }
    }

    // Convenience: promote and navigate to MacroStream
    fun promoteAndNavigateToMacroStream() {
        _promoteMacroStream.value = true
        _currentView.value = AppView.MACRO_STREAM
        _sessionLandingCount.value = _sessionLandingCount.value + 1
    }

    // Persist pattern sensitivity calibration (0..100)
    fun setPatternSensitivity(value: Float) {
        _patternSensitivity.value = value.coerceIn(0f, 100f)
        try {
            val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            prefs.edit().putFloat("pattern_sensitivity", _patternSensitivity.value).apply()
        } catch (_: Exception) {
            // ignore persistence failure
        }
    }

    // External API to set the Dashboard's active top tab by name (e.g., "MACRO_STREAM")
    fun setDashboardTab(tabName: String) {
        _dashboardTabTarget.value = tabName
    }

    // Settings API: control whether remote config should force-override local choice.
    fun setForceRemoteOverride(force: Boolean) {
        _forceRemoteOverride.value = force
        try {
            val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("force_remote_override", force).apply()
            TelemetryManager.recordEvent("force_remote_override_set", mapOf("value" to force))
        } catch (_: Exception) { }
    }

    // Settings API: set poll interval (ms) for remote config fetches.
    fun setRemotePollIntervalMs(ms: Long) {
        val safe = ms.coerceIn(2000L, 300_000L)
        _remotePollIntervalMs.value = safe
        try {
            val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("remote_poll_ms", safe).apply()
            TelemetryManager.recordEvent("remote_poll_interval_set", mapOf("ms" to safe))
        } catch (_: Exception) { }
    }
}