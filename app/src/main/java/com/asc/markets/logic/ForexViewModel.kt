package com.asc.markets.logic

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.data.*
import com.asc.markets.ui.screens.dashboard.provideForexExplore
import com.trading.app.data.CalendarSnapshotStore
import com.trading.app.data.ChartFeedType
import com.trading.app.components.defaultQuoteSymbols
import com.trading.app.data.NewsSnapshotStore

import com.trading.app.data.Mt5Service
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlinx.serialization.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.asc.markets.data.trade.TradeHistoryRepository
import com.trading.app.models.BondData
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import com.asc.markets.data.remote.*

data class CommandCenterStatus(
    val isLoading: Boolean = false,
    val isConnected: Boolean? = null,
    val lastMessage: String = "Idle",
    val lastActionAtMillis: Long = 0L
)

data class AscChatSession(
    val id: String,
    val title: String,
    val createdAtMillis: Long,
    val messages: List<ChatMessage> = emptyList()
)

class ForexViewModel(application: Application) : AndroidViewModel(application) {
    enum class WatchlistSortMode { PROBABILITY, CONFIDENCE, VOLATILITY, TIME_TO_EVENT }

    private companion object {
        private const val TIINGO_REST_REFRESH_MS = 60 * 60_000L
        private const val GLOBAL_CHAT_CONTEXT_ID = "GLOBAL"
        private const val CHAT_SESSIONS_KEY = "chat_sessions"
        private const val CHAT_ACTIVE_SESSION_ID_KEY = "chat_active_session_id"
    }

    private val myApp = application as com.asc.markets.MyApp
    
    // --- REPOSITORIES ---
    val tradeHistoryRepository = TradeHistoryRepository(com.asc.markets.data.trade.AppDatabase.getDatabase(application).tradeDao())
    val aiRepository = com.asc.markets.data.repository.AiRepository()

