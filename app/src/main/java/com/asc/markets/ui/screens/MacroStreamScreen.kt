@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.asc.markets.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import androidx.compose.runtime.collectAsState
import com.asc.markets.data.MacroEvent
import com.asc.markets.data.MacroEventStatus
import com.asc.markets.data.ImpactPriority
import com.asc.markets.data.displayTitle
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.screens.dashboard.rememberTechnicalVitals
import com.asc.markets.data.remote.FinalDecisionItem
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun MacroStreamScreen(viewModel: ForexViewModel = viewModel()) {
    val macroEvents by viewModel.macroStreamEvents.collectAsState()
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val vitalsData = rememberTechnicalVitals()

    // UI state: search and category
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showSearch by remember { mutableStateOf(false) }

    // heartbeat animation
    val infinite = rememberInfiniteTransition()
    val pulse by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900, easing = LinearEasing))
    )

    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header styled like Post-Move Audit header (DeepBlack background + PureBlack surface)
                Column(modifier = Modifier.background(DeepBlack)) {
                    Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 12.dp)) {
                            Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                            }
                            Text(
                                "AI SIGNALS",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search bar (shows when search icon tapped)
                    if (showSearch) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            placeholder = { Text("Filter by asset, type, or ref id") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            textStyle = TextStyle(color = Color.White),
                            colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color(0xFF0B0B0B), unfocusedBorderColor = Color.Transparent, focusedBorderColor = IndigoAccent)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pills / sub-menu
                    val pills = listOf("ALL", "UPCOMING", "CONFIRMED", "HIGH IMPACT", "MACRO NEWS", "SYSTEM")
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                            items(pills) { p ->
                                val selected = p == selectedCategory
                                Surface(
                                    color = if (selected) Color(0xFF2d2d2d) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (!selected) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                                    modifier = Modifier.clickable { selectedCategory = p }
                                ) {
                                    Text(
                                        p,
                                        color = if (selected) Color.White else Color(0xFF94a3b8),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stream list (Macro Stream focuses on upcoming events and confirmed signals)
                val filtered = remember(macroEvents, query, selectedCategory) {
                    macroEvents.filter { it ->
                        val q = query.trim().lowercase()
                        val matchesQuery = q.isEmpty() || it.currency.lowercase().contains(q) || it.title.lowercase().contains(q) || it.details.lowercase().contains(q)
                        
                        // Macro Stream shows UPCOMING events primarily, but can show confirmed captures based on filters
                        val isUpcomingOrConfirmed = it.status == MacroEventStatus.UPCOMING || it.status == MacroEventStatus.CONFIRMED
                        
                        val matchesCategory = when(selectedCategory) {
                            "ALL" -> isUpcomingOrConfirmed
                            "HIGH IMPACT" -> isUpcomingOrConfirmed && (it.priority == ImpactPriority.HIGH || it.priority == ImpactPriority.CRITICAL)
                            "CONFIRMED" -> it.status == MacroEventStatus.CONFIRMED
                            "UPCOMING" -> it.status == MacroEventStatus.UPCOMING
                            else -> isUpcomingOrConfirmed && it.source.equals(selectedCategory, true)
                        }
                        matchesQuery && matchesCategory
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        // Situational awareness: heartbeat + sources + event stats inside InfoBox
                        com.asc.markets.ui.components.InfoBox(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = ErieBlack,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Top row: Node heartbeat + Event stats
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    // Node heartbeat
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = if (vitalsData.nodeHealth > 0.8) Color(0xFF2EE08A) else Color(0xFFE53935), modifier = Modifier.size((8.dp * pulse))) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Node: ${if(vitalsData.latencyMs < 50) "NY4" else "LD4"}", color = SlateText, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    // Captured Event Stats
                                    Column(modifier = Modifier.width(200.dp)) {
                                        val confirmedCount = macroEvents.count { it.status == MacroEventStatus.CONFIRMED }
                                        val totalEvents = macroEvents.size
                                        Text("Events Captured: $confirmedCount", color = SlateText, fontSize = 12.sp)
                                        val progress = if(totalEvents > 0) confirmedCount.toFloat() / totalEvents.toFloat() else 1f
                                        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF2EE08A), trackColor = Color(0xFF0B0B0B))
                                    }
                                }
                                
                                // Divider
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                                
                                // News Sources row
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("NEWS SOURCES:", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("14 RSS Feeds", color = SlateText, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("•", color = SlateText, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fed, ECB, BOE, BOJ", color = SlateText, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("•", color = SlateText, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ForexLive, FXStreet", color = SlateText, fontSize = 10.sp)
                                }
                                
                                // Source categories
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SourceBadge("Central Banks", Color(0xFF1E88E5))
                                    SourceBadge("Forex", Color(0xFF43A047))
                                    SourceBadge("Commodities", Color(0xFFFFA726))
                                    SourceBadge("Macro Data", Color(0xFFAB47BC))
                                }
                            }
                        }
                    }

                    items(filtered) { item ->
                        val decision = aiDeployments?.final_decision?.find { it.asset_1?.contains(item.currency, true) == true }
                        MacroStreamCard(item, decision)
                    }
                }
            }

            // Footer / governance pinned to bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(PureBlack)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LEDGER: ${macroEvents.size} events captured this session.", color = SlateText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("STREAM MAINTENANCE", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.navigateTo(com.asc.markets.data.AppView.PUSH_SETTINGS) }.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun MacroStreamCard(item: MacroEvent, decision: FinalDecisionItem?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ErieBlack),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 0.dp)
            ) {
                // icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF111217),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (item.priority) {
                            ImpactPriority.CRITICAL -> Icon(Icons.Filled.Psychology, contentDescription = "Critical Impact", tint = RoseError)
                            ImpactPriority.HIGH -> Icon(Icons.Filled.MyLocation, contentDescription = "High Impact", tint = Color.White)
                            ImpactPriority.MEDIUM -> Icon(Icons.Filled.Layers, contentDescription = "Medium Impact", tint = Color.White)
                            ImpactPriority.LOW -> Icon(Icons.Filled.Article, contentDescription = "Low Impact", tint = Color.White)
                        }
                    }
                }
                // minimal space
                Spacer(modifier = Modifier.width(6.dp))
                // pair and tag
                Text(
                    item.currency,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                
                val tagColor = when (item.priority) {
                    ImpactPriority.CRITICAL -> RoseError
                    ImpactPriority.HIGH -> Color(0xFFE53935)
                    ImpactPriority.MEDIUM -> Color(0xFF1E88E5)
                    ImpactPriority.LOW -> Color(0xFF43A047)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.priority.name,
                        color = tagColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        item.status.name,
                        color = SlateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                // timestamp with milliseconds UTC
                val fmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS 'UTC'").withZone(ZoneOffset.UTC)
                Text(fmt.format(Instant.ofEpochMilli(item.datetimeUtc)), color = SlateText, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(item.displayTitle(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(12.dp))

            var expanded by remember { mutableStateOf(false) }

            if (!expanded) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "REF: ${item.id.take(8)}",
                        color = SlateText,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text("VIEW REASONING", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.clickable { expanded = true })
                }
            } else {
                // Expanded details view (detection detail + system trace + confidence + alignment)
                Spacer(modifier = Modifier.height(8.dp))
                Text("DETECTION DETAIL", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(item.details.ifEmpty { "NO ADDITIONAL SYSTEM CONTEXT FOR THIS EVENT." }, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)

                Spacer(modifier = Modifier.height(12.dp))

                // System trace box
                Surface(
                    color = Color(0xFF0B0B0B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)
                ) {
                    Row(modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        // trace icon
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF081A2B), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("🔁", fontSize = 14.sp, color = Color.White) }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("SYSTEM TRACE", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("SOURCE: ${item.source.uppercase()}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AI Confidence & Alignment
                if (decision != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Confidence", color = SlateText, fontSize = 11.sp)
                            val confValue = (decision.journal_score ?: 0.0).toFloat() / 100f
                            LinearProgressIndicator(
                                progress = confValue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (confValue > 0.75) Color(0xFF2EE08A) else if (confValue > 0.50) Color(0xFFFFC107) else Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${(confValue * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                        }

                        Column(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .wrapContentWidth(Alignment.End)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text("Alignment", color = SlateText, fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (decision.journal_direction?.lowercase()) {
                                    "bullish" -> Color(0xFF2EE08A)
                                    "bearish" -> Color(0xFFE53935)
                                    else -> Color(0xFF374151)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    decision.journal_direction?.uppercase() ?: "NEUTRAL",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    Text("NO AI CORRELATION AVAILABLE FOR THIS ASSET.", color = SlateText, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("REF: ${item.id}", color = SlateText, fontSize = 10.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "HIDE DETAILS",
                        color = IndigoAccent,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { expanded = false }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun SourceBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
