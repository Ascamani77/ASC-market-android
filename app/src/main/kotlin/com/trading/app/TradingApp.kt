package com.trading.app

import android.content.Context
import android.util.Log
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
import com.trading.app.components.*
import com.trading.app.models.*
import com.trading.app.data.CalendarSnapshotStore
import com.trading.app.data.NewsSnapshotStore
import com.trading.app.data.Mt5NewsStore
import com.trading.app.data.Mt5Service
import com.trading.app.data.Mt5ReverseBridge
import com.trading.app.data.BinanceTradingMode
import com.trading.app.data.BinanceMarketType
import com.trading.app.data.BinanceConnectService
import com.trading.app.data.PaperTradingAccountSnapshot
import com.trading.app.data.PaperTradingSnapshotStore
import com.trading.app.data.BinanceService
import com.trading.app.data.BinanceTradingService
import com.trading.app.data.BinanceFuturesService
import com.trading.app.data.BinanceTradingService.Balance
import com.trading.app.data.CTraderService
import com.trading.app.data.ChartFeedType
import com.trading.app.data.PepperstoneChartService
import com.trading.app.data.chartFeedQuotes
import com.trading.app.data.chartFeedSymbolFor
import com.asc.markets.data.NetworkConfig
import com.asc.markets.logic.PriceStreamManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

private const val LIVE_QUOTE_MAX_AGE_MS = 15_000L

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

