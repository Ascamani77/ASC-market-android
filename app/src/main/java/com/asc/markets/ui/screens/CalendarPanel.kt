package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.EconomicEvent
import com.asc.markets.logic.CalendarViewModel
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EconomicCalendarScreen() {
    val viewModel: CalendarViewModel = viewModel()
    val events = viewModel.events.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val error = viewModel.error.collectAsState().value
    var selectedAsset by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { focusManager.clearFocus() }) {
        // Header
        Column(modifier = Modifier.padding(16.dp)) {
            Text("INTELLIGENCE FEED", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(12.dp))

            // Asset filter tabs
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val assets = listOf("ALL", "MACRO", "FOREX", "STOCK", "COMMODITY", "INDEX", "FUTURES", "BONDS")
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
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search icon
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = SlateText,
                        modifier = Modifier.size(18.dp)
                    )
                    
                    // Search input
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
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
                    
                    // X icon to clear search
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = SlateText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Events list
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IndigoAccent)
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error Loading Events", color = RoseError, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                }
            }
            events.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No events available", color = SlateText, fontSize = 14.sp, fontFamily = InterFontFamily)
                }
            }
            else -> {
                val filteredEvents = if (selectedAsset == "ALL") events else events.filter { it.asset_class.uppercase() == selectedAsset }
                
                // Apply search filter
                val searchedEvents = if (searchQuery.isBlank()) {
                    filteredEvents
                } else {
                    val query = searchQuery.uppercase()
                    filteredEvents.filter { event ->
                        event.title.uppercase().contains(query) ||
                        event.event_type.uppercase().contains(query) ||
                        event.assets_affected.any { it.uppercase().contains(query) } ||
                        event.narrative_summary?.uppercase()?.contains(query) ?: false ||
                        event.asset_class.uppercase().contains(query)
                    }
                }
                
                // Sort: upcoming events first, past events at bottom
                val sortedEvents = searchedEvents.sortedBy { event ->
                    val now = System.currentTimeMillis()
                    // Events with timestamps in the future come first (0), past events come last (1)
                    if (event.timestamp_utc > now) 0 else 1
                }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedEvents) { event ->
                        val isPastEvent = event.timestamp_utc <= System.currentTimeMillis()
                        InstitutionalEventCard(event, isPastEvent)
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
private fun InstitutionalEventCard(event: EconomicEvent, isPastEvent: Boolean = false) {
    val statusColor = getStatusColor(event.ebc_status, event.unlock_state, event.visual_state)
    // Use statusColor for accent stripe - matches the badge colors
    
    // Grey out past events
    val cardOpacity = if (isPastEvent) 0.3f else 1.0f
    val borderColor = if (isPastEvent) Color.DarkGray.copy(alpha = 0.3f) else HairlineBorder
    
    // Timer state to trigger recomposition every second
    var timerTick by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L) // Update every 1 second
            timerTick = System.currentTimeMillis()
        }
    }
    
    // Calculate Macro Safety Gate phase and countdown
    var displayTime: String = "N/A"
    var safetyPhase: String = "READY"
    
    try {
        val now = System.currentTimeMillis()
        val gateReleaseMs = event.gate_release_time ?: 0L
        val hardUnlockMs = event.hard_unlock_time ?: 0L
        
        // Determine current phase based on time windows
        when {
            now < gateReleaseMs -> {
                // SAFETY GATE PHASE: Time until gate releases
                safetyPhase = "LOCKED"
                val msUntil = gateReleaseMs - now
                displayTime = formatCountdown(msUntil)
            }
            now < hardUnlockMs && gateReleaseMs > 0 -> {
                // STABILIZATION PHASE: Time until hard unlock
                safetyPhase = "SOFT"
                val msUntil = hardUnlockMs - now
                displayTime = formatCountdown(msUntil)
            }
            else -> {
                // HARD UNLOCK / NORMAL OPERATIONS
                safetyPhase = "READY"
                val msUntilEvent = event.timestamp_utc - now
                displayTime = when {
                    msUntilEvent <= 0 -> "LIVE"
                    msUntilEvent < 60000 -> "${(msUntilEvent / 1000).toInt()}s"
                    msUntilEvent < 3600000 -> {
                        val mins = (msUntilEvent / 60000).toInt()
                        val secs = ((msUntilEvent % 60000) / 1000).toInt()
                        "${mins}m ${secs}s"
                    }
                    else -> {
                        val hours = (msUntilEvent / 3600000).toInt()
                        val mins = ((msUntilEvent % 3600000) / 60000).toInt()
                        val secs = ((msUntilEvent % 60000) / 1000).toInt()
                        "${hours}h ${mins}m ${secs}s"
                    }
                }
            }
        }
    } catch (e: Exception) {
        safetyPhase = "ERROR"
        displayTime = e.message?.take(10) ?: "unknown"
    }
    
    // Use timerTick to ensure recomposition happens
    @Suppress("UNUSED_EXPRESSION")
    timerTick
    
    // Calculate timer opacity based on Macro Safety Gate phase
    val timerOpacity = when {
        safetyPhase.equals("LOCKED", ignoreCase = true) -> 0.4f // Safety Gate: observation only
        safetyPhase.equals("SOFT", ignoreCase = true) -> 1.0f   // Stabilization: actionable but cautious
        safetyPhase.equals("READY", ignoreCase = true) -> 1.0f  // Hard Unlock: normal operations
        else -> 0.5f
    }
    
    val strategyBias = inferStrategyBias(event.execution_regime)
    val posture = inferPosture(event.unlock_state, event.ebc_status)

    Surface(
        color = PureBlack.copy(alpha = cardOpacity),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Top accent stripe (matches badge color = statusColor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(statusColor.copy(alpha = cardOpacity))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Row 1: Asset class + Status badges + EBC status + TIME
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(text = event.asset_class.uppercase(), color = Color.DarkGray, opacity = cardOpacity)
                        Badge(text = event.execution_regime, color = statusColor, opacity = cardOpacity)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(text = event.ebc_status, color = statusColor, opacity = cardOpacity)
                        // MACRO SAFETY GATE phase + countdown timer
                        Text(
                            "$safetyPhase: $displayTime",
                            color = Color.White.copy(alpha = timerOpacity * cardOpacity),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Row 2: Title (main event name)
                Text(
                    event.title,
                    color = Color.White.copy(alpha = cardOpacity),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Row 3: Strategy Bias | Posture | Market Vitals | Currency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    DetailColumn("STRATEGY BIAS", strategyBias.first, strategyBias.second, opacity = cardOpacity)
                    DetailColumn("POSTURE", posture.first, posture.second, opacity = cardOpacity)
                    DetailColumn("MARKET VITALS", "", Color.Transparent, vitals = true, liquidity = event.liquidity_depth, correlation = event.correlation_heat, opacity = cardOpacity)
                    
                    // Currency/Asset code on right
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "",
                            color = SlateText.copy(alpha = cardOpacity),
                            fontSize = 8.sp,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            event.assets_affected.firstOrNull() ?: "",
                            color = SlateText.copy(alpha = cardOpacity),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row 4: Narrative
                if (!event.narrative_summary.isNullOrBlank()) {
                    Text(
                        event.narrative_summary,
                        color = Color.Gray.copy(alpha = cardOpacity),
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color, opacity: Float = 1.0f) {
    Surface(
        color = color.copy(alpha = opacity),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f * opacity))
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = opacity),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DetailColumn(
    label: String,
    value: String,
    color: Color,
    vitals: Boolean = false,
    liquidity: Int = 0,
    correlation: Int = 0,
    opacity: Float = 1.0f
) {
    Column {
        Text(label, color = SlateText.copy(alpha = opacity), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        if (vitals) {
            // Market Vitals: colored dots based on values
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
                repeat(Math.round(liquidity / 20f).toInt().coerceIn(1, 5)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF22C55E).copy(alpha = opacity), RoundedCornerShape(1.dp))
                    )
                }
                repeat(Math.round(correlation / 20f).toInt().coerceIn(1, 5)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF00D9FF).copy(alpha = opacity), RoundedCornerShape(1.dp))
                    )
                }
            }
        } else {
            Text(value, color = color.copy(alpha = opacity), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
    }
}

