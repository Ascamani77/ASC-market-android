package com.asc.markets.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.platform.LocalFocusManager
import com.asc.markets.data.*
import com.asc.markets.logic.IntelligenceUiState
import com.asc.markets.logic.IntelligenceViewModel
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IntelligenceDashboardScreen(
    viewModel: IntelligenceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    
    var selectedAsset by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { focusManager.clearFocus() },
        color = DeepBlack
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section matching reference image
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "INTELLIGENCE FEED", 
                    color = SlateText, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 2.sp, 
                    fontFamily = InterFontFamily
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Asset filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val assets = listOf("ALL", "MACRO", "FOREX", "STOCK", "COMMODITY")
                    assets.forEach { asset ->
                        FilterTab(
                            label = asset,
                            isSelected = selectedAsset == asset,
                            onClick = { selectedAsset = asset }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search bar
                val searchInteractionSource = remember { MutableInteractionSource() }
                val isSearchFocused = searchInteractionSource.collectIsFocusedAsState().value
                
                Surface(
                    color = Color.DarkGray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = SlateText,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily
                            ),
                            cursorBrush = SolidColor(Color.White),
                            interactionSource = searchInteractionSource,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty() && !isSearchFocused) {
                                    Text(
                                        "SEARCH ASSETS OR INSTRUMENTS",
                                        color = SlateText,
                                        fontSize = 11.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }
                                innerTextField()
                            }
                        )
                        
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = SlateText,
                                modifier = Modifier.size(18.dp).clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val state = uiState) {
                    is IntelligenceUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IndigoAccent)
                        }
                    }
                    is IntelligenceUiState.Success -> {
                        // Apply filters
                        val filteredEvents = if (selectedAsset == "ALL") state.events else state.events.filter { it.asset_class.name.uppercase() == selectedAsset }
                        val searchedEvents = if (searchQuery.isBlank()) {
                            filteredEvents
                        } else {
                            val query = searchQuery.uppercase()
                            filteredEvents.filter { event ->
                                event.title.uppercase().contains(query) ||
                                event.narrative_summary.uppercase().contains(query) ||
                                event.asset_class.name.uppercase().contains(query)
                            }
                        }

                        if (searchedEvents.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No intelligence events found", color = SlateText, fontSize = 14.sp, fontFamily = InterFontFamily)
                            }
                        } else {
                            EventList(
                                events = searchedEvents,
                                watchlist = watchlist,
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                            )
                        }
                    }
                    is IntelligenceUiState.Error -> {
                        // Styled error message matching reference image
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Text(
                                    "Error Loading Events", 
                                    color = RoseError, 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Black, 
                                    fontFamily = InterFontFamily
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    state.message, 
                                    color = SlateText, 
                                    fontSize = 12.sp, 
                                    fontFamily = InterFontFamily,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (isSelected) Color.Black else Color.DarkGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

    val isLocked = event.unlock_state == IntelligenceUnlockState.LOCKED
    val isSoft = event.unlock_state == IntelligenceUnlockState.SOFT_UNLOCK
    val isHard = event.unlock_state == IntelligenceUnlockState.HARD_UNLOCK
    val isPast = event.timestamp_utc < System.currentTimeMillis()

    LaunchedEffect(event.gate_release_time, event.hard_unlock_time) {
        val target = if (isLocked) event.gate_release_time else if (isSoft) event.hard_unlock_time else null
        val start = if (isLocked) event.timestamp_utc else if (isSoft) event.gate_release_time else null

        if (target != null && target > 0 && start != null && start > 0) {
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
            event.ebc?.status == IntelligenceEBCStatus.BLOCKED -> {
                CardStyles(
                    cardBorder = RoseError.copy(alpha = 0.5f),
                    cardBg = RoseError.copy(alpha = 0.05f),
                    badgeBg = RoseError,
                    badgeText = Color.White,
                    headerText = Color.White
                )
            }
            event.transition_status == TransitionStatus.aborted -> {
                CardStyles(
                    cardBorder = Color(0xFF27272A),
                    cardBg = PureBlack,
                    badgeBg = PureBlack,
                    badgeText = Color(0xFF52525B),
                    headerText = Color(0xFF52525B)
                )
            }
            isSoft -> {
                CardStyles(
                    cardBorder = Color(0xFF06B6D4).copy(alpha = 0.3f),
                    cardBg = PureBlack,
                    badgeBg = Color(0xFF0891B2),
                    badgeText = Color.White,
                    headerText = Color.White
                )
            }
            isHard && isPast && event.persistence_count >= 2 -> {
                CardStyles(
                    cardBorder = IndigoAccent.copy(alpha = 0.3f),
                    cardBg = PureBlack,
                    badgeBg = IndigoAccent,
                    badgeText = Color.White,
                    headerText = Color.White
                )
            }
            else -> {
                CardStyles(
                    cardBorder = Color(0xFF27272A),
                    cardBg = PureBlack,
                    badgeBg = Color(0xFF27272A),
                    badgeText = Color(0xFF71717A),
                    headerText = Color(0xFFA1A1AA)
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
            .border(1.dp, if (isWatched) IndigoAccent.copy(alpha = 0.4f) else styles.cardBorder, RoundedCornerShape(8.dp))
            .padding(20.dp)
    ) {
        // Watchlist Star
        Box(
            modifier = Modifier
                .offset(x = (-10).dp, y = (-10).dp)
                .size(28.dp)
                .background(PureBlack, CircleShape)
                .border(1.dp, if (isWatched) IndigoAccent else Color(0xFF27272A), CircleShape)
                .clickable { onToggleWatchlist(event.id) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Watchlist",
                tint = if (isWatched) IndigoAccent else Color(0xFF27272A),
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
                            color = Color(0xFF3F3F46),
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
                                    .background(PureBlack, CircleShape)
                                    .border(1.dp, Color(0xFF27272A), CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress / 100f)
                                        .background(if (isLocked) Color(0xFFDC2626) else Color(0xFF06B6D4))
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
                            .background(if (event.ebc?.status == IntelligenceEBCStatus.BLOCKED) Color(0xFFDC2626) else PureBlack)
                            .border(1.dp, if (event.ebc?.status == IntelligenceEBCStatus.BLOCKED) RoseError else Color(0xFF27272A), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(if (event.ebc?.status == IntelligenceEBCStatus.BLOCKED) Color.White else EmeraldSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EBC: ${event.ebc?.status?.name ?: "UNKNOWN"}",
                                color = if (event.ebc?.status == IntelligenceEBCStatus.BLOCKED) Color.White else EmeraldSuccess,
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
                            .background(PureBlack)
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dateStr,
                            color = Color(0xFFA1A1AA),
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
                    .background(PureBlack)
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(99.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.strategy_context != null) {
                        Column(modifier = Modifier.padding(end = 20.dp)) {
                            Text(
                                text = "STRATEGY BIAS",
                                color = Color(0xFF3F3F46),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            val biasColor = when (event.strategy_context.bias) {
                                "long" -> EmeraldSuccess
                                "short" -> RoseError
                                else -> Color(0xFF52525B)
                            }
                            Text(
                                text = event.strategy_context.bias.uppercase(),
                                color = biasColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(modifier = Modifier.size(1.dp, 20.dp).background(Color(0xFF27272A)))
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.padding(end = 20.dp)) {
                            Text(
                                text = "POSTURE",
                                color = Color(0xFF3F3F46),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            val postureColor = when (event.strategy_context.risk_posture) {
                                "aggressive" -> EmeraldSuccess
                                "halted" -> RoseError
                                else -> Color(0xFF52525B)
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
                            color = Color(0xFF3F3F46),
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
                                            .background(if (i < corrHeat) IndigoAccent else PureBlack)
                                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(1.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(if (i < liqDepth) Color(0xFF059669) else PureBlack)
                                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(1.dp))
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
                                .background(PureBlack)
                                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = asset,
                                color = Color(0xFF3F3F46),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (event.ebc?.status == IntelligenceEBCStatus.BLOCKED) "Boundary contract breach: Observation-Only mode mandated." else event.narrative_summary,
                color = if (event.strategy_context?.risk_posture == "defensive" || event.strategy_context?.risk_posture == "halted") Color(0xFF52525B) else Color(0xFF71717A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF27272A)))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.source.uppercase(),
                    color = Color(0xFF27272A),
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
                                .background(if (i < event.persistence_count) Color(0xFF059669) else PureBlack)
                                .border(1.dp, Color(0xFF27272A), CircleShape)
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
                    color = Color(0xFF27272A),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF27272A),
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
                            .background(PureBlack)
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(99.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MARKET STABILIZATION",
                                    color = Color(0xFF3F3F46),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                val stabilizingLabel = if ((event.liquidity_depth ?: 0) > 70 && (event.correlation_heat ?: 0) > 70) "Structure stabilizing" 
                                                       else if ((event.liquidity_depth ?: 0) < 40 || (event.correlation_heat ?: 0) < 40) "Fragmented market" 
                                                       else "Normalizing"
                                val labelColor = if (stabilizingLabel == "Fragmented market") RoseError else if (stabilizingLabel == "Structure stabilizing") EmeraldSuccess else Color(0xFFD97706)
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
                                    Text("LIQUIDITY RECOVERY", color = Color(0xFF27272A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${event.liquidity_depth ?: 0}%", color = Color(0xFF71717A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(PureBlack, CircleShape).border(1.dp, Color(0xFF27272A), CircleShape)) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((event.liquidity_depth ?: 0) / 100f).background(getStabilizationColor(event.liquidity_depth ?: 0)))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Correlation Bar
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CORRELATION ALIGNMENT", color = Color(0xFF27272A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${event.correlation_heat ?: 0}%", color = Color(0xFF71717A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(PureBlack, CircleShape).border(1.dp, Color(0xFF27272A), CircleShape)) {
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
                                .background(PureBlack)
                                .border(1.dp, IndigoAccent.copy(alpha = 0.3f), RoundedCornerShape(99.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TACTICAL RATIONALE",
                                    color = IndigoAccent,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = event.strategy_context.rationale,
                                    color = Color(0xFFA1A1AA),
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
                                color = Color(0xFF27272A),
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
                                            .background(if (trigger.status == "met") EmeraldSuccess else PureBlack, CircleShape)
                                            .border(1.dp, if (trigger.status == "met") EmeraldSuccess else Color(0xFF27272A), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trigger.label,
                                        color = if (trigger.status == "met") EmeraldSuccess else Color(0xFF27272A),
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
                                color = Color(0xFF27272A),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (event.ebc?.violations?.isNotEmpty() == true) {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    event.ebc.violations.forEach { violation ->
                                        val label = violation.split(":").firstOrNull() ?: violation
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(RoseError.copy(alpha = 0.2f))
                                                .border(1.dp, RoseError.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = RoseError,
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
                                        .background(EmeraldSuccess.copy(alpha = 0.1f))
                                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NOMINAL STATUS",
                                        color = EmeraldSuccess,
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

fun getStabilizationColor(valInt: Int): Color {
    return when {
        valInt < 40 -> Color(0xFFDC2626)
        valInt <= 70 -> Color(0xFFD97706)
        else -> Color(0xFF059669)
    }
}

data class CardStyles(
    val cardBorder: Color,
    val cardBg: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val headerText: Color
)
