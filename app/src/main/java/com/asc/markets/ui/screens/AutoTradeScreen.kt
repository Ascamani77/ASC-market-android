package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.AssetParams
import com.asc.markets.logic.AutoTradeConfig
import com.asc.markets.logic.AutoTradeEngine
import com.asc.markets.logic.AutoTradeSignalSource
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.AutoTradePositionStrip
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.MultiAssetPickerSheet
import com.asc.markets.ui.components.PickableAsset
import com.asc.markets.ui.screens.dashboard.marketOverviewAssets
import com.asc.markets.data.MarketCategory
import com.asc.markets.ui.theme.*
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Composable
fun AutoTradeScreen(viewModel: ForexViewModel) {
    val scrollState = rememberScrollState()

    // Canonical 44 trained assets (same list as the Markets / AI Simulation pages).
    val allAssets = remember { marketOverviewAssets() }

    // Assets converted into the picker payload with icon type per category.
    val pickerAssets = remember(allAssets) {
        allAssets.map { trained ->
            PickableAsset(
                symbol = trained.symbol,
                name = trained.name,
                type = when (trained.category) {
                    MarketCategory.FOREX -> "forex"
                    MarketCategory.CRYPTO -> "crypto"
                    MarketCategory.COMMODITIES -> "commodity"
                    MarketCategory.INDICES -> "index"
                    MarketCategory.STOCK -> "stock"
                    MarketCategory.BONDS -> "bond"
                    MarketCategory.FUTURES -> "futures"
                }
            )
        }
    }

    val ctx = LocalContext.current
    val isAutoTradeEnabled by AutoTradeEngine.isRunning.collectAsState()
    val engineAccount by AutoTradeEngine.account.collectAsState()
    val enginePositions by AutoTradeEngine.positions.collectAsState()
    val autoOpenTickets by AutoTradeEngine.autoOpenTickets.collectAsState()
    val engineLog by AutoTradeEngine.log.collectAsState()
    val selectedAssets = rememberSaveable(saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf<String>() }
    var showAssetPicker by remember { mutableStateOf(false) }
    val enabledEntryModels = remember { mutableStateSetOf<EntryModel>() }
    val enabledSMC = remember { mutableStateSetOf<SMCTechnique>() }
    val enabledSessions = remember { mutableStateSetOf<MarketSession>() }
    var slTpRatio by remember { mutableStateOf(SlTpRatio.RATIO_1_2) }
    var minEAScore by remember { mutableStateOf(65) }
    var minAiScore by remember { mutableStateOf(70) }
    var minConfidence by remember { mutableStateOf(65) }
    var riskPerTrade by remember { mutableStateOf(1.0) }
    var maxDailyTrades by remember { mutableStateOf(5) }
    var maxOpenPositions by remember { mutableStateOf(3) }
    var newsFilter by remember { mutableStateOf(true) }
    var spreadFilter by remember { mutableStateOf(true) }
    var maxSpreadPips by remember { mutableStateOf(3.0) }
    var signalSource by remember { mutableStateOf(AutoTradeSignalSource.COMBINED) }
    val perAssetParams = remember { mutableStateMapOf<String, AssetParams>() }
    var editingAsset by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (enabledSMC.isEmpty()) {
            enabledSMC.add(SMCTechnique.PD)
            enabledSMC.add(SMCTechnique.FVG)
            enabledSMC.add(SMCTechnique.OB)
        }
        if (enabledEntryModels.isEmpty()) {
            enabledEntryModels.add(EntryModel.AUTO_CONFLUENCE)
        }
        if (enabledSessions.isEmpty()) {
            enabledSessions.add(MarketSession.LONDON)
            enabledSessions.add(MarketSession.NEW_YORK)
        }
    }

    val buildConfig: () -> AutoTradeConfig = {
        AutoTradeConfig(
            assets = selectedAssets.toSet(),
            minEAScore = minEAScore,
            minAiScore = minAiScore,
            minConfidence = minConfidence,
            riskPerTradePct = riskPerTrade,
            maxDailyTrades = maxDailyTrades,
            maxOpenPositions = maxOpenPositions,
            newsFilter = newsFilter,
            spreadFilter = spreadFilter,
            maxSpreadPips = maxSpreadPips,
            entryModels = enabledEntryModels.map { it.displayName }.toSet(),
            smcTechniques = (enabledSMC + SMCTechnique.PD + SMCTechnique.FVG + SMCTechnique.OB).map { it.code }.toSet(),
            sessions = enabledSessions.map { it.displayName }.toSet(),
            rrMultiplier = slTpRatio.value.toDoubleOrNull() ?: -1.0,
            signalSource = signalSource,
            perAsset = perAssetParams.toMap()
        )
    }

    val defaultParams: () -> AssetParams = {
        AssetParams(
            entryModels = enabledEntryModels.map { it.displayName }.toSet(),
            smcTechniques = (enabledSMC + SMCTechnique.PD + SMCTechnique.FVG + SMCTechnique.OB).map { it.code }.toSet(),
            sessions = enabledSessions.map { it.displayName }.toSet(),
            rrMultiplier = slTpRatio.value.toDoubleOrNull() ?: -1.0,
            minEAScore = minEAScore,
            minAiScore = minAiScore,
            minConfidence = minConfidence,
            riskPerTradePct = riskPerTrade,
            newsFilter = newsFilter,
            spreadFilter = spreadFilter,
            maxSpreadPips = maxSpreadPips
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header (fixed — does not scroll)
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (isAutoTradeEnabled) EmeraldSuccess.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = if (isAutoTradeEnabled) EmeraldSuccess else SlateText, modifier = Modifier.size(24.dp))
                }
            }
            Column {
                Text("AUTO TRADE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("EA AUTOMATED EXECUTION ENGINE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {

        // Master Toggle
        Surface(
            color = if (isAutoTradeEnabled) EmeraldSuccess.copy(alpha = 0.08f) else Color(0xFFEF4444).copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isAutoTradeEnabled) EmeraldSuccess.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AUTO TRADE ENGINE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Text(
                        if (isAutoTradeEnabled) {
                            if (selectedAssets.isNotEmpty()) {
                                val names = selectedAssets.joinToString(", ")
                                if (selectedAssets.size <= 4) "ACTIVE — Monitoring $names"
                                else "ACTIVE — Monitoring ${selectedAssets.size} assets ($names)"
                            } else {
                                "ACTIVE — Monitoring"
                            }
                        } else {
                            "INACTIVE — Use START AUTO TRADE below"
                        },
                        color = if (isAutoTradeEnabled) EmeraldSuccess else Color(0xFFEF4444),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. ASSET SELECTION — Bottom sheet (same style as AI Simulation page)
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ASSETS TO AUTO TRADE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Text("${selectedAssets.size} OF ${pickerAssets.size} SELECTED", color = if (selectedAssets.isNotEmpty()) EmeraldSuccess else SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }

                Box {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().clickable { showAssetPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedAssets.isEmpty()) {
                                Text("Tap to select assets...", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                            } else if (selectedAssets.size <= 3) {
                                Text(selectedAssets.joinToString(", "), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
                            } else {
                                Text("${selectedAssets.size} assets selected", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SlateText, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (showAssetPicker) {
                    MultiAssetPickerSheet(
                        assets = pickerAssets,
                        selected = selectedAssets.toSet(),
                        onToggleAsset = { symbol ->
                            if (symbol in selectedAssets) selectedAssets.remove(symbol) else selectedAssets.add(symbol)
                        },
                        onSelectAll = { pickerAssets.forEach { selectedAssets.add(it.symbol) } },
                        onClearAll = { selectedAssets.clear() },
                        onDismiss = { showAssetPicker = false },
                        onDone = { showAssetPicker = false },
                        title = "Select assets",
                        subtitle = "TARGETS FOR AUTO TRADE"
                    )
                }

                if (selectedAssets.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selectedAssets.toList()) { symbol ->
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(symbol, color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = EmeraldSuccess, modifier = Modifier.size(14.dp).clickable { selectedAssets.remove(symbol) })
                                }
                            }
                        }
                    }
                }

            }
        }

        editingAsset?.let { symbol ->
            AssetParamsEditorDialog(
                asset = symbol,
                initial = perAssetParams[symbol] ?: defaultParams(),
                onSave = { perAssetParams[symbol] = it },
                onReset = {
                    perAssetParams.remove(symbol)
                    editingAsset = null
                },
                onDismiss = { editingAsset = null }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. SIGNAL SOURCE — EA only / AI only / combined score
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SIGNAL SOURCE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Text(signalSourceLabel(signalSource), color = sourceColor(signalSource), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AutoTradeSignalSource.values().forEach { source ->
                        val color = sourceColor(source)
                        val active = signalSource == source
                        Surface(
                            color = if (active) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (active) color else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f).clickable { signalSource = source }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (active) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (active) color else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(signalSourceLabel(source), color = if (active) color else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                                Text(sourceDescription(source), color = SlateText, fontSize = 8.sp, fontFamily = InterFontFamily, maxLines = 3)
                            }
                        }
                    }
                }

                Text(
                    when (signalSource) {
                        AutoTradeSignalSource.EA -> "Trades on MT5 EA validator vote + confidence only."
                        AutoTradeSignalSource.AI -> "Trades on backend AI journal score + direction only. Requires the AI backend (8003/8001)."
                        AutoTradeSignalSource.COMBINED -> "Both signals must exist, directions must agree, and the average score passes the threshold."
                    },
                    color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. ENTRY MODEL — Multi-select chips
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ENTRY MODEL", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    if (enabledEntryModels.isNotEmpty()) {
                        Text("${enabledEntryModels.size} active", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EntryModel.values().toList()) { model ->
                        val active = model in enabledEntryModels
                        Surface(
                            color = if (active) IndigoAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) IndigoAccent else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.clickable {
                                if (active) enabledEntryModels.remove(model) else enabledEntryModels.add(model)
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).width(130.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (active) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (active) IndigoAccent else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(model.displayName, color = if (active) IndigoAccent else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                                Text(model.description, color = SlateText, fontSize = 8.sp, fontFamily = InterFontFamily, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. SMC TECHNIQUES
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SMC TECHNIQUES", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Text("Auto uses: PD, FVG, OB", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(SMCTechnique.values().toList()) { tech ->
                        val isAutoCore = tech in setOf(SMCTechnique.PD, SMCTechnique.FVG, SMCTechnique.OB)
                        val isEnabled = tech in enabledSMC || isAutoCore
                        Surface(
                            color = if (isEnabled) tech.color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isEnabled) tech.color else Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier
                                .width(140.dp)
                                .height(100.dp)
                                .clickable {
                                    if (!isAutoCore) {
                                        if (tech in enabledSMC) enabledSMC.remove(tech) else enabledSMC.add(tech)
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(tech.code, color = tech.color, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text(tech.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                Text(tech.shortDesc, color = SlateText, fontSize = 8.sp, textAlign = TextAlign.Center, fontFamily = InterFontFamily, maxLines = 2)
                                if (isAutoCore) {
                                    Text("AUTO CORE", color = IndigoAccent, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                                } else if (isEnabled) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = tech.color, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. MARKET SESSION — Multi-select chips
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MARKET SESSION", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    if (enabledSessions.isNotEmpty()) {
                        Text("${enabledSessions.size} active", color = Color(0xFFFFA500), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MarketSession.values().toList()) { session ->
                        val active = session in enabledSessions
                        Surface(
                            color = if (active) Color(0xFFFFA500).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) Color(0xFFFFA500) else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.clickable {
                                if (active) enabledSessions.remove(session) else enabledSessions.add(session)
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).width(140.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (active) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (active) Color(0xFFFFA500) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(session.displayName, color = if (active) Color(0xFFFFA500) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                                Text(session.description, color = SlateText, fontSize = 8.sp, fontFamily = InterFontFamily, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. SL/TP RATIO
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("RISK:REWARD RATIO", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SlTpRatio.values().toList()) { ratio ->
                        val active = slTpRatio == ratio
                        Surface(
                            color = if (active) EmeraldSuccess.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) EmeraldSuccess else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.clickable { slTpRatio = ratio }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).width(120.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("1 : ${ratio.value}", color = if (active) EmeraldSuccess else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text(ratio.description, color = SlateText, fontSize = 7.sp, textAlign = TextAlign.Center, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 7. SCORE & CONFIDENCE THRESHOLDS
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MIN EA SCORE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text("$minEAScore%", color = if (minEAScore >= 70) EmeraldSuccess else if (minEAScore >= 50) Color(0xFFFFA500) else RoseError, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Slider(
                        value = minEAScore.toFloat(),
                        onValueChange = { minEAScore = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 20,
                        colors = SliderDefaults.colors(
                            thumbColor = IndigoAccent,
                            activeTrackColor = IndigoAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Text(
                        when (signalSource) {
                            AutoTradeSignalSource.EA -> "Trades only when EA validator vote ≥ $minEAScore%"
                            AutoTradeSignalSource.AI -> "Not used — AI mode ignores the EA score"
                            AutoTradeSignalSource.COMBINED -> "Combined trades need EA vote ≥ $minEAScore%"
                        },
                        color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MIN AI SCORE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text("$minAiScore%", color = if (minAiScore >= 70) EmeraldSuccess else if (minAiScore >= 50) Color(0xFFFFA500) else RoseError, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Slider(
                        value = minAiScore.toFloat(),
                        onValueChange = { minAiScore = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 20,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Text(
                        when (signalSource) {
                            AutoTradeSignalSource.EA -> "Not used — EA mode ignores the AI score"
                            AutoTradeSignalSource.AI -> "Trades only when AI journal score ≥ $minAiScore%"
                            AutoTradeSignalSource.COMBINED -> "Combined trades need AI score ≥ $minAiScore%"
                        },
                        color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MIN CONFIDENCE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text("$minConfidence%", color = if (minConfidence >= 75) EmeraldSuccess else if (minConfidence >= 55) Color(0xFFFFA500) else RoseError, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Slider(
                        value = minConfidence.toFloat(),
                        onValueChange = { minConfidence = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 20,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFA500),
                            activeTrackColor = Color(0xFFFFA500),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Text(
                        if (signalSource == AutoTradeSignalSource.AI) "AI mode — confidence equals the AI score (no EA validation needed)" else "Trades only when EA validation confidence ≥ $minConfidence%",
                        color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 8. RISK MANAGEMENT
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("RISK MANAGEMENT", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatInput("RISK/TRADE", "${String.format("%.1f", riskPerTrade)}%", IndigoAccent, modifier = Modifier.weight(1f)) { riskPerTrade = it }
                    StatInput("MAX DAILY TRADES", "$maxDailyTrades", Color(0xFFFFA500), modifier = Modifier.weight(1f)) { maxDailyTrades = it.toInt() }
                    StatInput("MAX OPEN POSITIONS", "$maxOpenPositions", EmeraldSuccess, modifier = Modifier.weight(1f)) { maxOpenPositions = it.toInt() }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 9. FILTERS
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("EXECUTION FILTERS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)

                ToggleRow("NEWS FILTER", "Avoid high-impact news (±15 min)", newsFilter, { newsFilter = it })
                ToggleRow("SPREAD FILTER", "Block trades if spread exceeds threshold", spreadFilter, { spreadFilter = it })

                if (spreadFilter) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MAX SPREAD", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                            Text("${String.format("%.1f", maxSpreadPips)} pips", color = RoseError, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                        Slider(
                            value = maxSpreadPips.toFloat(),
                            onValueChange = { maxSpreadPips = it.toDouble() },
                            valueRange = 0.5f..10f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = RoseError,
                                activeTrackColor = RoseError,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val autoPositions = enginePositions.filter { it.ticket in autoOpenTickets }

        // 10. EA STATUS & ACTION
        InfoBox {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isAutoTradeEnabled) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("EQUITY", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text("BALANCE", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text("POSITIONS", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text("TODAY", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$ ${String.format("%.2f", engineAccount.equity)}", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("$ ${String.format("%.2f", engineAccount.balance)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("${autoPositions.size}", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("${AutoTradeEngine.tradesToday.collectAsState().value}", color = Color(0xFFFFA500), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (autoPositions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        autoPositions.forEach { pos ->
                            AutoTradePositionStrip(position = pos)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (engineLog.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        engineLog.take(5).forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(if (entry.ok) EmeraldSuccess else RoseError, RoundedCornerShape(3.dp)).align(Alignment.CenterVertically))
                                Text(
                                    entry.message,
                                    color = if (entry.ok) Color.White else Color(0xFFF87171),
                                    fontSize = 8.sp,
                                    fontFamily = InterFontFamily,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    Instant.ofEpochMilli(entry.time).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                    color = SlateText, fontSize = 7.sp, fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (isAutoTradeEnabled) AutoTradeEngine.stop()
                        else AutoTradeEngine.start(ctx, buildConfig())
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoTradeEnabled) Color(0xFFEF4444) else IndigoAccent
                    ),
                    enabled = selectedAssets.isNotEmpty() || isAutoTradeEnabled
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (isAutoTradeEnabled) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Text(
                            if (isAutoTradeEnabled) "STOP AUTO TRADE" else "START AUTO TRADE",
                            color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = InterFontFamily
                        )
                    }
                }

                if (selectedAssets.isEmpty()) {
                    Text("Select at least one asset to enable auto trade", color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // 11. PER-ASSET PARAMETERS (bottom, only while running)
        if (isAutoTradeEnabled && selectedAssets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            InfoBox {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PER-ASSET PARAMETERS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text("${selectedAssets.size} ASSET(S)", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                    selectedAssets.forEach { symbol ->
                        val p = perAssetParams[symbol]
                        AssetParameterCard(
                            symbol = symbol,
                            params = p ?: defaultParams(),
                            hasOverride = p != null,
                            onEdit = { editingAsset = symbol },
                            onReset = { perAssetParams.remove(symbol) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// Supporting enums
enum class EntryModel(val displayName: String, val description: String) {
    AUTO_CONFLUENCE("AUTO CONFLUENCE", "PD + FVG + OB confluence (3-factor minimum)"),
    MANUAL_PD("PREMIUM/DISCOUNT", "Premium/Discount zones only"),
    MANUAL_FVG("FAIR VALUE GAP", "Fair Value Gaps only"),
    MANUAL_OB("ORDER BLOCK", "Order Blocks only"),
    MANUAL_BOS("BOS/CHoCH", "Break of Structure / Change of Character"),
    MANUAL_SWEEP("LIQUIDITY SWEEP", "Sweep + reversal entry"),
    MANUAL_COMBO("CUSTOM COMBO", "Any combination of selected SMC techniques")
}

enum class SMCTechnique(val label: String, val code: String, val shortDesc: String, val color: Color) {
    PD("Premium/Discount", "PD", "Mitigation of premium/discount zones", IndigoAccent),
    FVG("Fair Value Gap", "FVG", "Unfilled imbalance zones", EmeraldSuccess),
    OB("Order Block", "OB", "Institutional order footprints", Color(0xFFFFA500)),
    BOS("BOS/CHoCH", "BOS", "Structure break / character change", Color(0xFFBB86FC)),
    SWEEP("Liquidity Sweep", "SWEEP", "Stop hunt + reversal", RoseError),
    IMBALANCE("Imbalance", "IMB", "Volume/price inefficiency", Color(0xFF00FFFF)),
    VWAP("VWAP Bands", "VWAP", "Institutional VWAP levels", Color(0xFFFF00FF)),
    FIB("Fibonacci", "FIB", "Key retracement levels", Color(0xFF00FFAA))
}

enum class MarketSession(val displayName: String, val description: String) {
    LONDON("London", "07:00-16:00 UTC"),
    NEW_YORK("New York", "13:00-22:00 UTC"),
    ASIA("Asia", "23:00-08:00 UTC"),
    SYDNEY("Sydney", "22:00-07:00 UTC"),
    ALL_SESSIONS("All Sessions", "24/5 — No session filter"),
    CUSTOM("Custom Hours", "Set custom UTC window")
}

enum class SlTpRatio(val value: String, val description: String) {
    RATIO_1_1("1", "Conservative — 1:1"),
    RATIO_1_2("2", "Balanced — 1:2 (DEFAULT)"),
    RATIO_1_3("3", "Aggressive — 1:3"),
    RATIO_1_4("4", "High R:R — 1:4"),
    RATIO_CUSTOM("Custom", "Manual SL/TP in settings")
}

@Composable
private fun signalSourceLabel(source: AutoTradeSignalSource): String = when (source) {
    AutoTradeSignalSource.EA -> "EA ONLY"
    AutoTradeSignalSource.AI -> "AI ONLY"
    AutoTradeSignalSource.COMBINED -> "EA + AI"
}

@Composable
private fun sourceColor(source: AutoTradeSignalSource): Color = when (source) {
    AutoTradeSignalSource.EA -> IndigoAccent
    AutoTradeSignalSource.AI -> Color(0xFF00E5FF)
    AutoTradeSignalSource.COMBINED -> EmeraldSuccess
}

@Composable
private fun sourceDescription(source: AutoTradeSignalSource): String = when (source) {
    AutoTradeSignalSource.EA -> "MT5 EA validator vote + validation confidence"
    AutoTradeSignalSource.AI -> "Backend AI journal score + direction"
    AutoTradeSignalSource.COMBINED -> "EA + AI must agree; combined average score"
}

@Composable
private fun StatInput(label: String, value: String, color: Color, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(80.dp).fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onChange(value.replace("%", "").replace("pips", "").trim().toDoubleOrNull() ?: 1.0 - 1.0) }) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onChange(value.replace("%", "").replace("pips", "").trim().toDoubleOrNull() ?: 1.0 + 1.0) }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(subtitle, color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = IndigoAccent,
                checkedTrackColor = IndigoAccent.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun AssetParameterCard(
    symbol: String,
    params: AssetParams,
    hasOverride: Boolean,
    onEdit: () -> Unit,
    onReset: () -> Unit
) {
    var expanded by remember(symbol) { mutableStateOf(false) }
    Surface(
        color = IndigoAccent.copy(alpha = 0.06f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = IndigoAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                ) {
                    Text("EDIT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (hasOverride) "CUSTOM" else "GLOBAL",
                        color = if (hasOverride) Color(0xFFFFA500) else SlateText,
                        fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily
                    )
                    if (hasOverride) {
                        TextButton(onClick = onReset) {
                            Text("RESET", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                    }
                }
                ParamLine("ENTRY", params.entryModels.joinToString(", "))
                ParamLine("SMC", params.smcTechniques.joinToString(", "))
                ParamLine("SESSION", params.sessions.joinToString(", "))
                val rrLabel = SlTpRatio.values().firstOrNull { it.value == params.rrMultiplier.toInt().toString() }
                ParamLine("R:R", rrLabel?.description ?: "Custom")
                ParamLine("THRESHOLDS", "EA ≥ ${params.minEAScore}% · AI ≥ ${params.minAiScore}% · CONF ≥ ${params.minConfidence}%")
                ParamLine("RISK", "${params.riskPerTradePct}% per trade")
                ParamLine("FILTERS", buildString {
                    if (params.newsFilter) append("News · ")
                    if (params.spreadFilter) append("Spread ≤ ${params.maxSpreadPips}pips")
                    if (isEmpty()) append("None")
                }.trimEnd(' ', '\u00B7'))
            }
        }
    }
}

@Composable
private fun ParamLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Text(value, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, textAlign = TextAlign.End, maxLines = 2)
    }
}

@Composable
private fun ParamSectionTitle(title: String) {
    Text(title, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
}

@Composable
private fun ParamChip(label: String, active: Boolean, color: Color, onToggle: (Boolean) -> Unit) {
    Surface(
        color = if (active) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) color else Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.clickable { onToggle(!active) }
    ) {
        Text(
            label,
            color = if (active) color else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Int,
    color: Color,
    suffix: String,
    onChange: (Int) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text("$value$suffix", color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
    }
}

@Composable
private fun AssetParamsEditorDialog(
    asset: String,
    initial: AssetParams,
    onSave: (AssetParams) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val models = remember { mutableStateListOf<EntryModel>().apply { addAll(EntryModel.values().filter { it.displayName in initial.entryModels }) } }
    val techniques = remember { mutableStateListOf<SMCTechnique>().apply { addAll(SMCTechnique.values().filter { it.code in initial.smcTechniques }) } }
    val sessions = remember { mutableStateListOf<MarketSession>().apply { addAll(MarketSession.values().filter { it.displayName in initial.sessions }) } }
    val ratio = remember { mutableStateOf(SlTpRatio.values().firstOrNull { it.value == initial.rrMultiplier.toInt().toString() } ?: SlTpRatio.RATIO_CUSTOM) }
    var eaScore by remember { mutableStateOf(initial.minEAScore) }
    var aiScore by remember { mutableStateOf(initial.minAiScore) }
    var confScore by remember { mutableStateOf(initial.minConfidence) }
    var risk by remember { mutableStateOf(initial.riskPerTradePct) }
    var newsOn by remember { mutableStateOf(initial.newsFilter) }
    var spreadOn by remember { mutableStateOf(initial.spreadFilter) }
    var maxSpread by remember { mutableStateOf(initial.maxSpreadPips) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0B1220),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PARAMETERS — $asset", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("Per-asset overrides for auto trade", color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                    }
                    Text(
                        if (models.isEmpty()) "NO ENTRY MODEL" else "${models.size} MODEL(S)",
                        color = if (models.isEmpty()) RoseError else IndigoAccent,
                        fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily
                    )
                }

                ParamSectionTitle("ENTRY MODEL")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EntryModel.values().toList()) { model ->
                        ParamChip(model.displayName, model in models, IndigoAccent) {
                            if (it) models.add(model) else models.remove(model)
                        }
                    }
                }

                ParamSectionTitle("SMC TECHNIQUES (PD, FVG, OB always active)")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SMCTechnique.values().toList()) { tech ->
                        val core = tech in setOf(SMCTechnique.PD, SMCTechnique.FVG, SMCTechnique.OB)
                        if (!core) {
                            ParamChip(tech.label, tech in techniques, tech.color) {
                                if (it) techniques.add(tech) else techniques.remove(tech)
                            }
                        }
                    }
                }

                ParamSectionTitle("MARKET SESSION")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MarketSession.values().toList()) { session ->
                        ParamChip(session.displayName, session in sessions, Color(0xFFFFA500)) {
                            if (it) sessions.add(session) else sessions.remove(session)
                        }
                    }
                }

                ParamSectionTitle("SL/TP R:R")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SlTpRatio.values().toList()) { r ->
                        ParamChip(r.value, ratio.value == r, EmeraldSuccess) { ratio.value = r }
                    }
                }

                ParamSlider("MIN EA SCORE", eaScore, IndigoAccent, "%") { eaScore = it }
                ParamSlider("MIN AI SCORE", aiScore, Color(0xFF00E5FF), "%") { aiScore = it }
                ParamSlider("MIN CONFIDENCE", confScore, Color(0xFFFFA500), "%") { confScore = it }

                ParamSectionTitle("RISK PER TRADE")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$risk%", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Slider(
                    value = risk.toFloat(),
                    onValueChange = { risk = it.toDouble() },
                    valueRange = 0.5f..5f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
                )

                ParamSectionTitle("FILTERS")
                ToggleRow("NEWS FILTER", "Skip entries near high-impact news", newsOn) { newsOn = it }
                ToggleRow("SPREAD FILTER", "Skip when spread exceeds the cap", spreadOn) { spreadOn = it }
                if (spreadOn) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("MAX SPREAD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = { maxSpread = (maxSpread - 0.5).coerceAtLeast(0.5) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Text("${maxSpread} pips", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            IconButton(onClick = { maxSpread += 0.5 }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            onSave(
                                AssetParams(
                                    entryModels = models.map { it.displayName }.toSet(),
                                    smcTechniques = (techniques.toSet() + setOf(SMCTechnique.PD, SMCTechnique.FVG, SMCTechnique.OB)).map { it.code }.toSet(),
                                    sessions = sessions.map { it.displayName }.toSet(),
                                    rrMultiplier = ratio.value.value.toDoubleOrNull() ?: -1.0,
                                    minEAScore = eaScore,
                                    minAiScore = aiScore,
                                    minConfidence = confScore,
                                    riskPerTradePct = risk,
                                    newsFilter = newsOn,
                                    spreadFilter = spreadOn,
                                    maxSpreadPips = maxSpread
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Text("SAVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    TextButton(onClick = onReset, modifier = Modifier.height(40.dp)) {
                        Text("RESET", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.height(40.dp)) {
                        Text("CANCEL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}