private fun getStatusColor(ebcStatus: String, unlockState: String, visualState: String): Color {
    return when {
        ebcStatus.contains("PERMITTED", ignoreCase = true) -> Color(0xFF22C55E) // Emerald
        ebcStatus.contains("BLOCKED", ignoreCase = true) -> Color(0xFFE85D5D) // Rose Red
        unlockState.contains("SOFT", ignoreCase = true) -> Color(0xFF00D9FF) // Cyan
        visualState.contains("HIGH_CONVICTION", ignoreCase = true) -> IndigoAccent
        visualState.contains("REGIME_CONFIRMED", ignoreCase = true) -> IndigoAccent
        else -> Color.DarkGray
    }
}

private fun getAccentColor(event: EconomicEvent): Color {
    // Visual Risk Governance Color System - Regime/Visual State Priority
    return when {
        // VISUAL STATE takes highest priority (Regime Confirmation)
        event.visual_state.contains("HIGH_CONVICTION", ignoreCase = true) -> Color(0xFF6366F1) // Indigo
        event.visual_state.contains("DIRECTIONAL_CONFIRMED", ignoreCase = true) -> Color(0xFF6366F1) // Indigo
        event.visual_state.contains("TRANSITION_WATCH", ignoreCase = true) -> Color(0xFF00D9FF) // Cyan
        
        // EXECUTION REGIME
        event.execution_regime.contains("HAWKISH", ignoreCase = true) -> Color(0xFFE85D5D) // Red
        event.execution_regime.contains("DOVISH", ignoreCase = true) -> Color(0xFF00D9FF) // Cyan
        event.execution_regime.contains("VOLATILE", ignoreCase = true) -> Color(0xFFF59E0B) // Amber
        
        // Then EBC Status
        event.ebc_status.equals("PERMITTED", ignoreCase = true) -> Color(0xFF10B981) // Emerald
        event.ebc_status.equals("BLOCKED", ignoreCase = true) -> Color(0xFFE85D5D) // Red
        event.ebc_status.equals("DEGRADED", ignoreCase = true) -> Color(0xFFF59E0B) // Amber
        
        // Then unlock_state
        event.unlock_state.equals("LOCKED", ignoreCase = true) -> Color(0xFFE85D5D) // Red
        event.unlock_state.equals("HARD_UNLOCK", ignoreCase = true) -> Color(0xFFE85D5D) // Red
        event.safety_gate -> Color(0xFFE85D5D) // Red
        event.unlock_state.equals("SOFT_UNLOCK", ignoreCase = true) -> Color(0xFF00D9FF) // Cyan
        
        // Stale signals
        event.persistence_count < 2 -> Color(0xFF52525B) // Dark Gray
        event.transition_status.equals("aborted", ignoreCase = true) -> Color(0xFF52525B) // Dark Gray
        
        else -> Color(0xFF6B7280) // Default gray
    }
}

