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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class StreamItem(
    val id: String,
    val pair: String,
    val tag: String,
    val category: String,
    val headline: String,
    val timestampMillis: Long,
    val severity: String,
    val refId: String,
    val confidence: Int,
    val alignment: String
)

@Composable
fun IntelligenceStreamScreen(viewModel: ForexViewModel = viewModel()) {
    val now = System.currentTimeMillis()
    val sample = remember {
        listOf(
            StreamItem("N1","EUR/USD","Target","POSSIBLE ENTRY","EUR/USD LONG SETUP DETECTED", now - 6_200, "INFO", "LOG::E1-L14", 84, "Confirmed"),
            StreamItem("Z1","GBP/USD","Layers","ZONE GUARDS","GBP/USD SUPPLY ZONE BREACH", now - 120_500, "WARNING", "LOG::Z1-L02", 62, "Monitoring"),
            StreamItem("01","DXY","Brain","OBSERVATIONS","DXY MOMENTUM SHIFT", now - 3600_345, "INFO", "LOG::O1-L88", 47, "Active"),
            StreamItem("Z2","BTC/USD","Layers","ZONE GUARDS","BTC/USDT LIQUIDITY SWEEP", now - 9_500, "CRITICAL", "LOG::Z2-L99", 91, "Confirmed")
        )
    }

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

    // lead-time mock progress (0..1)
    var leadProgress by remember { mutableStateOf(0.35f) }
    val coroutineScope = rememberCoroutineScope()
    // gently advance the leadProgress for demo
    coroutineScope.launch { while (true) { delay(5_000); leadProgress = (leadProgress + 0.03f).coerceAtMost(1f) } }

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
                                "INTELLIGENCE STREAM",
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
                    val pills = listOf("ALL", "POSSIBLE ENTRY", "ZONE GUARDS", "OBSERVATIONS", "MACRO NEWS", "SYSTEM")
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

                // Stream list (apply search + category filters)
                val filtered = remember(sample, query, selectedCategory) {
                    sample.filter { it ->
                        val q = query.trim().lowercase()
                        val matchesQuery = q.isEmpty() || it.pair.lowercase().contains(q) || it.headline.lowercase().contains(q) || it.refId.lowercase().contains(q) || it.category.lowercase().contains(q)
                        val matchesCategory = selectedCategory == "ALL" || it.category.equals(selectedCategory, true)
                        matchesQuery && matchesCategory
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        // Situational awareness: heartbeat + lead-time inside InfoBox - positioned at top (touches screen edge)
                        com.asc.markets.ui.components.InfoBox(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                // Node heartbeat
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = if (pulse > 0.8f) Color(0xFF2EE08A) else Color(0xFF1B7A49), modifier = Modifier.size((8.dp * pulse))) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Node: NY4/LD4", color = SlateText, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                // Lead-time
                                Column(modifier = Modifier.width(200.dp)) {
                                    Text("Lead-Time: next macro in ~12m", color = SlateText, fontSize = 12.sp)
                                    LinearProgressIndicator(progress = leadProgress, modifier = Modifier.fillMaxWidth().height(6.dp), color = IndigoAccent, trackColor = Color(0xFF0B0B0B))
                                }
                            }
                        }
                    }

                    items(filtered) { item ->
                        StreamCard(item)
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
                Text("LEDGER: ${sample.size} events captured this session.", color = SlateText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("STREAM MAINTENANCE", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.navigateTo(com.asc.markets.data.AppView.NOTIFICATIONS) }.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun StreamCard(item: StreamItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureBlack),
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
                        when (item.tag.lowercase()) {
                            "target" -> Icon(Icons.Filled.MyLocation, contentDescription = "Target", tint = Color.White)
                            "layers" -> Icon(Icons.Filled.Layers, contentDescription = "Layers", tint = Color.White)
                            "brain" -> Icon(Icons.Filled.Psychology, contentDescription = "Brain", tint = Color.White)
                            "newspaper" -> Icon(Icons.Filled.Article, contentDescription = "News", tint = Color.White)
                            else -> Icon(Icons.Filled.Search, contentDescription = "Other", tint = Color.White)
                        }
                    }
                }
                // minimal space
                Spacer(modifier = Modifier.width(6.dp))
                // pair and tag
                Text(
                    item.pair,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                val plainTagKinds = setOf("brain", "target", "layers")
                if (item.tag.lowercase() in plainTagKinds) {
                    val (tagColor, catColor) = when (item.tag.lowercase()) {
                        "target" -> Pair(Color(0xFFE53935), Color(0xFFEF9A9A)) // Red for Target
                        "layers" -> Pair(Color(0xFF1E88E5), Color(0xFF90CAF9)) // Blue for Layers
                        "brain" -> Pair(Color(0xFF43A047), Color(0xFFA5D6A7)) // Green for Brain
                        else -> Pair(SlateText, SlateText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.tag,
                            color = tagColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            item.category,
                            color = SlateText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                } else {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF111827)) {
                        Text(item.tag, color = SlateText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // timestamp with milliseconds UTC
                val fmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS 'UTC'").withZone(ZoneOffset.UTC)
                Text(fmt.format(Instant.ofEpochMilli(item.timestampMillis)), color = SlateText, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(item.headline, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(12.dp))

                    var expanded by remember { mutableStateOf(false) }

                    if (!expanded) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.refId,
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
                        val detectionDetail = when {
                            item.pair.contains("DXY") ->
                                "US DOLLAR INDEX SHOWING SIGNS OF DISTRIBUTION AT 104.50. RISK-ON BIAS LIKELY FOR UPCOMING LONDON OPEN."
                            item.pair.contains("EUR/USD") && item.category.equals("POSSIBLE ENTRY", true) ->
                                "EUR/USD LONG SETUP DETECTED. POTENTIAL BREAKOUT ABOVE 1.0900. WATCH FOR CONFIRMATION ON VOLUME."
                            item.pair.contains("GBP/USD") && item.category.equals("ZONE GUARDS", true) ->
                                "GBP/USD SUPPLY ZONE BREACH. PRICE TESTING 1.2700. MONITOR FOR REVERSAL OR CONTINUATION."
                            item.pair.contains("BTC/USD") && item.category.equals("ZONE GUARDS", true) ->
                                "BTC/USDT LIQUIDITY SWEEP. LARGE ORDERS DETECTED NEAR 42000. EXPECT VOLATILITY IN THE NEXT SESSION."
                            else ->
                                (item.headline.uppercase() + ".")
                        }
                        Text(detectionDetail, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)

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
                                        Text("RELAY_SYNC_NOMINAL", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.height(12.dp))

                        // AI Confidence & Alignment
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Confidence", color = SlateText, fontSize = 11.sp)
                                LinearProgressIndicator(
                                    progress = item.confidence / 100f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (item.confidence > 75) Color(0xFF2EE08A) else if (item.confidence > 50) Color(0xFFFFC107) else Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("${item.confidence}%", color = Color.White, fontSize = 12.sp)
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
                                    color = when (item.alignment.lowercase()) {
                                        "confirmed" -> Color(0xFF2EE08A)
                                        "active" -> Color(0xFFFFC107)
                                        else -> Color(0xFF374151)
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        item.alignment.uppercase(),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.refId, color = SlateText)
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


