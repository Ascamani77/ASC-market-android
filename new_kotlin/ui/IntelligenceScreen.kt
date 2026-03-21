package com.intelligence.dashboard.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intelligence.dashboard.model.*
import com.intelligence.dashboard.ui.theme.*
import com.intelligence.dashboard.viewmodel.IntelligenceUiState
import com.intelligence.dashboard.viewmodel.IntelligenceViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- THEME COLORS ---
// (Moved to com.intelligence.dashboard.ui.theme.Theme.kt)

@Composable
fun IntelligenceDashboardScreen(
    viewModel: IntelligenceViewModel = IntelligenceViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ColorBlack
    ) {
        when (val state = uiState) {
            is IntelligenceUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorIndigo500)
                }
            }
            is IntelligenceUiState.Success -> {
                EventList(
                    events = state.events,
                    watchlist = watchlist,
                    onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                )
            }
            is IntelligenceUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "System Fault: ${state.message}",
                        color = ColorRose500,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EventList(
    events: List<IntelligenceEvent>,
    watchlist: Set<String>,
    onToggleWatchlist: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ColorIndigo500, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "LIVE SESSION",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(ColorZinc800, Color.Transparent)
                            )
                        )
                )
            }
        }
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                isWatched = watchlist.contains(event.id),
                onToggleWatchlist = onToggleWatchlist
            )
        }
    }
}

