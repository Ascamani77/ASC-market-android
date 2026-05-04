package com.asc.markets.ui.terminal.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.ui.terminal.models.*
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChartViewModel : ViewModel() {
    private val _activeSymbol = MutableStateFlow(
        MarketDataStore.pairSnapshot("BTC/USDT")?.symbol ?: "BTC/USDT"
    )
    val activeSymbol: StateFlow<String> = _activeSymbol.asStateFlow()

    private val _timeframe = MutableStateFlow("5m")
    val timeframe: StateFlow<String> = _timeframe.asStateFlow()

    private val _settings = MutableStateFlow(ChartSettings())
    val settings: StateFlow<ChartSettings> = _settings.asStateFlow()

    private val _activeTool = MutableStateFlow<String?>(null)
    val activeTool: StateFlow<String?> = _activeTool.asStateFlow()

    private val _indicators = MutableStateFlow<List<String>>(emptyList())
    val indicators: StateFlow<List<String>> = _indicators.asStateFlow()

    private val _chartType = MutableStateFlow("candles")
    val chartType: StateFlow<String> = _chartType.asStateFlow()

    private val _theme = MutableStateFlow("dark")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _selectedTimezone = MutableStateFlow("UTC-8")
    val selectedTimezone: StateFlow<String> = _selectedTimezone.asStateFlow()

    private val _isMagnetMode = MutableStateFlow(false)
    val isMagnetMode: StateFlow<Boolean> = _isMagnetMode.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isCrosshairEnabled = MutableStateFlow(true)
    val isCrosshairEnabled: StateFlow<Boolean> = _isCrosshairEnabled.asStateFlow()

    private val _isScrolledBack = MutableStateFlow(false)
    val isScrolledBack: StateFlow<Boolean> = _isScrolledBack.asStateFlow()

    private val _tradeRequest = MutableStateFlow<Pair<String, Long>?>(null)
    val tradeRequest: StateFlow<Pair<String, Long>?> = _tradeRequest.asStateFlow()

    private val _activePositions = MutableStateFlow<Set<String>>(emptySet())
    val activePositions: StateFlow<Set<String>> = _activePositions.asStateFlow()

    private val _drawings = MutableStateFlow<List<DrawingItem>>(emptyList())
    val drawings: StateFlow<List<DrawingItem>> = _drawings.asStateFlow()

    private val _selectedDrawingId = MutableStateFlow<String?>(null)
    val selectedDrawingId: StateFlow<String?> = _selectedDrawingId.asStateFlow()

    private val _candleData = MutableStateFlow<List<Candle>>(emptyList())
    val candleData: StateFlow<List<Candle>> = _candleData.asStateFlow()

    private val _currentPair = MutableStateFlow(
        MarketDataStore.pairSnapshot(_activeSymbol.value) ?: MarketDataStore.allPairs.value.first()
    )
    val currentPair: StateFlow<ForexPair> = _currentPair.asStateFlow()

    private val _priceHistory = MutableStateFlow(MarketDataStore.historySnapshot(_activeSymbol.value))
    val priceHistory: StateFlow<List<Double>> = _priceHistory.asStateFlow()

    init {
        observeMarketData()
        refreshCandles()
    }

    private fun observeMarketData() {
        viewModelScope.launch {
            combine(_activeSymbol, MarketDataStore.allPairs, MarketDataStore.priceHistory) { symbol, pairs, histories ->
                val pair = MarketDataStore.pairSnapshot(symbol) ?: pairs.first()
                val history = histories[pair.symbol] ?: List(40) { pair.price }
                pair to history
            }.collect { (pair, history) ->
                _currentPair.value = pair
                _priceHistory.value = history
                refreshCandles()
            }
        }
    }

    private fun refreshCandles() {
        _candleData.value = calculateRSI(
            buildCandlesFromHistory(
                history = _priceHistory.value,
                timeframe = _timeframe.value,
                fallbackPrice = _currentPair.value.price
            )
        )
    }

    private fun buildCandlesFromHistory(
        history: List<Double>,
        timeframe: String,
        fallbackPrice: Double
    ): List<Candle> {
        val prices = when {
            history.isNotEmpty() -> history.takeLast(60)
            fallbackPrice > 0.0 -> List(40) { fallbackPrice }
            else -> emptyList()
        }

        if (prices.isEmpty()) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val stepMillis = timeframeToMillis(timeframe)
        return prices.mapIndexed { index, close ->
            val open = if (index == 0) prices.first() else prices[index - 1]
            Candle(
                time = now - ((prices.lastIndex - index).toLong() * stepMillis),
                open = open,
                high = max(open, close),
                low = min(open, close),
                close = close,
                volume = 0.0
            )
        }
    }

    private fun timeframeToMillis(timeframe: String): Long {
        return when (timeframe) {
            "5m" -> 5 * 60 * 1000L
            "15m" -> 15 * 60 * 1000L
            "30m" -> 30 * 60 * 1000L
            "1h" -> 60 * 60 * 1000L
            "4h" -> 4 * 60 * 60 * 1000L
            "D" -> 24 * 60 * 60 * 1000L
            "W" -> 7 * 24 * 60 * 60 * 1000L
            else -> 5 * 60 * 1000L
        }
    }

    private fun calculateRSI(candles: List<Candle>, period: Int = 14): List<Candle> {
        if (candles.size <= period) return candles
        val result = candles.toMutableList()
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) avgGain += change else avgLoss += Math.abs(change)
        }
        avgGain /= period
        avgLoss /= period
        for (i in period until candles.size) {
            if (i > period) {
                val change = candles[i].close - candles[i - 1].close
                val gain = if (change > 0) change else 0.0
                val loss = if (change < 0) Math.abs(change) else 0.0
                avgGain = (avgGain * (period - 1) + gain) / period
                avgLoss = (avgLoss * (period - 1) + loss) / period
            }
            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            val rsi = 100.0 - (100.0 / (1.0 + rs))
            result[i] = result[i].copy(rsi = rsi)
        }
        return result
    }

    fun setSymbol(symbol: String) {
        _activeSymbol.value = MarketDataStore.pairSnapshot(symbol)?.symbol ?: symbol.uppercase()
    }

    fun setTimeframe(tf: String) {
        _timeframe.value = tf
        refreshCandles()
    }

    fun setSettings(newSettings: ChartSettings) {
        _settings.value = newSettings
    }

    fun setChartType(type: String) {
        _chartType.value = type
    }

    fun setTheme(theme: String) {
        _theme.value = theme
    }

    fun setTimezone(timezone: String) {
        _selectedTimezone.value = timezone
    }

    fun setMagnetMode(enabled: Boolean) {
        _isMagnetMode.value = enabled
    }

    fun setLocked(enabled: Boolean) {
        _isLocked.value = enabled
    }

    fun setCrosshairEnabled(enabled: Boolean) {
        _isCrosshairEnabled.value = enabled
    }

    fun setIsScrolledBack(scrolled: Boolean) {
        _isScrolledBack.value = scrolled
    }

    fun setActiveTool(tool: String?) {
        _activeTool.value = tool
    }

    fun toggleIndicator(indicator: String) {
        val current = _indicators.value.toMutableList()
        if (current.contains(indicator)) current.remove(indicator) else current.add(indicator)
        _indicators.value = current
    }

    fun resetDrawings() {}
    fun resetZoom() {}

    fun handleTrade(type: String) {
        val symbol = _activeSymbol.value
        val positionKey = "${symbol}_${type}"
        if (_activePositions.value.contains(positionKey)) return
        _tradeRequest.value = Pair(type, System.currentTimeMillis())
        _activePositions.value = _activePositions.value + positionKey
    }

    fun closeTrade(type: String) {
        val symbol = _activeSymbol.value
        val positionKey = "${symbol}_${type}"
        _activePositions.value = _activePositions.value - positionKey
    }

    fun addDrawing(drawing: DrawingItem) {
        _drawings.value = _drawings.value + drawing
    }

    fun removeDrawing(id: String) {
        _drawings.value = _drawings.value.filter { it.id != id }
        if (_selectedDrawingId.value == id) _selectedDrawingId.value = null
    }

    fun selectDrawing(id: String?) {
        _selectedDrawingId.value = id
    }

    fun setDrawings(drawings: List<DrawingItem>) {
        _drawings.value = drawings
    }
}
