package com.trading.app

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trading.app.components.*
import com.trading.app.models.*
import com.trading.app.data.CalendarSnapshotStore
import com.trading.app.data.NewsSnapshotStore
import com.trading.app.data.Mt5NewsStore
import com.trading.app.data.Mt5Service
import com.trading.app.data.Mt5ReverseBridge

import com.trading.app.data.ChartFeedType
import com.trading.app.data.chartFeedQuotes
import com.trading.app.data.chartFeedSymbolFor
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.trading.app.models.Drawing
import com.asc.markets.logic.PriceStreamManager
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.ui.components.AutoTradeChartBadge
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Helper to parse color string to Compose Color
private fun parseComposeColor(colorString: String?, defaultColor: Color = Color(0xFF131722)): Color {
    if (colorString.isNullOrBlank()) return defaultColor
    return try {
        if (colorString.startsWith("rgba", ignoreCase = true)) {
            val parts = colorString.substringAfter("(").substringBefore(")").split(",")
            val r = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val g = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val b = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
            val a = parts.getOrNull(3)?.trim()?.toFloatOrNull() ?: 1f
            Color(android.graphics.Color.argb((a * 255).toInt(), r, g, b))
        } else if (colorString.startsWith("rgb", ignoreCase = true)) {
            val parts = colorString.substringAfter("(").substringBefore(")").split(",")
            val r = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val g = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val b = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
            Color(android.graphics.Color.rgb(r, g, b))
        } else {
            Color(android.graphics.Color.parseColor(colorString))
        }
    } catch (e: Exception) {
        defaultColor
    }
}

private fun todayIsoDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

private fun parseIsoCalendar(value: String): Calendar {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return Calendar.getInstance().apply {
        time = formatter.parse(value) ?: Date()
    }
}

private fun formatIsoCalendar(calendar: Calendar): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

private fun formatHeaderDateLabel(isoDate: String): String {
    val calendar = parseIsoCalendar(isoDate)
    val month = SimpleDateFormat("MMM", Locale.US).format(calendar.time)
    return "${calendar.get(Calendar.DAY_OF_MONTH)} $month ${calendar.get(Calendar.YEAR)}"
}

private fun updateCalendarSelection(
    payload: EconomicCalendarDisplayPayload,
    selectedDateIso: String
): EconomicCalendarDisplayPayload {
    return payload.copy(
        selectedDateIso = selectedDateIso,
        headerDateLabel = formatHeaderDateLabel(selectedDateIso),
        dayChips = payload.dayChips.map { chip ->
            chip.copy(isSelected = chip.isoDate == selectedDateIso)
        }
    )
}

private fun shiftMonth(isoDate: String, monthDelta: Int): String {
    val calendar = parseIsoCalendar(isoDate)
    calendar.add(Calendar.MONTH, monthDelta)
    return formatIsoCalendar(calendar)
}

private fun recentPairQuoteKey(symbol: String, timeframe: String): String {
    return "${symbol}_$timeframe".uppercase(Locale.US)
}

private fun liveQuoteSymbolKeys(symbol: String): List<String> {
    val cleaned = symbol.trim().uppercase(Locale.US)
        .removeSuffix(".M")
        .removeSuffix(".PRO")
        .removeSuffix(".ECN")
        .removeSuffix(".S")
        .removeSuffix(".SPOT")
        .removeSuffix("+")
        .let { if (it.length > 1 && it.endsWith("M")) it.dropLast(1) else it }
    if (cleaned.isBlank()) return emptyList()
    return buildList {
        add(cleaned)
        if (cleaned.endsWith("USDT")) add(cleaned.dropLast(1))
        if (cleaned.endsWith("USD")) add("${cleaned}T")
    }.distinct()
}

private const val LIVE_QUOTE_MAX_AGE_MS = 60_000L

private fun isFreshLiveQuote(quote: SymbolQuote?, nowMillis: Long = System.currentTimeMillis()): Boolean {
    return quote != null && quote.lastPrice > 0f && quote.time > 0L && nowMillis - quote.time <= LIVE_QUOTE_MAX_AGE_MS
}

private fun persistNewsAiPayload(
    context: Context,
    sharedPrefs: android.content.SharedPreferences,
    payloadJson: String
) {
    try {
        sharedPrefs.edit().putString("news_payload", payloadJson).apply()
        context.openFileOutput("news_ai_payload.json", Context.MODE_PRIVATE).use { stream ->
            stream.write(payloadJson.toByteArray(Charsets.UTF_8))
        }
    } catch (e: Exception) {
        android.util.Log.e("TradingApp", "Failed to persist news payload", e)
    }
}

/**
 * One-time migration: the Volume indicator (and its MA) used to be a hardcoded
 * in-memory default of ON for every user, and the pre-persistence save already
 * wrote showVolume=true into chart_settings. Force both back to OFF once so the
 * volume pane stops appearing by default; everything else is left untouched.
 */
private fun migrateIndicatorDefaults(
    sharedPrefs: android.content.SharedPreferences,
    key: String,
    settings: ChartSettings,
    gson: Gson
): ChartSettings {
    if (sharedPrefs.getBoolean("indicator_defaults_applied_v2", false)) return settings
    sharedPrefs.edit().putBoolean("indicator_defaults_applied_v2", true).apply()
    val migrated = settings.copy(
        indicators = settings.indicators.copy(
            showVolume = false,
            showVolumeMa = false
        )
    )
    sharedPrefs.edit().putString(key, gson.toJson(migrated)).apply()
    return migrated
}

