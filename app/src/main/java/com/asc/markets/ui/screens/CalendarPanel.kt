package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.CalendarViewModel
import com.asc.markets.ui.theme.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import com.asc.markets.data.EconomicEvent

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
            Text("INTELLIGENCE FEED", color = SlateText.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(16.dp))

            // Asset filter tabs
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

            Spacer(modifier = Modifier.height(20.dp))

            // Search bar
            Surface(
                color = PureBlack,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = InterFontFamily),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("SEARCH ASSETS OR INSTRUMENTS...", color = Color.DarkGray, fontSize = 12.sp, fontFamily = InterFontFamily)
                            }
                            innerTextField()
                        }
                    )
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Live Session Header
        LiveSessionHeader()

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
                
                val searchedEvents = if (searchQuery.isBlank()) {
                    filteredEvents
                } else {
                    val query = searchQuery.uppercase()
                    filteredEvents.filter { event ->
                        event.title.uppercase().contains(query) ||
                        event.asset_class.uppercase().contains(query)
                    }
                }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchedEvents) { event ->
                        val isPastEvent = event.timestamp_utc <= System.currentTimeMillis()
                        InstitutionalEventCard(event, isPastEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSessionHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF6366F1), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "LIVE SESSION",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontFamily = InterFontFamily
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.1f))
        )
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
    val cardOpacity = if (isPastEvent) 0.4f else 1.0f
    
    // Status Badge Logic
    val statusBadgeText = when {
        event.unlock_state.contains("SOFT", ignoreCase = true) || event.unlock_state.contains("LOCKED", ignoreCase = true) -> {
            val gateTime = if (event.gate_release_time != null) formatTime(event.gate_release_time) else "14:21"
            if (event.unlock_state.contains("LOCKED", ignoreCase = true)) "LOCKED [$gateTime]" else "SOFT [$gateTime]"
        }
        event.visual_state.contains("REGIME_CONFIRMED", ignoreCase = true) -> "REGIME CONFIRMED"
        else -> "PLANNED"
    }
    
    val statusBadgeColor = when {
        statusBadgeText.startsWith("LOCKED") -> Color(0xFFE11D48)
        statusBadgeText.startsWith("SOFT") -> Color(0xFF06B6D4)
        statusBadgeText == "REGIME CONFIRMED" -> Color(0xFF6366F1)
        else -> Color(0xFFE11D48)
    }

    Surface(
        color = PureBlack.copy(alpha = cardOpacity),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (statusBadgeText.startsWith("LOCKED")) Color(0xFFE11D48).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Asset Class + Status Badge | EBC Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        event.asset_class.uppercase(),
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    Badge(text = statusBadgeText, color = statusBadgeColor, opacity = cardOpacity)
                    
                    if (statusBadgeText.startsWith("SOFT") || statusBadgeText.startsWith("LOCKED")) {
                        Box(modifier = Modifier.width(40.dp).height(2.dp).background(statusBadgeColor.copy(alpha = 0.3f))) {
                            Box(modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight().background(statusBadgeColor))
                        }
                    }
                }
                
                val ebcBlocked = event.ebc_status.contains("BLOCKED", ignoreCase = true)
                val ebcColor = if (ebcBlocked) Color(0xFFE11D48) else Color(0xFF10B981)
                Badge(text = "EBC: ${event.ebc_status.uppercase()}", color = ebcColor, opacity = cardOpacity)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Title and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    event.title,
                    color = Color.White.copy(alpha = cardOpacity),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        formatTime(event.timestamp_utc),
                        color = Color.White.copy(alpha = 0.8f * cardOpacity),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vitals Box
            VitalsBox(event, cardOpacity)

            Spacer(modifier = Modifier.height(14.dp))

            // Row 4: Narrative
            if (!event.narrative_summary.isNullOrBlank()) {
                Text(
                    event.narrative_summary,
                    color = Color.Gray.copy(alpha = cardOpacity),
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily,
                    fontStyle = FontStyle.Normal,
                    lineHeight = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
            Spacer(modifier = Modifier.height(10.dp))
            
            // Intelligence Monitor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INTELLIGENCE MONITOR [OPEN]",
                    color = Color.DarkGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = InterFontFamily
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color, opacity: Float = 1.0f) {
    Surface(
        color = color.copy(alpha = opacity),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text,
                color = Color.White.copy(alpha = opacity),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun VitalsBox(event: EconomicEvent, opacity: Float) {
    val strategyBias = inferStrategyBias(event.execution_regime)
    val posture = inferPosture(event.unlock_state, event.ebc_status)
    
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VitalsColumn("STRATEGY BIAS", strategyBias.first, strategyBias.second, modifier = Modifier.weight(1f), opacity = opacity)
            VitalsDivider()
            VitalsColumn("POSTURE", posture.first, posture.second, modifier = Modifier.weight(1f), opacity = opacity)
            VitalsDivider()
            VitalsColumn("MARKET VITALS", "", Color.Transparent, isVitals = true, liquidity = event.liquidity_depth, correlation = event.correlation_heat, modifier = Modifier.weight(1.2f), opacity = opacity)
            VitalsDivider()
            VitalsColumn("", event.assets_affected.firstOrNull() ?: "", Color.DarkGray, modifier = Modifier.weight(0.7f), opacity = opacity, isAsset = true)
        }
    }
}

@Composable
private fun VitalsColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    isVitals: Boolean = false,
    isAsset: Boolean = false,
    liquidity: Int = 0,
    correlation: Int = 0,
    opacity: Float = 1f
) {
    Column(modifier = modifier, horizontalAlignment = if (isAsset) Alignment.End else Alignment.Start) {
        if (label.isNotEmpty()) {
            Text(
                label,
                color = Color.Gray.copy(alpha = 0.6f * opacity),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.height(1.dp))
        }
        if (isVitals) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
                // Liquidity (Green)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { i ->
                        val active = i < (liquidity / 20).coerceIn(1, 5)
                        Box(
                            modifier = Modifier
                                .size(6.dp, 4.dp)
                                .background(
                                    if (active) Color(0xFF22C55E).copy(alpha = opacity) 
                                    else Color.DarkGray.copy(alpha = 0.2f * opacity),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
                // Correlation (Cyan)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { i ->
                        val active = i < (correlation / 20).coerceIn(1, 5)
                        Box(
                            modifier = Modifier
                                .size(6.dp, 4.dp)
                                .background(
                                    if (active) Color(0xFF00D9FF).copy(alpha = opacity) 
                                    else Color.DarkGray.copy(alpha = 0.2f * opacity),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        } else {
            Text(
                value,
                color = color.copy(alpha = opacity),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun VitalsDivider() {
    Box(modifier = Modifier.padding(horizontal = 4.dp).width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.05f)))
}

private fun inferStrategyBias(regime: String): Pair<String, Color> {
    return when {
        regime.contains("HAWKISH", ignoreCase = true) || regime.contains("SHORT", ignoreCase = true) -> "- SHORT" to Color(0xFFE11D48)
        regime.contains("DOVISH", ignoreCase = true) || regime.contains("LONG", ignoreCase = true) -> "- LONG" to Color(0xFF00D9FF)
        else -> "- NEUTRAL" to Color.Gray
    }
}

private fun inferPosture(unlockState: String, ebcStatus: String): Pair<String, Color> {
    return when {
        unlockState.contains("LOCKED", ignoreCase = true) || ebcStatus.contains("BLOCKED", ignoreCase = true) -> "HALTED" to Color(0xFFE11D48)
        unlockState.contains("SOFT", ignoreCase = true) -> "DEFENSIVE" to Color(0xFF00D9FF)
        ebcStatus.contains("PERMITTED", ignoreCase = true) -> "AGGRESSIVE" to Color(0xFF10B981)
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