@Composable
fun EventCard(
    event: IntelligenceEvent,
    isWatched: Boolean,
    onToggleWatchlist: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var timerText by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(100f) }

    val isLocked = event.unlock_state == UnlockState.LOCKED
    val isSoft = event.unlock_state == UnlockState.SOFT_UNLOCK
    val isHard = event.unlock_state == UnlockState.HARD_UNLOCK
    val isPast = event.timestamp_utc < System.currentTimeMillis()

    LaunchedEffect(event.gate_release_time, event.hard_unlock_time) {
        val target = if (isLocked) event.gate_release_time else if (isSoft) event.hard_unlock_time else null
        val start = if (isLocked) event.timestamp_utc else if (isSoft) event.gate_release_time else null

        if (target != null && start != null) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = target - now
                if (diff <= 0) {
                    timerText = "00:00"
                    progress = 0f
                    break
                }
                val mins = (diff / 60000).toInt()
                val secs = ((diff % 60000) / 1000).toInt()
                timerText = String.format("%02d:%02d", mins, secs)

                val total = target - start
                val elapsed = now - start
                progress = (100f - (elapsed.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
                delay(1000)
            }
        } else {
            timerText = ""
            progress = 100f
        }
    }

    val styles = remember(event.ebc?.status, event.transition_status, isSoft, isHard, isPast, event.persistence_count) {
        when {
            event.ebc?.status == EBCStatus.BLOCKED -> {
                CardStyles(
                    cardBorder = ColorRose600.copy(alpha = 0.5f),
                    cardBg = ColorRose600.copy(alpha = 0.05f),
                    badgeBg = ColorRose600,
                    badgeText = Color.White,
                    headerText = Color.White
                )
            }
            event.transition_status == TransitionStatus.aborted -> {
                CardStyles(
                    cardBorder = ColorZinc800,
                    cardBg = ColorBlack,
                    badgeBg = ColorBlack,
                    badgeText = ColorZinc600,
                    headerText = ColorZinc600
                )
            }
            isSoft -> {
                CardStyles(
                    cardBorder = ColorCyan500.copy(alpha = 0.3f),
                    cardBg = ColorBlack,
                    badgeBg = ColorCyan600,
                    badgeText = Color.White,
                    headerText = Color.White
                )
            }
            isHard && isPast && event.persistence_count >= 2 -> {
                CardStyles(
                    cardBorder = ColorIndigo500.copy(alpha = 0.3f),
                    cardBg = ColorBlack,
                    badgeBg = ColorIndigo600,
                    badgeText = Color.White,
                    headerText = Color.White
                )
            }
            else -> {
                CardStyles(
                    cardBorder = ColorZinc800,
                    cardBg = ColorBlack,
                    badgeBg = ColorZinc800,
                    badgeText = ColorZinc500,
                    headerText = ColorZinc400
                )
            }
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateStr = remember(event.timestamp_utc) { timeFormat.format(Date(event.timestamp_utc)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(styles.cardBg)
            .border(1.dp, if (isWatched) ColorIndigo500.copy(alpha = 0.4f) else styles.cardBorder, RoundedCornerShape(8.dp))
            .padding(20.dp)
    ) {
        // Watchlist Star
        Box(
            modifier = Modifier
                .offset(x = (-10).dp, y = (-10).dp)
                .size(28.dp)
                .background(ColorBlack, CircleShape)
                .border(1.dp, if (isWatched) ColorIndigo500 else ColorZinc800, CircleShape)
                .clickable { onToggleWatchlist(event.id) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Watchlist",
                tint = if (isWatched) ColorIndigo500 else ColorZinc800,
                modifier = Modifier.size(12.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.asset_class.name.uppercase(),
                            color = ColorZinc700,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(styles.badgeBg)
                                .border(1.dp, styles.cardBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val label = if (timerText.isNotEmpty()) {
                                if (isLocked) "LOCKED [$timerText]" else "SOFT [$timerText]"
                            } else {
                                if (isSoft) "SOFT UNLOCK" 
                                else if (isHard && isPast && event.persistence_count >= 2) "REGIME CONFIRMED"
                                else "PLANNED"
                            }
                            Text(
                                text = label,
                                color = styles.badgeText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (timerText.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(ColorBlack, CircleShape)
                                    .border(1.dp, ColorZinc800, CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress / 100f)
                                        .background(if (isLocked) ColorRose600 else ColorCyan500)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.title,
                        color = styles.headerText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (event.ebc?.status == EBCStatus.BLOCKED) ColorRose600 else ColorBlack)
                            .border(1.dp, if (event.ebc?.status == EBCStatus.BLOCKED) ColorRose500 else ColorZinc800, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(if (event.ebc?.status == EBCStatus.BLOCKED) Color.White else ColorEmerald500, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EBC: ${event.ebc?.status?.name ?: "UNKNOWN"}",
                                color = if (event.ebc?.status == EBCStatus.BLOCKED) Color.White else ColorEmerald500,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorBlack)
                            .border(1.dp, ColorZinc800, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dateStr,
                            color = ColorZinc400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Strategy & Vitals
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(99.dp))
                    .background(ColorBlack)
                    .border(1.dp, ColorZinc800, RoundedCornerShape(99.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.strategy_context != null) {
                        Column(modifier = Modifier.padding(end = 20.dp)) {
                            Text(
                                text = "STRATEGY BIAS",
                                color = ColorZinc700,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            val biasColor = when (event.strategy_context.bias) {
                                "long" -> ColorEmerald500
                                "short" -> ColorRose500
                                else -> ColorZinc600
                            }
                            Text(
                                text = event.strategy_context.bias.uppercase(),
                                color = biasColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(modifier = Modifier.size(1.dp, 20.dp).background(ColorZinc800))
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.padding(end = 20.dp)) {
                            Text(
                                text = "POSTURE",
                                color = ColorZinc700,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            val postureColor = when (event.strategy_context.risk_posture) {
                                "aggressive" -> ColorEmerald500
                                "halted" -> ColorRose500
                                else -> ColorZinc600
                            }
                            Text(
                                text = event.strategy_context.risk_posture.uppercase(),
                                color = postureColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Vitals
                    Column {
                        Text(
                            text = "MARKET VITALS",
                            color = ColorZinc700,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(6) { i ->
                                val corrHeat = (event.correlation_heat ?: 50) / 100f * 6
                                val liqDepth = (event.liquidity_depth ?: 50) / 100f * 6
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(if (i < corrHeat) ColorIndigo500 else ColorBlack)
                                            .border(1.dp, ColorZinc800, RoundedCornerShape(1.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(if (i < liqDepth) ColorEmerald600 else ColorBlack)
                                            .border(1.dp, ColorZinc800, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Assets
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    event.assets_affected.take(3).forEach { asset ->
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ColorBlack)
                                .border(1.dp, ColorZinc800, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = asset,
                                color = ColorZinc700,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (event.ebc?.status == EBCStatus.BLOCKED) "Boundary contract breach: Observation-Only mode mandated." else event.narrative_summary,
                color = if (event.strategy_context?.risk_posture == "defensive" || event.strategy_context?.risk_posture == "halted") ColorZinc600 else ColorZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorZinc800))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.source.uppercase(),
                    color = ColorZinc800,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .size(12.dp, 4.dp)
                                .clip(CircleShape)
                                .background(if (i < event.persistence_count) ColorEmerald600 else ColorBlack)
                                .border(1.dp, ColorZinc800, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTELLIGENCE MONITOR [${if (expanded) "CLOSE" else "OPEN"}]",
                    color = ColorZinc800,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ColorZinc800,
                    modifier = Modifier.size(12.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Monitor Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(99.dp))
                            .background(ColorBlack)
                            .border(1.dp, ColorZinc800, RoundedCornerShape(99.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MARKET STABILIZATION",
                                    color = ColorZinc700,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                val stabilizingLabel = if ((event.liquidity_depth ?: 0) > 70 && (event.correlation_heat ?: 0) > 70) "Structure stabilizing" 
                                                       else if ((event.liquidity_depth ?: 0) < 40 || (event.correlation_heat ?: 0) < 40) "Fragmented market" 
                                                       else "Normalizing"
                                val labelColor = if (stabilizingLabel == "Fragmented market") ColorRose500 else if (stabilizingLabel == "Structure stabilizing") ColorEmerald500 else ColorAmber600
                                Text(
                                    text = stabilizingLabel.uppercase(),
                                    color = labelColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Liquidity Bar
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("LIQUIDITY RECOVERY", color = ColorZinc800, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${event.liquidity_depth ?: 0}%", color = ColorZinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(ColorBlack, CircleShape).border(1.dp, ColorZinc800, CircleShape)) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((event.liquidity_depth ?: 0) / 100f).background(getStabilizationColor(event.liquidity_depth ?: 0)))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Correlation Bar
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CORRELATION ALIGNMENT", color = ColorZinc800, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${event.correlation_heat ?: 0}%", color = ColorZinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(ColorBlack, CircleShape).border(1.dp, ColorZinc800, CircleShape)) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((event.correlation_heat ?: 0) / 100f).background(getStabilizationColor(event.correlation_heat ?: 0)))
                                }
                            }
                        }
                    }
                    
                    if (event.strategy_context != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(99.dp))
                                .background(ColorBlack)
                                .border(1.dp, ColorIndigo600.copy(alpha = 0.3f), RoundedCornerShape(99.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TACTICAL RATIONALE",
                                    color = ColorIndigo500,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = event.strategy_context.rationale,
                                    color = ColorZinc400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Protocol Triggers
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PROTOCOL TRIGGERS",
                                color = ColorZinc800,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            event.transition_triggers?.forEach { trigger ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(if (trigger.status == "met") ColorEmerald500 else ColorBlack, CircleShape)
                                            .border(1.dp, if (trigger.status == "met") ColorEmerald500 else ColorZinc800, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trigger.label,
                                        color = if (trigger.status == "met") ColorEmerald500 else ColorZinc800,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Boundary Faults
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BOUNDARY FAULTS",
                                color = ColorZinc800,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (event.ebc?.violations?.isNotEmpty() == true) {
                                FlowRow(
                                    mainAxisSpacing = 4.dp,
                                    crossAxisSpacing = 4.dp
                                ) {
                                    event.ebc.violations.forEach { violation ->
                                        val label = violation.split(":").firstOrNull() ?: violation
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ColorRose600.copy(alpha = 0.2f))
                                                .border(1.dp, ColorRose600.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = ColorRose500,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ColorEmerald600.copy(alpha = 0.1f))
                                        .border(1.dp, ColorEmerald600.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NOMINAL STATUS",
                                        color = ColorEmerald600,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing),
        content = { content() }
    )
}

fun getStabilizationColor(valInt: Int): Color {
    return when {
        valInt < 40 -> ColorRose600
        valInt <= 70 -> ColorAmber600
        else -> ColorEmerald600
    }
}

data class CardStyles(
    val cardBorder: Color,
    val cardBg: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val headerText: Color
)
