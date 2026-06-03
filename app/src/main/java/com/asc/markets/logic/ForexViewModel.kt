package com.asc.markets.logic

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.data.*
import com.asc.markets.ui.screens.dashboard.provideForexExplore
import com.asc.markets.data.ForexDataPoint
import com.trading.app.data.CalendarSnapshotStore
import com.trading.app.data.ChartFeedType
import com.trading.app.data.NewsSnapshotStore
import com.trading.app.data.PaperTradingSnapshotStore
import com.trading.app.data.Mt5Service
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import androidx.room.Room
import com.asc.markets.data.trade.AppDatabase
import com.asc.markets.data.trade.TradeHistoryRepository
import com.asc.markets.data.MacroEvent
import com.asc.markets.data.PersistenceManager
import com.asc.markets.data.AuditRecord
    
import com.asc.markets.data.TelemetryManager
import com.asc.markets.data.MacroEventStatus
import com.asc.markets.data.ImpactPriority
import com.asc.markets.BuildConfig

import com.asc.markets.data.remote.LatestDeploymentsResponse
import com.asc.markets.ui.screens.dashboard.AIAppManager
import com.researchcenter.services.NewsService
import kotlinx.coroutines.flow.filterNotNull
import com.asc.markets.network.CTraderBridgeClient
import com.asc.markets.network.TiingoFxRestClient
import com.asc.markets.network.TiingoFxWebSocketManager
import com.asc.markets.network.TiingoIexRestClient
import com.asc.markets.network.TiingoIexWebSocketManager

