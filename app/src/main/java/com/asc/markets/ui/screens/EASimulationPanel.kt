package com.asc.markets.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.EALiveDataStore
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.data.ASCSignalData
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.data.trade.TradeHistoryRepository
import com.asc.markets.logic.BacktestEngine
import com.asc.markets.logic.BacktestResult
import com.asc.markets.logic.PriceStreamManager
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.screens.dashboard.marketOverviewAssetSymbols
import com.trading.app.data.ChartFeedType
import com.trading.app.data.Mt5Service
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.OHLCData
import com.trading.app.models.SymbolInfo
import com.trading.app.components.AssetIcon
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

private fun noUnderscores(s: String): String = s.replace("_", " ")

/** Parses a free-form EA writeup string into labelled rows (label -> value).
 *  Handles `key = value`, `key: value`, `LABEL 0.8000` and plain sentences.
 *  Segments may be separated by `|`, `•`, `,`, `;` or newlines. */
private fun eaWriteupLines(raw: String): List<Pair<String?, String>> {
    // Normalise all known separators to `|` first so pipes like
    // "A=1 | B=2 | C=3" don't collapse into one clustered line.
    val normalized = raw
        .replace("\n", "|")
        .replace(";", "|")
        .replace("•", "|")
        .replace("·", "|")
        .replace("///", "|")
    val tokens = normalized.split("|")
        .flatMap { it.split(Regex(",\\s*")) }
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotBlank() }
    // Trailing-number pattern: "CONFLUENCE SCORE 0.8000" -> ("CONFLUENCE SCORE", "0.8000")
    val trailingNumber = Regex("^([A-Za-z /()_-]{2,50}?)\\s+(\\d+\\.\\d+|\\d+\\s*%?)$")
    return tokens.mapNotNull { tok ->
        val norm = noUnderscores(tok).trim().replace(Regex("\\s+"), " ")
        if (norm.isBlank()) return@mapNotNull null
        val eq = norm.indexOf('=')
        if (eq > 0 && eq < norm.length - 1) {
            Pair(
                norm.substring(0, eq).trim().uppercase(),
                norm.substring(eq + 1).trim()
            )
        } else {
            val colon = norm.indexOf(':')
            if (colon > 0 && colon < norm.length - 1 &&
                norm.substring(0, colon).length <= 32
            ) {
                Pair(
                    norm.substring(0, colon).trim().uppercase(),
                    norm.substring(colon + 1).trim()
                )
            } else {
                val m = trailingNumber.matchEntire(norm)
                if (m != null) {
                    Pair(m.groupValues[1].trim().uppercase(), m.groupValues[2].trim())
                } else {
                    Pair<String?, String>(null, norm)
                }
            }
        }
    }.filter { it.second.isNotBlank() }
}

private fun writeupValueColor(value: String): Color {
    val v = value.uppercase()
    return when {
        v.contains("STRONG") || v.contains("IMPULSE UP") || v.contains("EVENT SAFE") ||
            v.contains("OPTIMAL") || v.contains("ELITE") || v.contains("BULL") ||
            v.contains("HIGH") || v == "SAFE" || v == "UP" -> EmeraldSuccess
        v.contains("WEAK") || v.contains("BEAR") || v.contains("RISKY") ||
            v.contains("BLOCKED") || v.contains("FILTERED") || v.contains("AVOID") ||
            v.contains("IMPULSE DOWN") || v == "DOWN" -> RoseError
        v.contains("NEUTRAL") || v.contains("WAIT") || v.contains("PULLBACK") ||
            v.contains("RANG") -> Color(0xFFF59E0B)
        else -> {
            // Numeric 0-1 or 0-100 score
            val num = v.replace("%", "").trim().toDoubleOrNull()
            if (num != null) {
                val n = if (num > 1.0) num / 100.0 else num
                if (n >= 0.6) EmeraldSuccess else if (n >= 0.4) Color(0xFFF59E0B) else RoseError
            } else Color(0xFFE2E8F0)
        }
    }
}

data class SimTrade(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val asset: String,
    val direction: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val size: Double = 1.0,
    val smcFilter: List<String> = emptyList(),
    val openTime: Long = System.currentTimeMillis(),
    var closeTime: Long? = null,
    var exitPrice: Double? = null,
    var pnl: Double? = null,
    var status: String = "OPEN"
)

