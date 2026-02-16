package com.asc.markets.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.*
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.asc.markets.ui.theme.TerminalTypography
import androidx.compose.ui.text.font.FontFamily

@Composable
fun MacroStreamView(events: List<MacroEvent> = sampleMacroEvents(), viewModel: com.asc.markets.logic.ForexViewModel? = null) {
    var showCatalystsPanel by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // UI state
        // Header: full-width bar (no rounded box) with compact horizontal layout
        Box(modifier = Modifier.fillMaxWidth().background(PureBlack)) {
            Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                // Left grouping: title + pill + refresh, and lead-time row below
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MACRO STREAM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MACRO-WEIGHTED", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp), fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00C853), shape = CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LEAD TIME ENGINE ACTIVE", color = RoseError, style = TerminalTypography.labelSmall.copy(fontSize = 9.sp, fontFamily = InterFontFamily))
                    }
                }

                // Vertical divider
                Box(modifier = Modifier.width(1.dp).height(44.dp).background(Color(0xFF121212)))

                // Right grouping: terminal sync + clock
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(start = 12.dp)) {
                    Text("TERMINAL SYNC", color = SlateText, style = TerminalTypography.labelSmall.copy(fontFamily = InterFontFamily))
                    val now = Instant.now()
                    Text(DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.systemDefault()).format(now), color = Color.White, style = TerminalTypography.bodyLarge.copy(fontFamily = InterFontFamily), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Circular clock button to open catalysts panel
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2B2B2B),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { showCatalystsPanel = !showCatalystsPanel }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Refresh, contentDescription = "Catalysts", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        
        // Catalysts panel slide-in overlay
        if (showCatalystsPanel) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { showCatalystsPanel = false }) {
                MacroCatalystsPanel(modifier = Modifier.align(Alignment.CenterEnd))
            }
        }

        

        // UI Controls removed: category pills and look-ahead slider were intentionally removed per design

        // Main area — removed external padding so items are full-bleed
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.9f).fillMaxHeight()) {
                        val listState = rememberLazyListState()

                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    val allEvents = events  // Show all events (both UPCOMING and CONFIRMED)
                    items(allEvents) { ev ->
                        val isUpcoming = ev.status == MacroEventStatus.UPCOMING
                        val visualAlpha = if (isUpcoming) 1f else 0.32f
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            // Full-width black event box with all content stacked vertically
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = PureBlack,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(modifier = Modifier.alpha(visualAlpha)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    // Top row: priority + currency + title
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        // Priority label (no background)
                                        Text(ev.priority.name, color = when (ev.priority) { ImpactPriority.CRITICAL -> RoseError; ImpactPriority.HIGH -> Color(0xFFBBBBBB); else -> IndigoAccent }, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp), fontFamily = InterFontFamily)
                                        // Identity: currency code (no background)
                                        Text(ev.currency, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 10.dp), fontFamily = InterFontFamily)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Use the consistent display title helper (Full name followed by abbreviation in brackets)
                                        Text(
                                            ev.displayTitle(),
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 24.sp,
                                            fontFamily = InterFontFamily
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // Brief explanation / details
                                    Text(ev.details, color = Color.White.copy(alpha = 0.8f), style = TerminalTypography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily), modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // Source with world icon
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Public, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(ev.source, color = SlateText, style = TerminalTypography.labelSmall.copy(fontSize = 10.sp, fontFamily = InterFontFamily))
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Bottom row: time + timezone (left) + status with blinking dot (right)
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            val fmtBig = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.systemDefault())
                                            Text(fmtBig.format(Instant.ofEpochMilli(ev.datetimeUtc)), color = Color.White.copy(alpha = 0.9f), style = TerminalTypography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("UTC WINDOW", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                        }
                                        
                                        Spacer(modifier = Modifier.weight(1f))
                                        
                                        // Status text with blinking dot (only for upcoming)
                                        if (isUpcoming) {
                                            val infiniteTransition = rememberInfiniteTransition(label = "blinking")
                                            val blinkAlpha by infiniteTransition.animateFloat(
                                                initialValue = 1f,
                                                targetValue = 0.3f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "blinkAlpha"
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FF00).copy(alpha = blinkAlpha), shape = CircleShape))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("UPCOMING", color = Color(0xFF0F6F52), style = TerminalTypography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily))
                                            }
                                        } else {
                                            Text("CONFIRMED", color = Color(0xFF7A7A7A), style = TerminalTypography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily))
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
private fun MacroCatalystsPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(DeepBlack)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Header with tabs
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("SCHEDULED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF00C853), shape = CircleShape))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Macro Catalysts section
        Text("MACRO CATALYSTS", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Event item 1
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = PureBlack,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.padding(end = 6.dp),
                            color = RoseError,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("CRITICAL", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontFamily = InterFontFamily)
                        }
                    }
                    Text("US NON-FA...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("FORECASTING 180K. CONSENSU...", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = InterFontFamily, maxLines = 1)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BLS EMPLOYMENT", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("14:30", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UTC WINDOW", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF00C853), shape = CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UPCOMING", color = Color(0xFF00C853), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Event item 2
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = PureBlack,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.padding(end = 6.dp),
                            color = RoseError,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("CRITICAL", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontFamily = InterFontFamily)
                        }
                    }
                    Text("ECB RATE ...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("POLICY HOLD EXPECTED. FOCU...", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = InterFontFamily, maxLines = 1)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CENTRAL BANK", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

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