import com.trading.app.data.DerivService
import com.trading.app.data.FredService
import com.trading.app.models.BondData
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

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
    private companion object {
        private const val TIINGO_REST_REFRESH_MS = 60 * 60_000L
        private const val COMBINED_FALLBACK_RETRY_MS = 5_000L
        private const val GLOBAL_CHAT_CONTEXT_ID = "GLOBAL"
        private const val CHAT_SESSIONS_KEY = "chat_sessions"
        private const val CHAT_ACTIVE_SESSION_ID_KEY = "chat_active_session_id"
        private const val AI_DEPLOYMENTS_POLL_INTERVAL_MS = 5_000L // Poll AI deployments every 5 seconds
    }

    private val myApp = application as com.asc.markets.MyApp
    private val aiRepository = myApp.aiRepository
    val aiDeployments: StateFlow<LatestDeploymentsResponse?> = aiRepository.deployments
    val aiDecisions = aiDeployments.map { it?.final_decision.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val chatPrefs = application.getSharedPreferences("asc_engine_chat", Context.MODE_PRIVATE)
    private val initialAscChatSessions = loadAscChatSessions()
    private val _ascChatSessions = MutableStateFlow(initialAscChatSessions)
    val ascChatSessions = _ascChatSessions.asStateFlow()
    private val _ascChatSessionId = MutableStateFlow(loadAscChatSessionId(initialAscChatSessions))
    val ascChatSessionId = _ascChatSessionId.asStateFlow()
    private val _ascChatMessages = MutableStateFlow(loadActiveAscChatMessages(initialAscChatSessions, _ascChatSessionId.value))
    val ascChatMessages = _ascChatMessages.asStateFlow()
    private val _ascChatResponding = MutableStateFlow(false)
    val ascChatResponding = _ascChatResponding.asStateFlow()
    private val _ascChatPersonaId = MutableStateFlow(loadAscChatPersonaId())
    val ascChatPersonaId = _ascChatPersonaId.asStateFlow()
    private val _ascChatContextPageId = MutableStateFlow(loadAscChatContextPageId())
    val ascChatContextPageId = _ascChatContextPageId.asStateFlow()

    fun fetchLatestDeployments() {
        viewModelScope.launch(Dispatchers.IO) {
            aiRepository.fetchLatestDeployments()
        }
    }

    private val _currentView = MutableStateFlow(AppView.DASHBOARD)
    val currentView = _currentView.asStateFlow()

    // Track previous view to allow back-navigation from screens like POST_MOVE_AUDIT
    private val _previousView = MutableStateFlow<AppView?>(null)
    val previousView = _previousView.asStateFlow()
    // Remember if navigation originated from the drawer being open so back can re-open it
    private val _previousWasDrawerOpen = MutableStateFlow(false)
    val previousWasDrawerOpen = _previousWasDrawerOpen.asStateFlow()

    private val _isSidebarCollapsed = MutableStateFlow(false)
    val isSidebarCollapsed = _isSidebarCollapsed.asStateFlow()

    private val _bondData = MutableStateFlow<Map<String, BondData>>(emptyMap())
    val bondData = _bondData.asStateFlow()

    private val fredService = FredService()
    private val derivService = DerivService(
        onQuoteUpdate = derivQuote@ { quote ->
            val quoteCategory = when {
                quote.name.contains("NAS") || quote.name.contains("US30") || quote.name.contains("SPX") -> MarketCategory.INDICES
                quote.name.contains("XAU") || quote.name.contains("XAG") || quote.name.contains("OIL") -> MarketCategory.COMMODITIES
                quote.name.contains("BTC") || quote.name.contains("ETH") -> MarketCategory.CRYPTO
                else -> MarketCategory.FOREX
            }
            if (quoteCategory != MarketCategory.CRYPTO && cTraderBridgeClient.hasRecentPrice(quote.name)) {
                android.util.Log.d("DerivService", "Skipping Deriv update ${quote.name}; Pepperstone cTrader is primary")
                return@derivQuote
            }
            val pair = ForexPair(
                symbol = quote.name,
                name = quote.name,
                price = quote.lastPrice.toDouble(),
                change = quote.change.toDouble(),
                changePercent = quote.changePercent.toDouble(),
                category = quoteCategory
            )
            if (!canUseCombinedFallback(
                    source = "Combined fallback",
                    reason = "Pepperstone primary data is unavailable. Combined fallback is ready."
                )
            ) {
                android.util.Log.i("DerivService", "Waiting for Pepperstone availability or Combined fallback approval before routing ${quote.name}")
                return@derivQuote
            }
            PriceStreamManager.updatePrice(quote.name, quote.lastPrice.toDouble())
            CombinedFallbackDataStore.updatePair(pair)
            android.util.Log.d("DerivService", "Routing Deriv update ${quote.name} ${quote.lastPrice} to CombinedFallbackDataStore")
        },
        onHistoryUpdate = { _, _ -> }
    )

    private fun canUseCombinedFallback(source: String, reason: String): Boolean {
        return CombinedFallbackStore.canUseFallback()
    }

    private fun markPrimaryLiveDataRestored() {
        CombinedFallbackStore.markPrimaryRestored()
    }

    private fun pepperstoneCTraderSymbol(pair: ForexPair): String {
        return when (pair.symbol.uppercase(Locale.US).replace("/", "")) {
            "US10Y" -> "USTN10YR-F"
            "US02Y" -> "USTN2YR-F"
            else -> pair.symbol.replace("/", "")
        }
    }

    fun refreshBondData() {
        val seriesToFetch = listOf(
            Triple("DGS10", "US10Y", "US 10Y Treasury Yield"),
            Triple("DGS2", "US02Y", "US 2Y Treasury Yield")
        )
        seriesToFetch.forEach { (seriesId, symbol, name) ->
            fredService.fetchSeriesObservations(seriesId, object : FredService.FredCallback {
                override fun onSuccess(data: JSONObject) {
                    val observations = data.optJSONArray("observations")
                    if (observations != null && observations.length() > 0) {
                        val parsedObservations = mutableListOf<Pair<JSONObject, Float>>()
                        for (index in 0 until observations.length()) {
                            val item = observations.optJSONObject(index) ?: continue
                            val parsed = item.optString("value").toFloatOrNull() ?: continue
                            parsedObservations += item to parsed
                        }
                        val latestObservation = parsedObservations.firstOrNull()?.first ?: return
                        val latestValue = parsedObservations.firstOrNull()?.second ?: return
                        val date = latestObservation.optString("date")
                        val existing = CombinedFallbackDataStore.pairSnapshot(symbol)
                        val previousPrice = existing?.price ?: latestValue.toDouble()
                        val change = latestValue.toDouble() - previousPrice
                        val changePercent = if (previousPrice != 0.0) (change / previousPrice) * 100.0 else 0.0

                        _bondData.value = _bondData.value + (seriesId to BondData(seriesId, latestValue, date, name))
                        if (!canUseCombinedFallback(
                                source = "Combined fallback",
                                reason = "Pepperstone primary bond data is unavailable. Combined fallback is ready."
                            )
                        ) {
                            android.util.Log.i("FredService", "Waiting for Pepperstone availability or Combined fallback approval before routing $symbol")
                            return
                        }
                        android.util.Log.i("FredService", "Routing FRED bond data to CombinedFallbackDataStore $symbol $latestValue")
                        CombinedFallbackDataStore.updatePair(
                            ForexPair(
                                symbol = symbol,
                                name = name,
                                price = latestValue.toDouble(),
                                change = change,
                                changePercent = changePercent,
                                category = MarketCategory.BONDS
                            )
                        )
                        CombinedFallbackDataStore.replaceHistory(
                            symbol = symbol,
                            prices = parsedObservations.asReversed().map { it.second.toDouble() }
                        )
                    }
                }

                override fun onError(error: String) {
                    android.util.Log.e("ForexViewModel", "Error fetching FRED bond data: $error")
                }
            })
        }
    }
    
    // Drawer state for modal (hidden-by-default) sidebar on mobile
    // Start closed by default so a single explicit open action reliably opens the drawer
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen = _isDrawerOpen.asStateFlow()

    private val _isCommandPaletteOpen = MutableStateFlow(false)
    val isCommandPaletteOpen = _isCommandPaletteOpen.asStateFlow()

    // Global header visibility (used by MacroStream to hide/show app header on scroll)
    private val _isGlobalHeaderVisible = MutableStateFlow(true)
    val isGlobalHeaderVisible = _isGlobalHeaderVisible.asStateFlow()

    fun setGlobalHeaderVisible(visible: Boolean) {
        _isGlobalHeaderVisible.value = visible
    }

    // Continuous collapse progress for the global header (0f = expanded, 1f = fully collapsed)
    private val _globalHeaderCollapse = MutableStateFlow(0f)
    val globalHeaderCollapse = _globalHeaderCollapse.asStateFlow()

    fun setGlobalHeaderCollapse(progress: Float) {
        _globalHeaderCollapse.value = progress.coerceIn(0f, 1f)
    }

    private val _marketState = MutableStateFlow<MarketState?>(null)
    val marketState = _marketState.asStateFlow()

    private val _isRiskAccepted = MutableStateFlow(false)
    val isRiskAccepted = _isRiskAccepted.asStateFlow()

    // Risk disclosure setting: when true, show disclaimer on app startup
    private val _showRiskDisclosure = MutableStateFlow(
        getApplication<Application>()
            .getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            .getBoolean("show_risk_disclosure", true)
    )
    val showRiskDisclosure = _showRiskDisclosure.asStateFlow()

    fun setShowRiskDisclosure(show: Boolean) {
        _showRiskDisclosure.value = show
        getApplication<Application>()
            .getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("show_risk_disclosure", show)
            .apply()
    }

    private val _selectedPair = MutableStateFlow(
        BinanceDataStore.pairSnapshot("BTC/USDT") ?: provideForexExplore().first()
    )
    val selectedPair = _selectedPair.asStateFlow()

    private val _cryptoPairs = MutableStateFlow(
        (
            BinanceDataStore.allPairs.value +
                MarketDataStore.allPairs.value.filter { it.category == MarketCategory.CRYPTO } +
                CombinedFallbackDataStore.allPairs.value.filter { it.category == MarketCategory.CRYPTO }
            )
            .distinctBy { it.symbol }
    )
    val cryptoPairs = _cryptoPairs.asStateFlow()

    private val _preMoveCandidates = MutableStateFlow<List<PreMoveCandidate>>(emptyList())
    val preMoveCandidates = _preMoveCandidates.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    private val _isArmed = MutableStateFlow(false)
    val isArmed = _isArmed.asStateFlow()
    private val _commandCenterStatus = MutableStateFlow(CommandCenterStatus())
    val commandCenterStatus = _commandCenterStatus.asStateFlow()

    // Feature flag: when true, Macro Intelligence Stream is promoted as a landing/highlight view
    private val _promoteMacroStream = MutableStateFlow(false)
    val promoteMacroStream = _promoteMacroStream.asStateFlow()

    // Watchlist data (AI-curated) - loaded from JSON
    private val _watchlistItems = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val watchlistItems = _watchlistItems.asStateFlow()

    enum class WatchlistSortMode { PROBABILITY, CONFIDENCE, VOLATILITY, TIME_TO_EVENT }

    private val _watchlistSortMode = MutableStateFlow(WatchlistSortMode.PROBABILITY)
    val watchlistSortMode = _watchlistSortMode.asStateFlow()

    private val _watchlistCategoryFilter = MutableStateFlow<MarketCategory?>(null)
    val watchlistCategoryFilter = _watchlistCategoryFilter.asStateFlow()

    private val _watchlistCompactMode = MutableStateFlow(false)
    val watchlistCompactMode = _watchlistCompactMode.asStateFlow()

    private val _hiddenWatchlistIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenWatchlistIds = _hiddenWatchlistIds.asStateFlow()

    private val _isWatchlistAnalyzing = MutableStateFlow(false)
    val isWatchlistAnalyzing = _isWatchlistAnalyzing.asStateFlow()

    private val _lastWatchlistUpdate = MutableStateFlow(System.currentTimeMillis())
    val lastWatchlistUpdate = _lastWatchlistUpdate.asStateFlow()

    init {
        loadWatchlistFromAI()
    }

    private fun loadWatchlistFromAI() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try loading from assets first
                val jsonString = try {
                    getApplication<Application>().assets.open("ai_watchlist.json").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    // Fallback to external storage
                    val file = java.io.File(getApplication<Application>().filesDir.parent, "ai_watchlist.json")
                    if (file.exists()) file.readText() else null
                }
                
                if (jsonString != null) {
                    val watchlistData = parseWatchlistJson(jsonString)
                    _watchlistItems.value = watchlistData
                    _lastWatchlistUpdate.value = System.currentTimeMillis()
                    android.util.Log.i("ForexViewModel", "✅ Loaded ${watchlistData.size} items from AI watchlist")
                } else {
                    android.util.Log.w("ForexViewModel", "⚠️ AI watchlist not found, using empty list")
                    _watchlistItems.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ForexViewModel", "Failed to load AI watchlist: ${e.message}", e)
                _watchlistItems.value = emptyList()
            }
        }
    }

    private fun parseWatchlistJson(json: String): List<WatchlistItem> {
        val jsonObject = JSONObject(json)
        val itemsArray = jsonObject.getJSONArray("items")
        val items = mutableListOf<WatchlistItem>()
        
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.getJSONObject(i)
            items.add(
                WatchlistItem(
                    id = item.getString("id"),
                    assetName = item.getString("assetName"),
                    status = item.getString("status"),
                    confidence = item.getInt("confidence"),
                    newsRisk = item.getString("newsRisk"),
                    moveProbability = item.getInt("moveProbability"),
                    priority = item.getInt("priority"),
                    preMoveSignal = item.getString("preMoveSignal"),
                    volatilityScore = item.getInt("volatilityScore"),
                    triggerEvent = item.optString("triggerEvent", ""),
                    timeToEvent = item.optString("timeToEvent", ""),
                    price = item.optDouble("price", 0.0),
                    changePercent = item.optDouble("changePercent", 0.0),
                    category = try {
                        MarketCategory.valueOf(item.getString("category"))
                    } catch (e: Exception) {
                        MarketCategory.FOREX
                    },
                    rationale = item.optString("rationale", ""),
                    isNew = item.optBoolean("isNew", false),
                    addedAt = item.optLong("addedAt", System.currentTimeMillis())
                )
            )
        }
        
        return items
    }

    fun setWatchlistSort(mode: WatchlistSortMode) { _watchlistSortMode.value = mode }
    fun setWatchlistCategoryFilter(category: MarketCategory?) { _watchlistCategoryFilter.value = category }
    fun toggleWatchlistCompactMode() { _watchlistCompactMode.value = !_watchlistCompactMode.value }
    fun hideWatchlistItem(id: String) { _hiddenWatchlistIds.value += id }
    fun unhideAllWatchlistItems() { _hiddenWatchlistIds.value = emptySet() }
    fun refreshWatchlist() {
        _isWatchlistAnalyzing.value = true
        _lastWatchlistUpdate.value = System.currentTimeMillis()
        
        // Reload from AI
        loadWatchlistFromAI()
        
        viewModelScope.launch {
            delay(1000)  // Simulate analysis
            _isWatchlistAnalyzing.value = false
        }
    }
    fun markWatchlistItemSeen(id: String) {
        _watchlistItems.value = _watchlistItems.value.map {
            if (it.id == id) it.copy(isNew = false) else it
        }
    }

    private fun syncWatchlistWithLivePrices() {
        _watchlistItems.value = _watchlistItems.value.map { item ->
            val livePair = livePairSnapshot(item.assetName) ?: return@map item
            item.copy(
                price = livePair.price,
                changePercent = livePair.changePercent,
                category = livePair.category
            )
        }
    }

    private fun livePairSnapshot(symbol: String): ForexPair? {
        return BinanceDataStore.pairSnapshot(symbol)
            ?: MarketDataStore.pairSnapshot(symbol)
            ?: CombinedFallbackDataStore.pairSnapshot(symbol)
    }

    private fun mergedMarketPairs(): List<ForexPair> {
        return (
            MarketDataStore.allPairs.value +
                BinanceDataStore.allPairs.value +
                CombinedFallbackDataStore.allPairs.value
            ).distinctBy { it.symbol }
    }

    private fun mergedCryptoPairs(
        marketPairs: List<ForexPair>,
        binancePairs: List<ForexPair>,
        fallbackPairs: List<ForexPair>
    ): List<ForexPair> {
        return (
            binancePairs +
                marketPairs.filter { it.category == MarketCategory.CRYPTO } +
                fallbackPairs.filter { it.category == MarketCategory.CRYPTO }
            ).distinctBy { it.symbol }
    }

    // Initialize persistent trade repository from Application single instance
    val tradeHistoryRepository: TradeHistoryRepository? = myApp.tradeRepository

    /**
     * Example helper to persist a confirmed trade into the persistent TradeHistoryRepository.
     * Call this only after a confirmed fill. This method launches a coroutine on the
     * ViewModel scope and will not block the caller.
     *
     * Example usage after confirmed fill:
     * viewModel.saveConfirmedTrade(
     *   asset = "BTCUSDT",
     *   regimeStack = "H1_HIGH_COMP_LOW_VOL",
     *   direction = "LONG",
     *   entryPrice = 42000.0,
     *   exitPrice = 42350.0,
     *   pnl = 350.0,
     *   win = true,
     *   entryVolatility = 0.12,
     *   entryCorrelation = 0.30
     * )
     */
    fun saveConfirmedTrade(
        asset: String,
        regimeStack: String,
        direction: String,
        entryPrice: Double,
        exitPrice: Double,
        pnl: Double,
        win: Boolean,
        entryVolatility: Double,
        entryCorrelation: Double,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val repo = tradeHistoryRepository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = com.asc.markets.data.trade.TradeEntity(
                    asset = asset,
                    regimeStack = regimeStack,
                    direction = direction,
                    entryPrice = entryPrice,
                    exitPrice = exitPrice,
                    pnl = pnl,
                    win = win,
                    entryVolatility = entryVolatility,
                    entryCorrelation = entryCorrelation,
                    timestamp = timestamp
                )
                repo.saveTrade(entity)
            } catch (t: Throwable) {
                android.util.Log.e("ASC", "Error saving confirmed trade: ${t.message}")
            }
        }
    }

    private fun buildTerminalHistory(limit: Int = 12): String {
        val messages = _terminalLogs.value
            .dropLast(1)
            .takeLast(limit)

        if (messages.isEmpty()) return "No previous terminal turns."

        return buildString {
            appendLine("Recent terminal turns:")
            messages.asReversed().forEach { message ->
                val speaker = when (message.role.lowercase(Locale.US)) {
                    "user" -> "User"
                    "model", "assistant" -> "ASC Terminal"
                    else -> message.role.replaceFirstChar { ch -> ch.titlecase(Locale.US) }
                }
                appendLine("$speaker: ${message.content}")
            }
        }
    }

    // Dashboard tab target (string name of DashboardTab) allows external callers to set which
    // top-tab the Dashboard should show when navigated to (e.g., Home button -> COMMAND_CENTER)
    private val _dashboardTabTarget = MutableStateFlow("COMMAND_CENTER")
    val dashboardTabTarget = _dashboardTabTarget.asStateFlow()

    private val _activeAlgo = MutableStateFlow("MARKET")
    val activeAlgo = _activeAlgo.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(role = "model", content = "Local Analytical Node is ready. Protocol L14 is active.")
    ))
    val terminalLogs = _terminalLogs.asStateFlow()

    // Macro events source (can be updated by network or local ingestion)
    private val _allMacroEvents = MutableStateFlow<List<MacroEvent>>(emptyList())
    val allMacroEvents = _allMacroEvents.asStateFlow()

    // In-app notifications (persisted elsewhere later). Track seen/unseen state here.
    private val _inAppNotifications = MutableStateFlow<List<com.asc.markets.data.NotificationModel>>(emptyList())
    private val _inAppNotifications_OLD_DUMMY = MutableStateFlow<List<com.asc.markets.data.NotificationModel>>(emptyList())
    val inAppNotifications = _inAppNotifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()
    private val _alertNotificationCount = MutableStateFlow(0)
    val alertNotificationCount = _alertNotificationCount.asStateFlow()

    // Filtered list intended for the MacroStream view � ensure ~90% UPCOMING vs CONFIRMED
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

    private val newsService = NewsService()
    // Instantiate BinanceWebSocketManager with Redis configuration read from SharedPreferences so
    // the Redis host/port can be changed at runtime (useful for testing on a phone).
    private val binanceWsManager: com.asc.markets.network.BinanceWebSocketManager by lazy {
        val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
        // Default to laptop LAN IP for physical-device testing. Users can override in settings.
        val redisHost = prefs.getString("redis_host", NetworkConfig.DEFAULT_HOST) ?: NetworkConfig.DEFAULT_HOST
        val redisPort = prefs.getInt("redis_port", 6379)
        val redisPassword = prefs.getString("redis_password", null)
        val redisUseSsl = prefs.getBoolean("redis_use_ssl", false)
        val streamName = prefs.getString("stream_name", "market.ticks.stream") ?: "market.ticks.stream"
        val fieldName = prefs.getString("field_name", "data") ?: "data"
        // 10.0.2.2 is emulator-only; use LAN default for real phone.
        val backendUrl = prefs.getString("backend_url", NetworkConfig.DEFAULT_BACKEND_URL)
        val publishApiKey = prefs.getString("publish_api_key", null)

            com.asc.markets.network.BinanceWebSocketManager(
            scope = viewModelScope,
            redisHost = redisHost,
            redisPort = redisPort,
            redisPassword = redisPassword,
            redisUseSsl = redisUseSsl,
            streamName = streamName,
            fieldName = fieldName,
            backendUrl = backendUrl,
            publishApiKey = publishApiKey
        )
    }

    private val mt5BridgeClient: MT5BridgeClient by lazy {
        val bridgeUrl = NetworkConfig.mt5BridgeUrl(getApplication())
        MT5BridgeClient(
            bridgeUrl = bridgeUrl,
            scope = viewModelScope,
            brokerSuffix = "m",
            onAccountUpdate = { account ->
                emitBrokerAccountLog("EXNESS", account)
            }
        )
    }

    private val cTraderBridgeClient: CTraderBridgeClient by lazy {
        CTraderBridgeClient(
            bridgeUrl = NetworkConfig.cTraderBridgeUrl(getApplication()),
            scope = viewModelScope,
            onAccountUpdate = { account ->
                emitBrokerAccountLog("PEPPERSTONE", account)
            }
        )
    }

    private val tiingoFxManager: TiingoFxWebSocketManager? by lazy {
        val apiKey = BuildConfig.TIINGO_API_KEY.trim()
        android.util.Log.i(
            "TiingoFxWS",
            "BuildConfig Tiingo FX configured=${apiKey.isNotBlank()} threshold=${BuildConfig.TIINGO_THRESHOLD_LEVEL}"
        )
        apiKey
            .takeIf { it.isNotBlank() }
            ?.let { apiKey ->
                TiingoFxWebSocketManager(
                    apiKey = apiKey,
                    scope = viewModelScope,
                    thresholdLevel = BuildConfig.TIINGO_THRESHOLD_LEVEL
                )
            }
    }

    private val tiingoIexManager: TiingoIexWebSocketManager? by lazy {
        val apiKey = BuildConfig.TIINGO_API_KEY.trim()
        android.util.Log.i(
            "TiingoIexWS",
            "BuildConfig Tiingo IEX configured=${apiKey.isNotBlank()} threshold=${BuildConfig.TIINGO_THRESHOLD_LEVEL}"
        )
        apiKey
            .takeIf { it.isNotBlank() }
            ?.let { apiKey ->
                TiingoIexWebSocketManager(
                    apiKey = apiKey,
                    scope = viewModelScope,
                    thresholdLevel = BuildConfig.TIINGO_THRESHOLD_LEVEL
                )
            }
    }

    private val tiingoFxRestClient: TiingoFxRestClient? by lazy {
        BuildConfig.TIINGO_API_KEY
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { apiKey -> TiingoFxRestClient(apiKey, getApplication()) }
    }

    private val tiingoIexRestClient: TiingoIexRestClient? by lazy {
        BuildConfig.TIINGO_API_KEY
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { apiKey -> TiingoIexRestClient(apiKey, getApplication()) }
    }

    private var lastBrokerAccountLog: String? = null

    init {
        CombinedFallbackStore.setManualEnabled(
            getApplication<Application>()
                .getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("combined_fallback_manual_enabled", false)
        )

        val tiingoFxAvailable = tiingoFxManager != null
        val tiingoIexAvailable = tiingoIexManager != null

        // Binance = USDT pairs only (including BTC/USDT, ETH/USDT for most of the app).
        // BTC/USD and ETH/USD are handled by Pepperstone cTrader.
        val usdtSymbols = FOREX_PAIRS
            .filter { it.symbol.endsWith("/USDT") }
            .map { it.symbol.replace("/", "") }
            .distinct()

        val cTraderSymbols = FOREX_PAIRS
            .filter { pair ->
                !pair.symbol.endsWith("/USDT") &&
                    (pair.category == MarketCategory.FOREX ||
                        pair.category == MarketCategory.COMMODITIES ||
                        pair.category == MarketCategory.INDICES ||
                        pair.category == MarketCategory.STOCK ||
                        pair.category == MarketCategory.CRYPTO ||
                        pair.category == MarketCategory.BONDS)
            }
            .map(::pepperstoneCTraderSymbol)
            .distinct()

        // MT5 polling is disabled here to avoid overlapping Pepperstone cTrader crypto prices.
        // Other non-USDT pairs are handled by dedicated live routes when available.
        val mt5PollingSymbols = emptyList<String>()

        val tiingoForexSymbols = FOREX_PAIRS
            .filter { it.category == MarketCategory.FOREX }
            .map { it.symbol.replace("/", "") }
            .distinct()

        val tiingoStockSymbols = FOREX_PAIRS
            .filter { it.category == MarketCategory.STOCK }
            .map { it.symbol.replace("/", "") }
            .distinct()

        android.util.Log.i(
            "TiingoFxWS",
            "ForexViewModel Tiingo startup available=$tiingoFxAvailable symbols=${tiingoForexSymbols.joinToString(",")}"
        )
        android.util.Log.i(
            "TiingoIexWS",
            "ForexViewModel Tiingo IEX startup available=${tiingoIexManager != null} symbols=${tiingoStockSymbols.joinToString(",")}"
        )
        android.util.Log.i(
            "CTraderBridge",
            "ForexViewModel Pepperstone cTrader startup symbols=${cTraderSymbols.joinToString(",")}"
        )

        viewModelScope.launch {
            cTraderBridgeClient.connectionState.collect { state ->
                android.util.Log.i("CTraderBridge", "Pepperstone cTrader bridge state=$state")
            }
        }

        viewModelScope.launch {
            cTraderBridgeClient.priceUpdates.collect { pair ->
                MarketDataStore.updatePair(pair)
                PriceStreamManager.updatePrice(pair.symbol, pair.price)
                markPrimaryLiveDataRestored()
            }
        }

        viewModelScope.launch {
            PreMoveIntelligenceStore.candidates.collect { candidates ->
                _preMoveCandidates.value = candidates
            }
        }

        if (usdtSymbols.isNotEmpty()) {
            binanceWsManager.connect(usdtSymbols)
        }

        if (cTraderSymbols.isNotEmpty()) {
            cTraderBridgeClient.connect(cTraderSymbols)
        }

        derivService.connect()
        viewModelScope.launch {
            delay(1000)
            // Subscribe to FOREX, commodities, and indices via Deriv (primary source)
            listOf("EUR/USD", "GBP/USD", "USD/JPY", "USD/CHF", "AUD/USD", "XAU/USD", "XAG/USD", "Crude-F", "Brent-F", "NAS100", "US30", "SPX500").forEach { symbol ->
                derivService.subscribe(symbol)
            }
        }

        refreshBondData()

        // Tiingo FX as fallback - connect only if Deriv is unavailable or fails
        if (tiingoForexSymbols.isNotEmpty()) {
            viewModelScope.launch {
                delay(5000) // Wait for Deriv to attempt connection
                // Check if Deriv is connected, if not use Tiingo as fallback
                if (!derivService.isConnected()) {
                    android.util.Log.w("ForexViewModel", "Deriv not connected, using Tiingo FX as fallback")
                    if (tiingoFxManager != null) {
                        tiingoFxManager?.connect(tiingoForexSymbols)
                    } else {
                        android.util.Log.w("TiingoFxWS", "Tiingo FX unavailable: TIINGO_API_KEY is not configured")
                    }
                }
            }
        }

        if (tiingoStockSymbols.isNotEmpty()) {
            if (tiingoIexManager != null) {
                tiingoIexManager?.connect(tiingoStockSymbols)
            } else {
                android.util.Log.w("TiingoIexWS", "Tiingo IEX unavailable: TIINGO_API_KEY is not configured")
            }
        }

        // Tiingo FX REST fallback - only run if Deriv is not connected
        if (tiingoForexSymbols.isNotEmpty()) {
            tiingoFxRestClient?.let { restClient ->
                viewModelScope.launch {
                    while (isActive) {
                        if (!cTraderBridgeClient.hasAnyRecentPrice() && !derivService.isConnected()) {
                            if (!canUseCombinedFallback(
                                    source = "Combined fallback",
                                    reason = "Primary live data is unavailable. Combined fallback is ready."
                                )
                            ) {
                                delay(COMBINED_FALLBACK_RETRY_MS)
                                continue
                            }
                            val pairs = restClient.fetchTopPairs(tiingoForexSymbols)
                            pairs.forEach { pair ->
                                android.util.Log.i("TiingoFxREST", "Routing Tiingo FX REST top to CombinedFallbackDataStore ${pair.symbol} ${pair.price}")
                                CombinedFallbackDataStore.updatePair(pair)
                            }
                        }
                        delay(TIINGO_REST_REFRESH_MS)
                    }
                }
            }
        }

        if (tiingoStockSymbols.isNotEmpty()) {
            tiingoIexRestClient?.let { restClient ->
                viewModelScope.launch {
                    while (isActive) {
                        if (!canUseCombinedFallback(
                                source = "Combined fallback",
                                reason = "Pepperstone primary data is unavailable. Combined fallback is ready."
                            )
                        ) {
                            delay(COMBINED_FALLBACK_RETRY_MS)
                            continue
                        }
                        val pairs = restClient.fetchTopPairs(tiingoStockSymbols)
                        pairs.forEach { pair ->
                            android.util.Log.i("TiingoIexREST", "Routing Tiingo IEX REST top to CombinedFallbackDataStore ${pair.symbol} ${pair.price}")
                            CombinedFallbackDataStore.updatePair(pair)
                        }
                        delay(TIINGO_REST_REFRESH_MS)
                    }
                }
            }
        }

        if (mt5PollingSymbols.isNotEmpty()) {
            viewModelScope.launch {
                while (isActive) {
                    mt5PollingSymbols.forEach { symbol ->
                        val tick = mt5BridgeClient.getTick(symbol) ?: return@forEach
                        val price = (tick.bid + tick.ask) / 2.0
                        PriceStreamManager.updatePrice(symbol, price)
                    }
                    delay(3000)
                }
            }
        }

        tiingoFxManager?.let { manager ->
            viewModelScope.launch {
                manager.connectionState.collect { state ->
                    android.util.Log.i("TiingoFxWS", "Tiingo FX connection state=$state")
                }
            }
            viewModelScope.launch {
                manager.priceUpdates.collect { pair ->
                    if (cTraderBridgeClient.hasRecentPrice(pair.symbol)) {
                        android.util.Log.i("TiingoFxWS", "Skipping Tiingo FX update ${pair.symbol}; Pepperstone cTrader is primary")
                        return@collect
                    }
                    if (!canUseCombinedFallback(
                            source = "Combined fallback",
                            reason = "Primary live data is stale. Combined fallback is ready."
                        )
                    ) {
                        android.util.Log.i("TiingoFxWS", "Waiting for Combined fallback approval before routing ${pair.symbol}")
                        return@collect
                    }
                    android.util.Log.i("TiingoFxWS", "Routing Tiingo FX update to CombinedFallbackDataStore ${pair.symbol} ${pair.price}")
                    CombinedFallbackDataStore.updatePair(pair)
                }
            }
        }

        tiingoIexManager?.let { manager ->
            viewModelScope.launch {
                manager.connectionState.collect { state ->
                    android.util.Log.i("TiingoIexWS", "Tiingo IEX connection state=$state")
                }
            }
            viewModelScope.launch {
                manager.priceUpdates.collect { pair ->
                    if (!canUseCombinedFallback(
                            source = "Combined fallback",
                            reason = "Pepperstone primary data is unavailable. Combined fallback is ready."
                        )
                    ) {
                        android.util.Log.i("TiingoIexWS", "Waiting for Pepperstone availability or Combined fallback approval before routing ${pair.symbol}")
                        return@collect
                    }
                    android.util.Log.i("TiingoIexWS", "Routing Tiingo IEX update to CombinedFallbackDataStore ${pair.symbol} ${pair.price}")
                    CombinedFallbackDataStore.updatePair(pair)
                }
            }
        }

        // Throttle market updates to backend every 500ms
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                try {
                    val pairs = mergedMarketPairs()
                    if (pairs.isNotEmpty()) {
                        val assetsMap = pairs.associate { pair ->
                            val symbol = pair.symbol.replace("/", "")
                            symbol to com.asc.markets.data.remote.MarketAssetSnapshot(
                                price = pair.price,
                                timestamp = java.time.Instant.now().toString(),
                                // For now we use price as bid/ask as ForexPair doesn't have them
                                bid = pair.price - 0.1,
                                ask = pair.price + 0.1,
                                volume = 0.0
                            )
                        }
                        val request = com.asc.markets.data.remote.MarketUpdateRequest(assets = assetsMap)
                        aiRepository.updateMarketData(request)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ASC", "Failed to push market update: ${e.message}")
                }
            }
        }

        viewModelScope.launch {
            binanceWsManager.priceUpdates.collect { pair ->
                android.util.Log.i("BinanceWS", "Routing Binance update to BinanceDataStore ${pair.symbol} ${pair.price}")
                BinanceDataStore.updatePair(pair)
            }
        }

        viewModelScope.launch {
            combine(
                MarketDataStore.allPairs,
                BinanceDataStore.allPairs,
                CombinedFallbackDataStore.allPairs
            ) { marketPairs, binancePairs, fallbackPairs ->
                Triple(marketPairs, binancePairs, fallbackPairs)
            }.collect { (marketPairs, binancePairs, fallbackPairs) ->
                _cryptoPairs.value = mergedCryptoPairs(marketPairs, binancePairs, fallbackPairs)
                livePairSnapshot(_selectedPair.value.symbol)?.let { latest ->
                    _selectedPair.value = latest
                }
                syncWatchlistWithLivePrices()
            }
        }

        viewModelScope.launch {
            binanceWsManager.accountStatus.collect { statusJson ->
                _terminalLogs.value = listOf(ChatMessage(role = "model", content = "Binance account status: $statusJson")) + _terminalLogs.value
            }
        }

        viewModelScope.launch {
            // Start continuous polling of RSS feeds and API news for Macro Stream
            launch(Dispatchers.IO) {
                while (isActive) {
                    try {
                        val articles = newsService.fetchAllNews()
                        if (articles.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            val newsEvents = articles.map { article ->
                                val eventTime = try {
                                    java.time.OffsetDateTime.parse(article.publishedAt).toInstant().toEpochMilli()
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                                MacroEvent(
                                    id = article.id,
                                    title = article.title,
                                    currency = article.intelligence?.asset_tags?.firstOrNull() ?: "GLOBAL",
                                    datetimeUtc = eventTime,
                                    priority = when (val score = article.intelligence?.impact_score) {
                                        null -> ImpactPriority.LOW
                                        in 80.0..100.0 -> ImpactPriority.CRITICAL
                                        in 60.0..80.0 -> ImpactPriority.HIGH
                                        in 40.0..60.0 -> ImpactPriority.MEDIUM
                                        else -> ImpactPriority.LOW
                                    },
                                    status = if (eventTime > now) MacroEventStatus.UPCOMING else MacroEventStatus.CONFIRMED,
                                    source = article.source,
                                    details = article.summary
                                )
                            }
                            // Ingest into the existing macro stream logic
                            withContext(Dispatchers.Main) {
                                ingestMacroEventsFromSources(newsEvents)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ASC", "Error polling news for Macro Stream: ${e.message}")
                    }
                    delay(60_000) // Poll every 60 seconds
                }
            }

            delay(1200)
            _isInitializing.value = false
            // ensure initial stream list is computed
            _macroStreamEvents.value = computeMacroStreamList(_allMacroEvents.value)

            // Read persisted preferences first so user toggles survive restarts
            try {
                val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
                val persisted = prefs.getBoolean("promote_macro_stream", false)
                _promoteMacroStream.value = persisted
                // Removed auto-navigation to MACRO_STREAM on startup
                // if (persisted) {
                //     _currentView.value = AppView.MACRO_STREAM
                //     _sessionLandingCount.value = _sessionLandingCount.value + 1
                // }
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
                    // Removed auto-navigation to MACRO_STREAM on startup
                    // if (promoteDefault) {
                    //     _currentView.value = AppView.MACRO_STREAM
                    //     _sessionLandingCount.value = _sessionLandingCount.value + 1
                    // }
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

            // Initial AI deployments fetch
            aiRepository.fetchLatestDeployments()

            // Start periodic AI deployments polling for real-time updates
            viewModelScope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(AI_DEPLOYMENTS_POLL_INTERVAL_MS) // Poll every 5 seconds for real-time updates
                    try {
                        aiRepository.fetchLatestDeployments()
                    } catch (e: Exception) {
                        android.util.Log.e("ForexViewModel", "Error fetching AI deployments: ${e.message}")
                    }
                }
            }

            // Synchronize AI state with Dashboard Data Providers for legacy widget compatibility
            viewModelScope.launch {
                aiDeployments.filterNotNull().collect { res ->
                    AIAppManager.updateDashboardSession(
                        nextEventTime = res.last_updated?.takeLast(8) ?: "N/A",
                        nextEventLabel = "AI SYNC",
                        globalRegimeText = "AI Node active. Processing ${res.count} deployments. Monitoring for high-probability institutional liquidity sweeps."
                    )
                    
                    if (res.final_decision.isNotEmpty()) {
                        // Ingest into Audit Records
                        res.final_decision.forEach { decision ->
                            val record = com.asc.markets.data.AuditRecord(
                                headline = decision.journal_label ?: "AI DEPLOYMENT",
                                impact = decision.journal_priority ?: "INFO",
                                confidence = (decision.journal_score ?: 0.0).toInt(),
                                assets = decision.asset_1 ?: "GLOBAL",
                                status = "CONFIRMED",
                                reasoning = decision.portfolio_decision_reason ?: "",
                                direction = decision.journal_direction,
                                riskPct = decision.final_risk_pct,
                                deploymentLabel = decision.portfolio_decision_label
                            )
                            // Avoid duplicates by headline and asset for now or ID if available
                            if (!_auditRecords.value.any { it.headline == record.headline && it.assets == record.assets && it.timeUtc > System.currentTimeMillis() - 60000 }) {
                                appendAuditRecord(record)
                            }
                        }

                        // Use the highest confidence signal to drive dashboard sentiment boxes
                        val top = res.final_decision.maxByOrNull { it.journal_score ?: 0.0 }
                        top?.let { t ->
                            AIAppManager.updateAISentiment(
                                sentimentScore = (t.journal_score ?: 0.0).toInt(),
                                sentimentState = t.journal_direction?.uppercase() ?: "NEUTRAL",
                                confidence = (t.journal_score ?: 0.0).toInt()
                            )
                            AIAppManager.updateProbabilityScore(
                                scoreValue = (t.journal_score ?: 0.0).toInt(),
                                confidenceLevel = if ((t.journal_score ?: 0.0) >= 75) "HIGH" else "MODERATE",
                                prediction = t.journal_direction?.uppercase() ?: "NEUTRAL"
                            )
                        }
                    }
                }
            }
        }
    }

    // Mark a single notification as seen (reduces unread count)
    fun markNotificationSeen(id: String) {
        val current = _inAppNotifications.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx >= 0 && !current[idx].seen) {
            current[idx] = current[idx].copy(seen = true)
            _inAppNotifications.value = current
            _unreadCount.value = current.count { !it.seen }
            _alertNotificationCount.value = calculateAlertNotificationCount(current)
        }
    }

    fun markAllNotificationsSeen() {
        val updated = _inAppNotifications.value.map { it.copy(seen = true) }
        _inAppNotifications.value = updated
        _unreadCount.value = 0
        _alertNotificationCount.value = 0
    }

    fun appendInAppNotification(notification: com.asc.markets.data.NotificationModel) {
        val updated = listOf(notification) + _inAppNotifications.value
        _inAppNotifications.value = updated
        _unreadCount.value = updated.count { !it.seen }
        _alertNotificationCount.value = calculateAlertNotificationCount(updated)
    }

    /**
     * Centralised helper: register a VigilanceNode by appending both an AuditRecord
     * and an in-app notification.  Call this from AlertsScreen / CreateAlertScreen
     * instead of duplicating the logic in each Composable.
     */
    fun registerVigilanceNode(node: com.asc.markets.logic.VigilanceNode, prefix: String = "Alert") {
        val impact = when {
            node.confidenceScore >= 75 -> "CRITICAL"
            node.confidenceScore >= 50 -> "HIGH"
            else -> "INFO"
        }
        appendAuditRecord(
            com.asc.markets.data.AuditRecord(
                id = node.id,
                headline = node.description.ifEmpty { node.trigger },
                impact = impact,
                confidence = node.confidenceScore,
                assets = node.pair,
                status = "ACTIVE",
                timeUtc = System.currentTimeMillis(),
                reasoning = node.description,
                nodeId = node.id,
                integrityHash = ""
            )
        )
        appendInAppNotification(
            com.asc.markets.data.NotificationModel(
                id = java.util.UUID.randomUUID().toString(),
                type = "ALERT",
                msg = "$prefix: ${node.description.ifEmpty { node.trigger }}",
                time = "Just now",
                severity = impact,
                seen = false,
                symbol = node.pair,
                timeframe = node.timeframe,
                targetView = com.asc.markets.data.AppView.TRADING_ASSISTANT.name
            )
        )
    }

    private fun calculateAlertNotificationCount(
        notifications: List<com.asc.markets.data.NotificationModel>
    ): Int {
        return notifications.count { notification ->
            !notification.seen && notification.type.equals("ALERT", ignoreCase = true)
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
            _previousWasDrawerOpen.value = _isDrawerOpen.value
            
            // Reset header collapse and visibility states when navigating to a new view
            // to ensure the main menu is visible by default.
            _globalHeaderCollapse.value = 0f
            _isGlobalHeaderVisible.value = true
        }
        _currentView.value = view
    }

    /**
     * Navigate back to the previously recorded view. Falls back to DASHBOARD when none recorded.
     */
    fun navigateBack() {
        val prev = _previousView.value ?: AppView.DASHBOARD
        _currentView.value = prev
        
        // Reset header collapse and visibility states on back navigation as well
        _globalHeaderCollapse.value = 0f
        _isGlobalHeaderVisible.value = true

        // If navigation originated from an open drawer, reopen it to return 'where' the user clicked
        if (_previousWasDrawerOpen.value) {
            _isDrawerOpen.value = true
        }
        _previousView.value = null
        _previousWasDrawerOpen.value = false
    }
    fun toggleSidebar() { _isSidebarCollapsed.value = !_isSidebarCollapsed.value }
    fun toggleDrawer() { _isDrawerOpen.value = !_isDrawerOpen.value }
    fun openDrawer() { _isDrawerOpen.value = true }
    fun closeDrawer() { _isDrawerOpen.value = false }
    fun openCommandPalette() { _isCommandPaletteOpen.value = true }
    fun closeCommandPalette() { _isCommandPaletteOpen.value = false }
    fun acceptRisk() { _isRiskAccepted.value = true }
    fun selectPair(pair: ForexPair) { 
        _selectedPair.value = livePairSnapshot(pair.symbol) ?: pair
        _currentView.value = AppView.DASHBOARD
    }
    
    /**
     * Select a pair without changing the current view (no navigation).
     * Use this when updating selection from within a screen that should stay active.
     */
    fun selectPairNoNavigate(pair: ForexPair) {
        _selectedPair.value = livePairSnapshot(pair.symbol) ?: pair
    }

    fun selectPairBySymbol(symbol: String) {
        val pair = livePairSnapshot(symbol)
        pair?.let {
            selectPair(it)
        }
    }

    /**
     * Select a pair without changing the current view (no navigation).
     * Use this when updating selection from within a modal or settings screen.
     */
    fun selectPairBySymbolNoNavigate(symbol: String) {
        val pair = livePairSnapshot(symbol)
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
        val text = cmd.trim()
        if (text.isBlank()) return

        val userMsg = ChatMessage(role = "user", content = text)
        _terminalLogs.value = listOf(userMsg) + _terminalLogs.value
        
        viewModelScope.launch {
            val upper = text.uppercase()
            val latest = aiDeployments.value ?: aiRepository.fetchLatestDeployments().getOrNull()
            val response = when {
                upper == "ARM" || upper == "ARM SURVEILLANCE" || upper == "ARM_SURVEILLANCE" -> {
                    _isArmed.value = true
                    TradingAssistantEngine.armed = true
                    TradingAssistantEngine.safetyLockActive = false
                    "Surveillance is armed now."
                }
                upper == "DISARM" || upper == "DISARM SURVEILLANCE" || upper == "DISARM_SURVEILLANCE" -> {
                    _isArmed.value = false
                    TradingAssistantEngine.armed = false
                    TradingAssistantEngine.safetyLockActive = true
                    "Surveillance is disarmed now."
                }
                upper == "ACCOUNT" -> {
                    when (ChartFeedType.streamCurrent(getApplication())) {
                        ChartFeedType.BINANCE -> {
                            binanceWsManager.fetchAccountStatus()
                            "I�m requesting Binance account status now."
                        }
                        ChartFeedType.BINANCE_CONNECT -> {
                            "Binance Connect is view-only mode. No account data available."
                        }
ChartFeedType.EXNESS -> {
                            mt5BridgeClient.requestAccountStatus()
                            "I�m requesting Exness account status now."
                        }
                        ChartFeedType.PEPPERSTONE_CTRADER -> {
                            cTraderBridgeClient.requestAccountStatus()
                            "I'm requesting Pepperstone account status now."
                        }
                        ChartFeedType.PEPPERSTONE_DEMO -> {
                            cTraderBridgeClient.requestAccountStatus()
                            "I'm requesting Pepperstone Demo account status now."
                        }
                    }
                }
                shouldAnswerTerminalSnapshotDirectly(text) -> {
                    buildTerminalSnapshotReply(text, latest)
                }
                upper == "RUN AI" || upper == "RUN ASC AI" || upper == "RUN PIPELINE" || upper == "RUN ASC PIPELINE" -> {
                    aiRepository.runAiPipeline().fold(
                        onSuccess = { resp ->
                            if (resp.success) {
                                "I ran the ASC AI pipeline and refreshed ${resp.final_decision.size} decisions. ${resp.message ?: "Latest deployments refreshed."}"
                            } else {
                                "I ran the ASC AI pipeline, but it returned an unsuccessful response. ${resp.message ?: "Please try refreshing again."}"
                            }
                        },
                        onFailure = { err ->
                            "I couldn�t run the ASC AI pipeline: ${err.message ?: "unknown error"}"
                        }
                    )
                }
                upper == "ASC" || upper == "ASC STATUS" || upper == "AI STATUS" || upper == "DEPLOYMENTS" || upper == "REFRESH AI" -> {
                    buildTerminalAscSummary(latest)
                }
                isDeepTerminalCommand(upper) -> {
                    val result = TradingAssistantEngine.handleInput(text)
                    syncTerminalEngineState()
                    result.first
                }
                else -> {
                    val terminalHistory = buildTerminalHistory()
                    AscAiTextExplainer.explain(
                        userQuery = text,
                        personaName = "Terminal Desk",
                        personaInstruction = "Deep operator terminal. Explain ASC AI deployment state, selected assets, decision labels, risk, entry windows, exit plans, live tick state, and what the system is waiting for. Do not create a new signal. Use plain human-readable headings only. Do not use brackets or underscore-separated writeups.",
                        deployments = latest,
                        appContext = buildChatAppContext(latest),
                        conversationHistory = terminalHistory
                    )
                }
            }
            _terminalLogs.value = listOf(ChatMessage(role = "model", content = response)) + _terminalLogs.value
        }
    }

    fun askAscAiTextLayer(
        userQuery: String,
        personaName: String,
        personaInstruction: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val latest = aiDeployments.value ?: aiRepository.fetchLatestDeployments().getOrNull()
            val response = AscAiTextExplainer.explain(
                userQuery = userQuery,
                personaName = personaName,
                personaInstruction = personaInstruction,
                deployments = latest,
                appContext = buildChatAppContext(latest)
            )
            onResult(response)
        }
    }

    fun sendAscChatMessage(
        userQuery: String,
        personaName: String,
        personaInstruction: String
    ) {
        val text = userQuery.trim()
        if (text.isBlank() || _ascChatResponding.value) return

        val userMessage = ChatMessage(role = "user", content = text)
        val shouldRenameActiveSession = _ascChatMessages.value.none { it.role.equals("user", ignoreCase = true) }
        if (shouldRenameActiveSession) {
            updateActiveAscChatSession { session ->
                session.copy(
                    title = buildAscChatSessionTitleFromPrompt(text),
                    messages = (session.messages + userMessage).takeLast(80)
                )
            }
        } else {
            appendAscChatMessage(userMessage)
        }
        _ascChatResponding.value = true

        viewModelScope.launch {
            val latest = aiDeployments.value ?: aiRepository.fetchLatestDeployments().getOrNull()
            val conversationHistory = buildAscChatHistory()
            val response = AscAiTextExplainer.explain(
                userQuery = text,
                personaName = personaName,
                personaInstruction = personaInstruction,
                deployments = latest,
                appContext = buildChatAppContext(latest),
                conversationHistory = conversationHistory
            )
            appendAscChatMessage(ChatMessage(role = "model", content = response))
            _ascChatResponding.value = false
        }
    }

    fun addAscChatSystemMessage(content: String) {
        appendAscChatMessage(ChatMessage(role = "model", content = content))
    }

    private fun pushTerminalLog(content: String) {
        viewModelScope.launch {
            _terminalLogs.value = listOf(ChatMessage(role = "model", content = content)) + _terminalLogs.value
        }
    }

    private fun emitBrokerAccountLog(brokerName: String, account: Mt5Service.AccountInfo) {
        val content = "$brokerName account status: ${formatAccountSummary(account)}"
        if (content == lastBrokerAccountLog) return
        lastBrokerAccountLog = content
        pushTerminalLog(content)
    }

    private fun formatAccountSummary(account: Mt5Service.AccountInfo): String {
        return "balance ${chatFmt(account.balance)}, equity ${chatFmt(account.equity)}, floating PnL ${chatFmt(account.unrealizedPnl)}, realized PnL ${chatFmt(account.realizedPnl)}, margin ${chatFmt(account.margin)}, free margin ${chatFmt(account.availableFunds)}, orders margin ${chatFmt(account.ordersMargin)}, margin buffer ${chatFmt(account.marginBuffer)}"
    }

    fun setAscChatPersona(personaId: String) {
        val resolvedId = ANALYST_MODELS.firstOrNull { it.id == personaId }?.id ?: ANALYST_MODELS.first().id
        _ascChatPersonaId.value = resolvedId
        chatPrefs.edit().putString("persona_id", resolvedId).apply()
    }

    fun setAscChatContextPage(page: AppView) {
        val resolvedPage = AppView.values().firstOrNull { it.name == page.name } ?: AppView.CHAT
        _ascChatContextPageId.value = resolvedPage.name
        chatPrefs.edit().putString("chat_context_page_id", resolvedPage.name).apply()
    }

    fun clearAscChatContextPage() {
        _ascChatContextPageId.value = GLOBAL_CHAT_CONTEXT_ID
        chatPrefs.edit().putString("chat_context_page_id", GLOBAL_CHAT_CONTEXT_ID).apply()
    }

    fun startNewAscChatSession() {
        val newSession = AscChatSession(
            id = java.util.UUID.randomUUID().toString(),
            title = "New Chat",
            createdAtMillis = System.currentTimeMillis(),
            messages = emptyList()
        )
        _ascChatSessions.value = listOf(newSession) + _ascChatSessions.value
        _ascChatSessionId.value = newSession.id
        _ascChatMessages.value = emptyList()
        persistAscChatState()
    }

    fun setActiveAscChatSession(sessionId: String) {
        val session = _ascChatSessions.value.firstOrNull { it.id == sessionId } ?: return
        _ascChatSessionId.value = session.id
        _ascChatMessages.value = session.messages
        persistAscChatState()
    }

    fun deleteAscChatSession(sessionId: String) {
        val remainingSessions = _ascChatSessions.value.filterNot { it.id == sessionId }
        if (remainingSessions.isEmpty()) {
            val replacement = createEmptyAscChatSession()
            _ascChatSessions.value = listOf(replacement)
            _ascChatSessionId.value = replacement.id
            _ascChatMessages.value = replacement.messages
            persistAscChatState()
            return
        }

        val activeSessionId = _ascChatSessionId.value
        val nextActiveSessionId = when {
            activeSessionId == sessionId -> remainingSessions.first().id
            remainingSessions.any { it.id == activeSessionId } -> activeSessionId
            else -> remainingSessions.first().id
        }

        _ascChatSessions.value = remainingSessions
        _ascChatSessionId.value = nextActiveSessionId
        _ascChatMessages.value = remainingSessions.firstOrNull { it.id == nextActiveSessionId }?.messages ?: emptyList()
        persistAscChatState()
    }

    fun activeAscChatSessionTitle(): String {
        return _ascChatSessions.value.firstOrNull { it.id == _ascChatSessionId.value }?.title ?: "Chat"
    }

    fun clearAscChatMessages() {
        updateActiveAscChatSession { session -> session.copy(messages = emptyList()) }
    }

    private fun appendAscChatMessage(message: ChatMessage) {
        updateActiveAscChatSession { session ->
            session.copy(messages = (session.messages + message).takeLast(80))
        }
    }

    private fun buildAscChatHistory(limit: Int = 12): String {
        val messages = _ascChatMessages.value
            .dropLast(1)
            .takeLast(limit)

        if (messages.isEmpty()) return "No previous conversation turns."

        return buildString {
            appendLine("Recent conversation turns:")
            messages.forEach { message ->
                val speaker = when (message.role.lowercase(Locale.US)) {
                    "user" -> "User"
                    "model", "assistant" -> "ASC Engine"
                    else -> message.role.replaceFirstChar { ch -> ch.titlecase(Locale.US) }
                }
                appendLine("$speaker: ${message.content}")
            }
        }
    }

    private fun shouldAnswerTerminalSnapshotDirectly(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.US)
        val keywords = listOf(
            "balance",
            "equity",
            "pnl",
            "profit",
            "loss",
            "account",
            "trade",
            "trades",
            "order",
            "orders",
            "position",
            "positions",
            "deployment",
            "deployments",
            "status",
            "refresh",
            "pipeline",
            "armed",
            "disarm",
            "prompt",
            "chat",
            "help"
        )
        return keywords.any { normalized.contains(it) }
    }

    private fun buildTerminalSnapshotReply(text: String, deployments: LatestDeploymentsResponse?): String {
        val normalized = text.trim().lowercase(Locale.US)
        val snapshot = PaperTradingSnapshotStore.snapshot

        val asksForAccount = listOf("balance", "equity", "pnl", "profit", "loss", "account").any { normalized.contains(it) }
        if (asksForAccount) {
            if (!snapshot.hasLiveAccountData && snapshot.balance == 0.0 && snapshot.equity == 0.0 && snapshot.activeTrades == 0 && snapshot.activeOrders == 0) {
                return "I don�t have live account data yet. Please refresh the app state, then ask me again."
            }

            return buildString {
                append("Here�s your account snapshot: ")
                append("balance ${chatFmt(snapshot.balance)}, ")
                append("equity ${chatFmt(snapshot.equity)}, ")
                append("floating PnL ${chatFmt(snapshot.floatingPnl)}, ")
                append("realized PnL ${chatFmt(snapshot.realizedPnl)}, ")
                append("active trades ${snapshot.activeTrades}, ")
                append("active orders ${snapshot.activeOrders}.")
            }
        }

        val asksForDeployment = listOf("deployment", "deployments", "status", "pipeline", "refresh", "armed", "disarm").any { normalized.contains(it) }
        if (asksForDeployment) {
            return buildTerminalAscSummary(deployments)
        }

        if (listOf("prompt", "chat", "help").any { normalized.contains(it) }) {
            return "You can just talk to me naturally. Try asking about your balance, trades, deployments, risk, or current chart, and I�ll answer in plain language."
        }

        return "Ask me naturally about your balance, trades, deployments, risk, or current chart, and I�ll answer in plain language."
    }

    private fun persistAscChatState() {
        val jsonArray = JSONArray()
        _ascChatSessions.value.forEach { session ->
            jsonArray.put(JSONObject().apply {
                put("id", session.id)
                put("title", session.title)
                put("createdAtMillis", session.createdAtMillis)
                val messagesArray = JSONArray()
                session.messages.forEach { message ->
                    messagesArray.put(JSONObject().apply {
                        put("id", message.id)
                        put("role", message.role)
                        put("content", message.content)
                        put("timestamp", message.timestamp)
                    })
                }
                put("messages", messagesArray)
            })
        }
        chatPrefs.edit()
            .putString(CHAT_SESSIONS_KEY, jsonArray.toString())
            .putString(CHAT_ACTIVE_SESSION_ID_KEY, _ascChatSessionId.value)
            .putString("messages", JSONArray().apply {
                _ascChatMessages.value.forEach { message ->
                    put(JSONObject().apply {
                        put("id", message.id)
                        put("role", message.role)
                        put("content", message.content)
                        put("timestamp", message.timestamp)
                    })
                }
            }.toString())
            .apply()
    }

    private fun updateActiveAscChatSession(transform: (AscChatSession) -> AscChatSession) {
        val currentId = _ascChatSessionId.value
        val nextSessions = _ascChatSessions.value.map { session ->
            if (session.id == currentId) transform(session) else session
        }
        val activeSession = nextSessions.firstOrNull { it.id == currentId } ?: return
        _ascChatSessions.value = nextSessions
        _ascChatMessages.value = activeSession.messages
        persistAscChatState()
    }

    private fun loadAscChatPersonaId(): String {
        val saved = chatPrefs.getString("persona_id", ANALYST_MODELS.first().id) ?: ANALYST_MODELS.first().id
        return ANALYST_MODELS.firstOrNull { it.id == saved }?.id ?: ANALYST_MODELS.first().id
    }

    private fun loadAscChatContextPageId(): String {
        val saved = chatPrefs.getString("chat_context_page_id", AppView.CHAT.name) ?: AppView.CHAT.name
        return when {
            saved == GLOBAL_CHAT_CONTEXT_ID -> GLOBAL_CHAT_CONTEXT_ID
            AppView.values().any { it.name == saved } -> saved
            else -> AppView.CHAT.name
        }
    }

    private fun buildSharedCalendarContext(): String {
        val display = CalendarSnapshotStore.latestDisplayPayload
        val ai = CalendarSnapshotStore.latestAiPayload

        if (display == null && ai == null) return "calendar_shared_context=unavailable"

        val nextHolidayDisplay = display?.events?.firstOrNull {
            val title = it.title.lowercase(Locale.US)
            title.contains("holiday") || title.contains("bank holiday")
        }
        val nextHolidayAi = if (nextHolidayDisplay == null) {
            ai?.events?.firstOrNull {
                val title = it.title.lowercase(Locale.US)
                title.contains("holiday") || title.contains("bank holiday")
            }
        } else {
            null
        }

        return buildString {
            appendLine("calendar_shared_context_available=true")
            display?.let { payload ->
                appendLine("calendar_shared_source=display")
                appendLine("calendar_shared_selected_date=${payload.selectedDateIso}")
                appendLine("calendar_shared_range=${payload.rangeStartIso} -> ${payload.rangeEndIso}")
                appendLine("calendar_shared_header=${payload.headerDateLabel}")
                appendLine("calendar_shared_event_count=${payload.events.size}")
            } ?: ai?.let { payload ->
                appendLine("calendar_shared_source=ai")
                appendLine("calendar_shared_selected_date=${payload.selectedDateIso}")
                appendLine("calendar_shared_range=${payload.rangeStartIso} -> ${payload.rangeEndIso}")
                appendLine("calendar_shared_event_count=${payload.events.size}")
            }

            ai?.let { payload ->
                appendLine("calendar_shared_source=ai")
                appendLine("calendar_shared_selected_date=${payload.selectedDateIso}")
                appendLine("calendar_shared_range=${payload.rangeStartIso} -> ${payload.rangeEndIso}")
                appendLine("calendar_shared_event_count_ai=${payload.events.size}")
            }

            nextHolidayDisplay?.let { event ->
                appendLine("calendar_shared_next_holiday=${event.releaseTimeLabel} ${event.currencyCode} ${event.title}")
            }

            nextHolidayAi?.let { event ->
                appendLine("calendar_shared_next_holiday_ai=${event.isoDateTime} ${event.currencyCode} ${event.title}")
            }

            val sortedDisplayEvents = display?.events?.sortedWith(compareByDescending<com.trading.app.models.EconomicCalendarDisplayEvent> {
                when (it.importance.lowercase(Locale.US)) {
                    "high" -> 3
                    "medium" -> 2
                    "low" -> 1
                    else -> 0
                }
            }.thenBy { it.releaseTimeLabel }) ?: emptyList()

            sortedDisplayEvents.take(15).forEach { event ->
                appendLine(
                    "calendar_shared_display_event=${event.releaseTimeLabel} ${event.currencyCode} ${event.title} actual=${event.actual} forecast=${event.forecast} previous=${event.previous} importance=${event.importance}"
                )
            }

            val sortedAiEvents = ai?.events?.sortedWith(compareByDescending<com.trading.app.models.EconomicCalendarAiEvent> {
                when (it.importance.lowercase(Locale.US)) {
                    "high" -> 3
                    "medium" -> 2
                    "low" -> 1
                    else -> 0
                }
            }.thenBy { it.isoDateTime }) ?: emptyList()

            sortedAiEvents.take(15).forEach { event ->
                appendLine(
                    "calendar_shared_event=${event.isoDateTime} ${event.currencyCode} ${event.title} actual=${event.actual} forecast=${event.forecast} previous=${event.previous} importance=${event.importance}"
                )
            }
        }
    }

    private fun buildSharedNewsContext(): String {
        val payload = NewsSnapshotStore.latestPayload ?: return "news_shared_context=unavailable"

        return buildString {
            appendLine("news_shared_context_available=true")
            appendLine("news_shared_type=${payload.type}")
            appendLine("news_shared_last_updated=${payload.lastUpdatedIso}")
            appendLine("news_shared_item_count=${payload.items.size}")
            payload.items.take(3).forEach { item ->
                appendLine("news_shared_item=${item.timeLabel} ${item.countryCode} ${item.category} ${item.title}")
            }
        }
    }

    private fun buildSharedMacroContext(): String {
        val allEvents = _allMacroEvents.value
        val streamEvents = _macroStreamEvents.value

        if (allEvents.isEmpty() && streamEvents.isEmpty()) {
            return "macro_shared_context=unavailable"
        }

        return buildString {
            appendLine("macro_shared_context_available=true")
            appendLine("macro_shared_total_events=${allEvents.size}")
            appendLine("macro_shared_stream_events=${streamEvents.size}")
            appendLine("macro_shared_upcoming_count=${allEvents.count { it.status == MacroEventStatus.UPCOMING }}")
            appendLine("macro_shared_confirmed_count=${allEvents.count { it.status == MacroEventStatus.CONFIRMED }}")
            streamEvents.take(4).forEach { event ->
                appendLine(
                    "macro_shared_event=${event.displayTitle()} currency=${event.currency} priority=${event.priority} status=${event.status}"
                )
            }
        }
    }

    private fun buildSharedMarketStateContext(): String {
        val marketState = _marketState.value ?: return "market_state_shared_context=unavailable"
        val lastClose = marketState.chartData.lastOrNull()?.close

        return buildString {
            appendLine("market_state_shared_context_available=true")
            appendLine("market_state_shared_symbol=${marketState.symbol}")
            appendLine("market_state_shared_bias=${marketState.technicalBias}")
            appendLine("market_state_shared_confidence=${marketState.confidence}")
            appendLine("market_state_shared_safety_blocked=${marketState.safetyBlocked}")
            appendLine("market_state_shared_chart_points=${marketState.chartData.size}")
            if (lastClose != null) {
                appendLine("market_state_shared_last_close=${chatFmt(lastClose)}")
            }
        }
    }

    private fun buildSharedWatchlistContext(): String {
        val items = _watchlistItems.value
        if (items.isEmpty()) return "watchlist_shared_context=unavailable"

        val visibleItems = items.take(4)
        return buildString {
            appendLine("watchlist_shared_context_available=true")
            appendLine("watchlist_shared_total=${items.size}")
            appendLine("watchlist_shared_analyzing=${_isWatchlistAnalyzing.value}")
            appendLine("watchlist_shared_filter=${_watchlistCategoryFilter.value ?: "ALL"}")
            appendLine("watchlist_shared_sort=${_watchlistSortMode.value}")
            visibleItems.forEach { item ->
                appendLine(
                    "watchlist_shared_item=${item.assetName} status=${item.status} confidence=${item.confidence} move_probability=${item.moveProbability} volatility=${item.volatilityScore} category=${item.category} news_risk=${item.newsRisk} trigger=${item.triggerEvent} time_to_event=${item.timeToEvent}"
                )
            }
        }
    }

    private fun buildSharedDashboardContext(): String {
        val selected = _selectedPair.value
        val snapshot = PaperTradingSnapshotStore.snapshot
        val status = _commandCenterStatus.value

        return buildString {
            appendLine("dashboard_shared_context_available=true")
            appendLine("dashboard_shared_target=${_dashboardTabTarget.value}")
            appendLine("dashboard_shared_selected_pair=${selected.symbol}")
            appendLine("dashboard_shared_selected_pair_price=${chatFmt(selected.price)}")
            appendLine("dashboard_shared_selected_pair_change=${chatFmt(selected.changePercent)}")
            appendLine("dashboard_shared_command_status_loading=${status.isLoading}")
            appendLine("dashboard_shared_command_status_connected=${status.isConnected ?: "unknown"}")
            appendLine("dashboard_shared_command_status_message=${status.lastMessage}")
            appendLine("dashboard_shared_account_balance=${chatFmt(snapshot.balance)}")
            appendLine("dashboard_shared_account_equity=${chatFmt(snapshot.equity)}")
            appendLine("dashboard_shared_account_floating_pnl=${chatFmt(snapshot.floatingPnl)}")
            appendLine("dashboard_shared_account_active_trades=${snapshot.activeTrades}")
            appendLine("dashboard_shared_account_active_orders=${snapshot.activeOrders}")
            appendLine("dashboard_shared_account_current_trade=${snapshot.currentTradeSymbol ?: "none"}")
        }
    }

    private fun buildSharedTradeContext(): String {
        val snapshot = PaperTradingSnapshotStore.snapshot
        if (!snapshot.isConnected && snapshot.balance == 0.0 && snapshot.equity == 0.0 && snapshot.activeTrades == 0 && snapshot.activeOrders == 0) {
            return "trade_shared_context=unavailable"
        }

        return buildString {
            appendLine("trade_shared_context_available=true")
            appendLine("trade_shared_connected=${snapshot.isConnected}")
            appendLine("trade_shared_balance=${chatFmt(snapshot.balance)}")
            appendLine("trade_shared_equity=${chatFmt(snapshot.equity)}")
            appendLine("trade_shared_floating_pnl=${chatFmt(snapshot.floatingPnl)}")
            appendLine("trade_shared_realized_pnl=${chatFmt(snapshot.realizedPnl)}")
            appendLine("trade_shared_open_risk=${chatFmt(snapshot.openRisk)}")
            appendLine("trade_shared_open_risk_pct=${chatFmt(snapshot.openRiskPct)}")
            appendLine("trade_shared_active_trades=${snapshot.activeTrades}")
            appendLine("trade_shared_active_orders=${snapshot.activeOrders}")
            appendLine("trade_shared_current_trade=${snapshot.currentTradeSymbol ?: "none"}")
        }
    }

    private fun buildSharedQuotesContext(): String {
        val livePairs = mergedMarketPairs()
            .filter { it.price.isFinite() && it.price > 0.0 }
            .take(8)

        if (livePairs.isEmpty()) return "quotes_shared_context=unavailable"

        return buildString {
            appendLine("quotes_shared_context_available=true")
            appendLine("quotes_shared_count=${livePairs.size}")
            livePairs.forEach { pair ->
                appendLine("quotes_shared_pair=${pair.symbol} price=${chatFmt(pair.price)} change_pct=${chatFmt(pair.changePercent)} category=${pair.category}")
            }
        }
    }

    private fun buildSharedMarketWatchContext(): String {
        val candidates = _preMoveCandidates.value.take(4)
        if (candidates.isEmpty()) return "market_watch_shared_context=unavailable"

        return buildString {
            appendLine("market_watch_shared_context_available=true")
            appendLine("market_watch_shared_count=${candidates.size}")
            candidates.forEach { candidate ->
                appendLine(
                    "market_watch_shared_candidate=${candidate.symbol} score=${candidate.preMoveScore} compression=${candidate.compressionScore} ignition=${candidate.ignitionScore} regime=${candidate.regime} state=${candidate.state} risk_gate=${candidate.riskGate}"
                )
            }
        }
    }

    private fun buildSharedPageContextSummary(): String {
        return buildString {
            appendLine(buildSharedCalendarContext())
            appendLine(buildSharedNewsContext())
            appendLine(buildSharedMacroContext())
            appendLine(buildSharedMarketStateContext())
            appendLine(buildSharedWatchlistContext())
            appendLine(buildSharedDashboardContext())
            appendLine(buildSharedTradeContext())
            appendLine(buildSharedQuotesContext())
            appendLine(buildSharedMarketWatchContext())
        }
    }

    private fun buildFocusedPageContext(page: AppView?): String {
        return when (page) {
            null -> buildGlobalFocusedContext()
            AppView.CALENDAR -> buildCalendarFocusedContext()
            AppView.NEWS, AppView.ANALYSIS_OPINION -> buildNewsFocusedContext()
            AppView.MACRO_STREAM -> buildMacroFocusedContext()
            AppView.MARKET_STATUS -> buildMarketStatusFocusedContext()
            AppView.WATCHLIST -> buildWatchlistFocusedContext()
            AppView.DASHBOARD -> buildDashboardFocusedContext()
            AppView.TRADE -> buildTradeFocusedContext()
            AppView.QUOTES -> buildQuotesFocusedContext()
            AppView.MARKET_WATCH -> buildMarketWatchFocusedContext()
            AppView.MARKETS -> buildMarketsFocusedContext()
            AppView.STREAM, AppView.INTELLIGENCE_STREAM, AppView.SENTIMENT -> buildStreamFocusedContext()
            AppView.ALERTS, AppView.CREATE_ALERT, AppView.NOTIFICATIONS, AppView.PUSH_SETTINGS, AppView.HOME_ALERTS, AppView.MY_ALERTS -> buildNotificationsFocusedContext()
            AppView.LIQUIDITY_HUB -> buildLiquidityFocusedContext()
            AppView.ANALYSIS_RESULTS -> buildAnalysisResultsFocusedContext()
            AppView.TRADE_DASHBOARD -> buildTradeDashboardFocusedContext()
            AppView.POST_MOVE_AUDIT, AppView.TRADE_RECONSTRUCTION -> buildAuditFocusedContext(page)
            AppView.MARKET_VIEW -> buildMarketsFocusedContext()
            AppView.BACKTEST, AppView.MULTI_TIMEFRAME, AppView.FULL_CHART, AppView.SIMULATION, AppView.MY_SIMULATION,
            AppView.DATA_HUB, AppView.DATA_VAULT, AppView.DIAGNOSTICS, AppView.EDUCATION, AppView.PROFILE,
            AppView.SETTINGS, AppView.SIDEBAR_PAGE, AppView.CHAT, AppView.AI_TERMINAL, AppView.PAPER_TRADING,
            AppView.TRADING_ASSISTANT, AppView.PORTFOLIO_MANAGER -> buildOperationalFocusedContext(page)
            else -> buildGenericFocusedContext(page)
        }
    }

    private fun buildGlobalFocusedContext(): String {
        return buildString {
            appendLine("focus_mode=GLOBAL")
            appendLine("focus_hint=Use the shared context summaries as the primary lens. No page-specific focus is currently selected.")
            appendLine("focus_available_pages=${AppView.values().joinToString(", ") { it.toAiContextLabel() }}")
        }
    }

    fun dedicatedChatContextPages(): List<AppView> {
        return listOf(
            AppView.CALENDAR,
            AppView.NEWS,
            AppView.ANALYSIS_OPINION,
            AppView.MACRO_STREAM,
            AppView.MARKET_STATUS,
            AppView.WATCHLIST,
            AppView.DASHBOARD,
            AppView.TRADE,
            AppView.QUOTES,
            AppView.MARKET_WATCH,
            AppView.MARKETS,
            AppView.STREAM,
            AppView.INTELLIGENCE_STREAM,
            AppView.SENTIMENT,
            AppView.ALERTS,
            AppView.CREATE_ALERT,
            AppView.NOTIFICATIONS,
            AppView.PUSH_SETTINGS,
            AppView.HOME_ALERTS,
            AppView.MY_ALERTS,
            AppView.LIQUIDITY_HUB,
            AppView.ANALYSIS_RESULTS,
            AppView.TRADE_DASHBOARD,
            AppView.POST_MOVE_AUDIT,
            AppView.TRADE_RECONSTRUCTION,
            AppView.MARKET_VIEW,
            AppView.BACKTEST,
            AppView.MULTI_TIMEFRAME,
            AppView.FULL_CHART,
            AppView.SIMULATION,
            AppView.MY_SIMULATION,
            AppView.DATA_HUB,
            AppView.DATA_VAULT,
            AppView.DIAGNOSTICS,
            AppView.EDUCATION,
            AppView.PROFILE,
            AppView.SETTINGS,
            AppView.SIDEBAR_PAGE,
            AppView.CHAT,
            AppView.AI_TERMINAL,
            AppView.PAPER_TRADING,
            AppView.TRADING_ASSISTANT,
            AppView.PORTFOLIO_MANAGER
        )
    }

    private fun buildCalendarFocusedContext(): String {
        val display = CalendarSnapshotStore.latestDisplayPayload
        val ai = CalendarSnapshotStore.latestAiPayload

        if (display == null && ai == null) {
            return "No calendar snapshot is currently available."
        }

        return buildString {
            appendLine("calendar_context_available=true")
            display?.let { payload ->
                appendLine("calendar_display_source=${payload.sourceLabel}")
                appendLine("calendar_display_range=${payload.rangeStartIso} -> ${payload.rangeEndIso}")
                appendLine("calendar_display_selected_date=${payload.selectedDateIso}")
                appendLine("calendar_display_header=${payload.headerDateLabel}")
                appendLine("calendar_display_last_updated=${payload.lastUpdatedIso}")
                appendLine("calendar_display_event_count=${payload.events.size}")
                
                val sortedEvents = payload.events.sortedWith(compareByDescending<com.trading.app.models.EconomicCalendarDisplayEvent> {
                    when (it.importance.lowercase(Locale.US)) {
                        "high" -> 3
                        "medium" -> 2
                        "low" -> 1
                        else -> 0
                    }
                }.thenBy { it.releaseTimeLabel })
                
                sortedEvents.take(25).forEach { event ->
                    appendLine(
                        "calendar_display_event=${event.releaseTimeLabel} ${event.currencyCode} ${event.title} actual=${event.actual} forecast=${event.forecast} previous=${event.previous} importance=${event.importance} impact_direction=${event.impactDirection} all_day=${event.isAllDay} speech_or_report=${event.isSpeechOrReport}"
                    )
                }
            }
            ai?.let { payload ->
                appendLine("calendar_ai_source=${payload.source}")
                appendLine("calendar_ai_generated_at=${payload.generatedAtIso}")
                appendLine("calendar_ai_selected_date=${payload.selectedDateIso}")
                appendLine("calendar_ai_range=${payload.rangeStartIso} -> ${payload.rangeEndIso}")
                appendLine("calendar_ai_event_count=${payload.events.size}")
                
                val sortedEvents = payload.events.sortedWith(compareByDescending<com.trading.app.models.EconomicCalendarAiEvent> {
                    when (it.importance.lowercase(Locale.US)) {
                        "high" -> 3
                        "medium" -> 2
                        "low" -> 1
                        else -> 0
                    }
                }.thenBy { it.isoDateTime })
                
                sortedEvents.take(25).forEach { event ->
                    appendLine(
                        "calendar_ai_event=${event.isoDateTime} ${event.currencyCode} ${event.title} actual=${event.actual} forecast=${event.forecast} previous=${event.previous} importance=${event.importance} impact_direction=${event.impactDirection} processed=${event.processed}"
                    )
                }
            }
        }
    }

    private fun buildNewsFocusedContext(): String {
        val payload = NewsSnapshotStore.latestPayload
            ?: return "No news snapshot is currently available."

        return buildString {
            appendLine("news_context_available=true")
            appendLine("news_context_type=${payload.type}")
            appendLine("news_context_last_updated=${payload.lastUpdatedIso}")
            appendLine("news_context_item_count=${payload.items.size}")
            payload.items.take(8).forEach { item ->
                appendLine("news_item=${item.timeLabel} ${item.countryCode} ${item.category} ${item.title}")
            }
        }
    }

    private fun buildMacroFocusedContext(): String {
        val allEvents = _allMacroEvents.value
        val streamEvents = _macroStreamEvents.value

        if (allEvents.isEmpty() && streamEvents.isEmpty()) {
            return "No macro stream snapshot is currently available."
        }

        return buildString {
            appendLine("macro_context_available=true")
            appendLine("macro_context_total_events=${allEvents.size}")
            appendLine("macro_context_stream_events=${streamEvents.size}")
            streamEvents.take(8).forEach { event ->
                appendLine(
                    "macro_event=${event.displayTitle()} currency=${event.currency} priority=${event.priority} status=${event.status} source=${event.source}"
                )
            }
        }
    }

    private fun buildMarketStatusFocusedContext(): String {
        val marketState = _marketState.value
        if (marketState == null) {
            return buildSharedMarketStateContext()
        }

        return buildString {
            appendLine("market_status_context_available=true")
            appendLine("market_status_symbol=${marketState.symbol}")
            appendLine("market_status_bias=${marketState.technicalBias}")
            appendLine("market_status_confidence=${marketState.confidence}")
            appendLine("market_status_safety_blocked=${marketState.safetyBlocked}")
            appendLine("market_status_chart_points=${marketState.chartData.size}")
            marketState.chartData.takeLast(3).forEach { point ->
                appendLine("market_status_close=${chatFmt(point.close)}")
            }
        }
    }

    private fun buildWatchlistFocusedContext(): String {
        val visibleItems = _watchlistItems.value
            .sortedWith(compareByDescending<WatchlistItem> { it.confidence }.thenByDescending { it.moveProbability })
            .take(8)

        if (visibleItems.isEmpty()) return "watchlist_context=unavailable"

        return buildString {
            appendLine("watchlist_context_available=true")
            appendLine("watchlist_context_total=${_watchlistItems.value.size}")
            appendLine("watchlist_context_sort=${_watchlistSortMode.value}")
            appendLine("watchlist_context_filter=${_watchlistCategoryFilter.value ?: "ALL"}")
            appendLine("watchlist_context_compact=${_watchlistCompactMode.value}")
            visibleItems.forEach { item ->
                appendLine(
                    "watchlist_item=${item.assetName} status=${item.status} confidence=${item.confidence} move_probability=${item.moveProbability} volatility=${item.volatilityScore} premove=${item.preMoveSignal} trigger=${item.triggerEvent} time_to_event=${item.timeToEvent} rationale=${item.rationale}"
                )
            }
        }
    }

    private fun buildDashboardFocusedContext(): String {
        val selected = _selectedPair.value
        val snapshot = PaperTradingSnapshotStore.snapshot
        val status = _commandCenterStatus.value
        val livePairs = mergedMarketPairs().filter { it.price.isFinite() && it.price > 0.0 }.take(6)

        return buildString {
            appendLine("dashboard_context_available=true")
            appendLine("dashboard_selected_pair=${selected.symbol}")
            appendLine("dashboard_selected_pair_price=${chatFmt(selected.price)}")
            appendLine("dashboard_selected_pair_change_pct=${chatFmt(selected.changePercent)}")
            appendLine("dashboard_balance=${chatFmt(snapshot.balance)}")
            appendLine("dashboard_equity=${chatFmt(snapshot.equity)}")
            appendLine("dashboard_floating_pnl=${chatFmt(snapshot.floatingPnl)}")
            appendLine("dashboard_active_trades=${snapshot.activeTrades}")
            appendLine("dashboard_active_orders=${snapshot.activeOrders}")
            appendLine("dashboard_current_trade=${snapshot.currentTradeSymbol ?: "none"}")
            appendLine("dashboard_pipeline_loading=${status.isLoading}")
            appendLine("dashboard_pipeline_connected=${status.isConnected ?: "unknown"}")
            appendLine("dashboard_pipeline_last_message=${status.lastMessage}")
            livePairs.forEach { pair ->
                appendLine("dashboard_live_pair=${pair.symbol} price=${chatFmt(pair.price)} change_pct=${chatFmt(pair.changePercent)} category=${pair.category}")
            }
        }
    }

    private fun buildTradeFocusedContext(): String {
        val snapshot = PaperTradingSnapshotStore.snapshot
        return buildString {
            appendLine("trade_context_available=true")
            appendLine("trade_connected=${snapshot.isConnected}")
            appendLine("trade_balance=${chatFmt(snapshot.balance)}")
            appendLine("trade_equity=${chatFmt(snapshot.equity)}")
            appendLine("trade_floating_pnl=${chatFmt(snapshot.floatingPnl)}")
            appendLine("trade_realized_pnl=${chatFmt(snapshot.realizedPnl)}")
            appendLine("trade_open_risk=${chatFmt(snapshot.openRisk)}")
            appendLine("trade_open_risk_pct=${chatFmt(snapshot.openRiskPct)}")
            appendLine("trade_active_trades=${snapshot.activeTrades}")
            appendLine("trade_active_orders=${snapshot.activeOrders}")
            appendLine("trade_current_trade=${snapshot.currentTradeSymbol ?: "none"}")
            appendLine("trade_current_side=${snapshot.currentTradeSide ?: "none"}")
            appendLine("trade_current_entry=${chatFmt(snapshot.currentTradeEntryPrice)}")
            appendLine("trade_current_price=${chatFmt(snapshot.currentTradePrice)}")
            appendLine("trade_current_pnl=${chatFmt(snapshot.currentTradePnl)}")
        }
    }

    private fun buildQuotesFocusedContext(): String {
        val livePairs = mergedMarketPairs()
            .filter { it.price.isFinite() && it.price > 0.0 }
            .take(12)

        if (livePairs.isEmpty()) return "quotes_context=unavailable"

        return buildString {
            appendLine("quotes_context_available=true")
            appendLine("quotes_context_count=${livePairs.size}")
            livePairs.forEach { pair ->
                appendLine("quote=${pair.symbol} price=${chatFmt(pair.price)} change_pct=${chatFmt(pair.changePercent)} category=${pair.category}")
            }
        }
    }

    private fun buildMarketWatchFocusedContext(): String {
        val candidates = _preMoveCandidates.value
            .sortedByDescending { it.preMoveScore }
            .take(8)

        if (candidates.isEmpty()) return "market_watch_context=unavailable"

        return buildString {
            appendLine("market_watch_context_available=true")
            appendLine("market_watch_context_total=${_preMoveCandidates.value.size}")
            candidates.forEach { candidate ->
                appendLine(
                    "market_watch_candidate=${candidate.symbol} score=${candidate.preMoveScore} compression=${candidate.compressionScore} ignition=${candidate.ignitionScore} regime=${candidate.regime} state=${candidate.state} risk_gate=${candidate.riskGate}"
                )
            }
        }
    }

    private fun buildMarketsFocusedContext(): String {
        val selected = _selectedPair.value
        val marketState = _marketState.value
        val livePairs = mergedMarketPairs().filter { it.price.isFinite() && it.price > 0.0 }.take(10)

        return buildString {
            appendLine("markets_context_available=true")
            appendLine("markets_selected_pair=${selected.symbol}")
            appendLine("markets_selected_pair_price=${chatFmt(selected.price)}")
            appendLine("markets_selected_pair_change_pct=${chatFmt(selected.changePercent)}")
            appendLine("markets_selected_pair_category=${selected.category}")
            appendLine("markets_state_symbol=${marketState?.symbol ?: "none"}")
            appendLine("markets_state_bias=${marketState?.technicalBias ?: "unknown"}")
            appendLine("markets_state_confidence=${marketState?.confidence ?: 0}")
            appendLine("markets_state_points=${marketState?.chartData?.size ?: 0}")
            livePairs.forEach { pair ->
                appendLine("markets_pair=${pair.symbol} price=${chatFmt(pair.price)} change_pct=${chatFmt(pair.changePercent)} category=${pair.category}")
            }
        }
    }

    private fun buildStreamFocusedContext(): String {
        val news = NewsSnapshotStore.latestPayload
        val macroEvents = _macroStreamEvents.value.take(8)

        return buildString {
            appendLine("stream_context_available=true")
            appendLine("stream_news_type=${news?.type ?: "none"}")
            appendLine("stream_news_updated=${news?.lastUpdatedIso ?: "none"}")
            appendLine("stream_news_count=${news?.items?.size ?: 0}")
            news?.items?.take(5)?.forEach { item ->
                appendLine("stream_news_item=${item.timeLabel} ${item.countryCode} ${item.category} ${item.title}")
            }
            appendLine("stream_macro_count=${_macroStreamEvents.value.size}")
            macroEvents.forEach { event ->
                appendLine("stream_macro_event=${event.displayTitle()} currency=${event.currency} priority=${event.priority} status=${event.status}")
            }
            appendLine("stream_notifications_unread=${_unreadCount.value}")
        }
    }

    private fun buildNotificationsFocusedContext(): String {
        val notifications = _inAppNotifications.value.take(8)
        return buildString {
            appendLine("notifications_context_available=true")
            appendLine("notifications_unread=${_unreadCount.value}")
            appendLine("notifications_alert=${_alertNotificationCount.value}")
            appendLine("notifications_total=${_inAppNotifications.value.size}")
            notifications.forEach { notification ->
                appendLine("notification=${notification.type} severity=${notification.severity} seen=${notification.seen} message=${notification.msg}")
            }
        }
    }

    private fun buildLiquidityFocusedContext(): String {
        val selected = _selectedPair.value
        val marketState = _marketState.value
        val topWatchlist = _watchlistItems.value.maxByOrNull { it.confidence }

        return buildString {
            appendLine("liquidity_context_available=true")
            appendLine("liquidity_selected_pair=${selected.symbol}")
            appendLine("liquidity_selected_pair_price=${chatFmt(selected.price)}")
            appendLine("liquidity_selected_pair_change_pct=${chatFmt(selected.changePercent)}")
            appendLine("liquidity_market_bias=${marketState?.technicalBias ?: "unknown"}")
            appendLine("liquidity_market_confidence=${marketState?.confidence ?: 0}")
            topWatchlist?.let { item ->
                appendLine("liquidity_top_watchlist=${item.assetName} status=${item.status} confidence=${item.confidence} move_probability=${item.moveProbability} trigger=${item.triggerEvent}")
            }
        }
    }

    private fun buildAnalysisResultsFocusedContext(): String {
        val selected = _selectedPair.value
        val marketState = _marketState.value
        val topWatchlist = _watchlistItems.value.maxByOrNull { it.moveProbability }

        return buildString {
            appendLine("analysis_context_available=true")
            appendLine("analysis_selected_pair=${selected.symbol}")
            appendLine("analysis_selected_pair_price=${chatFmt(selected.price)}")
            appendLine("analysis_selected_pair_change_pct=${chatFmt(selected.changePercent)}")
            appendLine("analysis_market_bias=${marketState?.technicalBias ?: "unknown"}")
            appendLine("analysis_market_confidence=${marketState?.confidence ?: 0}")
            topWatchlist?.let { item ->
                appendLine("analysis_top_watchlist=${item.assetName} status=${item.status} confidence=${item.confidence} move_probability=${item.moveProbability}")
            }
        }
    }

    private fun buildTradeDashboardFocusedContext(): String {
        val snapshot = PaperTradingSnapshotStore.snapshot
        val status = _commandCenterStatus.value

        return buildString {
            appendLine("trade_dashboard_context_available=true")
            appendLine("trade_dashboard_connected=${snapshot.isConnected}")
            appendLine("trade_dashboard_balance=${chatFmt(snapshot.balance)}")
            appendLine("trade_dashboard_equity=${chatFmt(snapshot.equity)}")
            appendLine("trade_dashboard_floating_pnl=${chatFmt(snapshot.floatingPnl)}")
            appendLine("trade_dashboard_open_risk=${chatFmt(snapshot.openRisk)}")
            appendLine("trade_dashboard_active_trades=${snapshot.activeTrades}")
            appendLine("trade_dashboard_active_orders=${snapshot.activeOrders}")
            appendLine("trade_dashboard_pipeline_loading=${status.isLoading}")
            appendLine("trade_dashboard_pipeline_message=${status.lastMessage}")
        }
    }

    private fun buildAuditFocusedContext(page: AppView): String {
        val records = _auditRecords.value.take(8)
        return buildString {
            appendLine("audit_context_available=true")
            appendLine("audit_context_page=${page.name}")
            appendLine("audit_context_count=${_auditRecords.value.size}")
            records.forEach { record ->
                appendLine("audit_record=${record.headline} impact=${record.impact} confidence=${record.confidence} assets=${record.assets} status=${record.status} audited=${record.audited}")
            }
        }
    }

    private fun buildOperationalFocusedContext(page: AppView): String {
        val selected = _selectedPair.value
        val status = _commandCenterStatus.value
        val marketState = _marketState.value
        return buildString {
            appendLine("operational_context_available=true")
            appendLine("operational_page=${page.name}")
            appendLine("operational_focus=${page.toAiContextLabel()}")
            appendLine("operational_selected_pair=${selected.symbol}")
            appendLine("operational_selected_pair_price=${chatFmt(selected.price)}")
            appendLine("operational_pipeline_loading=${status.isLoading}")
            appendLine("operational_pipeline_connected=${status.isConnected ?: "unknown"}")
            appendLine("operational_pipeline_message=${status.lastMessage}")
            appendLine("operational_market_bias=${marketState?.technicalBias ?: "unknown"}")
            appendLine("operational_market_confidence=${marketState?.confidence ?: 0}")
            appendLine("operational_unread_notifications=${_unreadCount.value}")
            appendLine("operational_watchlist_count=${_watchlistItems.value.size}")
        }
    }

    private fun buildGenericFocusedContext(page: AppView): String {
        val selected = _selectedPair.value
        return buildString {
            appendLine("generic_context_available=true")
            appendLine("generic_page=${page.name}")
            appendLine("generic_focus_label=${page.toAiContextLabel()}")
            appendLine("generic_selected_pair=${selected.symbol}")
            appendLine("generic_selected_pair_price=${chatFmt(selected.price)}")
            appendLine("generic_help=Use the shared context summaries together with this page lens.")
        }
    }

    private fun loadAscChatSessions(): List<AscChatSession> {
        val raw = chatPrefs.getString(CHAT_SESSIONS_KEY, null)
        if (!raw.isNullOrBlank()) {
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val id = item.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
                        val title = item.optString("title").takeIf { it.isNotBlank() } ?: "Chat"
                        val createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                        val messagesArray = item.optJSONArray("messages") ?: JSONArray()
                        val messages = buildList {
                            for (messageIndex in 0 until messagesArray.length()) {
                                val messageItem = messagesArray.optJSONObject(messageIndex) ?: continue
                                val role = messageItem.optString("role").takeIf { it.isNotBlank() } ?: continue
                                val content = messageItem.optString("content").takeIf { it.isNotBlank() } ?: continue
                                add(
                                    ChatMessage(
                                        id = messageItem.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                                        role = role,
                                        content = content,
                                        timestamp = messageItem.optLong("timestamp", System.currentTimeMillis())
                                    )
                                )
                            }
                        }.takeLast(80)
                        add(
                            AscChatSession(
                                id = id,
                                title = title,
                                createdAtMillis = createdAtMillis,
                                messages = messages
                            )
                        )
                    }
                }.ifEmpty { listOf(createEmptyAscChatSession()) }
            }.getOrElse {
                loadLegacyAscChatSessions()
            }
        }

        return loadLegacyAscChatSessions()
    }

    private fun loadLegacyAscChatSessions(): List<AscChatSession> {
        val legacyMessages = loadLegacyAscChatMessages()
        return if (legacyMessages.isNotEmpty()) {
            listOf(
                AscChatSession(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "Old Chat",
                    createdAtMillis = System.currentTimeMillis(),
                    messages = legacyMessages
                )
            )
        } else {
            listOf(createEmptyAscChatSession())
        }
    }

    private fun loadLegacyAscChatMessages(): List<ChatMessage> {
        val raw = chatPrefs.getString("messages", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val role = item.optString("role").takeIf { it.isNotBlank() } ?: continue
                    val content = item.optString("content").takeIf { it.isNotBlank() } ?: continue
                    add(
                        ChatMessage(
                            id = item.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                            role = role,
                            content = content,
                            timestamp = item.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }.takeLast(80)
        }.getOrDefault(emptyList())
    }

    private fun loadAscChatSessionId(sessions: List<AscChatSession>): String {
        val saved = chatPrefs.getString(CHAT_ACTIVE_SESSION_ID_KEY, null)
        return when {
            saved != null && sessions.any { it.id == saved } -> saved
            sessions.isNotEmpty() -> sessions.first().id
            else -> createEmptyAscChatSession().id
        }
    }

    private fun loadActiveAscChatMessages(
        sessions: List<AscChatSession>,
        sessionId: String
    ): List<ChatMessage> {
        return sessions.firstOrNull { it.id == sessionId }?.messages
            ?: sessions.firstOrNull()?.messages
            ?: emptyList()
    }

    private fun createEmptyAscChatSession(): AscChatSession {
        return AscChatSession(
            id = java.util.UUID.randomUUID().toString(),
            title = "New Chat",
            createdAtMillis = System.currentTimeMillis(),
            messages = emptyList()
        )
    }

    private fun buildAscChatSessionTitleFromPrompt(prompt: String): String {
        val words = prompt
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.isEmpty()) return "New Chat"

        val titleWords = when {
            words.size >= 8 -> words.take(8)
            words.size >= 3 -> words.take(words.size.coerceAtMost(8))
            words.size == 2 -> words + "Chat"
            else -> listOf(words.first(), "Chat", "Session")
        }

        return titleWords.joinToString(" ")
            .replaceFirstChar { ch -> ch.titlecase(Locale.US) }
    }

    private fun buildChatAppContext(deployments: LatestDeploymentsResponse?): String {
        val snapshot = PaperTradingSnapshotStore.snapshot
        val selected = _selectedPair.value
        val status = _commandCenterStatus.value
        val priceStreamEntries = PriceStreamManager.priceUpdates.value.entries
            .asSequence()
            .filter { it.value.isFinite() && it.value > 0.0 }
            .sortedBy { it.key }
            .take(24)
            .toList()
        val livePairs = mergedMarketPairs()
            .filter { it.price.isFinite() && it.price > 0.0 }
            .take(12)
        val visiblePricePairs = livePairs.take(8)
        val currentTrade = snapshot.currentTradeSymbol?.let { symbol ->
            "${snapshot.currentTradeSide ?: "UNKNOWN"} $symbol volume=${chatFmt(snapshot.currentTradeVolume)} entry=${chatFmt(snapshot.currentTradeEntryPrice)} price=${chatFmt(snapshot.currentTradePrice)} pnl=${chatFmt(snapshot.currentTradePnl)}"
        } ?: "none"

        return buildString {
            appendLine("current_view=${_currentView.value}")
            appendLine("shared_context_start")
            appendLine(buildSharedPageContextSummary())
            appendLine("shared_context_end")
            appendLine("shared_context_policy=These summaries are always available background context for every page selection.")
            appendLine("price_stream_snapshot_start")
            appendLine("price_stream_snapshot_available=${priceStreamEntries.isNotEmpty()}")
            appendLine("price_stream_snapshot_rule=For any current price question, search the price stream snapshot for a matching symbol before using a fallback.")
            priceStreamEntries.forEach { (symbol, price) ->
                appendLine("price_stream_price=$symbol price=${chatFmt(price)}")
            }
            appendLine("price_stream_snapshot_end")
            appendLine("visible_price_context_start")
            appendLine("visible_price_context_available=${visiblePricePairs.isNotEmpty()}")
            appendLine("visible_price_context_rule=For current price questions, check the visible price context first. Use any matching symbol shown here, not only the selected asset.")
            visiblePricePairs.forEach { pair ->
                appendLine("visible_price=${pair.symbol} price=${chatFmt(pair.price)} change_pct=${chatFmt(pair.changePercent)} category=${pair.category}")
            }
            appendLine("visible_price_context_end")
            val chatContextPage = AppView.values().firstOrNull { it.name == _ascChatContextPageId.value }
            if (chatContextPage == null) {
                appendLine("chat_context_focus=GLOBAL")
                appendLine("chat_context_focus_label=Global Context")
            } else {
                appendLine("chat_context_focus=${chatContextPage.name}")
                appendLine("chat_context_focus_label=${chatContextPage.toAiContextLabel()}")
            }
            appendLine("chat_context_policy=Use the selected page as the main lens when one is selected, but you may still reference other pages in the app when useful.")
            appendLine("chat_context_focus_hint=If the focused page has a dedicated snapshot, read it first and answer from it before falling back to the shared app state.")
            appendLine("chat_context_payload_start")
            appendLine(buildFocusedPageContext(chatContextPage))
            appendLine("chat_context_payload_end")
            appendLine("available_pages=${AppView.values().joinToString(", ") { it.toAiContextLabel() }}")
            appendLine("selected_asset=${selected.symbol}")
            appendLine("selected_asset_price=${chatFmt(selected.price)}")
            appendLine("selected_asset_change_pct=${chatFmt(selected.changePercent)}")
            appendLine("active_persona_id=${_ascChatPersonaId.value}")
            appendLine("active_persona_name=${ANALYST_MODELS.firstOrNull { it.id == _ascChatPersonaId.value }?.name ?: "unknown"}")
            appendLine("account_connected=${snapshot.isConnected}")
            appendLine("has_live_account_data=${snapshot.hasLiveAccountData}")
            appendLine("has_live_trade_data=${snapshot.hasLiveTradeData}")
            appendLine("active_live_trades=${snapshot.activeTrades}")
            appendLine("active_orders=${snapshot.activeOrders}")
            appendLine("current_live_trade=$currentTrade")
            appendLine("balance=${chatFmt(snapshot.balance)}")
            appendLine("equity=${chatFmt(snapshot.equity)}")
            appendLine("floating_pnl=${chatFmt(snapshot.floatingPnl)}")
            appendLine("realized_pnl=${chatFmt(snapshot.realizedPnl)}")
            appendLine("open_risk=${chatFmt(snapshot.openRisk)}")
            appendLine("open_risk_pct=${chatFmt(snapshot.openRiskPct)}")
            appendLine("pipeline_loading=${status.isLoading}")
            appendLine("pipeline_connected=${status.isConnected ?: "unknown"}")
            appendLine("pipeline_last_message=${status.lastMessage}")
            appendLine("terminal_armed=${_isArmed.value}")
            appendLine("terminal_algo=${_activeAlgo.value}")
            appendLine("drawer_open=${_isDrawerOpen.value}")
            appendLine("command_palette_open=${_isCommandPaletteOpen.value}")
            appendLine("global_header_visible=${_isGlobalHeaderVisible.value}")
            appendLine("dashboard_tab_target=${_dashboardTabTarget.value}")
            appendLine("risk_accepted=${_isRiskAccepted.value}")
            appendLine("market_state_symbol=${_marketState.value?.symbol ?: "none"}")
            appendLine("market_state_bias=${_marketState.value?.technicalBias ?: "unknown"}")
            appendLine("market_state_confidence=${_marketState.value?.confidence ?: 0}")
            appendLine("watchlist_items=${_watchlistItems.value.size}")
            _watchlistItems.value.take(8).forEach { item ->
                appendLine("watchlist=${item.assetName} status=${item.status} confidence=${item.confidence} move_probability=${item.moveProbability} category=${item.category}")
            }
            appendLine("watchlist_analyzing=${_isWatchlistAnalyzing.value}")
            appendLine("watchlist_filter=${_watchlistCategoryFilter.value ?: "ALL"}")
            appendLine("macro_events_total=${_allMacroEvents.value.size}")
            appendLine("macro_stream_events=${_macroStreamEvents.value.size}")
            _macroStreamEvents.value.take(5).forEach { event ->
                appendLine("macro_event=${event.title} priority=${event.priority} status=${event.status} currency=${event.currency}")
            }
            appendLine("notifications_unread=${_unreadCount.value}")
            appendLine("notifications_alert=${_alertNotificationCount.value}")
            appendLine("notifications_total=${_inAppNotifications.value.size}")
            _inAppNotifications.value.take(5).forEach { notification ->
                appendLine("notification=${notification.type} severity=${notification.severity} seen=${notification.seen} message=${notification.msg}")
            }
            appendLine("telemetry_session_landing=${_sessionLandingCount.value}")
            appendLine("telemetry_clicks_to_execution=${_clicksToExecutionCount.value}")
            appendLine("telemetry_ingestion_dropped=${_ingestionDroppedCount.value}")
            appendLine("audit_records=${_auditLog.value.size}")
            appendLine("user_override_count=${_userOverrideCount.value}")
            appendLine("execution_opt_in_requested=${_executionOptInRequested.value}")
            appendLine("pending_execution_target=${_pendingExecutionTarget.value ?: "none"}")
            appendLine("pattern_sensitivity=${chatFmt(_patternSensitivity.value.toDouble())}")
            appendLine("remote_force_override=${_forceRemoteOverride.value}")
            appendLine("remote_poll_interval_ms=${_remotePollIntervalMs.value}")
            appendLine("bond_data_series=${_bondData.value.size}")
            _bondData.value.values.take(4).forEach { bond ->
                appendLine("bond=${bond.name ?: bond.seriesId} value=${bond.value} date=${bond.date}")
            }
            appendLine("asc_deployments_loaded=${deployments?.final_decision?.size ?: 0}")
            appendLine("asc_deployments_last_updated=${deployments?.last_updated ?: "not loaded"}")
            appendLine("live_market_pairs=${livePairs.size}")
            livePairs.forEach { pair ->
                appendLine("market_pair=${pair.symbol} price=${chatFmt(pair.price)} change_pct=${chatFmt(pair.changePercent)} category=${pair.category}")
            }
        }
    }

    private fun chatFmt(value: Double?): String {
        val safe = value ?: return "unknown"
        return if (safe.isFinite()) String.format(Locale.US, "%.4f", safe) else "unknown"
    }

    private fun isDeepTerminalCommand(upper: String): Boolean {
        return upper == "ARM PIPELINE" ||
            upper.startsWith("SET ALGO ") ||
            Regex("\\b(BUY|SELL)\\s+[A-Z0-9/]{3,12}\\s+[\\d.]+").containsMatchIn(upper)
    }

    private fun syncTerminalEngineState() {
        _isArmed.value = TradingAssistantEngine.armed && !TradingAssistantEngine.safetyLockActive
        _activeAlgo.value = TradingAssistantEngine.executionAlgo
    }

    private fun buildTerminalAscSummary(deployments: LatestDeploymentsResponse?): String {
        val decisions = deployments?.final_decision.orEmpty()
        if (deployments == null) return "I don�t have the latest deployment data yet. Try Refresh AI or Run ASC AI."
        if (decisions.isEmpty()) {
            return "I loaded the latest deployment data, but there are no final decisions yet. success=${deployments.success}, count=${deployments.count}, last_updated=${deployments.last_updated ?: "unknown"}"
        }
        return buildString {
            appendLine("Here�s the latest ASC deployment summary:")
            appendLine(if (deployments.success) "It completed successfully." else "It did not complete successfully.")
            appendLine("Total decisions: ${deployments.count}.")
            appendLine("Last updated: ${deployments.last_updated ?: "unknown"}.")
            decisions.take(5).forEachIndexed { index, decision ->
                appendLine(
                    "${index + 1}. ${decision.asset_1 ?: "UNKNOWN"} is ${decision.journal_direction ?: "WAIT"} with ${decision.portfolio_decision_label ?: decision.journal_label ?: "NO_LABEL"}, " +
                        "bucket ${decision.portfolio_deployment_bucket ?: "unknown"}, risk ${decision.final_risk_pct ?: decision.recommended_risk_pct ?: 0.0}, entry ${decision.entry_window ?: "unknown"}."
                )
            }
        }
    }

    fun toggleArm() {
        val next = !_isArmed.value
        _isArmed.value = next
        TradingAssistantEngine.armed = next
        TradingAssistantEngine.safetyLockActive = !next
    }

    fun runAiPipelineNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = true,
                lastMessage = "Running pipeline..."
            )
            aiRepository.runAiPipeline()
                .onSuccess { resp ->
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = true,
                        lastMessage = if (resp.success) {
                            "Pipeline completed: ${resp.final_decision.size} decisions"
                        } else {
                            "Pipeline response: ${resp.message ?: "unknown"}"
                        },
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
                .onFailure { err ->
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = false,
                        lastMessage = "Pipeline failed: ${err.message ?: "unknown error"}",
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
        }
    }

    fun refreshAiDeploymentsNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = true,
                lastMessage = "Refreshing deployments..."
            )
            aiRepository.fetchLatestDeployments()
                .onSuccess { resp ->
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = true,
                        lastMessage = "Deployments updated: ${resp.count}",
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
                .onFailure { err ->
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = false,
                        lastMessage = "Refresh failed: ${err.message ?: "unknown error"}",
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
        }
    }

    fun checkAiHealthNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = true,
                lastMessage = "Checking backend..."
            )
            aiRepository.healthCheck()
                .onSuccess { health ->
                    val status = health["status"]?.toString() ?: "unknown"
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = status.equals("ok", ignoreCase = true),
                        lastMessage = "Backend health: $status",
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
                .onFailure { err ->
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = false,
                        lastMessage = "Health check failed: ${err.message ?: "unknown error"}",
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
        }
    }

    fun syncDataVaultNow() {
        viewModelScope.launch {
            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = true,
                lastMessage = "Checking ASC AI vault backend..."
            )

            val healthResult = aiRepository.healthCheck()
            if (healthResult.isFailure) {
                val err = healthResult.exceptionOrNull()
                _commandCenterStatus.value = _commandCenterStatus.value.copy(
                    isLoading = false,
                    isConnected = false,
                    lastMessage = "Vault backend offline: ${err?.message ?: "unknown error"}",
                    lastActionAtMillis = System.currentTimeMillis()
                )
                return@launch
            }

            val status = healthResult.getOrNull().orEmpty()["status"]?.toString() ?: "unknown"
            if (!status.equals("ok", ignoreCase = true)) {
                _commandCenterStatus.value = _commandCenterStatus.value.copy(
                    isLoading = false,
                    isConnected = false,
                    lastMessage = "Vault backend health: $status",
                    lastActionAtMillis = System.currentTimeMillis()
                )
                return@launch
            }

            _commandCenterStatus.value = _commandCenterStatus.value.copy(
                isLoading = true,
                isConnected = true,
                lastMessage = "ASC AI online. Refreshing vault packets..."
            )

            aiRepository.fetchLatestDeployments()
                .onSuccess { resp ->
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = true,
                        lastMessage = "Vault synced: ${resp.count} ASC AI packets",
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
                .onFailure { err ->
                    _commandCenterStatus.value = _commandCenterStatus.value.copy(
                        isLoading = false,
                        isConnected = false,
                        lastMessage = "Vault sync failed: ${err.message ?: "unknown error"}",
                        lastActionAtMillis = System.currentTimeMillis()
                    )
                }
        }
    }

    // Trigger a UI education modal requiring explicit opt-in before exposing execution controls
    fun requestExecutionOptIn(target: AppView) {
        _pendingExecutionTarget.value = target
        _executionOptInRequested.value = true
        _terminalLogs.value = listOf(
            ChatMessage(role = "model", content = "I asked for execution confirmation for ${target.name} at ${System.currentTimeMillis()}.")
        ) + _terminalLogs.value
    }

    // Called by UI when user confirms they understand the risks and explicitly opts into execution
    fun confirmExecutionOptIn() {
        val target = _pendingExecutionTarget.value
        _pendingExecutionTarget.value = null
        _executionOptInRequested.value = false
        _userOverrideCount.value = _userOverrideCount.value + 1
        _terminalLogs.value = listOf(
            ChatMessage(role = "model", content = "Execution access was confirmed at ${System.currentTimeMillis()}.")
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
            ChatMessage(role = "model", content = "Execution access request was cancelled at ${System.currentTimeMillis()}.")
        ) + _terminalLogs.value
    }

    // API: enable/disable promotion of MacroStream (non-destructive)
    fun setPromoteMacroStream(enabled: Boolean, navigateNow: Boolean = false) {
        _promoteMacroStream.value = enabled
        try {
            val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("promote_macro_stream", enabled).apply()
            if (enabled) {
                _sessionLandingCount.value = _sessionLandingCount.value + 1
                if (navigateNow) {
                    _currentView.value = AppView.MACRO_STREAM
                }
            } else {
                _userOverrideCount.value = _userOverrideCount.value + 1
            }
        } catch (_: Exception) {
            // ignore persistence failure � runtime flag still set
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

    override fun onCleared() {
        super.onCleared()
        derivService.disconnect()
        binanceWsManager.disconnect()
        tiingoFxManager?.disconnect()
    }
}

