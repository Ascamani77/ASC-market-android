package com.asc.markets.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.VigilanceNodeEngine
import com.asc.markets.logic.VigilanceNode
import com.asc.markets.ui.theme.*

@Composable
fun MyAlertsScreen(
    viewModel: ForexViewModel,
    onOpenInbox: () -> Unit = {},
    onOpenPushSettings: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val activeNodes = remember { mutableStateListOf<VigilanceNode>() }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var expandedNodeId by remember { mutableStateOf<String?>(null) }
    
    // Load active nodes on composition
    LaunchedEffect(Unit) {
        activeNodes.addAll(VigilanceNodeEngine.getActiveNodes())
    }
    
    // Listen for node count changes
    val activeNodeCount by VigilanceNodeEngine.activeNodeCount.collectAsState(initial = 0)
    
    // Refresh nodes when count changes
    LaunchedEffect(activeNodeCount) {
        activeNodes.clear()
        activeNodes.addAll(VigilanceNodeEngine.getActiveNodes())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notifications, null, tint = IndigoAccent, modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text("MY ALERTS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenInbox, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.NotificationsActive, null, tint = Color.White)
                    }
                    IconButton(onClick = onOpenPushSettings, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Smartphone, null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Fired vigilance alerts (live monitor) — newest first
                val triggered by VigilanceNodeEngine.triggeredAlerts.collectAsState(initial = emptyList())
                val signalsByAsset by com.asc.markets.data.EASignalLiveStore.signalsByAsset.collectAsState(initial = emptyMap())
                var expandedAlertId by remember { mutableStateOf<String?>(null) }
                if (triggered.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔔", fontSize = 12.sp)
                            Text("TRIGGERED EVENTS (${triggered.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                        Text(
                            "CLEAR",
                            color = SlateText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.clickable { VigilanceNodeEngine.clearTriggeredAlerts() }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        triggered.take(20).forEach { alert ->
                            TriggeredEventCard(
                                alert = alert,
                                signal = signalsByAsset[normalizeForAlert(alert.pair)],
                                expanded = expandedAlertId == alert.id,
                                onToggle = { expandedAlertId = if (expandedAlertId == alert.id) null else alert.id },
                                onOpenDetails = {
                                    viewModel.selectPairBySymbol(alert.pair)
                                    viewModel.openAssetDetail(com.asc.markets.data.AppView.MY_ALERTS)
                                }
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("▼", fontSize = 12.sp, color = SlateText)
                        Text("ACTIVE ALERTS (${activeNodes.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Text(
                        "PURGE ALL",
                        color = RoseError,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.clickable {
                            VigilanceNodeEngine.getActiveNodes().forEach { VigilanceNodeEngine.clearNode(it.id) }
                            activeNodes.clear()
                            viewModel.markAllNotificationsSeen()
                        }
                    )
                }

                if (activeNodes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        activeNodes.forEach { node ->
                            ActiveNodeCardFull(
                                node = node,
                                expanded = expandedNodeId == node.id,
                                onToggle = { expandedNodeId = if (expandedNodeId == node.id) null else node.id },
                                showBreakdown = selectedNodeId == node.id,
                                onShowBreakdown = { selectedNodeId = if (selectedNodeId == node.id) null else node.id },
                                onDelete = {
                                    VigilanceNodeEngine.clearNode(node.id)
                                    activeNodes.removeAll { it.id == node.id }
                                    if (selectedNodeId == node.id) selectedNodeId = null
                                    if (expandedNodeId == node.id) expandedNodeId = null
                                    viewModel.markAllNotificationsSeen()
                                }
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.NotificationsNone, null, tint = SlateText, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("NO ACTIVE ALERTS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Deploy new alerts from Vigilance Setup in the sidebar", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val rejectedPatterns = remember { VigilanceNodeEngine.getRejectedPatterns() }
            if (rejectedPatterns.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⊘", fontSize = 14.sp, color = SlateText)
                        Text("REJECTED PATTERNS LOG", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }

                    rejectedPatterns.takeLast(3).forEach { pattern ->
                        Surface(
                            color = RoseError.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(pattern.pair, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    Text(
                                        java.text.SimpleDateFormat("HH:mm UTC", java.util.Locale.US).format(java.util.Date(pattern.timestamp)),
                                        color = Color.DarkGray,
                                        fontSize = 9.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }
                                Text("REJECTED: ${pattern.pattern}", color = RoseError, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text(pattern.reason, color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
fun ActiveNodeCardFull(
    node: VigilanceNode,
    expanded: Boolean,
    onToggle: () -> Unit,
    showBreakdown: Boolean,
    onShowBreakdown: () -> Unit,
    onDelete: () -> Unit
) {
    val strengthColor = when (node.strength) {
        "STRONG" -> EmeraldSuccess
        "MEDIUM" -> Color(0xFFFFA500)
        else -> RoseError
    }
    val directionColor = when (node.direction) {
        "LONG" -> EmeraldSuccess
        "SHORT" -> RoseError
        else -> SlateText
    }

    com.asc.markets.ui.components.InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Collapsed header — always visible, tap to expand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(strengthColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(node.pair, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), maxLines = 1)
                if (node.direction != "BOTH") {
                    Surface(color = directionColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                        Text(node.direction, color = directionColor, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (node.isActive) "ACTIVE" else "PAUSED",
                    color = if (node.isActive) EmeraldSuccess else Color.Gray,
                    fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, null, tint = RoseError.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = SlateText, modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

            // Intelligence Audit header with confidence
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, null, tint = SlateText, modifier = Modifier.size(16.dp))
                    Text("  ALERT CONFIGURATION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Text("CONFIDENCE ${node.confidenceScore}%", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }

            // Description box
            Surface(color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (node.confirmations.isNotEmpty()) {
                        Text("• TRIGGER VALIDATED BY ${node.confirmations.size} INSTITUTIONAL FILTERS.", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    if (!node.environmentContext.isNullOrEmpty()) {
                        Text("• ENVIRONMENT: ${node.environmentContext}", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    // Show regime filter if not ANY
                    if (node.regimeFilter != "ANY") {
                        Text("• REGIME: ${node.regimeFilter.replace("_", " ")}", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    // Show volatility filter if not ANY
                    if (node.volatilityFilter != "ANY") {
                        Text("• VOLATILITY: ${node.volatilityFilter}", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    // Show confluence threshold if set
                    if (node.confluenceThreshold > 0) {
                        Text("• MIN CONFLUENCE: ${node.confluenceThreshold}%", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    // Show price level for simple alerts
                    if (node.priceLevel != null) {
                        Text("• PRICE LEVEL: ${node.priceLevel}", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    // Show RSI config
                    if (node.trigger == "RSI_LEVEL") {
                        Text("• RSI(${node.rsiPeriod}) LEVEL ${node.rsiLevel}", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                    // Show MA config
                    if (node.trigger == "MA_CROSS") {
                        Text("• MA CROSS: FAST(${node.maFastPeriod}) / SLOW(${node.maSlowPeriod})", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                }
            }

            // Bottom badges
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.15f))) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
                        Text(" ${node.cooldownMinutes}M COOLDOWN", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                }
                Surface(color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = SlateText, modifier = Modifier.size(14.dp))
                        Text(" ${node.timeframe}", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                }
                node.riskFilters.forEach { f ->
                    Surface(color = IndigoAccent.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.28f))) {
                        Text(f.replace("_", " "), color = Color.White, modifier = Modifier.padding(6.dp), fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }
            }
            
            // Show breakdown toggle — expands inline within the same InfoBox
            if (showBreakdown) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = IndigoAccent.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                val breakdown = VigilanceNodeEngine.getScoringBreakdown(node.id)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SCORING BREAKDOWN", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    breakdown.forEach { (factor, points) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(factor, color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
                            Text("+$points pts", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                    }
                    HorizontalDivider(color = IndigoAccent.copy(alpha = 0.1f), thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TOTAL SCORE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("${breakdown.values.sum()}%", color = IndigoAccent, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onShowBreakdown() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View Scoring Breakdown", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
            }
            }
        }
    }
}

private fun normalizeForAlert(raw: String): String = raw.uppercase()
    .replace("/", "").replace("-", "").replace("_", "")
    .replace(" ", "").replace(".", "").removeSuffix("M")

@Composable
private fun TriggeredEventCard(
    alert: com.asc.markets.logic.TriggeredAlert,
    signal: com.asc.markets.data.ASCSignalData?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val direction = when (signal?.direction?.uppercase()) {
        "BUY" -> "LONG"
        "SELL" -> "SHORT"
        else -> signal?.direction?.uppercase() ?: ""
    }
    val directionColor = when (direction) {
        "LONG" -> EmeraldSuccess
        "SHORT" -> RoseError
        else -> SlateText
    }
    val rawVote = signal?.chart_panel?.votes?.win_pct ?: 0.0
    val votePct = (if (rawVote > 1.0) rawVote else rawVote * 100.0).toInt()
    val rawConf = signal?.confidence ?: 0.0
    val aiPct = (if (rawConf > 1.0) rawConf else rawConf * 100.0).toInt()
    val combine = ((votePct + aiPct) / 2.0).toInt()
    val tier = signal?.chart_panel?.quality_tier?.ifBlank { "NONE" } ?: "NONE"
    val regime = signal?.regime?.trend?.uppercase()?.ifBlank { "NEUTRAL" } ?: "NEUTRAL"
    val entry = signal?.entry?.state?.uppercase()?.ifBlank { "READY" } ?: "READY"
    val validatorActive = signal?.chart_panel?.validator_active == true
    val validatorAllowed = signal?.chart_panel?.validator_allowed == true
    val validatorDir = signal?.chart_panel?.validator_direction?.uppercase().orEmpty().ifBlank { "INACTIVE" }
    val validatorColor = when {
        validatorActive && validatorAllowed -> EmeraldSuccess
        validatorActive -> RoseError
        else -> SlateText
    }
    val rawPwin = signal?.chart_panel?.validator_pwin ?: 0.0
    val pwin = (if (rawPwin > 1.0) rawPwin else rawPwin * 100.0).toInt()

    val stats = buildList {
        add("VOTE %" to "$votePct%" to Color.White)
        add("REGIME" to regime to when (regime) {
            "BULLISH" -> EmeraldSuccess
            "BEARISH" -> RoseError
            else -> SlateText
        })
        add("ENTRY" to entry to when (entry) {
            "OPTIMAL" -> EmeraldSuccess
            "GOOD", "ACCEPTABLE" -> Color(0xFFFFA500)
            else -> SlateText
        })
        add("TIER" to tier to when (tier.uppercase()) {
            "ELITE" -> EmeraldSuccess
            "STRONG", "VALID" -> Color(0xFFFFA500)
            "FILTERED" -> RoseError
            else -> SlateText
        })
        add("VALIDATOR" to (if (validatorActive) "$validatorDir • P(WIN) $pwin%" else "INACTIVE") to validatorColor)
        add("EA SCORE" to "$votePct%" to Color.White)
        add("AI SCORE" to "$aiPct%" to Color.White)
        add("COMBINE" to "$combine%" to IndigoAccent)
    }

    com.asc.markets.ui.components.InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(directionColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(alert.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
                    Text(
                        "${alert.pair} • ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(alert.timestamp))}",
                        color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
                stats.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (pair, color) ->
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(pair.first, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), maxLines = 1)
                                Text(pair.second, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, maxLines = 1)
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenDetails() }
                ) {
                    Text("MORE DETAILS", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Icon(Icons.Default.ChevronRight, null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = SlateText,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onToggle() }
                )
            }
        }
    }
}
