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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.EALiveDataStore
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.ScannerSignalsStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.VigilanceNode
import com.asc.markets.logic.VigilanceNodeEngine
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.screens.dashboard.marketOverviewAssets
import com.asc.markets.ui.theme.*
import com.trading.app.components.AssetIcon
import com.trading.app.models.SymbolInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: ForexViewModel) {
    val scrollState = rememberScrollState()
    val liveAssets by EALiveDataStore.liveAssets.collectAsState()
    val signalsByAsset by EASignalLiveStore.signalsByAsset.collectAsState()
    val scannerSignals by ScannerSignalsStore.signals.collectAsState()
    val activeNodes = remember { mutableStateListOf<VigilanceNode>() }

    LaunchedEffect(Unit) {
        activeNodes.addAll(VigilanceNodeEngine.getActiveNodes())
    }

    fun registerNode(node: VigilanceNode) {
        activeNodes.add(0, node)
        viewModel.registerVigilanceNode(node, prefix = "Alert armed")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("\u26A1", fontSize = 20.sp)
                }
            }
            Column {
                Text("VIGILANCE SETUP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("EA-POWERED ALERT MONITORING", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

        // ─── EA STATUS ───
        val connected by EASignalLiveStore.isConnected.collectAsState()
        val eaConnected by EALiveDataStore.isConnected.collectAsState()
        InfoBox {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (connected && eaConnected) EmeraldSuccess else Color(0xFFEF4444), RoundedCornerShape(4.dp))
                    )
                    Text("EA FEED", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                }
                Text(
                    if (connected && eaConnected) "LIVE \u2022 ${liveAssets.size} assets" else "OFFLINE",
                    color = if (connected && eaConnected) EmeraldSuccess else Color(0xFFEF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── ASSET SELECTOR ───
        var selectedAsset by remember { mutableStateOf("") }
        val liveSymbols = liveAssets.map { it.symbol }.distinct()
        val pickerAssets = remember(liveSymbols) {
            val canonical = marketOverviewAssets()
            val known = canonical.map { normalizeSignalKey(it.symbol) }.toSet()
            val extras = liveSymbols.filter { s -> normalizeSignalKey(s) !in known }.map { s ->
                PickerAsset(symbol = s, name = s, type = guessAssetType(s))
            }
            canonical.map { a ->
                PickerAsset(symbol = a.symbol, name = a.name, type = assetTypeFor(a.category))
            } + extras
        }
        val assetList = pickerAssets.map { it.symbol }

        LaunchedEffect(assetList) {
            if (selectedAsset.isEmpty() && assetList.isNotEmpty()) {
                selectedAsset = assetList.first()
            }
        }

        // Keep this asset's EA write-up streaming so the live panel — and the
        // vigilance monitor — always have fresh data while this page is open.
        LaunchedEffect(selectedAsset, eaConnected) {
            if (selectedAsset.isNotEmpty() && eaConnected) {
                EASignalLiveStore.requestSignal(selectedAsset)
            }
        }

        // Warm the EA feed for every canonical asset so the picker and live
        // panel show vote / direction data even before an asset is selected.
        LaunchedEffect(assetList, eaConnected) {
            if (eaConnected) {
                assetList.forEach { EASignalLiveStore.requestSignal(it) }
            }
        }

        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TARGET ASSET", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)

                var showAssetPicker by remember { mutableStateOf(false) }
                Box {
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (showAssetPicker) {
                    AssetPickerSheet(
                        assets = pickerAssets,
                        signalsByAsset = signalsByAsset,
                        selectedAsset = selectedAsset,
                        onSelect = { asset ->
                            selectedAsset = asset
                            showAssetPicker = false
                        },
                        onDismiss = { showAssetPicker = false }
                    )
                }

                // Show current EA data for selected asset
                if (selectedAsset.isNotEmpty()) {
                    val signal = findSignalFor(signalsByAsset, selectedAsset)
                    val scanKey = selectedAsset.uppercase()
                        .replace("/", "").replace("-", "").replace("_", "")
                        .replace(" ", "").replace(".", "").removeSuffix("M")
                    val scanner = scannerSignals.firstOrNull {
                        it.asset.uppercase().replace("/", "").removeSuffix("M") == scanKey
                    }

                    if (signal != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Real AI score: signal.confidence (0-1) + direction, not the
                        // validator's internal confidence. Vote normalized (EA sends 0-100).
                        val votePct = normVotePct(signal.chart_panel?.votes?.win_pct ?: 0.0)
                        val aiConfPct = (signal.confidence * 100).toInt().coerceIn(0, 100)
                        val dirLabel = when (signal.direction.uppercase()) {
                            "BUY" -> "LONG"
                            "SELL" -> "SHORT"
                            else -> signal.direction.uppercase().ifBlank { "---" }
                        }
                        val tier = signal.chart_panel?.quality_tier ?: "NONE"

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniStat("VOTE", "$votePct%", if (votePct >= 50) EmeraldSuccess else Color.Gray, Modifier.weight(1f))
                            MiniStat("AI CONF", "$aiConfPct%", if (aiConfPct >= 60) EmeraldSuccess else Color.Gray, Modifier.weight(1f))
                            MiniStat("DIR", dirLabel, if (dirLabel == "LONG") EmeraldSuccess else if (dirLabel == "SHORT") RoseError else Color.Gray, Modifier.weight(1f))
                            MiniStat("TIER", tier.replace("_", " "), when (tier.uppercase()) {
                                "ELITE" -> EmeraldSuccess
                                "STRONG", "HIGH", "VALID" -> IndigoAccent
                                else -> Color.Gray
                            }, Modifier.weight(1f))
                        }

                        // Live scanner line (P(T) + age) when the MT5 scanner has this asset
                        if (scanner != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SCANNER", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                val scConf = if (scanner.confidence > 1.0) scanner.confidence.toInt().coerceIn(0, 100) else (scanner.confidence * 100).toInt().coerceIn(0, 100)
                                Text(
                                    "${scanner.direction.uppercase()} • P $scConf% • ${scanner.age}s ago",
                                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                                )
                            }
                        }

                        // How the scores work: live contributor breakdown
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("LIVE SCORE BREAKDOWN", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(8.dp))
                        val votes = signal.chart_panel?.votes
                        ScoreBar("SMC bull/bear", votes?.smc_bull ?: 0, (votes?.smc_bull ?: 0) + (votes?.smc_bear ?: 0))
                        ScoreBar("AI bull/bear", votes?.ai_bull ?: 0, (votes?.ai_bull ?: 0) + (votes?.ai_bear ?: 0))
                        ScoreBar("Regime ${(signal.regime?.trend ?: "").replace("_", " ")}", (signal.regime?.score?.times(100))?.toInt() ?: 0, 100)
                        ScoreBar("Volatility ${(signal.volatility?.state ?: "").replace("_", " ")}", (signal.volatility?.score?.times(100))?.toInt() ?: 0, 100)
                        ScoreBar("Structure ${(signal.structure?.bias ?: "").replace("_", " ")}", (signal.structure?.score?.times(100))?.toInt() ?: 0, 100)
                        ScoreBar("Indicators ${(signal.indicators?.bias ?: "").replace("_", " ")}", (signal.indicators?.score?.times(100))?.toInt() ?: 0, 100)
                        ScoreBar("Confluence ${(signal.confluence?.state ?: "").replace("_", " ")}", (signal.confluence?.score?.times(100))?.toInt() ?: 0, 100)
                        val validatorActive = signal.chart_panel?.validator_active == true
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Validator ${(signal.chart_panel?.validator_direction ?: "").replace("_", " ")}${if (validatorActive) " • ON" else " • OFF"}",
                                color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily
                            )
                            val pwin = signal.chart_panel?.validator_pwin ?: 0.0
                            val pwinPct = if (pwin > 1.0) pwin.toInt().coerceIn(0, 100) else (pwin * 100).toInt().coerceIn(0, 100)
                            Text("$pwinPct% win", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }

                        // Live SMC state — mirrors what the chart stream sees
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("LIVE SMC STATE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(8.dp))
                        val liq = signal.liquidity
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmcStateChip("FVG", (liq?.fvg_bull == true || liq?.fvg_bear == true), Modifier.weight(1f))
                            SmcStateChip("BOS", (liq?.bos_bull == true || liq?.bos_bear == true), Modifier.weight(1f))
                            SmcStateChip("SWEEP", (liq?.sweep_high == true || liq?.sweep_low == true), Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── TRIGGER CONDITIONS ───
        var voteThreshold by remember { mutableStateOf(40) }
        var confThreshold by remember { mutableStateOf(50) }
        var eaSide by remember { mutableStateOf("ANY") }
        var aiSide by remember { mutableStateOf("ANY") }
        var filterDirection by remember { mutableStateOf("ANY") }
        var filterTier by remember { mutableStateOf("ANY") }
        var requireFvg by remember { mutableStateOf(false) }
        var requireBos by remember { mutableStateOf(false) }
        var requireSweep by remember { mutableStateOf(false) }
        var requirePd by remember { mutableStateOf(false) }
        var pTradeThreshold by remember { mutableStateOf(0) }
        var combineScoreThreshold by remember { mutableStateOf(0) }
        var validatorFilter by remember { mutableStateOf("ANY") }
        var cooldownMinutes by remember { mutableStateOf(30) }

        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("1", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("TRIGGER CONDITIONS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp), modifier = Modifier.wrapContentSize()) {
                        Text("EA-BASED", color = IndigoAccent, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp))
                    }
                }

                // Vote Threshold
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("EA SCORE THRESHOLD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("${voteThreshold}%", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ANY" to IndigoAccent, "LONG" to EmeraldSuccess, "SHORT" to RoseError).forEach { (side, color) ->
                            val isSelected = eaSide == side
                            Surface(
                                modifier = Modifier.weight(1f).height(30.dp).clickable { eaSide = side },
                                color = if (isSelected) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(side, color = if (isSelected) color else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = voteThreshold.toFloat(),
                        onValueChange = { voteThreshold = it.toInt() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                        Text("100%", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                    }
                }

                // AI Confidence Threshold
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI SCORE THRESHOLD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("${confThreshold}%", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ANY" to IndigoAccent, "LONG" to EmeraldSuccess, "SHORT" to RoseError).forEach { (side, color) ->
                            val isSelected = aiSide == side
                            Surface(
                                modifier = Modifier.weight(1f).height(30.dp).clickable { aiSide = side },
                                color = if (isSelected) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(side, color = if (isSelected) color else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = confThreshold.toFloat(),
                        onValueChange = { confThreshold = it.toInt() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                        Text("100%", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                    }
                }

                // P TRADE Threshold (scanner)
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("P TRADE THRESHOLD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text(if (pTradeThreshold > 0) "${pTradeThreshold}%" else "OFF", color = if (pTradeThreshold > 0) IndigoAccent else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = pTradeThreshold.toFloat(),
                        onValueChange = { pTradeThreshold = it.toInt() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("OFF", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                        Text("100%", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                    }
                }

                // Validator State Filter
                Column {
                    Text("VALIDATOR STATE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ANY" to IndigoAccent, "LONG" to EmeraldSuccess, "SHORT" to RoseError, "INACTIVE" to SlateText).forEach { (state, color) ->
                            val isSelected = validatorFilter == state
                            Surface(
                                modifier = Modifier.weight(1f).height(36.dp).clickable { validatorFilter = state },
                                color = if (isSelected) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(state, color = if (isSelected) color else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                    Text("LONG/SHORT = validator active with that direction • INACTIVE = validator off", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(top = 4.dp))
                }

                // COMBINE SCORE Threshold
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("COMBINE SCORE THRESHOLD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text(if (combineScoreThreshold > 0) "${combineScoreThreshold}%" else "OFF", color = if (combineScoreThreshold > 0) IndigoAccent else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = combineScoreThreshold.toFloat(),
                        onValueChange = { combineScoreThreshold = it.toInt() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("OFF", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                        Text("100%", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                    }
                }

                // Direction Filter
                Column {
                    Text("VALIDATOR DIRECTION", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ANY" to IndigoAccent, "LONG" to EmeraldSuccess, "SHORT" to RoseError).forEach { (dir, color) ->
                            val isSelected = filterDirection == dir
                            Surface(
                                modifier = Modifier.weight(1f).height(40.dp).clickable { filterDirection = dir },
                                color = if (isSelected) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(dir, color = if (isSelected) color else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                }

                // Quality Tier Filter
                Column {
                    Text("QUALITY TIER", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ANY", "ELITE", "HIGH", "STANDARD").forEach { tier ->
                            val isSelected = filterTier == tier
                            val tierColor = when (tier) {
                                "ELITE" -> EmeraldSuccess
                                "HIGH" -> IndigoAccent
                                "STANDARD" -> Color(0xFFFFA500)
                                else -> Color.Gray
                            }
                            Surface(
                                modifier = Modifier.weight(1f).height(36.dp).clickable { filterTier = tier },
                                color = if (isSelected) tierColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) tierColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(tier, color = if (isSelected) tierColor else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                }

                // SMC Flags
                Column {
                    Text("SMC FLAGS (OPTIONAL)", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SMCFlagRow("Fair Value Gap (FVG)", "Bullish or Bearish FVG detected", requireFvg) { requireFvg = it }
                        SMCFlagRow("Break of Structure (BOS)", "Bullish or Bearish BOS confirmed", requireBos) { requireBos = it }
                        SMCFlagRow("Liquidity Sweep", "High or Low sweep detected", requireSweep) { requireSweep = it }
                        SMCFlagRow("Premium / Discount (PD)", "Price trading in a premium or discount zone", requirePd) { requirePd = it }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── COOLDOWN ───
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("2", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("COOLDOWN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = cooldownMinutes.toFloat(),
                            onValueChange = { cooldownMinutes = it.toInt() },
                            valueRange = 5f..120f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${cooldownMinutes}m", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("5 min", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                        Text("120 min", color = Color.Gray, fontSize = 8.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── DEPLOY ───
        Button(
            onClick = {
                val node = VigilanceNodeEngine.createEAAlert(
                    pair = selectedAsset,
                    voteThreshold = voteThreshold,
                    confidenceThreshold = confThreshold,
                    directionFilter = filterDirection,
                    qualityTierFilter = filterTier,
                    requireFvg = requireFvg,
                    requireBos = requireBos,
                    requireSweep = requireSweep,
                    requirePd = requirePd,
                    pTradeThreshold = pTradeThreshold,
                    combineScoreThreshold = combineScoreThreshold,
                    validatorFilter = validatorFilter,
                    eaSide = eaSide,
                    aiSide = aiSide,
                    cooldownMinutes = cooldownMinutes
                )
                registerNode(node)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B)),
            enabled = selectedAsset.isNotEmpty()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\u25C1", fontSize = 16.sp, color = Color.White)
                Text("DEPLOY VIGILANCE NODE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
            }
        }

        Text(
            "MONITORS EA SIGNALS IN REAL-TIME \u2022 FIRES WHEN CONDITIONS MATCH",
            color = SlateText,
            fontSize = 8.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ─── ACTIVE NODES ───
        if (activeNodes.isNotEmpty()) {
            NodesSectionHeader("ACTIVE NODES")
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = EmeraldSuccess.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(EmeraldSuccess, RoundedCornerShape(4.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${activeNodes.size} NODE${if (activeNodes.size > 1) "S" else ""} ACTIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("Monitoring EA feed for trigger conditions", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            activeNodes.forEach { node ->
                ActiveNodeCard(node, signalsByAsset, scannerSignals) { /* breakdown not shown inline */ }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// EA sends win_pct 0-100 (e.g. 98.1); normalize anything 0-1 to percent as well.
private fun normVotePct(raw: Double): Int =
    if (raw > 1.0) raw.toInt().coerceIn(0, 100) else (raw * 100).toInt().coerceIn(0, 100)

/** Asset entry in the picker: symbol + display name + icon type (matches the Market Overview page). */
private data class PickerAsset(
    val symbol: String,
    val name: String,
    val type: String
)

/** Map canonical category to the icon type used by AssetIcon, exactly as the Markets page does. */
private fun assetTypeFor(category: com.asc.markets.data.MarketCategory): String = when (category) {
    com.asc.markets.data.MarketCategory.FOREX -> "forex"
    com.asc.markets.data.MarketCategory.CRYPTO -> "crypto"
    com.asc.markets.data.MarketCategory.COMMODITIES -> "commodity"
    com.asc.markets.data.MarketCategory.INDICES -> "index"
    com.asc.markets.data.MarketCategory.STOCK -> "stock"
    com.asc.markets.data.MarketCategory.BONDS -> "bond"
    com.asc.markets.data.MarketCategory.FUTURES -> "futures"
}

/** Best-effort icon type for live EA symbols that aren't in the canonical trained list. */
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

/** Normalize an asset symbol to match live EA signal keys (e.g. EURUSD ↔ EURUSDm). */
private fun normalizeSignalKey(symbol: String): String = symbol.uppercase()
    .replace("/", "").replace("-", "").replace("_", "")
    .replace(" ", "").replace(".", "").removeSuffix("M")

/** Find a live signal for `symbol`, tolerating the EA's trailing-M / separator variants. */
private fun findSignalFor(
    signalsByAsset: Map<String, com.asc.markets.data.ASCSignalData>,
    symbol: String
): com.asc.markets.data.ASCSignalData? {
    val exact = signalsByAsset.entries.find { it.key.equals(symbol, ignoreCase = true) }
    if (exact != null) return exact.value
    val key = normalizeSignalKey(symbol)
    return signalsByAsset.entries.firstOrNull { normalizeSignalKey(it.key) == key }?.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetPickerSheet(
    assets: List<PickerAsset>,
    signalsByAsset: Map<String, com.asc.markets.data.ASCSignalData>,
    selectedAsset: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

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
        modifier = Modifier
            .fillMaxHeight(0.95f)
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
                    Text(
                        text = "Select asset",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "TARGET FOR VIGILANCE",
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = InterFontFamily
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
            ) {
                assets.forEach { asset ->
                    val symbol = asset.symbol
                    val signal = findSignalFor(signalsByAsset, symbol)
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
                                // Same signal flag chips as the Market Overview page.
                                val panel = signal?.chart_panel
                                val liq = signal?.liquidity
                                val flagChips = buildList {
                                    val vDir = panel?.validator_direction
                                    val vActive = panel?.validator_active == true
                                    if (vActive && !vDir.isNullOrBlank()) {
                                        add(vDir.uppercase() to when (vDir.uppercase()) {
                                            "LONG", "BUY" -> EmeraldSuccess
                                            "SHORT", "SELL" -> RoseError
                                            else -> SlateText
                                        })
                                    } else {
                                        add("INACTIVE" to SlateText)
                                    }
                                    val tier = panel?.quality_tier
                                    if (!tier.isNullOrBlank() && tier != "NONE") {
                                        add(tier to when (tier) {
                                            "ELITE" -> EmeraldSuccess
                                            "STRONG" -> Color(0xFF60A5FA)
                                            "VALID" -> Color(0xFFF59E0B)
                                            "FILTERED" -> RoseError
                                            else -> SlateText
                                        })
                                    }
                                    if (liq != null) {
                                        if (liq.fvg_bull || liq.fvg_bear) add("FVG" to Color(0xFF60A5FA))
                                        if (liq.bos_bull || liq.bos_bear) add("BOS" to Color(0xFFA78BFA))
                                        if (liq.sweep_high || liq.sweep_low) add("SWEEP" to Color(0xFFF59E0B))
                                    }
                                }
                                if (flagChips.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        flagChips.take(4).forEach { (label, color) ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(color.copy(alpha = 0.15f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontFamily = InterFontFamily)
                                            }
                                        }
                                    }
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

@Composable
private fun ScoreBar(label: String, value: Int, max: Int) {
    val frac = if (max > 0) (value.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(if (max == 100) "$value%" else "$value/$max", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = frac,
            color = IndigoAccent,
            trackColor = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth().height(4.dp)
        )
    }
}

@Composable
private fun SmcStateChip(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val color = if (active) EmeraldSuccess else Color.Gray
    Surface(
        color = color.copy(alpha = if (active) 0.15f else 0.06f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = if (active) 0.45f else 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).background(color, RoundedCornerShape(4.dp)))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(label, color = SlateText, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun SMCFlagRow(label: String, description: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (enabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(description, color = SlateText, fontSize = 8.sp, fontFamily = InterFontFamily)
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (enabled) IndigoAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(4.dp)
                )
                .border(1.dp, if (enabled) IndigoAccent else Color.DarkGray, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun ActiveNodeCard(
    node: VigilanceNode,
    signalsByAsset: Map<String, com.asc.markets.data.ASCSignalData> = emptyMap(),
    scannerSignals: List<com.asc.markets.data.ScannerSignal> = emptyList(),
    onShowBreakdown: (String) -> Unit
) {
    val strengthColor = when (node.strength) {
        "STRONG" -> EmeraldSuccess
        "MEDIUM" -> Color(0xFFFFA500)
        else -> RoseError
    }

    // Live match status: evaluate this node's conditions against its asset's
    // current EA signal so the user sees exactly why it has / hasn't fired.
    val liveSignal = remember(node.pair, signalsByAsset) {
        signalsByAsset.entries.find {
            it.key.equals(node.pair, ignoreCase = true) ||
                it.key.replace("/", "").removeSuffix("M").equals(
                    node.pair.uppercase().replace("/", "").replace("-", "")
                        .replace("_", "").replace(" ", "").removeSuffix("M"),
                    ignoreCase = true
                )
        }?.value
    }
    val checks = remember(node.id, liveSignal) {
        if (node.alertType != "EA_LIVE" || liveSignal == null) emptyList()
        else {
            val dir = when (liveSignal.direction.uppercase()) {
                "BUY" -> "LONG"
                "SELL" -> "SHORT"
                else -> liveSignal.direction.uppercase()
            }
            val rawVote = liveSignal.chart_panel?.votes?.win_pct ?: 0.0
            val rawConf = liveSignal.confidence
            val liq = liveSignal.liquidity
            val rawPwin = liveSignal.chart_panel?.validator_pwin ?: 0.0
            val pwinPct = (if (rawPwin > 1.0) rawPwin else rawPwin * 100.0).toInt()
            val scannerSig = scannerSignals.firstOrNull {
                it.asset.uppercase().replace("/", "").removeSuffix("M") == normalizeSignalKey(liveSignal.asset.ifBlank { node.pair })
            }
            val pTradePct = scannerSig?.pTrade?.let { if (it > 1.0) it else it * 100.0 }?.toInt() ?: -1
            val pdActive = liveSignal.zone_context_type.contains("PREMIUM", true) ||
                liveSignal.zone_context_type.contains("DISCOUNT", true) ||
                liveSignal.target_zone.contains("PREMIUM", true) ||
                liveSignal.target_zone.contains("DISCOUNT", true) ||
                liveSignal.chart_panels?.smc_details.orEmpty().contains("PREMIUM", true) ||
                liveSignal.chart_panels?.smc_details.orEmpty().contains("DISCOUNT", true) ||
                liveSignal.chart_panels?.smc_status.orEmpty().contains("PREMIUM", true) ||
                liveSignal.chart_panels?.smc_status.orEmpty().contains("DISCOUNT", true)
            VigilanceNodeEngine.checkEANode(
                nodeId = node.id,
                votePct = if (rawVote > 1.0) rawVote else rawVote * 100.0,
                confidence = if (rawConf > 1.0) (rawConf / 100.0).coerceIn(0.0, 1.0) else rawConf.coerceIn(0.0, 1.0),
                direction = dir,
                qualityTier = liveSignal.chart_panel?.quality_tier?.ifBlank { "NONE" } ?: "NONE",
                fvgBull = liq?.fvg_bull == true,
                fvgBear = liq?.fvg_bear == true,
                bosBull = liq?.bos_bull == true,
                bosBear = liq?.bos_bear == true,
                sweepHigh = liq?.sweep_high == true,
                sweepLow = liq?.sweep_low == true,
                pdActive = pdActive,
                pTradePct = pTradePct.toDouble(),
                pwinPct = pwinPct.toDouble(),
                validatorActive = liveSignal.chart_panel?.validator_active == true,
                validatorAllowed = liveSignal.chart_panel?.validator_allowed == true,
                validatorDirection = liveSignal.chart_panel?.validator_direction ?: ""
            )
        }
    }
    val armed = VigilanceNodeEngine.canTriggerNode(node.id)
    val allPass = checks.isNotEmpty() && checks.all { it.passed }

    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, strengthColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(node.pair, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        if (node.isActive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(strengthColor, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(node.description, color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                }

                Surface(
                    color = strengthColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        node.strength.replace("_", " "),
                        color = strengthColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(6.dp, 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SCORE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("${node.confidenceScore}%", color = strengthColor, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Text("${node.cooldownMinutes}m cooldown", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
            }

            // Live match status: which conditions pass/fail against the live signal
            if (node.alertType == "EA_LIVE") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LIVE MATCH", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    val stateLabel = when {
                        liveSignal == null -> "NO DATA"
                        !armed -> "COOLDOWN"
                        allPass -> "MATCH"
                        else -> "WAITING"
                    }
                    val stateColor = when (stateLabel) {
                        "MATCH" -> EmeraldSuccess
                        "COOLDOWN" -> Color(0xFFFFA500)
                        "NO DATA" -> Color.Gray
                        else -> SlateText
                    }
                    Text(stateLabel, color = stateColor, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (liveSignal == null) {
                    Text(
                        "No live EA signal for ${node.pair} yet — open the asset or wait for the feed.",
                        color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        checks.forEach { check ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    if (check.passed) "✓" else "✗",
                                    color = if (check.passed) EmeraldSuccess else RoseError,
                                    fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily
                                )
                                Text(check.label, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodesSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF999999),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 12.dp)
    )
}

@Composable
fun ScoringBreakdownPanel(nodeId: String, onDismiss: () -> Unit) {
    val breakdown = VigilanceNodeEngine.getScoringBreakdown(nodeId)

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SCORING BREAKDOWN", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            for ((factor, points) in breakdown) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(factor, color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Text("+$points pts", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(color = IndigoAccent.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TOTAL SCORE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("${breakdown.values.sum()}%", color = IndigoAccent, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}
