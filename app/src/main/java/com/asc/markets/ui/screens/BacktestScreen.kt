package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.*
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.terminal.components.SymbolSearchModal
import com.asc.markets.ui.theme.*
import com.trading.app.components.AssetIcon
import com.trading.app.models.SymbolInfo
import java.util.UUID

enum class BacktestWorkspaceMode { LIVE_MARKET_SIMULATION, BACKTEST_STRATEGY_TEST }

@Composable
fun BacktestScreen(viewModel: ForexViewModel) {
    val scroll = rememberScrollState()
    val livePair by viewModel.selectedPair.collectAsState()
    val marketPriceHistoryMap by MarketDataStore.priceHistory.collectAsState()
    val binancePriceHistoryMap by BinanceDataStore.priceHistory.collectAsState()
    val fallbackPriceHistoryMap by com.asc.markets.data.CombinedFallbackDataStore.priceHistory.collectAsState()
    val priceHistoryMap = remember(marketPriceHistoryMap, binancePriceHistoryMap, fallbackPriceHistoryMap) {
        marketPriceHistoryMap + binancePriceHistoryMap + fallbackPriceHistoryMap
    }
    var workspaceMode by remember { mutableStateOf(BacktestWorkspaceMode.LIVE_MARKET_SIMULATION) }
    var isSymbolSearchOpen by remember { mutableStateOf(false) }
    val liveChartState = rememberEmbeddedSimulationChartState(symbol = livePair.symbol.replace("/", ""), timeframe = "1h")
    val expandedLiveChartState = rememberEmbeddedSimulationChartState(symbol = livePair.symbol.replace("/", ""), timeframe = "1h")
    var isLiveChartExpanded by remember { mutableStateOf(false) }
    var isLiveChartTouchActive by remember { mutableStateOf(false) }
    val livePriceHistory = remember(livePair, priceHistoryMap) {
        priceHistoryMap[livePair.symbol]
            ?: priceHistoryMap.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, livePair.symbol) }?.value
            ?: emptyList()
    }
    var liveChartCloseHistory by remember { mutableStateOf<List<Double>>(emptyList()) }
    val liveScalePriceHistory = remember(liveChartCloseHistory, livePriceHistory) {
        if (liveChartCloseHistory.size >= 8) liveChartCloseHistory else livePriceHistory
    }

    // Simulation State
    var selectedSymbol by remember { mutableStateOf("EUR/USD") }
    var selectedTimeframe by remember { mutableStateOf("M15") }
    var simulationState by remember { mutableStateOf(SimulationControlState(false, false, SimulationSpeed.NORMAL)) }
    var currentPrice by remember { mutableStateOf("1.08450") }
    val liveCurrentPrice = livePair.price.takeIf { it.isFinite() && it > 0.0 } ?: (currentPrice.toDoubleOrNull() ?: 0.0)
    var liveLevelsSeededForSymbol by remember { mutableStateOf("") }
    var supportPrice by remember { mutableStateOf("1.08120") }
    var resistancePrice by remember { mutableStateOf("1.08500") }
    var marketCondition by remember { mutableStateOf(MarketScenarioCondition.CONSOLIDATING) }
    var marketPressure by remember { mutableStateOf(MarketPressure.NEUTRAL) }
    var volumeSignal by remember { mutableStateOf(VolumeSignal.RISING) }
    var strategyMode by remember { mutableStateOf(StrategySimulationMode.BREAKOUT) }
    var simulationResult by remember { mutableStateOf<StrategySimulationResult?>(null) }

    // Pending Orders State
    val pendingOrders = remember { mutableStateListOf<PendingOrder>() }
    var entryPrice by remember { mutableStateOf("1.08450") }
    var stopLoss by remember { mutableStateOf("1.08200") }
    var takeProfit by remember { mutableStateOf("1.08950") }
    var lotSize by remember { mutableStateOf("0.12") }
    var riskPercent by remember { mutableStateOf("1.0") }
    var orderType by remember { mutableStateOf(OrderType.BUY) }

    // SMC Controls State
    var showBOS by remember { mutableStateOf(false) }
    var showCHoCH by remember { mutableStateOf(false) }
    var showOrderBlocks by remember { mutableStateOf(false) }
    var showFVG by remember { mutableStateOf(false) }
    var showLiquiditySweeps by remember { mutableStateOf(false) }
    var showPremiumDiscount by remember { mutableStateOf(false) }

    // Supply/Demand State
    var sdZoneStrength by remember { mutableStateOf(0.7f) }
    var sdDetectionLength by remember { mutableStateOf(50) }
    var sdTouchRule by remember { mutableStateOf(TouchRule.SINGLE_TOUCH) }
    var dynamicSR by remember { mutableStateOf(true) }

    // Execution Realism State
    var slippage by remember { mutableStateOf(0.5f) }
    var spreadType by remember { mutableStateOf(SpreadType.VARIABLE) }
    var latency by remember { mutableStateOf(50) }
    var fillType by remember { mutableStateOf(FillType.DELAYED) }

    // Performance State
    var performance by remember { mutableStateOf<SimulationPerformance?>(null) }
    LaunchedEffect(livePair.symbol, selectedTimeframe) {
        liveChartState.symbol = livePair.symbol.replace("/", "")
        liveChartState.timeframe = selectedTimeframe.toStreamChartTimeframe()
        liveChartCloseHistory = emptyList()
    }
    LaunchedEffect(livePair.symbol, liveCurrentPrice) {
        if (liveCurrentPrice > 0.0 && liveLevelsSeededForSymbol != livePair.symbol) {
            currentPrice = formatBacktestPrice(liveCurrentPrice)
            supportPrice = formatBacktestPrice(liveCurrentPrice * 0.995)
            resistancePrice = formatBacktestPrice(liveCurrentPrice * 1.005)
            liveLevelsSeededForSymbol = livePair.symbol
        }
    }
    val placePendingOrder: () -> Unit = {
        val entryP = entryPrice.toDoubleOrNull() ?: 0.0
        val slP = stopLoss.toDoubleOrNull() ?: 0.0
        val tpP = takeProfit.toDoubleOrNull() ?: 0.0
        val lots = lotSize.toDoubleOrNull() ?: 0.0
        val risk = riskPercent.toDoubleOrNull() ?: 0.0

        pendingOrders.add(
            PendingOrder(
                id = UUID.randomUUID().toString().take(8).uppercase(),
                symbol = selectedSymbol,
                type = orderType,
                entryPrice = entryP,
                stopLoss = slP,
                takeProfit = tpP,
                lotSize = lots,
                riskPercent = risk,
                status = OrderStatus.WAITING,
                timestamp = System.currentTimeMillis(),
                triggerDistance = if (orderType == OrderType.BUY) entryP - slP else slP - entryP
            )
        )
        Unit
    }
    val runBacktestStrategyTest: () -> Unit = {
        val entryP = entryPrice.toDoubleOrNull() ?: 0.0
        val slP = stopLoss.toDoubleOrNull() ?: 0.0
        val tpP = takeProfit.toDoubleOrNull() ?: 0.0
        val input = ScenarioSimulationInput(
            symbol = selectedSymbol,
            timeframe = selectedTimeframe,
            currentPrice = entryP,
            supportPrice = minOf(entryP, slP, tpP).takeIf { it > 0.0 } ?: supportPrice.toDoubleOrNull() ?: 0.0,
            resistancePrice = maxOf(entryP, slP, tpP).takeIf { it > 0.0 } ?: resistancePrice.toDoubleOrNull() ?: 0.0,
            condition = marketCondition,
            pressure = marketPressure,
            volume = volumeSignal,
            strategy = strategyMode,
            bosEnabled = showBOS,
            chochEnabled = showCHoCH,
            orderBlocksEnabled = showOrderBlocks,
            fvgEnabled = showFVG,
            liquiditySweepEnabled = showLiquiditySweeps,
            premiumDiscountEnabled = showPremiumDiscount,
            zoneStrength = sdZoneStrength,
            touchRule = sdTouchRule,
            riskPercent = riskPercent.toDoubleOrNull() ?: 1.0
        )
        val result = simulateStrategyScenario(input)
        simulationResult = result
        performance = SimulationPerformance(
            winRate = result.expectedWinRate,
            lossRate = 1f - result.expectedWinRate,
            profitFactor = result.profitFactor,
            drawdown = result.expectedDrawdown,
            totalTrades = result.simulatedTrades,
            equityCurve = emptyList()
        )
        simulationState = simulationState.copy(isRunning = true, isPaused = false, currentBarIndex = 120, totalBars = 120)
    }
    val runScenarioSimulation: () -> Unit = {
        val input = ScenarioSimulationInput(
            symbol = if (workspaceMode == BacktestWorkspaceMode.LIVE_MARKET_SIMULATION) livePair.symbol else selectedSymbol,
            timeframe = selectedTimeframe,
            currentPrice = if (workspaceMode == BacktestWorkspaceMode.LIVE_MARKET_SIMULATION) liveCurrentPrice else currentPrice.toDoubleOrNull() ?: 0.0,
            supportPrice = supportPrice.toDoubleOrNull() ?: 0.0,
            resistancePrice = resistancePrice.toDoubleOrNull() ?: 0.0,
            condition = marketCondition,
            pressure = marketPressure,
            volume = volumeSignal,
            strategy = strategyMode,
            bosEnabled = showBOS,
            chochEnabled = showCHoCH,
            orderBlocksEnabled = showOrderBlocks,
            fvgEnabled = showFVG,
            liquiditySweepEnabled = showLiquiditySweeps,
            premiumDiscountEnabled = showPremiumDiscount,
            zoneStrength = sdZoneStrength,
            touchRule = sdTouchRule,
            riskPercent = riskPercent.toDoubleOrNull() ?: 1.0
        )
        val result = simulateStrategyScenario(input)
        simulationResult = result
        performance = SimulationPerformance(
            winRate = result.expectedWinRate,
            lossRate = 1f - result.expectedWinRate,
            profitFactor = result.profitFactor,
            drawdown = result.expectedDrawdown,
            totalTrades = result.simulatedTrades,
            equityCurve = emptyList()
        )
        simulationState = simulationState.copy(isRunning = true, isPaused = false, currentBarIndex = 80, totalBars = 120)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll, enabled = !isLiveChartTouchActive && !isLiveChartExpanded)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            BacktestInfoStack(
            symbol = if (workspaceMode == BacktestWorkspaceMode.LIVE_MARKET_SIMULATION) livePair.symbol else selectedSymbol,
            assetName = if (workspaceMode == BacktestWorkspaceMode.LIVE_MARKET_SIMULATION) livePair.name else backtestPairSnapshot(selectedSymbol)?.name ?: "Selected Market",
            timeframe = selectedTimeframe,
            isRunning = simulationState.isRunning,
            onSymbolClick = { isSymbolSearchOpen = true }
        )

        BacktestModeSelector(
            selectedMode = workspaceMode,
            onModeSelected = {
                workspaceMode = it
                simulationResult = null
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (workspaceMode == BacktestWorkspaceMode.LIVE_MARKET_SIMULATION) {
                LiveMarketStreamChartSection(
                    symbol = livePair.symbol,
                    timeframe = selectedTimeframe,
                    currentPrice = formatBacktestPrice(liveCurrentPrice),
                    supportPrice = supportPrice,
                    resistancePrice = resistancePrice,
                    pendingOrders = emptyList(),
                    showBOS = showBOS,
                    showCHoCH = showCHoCH,
                    showOrderBlocks = showOrderBlocks,
                    showFVG = showFVG,
                    showLiquiditySweeps = showLiquiditySweeps,
                    showPremiumDiscount = showPremiumDiscount,
                    simulationResult = simulationResult,
                    chartState = liveChartState,
                    onChartInteractionChange = { isLiveChartTouchActive = it },
                    onChartDataLoaded = { closes -> liveChartCloseHistory = closes },
                    onExpandChart = {
                        expandedLiveChartState.symbol = liveChartState.symbol
                        expandedLiveChartState.timeframe = liveChartState.timeframe
                        expandedLiveChartState.chartStyle = liveChartState.chartStyle
                        expandedLiveChartState.chartSettings = liveChartState.chartSettings
                        isLiveChartExpanded = true
                    }
                )
                AIPredeterministicMoveScalePanel(
                    result = simulationResult,
                    priceHistory = liveScalePriceHistory,
                    showBOS = showBOS,
                    showCHoCH = showCHoCH,
                    showOrderBlocks = showOrderBlocks,
                    showFVG = showFVG,
                    showLiquiditySweeps = showLiquiditySweeps,
                    zoneStrength = sdZoneStrength
                )
                LiveMarketCriteriaSection(
                    livePair = livePair,
                    livePriceHistory = livePriceHistory,
                    supportPrice = supportPrice,
                    onSupportPriceChange = { supportPrice = it },
                    resistancePrice = resistancePrice,
                    onResistancePriceChange = { resistancePrice = it },
                    marketCondition = marketCondition,
                    onMarketConditionChange = { marketCondition = it },
                    marketPressure = marketPressure,
                    onMarketPressureChange = { marketPressure = it },
                    volumeSignal = volumeSignal,
                    onVolumeSignalChange = { volumeSignal = it },
                    strategyMode = strategyMode,
                    onStrategyModeChange = { strategyMode = it },
                    showBOS = showBOS,
                    onBOSToggle = { showBOS = it },
                    showCHoCH = showCHoCH,
                    onCHoCHToggle = { showCHoCH = it },
                    showOrderBlocks = showOrderBlocks,
                    onOrderBlocksToggle = { showOrderBlocks = it },
                    showFVG = showFVG,
                    onFVGToggle = { showFVG = it },
                    showLiquiditySweeps = showLiquiditySweeps,
                    onLiquiditySweepsToggle = { showLiquiditySweeps = it },
                    showPremiumDiscount = showPremiumDiscount,
                    onPremiumDiscountToggle = { showPremiumDiscount = it },
                    result = simulationResult,
                    onRunSimulation = runScenarioSimulation
                )
            } else {
                BacktestStrategyTestSection(
                    selectedSymbol = selectedSymbol,
                    onSelectedSymbolChange = { selectedSymbol = it },
                    selectedTimeframe = selectedTimeframe,
                    onSelectedTimeframeChange = { selectedTimeframe = it },
                    entryPrice = entryPrice,
                    onEntryPriceChange = { entryPrice = it },
                    stopLoss = stopLoss,
                    onStopLossChange = { stopLoss = it },
                    takeProfit = takeProfit,
                    onTakeProfitChange = { takeProfit = it },
                    lotSize = lotSize,
                    onLotSizeChange = { lotSize = it },
                    riskPercent = riskPercent,
                    onRiskPercentChange = { riskPercent = it },
                    orderType = orderType,
                    onOrderTypeChange = { orderType = it },
                    marketCondition = marketCondition,
                    onMarketConditionChange = { marketCondition = it },
                    marketPressure = marketPressure,
                    onMarketPressureChange = { marketPressure = it },
                    volumeSignal = volumeSignal,
                    onVolumeSignalChange = { volumeSignal = it },
                    strategyMode = strategyMode,
                    onStrategyModeChange = { strategyMode = it },
                    showBOS = showBOS,
                    onBOSToggle = { showBOS = it },
                    showCHoCH = showCHoCH,
                    onCHoCHToggle = { showCHoCH = it },
                    showOrderBlocks = showOrderBlocks,
                    onOrderBlocksToggle = { showOrderBlocks = it },
                    showFVG = showFVG,
                    onFVGToggle = { showFVG = it },
                    showLiquiditySweeps = showLiquiditySweeps,
                    onLiquiditySweepsToggle = { showLiquiditySweeps = it },
                    showPremiumDiscount = showPremiumDiscount,
                    onPremiumDiscountToggle = { showPremiumDiscount = it },
                    result = simulationResult,
                    onRunBacktest = runBacktestStrategyTest
                )
                AIPredeterministicMoveScalePanel(
                    result = simulationResult,
                    priceHistory = emptyList(),
                    showBOS = showBOS,
                    showCHoCH = showCHoCH,
                    showOrderBlocks = showOrderBlocks,
                    showFVG = showFVG,
                    showLiquiditySweeps = showLiquiditySweeps,
                    zoneStrength = sdZoneStrength
                )
                PerformanceCurveSection(performance = performance)
            }
        }

        BacktestAccountStatusFooter()

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLiveChartExpanded) {
            BacktestExpandedStreamChart(
                chartState = expandedLiveChartState,
                onClose = {
                    liveChartState.symbol = expandedLiveChartState.symbol
                    liveChartState.timeframe = expandedLiveChartState.timeframe
                    liveChartState.chartStyle = expandedLiveChartState.chartStyle
                    liveChartState.chartSettings = expandedLiveChartState.chartSettings
                    isLiveChartExpanded = false
                    isLiveChartTouchActive = false
                },
                onSymbolClick = { isSymbolSearchOpen = true }
            )
        }
    }

    SymbolSearchModal(
        isOpen = isSymbolSearchOpen,
        onClose = { isSymbolSearchOpen = false },
        onSelect = { symbol ->
            viewModel.selectPairBySymbolNoNavigate(symbol)
            val streamSymbol = symbol.replace("/", "")
            liveChartState.symbol = streamSymbol
            expandedLiveChartState.symbol = streamSymbol
            selectedSymbol = symbol
            backtestPairSnapshot(symbol)?.price?.takeIf { it.isFinite() && it > 0.0 }?.let { price ->
                val formatted = formatBacktestPrice(price)
                currentPrice = formatted
                entryPrice = formatted
                stopLoss = formatBacktestPrice(price * 0.995)
                takeProfit = formatBacktestPrice(price * 1.005)
                supportPrice = formatBacktestPrice(price * 0.995)
                resistancePrice = formatBacktestPrice(price * 1.005)
            }
        }
    )
}

