package com.asc.markets.logic

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.api.ForexAnalysisEngine
import com.asc.markets.data.*
import com.asc.markets.data.ForexDataPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.asc.markets.data.MacroEvent
import com.asc.markets.data.MacroEventStatus
import com.asc.markets.data.ImpactPriority
import com.asc.markets.BuildConfig

class ForexViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentView = MutableStateFlow(AppView.DASHBOARD)
    val currentView = _currentView.asStateFlow()

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

    init {
        viewModelScope.launch {
            delay(1200)
            _isInitializing.value = false
            // ensure initial stream list is computed
            _macroStreamEvents.value = computeMacroStreamList(_allMacroEvents.value)

            // Read persisted preference first so user toggle survives restarts
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
            } catch (_: Exception) {
                // ignore and fall through to BuildConfig reflection fallback
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
        }
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
        }

        if (filtered.isEmpty()) return

        // merge with existing allMacroEvents, newest first
        val merged = (filtered + _allMacroEvents.value).distinctBy { it.title + it.datetimeUtc }
        _allMacroEvents.value = merged
        _macroStreamEvents.value = computeMacroStreamList(merged)
    }

    fun navigateTo(view: AppView) {
        // Track user attempts to open execution surfaces
        if (view == AppView.TRADE || view == AppView.TRADING_ASSISTANT || view == AppView.LIQUIDITY_HUB) {
            _clicksToExecutionCount.value = _clicksToExecutionCount.value + 1
            if (_promoteMacroStream.value) {
                // Require explicit opt-in before exposing execution screens when surveillance is promoted
                requestExecutionOptIn(view)
                return
            }
        }

        _currentView.value = view
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
}