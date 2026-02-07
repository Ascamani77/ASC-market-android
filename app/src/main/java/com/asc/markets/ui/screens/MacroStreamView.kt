package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.MacroEvent
import com.asc.markets.data.MacroEventStatus
import com.asc.markets.data.ImpactPriority
import com.asc.markets.data.sampleMacroEvents
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.asc.markets.ui.theme.TerminalTypography

@Composable
fun MacroStreamView(events: List<MacroEvent> = sampleMacroEvents(), viewModel: com.asc.markets.logic.ForexViewModel? = null) {
    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // UI state
        // Header: full-width bar (no rounded box) with compact horizontal layout
        Box(modifier = Modifier.fillMaxWidth().background(PureBlack)) {
            Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                // Left grouping: title + pill + refresh, and lead-time row below
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MACRO STREAM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MACRO-WEIGHTED", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp), fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = Color(0xFF0B0B0B), shape = RoundedCornerShape(12.dp)) {
                                IconButton(onClick = { /* refresh */ }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00C853), shape = CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LEAD TIME ENGINE ACTIVE", color = RoseError, style = TerminalTypography.labelSmall.copy(fontSize = 9.sp))
                    }
                }

                // Vertical divider
                Box(modifier = Modifier.width(1.dp).height(44.dp).background(Color(0xFF121212)))

                // Right grouping: terminal sync + clock
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(start = 12.dp)) {
                    Text("TERMINAL SYNC", color = SlateText, style = TerminalTypography.labelSmall)
                    val now = Instant.now()
                    Text(DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.systemDefault()).format(now), color = Color.White, style = TerminalTypography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        

        // UI Controls removed: category pills and look-ahead slider were intentionally removed per design
        var expandedMap = remember { mutableStateMapOf<Int, Boolean>() }

        // Main area
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.weight(0.9f).fillMaxHeight().padding(end = 8.dp)) {
                // Section header (compact)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("UPCOMING INTEL NODES", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Surface(color = Color(0xFF0F1B24), shape = RoundedCornerShape(8.dp)) {
                        Text("90% BIAS", color = IndigoAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                        val listState = rememberLazyListState()
                        // hide/show global header based on scroll direction
                        LaunchedEffect(listState) {
                            var previous = 0L
                            snapshotFlow { listState.firstVisibleItemIndex.toLong() * 100000L + listState.firstVisibleItemScrollOffset }
                                .collect { cur ->
                                    if (cur > previous) {
                                        // scrolled forward (user swiped up) -> hide header
                                        viewModel?.setGlobalHeaderVisible(false)
                                    } else if (cur < previous) {
                                        // scrolled backward (user swiped down) -> show header
                                        viewModel?.setGlobalHeaderVisible(true)
                                    }
                                    previous = cur
                                }
                        }

                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    val upcoming = events.filter { it.status == MacroEventStatus.UPCOMING }
                    items(upcoming) { ev ->
                        val key = ev.hashCode()
                        val expanded = expandedMap.getOrPut(key) { false }
                        Surface(color = Color(0xFF071017), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                            expandedMap[key] = !(expandedMap[key] ?: false)
                        }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(52.dp).background(Color(0xFF08121A), shape = RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Public, contentDescription = null, tint = IndigoAccent)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = when (ev.priority) { ImpactPriority.CRITICAL -> RoseError; ImpactPriority.HIGH -> Color(0xFF2B2B2B); else -> IndigoAccent }, shape = RoundedCornerShape(8.dp)) {
                                                Text(ev.priority.name, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF494F55), shape = RoundedCornerShape(4.dp)))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(ev.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Public, contentDescription = null, tint = SlateText, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(ev.source, color = SlateText, fontSize = 10.sp)
                                            }
                                            val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.systemDefault())
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(fmt.format(Instant.ofEpochMilli(ev.datetimeUtc)), color = SlateText, fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(color = Color(0xFF0F6F52), shape = RoundedCornerShape(8.dp)) { Text("UPCOMING", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp) }
                                            }
                                        }

                                        // Expanded Intent Layer
                                        if (expandedMap[key] == true) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(onClick = { viewModel?.navigateTo(com.asc.markets.data.AppView.ANALYSIS_RESULTS) }) { Text("VIEW CONTEXT", color = Color.White) }
                                                TextButton(onClick = { expandedMap[key] = false }) { Text("MINIMIZE", color = SlateText) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right column removed to match design (Confirmed History panel omitted)
        }
    }
}

// sampleMacroEvents now provided by com.asc.markets.data.SampleData

@Composable
private fun DominancePill(events: List<MacroEvent>) {
    val upcoming = events.count { it.status == MacroEventStatus.UPCOMING }
    val confirmed = events.count { it.status == MacroEventStatus.CONFIRMED }
    val total = (upcoming + confirmed).coerceAtLeast(1)
    val upcomingWeight = upcoming.toFloat() / total
    val confirmedWeight = confirmed.toFloat() / total

    Surface(color = Color(0xFF0F1B24), shape = RoundedCornerShape(12.dp), modifier = Modifier.width(140.dp).height(20.dp)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(if (upcomingWeight.isNaN()) 0.9f else upcomingWeight).fillMaxHeight().background(IndigoAccent.copy(alpha = 0.95f)))
            Box(modifier = Modifier.weight(if (confirmedWeight.isNaN()) 0.1f else confirmedWeight).fillMaxHeight().background(Color.White.copy(alpha = 0.06f)))
        }
    }
}