@Composable
fun BacktestInfoStack(
    symbol: String,
    assetName: String,
    timeframe: String,
    isRunning: Boolean,
    onSymbolClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BacktestInfoCard(title = "SYMBOL", accent = Color(0xFF2563EB)) {
            Surface(
                onClick = onSymbolClick,
                color = Color.White.copy(alpha = 0.025f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetSymbolIcon(symbol = symbol, assetName = assetName, size = 38)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(assetName, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            BacktestLabeledValue("Live Status", if (isRunning) "ONLINE" else "READY", if (isRunning) EmeraldSuccess else Color(0xFFF59E0B))
        }

        BacktestInfoCard(title = "TIMEFRAME STACK", accent = Color(0xFF3B82F6)) {
            BacktestLabeledValue("HTF", "H4", Color.White)
            Spacer(modifier = Modifier.height(7.dp))
            BacktestLabeledValue("MTF", "H1", Color.White)
            Spacer(modifier = Modifier.height(7.dp))
            BacktestLabeledValue("LTF", timeframe, Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            BacktestLabeledValue("Alignment", "CONFIRMED", EmeraldSuccess)
        }

        BacktestInfoCard(title = "MARKET PHASE (3-BAR CYCLE)", accent = EmeraldSuccess) {
            BacktestPhasePill("1", "SETUP", Color(0xFF6366F1), false)
            Spacer(modifier = Modifier.height(7.dp))
            BacktestPhasePill("2", "EXPANSION", EmeraldSuccess, true)
            Spacer(modifier = Modifier.height(7.dp))
            BacktestPhasePill("3", "REACTION", Color(0xFFF97316), false)
            Spacer(modifier = Modifier.height(12.dp))
            BacktestLabeledValue("Confidence", "82%", Color.White)
            LinearProgressIndicator(
                progress = 0.82f,
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(999.dp)),
                color = Color(0xFF2563EB),
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }

        BacktestInfoCard(title = "AI EDGE SCORE", accent = EmeraldSuccess) {
            Box(modifier = Modifier.fillMaxWidth().height(82.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 0.76f,
                    modifier = Modifier.size(72.dp),
                    color = EmeraldSuccess,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    strokeWidth = 5.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("76", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("/100", color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp)
                }
            }
            Text("High Edge", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        BacktestInfoCard(title = "EXPECTED WIN RATE", accent = EmeraldSuccess) {
            Text("54%", color = EmeraldSuccess, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(12.dp))
            BacktestLabeledValue("Profit Factor", "1.67", Color.White)
            Spacer(modifier = Modifier.height(7.dp))
            BacktestLabeledValue("Risk Reward", "1:2", Color.White)
        }
    }
}

@Composable
fun BacktestInfoCard(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(accent, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun BacktestLabeledValue(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.52f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun AssetSymbolIcon(symbol: String, assetName: String, size: Int) {
    val normalizedTicker = symbol.replace("/", "").replace("-", "").uppercase()
    val pair = backtestPairSnapshot(symbol)
    val type = pair?.category?.name?.lowercase()
        ?: when {
            normalizedTicker.startsWith("BTC") || normalizedTicker.startsWith("ETH") || normalizedTicker.endsWith("USDT") -> "crypto"
            normalizedTicker.length == 6 -> "forex"
            normalizedTicker.contains("XAU") || normalizedTicker.contains("XAG") || normalizedTicker.contains("OIL") -> "commodity"
            else -> "market"
        }
    AssetIcon(
        symbol = SymbolInfo(
            ticker = normalizedTicker,
            name = assetName,
            type = type,
            price = (pair?.price ?: 0.0).toFloat(),
            change = (pair?.change ?: 0.0).toFloat(),
            changePercent = (pair?.changePercent ?: 0.0).toFloat()
        ),
        size = size
    )
}

private fun backtestPairSnapshot(symbol: String): ForexPair? {
    return BinanceDataStore.pairSnapshot(symbol) ?: MarketDataStore.pairSnapshot(symbol)
}

@Composable
fun BacktestPhasePill(step: String, label: String, color: Color, active: Boolean) {
    Surface(
        color = if (active) color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) color.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(step, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(label, color = if (active) color else Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun BacktestAccountStatusFooter() {
    BacktestInfoCard(title = "ACCOUNT STATUS", accent = EmeraldSuccess) {
        BacktestLabeledValue("Balance", "\$10,000.00", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Equity", "\$10,000.00", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Unrealized P/L", "\$0.00", Color.White.copy(alpha = 0.72f))
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Used Margin", "\$0.00", Color.White.copy(alpha = 0.72f))
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Free Margin", "\$10,000.00", Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            color = Color(0xFF052E1A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(7.dp).background(EmeraldSuccess, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CONNECTED", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun BacktestModeSelector(
    selectedMode: BacktestWorkspaceMode,
    onModeSelected: (BacktestWorkspaceMode) -> Unit
) {
    BacktestInfoCard(title = "CHOOSE SIMULATION TYPE", accent = Color(0xFF60A5FA)) {
        ModeOptionCard(
            title = "LIVE MARKET SIMULATION",
            subtitle = "Watch live price/candles, set SMC/S&R criteria, then project likely direction.",
            selected = selectedMode == BacktestWorkspaceMode.LIVE_MARKET_SIMULATION,
            accent = EmeraldSuccess,
            onClick = { onModeSelected(BacktestWorkspaceMode.LIVE_MARKET_SIMULATION) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ModeOptionCard(
            title = "BACKTEST / STRATEGY TEST",
            subtitle = "Enter price, SL, TP, risk and strategy rules to test likely trade outcome.",
            selected = selectedMode == BacktestWorkspaceMode.BACKTEST_STRATEGY_TEST,
            accent = Color(0xFF8B5CF6),
            onClick = { onModeSelected(BacktestWorkspaceMode.BACKTEST_STRATEGY_TEST) }
        )
    }
}

@Composable
fun ModeOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.025f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(if (selected) accent else Color.Gray, CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (selected) accent else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.58f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (selected) "OPEN" else "SELECT", color = if (selected) accent else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun BacktestExpandedStreamChart(
    chartState: SimulationEmbeddedChartState,
    onClose: () -> Unit,
    onSymbolClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            com.trading.app.components.Header(
                symbol = chartState.symbol,
                timeframe = chartState.timeframe,
                chartStyle = chartState.chartStyle,
                onSymbolClick = onSymbolClick,
                onTimeframeClick = { chartState.timeframe = it.toStreamChartTimeframe() },
                onStyleChange = { chartState.chartStyle = it },
                onIndicatorClick = {},
                onSettingsClick = { chartState.showSettingsSheet = true },
                onAnalysisClick = {},
                onUndo = {},
                onRedo = {},
                canUndo = false,
                canRedo = false,
                onToolSearchClick = {},
                onRightPanelToggle = {},
                isRightPanelVisible = false,
                onDownloadChart = {},
                backgroundColor = Color(0xFF08090C),
                settings = chartState.chartSettings,
                isAtBottom = true,
                onGoToClick = {},
                onNewsClick = {},
                onLayersClick = {},
                onChatClick = {},
                onDrawingClick = {},
                onMoreClick = {},
                onTradeClick = {},
                onSellClick = {},
                onBuyClick = {},
                onCurrencyClick = {},
                showCurrencyButton = false,
                showTradeButton = false
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                com.trading.app.components.TradingChart2(
                    symbol = chartState.symbol,
                    timeframe = chartState.timeframe,
                    style = chartState.chartStyle,
                    chartSettings = chartState.chartSettings,
                    drawings = chartState.drawings,
                    onDrawingUpdate = { updatedDrawing ->
                        val existingIndex = chartState.drawings.indexOfFirst { it.id == updatedDrawing.id }
                        if (existingIndex >= 0) {
                            chartState.drawings[existingIndex] = updatedDrawing
                        } else {
                            chartState.drawings.add(updatedDrawing)
                        }
                    },
                    activeTool = "cursor",
                    onToolReset = {},
                    showVolume = true,
                    showVolumeMa = false,
                    isLocked = false,
                    isVisible = true,
                    selectedCurrency = backtestCurrencyForSymbol(chartState.symbol),
                    onCurrencyClick = {},
                    isFullscreen = true,
                    onFullscreenExit = onClose,
                    onLongPress = {},
                    onSettingsClick = { chartState.showSettingsSheet = true },
                    showCurrencySelector = false,
                    showSettingsButton = true,
                    selectedTimeZone = "UTC",
                    positions = chartState.positions,
                    onPositionUpdate = {},
                    onPositionDelete = {},
                    selectedIndicatorId = chartState.selectedIndicatorId,
                    onSelectedIndicatorIdChange = { chartState.selectedIndicatorId = it }
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(52.dp)
                .background(Color.Black.copy(alpha = 0.88f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close expanded chart", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }

    if (chartState.showSettingsSheet) {
        com.trading.app.components.ChartSettingsBottomSheet(
            settings = chartState.chartSettings,
            onUpdate = { chartState.chartSettings = it },
            onDismissRequest = { chartState.showSettingsSheet = false },
            onMoreSettingsClick = {}
        )
    }
}

@Composable
fun LiveMarketStreamChartSection(
    symbol: String,
    timeframe: String,
    currentPrice: String,
    supportPrice: String,
    resistancePrice: String,
    pendingOrders: List<PendingOrder>,
    showBOS: Boolean,
    showCHoCH: Boolean,
    showOrderBlocks: Boolean,
    showFVG: Boolean,
    showLiquiditySweeps: Boolean,
    showPremiumDiscount: Boolean,
    simulationResult: StrategySimulationResult?,
    chartState: SimulationEmbeddedChartState,
    onChartInteractionChange: (Boolean) -> Unit,
    onChartDataLoaded: (List<Double>) -> Unit,
    onExpandChart: () -> Unit
) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(EmeraldSuccess, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LIVE MARKET SIMULATION", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                val statusColor = simulationResult?.let { projectionColor(it.projection) } ?: EmeraldSuccess
                Surface(color = statusColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                    Text(if (simulationResult == null) "REAL CHART" else "SIMULATED", color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SimulationMetaItem("Price Feed", "Stream Chart Replica", EmeraldSuccess)
                SimulationMetaItem("Price", currentPrice, Color.White)
                SimulationMetaItem("Support", supportPrice, EmeraldSuccess)
                SimulationMetaItem("Resistance", resistancePrice, RoseError)
                SimulationMetaItem("Forecast", simulationResult?.let { formatSimulationLabel(it.projection.name) } ?: "Waiting", simulationResult?.let { projectionColor(it.projection) } ?: Color.White)
                SimulationMetaItem("Confidence", simulationResult?.let { "${(it.confidence * 100).toInt()}%" } ?: "--", simulationResult?.let { projectionColor(it.projection) } ?: Color.White)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            onChartInteractionChange(true)
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                            } while (event.changes.any { it.pressed })
                            onChartInteractionChange(false)
                        }
                    }
            ) {
                com.trading.app.components.TradingChart2(
                    symbol = chartState.symbol,
                    timeframe = chartState.timeframe,
                    style = chartState.chartStyle,
                    chartSettings = chartState.chartSettings,
                    drawings = chartState.drawings,
                    onDrawingUpdate = { updatedDrawing ->
                        val existingIndex = chartState.drawings.indexOfFirst { it.id == updatedDrawing.id }
                        if (existingIndex >= 0) {
                            chartState.drawings[existingIndex] = updatedDrawing
                        } else {
                            chartState.drawings.add(updatedDrawing)
                        }
                    },
                    activeTool = "cursor",
                    onToolReset = {},
                    showVolume = true,
                    showVolumeMa = false,
                    isLocked = false,
                    isVisible = true,
                    selectedCurrency = backtestCurrencyForSymbol(chartState.symbol),
                    onCurrencyClick = {},
                    onLongPress = {},
                    onSettingsClick = {},
                    showSettingsButton = false,
                    onDataLoaded = { candles ->
                        onChartDataLoaded(
                            candles.map { it.close.toDouble() }
                                .filter { it.isFinite() && it > 0.0 }
                        )
                    },
                    selectedTimeZone = "UTC",
                    positions = chartState.positions,
                    onPositionUpdate = {},
                    onPositionDelete = {},
                    selectedIndicatorId = chartState.selectedIndicatorId,
                    onSelectedIndicatorIdChange = { chartState.selectedIndicatorId = it }
                )

                IconButton(
                    onClick = onExpandChart,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.72f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Expand chart", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showBOS) OverlayBadge("BOS", Color(0xFF3B82F6))
                    if (showCHoCH) OverlayBadge("CHoCH", Color(0xFFF59E0B))
                    if (showOrderBlocks) OverlayBadge("OB", Color(0xFFEF4444))
                    if (showFVG) OverlayBadge("FVG", Color(0xFF10B981))
                    if (showLiquiditySweeps) OverlayBadge("Liq Sweep", Color(0xFF8B5CF6))
                    if (showPremiumDiscount) OverlayBadge("P/D", Color(0xFFEC4899))
                }

                simulationResult?.let { result ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.74f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, projectionColor(result.projection).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .fillMaxWidth(0.76f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(formatSimulationLabel(result.projection.name), color = projectionColor(result.projection), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("${(result.confidence * 100).toInt()}% confidence â€¢ ${result.expectedMovePips} pips", color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(result.suggestedAction, color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (pendingOrders.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    ) {
                        Text("${pendingOrders.size} pending", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
            }

            Text("$symbol â€¢ $timeframe â€¢ STREAM REAL CHART", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

private data class AIPredeterministicScaleState(
    val score: Int,
    val zoneName: String,
    val zoneColor: Color,
    val trendLabel: String,
    val trendColor: Color,
    val trendStrength: Int,
    val atrCompression: Int,
    val liquidityBuild: Int,
    val smcStructure: Int,
    val supplyDemandStrength: Int,
    val hasMarketData: Boolean
)

private fun calculateAIPredeterministicScaleState(
    result: StrategySimulationResult?,
    priceHistory: List<Double>,
    showBOS: Boolean,
    showCHoCH: Boolean,
    showOrderBlocks: Boolean,
    showFVG: Boolean,
    showLiquiditySweeps: Boolean,
    zoneStrength: Float
): AIPredeterministicScaleState {
    val cleanPrices = priceHistory.filter { it.isFinite() && it > 0.0 }.takeLast(96)
    val returns = cleanPrices.zipWithNext { previous, current ->
        ((current - previous) / previous.coerceAtLeast(0.00001)).takeIf { it.isFinite() } ?: 0.0
    }
    val averageMove = returns.map { kotlin.math.abs(it) }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    val atrCompression = if (returns.size >= 4) {
        (92f - (averageMove * 4200f).toFloat()).coerceIn(18f, 94f).toInt()
    } else {
        42
    }
    val liquidityBuild = if (showLiquiditySweeps) 76 else 48
    val smcStructure = (24 + listOf(showBOS, showCHoCH, showOrderBlocks, showFVG).count { it } * 13).coerceIn(20, 88)
    val supplyDemandStrength = (zoneStrength * 100f).toInt().coerceIn(0, 100)
    val hasMarketData = cleanPrices.size >= 8
    val longReturn = if (hasMarketData) {
        (cleanPrices.last() - cleanPrices.first()) / cleanPrices.first().coerceAtLeast(0.00001)
    } else {
        0.0
    }
    val shortAnchor = if (cleanPrices.size >= 8) cleanPrices[cleanPrices.lastIndex - 6] else cleanPrices.firstOrNull() ?: 0.0
    val shortReturn = if (shortAnchor > 0.0 && cleanPrices.isNotEmpty()) {
        (cleanPrices.last() - shortAnchor) / shortAnchor
    } else {
        0.0
    }
    val pathMove = returns.sumOf { kotlin.math.abs(it) }.coerceAtLeast(0.000001)
    val directionalEfficiency = if (hasMarketData) {
        (kotlin.math.abs(longReturn) / pathMove).coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    val trendThreshold = maxOf(averageMove * 2.0, 0.0006)
    val trendSign = when {
        longReturn > trendThreshold -> 1
        longReturn < -trendThreshold -> -1
        else -> 0
    }
    val shortSign = when {
        shortReturn > trendThreshold * 0.35 -> 1
        shortReturn < -trendThreshold * 0.35 -> -1
        else -> 0
    }
    val momentumAlignment = when {
        trendSign == 0 || shortSign == 0 -> 0.45
        trendSign == shortSign -> 1.0
        else -> 0.18
    }
    val normalizedTrendMove = if (averageMove > 0.0) {
        (kotlin.math.abs(longReturn) / (averageMove * 8.0)).coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    val trendStrength = if (hasMarketData) {
        ((directionalEfficiency * 58.0) + (normalizedTrendMove * 30.0) + (momentumAlignment * 12.0)).toInt().coerceIn(0, 100)
    } else {
        0
    }
    val marketReadiness = if (hasMarketData) {
        ((trendStrength * 0.50f) + (atrCompression * 0.18f) + (supplyDemandStrength * 0.12f) + (smcStructure * 0.10f) + (liquidityBuild * 0.10f)).toInt()
    } else {
        ((atrCompression + liquidityBuild + smcStructure + supplyDemandStrength) / 4)
    }
    val resultScore = result?.confidence?.let { (it * 100f).toInt().coerceIn(0, 100) }
    val score = if (hasMarketData && resultScore != null) {
        ((marketReadiness * 0.75f) + (resultScore * 0.25f)).toInt()
    } else {
        resultScore ?: marketReadiness
    }.coerceIn(0, 100)
    val zoneName = when {
        score < 30 -> "NOISE"
        score < 60 -> "STRUCTURE"
        score < 80 -> "PRE-MOVE"
        else -> "EXPANSION"
    }
    val zoneColor = when (zoneName) {
        "NOISE" -> RoseError
        "STRUCTURE" -> Color(0xFFF59E0B)
        "PRE-MOVE" -> EmeraldSuccess
        else -> Color(0xFF38BDF8)
    }
    val trendLabel = when {
        !hasMarketData -> "WAITING FOR REAL CHART DATA"
        trendSign > 0 -> "REAL CHART UPTREND"
        trendSign < 0 -> "REAL CHART DOWNTREND"
        else -> "REAL CHART SIDEWAYS"
    }
    val trendColor = when {
        !hasMarketData -> Color(0xFFF59E0B)
        trendSign > 0 -> EmeraldSuccess
        trendSign < 0 -> RoseError
        else -> Color(0xFFF59E0B)
    }
    return AIPredeterministicScaleState(
        score = score,
        zoneName = zoneName,
        zoneColor = zoneColor,
        trendLabel = trendLabel,
        trendColor = trendColor,
        trendStrength = trendStrength,
        atrCompression = atrCompression,
        liquidityBuild = liquidityBuild,
        smcStructure = smcStructure,
        supplyDemandStrength = supplyDemandStrength,
        hasMarketData = hasMarketData
    )
}

@Composable
fun AIPredeterministicChart(
    score: Int,
    trendStrength: Int,
    zoneColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.background(PureBlack)) {
        val leftPad = 36.dp.toPx()
        val rightPad = 14.dp.toPx()
        val topPad = 30.dp.toPx()
        val bottomPad = 44.dp.toPx()
        
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        fun xFor(s: Float): Float = leftPad + chartWidth * (s / 100f)
        fun yFor(v: Float): Float = topPad + chartHeight * (1f - v / 100f)
        fun curve(s: Float): Float {
            val sD = s.toDouble()
            val strengthLift = (trendStrength - 50).toDouble() * 0.18
            val lift = 60.0 / (1.0 + kotlin.math.exp(-(sD - 55.0) / 9.0))
            val centeredNoise = sD - 15.0
            val dip = 8.0 * kotlin.math.exp(-(centeredNoise * centeredNoise) / 50.0)
            val wave = 2.4 * kotlin.math.sin((sD + trendStrength) / 8.0)
            return (20.0 + lift - dip + strengthLift + wave).toFloat().coerceIn(0f, 100f)
        }

        // Zone colors
        val noiseBg = Color(0xFF1A0505)
        val structureBg = Color(0xFF1A1000)
        val preMoveBg = Color(0xFF051A0A)
        val expansionBg = Color(0xFF001020)
        
        val noiseText = Color(0xFFCC0000)
        val structureText = Color(0xFFCC7700)
        val preMoveText = Color(0xFF00CC00)
        val expansionText = Color(0xFF00AAFF)

        // Draw Backgrounds
        drawRect(noiseBg, topLeft = Offset(xFor(0f), topPad), size = Size(xFor(30f) - xFor(0f), chartHeight))
        drawRect(structureBg, topLeft = Offset(xFor(30f), topPad), size = Size(xFor(60f) - xFor(30f), chartHeight))
        drawRect(preMoveBg, topLeft = Offset(xFor(60f), topPad), size = Size(xFor(80f) - xFor(60f), chartHeight))
        drawRect(expansionBg, topLeft = Offset(xFor(80f), topPad), size = Size(xFor(100f) - xFor(80f), chartHeight))

        // Draw vertical separators
        drawLine(Color.White.copy(alpha=0.1f), Offset(xFor(30f), topPad), Offset(xFor(30f), size.height - bottomPad))
        drawLine(Color.White.copy(alpha=0.1f), Offset(xFor(60f), topPad), Offset(xFor(60f), size.height - bottomPad))
        drawLine(Color.White.copy(alpha=0.1f), Offset(xFor(80f), topPad), Offset(xFor(80f), size.height - bottomPad))

        // Axes lines
        drawLine(Color.White.copy(alpha=0.3f), Offset(leftPad, topPad), Offset(leftPad, size.height - bottomPad), strokeWidth = 1.dp.toPx())
        drawLine(Color.White.copy(alpha=0.3f), Offset(leftPad, size.height - bottomPad), Offset(size.width - rightPad, size.height - bottomPad), strokeWidth = 1.dp.toPx())

        // Top labels
        val textStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal)
        
        fun drawZoneLabel(text1: String, text2: String, color: Color, xCenter: Float) {
            val m1 = textMeasurer.measure(text1, textStyle.copy(color = color))
            val m2 = textMeasurer.measure(text2, textStyle.copy(color = color))
            drawText(m1, topLeft = Offset(xCenter - m1.size.width / 2, topPad + 4.dp.toPx()))
            drawText(m2, topLeft = Offset(xCenter - m2.size.width / 2, topPad + 4.dp.toPx() + m1.size.height))
        }

        drawZoneLabel("NOISE", "0-30", noiseText, xFor(15f))
        drawZoneLabel("STRUCTURE", "30-60", structureText, xFor(45f))
        drawZoneLabel("PRE-MOVE", "60-80", preMoveText, xFor(70f))
        drawZoneLabel("EXPANSION", "80-100", expansionText, xFor(90f))

        // Axes text
        val axisStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Normal)
        listOf(0, 20, 40, 60, 80, 100).forEach {
            val label = it.toString()
            val meas = textMeasurer.measure(label, axisStyle)
            drawText(meas, topLeft = Offset(leftPad - meas.size.width - 4.dp.toPx(), yFor(it.toFloat()) - meas.size.height / 2))
        }
        
        listOf(0, 20, 40, 60, 80, 100).forEach {
            val label = it.toString()
            val meas = textMeasurer.measure(label, axisStyle)
            drawText(meas, topLeft = Offset(xFor(it.toFloat()) - meas.size.width / 2, size.height - bottomPad + 4.dp.toPx()))
        }

        // Labels
        val scaleMeas = textMeasurer.measure("Scale", axisStyle)
        // draw rotated text for Scale
        drawContext.canvas.save()
        drawContext.canvas.translate(12.dp.toPx(), topPad + chartHeight / 2 + scaleMeas.size.width / 2)
        drawContext.canvas.rotate(-90f)
        drawText(scaleMeas, topLeft = Offset.Zero)
        drawContext.canvas.restore()

        val probMeas = textMeasurer.measure("Probability of Deterministic Move", axisStyle)
        drawText(probMeas, topLeft = Offset(leftPad + chartWidth / 2 - probMeas.size.width / 2, size.height - bottomPad + 18.dp.toPx()))

        // Curve
        val cyan = Color(0xFF67E8F9)
        val path = androidx.compose.ui.graphics.Path()
        var first = true
        for (i in 0..100) {
            val x = xFor(i.toFloat())
            val y = yFor(curve(i.toFloat()))
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Glow effect
        drawPath(path, color = cyan.copy(alpha = 0.2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx()))
        drawPath(path, color = cyan.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
        drawPath(path, color = zoneColor.copy(alpha = 0.98f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2.dp.toPx()))

        // Current Score
        val clamped = score.toFloat().coerceIn(0f, 100f)
        val scoreX = xFor(clamped)
        val scoreY = yFor(curve(clamped))

        // Dashed line
        val dashPath = androidx.compose.ui.graphics.Path()
        dashPath.moveTo(scoreX, topPad)
        dashPath.lineTo(scoreX, size.height - bottomPad)
        drawPath(
            dashPath,
            color = zoneColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // Dot
        drawCircle(zoneColor.copy(alpha = 0.26f), radius = 15.dp.toPx(), center = Offset(scoreX, scoreY))
        drawCircle(zoneColor, radius = 6.dp.toPx(), center = Offset(scoreX, scoreY))

        // Text "$score%\nREADY"
        val readyMeas1 = textMeasurer.measure("${score}%", textStyle.copy(color = zoneColor, fontWeight = FontWeight.Bold))
        val readyMeas2 = textMeasurer.measure("READY", textStyle.copy(color = zoneColor, fontWeight = FontWeight.Bold))
        drawText(readyMeas1, topLeft = Offset(scoreX + 6.dp.toPx(), scoreY - readyMeas1.size.height))
        drawText(readyMeas2, topLeft = Offset(scoreX + 6.dp.toPx(), scoreY))
    }
}

@Composable
fun AIPredeterministicMoveScalePanel(
    result: StrategySimulationResult?,
    priceHistory: List<Double>,
    showBOS: Boolean,
    showCHoCH: Boolean,
    showOrderBlocks: Boolean,
    showFVG: Boolean,
    showLiquiditySweeps: Boolean,
    zoneStrength: Float
) {
    val scaleState = calculateAIPredeterministicScaleState(
        result = result,
        priceHistory = priceHistory,
        showBOS = showBOS,
        showCHoCH = showCHoCH,
        showOrderBlocks = showOrderBlocks,
        showFVG = showFVG,
        showLiquiditySweeps = showLiquiditySweeps,
        zoneStrength = zoneStrength
    )

    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI PRE-DETERMINISTIC MOVE SCALE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(scaleState.trendLabel, color = scaleState.trendColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${scaleState.score}%", color = scaleState.zoneColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${scaleState.zoneName} ZONE", color = scaleState.zoneColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            AIPredeterministicChart(
                score = scaleState.score,
                trendStrength = scaleState.trendStrength,
                zoneColor = scaleState.zoneColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                BacktestLabeledValue("Trend Strength", if (scaleState.hasMarketData) "${scaleState.trendStrength}%" else "Waiting", scaleState.trendColor)
                BacktestLabeledValue("ATR Compression", "${scaleState.atrCompression}%", Color.White)
                BacktestLabeledValue("Liquidity Build", "${scaleState.liquidityBuild}%", Color.White)
                BacktestLabeledValue("SMC Structure", "${scaleState.smcStructure}%", Color.White)
                BacktestLabeledValue("S/D Zone Strength", "${scaleState.supplyDemandStrength}%", Color.White)
            }
        }
    }
}

@Composable
fun AIPredeterministicMoveScaleCard(
    result: StrategySimulationResult?,
    priceHistory: List<Double>,
    showBOS: Boolean,
    showCHoCH: Boolean,
    showOrderBlocks: Boolean,
    showFVG: Boolean,
    showLiquiditySweeps: Boolean,
    zoneStrength: Float
) {
    AIPredeterministicMoveScalePanel(
        result = result,
        priceHistory = priceHistory,
        showBOS = showBOS,
        showCHoCH = showCHoCH,
        showOrderBlocks = showOrderBlocks,
        showFVG = showFVG,
        showLiquiditySweeps = showLiquiditySweeps,
        zoneStrength = zoneStrength
    )
}

@Composable
fun LiveMarketCriteriaSection(
    livePair: ForexPair,
    livePriceHistory: List<Double>,
    supportPrice: String,
    onSupportPriceChange: (String) -> Unit,
    resistancePrice: String,
    onResistancePriceChange: (String) -> Unit,
    marketCondition: MarketScenarioCondition,
    onMarketConditionChange: (MarketScenarioCondition) -> Unit,
    marketPressure: MarketPressure,
    onMarketPressureChange: (MarketPressure) -> Unit,
    volumeSignal: VolumeSignal,
    onVolumeSignalChange: (VolumeSignal) -> Unit,
    strategyMode: StrategySimulationMode,
    onStrategyModeChange: (StrategySimulationMode) -> Unit,
    showBOS: Boolean,
    onBOSToggle: (Boolean) -> Unit,
    showCHoCH: Boolean,
    onCHoCHToggle: (Boolean) -> Unit,
    showOrderBlocks: Boolean,
    onOrderBlocksToggle: (Boolean) -> Unit,
    showFVG: Boolean,
    onFVGToggle: (Boolean) -> Unit,
    showLiquiditySweeps: Boolean,
    onLiquiditySweepsToggle: (Boolean) -> Unit,
    showPremiumDiscount: Boolean,
    onPremiumDiscountToggle: (Boolean) -> Unit,
    result: StrategySimulationResult?,
    onRunSimulation: () -> Unit
) {
    BacktestInfoCard(title = "LIVE SIMULATION CRITERIA", accent = EmeraldSuccess) {
        BacktestLabeledValue("Live Symbol", livePair.symbol, Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Live Price", formatBacktestPrice(livePair.price), EmeraldSuccess)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Live Candles", "${livePriceHistory.size}", if (livePriceHistory.isNotEmpty()) EmeraldSuccess else Color(0xFFF59E0B))
        Spacer(modifier = Modifier.height(12.dp))
        OrderInputField("Support / Demand Level", supportPrice, onSupportPriceChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Resistance / Supply Level", resistancePrice, onResistancePriceChange)
        Spacer(modifier = Modifier.height(12.dp))
        ScenarioSelector("Market Condition", MarketScenarioCondition.values(), marketCondition, onMarketConditionChange, Color(0xFF60A5FA))
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector("Pressure", MarketPressure.values(), marketPressure, onMarketPressureChange, EmeraldSuccess)
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector("Volume", VolumeSignal.values(), volumeSignal, onVolumeSignalChange, Color(0xFFF59E0B))
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector("Simulation Technique", StrategySimulationMode.values(), strategyMode, onStrategyModeChange, Color(0xFF8B5CF6))
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SMCControlToggle("BOS", showBOS, onBOSToggle, Color(0xFF3B82F6))
            SMCControlToggle("CHoCH", showCHoCH, onCHoCHToggle, Color(0xFFF59E0B))
            SMCControlToggle("Order Blocks", showOrderBlocks, onOrderBlocksToggle, Color(0xFFEF4444))
            SMCControlToggle("FVG", showFVG, onFVGToggle, Color(0xFF10B981))
            SMCControlToggle("Liquidity Sweeps", showLiquiditySweeps, onLiquiditySweepsToggle, Color(0xFF8B5CF6))
            SMCControlToggle("Premium/Discount", showPremiumDiscount, onPremiumDiscountToggle, Color(0xFFEC4899))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRunSimulation,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIMULATE LIVE MARKET DIRECTION", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        if (result != null) {
            Spacer(modifier = Modifier.height(14.dp))
            SimulationResultPanel(result)
        }
    }
}

@Composable
fun BacktestStrategyTestSection(
    selectedSymbol: String,
    onSelectedSymbolChange: (String) -> Unit,
    selectedTimeframe: String,
    onSelectedTimeframeChange: (String) -> Unit,
    entryPrice: String,
    onEntryPriceChange: (String) -> Unit,
    stopLoss: String,
    onStopLossChange: (String) -> Unit,
    takeProfit: String,
    onTakeProfitChange: (String) -> Unit,
    lotSize: String,
    onLotSizeChange: (String) -> Unit,
    riskPercent: String,
    onRiskPercentChange: (String) -> Unit,
    orderType: OrderType,
    onOrderTypeChange: (OrderType) -> Unit,
    marketCondition: MarketScenarioCondition,
    onMarketConditionChange: (MarketScenarioCondition) -> Unit,
    marketPressure: MarketPressure,
    onMarketPressureChange: (MarketPressure) -> Unit,
    volumeSignal: VolumeSignal,
    onVolumeSignalChange: (VolumeSignal) -> Unit,
    strategyMode: StrategySimulationMode,
    onStrategyModeChange: (StrategySimulationMode) -> Unit,
    showBOS: Boolean,
    onBOSToggle: (Boolean) -> Unit,
    showCHoCH: Boolean,
    onCHoCHToggle: (Boolean) -> Unit,
    showOrderBlocks: Boolean,
    onOrderBlocksToggle: (Boolean) -> Unit,
    showFVG: Boolean,
    onFVGToggle: (Boolean) -> Unit,
    showLiquiditySweeps: Boolean,
    onLiquiditySweepsToggle: (Boolean) -> Unit,
    showPremiumDiscount: Boolean,
    onPremiumDiscountToggle: (Boolean) -> Unit,
    result: StrategySimulationResult?,
    onRunBacktest: () -> Unit
) {
    BacktestInfoCard(title = "BACKTEST / STRATEGY TEST", accent = Color(0xFF8B5CF6)) {
        Text(
            "Set the trade idea, strategy and confirmation rules. The tester scores whether price is likely to hit TP, SL, continue, reverse or wait.",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        OrderInputField("Symbol", selectedSymbol, onSelectedSymbolChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Timeframe", selectedTimeframe, onSelectedTimeframeChange)
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onOrderTypeChange(OrderType.BUY) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (orderType == OrderType.BUY) Color(0xFF064E3B) else Color(0xFF18181B)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (orderType == OrderType.BUY) EmeraldSuccess.copy(alpha = 0.45f) else Color(0xFF27272A))
            ) {
                Text("BUY STRATEGY", color = if (orderType == OrderType.BUY) EmeraldSuccess else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onOrderTypeChange(OrderType.SELL) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (orderType == OrderType.SELL) Color(0xFF2C0B0C) else Color(0xFF18181B)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (orderType == OrderType.SELL) RoseError.copy(alpha = 0.45f) else Color(0xFF27272A))
            ) {
                Text("SELL STRATEGY", color = if (orderType == OrderType.SELL) RoseError else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OrderInputField("Entry Price", entryPrice, onEntryPriceChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Stop Loss", stopLoss, onStopLossChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Take Profit", takeProfit, onTakeProfitChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Lot Size", lotSize, onLotSizeChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Risk %", riskPercent, onRiskPercentChange)
        Spacer(modifier = Modifier.height(12.dp))
        ScenarioSelector("Market Condition", MarketScenarioCondition.values(), marketCondition, onMarketConditionChange, Color(0xFF60A5FA))
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector("Pressure", MarketPressure.values(), marketPressure, onMarketPressureChange, EmeraldSuccess)
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector("Volume", VolumeSignal.values(), volumeSignal, onVolumeSignalChange, Color(0xFFF59E0B))
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector("Strategy To Test", StrategySimulationMode.values(), strategyMode, onStrategyModeChange, Color(0xFF8B5CF6))
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SMCControlToggle("Use BOS", showBOS, onBOSToggle, Color(0xFF3B82F6))
            SMCControlToggle("Use CHoCH", showCHoCH, onCHoCHToggle, Color(0xFFF59E0B))
            SMCControlToggle("Use Order Blocks", showOrderBlocks, onOrderBlocksToggle, Color(0xFFEF4444))
            SMCControlToggle("Use FVG", showFVG, onFVGToggle, Color(0xFF10B981))
            SMCControlToggle("Use Liquidity Sweeps", showLiquiditySweeps, onLiquiditySweepsToggle, Color(0xFF8B5CF6))
            SMCControlToggle("Use Premium/Discount", showPremiumDiscount, onPremiumDiscountToggle, Color(0xFFEC4899))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRunBacktest,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Science, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RUN BACKTEST / STRATEGY TEST", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        if (result != null) {
            Spacer(modifier = Modifier.height(14.dp))
            SimulationResultPanel(result)
        }
    }
}

fun formatBacktestPrice(price: Double): String {
    return if (price >= 100.0) {
        String.format("%.2f", price)
    } else {
        String.format("%.5f", price) // 5 decimals for forex (like MT5)
    }
}

fun String.toStreamChartTimeframe(): String {
    return when (uppercase()) {
        "M1", "1M" -> "1m"
        "M5", "5M" -> "5m"
        "M15", "15M" -> "15m"
        "M30", "30M" -> "30m"
        "H1", "1H" -> "1h"
        "H4", "4H" -> "4h"
        "D1", "1D", "D" -> "1d"
        "W1", "1W", "W" -> "1w"
        else -> "1h"
    }
}

fun backtestCurrencyForSymbol(symbol: String): String {
    val normalized = symbol.uppercase()
    return when {
        normalized.endsWith("USDT") -> "USDT"
        normalized.endsWith("JPY") -> "JPY"
        normalized.endsWith("GBP") -> "GBP"
        normalized.endsWith("EUR") -> "EUR"
        else -> "USD"
    }
}

@Composable
fun ScenarioStrategyLabSection(
    currentPrice: String,
    onCurrentPriceChange: (String) -> Unit,
    supportPrice: String,
    onSupportPriceChange: (String) -> Unit,
    resistancePrice: String,
    onResistancePriceChange: (String) -> Unit,
    marketCondition: MarketScenarioCondition,
    onMarketConditionChange: (MarketScenarioCondition) -> Unit,
    marketPressure: MarketPressure,
    onMarketPressureChange: (MarketPressure) -> Unit,
    volumeSignal: VolumeSignal,
    onVolumeSignalChange: (VolumeSignal) -> Unit,
    strategyMode: StrategySimulationMode,
    onStrategyModeChange: (StrategySimulationMode) -> Unit,
    showBOS: Boolean,
    onBOSToggle: (Boolean) -> Unit,
    showCHoCH: Boolean,
    onCHoCHToggle: (Boolean) -> Unit,
    showOrderBlocks: Boolean,
    onOrderBlocksToggle: (Boolean) -> Unit,
    showFVG: Boolean,
    onFVGToggle: (Boolean) -> Unit,
    showLiquiditySweeps: Boolean,
    onLiquiditySweepsToggle: (Boolean) -> Unit,
    showPremiumDiscount: Boolean,
    onPremiumDiscountToggle: (Boolean) -> Unit,
    result: StrategySimulationResult?,
    onRunSimulation: () -> Unit
) {
    BacktestInfoCard(title = "SCENARIO + STRATEGY SIMULATOR", accent = Color(0xFF8B5CF6)) {
        Text(
            "Feed the market state and techniques, then run a projection for likely direction, confidence, entry logic, and strategy performance.",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        OrderInputField("Current Price", currentPrice, onCurrentPriceChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Support", supportPrice, onSupportPriceChange)
        Spacer(modifier = Modifier.height(8.dp))
        OrderInputField("Resistance", resistancePrice, onResistancePriceChange)
        Spacer(modifier = Modifier.height(12.dp))
        ScenarioSelector(
            title = "Market Condition",
            options = MarketScenarioCondition.values(),
            selected = marketCondition,
            onSelected = onMarketConditionChange,
            accent = Color(0xFF60A5FA)
        )
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector(
            title = "Pressure",
            options = MarketPressure.values(),
            selected = marketPressure,
            onSelected = onMarketPressureChange,
            accent = EmeraldSuccess
        )
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector(
            title = "Volume",
            options = VolumeSignal.values(),
            selected = volumeSignal,
            onSelected = onVolumeSignalChange,
            accent = Color(0xFFF59E0B)
        )
        Spacer(modifier = Modifier.height(10.dp))
        ScenarioSelector(
            title = "Strategy To Simulate",
            options = StrategySimulationMode.values(),
            selected = strategyMode,
            onSelected = onStrategyModeChange,
            accent = Color(0xFF8B5CF6)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Technique Inputs", color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SMCControlToggle("BOS", showBOS, onBOSToggle, Color(0xFF3B82F6))
            SMCControlToggle("CHoCH", showCHoCH, onCHoCHToggle, Color(0xFFF59E0B))
            SMCControlToggle("Order Blocks", showOrderBlocks, onOrderBlocksToggle, Color(0xFFEF4444))
            SMCControlToggle("FVG", showFVG, onFVGToggle, Color(0xFF10B981))
            SMCControlToggle("Liquidity Sweeps", showLiquiditySweeps, onLiquiditySweepsToggle, Color(0xFF8B5CF6))
            SMCControlToggle("Premium/Discount", showPremiumDiscount, onPremiumDiscountToggle, Color(0xFFEC4899))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRunSimulation,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Science, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RUN MARKET + STRATEGY SIMULATION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        if (result != null) {
            Spacer(modifier = Modifier.height(14.dp))
            SimulationResultPanel(result)
        }
    }
}

@Composable
fun <T> ScenarioSelector(
    title: String,
    options: Array<T>,
    selected: T,
    onSelected: (T) -> Unit,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, fontWeight = FontWeight.Black)
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelected(option) },
                color = if (isSelected) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.025f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatSimulationLabel(option.toString()), color = if (isSelected) accent else Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    if (isSelected) {
                        Text("ACTIVE", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SimulationResultPanel(result: StrategySimulationResult) {
    Surface(
        color = projectionColor(result.projection).copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, projectionColor(result.projection).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BacktestLabeledValue("Likely Direction", formatSimulationLabel(result.projection.name), projectionColor(result.projection))
            BacktestLabeledValue("Confidence", "${(result.confidence * 100).toInt()}%", Color.White)
            BacktestLabeledValue("Expected Move", "${result.expectedMovePips} pips", Color.White)
            BacktestLabeledValue("Suggested Action", result.suggestedAction, projectionColor(result.projection))
            BacktestLabeledValue("Best Strategy", formatSimulationLabel(result.bestStrategy.name), Color.White)
            BacktestLabeledValue("Entry Plan", result.entryPlan, Color.White.copy(alpha = 0.78f))
            BacktestLabeledValue("Invalidation", result.invalidation, RoseError)
            BacktestLabeledValue("Risk Plan", result.riskPlan, Color.White.copy(alpha = 0.78f))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Reasoning", color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            result.reasoning.take(5).forEach { reason ->
                Text("â€¢ $reason", color = Color.White.copy(alpha = 0.68f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun simulateStrategyScenario(input: ScenarioSimulationInput): StrategySimulationResult {
    var directionalScore = 0f
    val reasoning = mutableListOf<String>()
    val techniqueCount = listOf(
        input.bosEnabled,
        input.chochEnabled,
        input.orderBlocksEnabled,
        input.fvgEnabled,
        input.liquiditySweepEnabled,
        input.premiumDiscountEnabled
    ).count { it }
    val range = (input.resistancePrice - input.supportPrice).takeIf { it > 0.0 } ?: 0.0040
    val rangePosition = ((input.currentPrice - input.supportPrice) / range).coerceIn(0.0, 1.0)
    val pressureSign = when (input.pressure) {
        MarketPressure.BULLISH -> 1f
        MarketPressure.BEARISH -> -1f
        MarketPressure.NEUTRAL -> 0f
    }

    when (input.condition) {
        MarketScenarioCondition.CONSOLIDATING -> reasoning.add("Market is consolidating, so support/resistance breakout or rejection is the main decision point.")
        MarketScenarioCondition.TRENDING_UP -> {
            directionalScore += 18f
            reasoning.add("Trend state is bullish, so continuation has priority unless CHoCH invalidates it.")
        }
        MarketScenarioCondition.TRENDING_DOWN -> {
            directionalScore -= 18f
            reasoning.add("Trend state is bearish, so downside continuation has priority unless CHoCH invalidates it.")
        }
        MarketScenarioCondition.REVERSAL_SETUP -> {
            directionalScore += pressureSign * 10f
            reasoning.add("Reversal setup detected, so CHoCH, sweep, and volume confirmation carry more weight.")
        }
    }

    directionalScore += pressureSign * 16f
    if (input.pressure != MarketPressure.NEUTRAL) {
        reasoning.add("${formatSimulationLabel(input.pressure.name)} pressure tilts the scenario score.")
    }

    when (input.volume) {
        VolumeSignal.LOW -> reasoning.add("Low volume reduces confidence until expansion confirms direction.")
        VolumeSignal.NORMAL -> reasoning.add("Normal volume keeps the scenario balanced.")
        VolumeSignal.RISING -> {
            directionalScore += if (pressureSign == 0f) 3f else pressureSign * 8f
            reasoning.add("Rising volume supports the active directional pressure.")
        }
        VolumeSignal.CLIMAX -> {
            directionalScore += if (input.condition == MarketScenarioCondition.REVERSAL_SETUP) pressureSign * 12f else pressureSign * 6f
            reasoning.add("Climax volume increases expansion/reversal probability.")
        }
    }

    if (input.bosEnabled) {
        directionalScore += if (pressureSign == 0f) 6f else pressureSign * 10f
        reasoning.add("BOS is enabled, so structure break confirmation is included.")
    }
    if (input.chochEnabled) {
        directionalScore += if (input.condition == MarketScenarioCondition.REVERSAL_SETUP) {
            if (pressureSign == 0f) 8f else pressureSign * 12f
        } else {
            if (pressureSign == 0f) 3f else pressureSign * 5f
        }
        reasoning.add("CHoCH is enabled, so reversal/transition evidence is included.")
    }
    if (input.orderBlocksEnabled) {
        directionalScore += if (pressureSign == 0f) 4f else pressureSign * 6f
        reasoning.add("Order block confluence improves entry-zone quality.")
    }
    if (input.fvgEnabled) {
        directionalScore += if (pressureSign == 0f) 3f else pressureSign * 5f
        reasoning.add("FVG imbalance is included as continuation fuel.")
    }
    if (input.liquiditySweepEnabled) {
        directionalScore += when {
            rangePosition < 0.35 -> 8f
            rangePosition > 0.65 -> -8f
            else -> pressureSign * 4f
        }
        reasoning.add("Liquidity sweep logic checks whether price is rejecting support or resistance.")
    }
    if (input.premiumDiscountEnabled) {
        directionalScore += if (rangePosition < 0.5) 7f else -7f
        reasoning.add("Premium/discount location adjusts bias based on where price sits inside the range.")
    }

    when (input.strategy) {
        StrategySimulationMode.BREAKOUT -> {
            directionalScore += if (input.condition == MarketScenarioCondition.CONSOLIDATING) pressureSign * 6f else pressureSign * 3f
            reasoning.add("Breakout strategy waits for expansion outside the consolidation boundary.")
        }
        StrategySimulationMode.BOS_RETEST -> {
            directionalScore += if (input.bosEnabled) {
                if (pressureSign == 0f) 6f else pressureSign * 8f
            } else 0f
            reasoning.add("BOS retest strategy requires a break, retest, then continuation.")
        }
        StrategySimulationMode.RANGE_BOUNCE -> {
            directionalScore += when {
                rangePosition < 0.35 -> 14f
                rangePosition > 0.65 -> -14f
                else -> 0f
            }
            reasoning.add("Range bounce strategy scores support/rejection and resistance/rejection zones.")
        }
        StrategySimulationMode.REVERSAL_CONFIRMATION -> {
            directionalScore += if (input.chochEnabled) {
                if (pressureSign == 0f) 6f else pressureSign * 9f
            } else 0f
            reasoning.add("Reversal confirmation requires CHoCH plus volume/zone agreement.")
        }
    }

    val absScore = if (directionalScore < 0f) -directionalScore else directionalScore
    var confidence = (0.46f + (absScore / 90f) * 0.28f + techniqueCount * 0.025f + input.zoneStrength * 0.08f)
    if (input.volume == VolumeSignal.LOW) confidence -= 0.08f
    if (input.pressure == MarketPressure.NEUTRAL) confidence -= 0.04f
    if (absScore < 12f) confidence -= 0.06f
    confidence = confidence.coerceIn(0.42f, 0.92f)

    val projection = when {
        confidence < 0.56f || absScore < 10f -> MarketProjection.WAIT_FOR_CONFIRMATION
        input.condition == MarketScenarioCondition.CONSOLIDATING && input.strategy == StrategySimulationMode.RANGE_BOUNCE && absScore < 20f -> MarketProjection.RANGE_CONTINUATION
        input.condition == MarketScenarioCondition.REVERSAL_SETUP && directionalScore > 0f -> MarketProjection.REVERSAL_UP
        input.condition == MarketScenarioCondition.REVERSAL_SETUP && directionalScore < 0f -> MarketProjection.REVERSAL_DOWN
        directionalScore > 0f -> MarketProjection.BULLISH_BREAKOUT
        directionalScore < 0f -> MarketProjection.BEARISH_BREAKDOWN
        else -> MarketProjection.WAIT_FOR_CONFIRMATION
    }
    val bullishProjection = projection == MarketProjection.BULLISH_BREAKOUT || projection == MarketProjection.REVERSAL_UP
    val bearishProjection = projection == MarketProjection.BEARISH_BREAKDOWN || projection == MarketProjection.REVERSAL_DOWN
    val suggestedAction = when {
        bullishProjection -> "Look for long confirmation"
        bearishProjection -> "Look for short confirmation"
        projection == MarketProjection.RANGE_CONTINUATION -> "Trade range edges only"
        else -> "Wait for confirmation"
    }
    val entryPlan = when {
        bullishProjection -> "Break/retest above ${String.format("%.5f", input.resistancePrice)} or sweep support then reclaim."
        bearishProjection -> "Break/retest below ${String.format("%.5f", input.supportPrice)} or reject resistance."
        projection == MarketProjection.RANGE_CONTINUATION -> "Buy support rejection, sell resistance rejection."
        else -> "No entry until BOS/CHoCH and volume agree."
    }
    val invalidation = when {
        bullishProjection -> "Below ${String.format("%.5f", input.supportPrice)}"
        bearishProjection -> "Above ${String.format("%.5f", input.resistancePrice)}"
        else -> "Clean close outside range without retest"
    }
    val riskPlan = "Risk ${String.format("%.1f", input.riskPercent)}%, reduce size if spread/latency expands."
    val moveMultiplier = when (projection) {
        MarketProjection.BULLISH_BREAKOUT, MarketProjection.BEARISH_BREAKDOWN -> 1.35
        MarketProjection.REVERSAL_UP, MarketProjection.REVERSAL_DOWN -> 1.05
        MarketProjection.RANGE_CONTINUATION -> 0.55
        MarketProjection.WAIT_FOR_CONFIRMATION -> 0.30
    }
    val expectedMovePips = ((range * 10000.0) * moveMultiplier).toInt().coerceIn(12, 220)
    val expectedWinRate = (0.42f + confidence * 0.27f + techniqueCount * 0.012f).coerceIn(0.46f, 0.76f)
    val profitFactor = (1.0f + confidence * 1.15f + techniqueCount * 0.04f).coerceIn(1.05f, 2.45f)
    val expectedDrawdown = (0.24f - confidence * 0.12f + input.riskPercent.toFloat() * 0.01f).coerceIn(0.04f, 0.22f)
    val simulatedTrades = when (input.strategy) {
        StrategySimulationMode.BREAKOUT -> 64
        StrategySimulationMode.BOS_RETEST -> 52
        StrategySimulationMode.RANGE_BOUNCE -> 78
        StrategySimulationMode.REVERSAL_CONFIRMATION -> 41
    } + techniqueCount * 4

    reasoning.add("Zone strength is ${(input.zoneStrength * 100).toInt()}%, giving the simulation a ${if (input.zoneStrength >= 0.65f) "high" else "moderate"} confluence base.")

    return StrategySimulationResult(
        projection = projection,
        confidence = confidence,
        expectedMovePips = expectedMovePips,
        suggestedAction = suggestedAction,
        bestStrategy = input.strategy,
        entryPlan = entryPlan,
        invalidation = invalidation,
        riskPlan = riskPlan,
        expectedWinRate = expectedWinRate,
        profitFactor = profitFactor,
        expectedDrawdown = expectedDrawdown,
        simulatedTrades = simulatedTrades,
        reasoning = reasoning
    )
}

fun projectionColor(projection: MarketProjection): Color {
    return when (projection) {
        MarketProjection.BULLISH_BREAKOUT, MarketProjection.REVERSAL_UP -> EmeraldSuccess
        MarketProjection.BEARISH_BREAKDOWN, MarketProjection.REVERSAL_DOWN -> RoseError
        MarketProjection.RANGE_CONTINUATION -> Color(0xFFF59E0B)
        MarketProjection.WAIT_FOR_CONFIRMATION -> Color(0xFF60A5FA)
    }
}

fun formatSimulationLabel(value: String): String = value.replace("_", " ")

// ðŸŸ¢ 1. Live Market Simulation Section
@Composable
fun LiveMarketSimulationSection(
    symbol: String,
    timeframe: String,
    currentPrice: String,
    supportPrice: String,
    resistancePrice: String,
    pendingOrders: List<PendingOrder>,
    showBOS: Boolean,
    showCHoCH: Boolean,
    showOrderBlocks: Boolean,
    showFVG: Boolean,
    showLiquiditySweeps: Boolean,
    showPremiumDiscount: Boolean,
    simulationResult: StrategySimulationResult?,
    priceHistory: List<Double> = emptyList()
) {
    val chartPrices = remember(priceHistory) {
        priceHistory.filter { it.isFinite() && it > 0.0 }.takeLast(60)
    }
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(EmeraldSuccess, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LIVE MARKET SIMULATION", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                val statusColor = simulationResult?.let { projectionColor(it.projection) } ?: EmeraldSuccess
                Surface(color = statusColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                    Text(if (simulationResult == null) "LIVE" else "SIMULATED", color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SimulationMetaItem("Price Feed", "Real Time", EmeraldSuccess)
                SimulationMetaItem("Speed", "1x", Color.White)
                SimulationMetaItem("Spread", "1.2 pips", Color.White)
                SimulationMetaItem("Volatility", "Medium", Color(0xFF60A5FA))
                SimulationMetaItem("Session", "London + NY", Color.White)
                SimulationMetaItem("Forecast", simulationResult?.let { formatSimulationLabel(it.projection.name) } ?: "Waiting", simulationResult?.let { projectionColor(it.projection) } ?: Color.White)
                SimulationMetaItem("Confidence", simulationResult?.let { "${(it.confidence * 100).toInt()}%" } ?: "--", simulationResult?.let { projectionColor(it.projection) } ?: Color.White)
            }

            // Chart placeholder with overlays indicators
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            ) {
                // Grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val leftPad = 22.dp.toPx()
                    val rightPad = 34.dp.toPx()
                    val topPad = 28.dp.toPx()
                    val bottomPad = 28.dp.toPx()
                    val chartWidth = size.width - leftPad - rightPad
                    val chartHeight = size.height - topPad - bottomPad
                    val supplyTop = topPad + chartHeight * 0.18f
                    val demandTop = topPad + chartHeight * 0.68f

                    for (i in 0..6) {
                        val y = topPad + chartHeight * (i / 6f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.055f),
                            start = Offset(leftPad, y),
                            end = Offset(size.width - rightPad, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (i in 0..8) {
                        val x = leftPad + chartWidth * (i / 8f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.035f),
                            start = Offset(x, topPad),
                            end = Offset(x, size.height - bottomPad),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    drawRect(
                        color = RoseError.copy(alpha = 0.16f),
                        topLeft = Offset(leftPad, supplyTop),
                        size = Size(chartWidth, 35.dp.toPx())
                    )
                    drawRect(
                        color = Color(0xFF2563EB).copy(alpha = 0.18f),
                        topLeft = Offset(leftPad, demandTop),
                        size = Size(chartWidth, 35.dp.toPx())
                    )
                    drawLine(
                        color = RoseError.copy(alpha = 0.85f),
                        start = Offset(leftPad, supplyTop + 17.dp.toPx()),
                        end = Offset(size.width - rightPad, supplyTop + 17.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = EmeraldSuccess.copy(alpha = 0.5f),
                        start = Offset(leftPad, demandTop + 52.dp.toPx()),
                        end = Offset(size.width - rightPad, demandTop + 52.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = EmeraldSuccess.copy(alpha = 0.55f),
                        start = Offset(leftPad, topPad + chartHeight * 0.58f),
                        end = Offset(size.width - rightPad, topPad + chartHeight * 0.58f),
                        strokeWidth = 1.dp.toPx()
                    )

                    if (chartPrices.size >= 2) {
                        val minPrice = chartPrices.minOrNull() ?: 0.0
                        val maxPrice = chartPrices.maxOrNull() ?: minPrice
                        val priceRange = (maxPrice - minPrice).takeIf { it > 0.0 } ?: (maxPrice * 0.002).takeIf { it > 0.0 } ?: 1.0
                        val step = chartWidth / chartPrices.lastIndex.coerceAtLeast(1)
                        chartPrices.dropLast(1).forEachIndexed { index, open ->
                            val close = chartPrices[index + 1]
                            val x = leftPad + step * index
                            val openY = topPad + chartHeight * ((maxPrice - open) / priceRange).toFloat().coerceIn(0f, 1f)
                            val closeY = topPad + chartHeight * ((maxPrice - close) / priceRange).toFloat().coerceIn(0f, 1f)
                            val candleTop = if (openY < closeY) openY else closeY
                            val candleBottom = if (openY > closeY) openY else closeY
                            val color = if (close >= open) EmeraldSuccess else RoseError
                            drawLine(
                                color = color.copy(alpha = 0.85f),
                                start = Offset(x, (candleTop - 8.dp.toPx()).coerceAtLeast(topPad)),
                                end = Offset(x, (candleBottom + 8.dp.toPx()).coerceAtMost(size.height - bottomPad)),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawRect(
                                color = color.copy(alpha = 0.9f),
                                topLeft = Offset(x - 2.dp.toPx(), candleTop),
                                size = Size(4.dp.toPx(), (candleBottom - candleTop).coerceAtLeast(3.dp.toPx()))
                            )
                        }
                    }
                    simulationResult?.let { result ->
                        val forecastColor = projectionColor(result.projection)
                        val startX = leftPad + chartWidth * 0.78f
                        val endX = leftPad + chartWidth * 0.98f
                        val startY = topPad + chartHeight * 0.53f
                        val endY = when (result.projection) {
                            MarketProjection.BULLISH_BREAKOUT, MarketProjection.REVERSAL_UP -> topPad + chartHeight * 0.26f
                            MarketProjection.BEARISH_BREAKDOWN, MarketProjection.REVERSAL_DOWN -> topPad + chartHeight * 0.78f
                            MarketProjection.RANGE_CONTINUATION -> topPad + chartHeight * 0.50f
                            MarketProjection.WAIT_FOR_CONFIRMATION -> topPad + chartHeight * 0.58f
                        }
                        drawLine(
                            color = forecastColor.copy(alpha = 0.95f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3.dp.toPx()
                        )
                        drawCircle(forecastColor.copy(alpha = 0.22f), radius = 14.dp.toPx(), center = Offset(endX, endY))
                        drawCircle(forecastColor, radius = 5.dp.toPx(), center = Offset(endX, endY))
                    }
                }

                if (chartPrices.size < 2) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color.White.copy(alpha = 0.28f), modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No live candle data available yet", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Overlay indicators
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showBOS) OverlayBadge("BOS", Color(0xFF3B82F6))
                    if (showCHoCH) OverlayBadge("CHoCH", Color(0xFFF59E0B))
                    if (showOrderBlocks) OverlayBadge("OB", Color(0xFFEF4444))
                    if (showFVG) OverlayBadge("FVG", Color(0xFF10B981))
                    if (showLiquiditySweeps) OverlayBadge("Liq Sweep", Color(0xFF8B5CF6))
                    if (showPremiumDiscount) OverlayBadge("P/D", Color(0xFFEC4899))
                }

                // Pending order lines visualization
                pendingOrders.forEach { order ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 62.dp)
                            .fillMaxWidth(0.72f)
                            .height(2.dp)
                            .background(if (order.type == OrderType.BUY) EmeraldSuccess else RoseError)
                    )
                }

                simulationResult?.let { result ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, projectionColor(result.projection).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .fillMaxWidth(0.72f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(formatSimulationLabel(result.projection.name), color = projectionColor(result.projection), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("${(result.confidence * 100).toInt()}% confidence â€¢ ${result.expectedMovePips} pips", color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(result.suggestedAction, color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("$symbol â€¢ $timeframe", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopStart).padding(start = 70.dp, top = 14.dp))
                ChartZoneLabel("Supply Zone", RoseError, modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp))
                ChartZoneLabel("Resistance / $resistancePrice", RoseError, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
                ChartZoneLabel("Current / $currentPrice", Color.White, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 52.dp))
                ChartZoneLabel("Demand Zone", Color(0xFF60A5FA), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp))
                ChartZoneLabel("Support / $supportPrice", EmeraldSuccess, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 42.dp))
            }
        }
    }
}

@Composable
fun OverlayBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun SimulationMetaItem(label: String, value: String, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.025f),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ChartZoneLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

// ðŸ”µ 3. Active Orders Table
@Composable
fun ActiveOrdersTableSection(pendingOrders: List<PendingOrder>) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ACTIVE ORDERS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = Color(0xFF1E3A5F), shape = RoundedCornerShape(6.dp)) {
                    Text("${pendingOrders.size}", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pending orders", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pendingOrders.forEach { order ->
                        OrderCard(order)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: PendingOrder) {
    val statusColor = when (order.status) {
        OrderStatus.WAITING -> Color(0xFFF59E0B)
        OrderStatus.NEAR_TRIGGER -> Color(0xFFEF4444)
        OrderStatus.FILLED -> EmeraldSuccess
        OrderStatus.CANCELLED -> Color.Gray
    }

    val timeAgo = ((System.currentTimeMillis() - order.timestamp) / 60000).let { mins ->
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            else -> "${mins / 60}h ago"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        OrderDetailLine(
            label = "Type",
            value = order.type.name,
            color = if (order.type == OrderType.BUY) EmeraldSuccess else RoseError
        )
        OrderDetailLine(
            label = "Entry",
            value = String.format("%.5f", order.entryPrice),
            color = Color.White
        )
        OrderDetailLine(
            label = "Stop Loss / Take Profit",
            value = "${String.format("%.5f", order.stopLoss)} / ${String.format("%.5f", order.takeProfit)}",
            color = Color.White.copy(alpha = 0.76f)
        )
        OrderDetailLine(
            label = "Status",
            value = order.status.name,
            color = statusColor
        )
        OrderDetailLine(
            label = "Time",
            value = timeAgo,
            color = Color.White.copy(alpha = 0.62f)
        )
    }
}

@Composable
fun OrderDetailLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TriggerConditionsSection() {
    BacktestInfoCard(title = "TRIGGER CONDITIONS", accent = Color(0xFF2563EB)) {
        TriggerConditionRow("Touch Entry Price", true)
        Spacer(modifier = Modifier.height(7.dp))
        TriggerConditionRow("Spread Adjusted Trigger", true)
        Spacer(modifier = Modifier.height(7.dp))
        TriggerConditionRow("Candle Close Confirmation", true)
        Spacer(modifier = Modifier.height(7.dp))
        TriggerConditionRow("Liquidity Sweep Confirmation", true)
        Spacer(modifier = Modifier.height(7.dp))
        TriggerConditionRow("SMC Zone Confirmation", true)
        Spacer(modifier = Modifier.height(7.dp))
        TriggerConditionRow("Market Structure (BOS/CHoCH)", true)
    }
}

@Composable
fun TriggerConditionRow(label: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.025f), RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (checked) Color(0xFF2563EB) else Color.Transparent, RoundedCornerShape(3.dp))
                    .border(1.dp, if (checked) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.18f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Text("âœ“", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.76f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text("â“˜", color = Color.White.copy(alpha = 0.32f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ðŸ§  4. Smart Money + Structure Controls
@Composable
fun SmartMoneyControlsSection(
    showBOS: Boolean,
    onBOSToggle: (Boolean) -> Unit,
    showCHoCH: Boolean,
    onCHoCHToggle: (Boolean) -> Unit,
    showOrderBlocks: Boolean,
    onOrderBlocksToggle: (Boolean) -> Unit,
    showFVG: Boolean,
    onFVGToggle: (Boolean) -> Unit,
    showLiquiditySweeps: Boolean,
    onLiquiditySweepsToggle: (Boolean) -> Unit,
    showPremiumDiscount: Boolean,
    onPremiumDiscountToggle: (Boolean) -> Unit
) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SMART MONEY + STRUCTURE CONTROLS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SMCControlToggle("BOS", showBOS, onBOSToggle, Color(0xFF3B82F6))
                SMCControlToggle("CHoCH", showCHoCH, onCHoCHToggle, Color(0xFFF59E0B))
                SMCControlToggle("Order Blocks", showOrderBlocks, onOrderBlocksToggle, Color(0xFFEF4444))
                SMCControlToggle("FVG", showFVG, onFVGToggle, Color(0xFF10B981))
                SMCControlToggle("Liquidity Sweeps", showLiquiditySweeps, onLiquiditySweepsToggle, Color(0xFF8B5CF6))
                SMCControlToggle("Premium/Discount", showPremiumDiscount, onPremiumDiscountToggle, Color(0xFFEC4899))
            }
        }
    }
}

@Composable
fun SMCControlToggle(label: String, isChecked: Boolean, onToggle: (Boolean) -> Unit, color: Color) {
    Surface(
        onClick = { onToggle(!isChecked) },
        color = if (isChecked) color.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isChecked) color.copy(alpha = 0.5f) else Color(0xFF27272A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(if (isChecked) color else Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = if (isChecked) color else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ðŸŸ  5. Supply & Demand + S/R Panels
@Composable
fun SupplyDemandPanelsSection(
    zoneStrength: Float,
    onZoneStrengthChange: (Float) -> Unit,
    detectionLength: Int,
    onDetectionLengthChange: (Int) -> Unit,
    touchRule: TouchRule,
    onTouchRuleChange: (TouchRule) -> Unit,
    dynamicSR: Boolean,
    onDynamicSRToggle: (Boolean) -> Unit
) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SUPPLY & DEMAND + S/R PANELS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Zone Strength", color = Color.Gray, fontSize = 10.sp)
                    Slider(
                        value = zoneStrength,
                        onValueChange = onZoneStrengthChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFFF59E0B))
                    )
                    Text(String.format("%.0f%%", zoneStrength * 100), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Detection Length", color = Color.Gray, fontSize = 10.sp)
                    Slider(
                        value = detectionLength.toFloat(),
                        onValueChange = { onDetectionLengthChange(it.toInt()) },
                        valueRange = 10f..200f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFF10B981))
                    )
                    Text("$detectionLength bars", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Touch Rule", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                TouchRule.values().forEach { rule ->
                    Surface(
                        onClick = { onTouchRuleChange(rule) },
                        color = if (touchRule == rule) Color(0xFF142921) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (touchRule == rule) EmeraldSuccess.copy(alpha = 0.3f) else Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(rule.name, color = if (touchRule == rule) EmeraldSuccess else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dynamic S/R", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = dynamicSR,
                    onCheckedChange = onDynamicSRToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSuccess)
                )
            }
        }
    }
}

// ðŸŸ  6. Execution Realism
@Composable
fun ExecutionRealismSection(
    slippage: Float,
    onSlippageChange: (Float) -> Unit,
    spreadType: SpreadType,
    onSpreadTypeChange: (SpreadType) -> Unit,
    latency: Int,
    onLatencyChange: (Int) -> Unit,
    fillType: FillType,
    onFillTypeChange: (FillType) -> Unit
) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXECUTION REALISM", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Slippage", color = Color.Gray, fontSize = 10.sp)
                    Slider(
                        value = slippage,
                        onValueChange = onSlippageChange,
                        valueRange = 0f..5f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFFEF4444))
                    )
                    Text(String.format("%.1f pips", slippage), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Latency", color = Color.Gray, fontSize = 10.sp)
                    Slider(
                        value = latency.toFloat(),
                        onValueChange = { onLatencyChange(it.toInt()) },
                        valueRange = 0f..500f,
                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFF6366F1))
                    )
                    Text("${latency}ms", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Spread Type", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                SpreadType.values().forEach { type ->
                    Surface(
                        onClick = { onSpreadTypeChange(type) },
                        color = if (spreadType == type) Color(0xFF142921) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (spreadType == type) EmeraldSuccess.copy(alpha = 0.3f) else Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(type.name, color = if (spreadType == type) EmeraldSuccess else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fill Type", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FillType.values().forEach { type ->
                    Surface(
                        onClick = { onFillTypeChange(type) },
                        color = if (fillType == type) Color(0xFF142921) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (fillType == type) EmeraldSuccess.copy(alpha = 0.3f) else Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(type.name, color = if (fillType == type) EmeraldSuccess else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

// ðŸ“Š 7. Performance + Win/Loss Curve
@Composable
fun PerformanceCurveSection(performance: SimulationPerformance?) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("PERFORMANCE + WIN/LOSS CURVE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Performance metrics grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PerformanceMetricCard(
                        label = "Win Rate",
                        value = performance?.winRate?.let { String.format("%.1f%%", it * 100) } ?: "51%",
                        lowLabel = "Low",
                        highLabel = "High",
                        progress = performance?.winRate ?: 0.51f,
                        color = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceMetricCard(
                        label = "Loss Rate",
                        value = performance?.lossRate?.let { String.format("%.1f%%", it * 100) } ?: "49%",
                        lowLabel = "Low",
                        highLabel = "High",
                        progress = performance?.lossRate ?: 0.49f,
                        color = RoseError,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PerformanceMetricCard(
                        label = "Profit Factor",
                        value = performance?.profitFactor?.let { String.format("%.2f", it) } ?: "1.67",
                        lowLabel = "Weak",
                        highLabel = "Strong",
                        progress = ((performance?.profitFactor ?: 1.67f) / 3f).coerceIn(0f, 1f),
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceMetricCard(
                        label = "Expectancy",
                        value = "0.32R",
                        lowLabel = "Neg",
                        highLabel = "Pos",
                        progress = 0.66f,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PerformanceMetricCard(
                        label = "Max Drawdown",
                        value = performance?.drawdown?.let { String.format("%.1f%%", it * 100) } ?: "12.4%",
                        lowLabel = "Safe",
                        highLabel = "Risk",
                        progress = performance?.drawdown?.coerceIn(0f, 1f) ?: 0.24f,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceMetricCard(
                        label = "Total Trades",
                        value = "${performance?.totalTrades ?: "5,000"}",
                        lowLabel = "Few",
                        highLabel = "Many",
                        progress = ((performance?.totalTrades ?: 5000).toFloat() / 10000f).coerceIn(0f, 1f),
                        color = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Win/Loss curve placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Win/Loss curve visualization", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricCard(
    label: String,
    value: String,
    lowLabel: String,
    highLabel: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF070707),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.035f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Color(0xFF8B93A7), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackHeight = 6.dp.toPx()
                    val y = size.height / 2f
                    val knobRadius = 5.dp.toPx()
                    val startX = knobRadius
                    val endX = size.width - knobRadius
                    val clampedProgress = progress.coerceIn(0f, 1f)
                    val knobX = startX + (endX - startX) * clampedProgress

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        topLeft = Offset(startX, y - trackHeight / 2f),
                        size = Size(endX - startX, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                    drawRoundRect(
                        color = color.copy(alpha = 0.9f),
                        topLeft = Offset(startX, y - trackHeight / 2f),
                        size = Size((knobX - startX).coerceAtLeast(trackHeight), trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                    drawCircle(Color.White, radius = knobRadius, center = Offset(knobX, y))
                    drawCircle(color.copy(alpha = 0.22f), radius = knobRadius * 1.45f, center = Offset(knobX, y))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(lowLabel, color = Color(0xFF8B93A7), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(highLabel, color = Color(0xFF8B93A7), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun RiskManagementSection(riskPercent: String) {
    BacktestInfoCard(title = "RISK MANAGEMENT", accent = RoseError) {
        BacktestLabeledValue("Risk Per Trade", "${riskPercent}%", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Max Daily Loss", "3.0%", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Max Trades Per Day", "5", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Risk Reward Filter", "1:2", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Position Sizing", "Risk Based", EmeraldSuccess)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Account Currency", "USD", Color.White)
    }
}

@Composable
fun MarketSummarySection() {
    BacktestInfoCard(title = "MARKET SUMMARY", accent = EmeraldSuccess) {
        BacktestLabeledValue("Current Price", "1.08527", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("24H Range", "1.08211 - 1.08789", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Daily Bias", "Bullish", EmeraldSuccess)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Trend Strength", "Strong", EmeraldSuccess)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Volatility (ATR)", "0.00077", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Session", "London", Color.White)
        Spacer(modifier = Modifier.height(7.dp))
        BacktestLabeledValue("Next News", "14m 32s", Color(0xFFF59E0B))
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(Color.White.copy(alpha = 0.025f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(90.dp)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(5.dp.toPx(), 12.dp.toPx()),
                    size = Size(80.dp.toPx(), 80.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9.dp.toPx())
                )
                drawArc(
                    color = EmeraldSuccess,
                    startAngle = 180f,
                    sweepAngle = 135f,
                    useCenter = false,
                    topLeft = Offset(5.dp.toPx(), 12.dp.toPx()),
                    size = Size(80.dp.toPx(), 80.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ðŸ‚", fontSize = 22.sp)
                Text("BULLISH", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ðŸŸ£ 2. Pending Order Desk (right side)
@Composable
fun PendingOrderDeskSection(
    entryPrice: String,
    onEntryPriceChange: (String) -> Unit,
    stopLoss: String,
    onStopLossChange: (String) -> Unit,
    takeProfit: String,
    onTakeProfitChange: (String) -> Unit,
    lotSize: String,
    onLotSizeChange: (String) -> Unit,
    riskPercent: String,
    onRiskPercentChange: (String) -> Unit,
    orderType: OrderType,
    onOrderTypeChange: (OrderType) -> Unit,
    onPlaceOrder: () -> Unit
) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("PENDING ORDER DESK", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buy/Sell toggle
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onOrderTypeChange(OrderType.BUY) },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (orderType == OrderType.BUY) Color(0xFF064E3B) else Color(0xFF18181B)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (orderType == OrderType.BUY) EmeraldSuccess.copy(alpha = 0.35f) else Color(0xFF27272A))
                ) {
                    Text("BUY", color = if (orderType == OrderType.BUY) EmeraldSuccess else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onOrderTypeChange(OrderType.SELL) },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (orderType == OrderType.SELL) Color(0xFF2C0B0C) else Color(0xFF18181B)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (orderType == OrderType.SELL) RoseError.copy(alpha = 0.35f) else Color(0xFF27272A))
                ) {
                    Text("SELL", color = if (orderType == OrderType.SELL) RoseError else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input fields
            OrderInputField("Entry Price", entryPrice, onEntryPriceChange)
            Spacer(modifier = Modifier.height(12.dp))
            OrderInputField("Stop Loss", stopLoss, onStopLossChange)
            Spacer(modifier = Modifier.height(12.dp))
            OrderInputField("Take Profit", takeProfit, onTakeProfitChange)
            Spacer(modifier = Modifier.height(12.dp))
            OrderInputField("Lot Size", lotSize, onLotSizeChange)
            Spacer(modifier = Modifier.height(12.dp))
            OrderInputField("Risk %", riskPercent, onRiskPercentChange)

            Spacer(modifier = Modifier.height(16.dp))

            // Place Order button
            Button(
                onClick = onPlaceOrder,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("PLACE ORDER", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrderInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = Color(0xFF0A0A0A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// âš¡ 8. Full Control Buttons (Bottom)
@Composable
fun ControlButtonsSection(
    isRunning: Boolean,
    isPaused: Boolean,
    speed: SimulationSpeed,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFastForward: () -> Unit,
    onTestPendingOrder: () -> Unit,
    onCancelOrders: () -> Unit
) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("FULL CONTROL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart,
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B).copy(alpha = if (!isRunning) 0.3f else 0.1f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = if (!isRunning) 0.35f else 0.2f))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (!isRunning) EmeraldSuccess else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START", color = if (!isRunning) EmeraldSuccess else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPause,
                    enabled = isRunning && !isPaused,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F).copy(alpha = if (isRunning && !isPaused) 0.3f else 0.1f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = if (isRunning && !isPaused) 0.35f else 0.2f))
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, tint = if (isRunning && !isPaused) Color(0xFFF59E0B) else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PAUSE", color = if (isRunning && !isPaused) Color(0xFFF59E0B) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onFastForward,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(speed.name, color = Color(0xFF3B82F6), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTestPendingOrder,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C1D95).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TEST", color = Color(0xFF8B5CF6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCancelOrders,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C0B0C).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CANCEL ALL", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