@Composable
fun EASimulationPanel(
    viewModel: com.asc.markets.logic.ForexViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val liveAssets by EALiveDataStore.liveAssets.collectAsState()
    val signalsByAsset by EASignalLiveStore.signalsByAsset.collectAsState()
    val wsConnected by EASignalLiveStore.isConnected.collectAsState()
    val eaConnected by EALiveDataStore.isConnected.collectAsState()
    val connected = wsConnected || eaConnected

    // Current AI reality: backend deployments first, live EA write-ups + MT5
    // scanner as the on-phone fallback (same pattern as the Command Center).
    val aiDecisions by viewModel.aiDecisions.collectAsState()

    val tradeRepository = viewModel.tradeHistoryRepository

    var selectedAsset by remember { mutableStateOf("") }
    // 0 = ANALYZE, 1 = SIM TRADE, 2 = BACKTEST
    var selectedMode by remember { mutableStateOf(0) }
    var showAssetPicker by remember { mutableStateOf(false) }
    val simTrades = remember { mutableStateListOf<SimTrade>() }

    // ── Backtest state ────────────────────────────────────────────────
    val backtestTfs = listOf("M5" to 300L, "M15" to 900L, "H1" to 3600L, "H4" to 14400L, "D1" to 86400L)
    var backtestTf by remember { mutableStateOf(backtestTfs[2].first) }
    var backtestSlPct by remember { mutableStateOf(0.5f) }
    var backtestTpPct by remember { mutableStateOf(1.0f) }
    var backtestDirectionOpt by remember { mutableStateOf("AUTO") }
    var backtestResult by remember { mutableStateOf<BacktestResult?>(null) }
    var backtestRunning by remember { mutableStateOf(false) }
    var backtestRunReq by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val backtestHistory = remember { MutableStateFlow<Map<String, List<OHLCData>>>(emptyMap()) }
    var activeFetchKey by remember { mutableStateOf("") }
    val mt5Service = remember(context) {
        Mt5Service(
            pcIpAddress = NetworkConfig.mt5Host(context),
            port = NetworkConfig.mt5Port(context),
            onHistoryUpdate = { _, ohlc ->
                val key = activeFetchKey
                if (key.isNotBlank()) {
                    backtestHistory.value = backtestHistory.value + (key to ohlc)
                }
            },
            onQuoteUpdate = {},
            onConnectionStatusUpdate = {}
        )
    }
    val backtestTfSec = backtestTfs.first { it.first == backtestTf }.second

    val assetList = remember { marketOverviewAssetSymbols() }

    LaunchedEffect(assetList) {
        if (selectedAsset.isEmpty() && assetList.isNotEmpty()) {
            selectedAsset = assetList.first()
        }
    }

    LaunchedEffect(simTrades.size) {
        while (true) {
            delay(1000)
            val tradesCopy = simTrades.toList()
            tradesCopy.forEach { trade ->
                if (trade.status != "OPEN") return@forEach
                val currentPrice = PriceStreamManager.getPrice(trade.asset)
                    ?: PriceStreamManager.getPrice(trade.asset.replace("/", ""))
                if (currentPrice != null) {
                    val direction = if (trade.direction == "LONG") 1.0 else -1.0
                    val hitTp = (trade.direction == "LONG" && currentPrice >= trade.takeProfit) ||
                        (trade.direction == "SHORT" && currentPrice <= trade.takeProfit)
                    val hitSl = (trade.direction == "LONG" && currentPrice <= trade.stopLoss) ||
                        (trade.direction == "SHORT" && currentPrice >= trade.stopLoss)
                    if (hitTp || hitSl) {
                        val exitP = if (hitTp) trade.takeProfit else trade.stopLoss
                        val newStatus = if (hitTp) "TP_HIT" else "SL_HIT"
                        val idx = simTrades.indexOf(trade)
                        if (idx >= 0) {
                            simTrades[idx] = trade.copy(
                                exitPrice = exitP,
                                pnl = (exitP - trade.entryPrice) * direction * trade.size,
                                closeTime = System.currentTimeMillis(),
                                status = newStatus
                            )
                            // Save closed trade to Trade Ledger
                            CoroutineScope(Dispatchers.IO).launch {
                                tradeRepository?.saveTrade(TradeEntity(
                                    asset = trade.asset,
                                    regimeStack = "SIMULATION",
                                    direction = trade.direction,
                                    entryPrice = trade.entryPrice,
                                    exitPrice = exitP,
                                    pnl = (exitP - trade.entryPrice) * direction * trade.size,
                                    win = (hitTp),
                                    entryVolatility = 0.0,
                                    entryCorrelation = 0.0,
                                    timestamp = System.currentTimeMillis()
                                ))
                            }
                        }
                    }
                }
            }
        }
    }

    val selectedSignal = signalsByAsset.entries.find { it.key.equals(selectedAsset, ignoreCase = true) }?.value
    val hasSignal = selectedSignal != null
    val direction = selectedSignal?.chart_panel?.validator_direction ?: ""
    val entry = selectedSignal?.entry
    val tradeParams = selectedSignal?.trade_params
    val vote = selectedSignal?.chart_panel?.votes?.win_pct ?: 0.0
    val conf = selectedSignal?.validation?.confidence ?: 0.0
    val tier = selectedSignal?.chart_panel?.quality_tier ?: "NONE"
    val currentPrice = PriceStreamManager.getPrice(selectedAsset)
        ?: PriceStreamManager.getPrice(selectedAsset.replace("/", ""))
        ?: liveAssets.find { it.symbol.equals(selectedAsset, ignoreCase = true) }?.prices?.last

    val aiReality = remember(selectedAsset, aiDecisions, selectedSignal) {
        resolveAiReality(selectedAsset, aiDecisions, selectedSignal)
    }
    val hasAi = aiReality.source != "NONE"

    // SMC pattern state — which real SMC zones the user wants to filter on.
    // Zone set matches the stream-chart alerts (FVG, OB, CFVG, SD, OTE) plus
    // Premium/Discount (PD) from the EA zone context.
    val selectedSmcZones = remember { mutableStateSetOf<String>() }
    val smcCtx = remember(selectedSignal) { smcContextString(selectedSignal) }
    val eaAvailableSmcZones = remember(selectedSignal, smcCtx) {
        buildList {
            if (selectedSignal?.liquidity?.fvg_bull == true || selectedSignal?.liquidity?.fvg_bear == true || "FVG" in smcCtx) add("FVG")
            if ("ORDER BLOCK" in smcCtx || Regex("\\bOB\\b").containsMatchIn(smcCtx)) add("OB")
            if ("CFVG" in smcCtx || "CONFLUENCE" in smcCtx) add("CFVG")
            if ("SUPPLY" in smcCtx || "DEMAND" in smcCtx || Regex("\\bSD\\b").containsMatchIn(smcCtx)) add("SD")
            if ("OTE" in smcCtx || "OPTIMAL TRADE" in smcCtx) add("OTE")
            if ("PREMIUM" in smcCtx || "DISCOUNT" in smcCtx) add("PD")
        }
    }

    // SIM mode gates on REAL chart zones: fetch live candles from the bridge and only count
    // a selected pattern when the CURRENT PRICE actually sits inside that zone (same Snapshot
    // engine the backtest / stream chart use). ANALYZE mode keeps the EA write-up's zone text.
    val liveZoneKey = "$selectedAsset|$backtestTf"
    val liveZoneCandles = remember { mutableStateMapOf<String, Pair<Long, List<OHLCData>>>() }
    var liveZoneLoading by remember { mutableStateOf(false) }
    var liveZoneFailed by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMode, selectedAsset, backtestTf, selectedSmcZones.toList()) {
        val want = selectedMode == 1 && selectedSmcZones.isNotEmpty() && !backtestRunning
        if (!want) return@LaunchedEffect
        val cached = liveZoneCandles[liveZoneKey]
        if (cached != null && System.currentTimeMillis() - cached.first < 20_000L) return@LaunchedEffect
        liveZoneLoading = true
        liveZoneFailed = false
        backtestHistory.value = backtestHistory.value - liveZoneKey
        try {
            withContext(Dispatchers.Default) {
                mt5Service.connect()
                val target = chartFeedSymbolFor(ChartFeedType.EXNESS, selectedAsset)
                mt5Service.streamActiveSymbol(target, backtestTf, 400, force = true)
                val deadline = System.currentTimeMillis() + 8_000L
                var candles: List<OHLCData>? = null
                while (System.currentTimeMillis() < deadline && candles == null) {
                    val v = backtestHistory.value[liveZoneKey]
                    if (v != null && v.isNotEmpty()) candles = v else delay(120)
                }
                candles = candles ?: backtestHistory.value[liveZoneKey]
                if (candles != null && candles.isNotEmpty()) {
                    liveZoneCandles[liveZoneKey] = System.currentTimeMillis() to candles
                } else liveZoneFailed = true
            }
        } catch (e: Exception) {
            liveZoneFailed = true
        }
        liveZoneLoading = false
    }

    // Zones the current price is actually touching right now (recomputed on new price ticks).
    val liveZoneInfo = remember(liveZoneCandles, selectedAsset, backtestTf, currentPrice) {
        val c = liveZoneCandles[liveZoneKey]?.second
        if (c == null || c.size < 4 || currentPrice == null) emptyMap<String, String?>()
        else BacktestEngine.zonesAtPrice(c, backtestTfSec, currentPrice)
    }
    val liveZoneReady = remember(liveZoneCandles, selectedAsset, backtestTf) { liveZoneCandles.containsKey(liveZoneKey) }

    // In SIM mode a "DETECTED" pattern means price is inside that zone right now.
    val availableSmcZones = if (selectedMode == 1 && liveZoneReady) liveZoneInfo.keys.toList() else eaAvailableSmcZones

    var analyzeResult by remember { mutableStateOf<String?>(null) }
    var simMessage by remember { mutableStateOf<String?>(null) }

    // Direction implied by the SELECTED SMC patterns. In SIM mode this comes from the zones
    // the price really touches right now (liveZoneInfo); in ANALYZE mode from the EA write-up.
    // FVG/SD/OTE/PD carry directional bias; OB/CFVG are level zones and stay neutral (null).
    // Exactly ONE direction across selected patterns → SMC decides the trade. Zero → silent.
    // Two → SMC patterns disagree, blocked.
    val smcDirection = remember(selectedMode, selectedSignal, selectedSmcZones, smcCtx, liveZoneInfo, liveZoneReady) {
        if (selectedMode == 1 && liveZoneReady) {
            val dirs = selectedSmcZones.mapNotNull { liveZoneInfo[it] }.distinct()
            when (dirs.size) {
                1 -> dirs.first()
                else -> null
            }
        } else {
            val liq = selectedSignal?.liquidity
            val dirs = buildList {
                if ("FVG" in selectedSmcZones) {
                    if (liq?.fvg_bull == true && !liq.fvg_bear) add("LONG")
                    else if (liq?.fvg_bear == true && !liq.fvg_bull) add("SHORT")
                }
                if ("SD" in selectedSmcZones) {
                    val hasSupply = "SUPPLY" in smcCtx
                    val hasDemand = "DEMAND" in smcCtx
                    when {
                        hasSupply && !hasDemand -> add("SHORT")
                        hasDemand && !hasSupply -> add("LONG")
                    }
                }
                if ("PD" in selectedSmcZones) {
                    val inPremium = "PREMIUM" in smcCtx
                    val inDiscount = "DISCOUNT" in smcCtx
                    when {
                        inPremium && !inDiscount -> add("SHORT")
                        inDiscount && !inPremium -> add("LONG")
                    }
                }
            }.distinct()
            when (dirs.size) {
                1 -> dirs.first()
                else -> null
            }
        }
    }
    val smcPatternConflict = remember(selectedMode, selectedSignal, selectedSmcZones, smcCtx, liveZoneInfo, liveZoneReady) {
        if (selectedMode == 1 && liveZoneReady) {
            selectedSmcZones.mapNotNull { liveZoneInfo[it] }.distinct().size > 1
        } else {
            val liq = selectedSignal?.liquidity
            var sawLong = false; var sawShort = false
            if ("FVG" in selectedSmcZones) { sawLong = sawLong || liq?.fvg_bull == true; sawShort = sawShort || liq?.fvg_bear == true }
            if ("SD" in selectedSmcZones) {
                if ("SUPPLY" in smcCtx && "DEMAND" in smcCtx) { sawLong = true; sawShort = true }
                else if ("SUPPLY" in smcCtx) sawShort = true
                else if ("DEMAND" in smcCtx) sawLong = true
            }
            if ("PD" in selectedSmcZones) {
                if ("PREMIUM" in smcCtx && "DISCOUNT" in smcCtx) { sawLong = true; sawShort = true }
                else if ("PREMIUM" in smcCtx) sawShort = true
                else if ("DISCOUNT" in smcCtx) sawLong = true
            }
            sawLong && sawShort && selectedSmcZones.size > 1
        }
    }
    val smcEaConflict = smcDirection != null && direction.isNotEmpty() && direction != "WAIT" && smcDirection != direction

    // Fetch candles when backtest is requested, then execute the engine off the main thread.
    LaunchedEffect(backtestRunReq) {
        if (backtestRunReq == 0 || liveZoneLoading) return@LaunchedEffect
        backtestRunning = true
        backtestResult = null
        val key = "$selectedAsset|$backtestTf"
        activeFetchKey = key
        backtestHistory.value = backtestHistory.value - key
        try {
            val bridgeTarget = chartFeedSymbolFor(ChartFeedType.EXNESS, selectedAsset)
            withContext(Dispatchers.Default) {
                mt5Service.connect()
                mt5Service.streamActiveSymbol(bridgeTarget, backtestTf, 500, force = true)
                val deadline = System.currentTimeMillis() + 12_000L
                var candles: List<OHLCData>? = null
                while (System.currentTimeMillis() < deadline && candles == null) {
                    val v = backtestHistory.value[key]
                    if (v != null && v.isNotEmpty()) candles = v
                    else delay(120)
                }
                candles = candles ?: backtestHistory.value[key]
                backtestResult = BacktestEngine.run(
                    candles = candles ?: emptyList(),
                    tfSec = backtestTfSec,
                    selectedSmcZones = selectedSmcZones.toSet(),
                    direction = backtestDirectionOpt.takeIf { it != "AUTO" },
                    slPct = backtestSlPct / 100.0,
                    tpPct = backtestTpPct / 100.0
                )
            }
        } catch (e: Exception) {
            backtestResult = BacktestResult(error = "Backtest failed: ${e.message}")
        }
        backtestRunning = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("\uD83E\uDD16", fontSize = 20.sp)
                }
            }
            Text("AI SIMULATION", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth().background(PureBlack, RoundedCornerShape(12.dp)).padding(4.dp)) {
            listOf(0 to "ANALYZE", 1 to "SIM TRADE", 2 to "BACKTEST").forEach { (mode, label) ->
                val active = selectedMode == mode
                val accent = when (mode) {
                    1 -> Color(0xFFFF6A00)
                    2 -> Color(0xFF22D3EE)
                    else -> IndigoAccent
                }
                Surface(
                    color = if (active) Color(0xFF2B2B2B) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp).clickable {
                        selectedMode = mode
                        analyzeResult = null
                        simMessage = null
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if (active) accent else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        InfoBox {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(if (connected) EmeraldSuccess else Color(0xFFEF4444), RoundedCornerShape(4.dp)))
                    Text("EA FEED", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                }
                Text(
                    if (connected) "LIVE \u2022 ${assetList.size} assets" else "OFFLINE",
                    color = if (connected) EmeraldSuccess else Color(0xFFEF4444),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box {
            InfoBox {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("TARGET ASSET", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)

                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clickable { showAssetPicker = true },
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (selectedAsset.isEmpty()) "Select asset..." else selectedAsset,
                                color = if (selectedAsset.isEmpty()) Color.Gray else Color.White,
                                fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily
                            )
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        if (showAssetPicker) {
            val pickerAssets = remember(assetList, liveAssets, signalsByAsset) {
                val map = linkedMapOf<String, SimPickerAsset>()
                assetList.forEach { symbol ->
                    val live = liveAssets.find { it.symbol.equals(symbol, ignoreCase = true) }
                    val type = live?.let { guessAssetType(it.symbol) } ?: guessAssetType(symbol)
                    map[symbol.uppercase()] = SimPickerAsset(symbol = symbol, name = symbol, type = type)
                }
                map.values.toList()
            }
            AssetPickerSheet(
                assets = pickerAssets,
                signalsByAsset = signalsByAsset,
                selectedAsset = selectedAsset,
                onSelect = { selectedAsset = it; showAssetPicker = false },
                onDismiss = { showAssetPicker = false }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = IndigoAccent, shape = RoundedCornerShape(50), modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("\u2699", color = Color.White, fontSize = 12.sp) }
                        }
                        Text(if (hasSignal) "EA SIGNAL PREVIEW" else "LIVE DATA ANALYSIS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    if (!hasSignal) {
                        Surface(color = Color(0xFFFFA500).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("WAITING FOR EA SIGNAL", color = Color(0xFFFFA500), fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp))
                        }
                    } else if (entry?.state != null && entry.state != "NO_ENTRY") {
                        val entryColor = when (entry.state) {
                            "OPTIMAL" -> EmeraldSuccess
                            "GOOD" -> Color(0xFFFFA500)
                            else -> Color.Gray
                        }
                        Surface(color = entryColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text(noUnderscores(entry.state), color = entryColor, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("VOTE", if (hasSignal) "${vote.toInt()}%" else "—", if (vote >= 50) EmeraldSuccess else Color.Gray, Modifier.weight(1f))
                    StatChip("CONF", if (hasSignal) "${(conf * 100).toInt()}%" else "—", if (conf >= 0.6) EmeraldSuccess else Color.Gray, Modifier.weight(1f))
                    if (direction.isNotEmpty()) {
                        StatChip("DIR", noUnderscores(direction), if (direction == "LONG") EmeraldSuccess else RoseError, Modifier.weight(1f))
                    }
                    StatChip("TIER", noUnderscores(tier), when (tier) { "ELITE" -> EmeraldSuccess; "HIGH" -> IndigoAccent; else -> Color.Gray }, Modifier.weight(1f))
                }

                if (currentPrice != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("LIVE PRICE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text(String.format("%.5f", currentPrice), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }

                if (hasSignal && tradeParams != null && tradeParams.stop_loss > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("EA TRADE PARAMETERS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (tradeParams.stop_loss > 0) StatChip("SL", String.format("%.5f", tradeParams.stop_loss), RoseError, Modifier.weight(1f))
                        if (tradeParams.take_profit > 0) StatChip("TP", String.format("%.5f", tradeParams.take_profit), EmeraldSuccess, Modifier.weight(1f))
                        if (tradeParams.risk_pct > 0) StatChip("RISK", "${String.format("%.1f", tradeParams.risk_pct)}%", Color(0xFFFFA500), Modifier.weight(1f))
                    }
                }

                if (hasSignal && entry?.reason?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    EaWriteupCard(reason = entry.reason)
                }

                if (!hasSignal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFA500),
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text("LOADING EA SIGNAL...", color = Color(0xFFFFA500), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                        Text("The EA cycles through assets — signal will appear shortly", color = Color.Gray, fontSize = 9.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI REALITY — deployments first, live EA/scanner fallback
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Color(0xFF6366F1), shape = RoundedCornerShape(50), modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("\uD83E\uDDE0", color = Color.White, fontSize = 12.sp) }
                    }
                    Text("AI REALITY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = when (aiReality.source) { "DEPLOYMENTS" -> EmeraldSuccess; "EA_SIGNAL" -> IndigoAccent; else -> Color(0xFFEF4444) }.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            when (aiReality.source) { "DEPLOYMENTS" -> "AI DEPLOYMENTS"; "EA_SIGNAL" -> "EA SIGNAL"; else -> "NO AI DATA" },
                            color = when (aiReality.source) { "DEPLOYMENTS" -> EmeraldSuccess; "EA_SIGNAL" -> IndigoAccent; else -> Color(0xFFEF4444) },
                            fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp)
                        )
                    }
                }

                if (hasAi) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (aiReality.direction.isNotBlank()) {
                            StatChip("AI DIR", noUnderscores(aiReality.direction), when (aiReality.direction) { "LONG", "BUY" -> EmeraldSuccess; "SHORT", "SELL" -> RoseError; else -> Color.Gray }, Modifier.weight(1f))
                        }
                        StatChip("AI SCORE", "${(aiReality.score * 100).toInt()}%", if (aiReality.score >= 0.6) EmeraldSuccess else Color.Gray, Modifier.weight(1f))
                        StatChip("AI STATE", noUnderscores(aiReality.state), when (aiReality.state.uppercase()) { "PRIMARY", "STRONG", "ELITE" -> EmeraldSuccess; "SECONDARY", "VALID" -> IndigoAccent; "REJECTED", "FILTERED", "LOW" -> RoseError; else -> Color.Gray }, Modifier.weight(1f))
                    }

                    if (aiReality.confluence != null) {
                        StatChip("CONFLUENCE", "${(aiReality.confluence * 100).toInt()}%", if (aiReality.confluence >= 0.6) EmeraldSuccess else Color.Gray, Modifier.fillMaxWidth())
                    }

                    val isAligned = !hasSignal || aiReality.direction.isBlank() || direction.isBlank() ||
                        aiReality.direction == direction || aiReality.direction == "WAIT" || direction == "WAIT"
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = (if (isAligned) EmeraldSuccess else RoseError).copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                if (isAligned) "EA \u2713 AI ALIGNED" else "EA \u2717 AI DIVERGENT",
                                color = if (isAligned) EmeraldSuccess else RoseError, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp)
                            )
                        }
                        if (aiReality.rationale.isNotBlank()) {
                            Text(noUnderscores(aiReality.rationale), color = SlateText, fontSize = 8.sp, fontFamily = InterFontFamily, maxLines = 2)
                        }
                    }
                } else {
                    Text("No AI reality available for $selectedAsset", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SMC PATTERN FILTER
        if (hasSignal) {
            InfoBox {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Color(0xFF8B5CF6), shape = RoundedCornerShape(50), modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("\u25C6", color = Color.White, fontSize = 12.sp) }
                        }
                        Text("SMC PATTERNS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedSmcZones.isNotEmpty()) {
                            Text("${selectedSmcZones.size} selected", color = Color(0xFF8B5CF6), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                    }

                    Text(
                        if (selectedMode == 1) "SIM opens only when price is inside the selected zone" else "Tap to include in verdict filter",
                        color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily
                    )

                    val zones = listOf(
                        "FVG" to Color(0xFF60A5FA), "OB" to Color(0xFFA78BFA), "CFVG" to Color(0xFF22D3EE),
                        "SD" to Color(0xFF10B981), "OTE" to Color(0xFFF59E0B), "PD" to Color(0xFFF472B6)
                    )
                    zones.chunked(3).forEach { rowZones ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowZones.forEach { (zone, color) ->
                                val isActive = zone in selectedSmcZones
                                Surface(
                                    color = if (isActive) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isActive) selectedSmcZones.remove(zone) else selectedSmcZones.add(zone)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(zone, color = if (isActive) color else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                        val checking = selectedMode == 1 && !liveZoneReady && (liveZoneLoading || liveZoneFailed)
                                        Text(
                                            when {
                                                checking && liveZoneLoading -> "CHECKING"
                                                checking && liveZoneFailed -> "NO DATA"
                                                else -> if (zone in availableSmcZones) "DETECTED" else "ABSENT"
                                            },
                                            color = when {
                                                checking -> Color(0xFFF59E0B)
                                                zone in availableSmcZones -> EmeraldSuccess
                                                else -> Color.Gray
                                            },
                                            fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        simMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(msg, color = Color(0xFFF87171), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.padding(12.dp))
            }
        }

        if (selectedMode == 2) {
            // ── BACKTEST SETTINGS ──
            InfoBox {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Color(0xFF22D3EE), shape = RoundedCornerShape(50), modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("\uD83D\uDD5C", color = Color.Black, fontSize = 12.sp) }
                        }
                        Text("BACKTEST SETTINGS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }

                    Text("TIMEFRAME", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        backtestTfs.forEach { (code, _) ->
                            val active = backtestTf == code
                            Surface(
                                color = if (active) Color(0xFF22D3EE).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Color(0xFF22D3EE).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f).height(32.dp).clickable { backtestTf = code }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(code, color = if (active) Color(0xFF22D3EE) else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }

                    Text("DIRECTION", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AUTO" to IndigoAccent, "LONG" to EmeraldSuccess, "SHORT" to RoseError).forEach { (dir, color) ->
                            val active = backtestDirectionOpt == dir
                            Surface(
                                color = if (active) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (active) color.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f).height(32.dp).clickable { backtestDirectionOpt = dir }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(dir, color = if (active) color else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("STOP LOSS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.width(76.dp))
                        Slider(
                            value = backtestSlPct,
                            onValueChange = { backtestSlPct = it },
                            valueRange = 0.1f..2.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${String.format("%.1f", backtestSlPct)}%", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TAKE PROFIT", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.width(76.dp))
                        Slider(
                            value = backtestTpPct,
                            onValueChange = { backtestTpPct = it },
                            valueRange = 0.2f..4.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${String.format("%.1f", backtestTpPct)}%", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }

                    Text("History from the MT5 bridge (\u2248500 bars \u00B7 ${backtestTf}) \u2014 same SMC zones as the stream chart. No look-ahead.",
                        color = SlateText, fontSize = 8.sp, lineHeight = 12.sp, fontFamily = InterFontFamily)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Button(
            onClick = {
                when (selectedMode) {
                    1 -> {
                        val gated = selectedMode == 1 && selectedSmcZones.isNotEmpty()
                        val missingZones = if (gated && liveZoneReady) selectedSmcZones.filter { it !in liveZoneInfo } else emptyList()
                        val smcOk = !gated || (liveZoneReady && missingZones.isEmpty())
                        simMessage = when {
                            gated && liveZoneLoading -> "Checking SMC zones on the chart \u2014 retry in a second"
                            gated && (liveZoneFailed || !liveZoneReady) -> "SMC zone check failed \u2014 MT5 bridge unreachable. Trade blocked"
                            smcPatternConflict -> "SMC patterns disagree on direction \u2014 trade blocked"
                            smcDirection != null && smcEaConflict -> "SMC says $smcDirection but EA validator says $direction \u2014 trade blocked"
                            !smcOk -> "SMC filter not met for $selectedAsset \u2014 price is not inside ${missingZones.joinToString(" / ")}"
                            else -> null
                        }
                        if (hasSignal && direction.isNotEmpty() && currentPrice != null && tradeParams != null && simMessage == null) {
                            val sl = if (tradeParams.stop_loss > 0) tradeParams.stop_loss else currentPrice * 0.995
                            val tp = if (tradeParams.take_profit > 0) tradeParams.take_profit else currentPrice * 1.01
                            val effDirection = smcDirection ?: direction
                            simTrades.add(0, SimTrade(
                                asset = selectedAsset,
                                direction = effDirection,
                                entryPrice = currentPrice,
                                stopLoss = sl,
                                takeProfit = tp,
                                size = 1.0,
                                smcFilter = selectedSmcZones.toList()
                            ))
                        }
                    }
                    2 -> {
                        backtestRunReq++
                    }
                    else -> {
                        analyzeResult = buildAnalyzeSummary(selectedAsset, direction, vote, conf, tier, entry, tradeParams, currentPrice, aiReality, selectedSmcZones, availableSmcZones)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (selectedMode) {
                    1 -> Color(0xFFFF6A00)
                    2 -> Color(0xFF0891B2)
                    else -> IndigoAccent
                }
            ),
            enabled = when (selectedMode) {
                2 -> selectedAsset.isNotEmpty() && !backtestRunning
                else -> selectedAsset.isNotEmpty() && currentPrice != null
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    when (selectedMode) {
                        1 -> Icons.Default.PlayArrow
                        2 -> Icons.Default.Refresh
                        else -> Icons.Default.Analytics
                    },
                    contentDescription = null, tint = Color.White
                )
                Text(
                    when (selectedMode) {
                        1 -> "OPEN SIM TRADE"
                        2 -> if (backtestRunning) "BACKTESTING\u2026" else "RUN BACKTEST"
                        else -> "ANALYZE SETUP"
                    },
                    color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily
                )
            }
        }

        analyzeResult?.let { result ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = IndigoAccent.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.lines().forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(4.dp).background(IndigoAccent, RoundedCornerShape(2.dp)))
                            Text(line, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }

        if (selectedMode == 2) {
            val bt = backtestResult
            Spacer(modifier = Modifier.height(16.dp))

            if (bt?.error != null) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("BACKTEST ERROR", color = Color(0xFFF87171), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text(bt.error, color = Color(0xFFFCA5A5), fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                }
            }

            if (bt != null && bt.error == null) {
                Text("BACKTEST RESULT \u2022 $selectedAsset \u2022 $backtestTf", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("TRADES", "${bt.trades.size}", Color(0xFF22D3EE), Modifier.weight(1f))
                    StatChip("WIN RATE", "${(bt.winRate * 100).toInt()}%", if (bt.winRate >= 0.5) EmeraldSuccess else RoseError, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("NET P&L", "${if (bt.totalPnl >= 0) "+" else ""}${String.format("%.2f", bt.totalPnl)}", if (bt.totalPnl >= 0) EmeraldSuccess else RoseError, Modifier.weight(1f))
                    StatChip("PROFIT/FAIL", if (bt.profitFactor == Double.MAX_VALUE) "\u221E" else String.format("%.2f", bt.profitFactor), if (bt.profitFactor >= 1.0) EmeraldSuccess else RoseError, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("WINS", "${bt.wins}", EmeraldSuccess, Modifier.weight(1f))
                    StatChip("LOSSES", "${bt.losses}", RoseError, Modifier.weight(1f))
                    StatChip("MAX DD", "${(bt.maxDrawdownPct * 100).toInt()}%", if (bt.maxDrawdownPct <= 0.2) EmeraldSuccess else Color(0xFFFFA500), Modifier.weight(1f))
                }

                if (bt.zoneBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ENTRIES BY ZONE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("FVG" to Color(0xFF60A5FA), "OB" to Color(0xFFA78BFA), "CFVG" to Color(0xFF22D3EE), "SD" to Color(0xFF10B981), "OTE" to Color(0xFFF59E0B), "PD" to Color(0xFFF472B6))
                            .filter { bt.zoneBreakdown.containsKey(it.first) }
                            .forEach { (zone, color) ->
                                Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(zone, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                        Text("${bt.zoneBreakdown[zone]}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                    }
                }

                if (bt.trades.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("TRADES \u2022 first ${bt.trades.size}", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    bt.trades.takeLast(20).forEach { trade ->
                        Surface(
                            color = Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, (if (trade.won) EmeraldSuccess else RoseError).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(color = (if (trade.won) EmeraldSuccess else RoseError).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text(if (trade.won) "WIN" else "LOSS", color = if (trade.won) EmeraldSuccess else RoseError, fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(5.dp, 2.dp))
                                        }
                                        Surface(color = Color(0xFF22D3EE).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text(trade.zone, color = Color(0xFF67E8F9), fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(5.dp, 2.dp))
                                        }
                                        Text(trade.direction, color = if (trade.direction == "LONG") EmeraldSuccess else RoseError, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    }
                                    Text(
                                        "${String.format("%.5f", trade.entry)} \u2192 ${String.format("%.5f", trade.exit)}",
                                        color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                                    )
                                }
                                Text(
                                    "${if (trade.pnl >= 0) "+" else ""}${String.format("%.2f", trade.pnl)}",
                                    color = if (trade.pnl >= 0) EmeraldSuccess else RoseError,
                                    fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedMode == 1 && simTrades.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SIM TRADES", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val openCount = simTrades.count { it.status == "OPEN" }
                    val closedCount = simTrades.size - openCount
                    if (closedCount > 0) {
                        val totalPnl = simTrades.filter { it.pnl != null }.sumOf { it.pnl!! }
                        Text(
                            "PnL: ${if (totalPnl >= 0) "+" else ""}${String.format("%.2f", totalPnl)}",
                            color = if (totalPnl >= 0) EmeraldSuccess else RoseError,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                        )
                    }
                    Text("$openCount open", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val closedTrades = simTrades.filter { it.status != "OPEN" }
            if (closedTrades.isNotEmpty()) {
                val wins = closedTrades.count { it.status == "TP_HIT" }
                val winRate = (wins.toDouble() / closedTrades.size * 100).toInt()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStatCard("WIN RATE", "$winRate%", if (winRate >= 50) EmeraldSuccess else RoseError, Modifier.weight(1f))
                    MiniStatCard("TRADES", "${closedTrades.size}", Color.White, Modifier.weight(1f))
                    MiniStatCard("WINS", "$wins", EmeraldSuccess, Modifier.weight(1f))
                    MiniStatCard("LOSSES", "${closedTrades.size - wins}", RoseError, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            simTrades.forEach { trade ->
                val cp = PriceStreamManager.getPrice(trade.asset)
                    ?: PriceStreamManager.getPrice(trade.asset.replace("/", ""))
                val livePnl = if (trade.status == "OPEN" && cp != null) {
                    val d = if (trade.direction == "LONG") 1.0 else -1.0
                    (cp - trade.entryPrice) * d * trade.size
                } else trade.pnl

                val statusColor = when (trade.status) {
                    "TP_HIT" -> EmeraldSuccess
                    "SL_HIT" -> RoseError
                    "OPEN" -> Color(0xFFFFA500)
                    else -> Color.Gray
                }

                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = if (trade.direction == "LONG") EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text(trade.direction, color = if (trade.direction == "LONG") EmeraldSuccess else RoseError, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 2.dp))
                                }
                                Text(trade.asset, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            }
                            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text(trade.status.replace("_", " "), color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (trade.smcFilter.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                trade.smcFilter.forEach { zone ->
                                    Surface(color = Color(0xFF8B5CF6).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                        Text("$zone", color = Color(0xFFB39DFF), fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(5.dp, 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("ENTRY", color = SlateText, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                Text(String.format("%.5f", trade.entryPrice), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            }
                            Column {
                                Text("SL", color = RoseError, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                Text(String.format("%.5f", trade.stopLoss), color = RoseError, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            }
                            Column {
                                Text("TP", color = EmeraldSuccess, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                Text(String.format("%.5f", trade.takeProfit), color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("P&L", color = SlateText, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                if (livePnl != null) {
                                    Text(
                                        "${if (livePnl >= 0) "+" else ""}${String.format("%.2f", livePnl)}",
                                        color = if (livePnl >= 0) EmeraldSuccess else RoseError,
                                        fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily
                                    )
                                } else {
                                    Text("---", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                                }
                            }
                        }

                        if (trade.status != "OPEN") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("EXIT: ${String.format("%.5f", trade.exitPrice ?: 0.0)}", color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun buildAnalyzeSummary(
    asset: String,
    direction: String,
    vote: Double,
    conf: Double,
    tier: String,
    entry: com.asc.markets.data.ASCEntryData?,
    tradeParams: com.asc.markets.data.ASCTradeParams?,
    currentPrice: Double?,
    aiReality: AiReality? = null,
    selectedSmcZones: Set<String> = emptySet(),
    availableSmcZones: List<String> = emptyList()
): String {
    val sb = mutableListOf<String>()
    sb.add("SETUP ANALYSIS: $asset")

    if (direction.isNotEmpty()) {
        val strength = when {
            vote >= 75 && conf >= 0.8 -> "STRONG"
            vote >= 50 && conf >= 0.6 -> "MODERATE"
            else -> "WEAK"
        }
        sb.add("Signal Strength: $strength $direction")
    } else {
        sb.add("No active signal direction")
    }

    sb.add("Vote Score: ${vote.toInt()}% | Confidence: ${(conf * 100).toInt()}%")
    sb.add("Quality Tier: ${noUnderscores(tier)}")

    if (entry != null) {
        sb.add("Entry State: ${noUnderscores(entry.state)}")
        if (entry.style.isNotEmpty()) sb.add("Entry Style: ${noUnderscores(entry.style)}")
    }

    if (tradeParams != null && tradeParams.stop_loss > 0 && currentPrice != null) {
        val slDist = Math.abs(currentPrice - tradeParams.stop_loss) / currentPrice * 100
        val tpDist = Math.abs(tradeParams.take_profit - currentPrice) / currentPrice * 100
        val rr = if (slDist > 0) String.format("%.1f", tpDist / slDist) else "N/A"
        sb.add("Risk: ${String.format("%.2f", slDist)}% | Reward: ${String.format("%.2f", tpDist)}%")
        sb.add("Risk/Reward Ratio: 1:$rr")

        if (aiReality != null && aiReality.source != "NONE") {
            sb.add("AI Direction: ${noUnderscores(aiReality.direction).ifBlank { "—" }} | AI Score: ${(aiReality.score * 100).toInt()}% | AI State: ${noUnderscores(aiReality.state)}")
            val isAligned = direction.isBlank() || aiReality.direction.isBlank() || aiReality.direction == direction || aiReality.direction == "WAIT" || direction == "WAIT"
            sb.add("EA \u2713 AI Alignment: ${if (isAligned) "YES" else "NO \u2014 DIVERGENT"}")
        }

        if (selectedSmcZones.isNotEmpty()) {
            val detected = selectedSmcZones.filter { it in availableSmcZones }
            val absent = selectedSmcZones.filter { it !in availableSmcZones }
            sb.add("SMC Filter: ${selectedSmcZones.joinToString(", ")}")
            if (detected.isNotEmpty()) sb.add("  Detected: ${detected.joinToString(", ")}")
            if (absent.isNotEmpty()) sb.add("  Absent: ${absent.joinToString(", ")}")
        }

        val eaOk = vote >= 60 && conf >= 0.6 && tier != "NONE"
        val aiAligned = aiReality == null || aiReality.source == "NONE" ||
            aiReality.direction.isBlank() || direction.isBlank() ||
            aiReality.direction == direction || aiReality.direction == "WAIT" || direction == "WAIT"
        val aiScoreOk = aiReality == null || aiReality.source == "NONE" || aiReality.score >= 0.55
        val smcOk = selectedSmcZones.isEmpty() || selectedSmcZones.all { it in availableSmcZones }

        sb.add("VERDICT: " + when {
            eaOk && aiAligned && aiScoreOk && smcOk -> "QUALIFIED \u2014 EA + AI + SMC aligned, consider sim trade"
            eaOk && !aiAligned -> "BORDERLINE \u2014 EA qualifies but AI divergent, exercise caution"
            eaOk && !smcOk -> "BORDERLINE \u2014 EA qualifies but SMC patterns missing"
            eaOk && !aiScoreOk -> "BORDERLINE \u2014 EA qualifies but AI score weak"
            vote >= 40 || conf >= 0.4 -> "NOT QUALIFIED \u2014 Weak EA or misaligned, wait"
            else -> "NOT QUALIFIED \u2014 Skip"
        })
    } else {
        sb.add("No EA trade parameters available")
    }

    return sb.joinToString("\n")
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(label, color = SlateText, fontSize = 6.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun EaWriteupCard(reason: String) {
    val rows = remember(reason) { eaWriteupLines(reason) }
    // Pull the headline score out for a hero strip (e.g. CONFLUENCE SCORE 0.8000).
    val scoreRow = remember(rows) {
        rows.firstOrNull {
            val l = it.first ?: ""
            l.contains("CONFLUENCE SCORE") && it.second.toDoubleOrNull() != null
        }
    }
    val bodyRows = remember(rows, scoreRow) { rows.filter { it != scoreRow } }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFA78BFA))
                )
                Text(
                    "EA WRITE-UP",
                    color = Color(0xFFA78BFA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = InterFontFamily
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF8B5CF6).copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        "${rows.size} SIGNALS",
                        color = Color(0xFFC4B5FD),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Hero score strip
            if (scoreRow != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF8B5CF6).copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                scoreRow.first ?: "CONFLUENCE SCORE",
                                color = SlateText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                scoreRow.second,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                        }
                        val countRow = bodyRows.firstOrNull { (it.first ?: "").contains("CONFLUENCE COUNT") }
                        if (countRow != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "CONFLUENCE COUNT",
                                    color = SlateText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontFamily = InterFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    countRow.second,
                                    color = Color(0xFF22D3EE),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metric rows — one per line with divider, label left / pill value right
            val displayRows = bodyRows.filterNot {
                (it.first ?: "").contains("CONFLUENCE COUNT") && scoreRow != null
            }
            displayRows.forEachIndexed { index, (label, value) ->
                if (label != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        val vc = writeupValueColor(value)
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, vc.copy(alpha = 0.30f))
                        ) {
                            Text(
                                value.uppercase(),
                                color = vc,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (index < displayRows.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 0.5.dp
                        )
                    }
                } else {
                    // Free-form sentence — full-width body text with bullet
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.padding(top = 6.dp).size(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF22D3EE))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            value,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (index < displayRows.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.06f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(label, color = SlateText, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}

private data class AiReality(
    val direction: String,
    val score: Double,
    val confluence: Double?,
    val state: String,
    val rationale: String,
    val source: String // "DEPLOYMENTS" | "LIVE_FALLBACK" | "NONE"
)

/** Uppercased joined zone-context strings from the EA signal used to detect SMC zones. */
private fun smcContextString(signal: ASCSignalData?): String {
    if (signal == null) return ""
    return listOf(
        signal.zone_context_type,
        signal.zone_relationship,
        signal.current_zones,
        signal.target_zone,
        signal.pattern_detection?.detected_pattern.orEmpty(),
        signal.chart_panels?.smc_details.orEmpty(),
        signal.chart_panels?.smc_status.orEmpty()
    ).filter { it.isNotBlank() }.joinToString(" ").uppercase()
}

private fun resolveAiReality(
    asset: String,
    aiDecisions: List<FinalDecisionItem>,
    eaSignal: ASCSignalData?
): AiReality {
    val normalized = asset.trim().uppercase().replace("/", "").replace("_", "").replace(" ", "")
    if (normalized.isBlank()) return AiReality("", 0.0, null, "UNKNOWN", "", "NONE")

    // Backend deployments — real AI reality only
    val match = aiDecisions.firstOrNull {
        val a = (it.asset_1 ?: "").trim().uppercase().replace("/", "").replace("_", "").replace(" ", "").removeSuffix("M")
        a == normalized || a.startsWith(normalized)
    }
    if (match != null) {
        val dir = (match.final_trade_direction ?: match.journal_direction ?: "").uppercase()
        val score = match.final_trade_score ?: match.journal_score ?: 0.0
        val conf = match.confluence_score
        val state = match.final_trade_state ?: match.portfolio_deployment_bucket ?: "UNKNOWN"
        val rationale = match.final_trade_reason ?: match.portfolio_decision_reason ?: ""
        return AiReality(direction = dir, score = score, confluence = conf, state = state, rationale = rationale, source = "DEPLOYMENTS")
    }

    // EA signal data — the real EA reality for this asset
    if (eaSignal != null) {
        val dir = (eaSignal.chart_panel?.validator_direction ?: eaSignal.direction ?: "").uppercase()
        val score = eaSignal.validation?.confidence ?: eaSignal.chart_panel?.votes?.win_pct ?: 0.0
        val tier = eaSignal.chart_panel?.quality_tier?.takeIf { it.isNotBlank() && !it.equals("NONE", true) } ?: ""
        return AiReality(direction = dir, score = score, confluence = null, state = tier.ifBlank { "EA_SIGNAL" }, rationale = "From EA signal", source = "EA_SIGNAL")
    }

    return AiReality("", 0.0, null, "UNKNOWN", "", "NONE")
}

private data class SimPickerAsset(
    val symbol: String,
    val name: String,
    val type: String
)

private fun guessAssetType(symbol: String): String {
    val s = symbol.uppercase()
    return when {
        s == "USOIL" || s == "UKOIL" || s == "BRENT" || s.startsWith("XAU") || s.startsWith("XAG") || s.startsWith("XCU") || s.startsWith("XPT") -> "commodity"
        s == "DE30" || s == "DXY" || s == "JP225" || s == "STOXX50" || s == "UK100" || s == "US30" || s == "US500" || s == "USTEC" -> "index"
        s.contains("BTC") || s.contains("ETH") || s.contains("USDT") || s == "USDC" -> "crypto"
        s in listOf("AAPL", "AMZN", "META", "MSFT", "NFLX", "NVDA", "PYPL", "TSLA") -> "stock"
        else -> "forex"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetPickerSheet(
    assets: List<SimPickerAsset>,
    signalsByAsset: Map<String, ASCSignalData>,
    selectedAsset: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363A45))
            )
        },
        contentWindowInsets = { WindowInsets(0) },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Select asset", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("TARGET FOR AI SIMULATION", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val scroll = rememberScrollState()
            Column(modifier = Modifier.weight(1f).verticalScroll(scroll)) {
                assets.forEach { asset ->
                    val symbol = asset.symbol
                    val signal = signalsByAsset.entries.find { it.key.equals(symbol, ignoreCase = true) }?.value
                        ?: signalsByAsset.entries.firstOrNull {
                            it.key.uppercase().replace("/", "").replace("_", "").replace(" ", "").removeSuffix("M") ==
                                symbol.uppercase().replace("/", "").replace("_", "").replace(" ", "").removeSuffix("M")
                        }?.value
                    val isSelected = symbol.equals(selectedAsset, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) IndigoAccent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f))
                            .clickable { onSelect(symbol) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AssetIcon(
                                symbol = SymbolInfo(ticker = symbol.replace("/", "").removeSuffix("M").removeSuffix("m"), name = asset.name, type = asset.type),
                                size = 34
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(symbol, color = if (isSelected) IndigoAccent else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(asset.name, color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val panel = signal?.chart_panel
                                val flagLabel = when {
                                    signal == null -> "NO SIGNAL"
                                    panel?.validator_active != true -> "INACTIVE"
                                    else -> "ACTIVE"
                                }
                                val flagColor = if (flagLabel == "ACTIVE") EmeraldSuccess else SlateText
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(flagColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(flagLabel, color = flagColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}