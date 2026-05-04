package com.asc.markets.logic

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.api.ForexAnalysisEngine
import com.asc.markets.data.*
import com.asc.markets.ui.screens.dashboard.provideForexExplore
import com.asc.markets.data.ForexDataPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
import com.asc.markets.network.MetaApiClient
import com.asc.markets.network.TiingoFxRestClient
import com.asc.markets.network.TiingoFxWebSocketManager
import com.asc.markets.network.TiingoIexRestClient
import com.asc.markets.network.TiingoIexWebSocketManager

import com.trading.app.data.FredService
import com.trading.app.models.BondData
import org.json.JSONObject

data class CommandCenterStatus(
    val isLoading: Boolean = false,
    val isConnected: Boolean? = null,
    val lastMessage: String = "Idle",
    val lastActionAtMillis: Long = 0L
)

class ForexViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        private const val TIINGO_REST_REFRESH_MS = 60 * 60_000L
    }

    private val myApp = application as com.asc.markets.MyApp
    private val aiRepository = myApp.aiRepository
    val aiDeployments: StateFlow<LatestDeploymentsResponse?> = aiRepository.deployments

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
                        val existing = MarketDataStore.pairSnapshot(symbol)
                        val previousPrice = existing?.price ?: latestValue.toDouble()
                        val change = latestValue.toDouble() - previousPrice
                        val changePercent = if (previousPrice != 0.0) (change / previousPrice) * 100.0 else 0.0

                        _bondData.value = _bondData.value + (seriesId to BondData(seriesId, latestValue, date, name))
                        android.util.Log.i("FredService", "Routing FRED bond data to MarketDataStore $symbol $latestValue")
                        MarketDataStore.updatePair(
                            ForexPair(
                                symbol = symbol,
                                name = name,
                                price = latestValue.toDouble(),
                                change = change,
                                changePercent = changePercent,
                                category = MarketCategory.BONDS
                            )
                        )
                        MarketDataStore.replaceHistory(
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

    private val _selectedPair = MutableStateFlow(
        MarketDataStore.pairSnapshot("BTC/USDT") ?: provideForexExplore().first()
    )
    val selectedPair = _selectedPair.asStateFlow()

    private val _cryptoPairs = MutableStateFlow(
        MarketDataStore.allPairs.value.filter { it.category == MarketCategory.CRYPTO }
    )
    val cryptoPairs = _cryptoPairs.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    private val _isArmed = MutableStateFlow(false)
    val isArmed = _isArmed.asStateFlow()
    private val _commandCenterStatus = MutableStateFlow(CommandCenterStatus())
    val commandCenterStatus = _commandCenterStatus.asStateFlow()

    // Feature flag: when true, Macro Intelligence Stream is promoted as a landing/highlight view
    private val _promoteMacroStream = MutableStateFlow(false)
    val promoteMacroStream = _promoteMacroStream.asStateFlow()

    // Watchlist data (AI-curated)
    private val _watchlistItems = MutableStateFlow<List<WatchlistItem>>(listOf(
        WatchlistItem(
            assetName = "EURUSD",
            status = "Volatility Compression",
            confidence = 85,
            newsRisk = "High (CPI in 1h)",
            moveProbability = 76,
            priority = 1,
            preMoveSignal = "Accumulation",
            volatilityScore = 45,
            triggerEvent = "US CPI",
            timeToEvent = "42 mins",
            price = 1.0850,
            changePercent = 0.12,
            category = MarketCategory.FOREX,
            rationale = "AI detects tight range compression on H1 with accumulation signature. US CPI in 42 mins expected to catalyze directional expansion.",
            isNew = false
        ),
        WatchlistItem(
            assetName = "BTCUSD",
            status = "Liquidity Build",
            confidence = 72,
            newsRisk = "Low",
            moveProbability = 68,
            priority = 2,
            preMoveSignal = "Compression",
            volatilityScore = 84,
            price = 64230.50,
            changePercent = 2.45,
            category = MarketCategory.CRYPTO,
            rationale = "Large resting liquidity clusters identified above 64.8k and below 63.2k. Order-book depth expanding. Breakout probability rising as volume profile compresses.",
            isNew = true
        ),
        WatchlistItem(
            assetName = "NAS100",
            status = "Trend Expansion",
            confidence = 64,
            newsRisk = "Medium",
            moveProbability = 61,
            priority = 3,
            preMoveSignal = "Expansion",
            volatilityScore = 92,
            price = 17850.25,
            changePercent = -0.85,
            category = MarketCategory.INDICES,
            rationale = "Tech sector rotation driving volatility expansion. NAS100 breaking above prior session VWAP with momentum divergence flagged by the AI regime engine.",
            isNew = false
        ),
        WatchlistItem(
            assetName = "XAUUSD",
            status = "Trend Alignment",
            confidence = 78,
            newsRisk = "Low",
            moveProbability = 55,
            priority = 4,
            preMoveSignal = "Trend Alignment",
            volatilityScore = 67,
            price = 2345.80,
            changePercent = 0.45,
            category = MarketCategory.COMMODITIES,
            rationale = "Gold holding above 2330 structural support. DXY weakness and real-yield compression align with long-bias model. Low event risk today.",
            isNew = false
        )
    ))
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

    fun setWatchlistSort(mode: WatchlistSortMode) { _watchlistSortMode.value = mode }
    fun setWatchlistCategoryFilter(category: MarketCategory?) { _watchlistCategoryFilter.value = category }
    fun toggleWatchlistCompactMode() { _watchlistCompactMode.value = !_watchlistCompactMode.value }
    fun hideWatchlistItem(id: String) { _hiddenWatchlistIds.value += id }
    fun unhideAllWatchlistItems() { _hiddenWatchlistIds.value = emptySet() }
    fun refreshWatchlist() {
        _isWatchlistAnalyzing.value = true
        viewModelScope.launch {
            delay(1200)
            _watchlistItems.value = _watchlistItems.value.map { it.copy(isNew = false) }
            _lastWatchlistUpdate.value = System.currentTimeMillis()
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
            val livePair = MarketDataStore.pairSnapshot(item.assetName) ?: return@map item
            item.copy(
                price = livePair.price,
                changePercent = livePair.changePercent,
                category = livePair.category
            )
        }
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

    // Dashboard tab target (string name of DashboardTab) allows external callers to set which
    // top-tab the Dashboard should show when navigated to (e.g., Home button -> COMMAND_CENTER)
    private val _dashboardTabTarget = MutableStateFlow("COMMAND_CENTER")
    val dashboardTabTarget = _dashboardTabTarget.asStateFlow()

    private val _activeAlgo = MutableStateFlow("MARKET")
    val activeAlgo = _activeAlgo.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(role = "model", content = "[SYSTEM BOOT] Local Analytical Node initialized. Protocol L14 active.")
    ))
    val terminalLogs = _terminalLogs.asStateFlow()

    // Macro events source (can be updated by network or local ingestion)
    private val _allMacroEvents = MutableStateFlow<List<MacroEvent>>(emptyList())
    val allMacroEvents = _allMacroEvents.asStateFlow()

    // In-app notifications (persisted elsewhere later). Track seen/unseen state here.
    private val _inAppNotifications = MutableStateFlow<List<com.asc.markets.data.NotificationModel>>(
        listOf(
            com.asc.markets.data.NotificationModel(id = "n1", type = "SYSTEM", msg = "Analytical engine updated — new model deployed.", time = "2m ago", severity = "INFO", seen = false),
            com.asc.markets.data.NotificationModel(id = "n2", type = "TRADE", msg = "Order #4521 executed: 100 BTC @ 42,100.", time = "12m ago", severity = "WARNING", seen = false),
            com.asc.markets.data.NotificationModel(id = "n3", type = "SECURITY", msg = "Login from new device — location: Berlin.", time = "1h ago", severity = "CRITICAL", seen = false)
        )
    )
    val inAppNotifications = _inAppNotifications.asStateFlow()

    private val _unreadCount = MutableStateFlow( _inAppNotifications.value.count { !it.seen } )
    val unreadCount = _unreadCount.asStateFlow()
    private val _alertNotificationCount = MutableStateFlow(calculateAlertNotificationCount(_inAppNotifications.value))
    val alertNotificationCount = _alertNotificationCount.asStateFlow()

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

    private val newsService = NewsService()
    // Instantiate BinanceWebSocketManager with Redis configuration read from SharedPreferences so
    // the Redis host/port can be changed at runtime (useful for testing on a phone).
    private val binanceWsManager: com.asc.markets.network.BinanceWebSocketManager by lazy {
        val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
        // Default to laptop LAN IP for physical-device testing. Users can override in settings.
        val redisHost = prefs.getString("redis_host", "10.95.77.133") ?: "10.95.77.133"
        val redisPort = prefs.getInt("redis_port", 6379)
        val redisPassword = prefs.getString("redis_password", null)
        val redisUseSsl = prefs.getBoolean("redis_use_ssl", false)
        val streamName = prefs.getString("stream_name", "market.ticks.stream") ?: "market.ticks.stream"
        val fieldName = prefs.getString("field_name", "data") ?: "data"
        // 10.0.2.2 is emulator-only; use LAN default for real phone.
        val backendUrl = prefs.getString("backend_url", "http://10.95.77.133:8000")
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
        val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
        val bridgeUrl = prefs.getString("mt5_bridge_url", "192.168.1.100:62100") ?: "192.168.1.100:62100"
        MT5BridgeClient(bridgeUrl = bridgeUrl, scope = viewModelScope, brokerSuffix = "m")
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

    init {
        val tiingoFxAvailable = tiingoFxManager != null

        // Binance = USDT pairs only. Tiingo is the primary live source for Forex when configured.
        val usdtSymbols = FOREX_PAIRS
            .filter { it.symbol.endsWith("/USDT") }
            .map { it.symbol.replace("/", "") }
            .distinct()

        val metaManagedPairs = FOREX_PAIRS
            .filter { pair ->
                !pair.symbol.endsWith("/USDT") &&
                    !(tiingoFxAvailable && pair.category == MarketCategory.FOREX)
            }

        val metaSymbols = metaManagedPairs
            .map { it.symbol.replace("/", "") }
            .distinct()

        val tiingoForexSymbols = FOREX_PAIRS
            .filter { it.category == MarketCategory.FOREX }
            .map { it.symbol.replace("/", "") }
            .distinct()

        val tiingoStockSymbols = FOREX_PAIRS
            .filter { it.category == MarketCategory.STOCK }
            .map { it.symbol.replace("/", "") }
            .distinct()

        val mt5FallbackSymbols = metaManagedPairs
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

        if (usdtSymbols.isNotEmpty()) {
            binanceWsManager.connect(usdtSymbols)
        }

        refreshBondData()

        if (tiingoForexSymbols.isNotEmpty()) {
            if (tiingoFxManager != null) {
                tiingoFxManager?.connect(tiingoForexSymbols)
            } else {
                android.util.Log.w("TiingoFxWS", "Tiingo FX unavailable: TIINGO_API_KEY is not configured")
            }
        }

        if (tiingoStockSymbols.isNotEmpty()) {
            if (tiingoIexManager != null) {
                tiingoIexManager?.connect(tiingoStockSymbols)
            } else {
                android.util.Log.w("TiingoIexWS", "Tiingo IEX unavailable: TIINGO_API_KEY is not configured")
            }
        }

        if (tiingoForexSymbols.isNotEmpty()) {
            tiingoFxRestClient?.let { restClient ->
                viewModelScope.launch {
                    while (isActive) {
                        val pairs = restClient.fetchTopPairs(tiingoForexSymbols)
                        pairs.forEach { pair ->
                            android.util.Log.i("TiingoFxREST", "Routing Tiingo FX REST top to MarketDataStore ${pair.symbol} ${pair.price}")
                            MarketDataStore.updatePair(pair)
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
                        val pairs = restClient.fetchTopPairs(tiingoStockSymbols)
                        pairs.forEach { pair ->
                            android.util.Log.i("TiingoIexREST", "Routing Tiingo IEX REST top to MarketDataStore ${pair.symbol} ${pair.price}")
                            MarketDataStore.updatePair(pair)
                        }
                        delay(TIINGO_REST_REFRESH_MS)
                    }
                }
            }
        }

        // MetaAPI primary for non-USDT, non-Tiingo assets; MT5 polling remains the fallback.
        val metaToken = BuildConfig.META_API_TOKEN.takeIf { it.isNotBlank() }
        val metaAccountId = BuildConfig.META_API_ACCOUNT_ID.takeIf { it.isNotBlank() }
        var fallbackActive = false
        var mt5FallbackJob: Job? = null

        fun activateGlobalFallback(reason: String) {
            if (fallbackActive) return
            android.util.Log.w("ASC", "Activating MetaAPI fallback routing: $reason")
            fallbackActive = true

            if (mt5FallbackSymbols.isNotEmpty()) {
                mt5FallbackJob?.cancel()
                mt5FallbackJob = viewModelScope.launch {
                    while (fallbackActive && isActive) {
                        mt5FallbackSymbols.forEach { symbol ->
                            val tick = mt5BridgeClient.getTick(symbol) ?: return@forEach
                            val price = (tick.bid + tick.ask) / 2.0
                            PriceStreamManager.updatePrice(symbol, price)
                        }
                        delay(3000)
                    }
                }
            }
        }

        fun deactivateGlobalFallback() {
            if (!fallbackActive) return
            fallbackActive = false
            mt5FallbackJob?.cancel()
            mt5FallbackJob = null
        }

        if (metaSymbols.isNotEmpty() && metaToken != null && metaAccountId != null) {
            val metaApiClient = MetaApiClient(
                accountId = metaAccountId,
                token = metaToken,
                scope = viewModelScope,
                brokerSuffix = "m"
            )
            metaApiClient.connect(metaSymbols)

            viewModelScope.launch {
                metaApiClient.priceUpdates.collect { rawPair ->
                    if (!fallbackActive) {
                        MarketDataStore.updatePair(rawPair)
                    }
                }
            }
            viewModelScope.launch {
                metaApiClient.connectionState.collect { state ->
                    when (state) {
                        MetaApiClient.ConnectionState.ERROR_UNAUTHORIZED,
                        MetaApiClient.ConnectionState.ERROR_UNAVAILABLE,
                        MetaApiClient.ConnectionState.TIMEOUT,
                        MetaApiClient.ConnectionState.DISCONNECTED -> {
                            activateGlobalFallback("MetaAPI state: $state")
                        }
                        MetaApiClient.ConnectionState.CONNECTED -> {
                            if (fallbackActive) {
                                android.util.Log.i("ASC", "MetaAPI recovered; returning forex to MetaAPI and stopping fallbacks")
                                deactivateGlobalFallback()
                            }
                        }
                        else -> {}
                    }
                }
            }
        } else if (metaSymbols.isNotEmpty() || usdtSymbols.isNotEmpty()) {
            activateGlobalFallback("No MetaAPI credentials configured")
        }

        tiingoFxManager?.let { manager ->
            viewModelScope.launch {
                manager.connectionState.collect { state ->
                    android.util.Log.i("TiingoFxWS", "Tiingo FX connection state=$state")
                }
            }
            viewModelScope.launch {
                manager.priceUpdates.collect { pair ->
                    android.util.Log.i("TiingoFxWS", "Routing Tiingo FX update to MarketDataStore ${pair.symbol} ${pair.price}")
                    MarketDataStore.updatePair(pair)
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
                    android.util.Log.i("TiingoIexWS", "Routing Tiingo IEX update to MarketDataStore ${pair.symbol} ${pair.price}")
                    MarketDataStore.updatePair(pair)
                }
            }
        }

        // Throttle market updates to backend every 500ms
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                try {
                    val pairs = MarketDataStore.allPairs.value
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
                // Route Binance updates through PriceStreamManager for unified logic
                PriceStreamManager.updatePrice(pair.symbol, pair.price)
            }
        }

        viewModelScope.launch {
            MarketDataStore.allPairs.collect { pairs ->
                _cryptoPairs.value = pairs.filter { it.category == MarketCategory.CRYPTO }
                MarketDataStore.pairSnapshot(_selectedPair.value.symbol)?.let { latest ->
                    _selectedPair.value = latest
                }
                syncWatchlistWithLivePrices()
            }
        }

        viewModelScope.launch {
            binanceWsManager.accountStatus.collect { statusJson ->
                _terminalLogs.value = listOf(ChatMessage(role = "model", content = "[BINANCE] Account Status: $statusJson")) + _terminalLogs.value
            }
        }

        viewModelScope.launch {
            // Start continuous polling of RSS feeds and API news for Macro Stream
            launch(Dispatchers.IO) {
                while (isActive) {
                    try {
                        val articles = newsService.fetchAllNews()
                        if (articles.isNotEmpty()) {
                            val newsEvents = articles.map { article ->
                                MacroEvent(
                                    id = article.id,
                                    title = article.title,
                                    currency = article.intelligence?.asset_tags?.firstOrNull() ?: "GLOBAL",
                                    datetimeUtc = try {
                                        java.time.OffsetDateTime.parse(article.publishedAt).toInstant().toEpochMilli()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    },
                                    priority = when (val score = article.intelligence?.impact_score) {
                                        null -> ImpactPriority.LOW
                                        in 80.0..100.0 -> ImpactPriority.CRITICAL
                                        in 60.0..80.0 -> ImpactPriority.HIGH
                                        in 40.0..60.0 -> ImpactPriority.MEDIUM
                                        else -> ImpactPriority.LOW
                                    },
                                    status = MacroEventStatus.CONFIRMED,
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
        _selectedPair.value = MarketDataStore.pairSnapshot(pair.symbol) ?: pair
        _currentView.value = AppView.DASHBOARD
    }

    fun selectPairBySymbol(symbol: String) {
        val pair = MarketDataStore.pairSnapshot(symbol)
        pair?.let {
            selectPair(it)
        }
    }

    /**
     * Select a pair without changing the current view (no navigation).
     * Use this when updating selection from within a modal or settings screen.
     */
    fun selectPairBySymbolNoNavigate(symbol: String) {
        val pair = MarketDataStore.pairSnapshot(symbol)
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
            } else if (cmd.uppercase() == "ACCOUNT") {
                binanceWsManager.fetchAccountStatus()
                "[SYSTEM] Requesting Binance account status..."
            } else {
                ForexAnalysisEngine.getAnalystResponse(cmd, "Core_Engine")
            }
            _terminalLogs.value = listOf(ChatMessage(role = "model", content = response)) + _terminalLogs.value
        }
    }

    fun toggleArm() { _isArmed.value = !_isArmed.value }

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

    override fun onCleared() {
        super.onCleared()
        binanceWsManager.disconnect()
        tiingoFxManager?.disconnect()
    }
}