@Composable
fun TradingApp(
    startInPaperTradingPanel: Boolean = false,
    onPaperTradingClose: (() -> Unit)? = null,
    streamFeedType: ChartFeedType = ChartFeedType.EXNESS,
    stateNamespace: String = "stream_${streamFeedType.prefValue}",
    initialSymbol: String? = null
) {
    val context = LocalContext.current
    val forexViewModel: ForexViewModel = viewModel()
    val sharedPrefs = remember { context.getSharedPreferences("trading_prefs", Context.MODE_PRIVATE) }
    val networkPrefs = remember { context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    // Stream chart closed-trade audit: dedupes recorded closes via a persistent
    // fingerprint so each closed deal enters the Trade Ledger / Post-Move Audit
    // exactly once — across explicit closes, TP/SL auto-closes, MT5-side closes,
    // and reconnect history reconciliation.
    fun recordAuditedClose(
        symbol: String,
        type: String,
        entry: Float,
        volume: Float,
        openTime: Long,
        closeTime: Long,
        backupExit: Float?
    ) {
        val canonSym = symbol.uppercase(Locale.US)
        val key = "$canonSym|${type.uppercase(Locale.US)}|$entry|$volume|${closeTime / 120_000L}"
        val recorded = (sharedPrefs.getStringSet("audit_recorded", null) ?: emptySet()).toMutableSet()
        if (!recorded.add(key)) return
        sharedPrefs.edit().putStringSet("audit_recorded", recorded).apply()

        val repo = forexViewModel.tradeHistoryRepository ?: return
        val isBuy = type.equals("buy", ignoreCase = true)
        val sign = if (isBuy) 1f else -1f
        val live = PriceStreamManager.getPrice(symbol)?.toFloat()
        val exit = when {
            live != null && live > 0f -> live
            backupExit != null && backupExit > 0f -> backupExit
            else -> entry
        }
        val pnl = (exit - entry) * volume * sign
        scope.launch(Dispatchers.IO) {
            runCatching {
                repo.saveTrade(
                    TradeEntity(
                        asset = symbol,
                        regimeStack = "",
                        direction = type,
                        entryPrice = entry.toDouble(),
                        exitPrice = exit.toDouble(),
                        pnl = pnl.toDouble(),
                        win = pnl > 0f,
                        entryVolatility = 0.0,
                        entryCorrelation = 0.0,
                        timestamp = openTime
                    )
                )
            }.onFailure { Log.w("TradingApp", "Audit save failed: ${it.message}") }
        }
    }

    fun recordStreamClose(position: Position) {
        recordAuditedClose(
            symbol = position.symbol,
            type = position.type,
            entry = position.entryPrice,
            volume = position.volume,
            openTime = position.time,
            closeTime = System.currentTimeMillis(),
            backupExit = position.tp ?: position.sl
        )
    }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawingInsets = WindowInsets.safeDrawing

    val streamStateNamespace = remember(stateNamespace, streamFeedType) {
        stateNamespace.ifBlank { "stream_${streamFeedType.prefValue}" }
    }
    fun streamScopedKey(base: String): String = "${base}_${streamStateNamespace}"

    val chartFeedQuoteCatalog = remember(streamFeedType) { chartFeedQuotes(streamFeedType) }
    val defaultStreamSymbol = remember(streamFeedType) {
        chartFeedQuoteCatalog.firstOrNull()?.ticker ?: "EURUSD"
    }

    // Core State
    var symbol by remember(streamStateNamespace, streamFeedType) {
        mutableStateOf(
            initialSymbol
                ?.let { chartFeedSymbolFor(streamFeedType, it) }
                ?.takeIf { it.isNotBlank() }
                ?: sharedPrefs.getString(streamScopedKey("selected_symbol"), defaultStreamSymbol)
                    ?.let { chartFeedSymbolFor(streamFeedType, it) }
                    ?: defaultStreamSymbol
        )
    }
    var timeframe by remember(streamStateNamespace) {
        mutableStateOf(sharedPrefs.getString(streamScopedKey("selected_timeframe"), "1h") ?: "1h")
    }
    var activeRange by remember(streamStateNamespace) {
        mutableStateOf(sharedPrefs.getString(streamScopedKey("active_range"), "1Y") ?: "1Y")
    }
    var chartStyle by remember(streamStateNamespace) {
        mutableStateOf(sharedPrefs.getString(streamScopedKey("chart_style"), "candles") ?: "candles")
    }
    var activeTool by remember { mutableStateOf("cursor") }
    var stayInDrawingMode by remember { mutableStateOf(false) }
    var isMagnetEnabled by remember { mutableStateOf(false) }
    
    // Loaded from settings
    var chartSettings by remember(streamStateNamespace) { 
        val loaded = sharedPrefs.getString(streamScopedKey("chart_settings"), null)?.let {
            try { 
                gson.fromJson(it, ChartSettings::class.java)
            } catch (e: Exception) { ChartSettings() }
        } ?: ChartSettings()
        mutableStateOf(
            migrateIndicatorDefaults(sharedPrefs, streamScopedKey("chart_settings"), loaded, gson)
        )
    }

    var isLocked by remember { mutableStateOf(chartSettings.quickActions.isLocked) }
    var areDrawingsVisible by remember { mutableStateOf(true) }
    var isCrosshairActive by remember { mutableStateOf(false) }
    var isReplayActive by remember { mutableStateOf(false) }

    // Currency State
    var selectedCurrency by remember { mutableStateOf("USD") }
    var showCurrencyModal by remember { mutableStateOf(false) }

    // UI visibility state
    var isSidebarVisible by remember { mutableStateOf(chartSettings.quickActions.isSidebarVisible) }
    var isBottomPanelVisible by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Live Data State
    var currentLiveQuote by remember { mutableStateOf<SymbolQuote?>(null) }
    var isConnected by remember { mutableStateOf(false) }

    val recentPairs = remember(streamStateNamespace) {
        val saved = sharedPrefs.getString(streamScopedKey("recent_pairs"), null)
        val list = if (saved != null) {
            try {
                val type = object : TypeToken<List<Pair<String, String>>>() {}.type
                gson.fromJson<List<Pair<String, String>>>(saved, type)
            } catch (e: Exception) { emptyList() }
        } else emptyList()
        mutableStateListOf<Pair<String, String>>().apply { addAll(list) }
    }

    val recentPairQuotes = remember(streamStateNamespace) {
        val saved = sharedPrefs.getString(streamScopedKey("recent_pair_quotes"), null)
        val map = if (saved != null) {
            try {
                val type = object : TypeToken<Map<String, SymbolQuote>>() {}.type
                gson.fromJson<Map<String, SymbolQuote>>(saved, type)
            } catch (e: Exception) { emptyMap() }
        } else emptyMap()
        mutableStateMapOf<String, SymbolQuote>().apply {
            putAll(map.filterValues { isFreshLiveQuote(it) })
        }
    }
    val symbolQuotesByTicker = remember(streamStateNamespace) {
        val saved = sharedPrefs.getString(streamScopedKey("symbol_quotes_by_ticker"), null)
        val map = if (saved != null) {
            try {
                val type = object : TypeToken<Map<String, SymbolQuote>>() {}.type
                gson.fromJson<Map<String, SymbolQuote>>(saved, type)
            } catch (e: Exception) { emptyMap() }
        } else emptyMap()
        mutableStateMapOf<String, SymbolQuote>().apply {
            putAll(map.filterValues { isFreshLiveQuote(it) })
        }
    }

    // Save quotes whenever they update
    LaunchedEffect(recentPairQuotes.toMap()) {
        sharedPrefs.edit().putString(streamScopedKey("recent_pair_quotes"), gson.toJson(recentPairQuotes.toMap())).apply()
    }
    LaunchedEffect(symbolQuotesByTicker.toMap()) {
        sharedPrefs.edit().putString(streamScopedKey("symbol_quotes_by_ticker"), gson.toJson(symbolQuotesByTicker.toMap())).apply()
    }
    LaunchedEffect(symbol, timeframe, activeRange, chartStyle, streamStateNamespace) {
        sharedPrefs.edit()
            .putString(streamScopedKey("selected_symbol"), symbol)
            .putString(streamScopedKey("selected_timeframe"), timeframe)
            .putString(streamScopedKey("active_range"), activeRange)
            .putString(streamScopedKey("chart_style"), chartStyle)
            .apply()
    }
    LaunchedEffect(initialSymbol, streamFeedType) {
        val target = initialSymbol
            ?.let { chartFeedSymbolFor(streamFeedType, it) }
            ?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect
        if (!target.equals(symbol, ignoreCase = true)) {
            symbol = target
        }
    }
    val availableQuotes = remember {
        mutableStateListOf<SymbolInfo>().apply { addAll(chartFeedQuoteCatalog) }
    }

    fun clearNonSelectedQuoteCache() {
        val allowedKeys = chartFeedQuoteCatalog
            .flatMap { listOf(it.ticker, it.brokerSymbol) }
            .filter { it.isNotBlank() }
            .map { it.uppercase(Locale.US) }
            .toSet()
        symbolQuotesByTicker.keys
            .filter { it.uppercase(Locale.US) !in allowedKeys }
            .toList()
            .forEach { symbolQuotesByTicker.remove(it) }
        symbolQuotesByTicker.entries
            .filter { !isFreshLiveQuote(it.value) }
            .map { it.key }
            .toList()
            .forEach { symbolQuotesByTicker.remove(it) }
    }

    LaunchedEffect(streamFeedType) {
        availableQuotes.clear()
        availableQuotes.addAll(chartFeedQuoteCatalog)
        clearNonSelectedQuoteCache()
        if (availableQuotes.none { it.ticker.equals(symbol, ignoreCase = true) || it.brokerSymbol.equals(symbol, ignoreCase = true) }) {
            symbol = chartFeedQuoteCatalog.firstOrNull()?.ticker ?: symbol
        }
    }
    fun brokerSymbolForTicker(symbol: String): String {
        val normalizedSymbol = symbol.trim()
        if (normalizedSymbol.isEmpty()) return normalizedSymbol

        val knownSymbol = availableQuotes.firstOrNull { quote ->
            quote.ticker.equals(normalizedSymbol, ignoreCase = true) ||
                quote.brokerSymbol.equals(normalizedSymbol, ignoreCase = true)
        } ?: chartFeedQuoteCatalog.firstOrNull { quote ->
            quote.ticker.equals(normalizedSymbol, ignoreCase = true) ||
                quote.brokerSymbol.equals(normalizedSymbol, ignoreCase = true)
        }

        return knownSymbol?.brokerSymbol?.ifBlank { knownSymbol.ticker } ?: normalizedSymbol
    }

    fun sourceQuoteSymbols(): List<String> {
        return chartFeedQuoteCatalog
            .map { quote -> quote.brokerSymbol.ifBlank { quote.ticker } }
            .filter { it.isNotEmpty() }
            .distinctBy { it.uppercase(Locale.US) }
    }

    fun liveTradeSourceName(): String = "Exness Live"

    fun liveTradeDefaultAccountLabel(): String = "Exness MT5"

    fun cacheSelectedSourceQuote(quote: SymbolQuote) {
        val incomingName = quote.name.trim()
        val normalizedName = chartFeedSymbolFor(streamFeedType, incomingName)
        val catalogItem = chartFeedQuoteCatalog.firstOrNull {
            it.ticker.equals(normalizedName, ignoreCase = true) ||
                it.brokerSymbol.equals(normalizedName, ignoreCase = true) ||
                it.ticker.equals(incomingName, ignoreCase = true) ||
                it.brokerSymbol.equals(incomingName, ignoreCase = true)
        } ?: return
        val keyedQuote = quote.copy(name = catalogItem.ticker)
        listOf(catalogItem.ticker, catalogItem.brokerSymbol, incomingName)
            .filter { it.isNotBlank() }
            .distinctBy { it.uppercase(Locale.US) }
            .forEach { key ->
                val upperKey = key.uppercase(Locale.US)
                if (keyedQuote.lastPrice > 0f || symbolQuotesByTicker[upperKey] == null) {
                    symbolQuotesByTicker[upperKey] = keyedQuote
                }
            }
        PriceStreamManager.updatePrice(catalogItem.ticker, keyedQuote.lastPrice.toDouble())
        
        // Update MarketDataStore so prices propagate to market overview list
        val category = when {
            catalogItem.type.contains("forex", ignoreCase = true) -> MarketCategory.FOREX
            catalogItem.type.contains("crypto", ignoreCase = true) -> MarketCategory.CRYPTO
            catalogItem.type.contains("stock", ignoreCase = true) -> MarketCategory.STOCK
            catalogItem.type.contains("index", ignoreCase = true) -> MarketCategory.INDICES
            catalogItem.type.contains("bond", ignoreCase = true) -> MarketCategory.BONDS
            catalogItem.type.contains("commodity", ignoreCase = true) -> MarketCategory.COMMODITIES
            else -> MarketCategory.FOREX
        }
        
        val forexPair = ForexPair(
            symbol = catalogItem.ticker,
            name = catalogItem.name,
            price = keyedQuote.lastPrice.toDouble(),
            change = keyedQuote.change.toDouble(),
            changePercent = keyedQuote.changePercent.toDouble(),
            category = category
        )
        MarketDataStore.updatePair(forexPair)
        
        if (catalogItem.ticker.equals(symbol, ignoreCase = true) || catalogItem.brokerSymbol.equals(symbol, ignoreCase = true)) {
            currentLiveQuote = keyedQuote
        }
    }

    val visibleQuoteSymbols = remember { mutableStateListOf<String>() }
    val visibleRecentSymbols = remember { mutableStateListOf<String>() }
    val mt5Host = remember { NetworkConfig.mt5Host(context) }
    val mt5Port = remember { NetworkConfig.mt5Port(context) }

    // Trading States
    val positions = remember { mutableStateListOf<Position>() }
    val localPositions = remember { mutableStateListOf<Position>() }
    val orders = remember { mutableStateListOf<Order>() }
    val orderHistory = remember { mutableStateListOf<Order>() }
    val balanceHistory = remember { mutableStateListOf<BalanceRecord>() }
    var mt5AccountInfo by remember { mutableStateOf<Mt5Service.AccountInfo?>(null) }

    val reverseBridge = remember { 
        Mt5ReverseBridge(
            pcIpAddress = mt5Host,
            port = mt5Port
        )
    }
    
    val mt5Service = remember {
        Mt5Service(
            pcIpAddress = mt5Host,
            port = mt5Port,
            onHistoryUpdate = { _, _ -> },
            onNewsUpdate = { newsPayload ->
                android.util.Log.i("TradingApp", "Received ${newsPayload.items.size} MT5 news items in Stream host")
                Mt5NewsStore.updateNews(newsPayload.items)
            },
            onQuoteUpdate = { quote ->
                // Propagate price updates to the global PriceStreamManager
                PriceStreamManager.updatePrice(quote.name, quote.lastPrice.toDouble())
                
                val pair = com.asc.markets.data.FOREX_PAIRS.find { it.symbol == quote.name }
                if (pair != null) {
                    val updatedPair = pair.copy(
                        price = quote.lastPrice.toDouble(),
                        change = quote.lastPrice.toDouble() - pair.price,
                        changePercent = if (pair.price > 0) ((quote.lastPrice.toDouble() - pair.price) / pair.price * 100) else 0.0
                    )
                    MarketDataStore.updatePair(updatedPair)
                }
            },
            onAccountUpdate = { accountInfo ->
                mt5AccountInfo = accountInfo
            },
onPositionsUpdate = { newPositions ->
                                    // Server-side closes (TP/SL hit, broker exit): positions that
                                    // vanished from the confirmed snapshot get audited too. Local
                                    // placeholder ids are UUIDs with dashes; server tickets are numeric.
                                    val prevSnapshot = positions.toList()
                                    val confirmedBefore = prevSnapshot.filter { !it.id.contains("-") }.map { it.id }.toSet()
                                    val confirmedNow = newPositions.map { it.id }.toSet()
                                    prevSnapshot.filter { it.id in confirmedBefore && it.id !in confirmedNow }
                                        .forEach { recordStreamClose(it) }

                                    positions.clear()
                                    positions.addAll(newPositions)
            },
            onOrdersUpdate = { newOrders ->
                orders.clear()
                orders.addAll(newOrders)
            },
onHistoryOrdersUpdate = { newHistory ->
                                    // Reconcile closed deals from MT5 account history so trades closed
                                    // while the app wasn't connected still get audited (persistent
                                    // fingerprint dedupes against live snapshot closes).
                                    newHistory.forEach { h ->
                                        if (h.status.equals("Filled", ignoreCase = true)) {
                                            recordAuditedClose(
                                                symbol = h.symbol,
                                                type = h.type,
                                                entry = h.price,
                                                volume = h.volume,
                                                openTime = h.time,
                                                closeTime = h.closingTime ?: h.time,
                                                backupExit = if (h.averagePrice > 0f) h.averagePrice else null
                                            )
                                        }
                                    }
                                    orderHistory.clear()
                                    orderHistory.addAll(newHistory)
                                },
            onBalanceHistoryUpdate = { newBalanceHistory ->
                balanceHistory.clear()
                balanceHistory.addAll(newBalanceHistory)
            },
            onConnectionStatusUpdate = { connected ->
                isConnected = connected
            }
        )
    }

    val watchlistSymbols by remember {
        derivedStateOf {
            (visibleQuoteSymbols + visibleRecentSymbols)
                .map { it.uppercase(Locale.US) }
                .distinct()
        }
    }

    // Subscribe to all symbols immediately on app startup
    LaunchedEffect(Unit) {
        val sourceSymbols = sourceQuoteSymbols()
        // Subscribe to all quote services regardless of current chartFeedType
        mt5Service.updateWatchlist(sourceSymbols)
    }

    LaunchedEffect(streamFeedType, chartFeedQuoteCatalog, timeframe) {
        val sourceSymbols = sourceQuoteSymbols()
        // Keep all services running, just update subscriptions as needed
        mt5Service.updateWatchlist(sourceSymbols)
    }

    LaunchedEffect(symbol, timeframe) {
        // Ensure the symbol is valid for the current chart feed before adding to recent history
        val isValidSymbol = availableQuotes.any { 
            it.ticker.equals(symbol, ignoreCase = true) || 
            it.brokerSymbol.equals(symbol, ignoreCase = true) 
        }

        if (isValidSymbol) {
            val newPair = symbol to timeframe
            val exists = recentPairs.any { it.first == symbol && it.second == timeframe }
            // Only add to history if it's a new pair. Don't reorder existing ones to avoid UI jumping.
            if (!exists) {
                recentPairs.add(0, newPair)
                if (recentPairs.size > 10) {
                    recentPairs.removeAt(recentPairs.size - 1)
                }
                sharedPrefs.edit().putString(streamScopedKey("recent_pairs"), gson.toJson(recentPairs.toList())).apply()
            }
        }

        val normalizedSymbol = symbol.trim()
        currentLiveQuote = symbolQuotesByTicker[normalizedSymbol.uppercase(Locale.US)]?.takeIf { isFreshLiveQuote(it) }
    }

    // Settings & Data
    val drawings = remember { mutableStateListOf<Drawing>() }
    val history = remember { mutableStateListOf<ChartSnapshot>() }
    val redoStack = remember { mutableStateListOf<ChartSnapshot>() }
    val userAlerts = remember { mutableStateOf(emptyList<UserAlert>()) }
    var liveTradeAccountLabel by remember(streamFeedType) { mutableStateOf(liveTradeDefaultAccountLabel()) }
    var liveTradeRefreshToken by remember(streamStateNamespace) { mutableIntStateOf(0) }

    LaunchedEffect(positions.toList(), localPositions.toList(), orders.toList(), streamFeedType) {
        val allowedSymbols = sourceQuoteSymbols().map { it.uppercase(Locale.US) }.toSet()
        val tradeSymbols = ((positions + localPositions).map { it.symbol } + orders.map { it.symbol })
            .flatMap(::liveQuoteSymbolKeys)
            .asSequence()
            .map(::brokerSymbolForTicker)
            .filter { it.isNotEmpty() }
            .filter { it.uppercase(Locale.US) in allowedSymbols }
            .distinctBy { it.uppercase(Locale.US) }
            .toList()
        visibleRecentSymbols.clear()
        visibleRecentSymbols.addAll(tradeSymbols)
    }
    val cachedCalendarDisplay = remember {
        sharedPrefs.getString("calendar_display_payload", null)?.let {
            try {
                gson.fromJson(it, EconomicCalendarDisplayPayload::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }
    val cachedCalendarAiJson = remember {
        sharedPrefs.getString("calendar_ai_payload", "") ?: ""
    }
    var calendarDisplayPayload by remember { mutableStateOf(cachedCalendarDisplay) }
    var calendarAiPayloadJson by remember { mutableStateOf(cachedCalendarAiJson) }
    var isCalendarLoading by remember { mutableStateOf(cachedCalendarDisplay == null) }
    var calendarSelectedDateIso by remember {
        mutableStateOf(cachedCalendarDisplay?.selectedDateIso ?: todayIsoDate())
    }
    var calendarRequestVersion by remember { mutableIntStateOf(0) }
    
    val tradeNotifications = remember { mutableStateListOf<TradeNotification>() }
    val notificationsToDismiss = remember { mutableStateListOf<String>() }
    val symbolQuoteSnapshot = symbolQuotesByTicker.toMap()

    LaunchedEffect(streamFeedType) {
        mt5AccountInfo = null
        positions.clear()
        localPositions.clear()
        orders.clear()
        orderHistory.clear()
        balanceHistory.clear()
        isConnected = false
        liveTradeAccountLabel = liveTradeDefaultAccountLabel()
        mt5Service.connect()
    }

    DisposableEffect(Unit) {
        onDispose {
            reverseBridge.disconnect()
            mt5Service.disconnect()
        }
    }

    fun placeStreamOrder(position: Position, orderType: String, stopLimitPrice: Float?) {
        // Route commands with the broker-side symbol (e.g. BTCUSDm), keep display ticker locally
        val brokerPosition = position.copy(symbol = mt5Service.outboundSymbolFor(position.symbol))
        if (orderType == "Market Execution") {
            reverseBridge.placePosition(brokerPosition)
            positions.add(position)
            tradeNotifications.add(
                TradeNotification(
                    symbol = position.symbol,
                    volume = position.volume,
                    price = position.entryPrice,
                    isBuy = position.type == "buy",
                    type = "executed"
                )
            )
        } else {
            val order = Order(
                symbol = brokerPosition.symbol,
                type = position.type,
                orderType = orderType,
                status = "Working",
                price = position.entryPrice,
                stopLimitPrice = stopLimitPrice,
                volume = position.volume,
                time = position.time,
                tp = position.tp,
                sl = position.sl
            )
            reverseBridge.placeOrder(order)
            orders.add(
                order.copy(symbol = position.symbol)
            )
        }
    }

    fun closeStreamPosition(position: Position) {
        recordStreamClose(position)
        reverseBridge.closePosition(position.copy(symbol = mt5Service.outboundSymbolFor(position.symbol)))
        positions.removeAll { it.id == position.id }
        localPositions.removeAll { it.id == position.id }
    }

    fun modifyStreamPosition(updatedPosition: Position, tp: Float?, sl: Float?) {
        reverseBridge.modifyPosition(updatedPosition.copy(symbol = mt5Service.outboundSymbolFor(updatedPosition.symbol)), tp, sl)
        val idxLocal = localPositions.indexOfFirst { it.id == updatedPosition.id }
        if (idxLocal != -1) localPositions[idxLocal] = updatedPosition
        val idxRemote = positions.indexOfFirst { it.id == updatedPosition.id }
        if (idxRemote != -1) positions[idxRemote] = updatedPosition
    }

    // Kill Switch Listener
    DisposableEffect(sharedPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "kill_switch_trigger") {
                Log.w("TradingApp", "!!! KILL-SWITCH TRIGGERED !!! Closing all active positions.")
                (positions + localPositions).toList().forEach { pos ->
                    closeStreamPosition(pos)
                }
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Timezone list
    val timeZones = remember {
        listOf(
            TimeZone("UTC", "UTC", ""),
            TimeZone("Exchange", "Exchange", ""),
            TimeZone("(UTC-7) Los Angeles", "America/Los_Angeles", "")
        )
    }
    var selectedTz by remember { mutableStateOf(timeZones.find { it.label == "(UTC-7) Los Angeles" } ?: timeZones[0]) }

    var hiddenIndicators by remember { mutableStateOf(setOf<String>()) }

    var showRsi by remember { mutableStateOf(chartSettings.indicators.showRsi) }
    var rsiPeriod by remember { mutableIntStateOf(chartSettings.indicators.rsiPeriod) }
    var rsiColor by remember { mutableStateOf(Color(0xFF7E57C2)) }
    var rsiShowLabels by remember { mutableStateOf(chartSettings.indicators.rsiShowLabels) }
    var rsiShowLines by remember { mutableStateOf(chartSettings.indicators.rsiShowLines) }

    var showEma10 by remember { mutableStateOf(chartSettings.indicators.showEma10) }
    var ema10Period by remember { mutableIntStateOf(chartSettings.indicators.ema10Period) }
    var ema10Color by remember { mutableStateOf(Color.White) }
    var ema10ShowLabels by remember { mutableStateOf(chartSettings.indicators.ema10ShowLabels) }
    var ema10ShowLines by remember { mutableStateOf(chartSettings.indicators.ema10ShowLines) }

    var showEma20 by remember { mutableStateOf(chartSettings.indicators.showEma20) }
    var ema20Period by remember { mutableIntStateOf(chartSettings.indicators.ema20Period) }
    var ema20Color by remember { mutableStateOf(Color.White) }
    var ema20ShowLabels by remember { mutableStateOf(chartSettings.indicators.ema20ShowLabels) }
    var ema20ShowLines by remember { mutableStateOf(chartSettings.indicators.ema20ShowLines) }

    var showSma1 by remember { mutableStateOf(chartSettings.indicators.showSma1) }
    var sma1Period by remember { mutableIntStateOf(chartSettings.indicators.sma1Period) }
    var sma1Color by remember { mutableStateOf(Color.White) }
    var sma1ShowLabels by remember { mutableStateOf(chartSettings.indicators.sma1ShowLabels) }
    var sma1ShowLines by remember { mutableStateOf(chartSettings.indicators.sma1ShowLines) }

    var showSma2 by remember { mutableStateOf(chartSettings.indicators.showSma2) }
    var sma2Period by remember { mutableIntStateOf(chartSettings.indicators.sma2Period) }
    var sma2Color by remember { mutableStateOf(Color.White) }
    var sma2ShowLabels by remember { mutableStateOf(chartSettings.indicators.sma2ShowLabels) }
    var sma2ShowLines by remember { mutableStateOf(chartSettings.indicators.sma2ShowLines) }

    var showVwap by remember { mutableStateOf(chartSettings.indicators.showVwap) }
    var vwapShowLabels by remember { mutableStateOf(chartSettings.indicators.vwapShowLabels) }
    var vwapShowLines by remember { mutableStateOf(chartSettings.indicators.vwapShowLines) }

    var showBb by remember { mutableStateOf(chartSettings.indicators.showBb) }
    var bbPeriod by remember { mutableIntStateOf(chartSettings.indicators.bbPeriod) }
    var bbStdDev by remember { mutableFloatStateOf(chartSettings.indicators.bbStdDev) }
    var bbColor by remember { mutableStateOf(Color(0xFF2196F3)) }
    var bbShowLabels by remember { mutableStateOf(chartSettings.indicators.bbShowLabels) }
    var bbShowLines by remember { mutableStateOf(chartSettings.indicators.bbShowLines) }

    var showAtr by remember { mutableStateOf(chartSettings.indicators.showAtr) }
    var atrPeriod by remember { mutableIntStateOf(chartSettings.indicators.atrPeriod) }
    var atrColor by remember { mutableStateOf(Color(0xFFFF5252)) }
    var atrShowLabels by remember { mutableStateOf(chartSettings.indicators.atrShowLabels) }
    var atrShowLines by remember { mutableStateOf(chartSettings.indicators.atrShowLines) }

    var showMacd by remember { mutableStateOf(chartSettings.indicators.showMacd) }
    var macdFast by remember { mutableIntStateOf(chartSettings.indicators.macdFast) }
    var macdSlow by remember { mutableIntStateOf(chartSettings.indicators.macdSlow) }
    var macdSignal by remember { mutableIntStateOf(chartSettings.indicators.macdSignal) }
    var macdColor by remember { mutableStateOf(Color(0xFF2196F3)) }
    var macdSignalColor by remember { mutableStateOf(Color(0xFFFF5252)) }
    var macdShowLabels by remember { mutableStateOf(chartSettings.indicators.macdShowLabels) }
    var macdShowLines by remember { mutableStateOf(chartSettings.indicators.macdShowLines) }

    // Volume State
    var showVolume by remember { mutableStateOf(chartSettings.indicators.showVolume) }
    var volumeShowLabels by remember { mutableStateOf(chartSettings.indicators.volumeShowLabels) }
    var volumeShowLines by remember { mutableStateOf(chartSettings.indicators.volumeShowLines) }
    var showVolumeMa by remember { mutableStateOf(chartSettings.indicators.showVolumeMa) }
    var volumeMaLength by remember { mutableIntStateOf(chartSettings.indicators.volumeMaLength) }
    var volumeMaColor by remember { mutableStateOf(Color(0xFF2196F3)) }
    var volumeGrowingColor by remember { mutableStateOf(Color(0xFF26A69A)) }
    var volumeFallingColor by remember { mutableStateOf(Color(0xFFEF5350)) }
    var volumeColorBasedOnPreviousClose by remember { mutableStateOf(false) }

    // Premium & Discount (Editors' picks) - BigBeluga overlay
    var showPremiumDiscount by remember { mutableStateOf(chartSettings.indicators.showPremiumDiscount) }
    // Fair Value Gap (Editors' picks) - LuxAlgo overlay
    var showFairValueGap by remember { mutableStateOf(chartSettings.indicators.showFairValueGap) }
    // Supply and Demand Daily (Editors' picks) - LuxAlgo overlay
    var showSupplyDemandDaily by remember { mutableStateOf(chartSettings.indicators.showSupplyDemandDaily) }
    // OTE visible chart (Editors' picks) - twingall overlay (VisibleChart Fib Box 61.8-78.6%)
    var showOteVisibleChart by remember { mutableStateOf(chartSettings.indicators.showOteVisibleChart) }
    // Liquidity Delta Profiler (Editors' picks) - LuxAlgo overlay (BSL/SSL pivot zones + delta quadrants)
    var showLiquidityDeltaProfiler by remember { mutableStateOf(chartSettings.indicators.showLiquidityDeltaProfiler) }
    // Auto Fib Retracement (Editors' picks) - enabled vs visible so Hide keeps it added
    val autoFibPrefs = remember { context.getSharedPreferences("trading_prefs", android.content.Context.MODE_PRIVATE) }
    var ldpSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.LiquidityDeltaProfilerSettings.fromJson(
                autoFibPrefs.getString("ldpSettings", null)
            )
        )
    }
    var showLdpSettingsModal by remember { mutableStateOf(false) }
    // EQH/EQL Liquidity Zones (Editors' picks) - LuxAlgo overlay (equal highs/lows boxes)
    var showEqhEqlLiquidityZones by remember { mutableStateOf(chartSettings.indicators.showEqhEqlLiquidityZones) }
    var eqhEqlSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.EqhEqlLiquidityZonesSettings.fromJson(
                autoFibPrefs.getString("eqhEqlSettings", null)
            )
        )
    }
    var showEqhEqlSettingsModal by remember { mutableStateOf(false) }
    // Power Hour Breakout (Editors' picks) - LuxAlgo overlay (NY session box + extensions + fibos + breakouts)
    var showPowerHourBreakout by remember { mutableStateOf(chartSettings.indicators.showPowerHourBreakout) }
    var powerHourSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.PowerHourBreakoutSettings.fromJson(
                autoFibPrefs.getString("powerHourSettings", null)
            )
        )
    }
    var showPowerHourSettingsModal by remember { mutableStateOf(false) }
    // Trendline Breakouts With Targets (Editors' picks) - ChartPrime overlay
    var showTrendlineBreakouts by remember { mutableStateOf(chartSettings.indicators.showTrendlineBreakouts) }
    var trendlineSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.TrendlineBreakoutsSettings.fromJson(
                autoFibPrefs.getString("trendlineSettings", null)
            )
        )
    }
    var showTrendlineSettingsModal by remember { mutableStateOf(false) }
    // Trendline Breakout Navigator (Editors' picks) - LuxAlgo overlay (swing trendlines + wick dots)
    var showTrendlineNavigator by remember { mutableStateOf(chartSettings.indicators.showTrendlineNavigator) }
    var navigatorSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.TrendlineNavigatorSettings.fromJson(
                autoFibPrefs.getString("navigatorSettings", null)
            )
        )
    }
    var showNavigatorSettingsModal by remember { mutableStateOf(false) }
    // Liquidity Pools (Editors' picks) - LuxAlgo overlay (running-extreme zone boxes + volume labels)
    var showLiquidityPools by remember { mutableStateOf(chartSettings.indicators.showLiquidityPools) }
    var liquidityPoolsSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.LiquidityPoolsSettings.fromJson(
                autoFibPrefs.getString("liquidityPoolsSettings", null)
            )
        )
    }
    var showLiquidityPoolsSettingsModal by remember { mutableStateOf(false) }
    // Pure Price Action Order & Breaker Blocks (Editors' picks) - LuxAlgo overlay (last registered OB/BB panes)
    var showOrderBlockBreaker by remember { mutableStateOf(chartSettings.indicators.showOrderBlockBreaker) }
    var obbSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.OrderBlockBreakerSettings.fromJson(
                autoFibPrefs.getString("orderBlockBreakerSettings", null)
            )
        )
    }
    var showObbSettingsModal by remember { mutableStateOf(false) }
    // Volumatic Fair Value Gaps (Editors' picks) - BigBeluga overlay (volume-split FVG boxes)
    var showVolumaticFvg by remember { mutableStateOf(chartSettings.indicators.showVolumaticFvg) }
    var volumaticFvgSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.VolumaticFvgSettings.fromJson(
                autoFibPrefs.getString("volumaticFvgSettings", null)
            )
        )
    }
    var showVolumaticFvgSettingsModal by remember { mutableStateOf(false) }
    var fvgSettings by remember { mutableStateOf(com.trading.app.indicators.FairValueGapSettings.fromJson(autoFibPrefs.getString("fvgSettings", null))) }
    var showFvgSettingsModal by remember { mutableStateOf(false) }
    var autoFibEnabled by remember { mutableStateOf(autoFibPrefs.getBoolean("autoFibEnabled", false)) }
    var autoFibVisible by remember { mutableStateOf(autoFibPrefs.getBoolean("autoFibVisible", true)) }
    var autoFibSettings by remember { mutableStateOf(loadAutoFibSettings(autoFibPrefs)) }
    var showAutoFibSettingsModal by remember { mutableStateOf(false) }

    // Supply & Demand Visible Range settings (persisted as JSON)
    var sdVrSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.SupplyDemandVrSettings.fromJson(
                autoFibPrefs.getString("sdVrSettings", null)
            )
        )
    }
    var showSdVrSettingsModal by remember { mutableStateOf(false) }

    // Confluence FVG Finder settings (persisted as JSON)
    var cfvgSettings by remember {
        mutableStateOf(
            com.trading.app.indicators.ConfluenceFvgSettings.fromJson(
                autoFibPrefs.getString("cfvgSettings", null)
            )
        )
    }
    var showCfvgSettingsModal by remember { mutableStateOf(false) }

    fun persistAutoFib() {
        autoFibPrefs.edit()
            .putBoolean("autoFibEnabled", autoFibEnabled)
            .putBoolean("autoFibVisible", autoFibVisible)
            .apply()
    }

    // Confluence FVG Finder (Editors' picks) - merged 60/120/240m FVG zones
    var confluenceFvgEnabled by remember { mutableStateOf(autoFibPrefs.getBoolean("cfvgEnabled", false)) }
    var confluenceFvgVisible by remember { mutableStateOf(autoFibPrefs.getBoolean("cfvgVisible", true)) }

    fun persistConfluence() {
        autoFibPrefs.edit()
            .putBoolean("cfvgEnabled", confluenceFvgEnabled)
            .putBoolean("cfvgVisible", confluenceFvgVisible)
            .apply()
    }

    // Persist settings whenever relevant parts change
    LaunchedEffect(chartSettings, isLocked, isSidebarVisible, 
        showRsi, rsiPeriod, rsiShowLabels, rsiShowLines,
        showEma10, ema10Period, ema10ShowLabels, ema10ShowLines,
        showEma20, ema20Period, ema20ShowLabels, ema20ShowLines,
        showSma1, sma1Period, sma1ShowLabels, sma1ShowLines,
        showSma2, sma2Period, sma2ShowLabels, sma2ShowLines,
        showVwap, vwapShowLabels, vwapShowLines,
        showBb, bbPeriod, bbStdDev, bbShowLabels, bbShowLines,
        showAtr, atrPeriod, atrShowLabels, atrShowLines,
        showMacd, macdFast, macdSlow, macdSignal, macdShowLabels, macdShowLines,
        volumeShowLabels, volumeShowLines, showVolume, showVolumeMa, volumeMaLength,
        showPremiumDiscount, showFairValueGap, showSupplyDemandDaily, showOteVisibleChart, showLiquidityDeltaProfiler, showEqhEqlLiquidityZones) {
        val updatedSettings = chartSettings.copy(
            quickActions = chartSettings.quickActions.copy(
                isLocked = isLocked,
                isSidebarVisible = isSidebarVisible
            ),
            indicators = chartSettings.indicators.copy(
                showRsi = showRsi,
                rsiPeriod = rsiPeriod,
                rsiShowLabels = rsiShowLabels,
                rsiShowLines = rsiShowLines,
                showEma10 = showEma10,
                ema10Period = ema10Period,
                ema10ShowLabels = ema10ShowLabels,
                ema10ShowLines = ema10ShowLines,
                showEma20 = showEma20,
                ema20Period = ema20Period,
                ema20ShowLabels = ema20ShowLabels,
                ema20ShowLines = ema20ShowLines,
                showSma1 = showSma1,
                sma1Period = sma1Period,
                sma1ShowLabels = sma1ShowLabels,
                sma1ShowLines = sma1ShowLines,
                showSma2 = showSma2,
                sma2Period = sma2Period,
                sma2ShowLabels = sma2ShowLabels,
                sma2ShowLines = sma2ShowLines,
                showVwap = showVwap,
                vwapShowLabels = vwapShowLabels,
                vwapShowLines = vwapShowLines,
                showBb = showBb,
                bbPeriod = bbPeriod,
                bbStdDev = bbStdDev,
                bbShowLabels = bbShowLabels,
                bbShowLines = bbShowLines,
                showAtr = showAtr,
                atrPeriod = atrPeriod,
                atrShowLabels = atrShowLabels,
                atrShowLines = atrShowLines,
                showMacd = showMacd,
                macdFast = macdFast,
                macdSlow = macdSlow,
                macdSignal = macdSignal,
                macdShowLabels = macdShowLabels,
                macdShowLines = macdShowLines,
                volumeShowLabels = volumeShowLabels,
                volumeShowLines = volumeShowLines,
                showVolume = showVolume,
                showVolumeMa = showVolumeMa,
                volumeMaLength = volumeMaLength,
                showPremiumDiscount = showPremiumDiscount,
                showFairValueGap = showFairValueGap,
                showSupplyDemandDaily = showSupplyDemandDaily,
                showOteVisibleChart = showOteVisibleChart,
                showLiquidityDeltaProfiler = showLiquidityDeltaProfiler,
                showEqhEqlLiquidityZones = showEqhEqlLiquidityZones
            )
        )
        sharedPrefs.edit().putString(streamScopedKey("chart_settings"), gson.toJson(updatedSettings)).apply()
    }
    
    // Tab State
    var activeTab by remember { mutableStateOf("Trading Panel") }
    var analysisContent by remember { mutableStateOf("Click refresh to generate analysis...") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var currentIndicatorData by remember { mutableStateOf(IndicatorData()) }

    // Modal Visibility
    var showQuotes by remember { mutableStateOf(false) }
    var showIndicatorModal by remember { mutableStateOf(false) }
    var selectedIndicatorId by remember { mutableStateOf<String?>(null) }
    var showGoToDateModal by remember { mutableStateOf(false) }
    var targetTimestamp by remember { mutableStateOf<Long?>(null) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showChartSettingsBottomSheet by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableStateOf<String?>(null) }
    var showToolSearchModal by remember { mutableStateOf(false) }
    var showAlertModal by remember { mutableStateOf(false) }
    var showCaptureModal by remember { mutableStateOf(false) }
    var showIndicatorSettingsModal by remember { mutableStateOf<String?>(null) }
    var showTimeZoneModal by remember { mutableStateOf(false) }
    var showCalendarPage by remember { mutableStateOf(false) }
    var showCalendarFilterPage by remember { mutableStateOf(false) }
    var calendarFilters by remember { mutableStateOf(CalendarFilters()) }
    var showDrawingsModal by remember { mutableStateOf(false) }
    var showNewsPage by remember { mutableStateOf(false) }
    val cachedNewsPayload = remember {
        sharedPrefs.getString("news_payload", null)?.let {
            try {
                gson.fromJson(it, NewsPayload::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }
    val newsItems = remember {
        mutableStateListOf<NewsItem>().apply { addAll(cachedNewsPayload?.items ?: emptyList()) }
    }
    var isNewsLoading by remember { mutableStateOf(cachedNewsPayload == null) }
    var showAnalysisHubModal by remember { mutableStateOf(false) }
    var showChartTypeModal by remember { mutableStateOf(false) }
    var showFloatingTradingButtons by remember { mutableStateOf(false) }
    var showOrderModal by remember { mutableStateOf(false) }
    var showSimpleOrderPage by remember { mutableStateOf(false) }
    var showPaperTradingPanel by remember { mutableStateOf(false) }
    var orderModalChartData by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
    var orderModalInitialSide by remember { mutableStateOf("buy") }
    var orderModalShowMarketSideButtons by remember { mutableStateOf(true) }
    var selectedPositionToModify by remember { mutableStateOf<Position?>(null) }
    var showModifyModal by remember { mutableStateOf(false) }
    var showPositionActionsModal by remember { mutableStateOf(false) }

    // Quick Actions State
    var showQuickActions by remember { mutableStateOf(false) }
    var quickActionsModalOffset by remember { 
        mutableStateOf(IntOffset(chartSettings.quickActions.modalX, chartSettings.quickActions.modalY)) 
    }
    var quickActionsButtonOffset by remember { 
        // Force the quick actions button to be visible and in a reasonable position
        mutableStateOf(IntOffset(chartSettings.quickActions.buttonX, chartSettings.quickActions.buttonY)) 
    }
    var isTimezonePaneVisible by remember { mutableStateOf(chartSettings.quickActions.isTimezoneVisible) }

    LaunchedEffect(Unit) {
        CalendarSnapshotStore.latestDisplayPayload = calendarDisplayPayload
        CalendarSnapshotStore.latestAiPayloadJson = calendarAiPayloadJson
        CalendarSnapshotStore.latestAiPayload =
            if (calendarAiPayloadJson.isBlank()) {
                null
            } else {
                try {
                    gson.fromJson(calendarAiPayloadJson, EconomicCalendarAiPayload::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        NewsSnapshotStore.latestPayload = cachedNewsPayload
        NewsSnapshotStore.latestAiPayloadJson =
            if (cachedNewsPayload == null) "" else gson.toJson(cachedNewsPayload)
    }

    // Responsive Reposition & Safe Area Awareness
    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp, safeDrawingInsets) {
        val leftInset = safeDrawingInsets.getLeft(density, layoutDirection)
        val topInset = safeDrawingInsets.getTop(density)
        val rightInset = safeDrawingInsets.getRight(density, layoutDirection)
        val bottomInset = safeDrawingInsets.getBottom(density)

        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
        
        val buttonSizePx = with(density) { 70.dp.roundToPx() }
        val modalWidthPx = with(density) { 260.dp.roundToPx() }
        val modalHeightPx = with(density) { 500.dp.roundToPx() }

        val minX = leftInset
        val maxX = (screenWidthPx - rightInset - buttonSizePx).coerceAtLeast(minX)
        val minY = topInset
        val maxY = (screenHeightPx - bottomInset - buttonSizePx).coerceAtLeast(minY)

        val clampedButtonX = quickActionsButtonOffset.x.coerceIn(minX, maxX)
        val clampedButtonY = quickActionsButtonOffset.y.coerceIn(minY, maxY)
        
        if (clampedButtonX != quickActionsButtonOffset.x || clampedButtonY != quickActionsButtonOffset.y) {
            quickActionsButtonOffset = IntOffset(clampedButtonX, clampedButtonY)
        }

        val modalMaxX = (screenWidthPx - rightInset - modalWidthPx).coerceAtLeast(minX)
        val modalMaxY = (screenHeightPx - bottomInset - modalHeightPx).coerceAtLeast(minY)

        val clampedModalX = quickActionsModalOffset.x.coerceIn(minX, modalMaxX)
        val clampedModalY = quickActionsModalOffset.y.coerceIn(minY, modalMaxY)

        if (clampedModalX != quickActionsModalOffset.x || clampedModalY != quickActionsModalOffset.y) {
            quickActionsModalOffset = IntOffset(clampedModalX, clampedModalY)
        }
    }

    // Update coordinates in persistent state
    LaunchedEffect(quickActionsButtonOffset, quickActionsModalOffset, isTimezonePaneVisible) {
        chartSettings = chartSettings.copy(
            quickActions = chartSettings.quickActions.copy(
                buttonX = quickActionsButtonOffset.x,
                buttonY = quickActionsButtonOffset.y,
                modalX = quickActionsModalOffset.x,
                modalY = quickActionsModalOffset.y,
                isTimezoneVisible = isTimezonePaneVisible
            )
        )
    }

    val refreshAnalysis = {
        scope.launch {
            isAnalyzing = true
            try {
                delay(1500)
                val rsiText = currentIndicatorData.rsi?.let { "RSI is ${String.format("%.2f", it)}." } ?: "RSI data unavailable."
                val macdText = currentIndicatorData.macd?.let { "MACD is ${String.format("%.4f", it)}." } ?: "MACD data unavailable."
                val atrText = currentIndicatorData.atr?.let { "ATR is ${String.format("%.4f", it)}." } ?: "ATR data unavailable."
                
                analysisContent = "AI Analysis for $symbol: Market is currently showing mixed signals. $rsiText $macdText $atrText Trend remains bullish on higher timeframes."
            } catch (e: Exception) {
                analysisContent = "Error: ${e.message}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun handleRangeChange(range: String) {
        activeRange = range
        timeframe = when (range) {
            "1D" -> "5m"
            "5D" -> "15m"
            "1M" -> "1h"
            "3M" -> "4h"
            "5Y", "All" -> "W"
            else -> "D"
        }
    }

    fun handleIndicatorSelect(id: String) {
        val lower = id.trim().lowercase()
        // Editors' picks - check substring first before exact when
        if (lower.contains("premium") && lower.contains("discount")) {
            showPremiumDiscount = !showPremiumDiscount
        } else if (lower.contains("fair value gap") || lower == "fvg" || lower.contains("fvg [luxalgo]")) {
            showFairValueGap = !showFairValueGap
        } else if (lower.contains("supply and demand")) {
            showSupplyDemandDaily = !showSupplyDemandDaily
        } else if (lower.contains("ote") && lower.contains("visible")) {
            showOteVisibleChart = !showOteVisibleChart
        } else if (lower.contains("eqh/eql")) {
            showEqhEqlLiquidityZones = !showEqhEqlLiquidityZones
            android.util.Log.d("TradingApp", "EQH/EQL toggled -> $showEqhEqlLiquidityZones")
        } else if (lower.contains("power hour")) {
            showPowerHourBreakout = !showPowerHourBreakout
            android.util.Log.d("TradingApp", "Power Hour toggled -> $showPowerHourBreakout")
        } else if (lower.contains("navigator")) {
            showTrendlineNavigator = !showTrendlineNavigator
            android.util.Log.d("TradingApp", "Trendline Navigator toggled -> $showTrendlineNavigator")
        } else if (lower.contains("liquidity pools")) {
            showLiquidityPools = !showLiquidityPools
            android.util.Log.d("TradingApp", "Liquidity Pools toggled -> $showLiquidityPools")
        } else if (lower.contains("order") || lower.contains("breaker")) {
            showOrderBlockBreaker = !showOrderBlockBreaker
            android.util.Log.d("TradingApp", "Order Block Breaker toggled -> $showOrderBlockBreaker")
        } else if (lower.contains("volumatic")) {
            showVolumaticFvg = !showVolumaticFvg
            android.util.Log.d("TradingApp", "Volumatic FVG toggled -> $showVolumaticFvg")
        } else if (lower.contains("trendline") || lower.contains("chartprime")) {
            showTrendlineBreakouts = !showTrendlineBreakouts
            android.util.Log.d("TradingApp", "Trendline Breakouts toggled -> $showTrendlineBreakouts")
        } else if (lower.contains("liquidity delta")) {
            showLiquidityDeltaProfiler = !showLiquidityDeltaProfiler
            android.util.Log.d("TradingApp", "LDP toggled -> $showLiquidityDeltaProfiler")
        } else if (lower.contains("auto fib")) {
            autoFibEnabled = !autoFibEnabled
            if (autoFibEnabled) autoFibVisible = true
            persistAutoFib()
        } else if (lower.contains("confluence")) {
            confluenceFvgEnabled = !confluenceFvgEnabled
            if (confluenceFvgEnabled) confluenceFvgVisible = true
            persistConfluence()
        } else when (lower) {
            "rsi" -> showRsi = !showRsi
            "ema", "exponential moving average" -> {
                if (!showEma10) showEma10 = true
                else if (!showEma20) showEma20 = true
                else { showEma10 = false; showEma20 = false }
            }
            "sma", "simple moving average" -> {
                if (!showSma1) showSma1 = true
                else if (!showSma2) showSma2 = true
                else { showSma1 = false; showSma2 = false }
            }
            "vwap" -> showVwap = !showVwap
            "bb", "bollinger bands" -> showBb = !showBb
            "macd" -> showMacd = !showMacd
            "vol", "volume" -> showVolume = !showVolume
            "atr" -> showAtr = !showAtr
        }
        // debug: confirm indicator toggle
        try {
            android.util.Log.d("TradingApp", "Indicator toggled: $id -> rsi:$showRsi ema10:$showEma10 ema20:$showEma20 sma1:$showSma1 sma2:$showSma2 vwap:$showVwap bb:$showBb vol:$showVolume atr:$showAtr premiumDiscount:$showPremiumDiscount fvg:$showFairValueGap supplyDemand:$showSupplyDemandDaily ote:$showOteVisibleChart liquidDelta:$showLiquidityDeltaProfiler eqhEql:$showEqhEqlLiquidityZones autoFib:$autoFibEnabled confluenceFvg:$confluenceFvgEnabled")
            android.widget.Toast.makeText(context, "Toggled: $id", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { /* ignore in non-UI tests */ }
    }

    val appBackgroundColor = when (chartSettings.canvas.fullChartColor) {
        "Pure Black" -> Color.Black
        "Dark Blue" -> Color(0xFF131722)
        else -> parseComposeColor(chartSettings.canvas.background)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showQuickActions = false
                showIndicatorModal = false
                showChartSettingsBottomSheet = false
                selectedIndicatorId = null
            },
        color = appBackgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(modifier = Modifier.weight(1f)) {
                    if (!isFullscreen && isSidebarVisible) {
                        Sidebar(
                            activeTool = activeTool,
                            onToolClick = { activeTool = it },
                            onToolSearchClick = { showToolSearchModal = true },
                            stayInDrawingMode = stayInDrawingMode,
                            onStayInModeToggle = { stayInDrawingMode = !stayInDrawingMode },
                            isMagnetEnabled = isMagnetEnabled,
                            onMagnetToggle = { isMagnetEnabled = !isMagnetEnabled },
                            isLocked = isLocked,
                            onLockToggle = { isLocked = !isLocked },
                            isVisible = areDrawingsVisible,
                            onVisibilityToggle = { areDrawingsVisible = !areDrawingsVisible },
                            onClearDrawings = { drawings.clear() },
                            backgroundColor = appBackgroundColor,
                            settings = chartSettings
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f)) {
                            val renderProviderChart: @Composable (ChartFeedType, ProviderChartData) -> Unit = { resolvedChartFeedType, providerChartData ->
TradingChart2(
                symbol = symbol,
                timeframe = timeframe,
                style = chartStyle,
                chartSettings = chartSettings,
                drawings = drawings,
                onDrawingUpdate = { drawing ->
                    val index = drawings.indexOfFirst { it.id == drawing.id }
                    if (index != -1) drawings[index] = drawing else drawings.add(drawing)
                },
                onAlertPriceUpdate = { drawingId, newPrice ->
                    userAlerts.value = userAlerts.value.map { 
                        if (it.drawingId == drawingId) it.copy(price = newPrice) else it 
                    }
                },
                activeTool = activeTool,
                onToolReset = { if (!stayInDrawingMode) activeTool = "cursor" },
                userAlerts = userAlerts.value,
                onAlertTriggered = { triggered ->
                    val now = System.currentTimeMillis()
                    userAlerts.value = userAlerts.value.map { a ->
                        if (a.id == triggered.id) {
                            if (triggered.triggerMode == "Once only") a.copy(isActive = false, triggeredAt = now)
                            else a.copy(lastTriggeredAt = now)
                        } else a
                    }
                },
                                showRsi = showRsi,
                                rsiPeriod = rsiPeriod,
                                onRsiToggle = { showRsi = it },
                                hiddenIndicators = hiddenIndicators,
                                onIndicatorHide = { id -> hiddenIndicators = if (id in hiddenIndicators) hiddenIndicators - id else hiddenIndicators + id },
                                rsiShowLabels = rsiShowLabels,
                                rsiShowLines = rsiShowLines,
                                showEma10 = showEma10,
                                ema10Period = ema10Period,
                                onEma10Toggle = { showEma10 = it },
                                ema10ShowLabels = ema10ShowLabels,
                                ema10ShowLines = ema10ShowLines,
                                showEma20 = showEma20,
                                ema20Period = ema20Period,
                                onEma20Toggle = { showEma20 = it },
                                ema20ShowLabels = ema20ShowLabels,
                                ema20ShowLines = ema20ShowLines,
                                showSma1 = showSma1,
                                sma1Period = sma1Period,
                                onSma1Toggle = { showSma1 = it },
                                sma1ShowLabels = sma1ShowLabels,
                                sma1ShowLines = sma1ShowLines,
                                showSma2 = showSma2,
                                sma2Period = sma2Period,
                                onSma2Toggle = { showSma2 = it },
                                sma2ShowLabels = sma2ShowLabels,
                                sma2ShowLines = sma2ShowLines,
                                showVwap = showVwap,
                                onVwapToggle = { showVwap = it },
                                vwapShowLabels = vwapShowLabels,
                                vwapShowLines = vwapShowLines,
                                showBb = showBb,
                                bbPeriod = bbPeriod,
                                onBbToggle = { showBb = it },
                                bbShowLabels = bbShowLabels,
                                bbShowLines = bbShowLines,
                                bbStdDev = bbStdDev,
                                showAtr = showAtr,
                                atrPeriod = atrPeriod,
                                onAtrToggle = { showAtr = it },
                                atrShowLabels = atrShowLabels,
                                atrShowLines = atrShowLines,
                                showMacd = showMacd,
                                macdFast = macdFast,
                                macdSlow = macdSlow,
                                macdSignal = macdSignal,
                                onMacdToggle = { showMacd = it },
                                macdShowLabels = macdShowLabels,
                                macdShowLines = macdShowLines,
                                selectedIndicatorId = selectedIndicatorId,
                                onSelectedIndicatorIdChange = { selectedIndicatorId = it },
                                showVolume = showVolume,
                                volumeShowLabels = volumeShowLabels,
                                volumeShowLines = volumeShowLines,
                                showPremiumDiscount = showPremiumDiscount,
                                onPremiumDiscountToggle = { showPremiumDiscount = it },
                                showFairValueGap = showFairValueGap,
                                onFairValueGapToggle = { showFairValueGap = it },
                                fvgSettings = fvgSettings,
                                onFvgSettingsClick = { showFvgSettingsModal = true },
                                showSupplyDemandDaily = showSupplyDemandDaily,
                                onSupplyDemandDailyToggle = { showSupplyDemandDaily = it },
                                showOteVisibleChart = showOteVisibleChart,
                                onOteVisibleChartToggle = { showOteVisibleChart = it },
                                showLiquidityDeltaProfiler = showLiquidityDeltaProfiler,
                                ldpSettings = ldpSettings,
                                onLdpSettingsClick = { showLdpSettingsModal = true },
                                onLiquidityDeltaProfilerToggle = { showLiquidityDeltaProfiler = it },
                                showEqhEqlLiquidityZones = showEqhEqlLiquidityZones,
                                eqhEqlSettings = eqhEqlSettings,
                                onEqhEqlSettingsClick = { showEqhEqlSettingsModal = true },
                                onEqhEqlLiquidityZonesToggle = { showEqhEqlLiquidityZones = it },
                                showPowerHourBreakout = showPowerHourBreakout,
                                powerHourSettings = powerHourSettings,
                                onPowerHourSettingsClick = { showPowerHourSettingsModal = true },
                                onPowerHourBreakoutToggle = { showPowerHourBreakout = it },
                                showTrendlineBreakouts = showTrendlineBreakouts,
                                trendlineSettings = trendlineSettings,
                                onTrendlineSettingsClick = { showTrendlineSettingsModal = true },
                                onTrendlineBreakoutsToggle = { showTrendlineBreakouts = it },
                                showTrendlineNavigator = showTrendlineNavigator,
                                navigatorSettings = navigatorSettings,
                                onNavigatorSettingsClick = { showNavigatorSettingsModal = true },
                                onTrendlineNavigatorToggle = { showTrendlineNavigator = it },
                                showLiquidityPools = showLiquidityPools,
                                liquidityPoolsSettings = liquidityPoolsSettings,
                                onLiquidityPoolsSettingsClick = { showLiquidityPoolsSettingsModal = true },
                                onLiquidityPoolsToggle = { showLiquidityPools = it },
                                showOrderBlockBreaker = showOrderBlockBreaker,
                                obbSettings = obbSettings,
                                onObbSettingsClick = { showObbSettingsModal = true },
                                onOrderBlockBreakerToggle = { showOrderBlockBreaker = it },
                                showVolumaticFvg = showVolumaticFvg,
                                volumaticFvgSettings = volumaticFvgSettings,
                                onVolumaticFvgSettingsClick = { showVolumaticFvgSettingsModal = true },
                                onVolumaticFvgToggle = { showVolumaticFvg = it },
                                showAutoFib = autoFibEnabled && autoFibVisible,
                                autoFibEnabled = autoFibEnabled,
                                onAutoFibToggle = { autoFibEnabled = it; if (!it) autoFibVisible = true; persistAutoFib() },
                                onAutoFibHide = { autoFibVisible = it; persistAutoFib() },
                                autoFibSettings = autoFibSettings,
                                onAutoFibSettingsChange = {
                                    autoFibSettings = it
                                    saveAutoFibSettings(autoFibPrefs, it)
                                },
                                onAutoFibSettingsClick = { showAutoFibSettingsModal = true },
                                sdVrSettings = sdVrSettings,
                                onSdVrSettingsClick = { showSdVrSettingsModal = true },
                                cfvgSettings = cfvgSettings,
                                onCfvgSettingsClick = { showCfvgSettingsModal = true },
                                showConfluenceFvg = confluenceFvgEnabled && confluenceFvgVisible,
                                confluenceFvgEnabled = confluenceFvgEnabled,
                                onConfluenceFvgToggle = { confluenceFvgEnabled = it; if (!it) confluenceFvgVisible = true; persistConfluence() },
                                onConfluenceFvgHide = { confluenceFvgVisible = it; persistConfluence() },
                                volumeColorBasedOnPreviousClose = volumeColorBasedOnPreviousClose,
                                onVolumeToggle = { showVolume = it },
                                onIndicatorSettingsClick = { showIndicatorSettingsModal = it },
                                chartFeedType = resolvedChartFeedType,
                                providerChartData = providerChartData,
                                isMagnetEnabled = isMagnetEnabled,
                                isLocked = isLocked,
                                isVisible = areDrawingsVisible,
                                isTradingBarVisible = showFloatingTradingButtons,
                                isCrosshairActive = isCrosshairActive,
                                onCrosshairToggle = { isCrosshairActive = it },
                                selectedCurrency = selectedCurrency,
                                onCurrencyClick = { showCurrencyModal = true },
                                isFullscreen = isFullscreen,
                                onFullscreenExit = { isFullscreen = false },
                                scrollToTimestamp = targetTimestamp,
                                onScrollDone = { targetTimestamp = null },
                                onLongPress = { showChartSettingsBottomSheet = true },
                                onSettingsClick = { showChartSettingsBottomSheet = true },
                                onDataLoaded = { candles ->
                                    orderModalChartData = candles
                                },
                                selectedTimeZone = selectedTz.label,
                                onQuoteUpdate = { quote ->
                                    currentLiveQuote = quote
                                    recentPairQuotes[recentPairQuoteKey(symbol, timeframe)] = quote
                                    val quoteUpperKey = quote.name.uppercase(Locale.US)
                                    if (quote.lastPrice > 0f || symbolQuotesByTicker[quoteUpperKey] == null) {
                                        symbolQuotesByTicker[quoteUpperKey] = quote
                                    }
                                },
                                onAnyQuoteUpdate = { quote ->
                                    val quoteUpperKey = quote.name.uppercase(Locale.US)
                                    if (quote.lastPrice > 0f || symbolQuotesByTicker[quoteUpperKey] == null) {
                                        symbolQuotesByTicker[quoteUpperKey] = quote
                                    }
                                    val matchingPairs = recentPairs.filter { (pairSymbol, _) ->
                                        pairSymbol.equals(quote.name, ignoreCase = true)
                                    }
                                    matchingPairs.forEach { (pairSymbol, pairTimeframe) ->
                                        recentPairQuotes[recentPairQuoteKey(pairSymbol, pairTimeframe)] = quote
                                    }
                                },
                                onSymbolsUpdate = { symbols ->
                                    val mergedQuotes = mergeQuoteCatalog(symbols, chartFeedQuoteCatalog)
                                    availableQuotes.clear()
                                    availableQuotes.addAll(mergedQuotes)
                                },
                                watchlistSymbols = watchlistSymbols,
                                positions = (positions + localPositions).distinctBy { it.id },
                                onPositionUpdate = { updated ->
                                    if (updated.id.startsWith("temp_")) {
                                        val idx = localPositions.indexOfFirst { it.id == updated.id }
                                        if (idx != -1) localPositions[idx] = updated else localPositions.add(updated)
                                    } else {
                                        val idx = positions.indexOfFirst { it.id == updated.id }
                                        if (idx != -1) positions[idx] = updated else positions.add(updated)
                                    }
                                },
                                onPositionDelete = { id ->
                                    val pos = positions.find { it.id == id } ?: localPositions.find { it.id == id }
                                    pos?.let(::closeStreamPosition)
                                },
                                onAccountUpdate = { mt5AccountInfo = it },
                                onPlaceOrder = { position, orderType, stopLimitPrice ->
                                    placeStreamOrder(position, orderType, stopLimitPrice)
                                },
                                onPositionsUpdate = { newPositions ->
                                    positions.clear()
                                    positions.addAll(newPositions)
                                    
                                    val serverIds = newPositions.map { it.id }.toSet()
                                    val symbolPositions = newPositions.filter { it.symbol.equals(symbol, ignoreCase = true) }
                                    
                                    val toRemove = mutableListOf<Position>()
                                    localPositions.forEach { local ->
                                        if (local.symbol.equals(symbol, ignoreCase = true)) {
                                            // Check if any server position matches this local one (roughly)
                                            val match = symbolPositions.find { 
                                                it.type == local.type && 
                                                Math.abs(it.volume - local.volume) < 0.001 
                                            }
                                            if (match != null) {
                                                toRemove.add(local)
                                            } else {
                                                // Optional: Remove if it's too old (e.g., > 10 seconds)
                                                if (System.currentTimeMillis() - local.time > 10000) {
                                                    toRemove.add(local)
                                                }
                                            }
                                        }
                                    }
                                    localPositions.removeAll(toRemove)
                                },
                                orders = orders,
                                onOrdersUpdate = { newOrders ->
                                    orders.clear()
                                    orders.addAll(newOrders)
                                },
                                onHistoryOrdersUpdate = { newHistory ->
                                    orderHistory.clear()
                                    orderHistory.addAll(newHistory)
                                },
                                onBalanceHistoryUpdate = { newBalanceHistory ->
                                    balanceHistory.clear()
                                    balanceHistory.addAll(newBalanceHistory)
                                },
                                onCalendarUpdate = { payload ->
                                    scope.launch {
                                        val aiJson = gson.toJson(payload.ai)
                                        calendarDisplayPayload = payload.display
                                        calendarAiPayloadJson = aiJson
                                        calendarSelectedDateIso = payload.display.selectedDateIso
                                        isCalendarLoading = false

                                        CalendarSnapshotStore.latestDisplayPayload = payload.display
                                        CalendarSnapshotStore.latestAiPayload = payload.ai
                                        CalendarSnapshotStore.latestAiPayloadJson = aiJson

                                        sharedPrefs.edit()
                                            .putString("calendar_display_payload", gson.toJson(payload.display))
                                            .putString("calendar_ai_payload", aiJson)
                                            .apply()

                                        // Auto-sync calendar events to AI backend
                                        try {
                                            forexViewModel.autoSyncCalendarEvents()
                                        } catch (e: Exception) {
                                            android.util.Log.w("TradingApp", "Failed to auto-sync calendar: ${e.message}")
                                        }
                                    }
                                },
                                isCalendarVisible = showCalendarPage,
                                calendarRequestDateIso = calendarSelectedDateIso,
                                calendarRequestVersion = calendarRequestVersion,
                                isNewsVisible = showNewsPage,
                                onNewsUpdate = { payload ->
                                    android.util.Log.d("TradingApp", "Received news update: ${payload.items.size} items")
                                    Mt5NewsStore.updateNews(payload.items)
                                    newsItems.clear()
                                    newsItems.addAll(payload.items)
                                    isNewsLoading = false
                                    val newsJson = gson.toJson(payload)
                                    NewsSnapshotStore.latestPayload = payload
                                    NewsSnapshotStore.latestAiPayloadJson = newsJson
                                    persistNewsAiPayload(context, sharedPrefs, newsJson)
                                },
                                reverseBridge = reverseBridge,
                                onTradeNotification = { tradeNotifications.add(it) },
                                onIndicatorDataUpdate = { currentIndicatorData = it }
                            )
                            }

                            TradingChartExness(
                                symbol = symbol,
                                timeframe = timeframe,
                                reverseBridge = reverseBridge,
                                isCalendarVisible = showCalendarPage,
                                calendarRequestDateIso = calendarSelectedDateIso,
                                calendarRequestVersion = calendarRequestVersion,
                                isNewsVisible = showNewsPage,
                                onAccountUpdate = { mt5AccountInfo = it },
                                onPositionsUpdate = { newPositions ->
                                    positions.clear()
                                    positions.addAll(newPositions)
                                },
                                onOrdersUpdate = { newOrders ->
                                    orders.clear()
                                    orders.addAll(newOrders)
                                },
                                onHistoryOrdersUpdate = { newHistory ->
                                    orderHistory.clear()
                                    orderHistory.addAll(newHistory)
                                },
                                onBalanceHistoryUpdate = { newBalanceHistory ->
                                    balanceHistory.clear()
                                    balanceHistory.addAll(newBalanceHistory)
                                },
                                onCalendarUpdate = { payload ->
                                    scope.launch {
                                        val aiJson = gson.toJson(payload.ai)
                                        calendarDisplayPayload = payload.display
                                        calendarAiPayloadJson = aiJson
                                        calendarSelectedDateIso = payload.display.selectedDateIso
                                        isCalendarLoading = false
                                        CalendarSnapshotStore.latestDisplayPayload = payload.display
                                        CalendarSnapshotStore.latestAiPayload = payload.ai
                                        CalendarSnapshotStore.latestAiPayloadJson = aiJson
                                        sharedPrefs.edit()
                                            .putString("calendar_display_payload", gson.toJson(payload.display))
                                            .putString("calendar_ai_payload", aiJson)
                                            .apply()

                                        // Auto-sync calendar events to AI backend
                                        try {
                                            forexViewModel.autoSyncCalendarEvents()
                                        } catch (e: Exception) {
                                            android.util.Log.w("TradingApp", "Failed to auto-sync calendar: ${e.message}")
                                        }
                                    }
                                },
                                onNewsUpdate = { payload ->
                                    android.util.Log.d("TradingApp", "Received news update: ${payload.items.size} items")
                                    Mt5NewsStore.updateNews(payload.items)
                                    newsItems.clear()
                                    newsItems.addAll(payload.items)
                                    isNewsLoading = false
                                    val newsJson = gson.toJson(payload)
                                    NewsSnapshotStore.latestPayload = payload
                                    NewsSnapshotStore.latestAiPayloadJson = newsJson
                                    persistNewsAiPayload(context, sharedPrefs, newsJson)
                                },
                                onSymbolsUpdate = { symbols ->
                                    val mergedQuotes = mergeQuoteCatalog(symbols, chartFeedQuoteCatalog)
                                    availableQuotes.clear()
                                    availableQuotes.addAll(mergedQuotes)
                                }
                            ) { resolvedChartFeedType, providerChartData ->
                                renderProviderChart(resolvedChartFeedType, providerChartData)
                            }

                            if (!isConnected) {
                                ConnectingToServerOverlay(
                                    backgroundColor = appBackgroundColor
                                )
                            }

                            AutoTradeChartBadge(
                                symbol = symbol,
                                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                            )
                        }

                        if (!isFullscreen && isBottomPanelVisible) {
                            TradingPanel(
                                activeTab = activeTab,
                                onTabChange = { activeTab = it },
                                analysisContent = analysisContent,
                                isAnalyzing = isAnalyzing,
                                onRefreshAnalysis = { refreshAnalysis() },
                                onClose = { isBottomPanelVisible = false },
                                backgroundColor = appBackgroundColor,
                                positions = (positions + localPositions).distinctBy { it.id },
                                quotePriceForSymbol = { sym ->
                                    val upperKey = sym.uppercase(Locale.US)
                                    symbolQuotesByTicker[upperKey]?.lastPrice
                                        ?: symbolQuotesByTicker[brokerSymbolForTicker(sym).uppercase(Locale.US)]?.lastPrice
                                        ?: (positions + localPositions)
                                            .firstOrNull { it.symbol.equals(sym, ignoreCase = true) }
                                            ?.entryPrice
                                },
                                onPositionClick = { pos ->
                                    selectedPositionToModify =
                                        positions.firstOrNull { it.id == pos.id }
                                            ?: localPositions.firstOrNull { it.id == pos.id }
                                            ?: pos
                                    showPositionActionsModal = true
                                }
                            )
                        }
                    }
                }

                if (!isFullscreen) {
                    val isHeaderHidden =
                        chartSettings.scales.hideHeaderPane ||
                            !chartSettings.canvas.headerVisible ||
                            (chartSettings.canvas.headerVisibility == "Auto-hide" && !isSidebarVisible)
                    
                    val renderHeader = @Composable {
                        AnimatedVisibility(
                            visible = !isHeaderHidden,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Header(
                                symbol = symbol,
                                timeframe = timeframe,
                                chartStyle = chartStyle,
                                onSymbolClick = { showQuotes = true },
                                onTimeframeClick = { timeframe = it },
                                onStyleChange = { chartStyle = it },
                                onIndicatorClick = { showIndicatorModal = true },
                                onSettingsClick = { showChartSettingsBottomSheet = true },
                                onAnalysisClick = { refreshAnalysis() },
                                onUndo = { /* Undo logic */ },
                                onRedo = { /* Redo logic */ },
                                canUndo = history.isNotEmpty(),
                                canRedo = redoStack.isNotEmpty(),
                                onToolSearchClick = { showToolSearchModal = true },
                                onRightPanelToggle = { },
                                isRightPanelVisible = false,
                                onDownloadChart = { showCaptureModal = true },
                                backgroundColor = appBackgroundColor,
                                settings = chartSettings,
                                isAtBottom = true,
                                onGoToClick = { showGoToDateModal = true },
                                onChatClick = { /* activeTab = "Chat"; isBottomPanelVisible = true */ },
                                onDrawingClick = { showDrawingsModal = true },
                                onMoreClick = { showAnalysisHubModal = true },
                                onTradeClick = { 
                                    if (chartSettings.trading.oneClickTrading) {
                                        showFloatingTradingButtons = !showFloatingTradingButtons
                                    } else {
                                        orderModalInitialSide = "buy"
                                        orderModalShowMarketSideButtons = true
                                        showOrderModal = true
                                    }
                                },
                                onCurrencyClick = {
                                    showPaperTradingPanel = true
                                }
                            )
                        }
                    }

                    val renderBottomBar = @Composable {
                        if (isTimezonePaneVisible) {
                            // Filter recent pairs to only show those valid for the current chart feed
                            val currentFeedSymbols = availableQuotes.map { it.ticker.uppercase(Locale.US) }.toSet()
                            val filteredRecentPairs = recentPairs.filter { (symbol, _) ->
                                symbol.uppercase(Locale.US) in currentFeedSymbols
                            }

                            BottomBar(
                                onRangeClick = { handleRangeChange(it) },
                                onGoToClick = { showGoToDateModal = true },
                                onTabClick = {
                                    if (activeTab == it && isBottomPanelVisible) {
                                        isBottomPanelVisible = false
                                    } else {
                                        activeTab = it
                                        isBottomPanelVisible = true
                                    }
                                },
                                activeTab = if (isBottomPanelVisible) activeTab else null,
                                recentPairs = if (chartSettings.scales.hideAssetLastViewedPane) emptyList() else filteredRecentPairs,
                                currentSymbol = symbol,
                                currentTimeframe = timeframe,
                                onPairSelect = { s: String, t: String ->
                                    symbol = s
                                    timeframe = t
                                },
                                backgroundColor = appBackgroundColor,
                                settings = chartSettings,
                                currentQuote = currentLiveQuote,
                                recentPairQuotes = recentPairQuotes,
                                availableQuotes = availableQuotes,
                                onAccountUpdate = { mt5AccountInfo = it },
                                onVisibleSymbolsChanged = { symbols: List<String> ->
                                    visibleRecentSymbols.clear()
                                    if (isTimezonePaneVisible) {
                                        visibleRecentSymbols.addAll(
                                            symbols
                                                .asSequence()
                                                .map(::brokerSymbolForTicker)
                                                .filter { it.isNotEmpty() }
                                                .distinctBy { it.uppercase(Locale.US) }
                                                .toList()
                                        )
                                    }
                                }
                            )
                        }
                    }

                    if (chartSettings.canvas.swapHeaderAndFooter) {
                        Divider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Color(0xFF2A2E39))
                        renderHeader()
                        Divider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Color(0xFF2A2E39))
                        renderBottomBar()
                    } else {
                        Divider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Color(0xFF2A2E39))
                        renderBottomBar()
                        Divider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Color(0xFF2A2E39))
                        renderHeader()
                    }
                }
            }

            // Live Trade Panel Overlay
            /* PaperTradingPanel REMOVED */
            BackHandler(enabled = showPaperTradingPanel) { showPaperTradingPanel = false }
            if (showPaperTradingPanel) {
                val paperQuoteResolver: (String) -> Float? = { positionSymbol ->
                    liveQuoteSymbolKeys(positionSymbol)
                        .firstNotNullOfOrNull { key ->
                            symbolQuoteSnapshot[key]?.takeIf { isFreshLiveQuote(it) }?.lastPrice
                        }
                }
                PaperTradingPanel(
                    onClose = {
                        if (onPaperTradingClose != null) {
                            onPaperTradingClose()
                        } else {
                            showPaperTradingPanel = false
                        }
                    },
                    onPositionClick = { pos ->
                        selectedPositionToModify = pos
                        showPositionActionsModal = true
                    },
                    positions = positions,
                    selectedPositionId = selectedPositionToModify?.id,
                    orders = orders,
                    orderHistory = orderHistory,
                    balanceHistory = balanceHistory,
                    currentPrice = currentLiveQuote?.lastPrice ?: 0f,
                    quotePriceForSymbol = paperQuoteResolver,
                    providerLabel = "Exness",
                    accountInfo = mt5AccountInfo,
                    sourceName = liveTradeSourceName(),
                    accountLabel = liveTradeAccountLabel,
                    isBrokerConnected = isConnected || mt5AccountInfo != null,
                    backgroundColor = appBackgroundColor,
                    onMarketTypeChange = null,
                    onAccountChange = null,
                    onRefresh = {
                        mt5Service.disconnect()
                        mt5Service.connect()
                    }
                )
            }

            if (showPositionActionsModal && selectedPositionToModify != null) {
                val pos = selectedPositionToModify!!
                PositionActionsModal(
                    position = pos,
                    lastPrice = currentLiveQuote?.lastPrice ?: pos.entryPrice,
                    onClose = { showPositionActionsModal = false },
                    onModify = { 
                        showPositionActionsModal = false
                        showModifyModal = true 
                    },
                    onClosePosition = {
                        val p = positions.find { it.id == pos.id } ?: localPositions.find { it.id == pos.id }
                        p?.let(::closeStreamPosition)
                        showPositionActionsModal = false
                    },
                    onNewOrder = {
                        symbol = pos.symbol
                        orderModalInitialSide = if (pos.type.equals("sell", ignoreCase = true)) "sell" else "buy"
                        showPositionActionsModal = false
                        showSimpleOrderPage = true
                    },
                    onViewChart = {
                        // Switch symbol and close panel to see chart
                        symbol = pos.symbol
                        showPositionActionsModal = false
                    }
                )
            }

            if (showModifyModal && selectedPositionToModify != null) {
                val pos = selectedPositionToModify!!
                ModifyTpSlModal(
                    symbol = pos.symbol,
                    qty = pos.volume.toString(),
                    entryPrice = pos.entryPrice,
                    lastTradedPrice = currentLiveQuote?.lastPrice ?: pos.entryPrice,
                    isBuy = pos.type.equals("buy", ignoreCase = true),
                    initialTp = pos.tp,
                    initialSl = pos.sl,
                    initialPartialOrders = pos.partialOrders,
                    currentPrice = currentLiveQuote?.lastPrice ?: 0f,
                    onConfirm = { tp, sl, partials ->
                        val oldTp = pos.tp
                        val oldSl = pos.sl
                        val updatedPos = pos.copy(tp = tp, sl = sl, partialOrders = partials)
                        modifyStreamPosition(updatedPos, tp, sl)

                        if (tp != null && tp != oldTp) {
                            tradeNotifications.add(
                                TradeNotification(
                                    symbol = pos.symbol,
                                    volume = pos.volume,
                                    price = tp,
                                    isBuy = pos.type.equals("buy", ignoreCase = true),
                                    type = "tp_placed"
                                )
                            )
                        }
                        if (sl != null && sl != oldSl) {
                            tradeNotifications.add(
                                TradeNotification(
                                    symbol = pos.symbol,
                                    volume = pos.volume,
                                    price = sl,
                                    isBuy = pos.type.equals("buy", ignoreCase = true),
                                    type = "sl_placed"
                                )
                            )
                        }
                        
                        showModifyModal = false
                        selectedPositionToModify = null
                    },
                    onCancel = { 
                        showModifyModal = false
                        selectedPositionToModify = null 
                    }
                )
            }

            // Backdrop for Quick Actions
            if (showQuickActions && chartSettings.scales.plusButton && !showNewsPage && !showCalendarPage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showQuickActions = false }
                        )
                )
            }

            if (showNewsPage) {
                NewsPage(
                    newsItems = newsItems,
                    isLoading = isNewsLoading && newsItems.isEmpty(),
                    onBack = {
                        showNewsPage = false
                        isNewsLoading = false
                    }
                )
            }
        }

        // Modals (Symbol Search, Currency, Indicators, Settings, Tool Search, Alert, Capture, TimeZone)
        if (showQuotes) {
            Quotes(
                onClose = { showQuotes = false },
                quotes = availableQuotes,
                onQuoteSelect = {
                    symbol = chartFeedSymbolFor(streamFeedType, it.ticker)
                },
                quotesByTicker = symbolQuotesByTicker,
                onVisibleSymbolsChanged = { symbols ->
                    visibleQuoteSymbols.clear()
                    visibleQuoteSymbols.addAll(
                        symbols
                            .asSequence()
                            .map(::brokerSymbolForTicker)
                            .filter { it.isNotEmpty() }
                            .distinctBy { it.uppercase(Locale.US) }
                            .toList()
                    )
                }
            )
        }
        if (showCurrencyModal) {
            CurrencySelectionModal(
                currentSymbol = symbol,
                selectedCurrency = selectedCurrency,
                onCurrencySelect = { selectedCurrency = it },
                onClose = { showCurrencyModal = false }
            )
        }
        if (showIndicatorModal) {
            IndicatorsModal(
                onClose = { showIndicatorModal = false },
                onIndicatorSelect = { handleIndicatorSelect(it) }
            )
        }
        if (showAutoFibSettingsModal) {
            AutoFibSettingsModal(
                settings = autoFibSettings,
                onChange = {
                    autoFibSettings = it
                    saveAutoFibSettings(autoFibPrefs, it)
                },
                onDismiss = { showAutoFibSettingsModal = false }
            )
        }
        if (showSdVrSettingsModal) {
            SupplyDemandVrSettingsModal(
                settings = sdVrSettings,
                onChange = {
                    sdVrSettings = it
                    autoFibPrefs.edit().putString("sdVrSettings", it.toJson()).apply()
                },
                onDismiss = { showSdVrSettingsModal = false }
            )
        }
        if (showCfvgSettingsModal) {
            ConfluenceFvgSettingsModal(
                settings = cfvgSettings,
                onChange = {
                    cfvgSettings = it
                    autoFibPrefs.edit().putString("cfvgSettings", it.toJson()).apply()
                },
                onDismiss = { showCfvgSettingsModal = false }
            )
        }
        if (showFvgSettingsModal) {
            FairValueGapSettingsModal(
                settings = fvgSettings,
                onChange = {
                    fvgSettings = it
                    autoFibPrefs.edit().putString("fvgSettings", it.toJson()).apply()
                },
                onDismiss = { showFvgSettingsModal = false }
            )
        }
        if (showLdpSettingsModal) {
            LiquidityDeltaProfilerSettingsModal(
                settings = ldpSettings,
                onChange = {
                    ldpSettings = it
                    autoFibPrefs.edit().putString("ldpSettings", it.toJson()).apply()
                },
                onDismiss = { showLdpSettingsModal = false }
            )
        }
        if (showEqhEqlSettingsModal) {
            EqhEqlLiquidityZonesSettingsModal(
                settings = eqhEqlSettings,
                onChange = {
                    eqhEqlSettings = it
                    autoFibPrefs.edit().putString("eqhEqlSettings", it.toJson()).apply()
                },
                onDismiss = { showEqhEqlSettingsModal = false }
            )
        }
        if (showPowerHourSettingsModal) {
            PowerHourBreakoutSettingsModal(
                settings = powerHourSettings,
                onChange = {
                    powerHourSettings = it
                    autoFibPrefs.edit().putString("powerHourSettings", it.toJson()).apply()
                },
                onDismiss = { showPowerHourSettingsModal = false }
            )
        }
        if (showTrendlineSettingsModal) {
            TrendlineBreakoutsSettingsModal(
                settings = trendlineSettings,
                onChange = {
                    trendlineSettings = it
                    autoFibPrefs.edit().putString("trendlineSettings", it.toJson()).apply()
                },
                onDismiss = { showTrendlineSettingsModal = false }
            )
        }
        if (showNavigatorSettingsModal) {
            TrendlineNavigatorSettingsModal(
                settings = navigatorSettings,
                onChange = {
                    navigatorSettings = it
                    autoFibPrefs.edit().putString("navigatorSettings", it.toJson()).apply()
                },
                onDismiss = { showNavigatorSettingsModal = false }
            )
        }
        if (showLiquidityPoolsSettingsModal) {
            LiquidityPoolsSettingsModal(
                settings = liquidityPoolsSettings,
                onChange = {
                    liquidityPoolsSettings = it
                    autoFibPrefs.edit().putString("liquidityPoolsSettings", it.toJson()).apply()
                },
                onDismiss = { showLiquidityPoolsSettingsModal = false }
            )
        }
        if (showObbSettingsModal) {
            OrderBlockBreakerSettingsModal(
                settings = obbSettings,
                onChange = {
                    obbSettings = it
                    autoFibPrefs.edit().putString("orderBlockBreakerSettings", it.toJson()).apply()
                },
                onDismiss = { showObbSettingsModal = false }
            )
        }
        if (showVolumaticFvgSettingsModal) {
            VolumaticFvgSettingsModal(
                settings = volumaticFvgSettings,
                onSettingsChange = { it2 ->
                    volumaticFvgSettings = it2
                    autoFibPrefs.edit().putString("volumaticFvgSettings", it2.toJson()).apply()
                },
                onDismiss = { showVolumaticFvgSettingsModal = false },
                onReset = {
                    volumaticFvgSettings = com.trading.app.indicators.VolumaticFvgSettings()
                    autoFibPrefs.edit().remove("volumaticFvgSettings").apply()
                }
            )
        }
        if (showGoToDateModal) {
            GoToDateModal(
                onClose = { showGoToDateModal = false },
                onGoTo = { timestamp ->
                    targetTimestamp = timestamp
                    showGoToDateModal = false
                }
            )
        }
        if (showSettingsModal) {
            SettingsModal(
                settings = chartSettings,
                initialTab = settingsInitialTab,
                onUpdate = {
                    try {
                        chartSettings = it
                        val newTz = timeZones.find { tz -> tz.label == it.symbol.timezone }
                        if (newTz != null) selectedTz = newTz
                    } catch (e: Exception) {
                        android.util.Log.e("TradingApp", "Failed applying settings", e)
                    }
                },
                onTimeZoneClick = { showTimeZoneModal = true },
                onClose = { 
                    showSettingsModal = false
                    settingsInitialTab = null
                }
            )
        }
        if (showChartSettingsBottomSheet) {
            ChartSettingsBottomSheet(
                settings = chartSettings,
                onUpdate = { chartSettings = it },
                onDismissRequest = { showChartSettingsBottomSheet = false },
                onMoreSettingsClick = {
                    showChartSettingsBottomSheet = false
                    showSettingsModal = true
                }
            )
        }
        if (showToolSearchModal) {
            ToolSearchModal(
                onToolSelect = { activeTool = it },
                onClose = { showToolSearchModal = false }
            )
        }
        if (showAlertModal) {
            AlertModal(
                symbol = symbol,
                currentPrice = currentLiveQuote?.lastPrice,
                alerts = userAlerts.value,
                onAlertCreate = { userAlerts.value = userAlerts.value + it },
                onAlertUpdate = { updated -> userAlerts.value = userAlerts.value.map { if (it.id == updated.id) updated else it } },
                onAlertDelete = { id -> userAlerts.value = userAlerts.value.filterNot { it.id == id } },
                onDrawingCreate = { drawings.add(it) },
                onClose = { showAlertModal = false }
            )
        }
        if (showCaptureModal) {
            ChartCaptureModal(
                onClose = { showCaptureModal = false },
                onDownload = { },
                onShare = { }
            )
        }
        if (showDrawingsModal) {
            DrawingsModal(
                onClose = { showDrawingsModal = false },
                onToolSelect = { activeTool = it }
            )
        }
        if (showAnalysisHubModal) {
            AnalysisHubModal(
                onClose = { showAnalysisHubModal = false },
                onIndicatorClick = { showIndicatorModal = true; showAnalysisHubModal = false },
                onAlertClick = { showAlertModal = true; showAnalysisHubModal = false },
                hasAlerts = userAlerts.value.isNotEmpty(),
                onCalendarClick = {
                    showCalendarPage = true
                    showAnalysisHubModal = false
                    isCalendarLoading = calendarDisplayPayload == null
                    calendarRequestVersion += 1
                },
                onChartTypeClick = { 
                    showChartTypeModal = true
                    showAnalysisHubModal = false
                },
                onNewsClick = {
                    isNewsLoading = newsItems.isEmpty()
                    showNewsPage = true
                    showAnalysisHubModal = false
                }
            )
        }
        if (showChartTypeModal) {
            ChartTypeModal(
                currentStyle = chartStyle,
                onStyleChange = { chartStyle = it },
                onClose = { showChartTypeModal = false }
            )
        }
        if (showOrderModal) {
            OrderModal(
                symbol = symbol,
                bidPrice = currentLiveQuote?.bid ?: 0f,
                askPrice = currentLiveQuote?.ask ?: 0f,
                priceChange = currentLiveQuote?.change ?: 0f,
                chartData = orderModalChartData,
                onClose = { showOrderModal = false },
                onPlaceOrder = { position, orderType, stopLimitPrice ->
                    placeStreamOrder(position, orderType, stopLimitPrice)
                },
                onTradingSettingsClick = {
                    showOrderModal = false
                    settingsInitialTab = "Trading"
                    showSettingsModal = true
                },
                showMarketSideButtons = orderModalShowMarketSideButtons,
                initialSide = orderModalInitialSide
            )
        }
        if (showSimpleOrderPage) {
            SimpleOrderPage(
                symbol = symbol,
                bidPrice = currentLiveQuote?.bid ?: 0f,
                askPrice = currentLiveQuote?.ask ?: 0f,
                priceChange = currentLiveQuote?.change ?: 0f,
                onClose = { showSimpleOrderPage = false },
                onPlaceOrder = { position, orderType, stopLimitPrice ->
                    placeStreamOrder(position, orderType, stopLimitPrice)
                },
                initialSide = orderModalInitialSide
            )
        }
        showIndicatorSettingsModal?.let { indicatorId ->
            when (indicatorId) {
                "Volume" -> {
                    VolumeIndicatorSettingsModal(
                        maLength = volumeMaLength,
                        onMaLengthChange = { volumeMaLength = it },
                        showMa = showVolumeMa,
                        onShowMaChange = { showVolumeMa = it },
                        colorBasedOnPreviousClose = volumeColorBasedOnPreviousClose,
                        onColorBasedOnPreviousCloseChange = { volumeColorBasedOnPreviousClose = it },
                        maColor = volumeMaColor,
                        onMaColorChange = { volumeMaColor = it },
                        growingColor = volumeGrowingColor,
                        onGrowingColorChange = { volumeGrowingColor = it },
                        fallingColor = volumeFallingColor,
                        onFallingColorChange = { volumeFallingColor = it },
                        showLabels = volumeShowLabels,
                        onShowLabelsChange = { volumeShowLabels = it },
                        showLines = volumeShowLines,
                        onShowLinesChange = { volumeShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "RSI" -> {
                    RSIIndicatorSettingsModal(
                        period = rsiPeriod,
                        onPeriodChange = { rsiPeriod = it },
                        rsiColor = rsiColor,
                        onRsiColorChange = { rsiColor = it },
                        showLabels = rsiShowLabels,
                        onShowLabelsChange = { rsiShowLabels = it },
                        showLines = rsiShowLines,
                        onShowLinesChange = { rsiShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "EMA 10" -> {
                    EMAIndicatorSettingsModal(
                        indicatorId = "EMA 10",
                        period = ema10Period,
                        onPeriodChange = { ema10Period = it },
                        lineColor = ema10Color,
                        onLineColorChange = { ema10Color = it },
                        showLabels = ema10ShowLabels,
                        onShowLabelsChange = { ema10ShowLabels = it },
                        showLines = ema10ShowLines,
                        onShowLinesChange = { ema10ShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "EMA 20" -> {
                    EMAIndicatorSettingsModal(
                        indicatorId = "EMA 20",
                        period = ema20Period,
                        onPeriodChange = { ema20Period = it },
                        lineColor = ema20Color,
                        onLineColorChange = { ema20Color = it },
                        showLabels = ema20ShowLabels,
                        onShowLabelsChange = { ema20ShowLabels = it },
                        showLines = ema20ShowLines,
                        onShowLinesChange = { ema20ShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "SMA 1" -> {
                    SMAIndicatorSettingsModal(
                        indicatorId = "SMA 21",
                        period = sma1Period,
                        onPeriodChange = { sma1Period = it },
                        lineColor = sma1Color,
                        onLineColorChange = { sma1Color = it },
                        showLabels = sma1ShowLabels,
                        onShowLabelsChange = { sma1ShowLabels = it },
                        showLines = sma1ShowLines,
                        onShowLinesChange = { sma1ShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "SMA 2" -> {
                    SMAIndicatorSettingsModal(
                        indicatorId = "SMA 10",
                        period = sma2Period,
                        onPeriodChange = { sma2Period = it },
                        lineColor = sma2Color,
                        onLineColorChange = { sma2Color = it },
                        showLabels = sma2ShowLabels,
                        onShowLabelsChange = { sma2ShowLabels = it },
                        showLines = sma2ShowLines,
                        onShowLinesChange = { sma2ShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "VWAP" -> {
                    VWAPIndicatorSettingsModal(
                        showLabels = vwapShowLabels,
                        onShowLabelsChange = { vwapShowLabels = it },
                        showLines = vwapShowLines,
                        onShowLinesChange = { vwapShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "BB" -> {
                    BBIndicatorSettingsModal(
                        period = bbPeriod,
                        onPeriodChange = { bbPeriod = it },
                        stdDev = bbStdDev,
                        onStdDevChange = { bbStdDev = it },
                        lineColor = bbColor,
                        onLineColorChange = { bbColor = it },
                        showLabels = bbShowLabels,
                        onShowLabelsChange = { bbShowLabels = it },
                        showLines = bbShowLines,
                        onShowLinesChange = { bbShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "ATR" -> {
                    ATRIndicatorSettingsModal(
                        period = atrPeriod,
                        onPeriodChange = { atrPeriod = it },
                        lineColor = atrColor,
                        onLineColorChange = { atrColor = it },
                        showLabels = atrShowLabels,
                        onShowLabelsChange = { atrShowLabels = it },
                        showLines = atrShowLines,
                        onShowLinesChange = { atrShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
                "MACD" -> {
                    MACDIndicatorSettingsModal(
                        fastPeriod = macdFast,
                        slowPeriod = macdSlow,
                        signalPeriod = macdSignal,
                        onParamsChange = { f, s, sig ->
                            macdFast = f
                            macdSlow = s
                            macdSignal = sig
                        },
                        macdColor = macdColor,
                        signalColor = macdSignalColor,
                        onMacdColorChange = { macdColor = it },
                        onSignalColorChange = { macdSignalColor = it },
                        showLabels = macdShowLabels,
                        onShowLabelsChange = { macdShowLabels = it },
                        showLines = macdShowLines,
                        onShowLinesChange = { macdShowLines = it },
                        onClose = { showIndicatorSettingsModal = null }
                    )
                }
            }
        }
        if (showTimeZoneModal) {
            TimeZoneSelectionModal(
                timeZones = timeZones,
                selectedTimeZone = selectedTz,
                onTimeZoneSelect = {
                    selectedTz = it
                    chartSettings = chartSettings.copy(
                        symbol = chartSettings.symbol.copy(timezone = it.label)
                    )
                    showTimeZoneModal = false
                },
                onClose = { showTimeZoneModal = false }
            )
        }
    }
}

private fun saveAutoFibSettings(prefs: android.content.SharedPreferences, s: com.trading.app.indicators.AutoFibSettings) {
    try {
        val levels = org.json.JSONArray()
        for (l in s.levels) {
            levels.put(org.json.JSONObject().put("s", l.shown).put("r", l.ratio.toDouble()).put("c", l.colorInt))
        }
        val json = org.json.JSONObject()
            .put("dev", s.deviation.toDouble())
            .put("depth", s.depth)
            .put("rev", s.reverse)
            .put("extL", s.extendLeft)
            .put("extR", s.extendRight)
            .put("prices", s.showPrices)
            .put("levels", s.showLevels)
            .put("fmtValues", s.levelsFormatValues)
            .put("lblLeft", s.labelsPositionLeft)
            .put("bgT", s.backgroundTransparency)
            .put("lvls", levels)
        prefs.edit().putString("autoFibSettings", json.toString()).apply()
    } catch (_: Exception) { }
}

private fun loadAutoFibSettings(prefs: android.content.SharedPreferences): com.trading.app.indicators.AutoFibSettings {
    val raw = prefs.getString("autoFibSettings", null) ?: return com.trading.app.indicators.AutoFibSettings()
    return try {
        val o = org.json.JSONObject(raw)
        val levelsArr = o.optJSONArray("lvls")
        val defaults = com.trading.app.indicators.AutoFibSettings.defaultLevels()
        val levels = if (levelsArr != null && levelsArr.length() == defaults.size) {
            (0 until levelsArr.length()).map { i ->
                val l = levelsArr.getJSONObject(i)
                com.trading.app.indicators.AutoFibLevelState(l.getBoolean("s"), l.getDouble("r").toFloat(), l.getInt("c"))
            }
        } else defaults
        AutoFibSettingsCompat(
            dev = o.getDouble("dev").toFloat(),
            depth = o.getInt("depth"),
            rev = o.getBoolean("rev"),
            extL = o.getBoolean("extL"),
            extR = o.getBoolean("extR"),
            prices = o.getBoolean("prices"),
            levelsOn = o.getBoolean("levels"),
            fmtValues = o.getBoolean("fmtValues"),
            lblLeft = o.getBoolean("lblLeft"),
            bgT = o.getInt("bgT"),
            lvls = levels
        ).toSettings()
    } catch (_: Exception) {
        com.trading.app.indicators.AutoFibSettings()
    }
}

private class AutoFibSettingsCompat(
    val dev: Float, val depth: Int, val rev: Boolean,
    val extL: Boolean, val extR: Boolean,
    val prices: Boolean, val levelsOn: Boolean,
    val fmtValues: Boolean, val lblLeft: Boolean,
    val bgT: Int, val lvls: List<com.trading.app.indicators.AutoFibLevelState>
) {
    fun toSettings() = com.trading.app.indicators.AutoFibSettings(
        deviation = dev, depth = depth, reverse = rev,
        extendLeft = extL, extendRight = extR,
        showPrices = prices, showLevels = levelsOn,
        levelsFormatValues = fmtValues, labelsPositionLeft = lblLeft,
        backgroundTransparency = bgT, levels = lvls
    )
}