@Composable
fun TradingApp(
    startInPaperTradingPanel: Boolean = false,
    onPaperTradingClose: (() -> Unit)? = null,
    streamFeedType: ChartFeedType = ChartFeedType.PEPPERSTONE_CTRADER,
    stateNamespace: String = "stream_${streamFeedType.prefValue}"
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("trading_prefs", Context.MODE_PRIVATE) }
    val networkPrefs = remember { context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawingInsets = WindowInsets.safeDrawing

    val streamStateNamespace = remember(stateNamespace, streamFeedType) {
        stateNamespace.ifBlank { "stream_${streamFeedType.prefValue}" }
    }
    fun streamScopedKey(base: String): String = "${base}_${streamStateNamespace}"

    val chartFeedType = streamFeedType
    var binanceTradingMode by remember {
        mutableStateOf(BinanceTradingMode.current(context))
    }
    val chartFeedQuoteCatalog = remember(chartFeedType) { chartFeedQuotes(chartFeedType) }
    val defaultStreamSymbol = remember(chartFeedType) {
        chartFeedQuoteCatalog.firstOrNull()?.ticker ?: "EURUSD"
    }

    DisposableEffect(networkPrefs, context) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == BinanceTradingMode.PREF_KEY) {
                binanceTradingMode = BinanceTradingMode.current(context)
            }
        }
        networkPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            networkPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Core State
    var symbol by remember(streamStateNamespace, chartFeedType) {
        mutableStateOf(
            sharedPrefs.getString(streamScopedKey("selected_symbol"), defaultStreamSymbol)
                ?.let { chartFeedSymbolFor(chartFeedType, it) }
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
        mutableStateOf(
            sharedPrefs.getString(streamScopedKey("chart_settings"), null)?.let {
                try { 
                    gson.fromJson(it, ChartSettings::class.java)
                } catch (e: Exception) { ChartSettings() }
            } ?: ChartSettings()
        )
    }

    var isLocked by remember { mutableStateOf(chartSettings.quickActions.isLocked) }
    var areDrawingsVisible by remember { mutableStateOf(true) }
    var isCrosshairActive by remember { mutableStateOf(false) }
    var isReplayActive by remember { mutableStateOf(false) }

    // Currency State
    var selectedCurrency by remember { mutableStateOf("USD") }
    var showCurrencyModal by remember { mutableStateOf(false) }
    var showPaperTradingPanel by remember { mutableStateOf(startInPaperTradingPanel) }

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

    LaunchedEffect(chartFeedType) {
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

    fun liveTradeSourceName(): String = when (chartFeedType) {
        ChartFeedType.BINANCE -> if (binanceTradingMode == BinanceTradingMode.DEMO) "Binance Demo Trade" else "Binance Live Trade"
        ChartFeedType.BINANCE_CONNECT -> "Binance Connect (View Only)"
        ChartFeedType.EXNESS -> "Exness Live Trade"
        ChartFeedType.PEPPERSTONE_CTRADER -> "Pepperstone cTrader Live"
    }

    fun liveTradeDefaultAccountLabel(): String = when (chartFeedType) {
        ChartFeedType.BINANCE -> if (binanceTradingMode == BinanceTradingMode.DEMO) "Binance Futures Demo" else "Binance Futures Live"
        ChartFeedType.BINANCE_CONNECT -> "Binance Connect (No Trading)"
        ChartFeedType.EXNESS -> "Exness MT5"
        ChartFeedType.PEPPERSTONE_CTRADER -> "Pepperstone cTrader"
    }

    fun binanceTradingSymbol(symbol: String): String {
        val normalized = chartFeedSymbolFor(ChartFeedType.BINANCE, symbol).trim().uppercase(Locale.US)
        return normalized.replace("/", "").replace("-", "").replace("_", "")
    }

    fun cacheSelectedSourceQuote(quote: SymbolQuote) {
        val incomingName = quote.name.trim()
        val normalizedName = chartFeedSymbolFor(chartFeedType, incomingName)
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
            .forEach { key -> symbolQuotesByTicker[key.uppercase(Locale.US)] = keyedQuote }
        PriceStreamManager.updatePrice(catalogItem.ticker, keyedQuote.lastPrice.toDouble())
        if (catalogItem.ticker.equals(symbol, ignoreCase = true) || catalogItem.brokerSymbol.equals(symbol, ignoreCase = true)) {
            currentLiveQuote = keyedQuote
        }
    }

    val visibleQuoteSymbols = remember { mutableStateListOf<String>() }
    val visibleRecentSymbols = remember { mutableStateListOf<String>() }
    val mt5Host = remember { NetworkConfig.mt5Host(context) }
    val mt5Port = remember { NetworkConfig.mt5Port(context) }
    val cTraderHost = remember { NetworkConfig.cTraderHost(context) }
    val cTraderPort = remember { NetworkConfig.cTraderPort(context) }

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
                
                // Route MT5 updates to CombinedFallbackDataStore
                val pair = com.asc.markets.data.FOREX_PAIRS.find { it.symbol == quote.name }
                if (pair != null) {
                    val updatedPair = pair.copy(
                        price = quote.lastPrice.toDouble(),
                        change = quote.lastPrice.toDouble() - pair.price,
                        changePercent = if (pair.price > 0) ((quote.lastPrice.toDouble() - pair.price) / pair.price * 100) else 0.0
                    )
                    com.asc.markets.data.CombinedFallbackDataStore.updatePair(updatedPair)
                }
            },
            onConnectionStatusUpdate = { isConnected = it }
        )
    }

    val pepperstoneQuoteService = remember {
        val prefs = context.getSharedPreferences("asc_prefs", android.content.Context.MODE_PRIVATE)
        val redisHost = prefs.getString("redis_host", "10.164.138.133") ?: "10.164.138.133"
        val redisPort = prefs.getInt("redis_port", 6379)
        
        PepperstoneChartService(
            host = cTraderHost,
            port = cTraderPort,
            redisHost = redisHost,
            redisPort = redisPort,
            publishToRedis = true,
            onQuoteUpdate = { quote ->
                if (chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER) {
                    cacheSelectedSourceQuote(quote)
                }
            },
            onHistoryUpdate = { _, _ -> }
        )
    }

    val binanceQuoteService = remember(binanceTradingMode) {
        BinanceService(
            tradingMode = binanceTradingMode,
            marketType = BinanceMarketType.FUTURES,
            onQuoteUpdate = { quote ->
                if (chartFeedType == ChartFeedType.BINANCE) {
                    cacheSelectedSourceQuote(quote)
                }
            },
            onHistoryUpdate = { _, _ -> }
        )
    }

    val binanceConnectQuoteService = remember {
        BinanceConnectService(
            onQuoteUpdate = { quote ->
                if (chartFeedType == ChartFeedType.BINANCE_CONNECT) {
                    cacheSelectedSourceQuote(quote)
                }
            },
            onHistoryUpdate = { _, _ -> }
        )
    }

    val binanceTradingService = remember(binanceTradingMode) { BinanceTradingService(binanceTradingMode) }
    val binanceFuturesService = remember(binanceTradingMode) { BinanceFuturesService(binanceTradingMode) }

    DisposableEffect(Unit) {
        onDispose {
            pepperstoneQuoteService.disconnect()
            binanceQuoteService.disconnect()
            binanceConnectQuoteService.disconnect()
        }
    }

    val watchlistSymbols by remember {
        derivedStateOf {
            (visibleQuoteSymbols + visibleRecentSymbols)
                .map { it.uppercase(Locale.US) }
                .distinct()
        }
    }

    LaunchedEffect(chartFeedType, watchlistSymbols, chartFeedQuoteCatalog, timeframe) {
        val sourceSymbols = sourceQuoteSymbols()
        when (chartFeedType) {
            ChartFeedType.EXNESS -> {
                pepperstoneQuoteService.stopActiveStream()
                binanceQuoteService.stopActiveStream()
                binanceConnectQuoteService.stopActiveStream()
                mt5Service.updateWatchlist(watchlistSymbols.ifEmpty { sourceSymbols })
            }
            ChartFeedType.PEPPERSTONE_CTRADER -> {
                binanceQuoteService.stopActiveStream()
                binanceConnectQuoteService.stopActiveStream()
                pepperstoneQuoteService.subscribeSymbols(sourceSymbols, timeframe)
            }
            ChartFeedType.BINANCE -> {
                pepperstoneQuoteService.stopActiveStream()
                binanceConnectQuoteService.stopActiveStream()
                Log.d("BinanceService", "Subscribing to Binance symbols: ${sourceSymbols.joinToString(", ")}")
                binanceQuoteService.subscribeSymbols(sourceSymbols)
            }
            ChartFeedType.BINANCE_CONNECT -> {
                pepperstoneQuoteService.stopActiveStream()
                binanceQuoteService.stopActiveStream()
                Log.d("BinanceConnect", "Subscribing Binance Connect quote symbols: ${sourceSymbols.joinToString(", ")}")
                binanceConnectQuoteService.subscribeSymbols(sourceSymbols)
            }
        }
    }

    LaunchedEffect(symbol, timeframe) {
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

        val normalizedSymbol = symbol.trim()
        currentLiveQuote = symbolQuotesByTicker[normalizedSymbol.uppercase(Locale.US)]?.takeIf { isFreshLiveQuote(it) }
    }

    // Settings & Data
    val drawings = remember { mutableStateListOf<Drawing>() }
    val history = remember { mutableStateListOf<ChartSnapshot>() }
    val redoStack = remember { mutableStateListOf<ChartSnapshot>() }
    val userAlerts = remember { mutableStateOf(emptyList<UserAlert>()) }
    val positions = remember { mutableStateListOf<Position>() }
    val localPositions = remember { mutableStateListOf<Position>() }
    val orders = remember { mutableStateListOf<Order>() }
    val orderHistory = remember { mutableStateListOf<Order>() }
    val balanceHistory = remember { mutableStateListOf<BalanceRecord>() }
    var mt5AccountInfo by remember { mutableStateOf<Mt5Service.AccountInfo?>(null) }
    var liveTradeAccountLabel by remember(chartFeedType, binanceTradingMode) { mutableStateOf(liveTradeDefaultAccountLabel()) }
    var liveTradeRefreshToken by remember(streamStateNamespace) { mutableIntStateOf(0) }
    val cTraderTradingService = remember {
        CTraderService(
            onQuoteUpdate = { quote ->
                if (chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER) {
                    cacheSelectedSourceQuote(quote)
                }
            },
            onPositionsUpdate = { updatedPositions ->
                Log.d("TradingApp", "cTrader positions update received: ${updatedPositions.size} positions")
                updatedPositions.forEachIndexed { index, pos ->
                    Log.d("TradingApp", "Position $index: id=${pos.id}, symbol=${pos.symbol}, type=${pos.type}, volume=${pos.volume}")
                }
                positions.clear()
                positions.addAll(updatedPositions)
                Log.d("TradingApp", "Positions list updated, new size: ${positions.size}")
            },
            onAccountUpdate = { accountInfo ->
                Log.d("TradingApp", "cTrader account update received: balance=${accountInfo?.balance}, equity=${accountInfo?.equity}, margin=${accountInfo?.margin}")
                mt5AccountInfo = accountInfo
                Log.d("TradingApp", "mt5AccountInfo updated to: $mt5AccountInfo")
            },
            onBalanceHistoryUpdate = { newBalanceHistory ->
                balanceHistory.clear()
                balanceHistory.addAll(newBalanceHistory)
            },
            onConnectionStatusUpdate = { connected ->
                if (chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER) {
                    isConnected = connected
                }
            }
        )
    }

    LaunchedEffect(positions.toList(), localPositions.toList(), orders.toList(), chartFeedType) {
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

    val paperTradingSnapshot by remember(
        positions.toList(),
        localPositions.toList(),
        orders.toList(),
        balanceHistory.toList(),
        currentLiveQuote,
        symbolQuoteSnapshot,
        mt5AccountInfo,
        isConnected,
        symbol
    ) {
        derivedStateOf {
            val paperPositions = (positions + localPositions).distinctBy { it.id }
            fun quoteForSymbol(symbolName: String) = liveQuoteSymbolKeys(symbolName).firstNotNullOfOrNull { key ->
                symbolQuoteSnapshot[key]
            } ?: currentLiveQuote?.takeIf { quote ->
                val quoteKeys = liveQuoteSymbolKeys(quote.name)
                liveQuoteSymbolKeys(symbolName).any { key -> key in quoteKeys } ||
                    liveQuoteSymbolKeys(symbol).any { key -> key in quoteKeys }
                }

            val currentPosition = paperPositions.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
                ?: paperPositions.maxByOrNull { it.time }
            val currentTradeQuote = currentPosition?.let { quoteForSymbol(it.symbol) }
            val calculatedFloatingPnl = paperPositions.sumOf { position ->
                val livePrice = quoteForSymbol(position.symbol)?.lastPrice ?: return@sumOf 0.0
                ((livePrice - position.entryPrice) * position.volume * (if (position.type == "buy") 1f else -1f)).toDouble()
            }
            val accountInfo = mt5AccountInfo
            val floatingPnl = if (chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER && paperPositions.isNotEmpty()) {
                calculatedFloatingPnl
            } else {
                accountInfo?.unrealizedPnl ?: calculatedFloatingPnl
            }
            val latestBalance = balanceHistory.maxByOrNull { it.time }?.balanceAfter
            val hasLiveAccountData = accountInfo != null || latestBalance != null
            val balance = accountInfo?.balance ?: latestBalance ?: 0.0
            val equity = when {
                chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER && paperPositions.isNotEmpty() -> balance + calculatedFloatingPnl
                accountInfo?.equity != null -> accountInfo.equity
                hasLiveAccountData || paperPositions.isNotEmpty() -> balance + floatingPnl
                else -> 0.0
            }
            val margin = accountInfo?.margin ?: paperPositions.sumOf { (it.margin.takeIf { value -> value > 0f } ?: (it.entryPrice * it.volume * 0.01f)).toDouble() }
            val freeMargin = accountInfo?.availableFunds ?: (equity - margin)
            val ordersMargin = accountInfo?.ordersMargin ?: orders.sumOf { it.margin.toDouble() }
            val marginLevel = accountInfo?.marginBuffer ?: (if (equity > 0.0) (freeMargin / equity) * 100.0 else 100.0)
            val openRisk = paperPositions.sumOf { position ->
                val stopLoss = position.sl
                if (stopLoss != null) {
                    kotlin.math.abs(position.entryPrice - stopLoss).toDouble() * position.volume.toDouble()
                } else {
                    (position.margin.takeIf { it > 0f } ?: (position.entryPrice * position.volume * 0.01f)).toDouble()
                }
            }
            val currentTradePnl = currentPosition?.let { position ->
                val livePrice = currentTradeQuote?.lastPrice ?: return@let null
                ((livePrice - position.entryPrice) * position.volume * (if (position.type == "buy") 1f else -1f)).toDouble()
            }
            val currentTradePnlPct = currentPosition?.let { position ->
                val livePrice = currentTradeQuote?.lastPrice ?: return@let null
                val directionalMove = ((livePrice - position.entryPrice) / position.entryPrice) * (if (position.type == "buy") 1f else -1f)
                directionalMove.toDouble() * 100.0
            }
            PaperTradingAccountSnapshot(
                balance = balance,
                equity = equity,
                floatingPnl = floatingPnl,
                realizedPnl = mt5AccountInfo?.realizedPnl ?: balanceHistory.sumOf { it.realizedPnl },
                margin = margin,
                freeMargin = freeMargin,
                ordersMargin = ordersMargin,
                marginLevel = marginLevel,
                openRisk = openRisk,
                openRiskPct = if (equity > 0.0) (openRisk / equity) * 100.0 else 0.0,
                activeTrades = paperPositions.size,
                activeOrders = orders.size,
                balanceHistoryCount = balanceHistory.size,
                lastUpdatedMillis = System.currentTimeMillis(),
                isConnected = isConnected || mt5AccountInfo != null,
                hasLiveAccountData = hasLiveAccountData,
                hasLiveTradeData = currentPosition != null && currentTradeQuote != null,
                currentTradeSymbol = currentPosition?.symbol,
                currentTradeSide = currentPosition?.type,
                currentTradeEntryPrice = currentPosition?.entryPrice?.toDouble(),
                currentTradeVolume = currentPosition?.volume?.toDouble(),
                currentTradePrice = currentTradeQuote?.lastPrice?.toDouble(),
                currentTradePriceChange = currentTradeQuote?.change?.toDouble(),
                currentTradePriceChangePct = currentTradeQuote?.changePercent?.toDouble(),
                currentTradePnl = currentTradePnl,
                currentTradePnlPct = currentTradePnlPct,
                currentQuoteUpdatedMillis = currentTradeQuote?.time?.takeIf { it > 0L } ?: if (currentTradeQuote != null) System.currentTimeMillis() else 0L
            )
        }
    }

    LaunchedEffect(paperTradingSnapshot) {
        PaperTradingSnapshotStore.snapshot = paperTradingSnapshot
    }

    LaunchedEffect(chartFeedType, binanceTradingMode) {
        mt5AccountInfo = null
        positions.clear()
        localPositions.clear()
        orders.clear()
        orderHistory.clear()
        balanceHistory.clear()
        isConnected = false
        liveTradeAccountLabel = liveTradeDefaultAccountLabel()
        when (chartFeedType) {
            ChartFeedType.EXNESS -> {
                cTraderTradingService.disconnect()
                mt5Service.connect()
            }
            ChartFeedType.PEPPERSTONE_CTRADER -> {
                reverseBridge.disconnect()
                mt5Service.disconnect()
                cTraderTradingService.connect()
            }
            ChartFeedType.BINANCE -> {
                reverseBridge.disconnect()
                mt5Service.disconnect()
                cTraderTradingService.disconnect()
                isConnected = binanceTradingService.isConfigured()
            }
            ChartFeedType.BINANCE_CONNECT -> {
                // Binance Connect is view-only, no trading connections needed
                reverseBridge.disconnect()
                mt5Service.disconnect()
                cTraderTradingService.disconnect()
                Log.d("BinanceConnect", "View-only mode - no trading connections")
            }
        }
    }

    LaunchedEffect(chartFeedType, binanceTradingMode, showPaperTradingPanel, symbol, liveTradeRefreshToken) {
        Log.d("BinanceBalance", "LaunchedEffect triggered: chartFeedType=$chartFeedType, showPaperTradingPanel=$showPaperTradingPanel, binanceTradingMode=$binanceTradingMode")
        
        if (!showPaperTradingPanel) return@LaunchedEffect

        // Auto-refresh loop for Binance orders and positions
        if (chartFeedType == ChartFeedType.BINANCE) {
            // Setup User Data Stream callbacks for real-time position/account updates
            binanceFuturesService.onPositionUpdate = { updatedPositions ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    // Merge WebSocket position updates into local state
                    updatedPositions.forEach { pos ->
                        val existingIndex = positions.indexOfFirst { it.symbol == pos.symbol && it.positionSide == pos.positionSide }
                        val position = Position(
                            symbol = pos.symbol,
                            type = if (pos.positionAmt > 0) "buy" else "sell",
                            entryPrice = pos.entryPrice.toFloat(),
                            volume = kotlin.math.abs(pos.positionAmt).toFloat(),
                            time = System.currentTimeMillis(),
                            leverage = "${pos.leverage}x",
                            margin = pos.isolatedMargin.toFloat(),
                            positionSide = pos.positionSide
                        )
                        if (existingIndex >= 0) {
                            if (pos.positionAmt == 0.0) {
                                positions.removeAt(existingIndex)
                            } else {
                                positions[existingIndex] = position
                            }
                        } else if (pos.positionAmt != 0.0) {
                            positions.add(position)
                        }
                    }
                    Log.i("BinanceBalance", "Positions updated via WebSocket: ${positions.size} positions")
                }
            }
            binanceFuturesService.onAccountUpdate = { accountInfo ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    mt5AccountInfo = Mt5Service.AccountInfo(
                        balance = accountInfo.totalWalletBalance,
                        equity = accountInfo.totalMarginBalance,
                        unrealizedPnl = accountInfo.totalUnrealizedProfit,
                        realizedPnl = 0.0,
                        margin = accountInfo.totalMarginBalance - accountInfo.availableBalance,
                        availableFunds = accountInfo.availableBalance,
                        ordersMargin = 0.0,
                        marginBuffer = if (accountInfo.totalMarginBalance > 0) (accountInfo.availableBalance / accountInfo.totalMarginBalance) * 100.0 else 100.0
                    )
                }
            }
            scope.launch {
                val key = binanceFuturesService.startUserDataStream()
                if (key != null) {
                    binanceFuturesService.connectUserDataStream()
                    Log.i("BinanceBalance", "Binance User Data Stream connected")
                } else {
                    Log.e("BinanceBalance", "Failed to start Binance User Data Stream")
                }
            }

            while (showPaperTradingPanel) {
                Log.d("BinanceBalance", "Entering BINANCE branch (Futures)")

                if (binanceFuturesService.isRegionBlocked()) {
                    liveTradeAccountLabel = "Binance blocked in your region"
                    mt5AccountInfo = null
                    positions.clear()
                    orders.clear()
                    delay(3000)
                    continue
                }

                // ==================== FUTURES ====================
                if (!binanceFuturesService.isConfigured()) {
                    Log.d("BinanceBalance", "Binance Futures service NOT configured")
                    liveTradeAccountLabel = if (binanceTradingMode == BinanceTradingMode.DEMO) {
                        "Binance Futures demo API not configured"
                    } else {
                        "Binance Futures live API not configured"
                    }
                    mt5AccountInfo = null
                    positions.clear()
                    orders.clear()
                    return@LaunchedEffect
                }

                Log.d("BinanceBalance", "Binance Futures service IS configured, fetching account info...")
                liveTradeAccountLabel = if (binanceTradingMode == BinanceTradingMode.DEMO) "Binance Futures Demo" else "Binance Futures Live"

                runCatching {
                    val account = binanceFuturesService.getAccountInfo()
                    
                    Log.d("BinanceBalance", "=== Binance Futures Account ===")
                    Log.d("BinanceBalance", "Trading Mode: $binanceTradingMode")
                    Log.d("BinanceBalance", "Wallet Balance: ${account.totalWalletBalance}")
                    Log.d("BinanceBalance", "Unrealized PnL: ${account.totalUnrealizedProfit}")
                    Log.d("BinanceBalance", "Margin Balance: ${account.totalMarginBalance}")
                    Log.d("BinanceBalance", "Available Balance: ${account.availableBalance}")
                    Log.d("BinanceBalance", "Positions: ${account.positions.size}")
                    
                    mt5AccountInfo = Mt5Service.AccountInfo(
                        balance = account.totalWalletBalance,
                        equity = account.totalMarginBalance,
                        unrealizedPnl = account.totalUnrealizedProfit,
                        realizedPnl = 0.0,
                        margin = account.totalMarginBalance - account.availableBalance,
                        availableFunds = account.availableBalance,
                        ordersMargin = 0.0,
                        marginBuffer = if (account.totalMarginBalance > 0) (account.availableBalance / account.totalMarginBalance) * 100.0 else 100.0
                    )
                    
                    positions.clear()
                    positions.addAll(
                        account.positions.map { pos ->
                            val isLong = pos.positionAmt > 0
                            Log.d("BinanceBalance", "  ${pos.symbol}: ${if (isLong) "LONG" else "SHORT"} ${kotlin.math.abs(pos.positionAmt)} @ ${pos.entryPrice}, PnL: ${pos.unRealizedProfit}, Leverage: ${pos.leverage}x, PositionSide: ${pos.positionSide}")
                            Position(
                                symbol = pos.symbol,
                                type = if (isLong) "buy" else "sell",
                                entryPrice = pos.entryPrice.toFloat(),
                                volume = kotlin.math.abs(pos.positionAmt).toFloat(),
                                time = System.currentTimeMillis(),
                                leverage = "${pos.leverage}x",
                                margin = pos.isolatedMargin.toFloat(),
                                positionSide = pos.positionSide
                            )
                        }
                    )
                    Log.d("BinanceBalance", "Total positions: ${positions.size}")
                    Log.d("BinanceBalance", "================================")
                }.onFailure { error ->
                    Log.e("BinanceBalance", "Failed to fetch Futures account info: ${error.message}", error)
                    mt5AccountInfo = null
                    positions.clear()
                    liveTradeAccountLabel = if (error.message?.contains("region blocked") == true || error.message?.contains("451") == true) {
                        "Binance blocked in your region"
                    } else {
                        "Binance Futures unavailable"
                    }
                }

                runCatching {
                    binanceFuturesService.getOpenOrders()
                }.onSuccess { openOrders ->
                    Log.d("BinanceBalance", "Fetched ${openOrders.size} open Futures orders")
                    orders.clear()
                    orders.addAll(
                        openOrders.map { openOrder ->
                            Order(
                                id = openOrder.optLong("orderId", 0L).toString(),
                                symbol = openOrder.optString("symbol"),
                                type = openOrder.optString("side", "BUY").lowercase(Locale.US),
                                orderType = openOrder.optString("type", "LIMIT"),
                                status = openOrder.optString("status", "Working"),
                                price = openOrder.optDouble("price", 0.0).toFloat(),
                                volume = openOrder.optDouble("origQty", 0.0).toFloat(),
                                time = openOrder.optLong("time", System.currentTimeMillis()),
                                filledQuantity = openOrder.optDouble("executedQty", 0.0).toFloat(),
                                averagePrice = openOrder.optDouble("avgPrice", 0.0).toFloat()
                            )
                        }
                    )
                }.onFailure { error ->
                    Log.e("BinanceBalance", "Failed to fetch Futures orders: ${error.message}", error)
                    orders.clear()
                }
                
                // Wait 3 seconds before next refresh
                delay(3000)
            }
        } else {
            // Non-Binance feeds - fetch once
            when (chartFeedType) {
                ChartFeedType.EXNESS -> {
                    liveTradeAccountLabel = "Exness MT5"
                }
                ChartFeedType.PEPPERSTONE_CTRADER -> {
                    liveTradeAccountLabel = "Pepperstone cTrader"
                    cTraderTradingService.subscribe(brokerSymbolForTicker(symbol))
                }
                else -> {}
            }
        }
    }

    DisposableEffect(chartFeedType) {
        // Always disconnect old stream when mode changes; LaunchedEffect will reconnect
        onDispose {
            binanceFuturesService.disconnectUserDataStream()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            reverseBridge.disconnect()
            mt5Service.disconnect()
            cTraderTradingService.disconnect()
            binanceFuturesService.disconnectUserDataStream()
        }
    }

    fun placeStreamOrder(position: Position, orderType: String, stopLimitPrice: Float?) {
        when (chartFeedType) {
            ChartFeedType.EXNESS -> {
                if (orderType == "Market Execution") {
                    reverseBridge.placePosition(position)
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
                        symbol = position.symbol,
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
                    orders.add(order)
                }
            }
            ChartFeedType.BINANCE -> {
                scope.launch {
                    val tradeSymbol = binanceTradingSymbol(position.symbol)
                    
                    Log.d("TradingApp", "placeStreamOrder - BINANCE: symbol=$tradeSymbol, side=${position.type}, volume=${position.volume}, orderType=$orderType")
                    
                    // Determine positionSide for hedge mode support
                    // In hedge mode: BUY -> LONG, SELL -> SHORT
                    // In one-way mode: use BOTH (default)
                    val positionSide = when (position.type.uppercase()) {
                        "BUY" -> "LONG"
                        "SELL" -> "SHORT"
                        else -> "BOTH"
                    }
                    
                    // Futures order placement
                    runCatching {
                        when (orderType) {
                            "Market Execution" -> {
                                binanceFuturesService.placeMarketOrder(
                                    symbol = tradeSymbol,
                                    side = position.type.uppercase(),
                                    quantity = position.volume.toDouble(),
                                    positionSide = positionSide
                                )
                            }
                            "Buy Limit", "Sell Limit" -> {
                                binanceFuturesService.placeLimitOrder(
                                    symbol = tradeSymbol,
                                    side = position.type.uppercase(),
                                    quantity = position.volume.toDouble(),
                                    price = position.entryPrice.toDouble(),
                                    positionSide = positionSide
                                )
                            }
                            else -> {
                                binanceFuturesService.placeStopMarketOrder(
                                    symbol = tradeSymbol,
                                    side = position.type.uppercase(),
                                    quantity = position.volume.toDouble(),
                                    stopPrice = (stopLimitPrice ?: position.entryPrice).toDouble(),
                                    positionSide = positionSide
                                )
                            }
                        }
                    }.onSuccess { result ->
                        val hasExecutedQuantity = result.executedQty > 0.0

                        if (orderType == "Market Execution" && hasExecutedQuantity) {
                            val executedPosition = position.copy(
                                symbol = result.symbol,
                                entryPrice = if (result.avgPrice > 0.0) result.avgPrice.toFloat() else position.entryPrice,
                                volume = result.executedQty.toFloat(),
                                positionSide = positionSide
                            )
                            positions.add(executedPosition)
                            orderHistory.add(
                                Order(
                                    id = result.orderId.toString(),
                                    symbol = result.symbol,
                                    type = position.type,
                                    orderType = orderType,
                                    status = result.status,
                                    price = if (result.avgPrice > 0.0) result.avgPrice.toFloat() else position.entryPrice,
                                    volume = result.executedQty.toFloat(),
                                    time = System.currentTimeMillis(),
                                    filledQuantity = result.executedQty.toFloat(),
                                    averagePrice = result.avgPrice.toFloat()
                                )
                            )
                            tradeNotifications.add(
                                TradeNotification(
                                    symbol = result.symbol,
                                    volume = result.executedQty.toFloat(),
                                    price = if (result.avgPrice > 0.0) result.avgPrice.toFloat() else position.entryPrice,
                                    isBuy = position.type == "buy",
                                    type = "executed"
                                )
                            )
                        } else {
                            orders.add(
                                Order(
                                    id = result.orderId.toString(),
                                    symbol = result.symbol,
                                    type = position.type,
                                    orderType = orderType,
                                    status = result.status,
                                    price = position.entryPrice,
                                    stopLimitPrice = stopLimitPrice,
                                    volume = position.volume,
                                    time = System.currentTimeMillis(),
                                    filledQuantity = result.executedQty.toFloat(),
                                    averagePrice = result.avgPrice.toFloat(),
                                    tp = position.tp,
                                    sl = position.sl
                                )
                            )
                        }
                        liveTradeRefreshToken += 1
                    }
                }
            }
            ChartFeedType.PEPPERSTONE_CTRADER -> {
                if (orderType == "Market Execution") {
                    cTraderTradingService.placeMarketOrder(
                        symbol = brokerSymbolForTicker(position.symbol),
                        side = position.type,
                        volume = position.volume.toDouble(),
                        stopLoss = position.sl?.toDouble(),
                        takeProfit = position.tp?.toDouble()
                    ) { success, message ->
                        Log.d("TradingApp", "Pepperstone order result: success=$success message=$message")
                        if (success) {
                            tradeNotifications.add(
                                TradeNotification(
                                    symbol = position.symbol,
                                    volume = position.volume,
                                    price = position.entryPrice,
                                    isBuy = position.type == "buy",
                                    type = "executed",
                                    exchange = "Pepperstone"
                                )
                            )
                        }
                    }
                } else {
                    orders.add(
                        Order(
                            symbol = position.symbol,
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
                    )
                }
            }
            ChartFeedType.BINANCE_CONNECT -> {
                // View-only mode, no trading allowed
                Log.w("BinanceConnect", "Cannot place order - Binance Connect is view-only")
            }
        }
    }

    fun closeStreamPosition(position: Position) {
        when (chartFeedType) {
            ChartFeedType.EXNESS -> reverseBridge.closePosition(position)
            ChartFeedType.BINANCE -> {
                scope.launch {
                    runCatching {
                        // Use the positionSide from the position if available (from Binance API)
                        // Otherwise, determine it from the position type for backward compatibility
                        val positionSide = position.positionSide ?: when (position.type.uppercase()) {
                            "BUY" -> "LONG"
                            "SELL" -> "SHORT"
                            else -> "BOTH"
                        }
                        
                        // In hedge mode, reduceOnly is not needed (and may cause issues)
                        // The positionSide parameter already specifies which position to close
                        val useReduceOnly = positionSide == "BOTH"
                        
                        Log.d("TradingApp", "Closing Binance position: symbol=${position.symbol}, type=${position.type}, positionSide=$positionSide, volume=${position.volume}, reduceOnly=$useReduceOnly")
                        
                        binanceFuturesService.placeMarketOrder(
                            symbol = binanceTradingSymbol(position.symbol),
                            side = if (position.type.equals("buy", ignoreCase = true)) "SELL" else "BUY",
                            quantity = position.volume.toDouble(),
                            positionSide = positionSide,
                            reduceOnly = useReduceOnly
                        )
                    }.onSuccess { result ->
                        Log.d("TradingApp", "Successfully closed position: ${position.symbol}, orderId=${result.orderId}")
                        positions.removeAll { it.id == position.id }
                        localPositions.removeAll { it.id == position.id }
                        liveTradeRefreshToken += 1
                    }.onFailure { error ->
                        Log.e("TradingApp", "Failed to close position: ${position.symbol}, error=${error.message}", error)
                    }
                }
                return
            }
            ChartFeedType.PEPPERSTONE_CTRADER -> {
                cTraderTradingService.closePosition(position.id, position.volume.toDouble()) { success, message ->
                    Log.d("TradingApp", "Pepperstone close result for ${position.id}: success=$success message=$message")
                    if (success) {
                        scope.launch {
                            liveTradeRefreshToken += 1
                        }
                    }
                }
                return
            }
            ChartFeedType.BINANCE_CONNECT -> {
                // View-only mode, no trading allowed
                Log.w("BinanceConnect", "Cannot close position - Binance Connect is view-only")
                return
            }
        }
        positions.removeAll { it.id == position.id }
        localPositions.removeAll { it.id == position.id }
    }

    fun modifyStreamPosition(updatedPosition: Position, tp: Float?, sl: Float?) {
        if (chartFeedType == ChartFeedType.EXNESS) {
            reverseBridge.modifyPosition(updatedPosition, tp, sl)
        }
        val idxLocal = localPositions.indexOfFirst { it.id == updatedPosition.id }
        if (idxLocal != -1) localPositions[idxLocal] = updatedPosition
        val idxRemote = positions.indexOfFirst { it.id == updatedPosition.id }
        if (idxRemote != -1) positions[idxRemote] = updatedPosition
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
    var showVolume by remember { mutableStateOf(true) }
    var volumeShowLabels by remember { mutableStateOf(chartSettings.indicators.volumeShowLabels) }
    var volumeShowLines by remember { mutableStateOf(chartSettings.indicators.volumeShowLines) }
    var showVolumeMa by remember { mutableStateOf(false) }
    var volumeMaLength by remember { mutableIntStateOf(20) }
    var volumeMaColor by remember { mutableStateOf(Color(0xFF2196F3)) }
    var volumeGrowingColor by remember { mutableStateOf(Color(0xFF26A69A)) }
    var volumeFallingColor by remember { mutableStateOf(Color(0xFFEF5350)) }
    var volumeColorBasedOnPreviousClose by remember { mutableStateOf(false) }

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
        volumeShowLabels, volumeShowLines) {
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
                volumeShowLines = volumeShowLines
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
        when (id.trim().lowercase()) {
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
            android.util.Log.d("TradingApp", "Indicator toggled: $id -> rsi:$showRsi ema10:$showEma10 ema20:$showEma20 sma1:$showSma1 sma2:$showSma2 vwap:$showVwap bb:$showBb vol:$showVolume atr:$showAtr")
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
                            val renderProviderChart: @Composable (ChartFeedType, BinanceMarketType, ProviderChartData) -> Unit = { resolvedChartFeedType, resolvedBinanceMarketType, providerChartData ->
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
                                activeTool = activeTool,
                                onToolReset = { if (!stayInDrawingMode) activeTool = "cursor" },
                                showRsi = showRsi,
                                rsiPeriod = rsiPeriod,
                                onRsiToggle = { showRsi = it },
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
                                volumeColorBasedOnPreviousClose = volumeColorBasedOnPreviousClose,
                                onVolumeToggle = { showVolume = it },
                                onIndicatorSettingsClick = { showIndicatorSettingsModal = it },
                                chartFeedType = resolvedChartFeedType,
                                binanceMarketType = resolvedBinanceMarketType,
                                providerChartData = providerChartData,
                                isMagnetEnabled = isMagnetEnabled,
                                isLocked = isLocked,
                                isVisible = areDrawingsVisible,
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
                                    symbolQuotesByTicker[quote.name.uppercase(Locale.US)] = quote
                                },
                                onAnyQuoteUpdate = { quote ->
                                    symbolQuotesByTicker[quote.name.uppercase(Locale.US)] = quote
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
                                    
                                    // RECONCILIATION LOGIC:
                                    // We only remove local positions if they match a server position (volume/type)
                                    // or if we have information that the request failed.
                                    // For now, let's keep local positions for at least 5 seconds or until matched.
                                    
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
                                isTradingBarVisible = showFloatingTradingButtons,
                                reverseBridge = if (resolvedChartFeedType == ChartFeedType.EXNESS) reverseBridge else null,
                                cTraderService = if (resolvedChartFeedType == ChartFeedType.PEPPERSTONE_CTRADER) cTraderTradingService else null,
                                onTradeNotification = { tradeNotifications.add(it) },
                                onIndicatorDataUpdate = { currentIndicatorData = it }
                            )
                            }

                            when (chartFeedType) {
                                ChartFeedType.BINANCE -> {
                                    TradingChartBinance(
                                        symbol = symbol,
                                        timeframe = timeframe,
                                        tradingMode = binanceTradingMode,
                                        marketType = BinanceMarketType.FUTURES,
                                    ) { resolvedChartFeedType, resolvedBinanceMarketType, providerChartData ->
                                        renderProviderChart(resolvedChartFeedType, resolvedBinanceMarketType, providerChartData)
                                    }
                                }
                                ChartFeedType.BINANCE_CONNECT -> {
                                    TradingChartBinanceConnect(
                                        symbol = symbol,
                                        timeframe = timeframe
                                    ) { resolvedChartFeedType, resolvedBinanceMarketType, providerChartData ->
                                        renderProviderChart(resolvedChartFeedType, resolvedBinanceMarketType, providerChartData)
                                    }
                                }
                                ChartFeedType.PEPPERSTONE_CTRADER -> {
                                    TradingChartPepperstoneCTrader(
                                        symbol = symbol,
                                        timeframe = timeframe
                                    ) { resolvedChartFeedType, providerChartData ->
                                        renderProviderChart(resolvedChartFeedType, BinanceMarketType.FUTURES, providerChartData)
                                    }
                                }
                                ChartFeedType.EXNESS -> {
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
                                        renderProviderChart(resolvedChartFeedType, BinanceMarketType.FUTURES, providerChartData)
                                    }
                                }
                            }

                            if (chartFeedType == ChartFeedType.EXNESS && !isConnected) {
                                ConnectingToServerOverlay(
                                    backgroundColor = appBackgroundColor,
                                    onRetryBridge = {
                                        mt5Service.disconnect()
                                        mt5Service.connect()
                                        mt5Service.requestSymbols()
                                    }
                                )
                            }
                        }

                        if (!isFullscreen && isBottomPanelVisible) {
                            TradingPanel(
                                activeTab = activeTab,
                                onTabChange = { activeTab = it },
                                analysisContent = analysisContent,
                                isAnalyzing = isAnalyzing,
                                onRefreshAnalysis = { refreshAnalysis() },
                                onClose = { isBottomPanelVisible = false },
                                backgroundColor = appBackgroundColor
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
                                onCurrencyClick = { showPaperTradingPanel = true }
                            )
                        }
                    }

                    val renderBottomBar = @Composable {
                        if (isTimezonePaneVisible) {
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
                                recentPairs = if (chartSettings.scales.hideAssetLastViewedPane) emptyList() else recentPairs,
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
            if (showPaperTradingPanel) {
                val pepperstoneQuoteResolver: (String) -> Float? = { positionSymbol ->
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
                    quotePriceForSymbol = pepperstoneQuoteResolver,
                    preferSnapshotStats = chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER,
                    providerLabel = when (chartFeedType) {
                        ChartFeedType.PEPPERSTONE_CTRADER -> "Pepperstone cTrader"
                        ChartFeedType.EXNESS -> "Exness"
                        ChartFeedType.BINANCE -> "Binance"
                        ChartFeedType.BINANCE_CONNECT -> "Binance Connect"
                    },
                    accountInfo = mt5AccountInfo,
                    sourceName = liveTradeSourceName(),
                    accountLabel = liveTradeAccountLabel,
                    isBrokerConnected = isConnected || mt5AccountInfo != null,
                    backgroundColor = appBackgroundColor,
                    onMarketTypeChange = null,
                    currentMarketType = "futures"
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
                        showPaperTradingPanel = false
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

            // Floating Draggable Quick Actions Button
            if (chartSettings.scales.plusButton && !showNewsPage && !showCalendarPage) {
                QuickActionsButton(
                    onClick = { showQuickActions = !showQuickActions },
                    offset = quickActionsButtonOffset,
                    onOffsetChange = { quickActionsButtonOffset = it },
                    isLocked = isLocked,
                    isModalOpen = showQuickActions
                )
            }

            // Quick Actions Modal
            if (showQuickActions && chartSettings.scales.plusButton && !showNewsPage && !showCalendarPage) {
                QuickActionsModal(
                    isFullscreen = isFullscreen,
                    onFullscreenToggle = { isFullscreen = !isFullscreen },
                    isHeaderVisible = chartSettings.canvas.headerVisible,
                    onHeaderToggle = {
                        chartSettings = chartSettings.copy(
                            canvas = chartSettings.canvas.copy(
                                headerVisible = !chartSettings.canvas.headerVisible
                            )
                        )
                    },
                    isBottomMenuVisible = isBottomPanelVisible,
                    onBottomMenuToggle = { isBottomPanelVisible = !isBottomPanelVisible },
                    onSettingsClick = { showChartSettingsBottomSheet = true; showQuickActions = false },
                    onDrawingsClick = { isSidebarVisible = !isSidebarVisible; showQuickActions = false },
                    onChartTypeClick = { 
                        showChartTypeModal = true
                        showQuickActions = false
                    },
                    isTimezoneVisible = isTimezonePaneVisible,
                    onTimezoneToggle = { isTimezonePaneVisible = !isTimezonePaneVisible },
                    isCrosshairActive = isCrosshairActive,
                    onCrosshairToggle = { isCrosshairActive = !isCrosshairActive },
                    onAlertClick = { showAlertModal = true; showQuickActions = false },
                    onReplayClick = { isReplayActive = !isReplayActive; showQuickActions = false },
                    isReplayActive = isReplayActive,
                    isLocked = isLocked,
                    onLockToggle = { isLocked = !isLocked },
                    onClose = { showQuickActions = false },
                    offset = quickActionsModalOffset,
                    onOffsetChange = { quickActionsModalOffset = it }
                )
            }
        }

        // Modals (Symbol Search, Currency, Indicators, Settings, Tool Search, Alert, Capture, TimeZone)
        if (showQuotes) {
            Quotes(
                onClose = { showQuotes = false },
                quotes = availableQuotes,
                onQuoteSelect = {
                    symbol = chartFeedSymbolFor(chartFeedType, it)
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
                onAlertCreate = { userAlerts.value = userAlerts.value + it },
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
                    isNewsLoading = true
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
