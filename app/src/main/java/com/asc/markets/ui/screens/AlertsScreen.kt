package com.asc.markets.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.VigilanceNodeEngine
import com.asc.markets.data.AuditRecord
import com.asc.markets.logic.VigilanceNode
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun AlertsScreen(viewModel: ForexViewModel) {
    var isSmartMode by remember { mutableStateOf(true) }
    val selectedConfirmations = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()
    val activeNodes = remember { mutableStateListOf<com.asc.markets.logic.VigilanceNode>() }
    
    
    // Load active nodes on composition
    LaunchedEffect(Unit) {
        activeNodes.addAll(VigilanceNodeEngine.getActiveNodes())
    }
    
    val logicScore = remember(selectedConfirmations.size) {
        40 + (selectedConfirmations.size * 15).coerceAtMost(60)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⚡", fontSize = 20.sp)
                }
            }
            Column {
                Text("VIGILANCE SETUP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("CONFIGURE & DEPLOY VIGILANCE NODES", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Mode Toggle Parity
        Row(modifier = Modifier.fillMaxWidth().background(PureBlack, RoundedCornerShape(12.dp)).padding(4.dp)) {
            listOf(true to "SMART ALERT", false to "SIMPLE").forEach { (mode, label) ->
                val active = isSmartMode == mode
                Surface(
                    color = if (active) Color(0xFF2B2B2B) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp).clickable { isSmartMode = mode }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (mode) "⊙" else "⊙", fontSize = 12.sp, color = if (active) Color.White else Color.Gray)
                            Text(label, color = if (active) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSmartMode) {
            SmartCalibration(logicScore, selectedConfirmations, viewModel) { node ->
                activeNodes.add(0, node)
                // Append an AuditRecord for the deployed node so Post-Move Audit shows it
                try {
                    val impact = when {
                        node.confidenceScore >= 75 -> "CRITICAL"
                        node.confidenceScore >= 50 -> "HIGH"
                        else -> "INFO"
                    }
                    viewModel.appendAuditRecord(AuditRecord(
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
                    ))
                } catch (_: Exception) { }
            }
        } else {
            SimpleCalibration { node ->
                activeNodes.add(0, node)
                try {
                    val impact = when {
                        node.confidenceScore >= 75 -> "CRITICAL"
                        node.confidenceScore >= 50 -> "HIGH"
                        else -> "INFO"
                    }
                    viewModel.appendAuditRecord(AuditRecord(
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
                    ))
                } catch (_: Exception) { }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // DEPLOYMENT CONFIRMATION
        if (activeNodes.isNotEmpty()) {
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
                        Text("${activeNodes.size} VIGILANCE NODE${if (activeNodes.size > 1) "S" else ""} DEPLOYED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("View and manage in My Alerts", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SmartCalibration(score: Int, selections: MutableList<String>, viewModel: ForexViewModel, onNodeDeployed: (VigilanceNode) -> Unit) {
    var selectedAsset by remember { mutableStateOf("EUR/USD") }
    var primaryEvent by remember { mutableStateOf("Break of Previous High") }
    var cooldownMinutes by remember { mutableStateOf(30) }
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    val environmentSelections = remember { mutableStateListOf<String>() }
    val riskFilterSelections = remember { mutableStateListOf<String>() }
    var selectedDirection by remember { mutableStateOf("BOTH") }
    var selectedRegime by remember { mutableStateOf("ANY") }
    var selectedVolatility by remember { mutableStateOf("ANY") }
    var confluenceThreshold by remember { mutableStateOf(0) }
    
    val validations = listOf(
        "CANDLE_CLOSE_BEYOND_LEVEL" to "CANDLE (+15%)",
        "STRONG_BODY_CLOSE" to "CANDLE (+15%)",
        "ENGULFING_CANDLE" to "CANDLE (+15%)",
        "PRICE_ABOVE_BELOW_MA" to "MA (+10%)",
        "MA_SLOPE_ALIGNMENT" to "MA (+10%)",
        "RSI_EXIT_DIVERGENCE" to "MOMENTUM (+10%)"
    )
    val environmentOptions = listOf(
        "HTF_BULLISH_BIAS" to "(+10%)",
        "HTF_BEARISH_BIAS" to "(+10%)",
        "LONDON_SESSION_ACTIVE" to "(+10%)",
        "NEW_YORK_SESSION_ACTIVE" to "(+10%)",
        "NY_REVERSAL_WINDOW" to "(+10%)",
        "AT_PREMIUM_ZONE" to "(+5%)",
        "AT_DISCOUNT_ZONE" to "(+5%)"
    )
    val riskFilters = listOf("IGNORE_LOW_LIQUIDITY", "IGNORE_NEWS_WINDOW", "ENHANCED_NOISE_REDUCTION")
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Quick Calibration Presets
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📌", fontSize = 14.sp)
                    Text("QUICK CALIBRATION PRESETS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                }
                listOf(
                    "BREAKOUT WATCH" to "HIGH SENSITIVITY MONITORING",
                    "CONFIRMED CONTINUATION" to "INSTITUTIONAL TREND ALIGNMENT",
                    "REVERSAL WINDOW" to "COUNTER-TREND EXHAUSTION"
                ).forEach { (title, desc) ->
                    val isPresetSelected = selectedPreset == title
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clickable { selectedPreset = if (isPresetSelected) null else title },
                        color = if (isPresetSelected) IndigoAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPresetSelected) IndigoAccent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
                            Text(title, color = if (isPresetSelected) Color.White else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text(desc, color = if (isPresetSelected) Color.White.copy(alpha = 0.85f) else SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }
        
        // Asset Target
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ASSET TARGET", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)

                // Dropdown state and asset lists
                val forex = listOf("EUR/USD","GBP/USD","USD/JPY","AUD/USD","USD/CAD","USD/CHF","NZD/USD","EUR/GBP","EUR/JPY","GBP/JPY")
                val stocks = listOf("AAPL","MSFT","GOOGL","AMZN","TSLA")
                val indices = listOf("SPX/500","NAS100","DOW30")
                val commodities = listOf("XAU/USD","XAG/USD","WTI")
                val crypto = listOf("BTC/USDT","ETH/USDT","BNB/USDT")
                val grouped = listOf(
                    "Forex" to forex,
                    "Stocks" to stocks,
                    "Indices" to indices,
                    "Commodities" to commodities,
                    "Crypto" to crypto
                )

                var assetMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clickable { assetMenuExpanded = true },
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedAsset, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = assetMenuExpanded,
                        onDismissRequest = { assetMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.95f)
                    ) {
                        val scroll = rememberScrollState()
                        Column(modifier = Modifier.heightIn(max = 340.dp).verticalScroll(scroll)) {
                            grouped.forEach { (group, items) ->
                                Text(group.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(12.dp, 8.dp), fontFamily = InterFontFamily)
                                items.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.03f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(item.takeWhile { it != '/' }.take(1), color = Color.White, fontWeight = FontWeight.Black)
                                                    }
                                                }
                                                Text(item, color = Color.White, modifier = Modifier.padding(start = 12.dp), fontFamily = InterFontFamily)
                                            }
                                        },
                                        onClick = {
                                            selectedAsset = item
                                            assetMenuExpanded = false
                                            viewModel.selectPairBySymbolNoNavigate(item)
                                        }
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                            }
                        }
                    }
                }
            }
        }
        
        // Direction Selector
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("D", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("TRADE DIRECTION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LONG" to EmeraldSuccess, "SHORT" to RoseError, "BOTH" to IndigoAccent).forEach { (dir, color) ->
                        val isSelected = selectedDirection == dir
                        Surface(
                            modifier = Modifier.weight(1f).height(44.dp).clickable { selectedDirection = dir },
                            color = if (isSelected) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(dir, color = if (isSelected) color else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }
        }
        
        // Primary Structural Event
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
                        Text("PRIMARY STRUCTURAL EVENT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Surface(color = RoseError.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp), modifier = Modifier.wrapContentSize()) {
                        Text("MANDATORY", color = RoseError, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp))
                    }
                }
                // Primary event dropdown
                val primaryOptions = listOf(
                    "Break of Previous High",
                    "Break of Previous Low",
                    "Break of Range High",
                    "Break of Range Low",
                    "Break of Support Level",
                    "Break of Resistance Level",
                    "Break of Trendline",
                    "Wick Rejection at Level",
                    "False Break / Sweep",
                    "Change of Character (CHoCH)",
                    "Market Structure Shift (MSS)"
                )
                var primaryMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clickable { primaryMenuExpanded = true },
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(primaryEvent, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = primaryMenuExpanded,
                        onDismissRequest = { primaryMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.95f)
                    ) {
                        val scroll = rememberScrollState()
                        Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(scroll)) {
                            primaryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(option, color = if (option == primaryEvent) Color.Black else Color.White, modifier = Modifier.weight(1f), fontFamily = InterFontFamily)
                                            if (option == primaryEvent) {
                                                Surface(color = IndigoAccent, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(start = 8.dp)) {
                                                    Text("✓", color = Color.White, modifier = Modifier.padding(6.dp, 2.dp))
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        primaryEvent = option
                                        primaryMenuExpanded = false
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                            }
                        }
                    }
                }
            }
        }
        
        // Validation Confirmations
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
                        Text("VALIDATION CONFIRMATIONS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp), modifier = Modifier.wrapContentSize()) {
                        Text("MIN. 1 REQUIRED", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp))
                    }
                }
                
                for (i in validations.indices step 2) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (j in 0..1) {
                            if (i + j < validations.size) {
                                val (label, scoring) = validations[i + j]
                                val isSelected = selections.contains(label)
                                Surface(
                                    modifier = Modifier.weight(1f).heightIn(min = 72.dp).clickable { if (isSelected) selections.remove(label) else selections.add(label) },
                                    color = if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoAccent.copy(alpha = 0.3f) else Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Top) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(14.dp).background(if (isSelected) IndigoAccent else Color.Transparent, RoundedCornerShape(3.dp)).border(1.dp, if (isSelected) IndigoAccent else Color.DarkGray, RoundedCornerShape(3.dp)))
                                            Text(label.replace("_", " "), color = if (isSelected) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(scoring, color = SlateText, fontSize = 8.sp, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Environment Context
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("3", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("ENVIRONMENT CONTEXT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    environmentOptions.forEach { (label, scoring) ->
                        val isSelected = environmentSelections.contains(label)
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(40.dp).clickable { if (isSelected) environmentSelections.remove(label) else environmentSelections.add(label) },
                            color = if (isSelected) IndigoAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoAccent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(label.replace("_", " "), color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                                Text(scoring, color = if (isSelected) Color.White else IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }
        }
        
        // Risk Filtering
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("5", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("RISK FILTERING", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    riskFilters.forEach { filter ->
                        val isSelected = riskFilterSelections.contains(filter)
                        Row(modifier = Modifier.fillMaxWidth().clickable { if (isSelected) riskFilterSelections.remove(filter) else riskFilterSelections.add(filter) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(filter.replace("_", " "), color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                            Box(modifier = Modifier.size(20.dp).background(if (isSelected) IndigoAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).border(1.dp, if (isSelected) IndigoAccent else Color.DarkGray, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        }
        
        // Volatility Filter
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("6", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("VOLATILITY STATE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp), modifier = Modifier.wrapContentSize()) {
                        Text("+5% BONUS", color = EmeraldSuccess, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(6.dp, 3.dp))
                    }
                }
                
                val volatilityOptions = listOf("ANY" to "No Filter", "EXPANDING" to "Momentum Building", "COMPRESSED" to "Pre-Expansion", "DEAD" to "Avoid Dead Markets")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    volatilityOptions.forEach { (vol, desc) ->
                        val isSelected = selectedVolatility == vol
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(44.dp).clickable { selectedVolatility = vol },
                            color = if (isSelected) IndigoAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoAccent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(vol, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                                    Text(desc, color = if (isSelected) Color.White.copy(alpha = 0.7f) else SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                                }
                                if (isSelected) {
                                    Surface(color = IndigoAccent, shape = RoundedCornerShape(4.dp), modifier = Modifier.wrapContentSize()) {
                                        Text("✓", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Logic Control
        InfoBox {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("7", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("LOGIC CONTROL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                
                // Confluence Threshold
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("CONFLUENCE THRESHOLD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("${confluenceThreshold}%", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = confluenceThreshold.toFloat(),
                            onValueChange = { confluenceThreshold = it.toInt() },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("0=Off", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cooldown
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("COOLDOWN PERIOD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = cooldownMinutes.toFloat(),
                            onValueChange = { cooldownMinutes = it.toInt() },
                            valueRange = 15f..120f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${cooldownMinutes}m", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val node = VigilanceNodeEngine.createSmartAlert(
                            pair = selectedAsset,
                            primaryEvent = primaryEvent,
                            confirmations = selections.toList(),
                            environmentContext = environmentSelections.joinToString(","),
                            riskFilters = riskFilterSelections.toList(),
                            direction = selectedDirection,
                            regimeFilter = selectedRegime,
                            volatilityFilter = selectedVolatility,
                            confluenceThreshold = confluenceThreshold,
                            cooldownMinutes = cooldownMinutes
                        )
                        onNodeDeployed(node)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("◇", fontSize = 16.sp, color = Color.White)
                        Text("DEPLOY SURVEILLANCE NODE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                }
                
                Text("COMMIT PARAMETERS TO ACTIVATE NODE MONITORING", color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
        

    }
}

@Composable
fun SimpleCalibration(onNodeDeployed: (VigilanceNode) -> Unit) {
    var selectedPair by remember { mutableStateOf("EUR/USD") }
    var selectedTrigger by remember { mutableStateOf("PRICE_THRESHOLD") }
    var selectedTimeframe by remember { mutableStateOf("H1") }
    var selectedDirection by remember { mutableStateOf("BOTH") }
    var priceLevel by remember { mutableStateOf("1.0850") }
    var rsiPeriod by remember { mutableStateOf("14") }
    var rsiLevel by remember { mutableStateOf("70") }
    var maFastPeriod by remember { mutableStateOf("9") }
    var maSlowPeriod by remember { mutableStateOf("21") }
    var cooldownMinutes by remember { mutableStateOf(30) }
    
    val pairs = listOf(
        // Forex majors
        "EUR/USD","GBP/USD","USD/JPY","AUD/USD","USD/CAD","USD/CHF","NZD/USD","EUR/GBP","EUR/JPY","GBP/JPY",
        // Stocks
        "AAPL","MSFT","GOOGL","AMZN","TSLA",
        // Indices
        "SPX/500","NAS100","DOW30",
        // Commodities
        "XAU/USD","XAG/USD","WTI",
        // Crypto
        "BTC/USDT","ETH/USDT","BNB/USDT"
    )
    val triggers = listOf("PRICE_THRESHOLD", "RSI_LEVEL", "MA_CROSS", "TRENDLINE_BREAK")
    val timeframes = listOf("M5", "M15", "H1", "H4", "D1")
    
    InfoBox {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("THRESHOLD EVALUATOR L14", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Text("Deterministic Rule-Based Triggers", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Pair selector (grouped dropdown like SmartCalibration)
            Text("PAIR", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(8.dp))
            val forex = listOf("EUR/USD","GBP/USD","USD/JPY","AUD/USD","USD/CAD","USD/CHF","NZD/USD","EUR/GBP","EUR/JPY","GBP/JPY")
            val stocks = listOf("AAPL","MSFT","GOOGL","AMZN","TSLA")
            val indices = listOf("SPX/500","NAS100","DOW30")
            val commodities = listOf("XAU/USD","XAG/USD","WTI")
            val crypto = listOf("BTC/USDT","ETH/USDT","BNB/USDT")
            val grouped = listOf(
                "Forex" to forex,
                "Stocks" to stocks,
                "Indices" to indices,
                "Commodities" to commodities,
                "Crypto" to crypto
            )

            var pairMenuExpanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable { pairMenuExpanded = true },
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedPair, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                DropdownMenu(
                    expanded = pairMenuExpanded,
                    onDismissRequest = { pairMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.heightIn(max = 340.dp).verticalScroll(scroll)) {
                        grouped.forEach { (group, items) ->
                            Text(group.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(12.dp, 8.dp), fontFamily = InterFontFamily)
                            items.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.03f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(item.takeWhile { it != '/' }.take(1), color = Color.White, fontWeight = FontWeight.Black)
                                                }
                                            }
                                            Text(item, color = Color.White, modifier = Modifier.padding(start = 12.dp), fontFamily = InterFontFamily)
                                        }
                                    },
                                    onClick = {
                                        selectedPair = item
                                        pairMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Direction Selector
            Text("DIRECTION", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("LONG" to EmeraldSuccess, "SHORT" to RoseError, "BOTH" to IndigoAccent).forEach { (dir, color) ->
                    val isSelected = selectedDirection == dir
                    Surface(
                        modifier = Modifier.weight(1f).height(36.dp).clickable { selectedDirection = dir },
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Trigger selector
            Text("TRIGGER TYPE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                triggers.forEach { trigger ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp).clickable { selectedTrigger = trigger },
                        color = if (selectedTrigger == trigger) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTrigger == trigger) IndigoAccent.copy(alpha = 0.3f) else Color.Transparent)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            Text(trigger, color = if (selectedTrigger == trigger) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Trigger-specific configuration
            when (selectedTrigger) {
                "PRICE_THRESHOLD" -> {
                    Text("PRICE LEVEL", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = priceLevel,
                                onValueChange = { priceLevel = it },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Black
                                ),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                "RSI_LEVEL" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("RSI PERIOD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("7", "14", "21").forEach { period ->
                                val isSelected = rsiPeriod == period
                                Surface(
                                    modifier = Modifier.weight(1f).height(36.dp).clickable { rsiPeriod = period },
                                    color = if (isSelected) IndigoAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(period, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                        
                        Text("RSI LEVEL", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("30" to "Oversold", "70" to "Overbought", "80" to "Extreme").forEach { (level, label) ->
                                val isSelected = rsiLevel == level
                                Surface(
                                    modifier = Modifier.weight(1f).height(44.dp).clickable { rsiLevel = level },
                                    color = if (isSelected) IndigoAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(level, color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                        Text(label, color = if (isSelected) Color.White.copy(alpha = 0.7f) else SlateText, fontSize = 8.sp, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                "MA_CROSS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("FAST MA PERIOD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("5", "9", "12", "20").forEach { period ->
                                val isSelected = maFastPeriod == period
                                Surface(
                                    modifier = Modifier.weight(1f).height(36.dp).clickable { maFastPeriod = period },
                                    color = if (isSelected) IndigoAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(period, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                        
                        Text("SLOW MA PERIOD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("21", "50", "100", "200").forEach { period ->
                                val isSelected = maSlowPeriod == period
                                Surface(
                                    modifier = Modifier.weight(1f).height(36.dp).clickable { maSlowPeriod = period },
                                    color = if (isSelected) EmeraldSuccess.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) EmeraldSuccess.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(period, color = if (isSelected) EmeraldSuccess else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Cooldown
            Text("COOLDOWN PERIOD", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = cooldownMinutes.toFloat(),
                    onValueChange = { cooldownMinutes = it.toInt() },
                    valueRange = 15f..120f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("${cooldownMinutes}m", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timeframe selector
            Text("RESOLUTION", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                timeframes.forEach { tf ->
                    Surface(
                        color = if (selectedTimeframe == tf) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTimeframe == tf) Color.White else Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f).height(32.dp).clickable { selectedTimeframe = tf }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(tf, color = if (selectedTimeframe == tf) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val priceLevelValue = if (selectedTrigger == "PRICE_THRESHOLD" && priceLevel.isNotEmpty()) {
                        priceLevel.toDoubleOrNull()
                    } else null
                    
                    val node = VigilanceNodeEngine.createSimpleAlert(
                        pair = selectedPair,
                        trigger = selectedTrigger,
                        timeframe = selectedTimeframe,
                        direction = selectedDirection,
                        priceLevel = priceLevelValue,
                        rsiPeriod = rsiPeriod.toIntOrNull() ?: 14,
                        rsiLevel = rsiLevel.toIntOrNull() ?: 70,
                        maFastPeriod = maFastPeriod.toIntOrNull() ?: 9,
                        maSlowPeriod = maSlowPeriod.toIntOrNull() ?: 21,
                        cooldownMinutes = cooldownMinutes
                    )
                    onNodeDeployed(node)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
            ) {
                Text("DEPLOY SIMPLE NODE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
            }
        }
    }
}
@Composable
fun ActiveNodeCard(node: com.asc.markets.logic.VigilanceNode, onShowBreakdown: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glowPulse"
    )
    
    val strengthColor = when (node.strength) {
        "STRONG" -> EmeraldSuccess
        "MEDIUM" -> Color(0xFFFFA500)
        else -> RoseError
    }
    
    Surface(
        color = if (node.isActive) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.01f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5f.dp, strengthColor.copy(alpha = if (node.isActive) glowAlpha else 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(node.pair, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        // Status indicator - pulsing dot if active
                        if (node.isActive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(strengthColor, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(node.trigger, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(node.description, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp, fontFamily = InterFontFamily)
                }
                
                // Strength badge
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
                    Text("CONFIDENCE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("${node.confidenceScore}%", color = strengthColor, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TIMEFRAME", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text(node.timeframe, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    
                    if (node.alertType == "SMART") {
                        Text("| ", color = SlateText, fontSize = 9.sp)
                        Text("COOLDOWN", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text("${node.cooldownMinutes}M", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }
                
                // Breakdown button
                IconButton(onClick = { onShowBreakdown(node.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
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

@Composable
fun RejectedPatternCard(pair: String, pattern: String, reason: String) {
    Surface(
        color = RoseError.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pair, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(pattern, color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(reason, color = SlateText, fontSize = 12.sp, lineHeight = 18.sp, fontFamily = InterFontFamily)
        }
    }
}
