package com.asc.markets.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.VigilanceNodeEngine
import com.asc.markets.logic.VigilanceNode
import com.asc.markets.ui.theme.*

@Composable
fun MyAlertsScreen() {
    val scrollState = rememberScrollState()
    val activeNodes = remember { mutableStateListOf<VigilanceNode>() }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
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
                Text("ACTIVE VIGILANCE NODES & TRIGGERED ALERTS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ACTIVE VIGILANCE NODES SECTION
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("▼", fontSize = 12.sp, color = SlateText)
                    Text("ACTIVE VIGILANCE NODES (${activeNodes.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
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
                    }
                )
            }
            
            if (activeNodes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeNodes.forEach { node ->
                        ActiveNodeCardFull(
                            node = node,
                            onShowBreakdown = { selectedNodeId = if (selectedNodeId == node.id) null else node.id },
                            onDelete = {
                                VigilanceNodeEngine.clearNode(node.id)
                                activeNodes.removeAll { it.id == node.id }
                                if (selectedNodeId == node.id) selectedNodeId = null
                            }
                        )
                        
                        // Show breakdown if selected
                        if (selectedNodeId == node.id) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ScoringBreakdownPanel(
                                nodeId = node.id,
                                onDismiss = { selectedNodeId = null }
                            )
                        }
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
                            Text("Create alerts in Vigilance Setup", color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // REJECTED PATTERNS LOG
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
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ActiveNodeCardFull(
    node: VigilanceNode,
    onShowBreakdown: () -> Unit,
    onDelete: () -> Unit
) {
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
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top header: pair, small icon, and delete
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        if (node.isActive) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(strengthColor.copy(alpha = glowAlpha), RoundedCornerShape(6.dp))
                            )
                        } else {
                            Text("⊙", fontSize = 20.sp, color = IndigoAccent) 
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(node.pair, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text(node.alertType.replace("_", " "), color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = RoseError, modifier = Modifier.size(18.dp))
                }
            }

            // Strength badge + Direction + Active Status
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (node.strength == "STRONG") EmeraldSuccess.copy(alpha = 0.12f) else IndigoAccent.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(if (node.strength == "STRONG") "STRONG ALERT" else "ALERT", color = if (node.strength == "STRONG") EmeraldSuccess else IndigoAccent, modifier = Modifier.padding(10.dp, 6.dp), fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                // Direction badge
                val directionColor = when (node.direction) {
                    "LONG" -> EmeraldSuccess
                    "SHORT" -> RoseError
                    else -> SlateText
                }
                Surface(color = directionColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, directionColor.copy(alpha = 0.3f))) {
                    Text(node.direction, color = directionColor, modifier = Modifier.padding(10.dp, 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(if (node.isActive) EmeraldSuccess else Color.Gray, RoundedCornerShape(3.dp)))
                    Text(if (node.isActive) "ACTIVE" else "PAUSED", color = if (node.isActive) EmeraldSuccess else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.padding(start = 6.dp))
                }
            }

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
                    Text("• ALERT WILL TRIGGER WHEN PRICE ACTION PERFORMS A ${node.trigger.replace("_", " ").uppercase()}", color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
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
                    if (node.riskFilters.isEmpty()) {
                        Text("• NOTE: SETUP CURRENTLY LACKS VOLATILITY ALIGNMENT.", color = SlateText, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontFamily = InterFontFamily)
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
            
            // Show breakdown button
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