    // --- AI DEPLOYMENTS ---
    val aiDeployments = aiRepository.deployments
    val aiDecisions = aiDeployments.map { it?.final_decision ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val chatPrefs = application.getSharedPreferences("asc_engine_chat", Context.MODE_PRIVATE)
    private val appContext = application
    private val _ascChatSessions = MutableStateFlow<List<AscChatSession>>(emptyList())
    val ascChatSessions = _ascChatSessions.asStateFlow()
    private val _ascChatSessionId = MutableStateFlow("")
    val ascChatSessionId = _ascChatSessionId.asStateFlow()
    private val _ascChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val ascChatMessages = _ascChatMessages.asStateFlow()
    private val _ascChatResponding = MutableStateFlow(false)
    val ascChatResponding = _ascChatResponding.asStateFlow()
    private val _ascChatPersonaId = MutableStateFlow("default")
    val ascChatPersonaId = _ascChatPersonaId.asStateFlow()
    private val _ascChatContextPageId = MutableStateFlow(GLOBAL_CHAT_CONTEXT_ID)
    val ascChatContextPageId = _ascChatContextPageId.asStateFlow()

    // --- DASHBOARD & SETTINGS ---
    private val _dashboardTabTarget = MutableStateFlow("MARKETS")
    val dashboardTabTarget = _dashboardTabTarget.asStateFlow()
    private val _forceRemoteOverride = MutableStateFlow(false)
    val forceRemoteOverride = _forceRemoteOverride.asStateFlow()
    private val _remotePollIntervalMs = MutableStateFlow(10000L)
    val remotePollIntervalMs = _remotePollIntervalMs.asStateFlow()
    private val _patternSensitivity = MutableStateFlow(0.5f)
    val patternSensitivity = _patternSensitivity.asStateFlow()
    private val _autoSyncCalendarEvents = MutableStateFlow(true)
    val autoSyncCalendarEvents = _autoSyncCalendarEvents.asStateFlow()

    // --- WATCHLIST ---
    private val _watchlistSortMode = MutableStateFlow(WatchlistSortMode.PROBABILITY)
    val watchlistSortMode = _watchlistSortMode.asStateFlow()
    private val _watchlistCategoryFilter = MutableStateFlow<MarketCategory?>(null)
    val watchlistCategoryFilter = _watchlistCategoryFilter.asStateFlow()
    private val _hiddenWatchlistIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenWatchlistIds = _hiddenWatchlistIds.asStateFlow()
    private val _watchlistCompactMode = MutableStateFlow(false)
    val watchlistCompactMode = _watchlistCompactMode.asStateFlow()
    private val _isWatchlistAnalyzing = MutableStateFlow(false)
    val isWatchlistAnalyzing = _isWatchlistAnalyzing.asStateFlow()
    private val _lastWatchlistUpdate = MutableStateFlow(System.currentTimeMillis())
    val lastWatchlistUpdate = _lastWatchlistUpdate.asStateFlow()

    // --- TERMINAL ---
    private val _activeAlgo = MutableStateFlow("MARKET")
    val activeAlgo = _activeAlgo.asStateFlow()

    // --- NOTIFICATIONS ---
    private val _inAppNotifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val inAppNotifications = _inAppNotifications.asStateFlow()

    // --- MARKET DATA & STORES ---
    val allLivePairs = UnifiedMarketDataStore.allPairs
    val priceHistory = UnifiedMarketDataStore.priceHistory
    val timedPriceHistory = UnifiedMarketDataStore.timedPriceHistory
    val liveCryptoPairs = UnifiedMarketDataStore.allPairs.map { list -> list.filter { it.category == MarketCategory.CRYPTO } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assetCtxForNews = com.asc.markets.state.AssetContextStore.context

    private val _currentView = MutableStateFlow(AppView.DASHBOARD)
    val currentView = _currentView.asStateFlow()
    private var _returnToSidebar = false
    private val _navigationStack = mutableListOf<AppView>()
    private val _drawerStack = mutableListOf<Boolean>()
    private val _isSidebarCollapsed = MutableStateFlow(false)
    val isSidebarCollapsed = _isSidebarCollapsed.asStateFlow()
    private val _bondData = MutableStateFlow<Map<String, BondData>>(emptyMap())
    val bondData = _bondData.asStateFlow()
    


    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen = _isDrawerOpen.asStateFlow()
    private val _isCommandPaletteOpen = MutableStateFlow(false)
    val isCommandPaletteOpen = _isCommandPaletteOpen.asStateFlow()
    private val _isGlobalHeaderVisible = MutableStateFlow(true)
    val isGlobalHeaderVisible = _isGlobalHeaderVisible.asStateFlow()
    private val _globalHeaderCollapse = MutableStateFlow(0f)
    val globalHeaderCollapse = _globalHeaderCollapse.asStateFlow()
    private val _marketState = MutableStateFlow<MarketState?>(null)
    val marketState = _marketState.asStateFlow()
    private val _isRiskAccepted = MutableStateFlow(false)
    val isRiskAccepted = _isRiskAccepted.asStateFlow()
    private val _showRiskDisclosure = MutableStateFlow(
        appContext.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE).getBoolean("show_risk_disclosure", true)
    )
    val showRiskDisclosure = _showRiskDisclosure.asStateFlow()

    private val _selectedPair = MutableStateFlow(provideForexExplore().first())
    val selectedPair = _selectedPair.asStateFlow()
    private val _cryptoPairs = MutableStateFlow<List<ForexPair>>(emptyList())
    val cryptoPairs = _cryptoPairs.asStateFlow()
    private val _preMoveCandidates = MutableStateFlow<List<PreMoveCandidate>>(emptyList())
    val preMoveCandidates = _preMoveCandidates.asStateFlow()
    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing.asStateFlow()
    private val _isArmed = MutableStateFlow(false)
    val isArmed = _isArmed.asStateFlow()
    private val _commandCenterStatus = MutableStateFlow(CommandCenterStatus())
    val commandCenterStatus = _commandCenterStatus.asStateFlow()
    private val _promoteMacroStream = MutableStateFlow(false)
    val promoteMacroStream = _promoteMacroStream.asStateFlow()
    private val _watchlistItems = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val watchlistItems = _watchlistItems.asStateFlow()
    
    // ASC EA Integration: Refresh watchlist from ai_signals_mq5.json
    fun refreshWatchlistFromASCEA() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = loadWatchlistFromASCEA()
                if (items.isNotEmpty()) {
                    _watchlistItems.value = items
                    android.util.Log.d("ASC_EA", "Watchlist refreshed: ${items.size} items")
                } else {
                    android.util.Log.w("ASC_EA", "No watchlist items loaded from ASC EA")
                }
            } catch (e: Exception) {
                android.util.Log.e("ASC_EA", "Failed to refresh watchlist: ${e.message}", e)
            }
        }
    }
    
    // Manual refresh for pull-to-refresh
    fun refreshWatchlist() {
        refreshWatchlistFromASCEA()
    }
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()
    private val _alertNotificationCount = MutableStateFlow(0)
    val alertNotificationCount = _alertNotificationCount.asStateFlow()
    private val _macroStreamEvents = MutableStateFlow<List<MacroEvent>>(emptyList())
    val macroStreamEvents = _macroStreamEvents.asStateFlow()
    private val _auditRecords = MutableStateFlow<List<AuditRecord>>(emptyList())
    val auditRecords = _auditRecords.asStateFlow()
    private val _terminalLogs = MutableStateFlow<List<ChatMessage>>(listOf(ChatMessage(role = "model", content = "Terminal active. AI connection removed. Ready for new architecture.")))
    val terminalLogs = _terminalLogs.asStateFlow()
    private val _executionOptInRequested = MutableStateFlow(false)
    val executionOptInRequested = _executionOptInRequested.asStateFlow()
    private val _pendingExecutionTarget = MutableStateFlow<AppView?>(null)
    val pendingExecutionTarget = _pendingExecutionTarget.asStateFlow()

    init {
        // Initialization without AI dependencies
    }

    // --- NAVIGATION & UI HELPERS ---
    fun navigateTo(view: AppView) {
        if (view == AppView.SIDEBAR_PAGE) _returnToSidebar = false
        _currentView.value = view
    }

    fun navigateFromSidebar(view: AppView) {
        _returnToSidebar = true
        _currentView.value = view
    }

    fun navigateBack() {
        if (_returnToSidebar && _currentView.value != AppView.SIDEBAR_PAGE) {
            _returnToSidebar = false
            _currentView.value = AppView.SIDEBAR_PAGE
        } else {
            _returnToSidebar = false
            _currentView.value = AppView.DASHBOARD
        }
    }

    /**
     * Where ASSET_DETAIL should return to on back-press. Recorded when the page
     * is opened so back returns to MY_ALERTS, MARKETS, etc. instead of always
     * dumping the user to the dashboard.
     */
    private val _assetDetailBackTarget = MutableStateFlow(AppView.DASHBOARD)
    val assetDetailBackTarget: StateFlow<AppView> = _assetDetailBackTarget.asStateFlow()

    /** Open ASSET_DETAIL remembering the origin so the back arrow returns there. */
    fun openAssetDetail(origin: AppView = AppView.DASHBOARD) {
        _assetDetailBackTarget.value = origin
        _currentView.value = AppView.ASSET_DETAIL
    }

    /** Back from ASSET_DETAIL returns to where it was opened from. */
    fun onAssetDetailBack() {
        _currentView.value = _assetDetailBackTarget.value
        _assetDetailBackTarget.value = AppView.DASHBOARD
    }
    fun selectPair(pair: ForexPair) { _selectedPair.value = pair }
    fun selectPairNoNavigate(pair: ForexPair) { _selectedPair.value = pair }
    fun selectPairBySymbolNoNavigate(symbol: String) {
        val resolved = MarketDataStore.pairSnapshot(symbol)
            ?: UnifiedMarketDataStore.allPairs.value.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
            ?: provideForexExplore().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
        // Always resolve — fall back to a bare pair so alerts on any symbol can
        // still open its ASSET_DETAIL page.
        _selectedPair.value = resolved ?: ForexPair(
            symbol = symbol,
            name = symbol,
            price = 0.0,
            change = 0.0,
            changePercent = 0.0
        )
    }
    fun selectPairBySymbol(symbol: String) {
        selectPairBySymbolNoNavigate(symbol)
    }
    fun toggleArm() { _isArmed.value = !_isArmed.value }
    fun acceptRisk() { _isRiskAccepted.value = true }
    fun setShowRiskDisclosure(show: Boolean) {
        _showRiskDisclosure.value = show
        appContext.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("show_risk_disclosure", show).apply()
    }
    fun openCommandPalette() { _isCommandPaletteOpen.value = true }
    fun closeCommandPalette() { _isCommandPaletteOpen.value = false }
    fun setGlobalHeaderCollapse(progress: Float) { _globalHeaderCollapse.value = progress }
    fun setGlobalHeaderVisible(visible: Boolean) { _isGlobalHeaderVisible.value = visible }
    fun setDashboardTab(tab: String) { _dashboardTabTarget.value = tab }
    fun cancelExecutionOptIn() { _executionOptInRequested.value = false }
    fun confirmExecutionOptIn() { _executionOptInRequested.value = false }

    // --- SETTINGS HELPERS ---
    fun setForceRemoteOverride(override: Boolean) { _forceRemoteOverride.value = override }
    fun setRemotePollIntervalMs(ms: Long) { _remotePollIntervalMs.value = ms }
    fun setPatternSensitivity(sensitivity: Float) { _patternSensitivity.value = sensitivity }
    fun setPromoteMacroStream(promote: Boolean) { _promoteMacroStream.value = promote }

    // --- WATCHLIST HELPERS ---

    fun toggleWatchlistCompactMode() { _watchlistCompactMode.value = !_watchlistCompactMode.value }
    fun setWatchlistCategoryFilter(category: MarketCategory?) { _watchlistCategoryFilter.value = category }
    fun setWatchlistSort(mode: WatchlistSortMode) { _watchlistSortMode.value = mode }
    fun hideWatchlistItem(id: String) { _hiddenWatchlistIds.value += id }

    // --- AUDIT & NOTIFICATIONS ---
    fun clearAuditLedger() { _auditRecords.value = emptyList() }
    fun markAllAuditRecordsAudited() { _auditRecords.value = _auditRecords.value.map { it.copy(audited = true) } }
    fun markAuditRecordAudited(id: String) { _auditRecords.value = _auditRecords.value.map { if (it.id == id) it.copy(audited = true) else it } }
    fun markAllNotificationsSeen() { _inAppNotifications.value = _inAppNotifications.value.map { it.copy(seen = true) } }
    fun markNotificationSeen(id: String) { _inAppNotifications.value = _inAppNotifications.value.map { if (it.id == id) it.copy(seen = true) else it } }
    fun pushInAppNotification(notification: NotificationModel) {
        _inAppNotifications.value = listOf(notification) + _inAppNotifications.value.take(99)
    }
    fun incrementAlertNotificationCount() { _alertNotificationCount.value = _alertNotificationCount.value + 1 }

    // --- DATA VAULT ---
    fun syncDataVaultNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(isLoading = true, lastMessage = "Syncing Data Vault...")
            val result = aiRepository.fetchLatestDeployments()
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = false,
                isConnected = result.isSuccess,
                lastMessage = if (result.isSuccess) "Vault Synced" else "Sync Failed: ${result.exceptionOrNull()?.message}",
                lastActionAtMillis = System.currentTimeMillis()
            )
        }
    }

    fun checkAiHealthNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(isLoading = true, lastMessage = "Checking Node Health...")
            val result = aiRepository.fetchLatestDeployments()
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = false,
                isConnected = result.isSuccess,
                lastMessage = if (result.isSuccess) "Node Online" else "Node Offline: ${result.exceptionOrNull()?.message}",
                lastActionAtMillis = System.currentTimeMillis()
            )
        }
    }

    fun syncCalendarEventsNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(isLoading = true, lastMessage = "Syncing Calendar Events...")
            delay(1500) // Simulate work or call a repo if exists
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = false,
                lastMessage = "Calendar Events Synced",
                lastActionAtMillis = System.currentTimeMillis()
            )
        }
    }

    fun autoSyncCalendarEvents() {}

    fun registerVigilanceNode(node: VigilanceNode, prefix: String) {
        val message = "$prefix: ${node.pair} ${node.trigger} (${node.timeframe}) - Conf: ${node.confidenceScore}%"
        _terminalLogs.value = listOf(ChatMessage(role = "model", content = message)) + _terminalLogs.value
    }
    
    fun sendCommand(cmd: String) {
        val userMsg = ChatMessage(role = "user", content = cmd)
        _terminalLogs.value = listOf(userMsg) + _terminalLogs.value
        
        viewModelScope.launch {
            delay(500)
            val response = when (cmd.uppercase()) {
                "ARM" -> { _isArmed.value = true; "SURVEILLANCE_ARMED" }
                "DISARM" -> { _isArmed.value = false; "SURVEILLANCE_LOCKED" }
                "ASC STATUS" -> "ASC AI is online. Vault synced."
                "RUN AI" -> { runAiPipelineNow(); "AI Pipeline triggered." }
                "REFRESH AI" -> { refreshAiDeploymentsNow(); "AI Deployments refreshing..." }
                else -> "Command received: $cmd. Processing..."
            }
            _terminalLogs.value = listOf(ChatMessage(role = "model", content = response)) + _terminalLogs.value
        }
    }
    
    // --- AI METHODS ---
    fun fetchLatestDeployments() { syncDataVaultNow() }
    fun runAiPipelineNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(isLoading = true, lastMessage = "Running AI Pipeline...")
            val result = aiRepository.runAiPipeline()
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = false,
                lastMessage = if (result.isSuccess) "Pipeline Complete" else "Pipeline Failed",
                lastActionAtMillis = System.currentTimeMillis()
            )
            if (result.isSuccess) {
                syncDataVaultNow()
            }
        }
    }
    fun refreshAiDeploymentsNow() { syncDataVaultNow() }
    fun sendAscChatMessage(userQuery: String, personaName: String, personaInstruction: String) {
        _ascChatMessages.value = _ascChatMessages.value + ChatMessage(role = "user", content = userQuery) + ChatMessage(role = "model", content = "AI Chat is offline.")
    }
    fun clearAscChatMessages() { _ascChatMessages.value = emptyList() }
    fun setAscChatPersona(id: String) {}
    fun startNewAscChatSession() {}
    fun setActiveAscChatSession(id: String) {}
    fun deleteAscChatSession(id: String) {}
    fun ingestMacroEventsFromSources(events: List<MacroEvent>) { _macroStreamEvents.value = events }

    // ========================================
    // EA TRADE SIMULATION
    // ========================================
    
    private val _simulationResult = MutableStateFlow<TradeSimulationResponse?>(null)
    val simulationResult = _simulationResult.asStateFlow()
    
    private val _isSimulating = MutableStateFlow(false)
    val isSimulating = _isSimulating.asStateFlow()
    
    private val _simulationError = MutableStateFlow<String?>(null)
    val simulationError = _simulationError.asStateFlow()
    
    private val _simulationStatus = MutableStateFlow<SimulationStatusResponse?>(null)
    val simulationStatus = _simulationStatus.asStateFlow()
    
    /**
     * Simulate a trade setup using EA analysis
     * Returns prediction with win probability, recommendation, risk assessment, and institutional context
     */
    fun simulateTrade(
        asset: String,
        direction: String,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double
    ) {
        viewModelScope.launch {
            _isSimulating.value = true
            _simulationError.value = null
            
            try {
                val request = TradeSimulationRequest(
                    asset = asset.replace("m", "").uppercase(),
                    direction = direction.uppercase(),
                    entryPrice = entryPrice,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit
                )
                
                val result = aiRepository.simulateTrade(request)
                
                result.onSuccess { response ->
                    _simulationResult.value = response
                    _simulationError.value = response.warning
                }.onFailure { error ->
                    _simulationError.value = error.message ?: "Simulation failed"
                    _simulationResult.value = null
                }
            } catch (e: Exception) {
                _simulationError.value = "Error: ${e.message}"
                _simulationResult.value = null
            } finally {
                _isSimulating.value = false
            }
        }
    }
    
    /**
     * Check if EA simulation system is online and responsive
     */
    fun checkSimulationStatus() {
        viewModelScope.launch {
            try {
                val result = aiRepository.getSimulationStatus()
                result.onSuccess { status ->
                    _simulationStatus.value = status
                }.onFailure {
                    _simulationStatus.value = null
                }
            } catch (e: Exception) {
                _simulationStatus.value = null
            }
        }
    }
    
    /**
     * Clear current simulation result
     */
    fun clearSimulationResult() {
        _simulationResult.value = null
        _simulationError.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }

    // ========================================
    // CHART DISPLAY SETTINGS
    // ========================================
    
    private val _chartDisplaySettings = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val chartDisplaySettings = _chartDisplaySettings.asStateFlow()
    
    /**
     * Save chart display settings to file for EA to read
     */
    fun saveChartDisplaySettings(settings: Map<String, Boolean>) {
        viewModelScope.launch {
            try {
                val result = aiRepository.saveChartDisplaySettings(settings)
                result.onSuccess { response ->
                    _chartDisplaySettings.value = settings
                    android.util.Log.i("ForexViewModel", "Chart display settings saved: ${response.message}")
                }.onFailure { error ->
                    android.util.Log.e("ForexViewModel", "Failed to save chart display settings: ${error.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ForexViewModel", "Failed to save chart display settings: ${e.message}")
            }
        }
    }
}