private fun inferStrategyBias(regime: String): Pair<String, Color> {
    return when {
        regime.contains("HAWKISH", ignoreCase = true) -> "AGGRESSIVE" to Color(0xFFE85D5D)
        regime.contains("DOVISH", ignoreCase = true) -> "DEFENSIVE" to Color(0xFF00D9FF)
        else -> "NEUTRAL" to Color.Gray
    }
}

private fun inferPosture(unlockState: String, ebcStatus: String): Pair<String, Color> {
    return when {
        unlockState.contains("LOCKED", ignoreCase = true) -> "HALTED" to Color(0xFFE85D5D)
        ebcStatus.contains("DEGRADED", ignoreCase = true) -> "DEFENSIVE" to Color(0xFF00D9FF)
        ebcStatus.contains("PERMITTED", ignoreCase = true) -> "AGGRESSIVE" to Color(0xFF22C55E)
        else -> "NEUTRAL" to Color.Gray
    }
}

private fun formatCountdown(ms: Long): String {
    return when {
        ms <= 0 -> "0s"
        ms < 60000 -> "${(ms / 1000).toInt()}s"
        ms < 3600000 -> {
            val mins = (ms / 60000).toInt()
            val secs = ((ms % 60000) / 1000).toInt()
            "${mins}m ${secs}s"
        }
        ms < 86400000 -> {
            val hours = (ms / 3600000).toInt()
            val mins = ((ms % 3600000) / 60000).toInt()
            val secs = ((ms % 60000) / 1000).toInt()
            "${hours}h ${mins}m ${secs}s"
        }
        else -> {
            val days = (ms / 86400000).toInt()
            val hours = ((ms % 86400000) / 3600000).toInt()
            val mins = ((ms % 3600000) / 60000).toInt()
            "${days}d ${hours}h ${mins}m"
        }
    }
}