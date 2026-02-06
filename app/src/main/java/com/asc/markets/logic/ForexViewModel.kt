package com.asc.markets.logic

import androidx.lifecycle.ViewModel
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

class ForexViewModel : ViewModel() {
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

    init {
        viewModelScope.launch {
            delay(1200)
            _isInitializing.value = false
            // ensure initial stream list is computed
            _macroStreamEvents.value = computeMacroStreamList(_allMacroEvents.value)

            // Step 6: read optional BuildConfig default for promoting MacroStream.
            // Use reflection so this code remains safe if the field isn't defined in BuildConfig.
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
                }
            } catch (_: Exception) {
                // no default defined; leave runtime flag as-is (false)
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

    fun navigateTo(view: AppView) { _currentView.value = view }
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

    // API: enable/disable promotion of MacroStream (non-destructive)
    fun setPromoteMacroStream(enabled: Boolean) { _promoteMacroStream.value = enabled }

    // Convenience: promote and navigate to MacroStream
    fun promoteAndNavigateToMacroStream() {
        _promoteMacroStream.value = true
        _currentView.value = AppView.MACRO_STREAM
    }
}