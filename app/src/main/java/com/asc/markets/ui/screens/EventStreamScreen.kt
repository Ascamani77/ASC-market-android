package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.*
import com.asc.markets.logic.EventStreamUiState
import com.asc.markets.logic.EventStreamViewModel
import com.asc.markets.ui.components.EventDetailsModal
import com.asc.markets.ui.theme.*
import com.trading.app.data.CalendarSnapshotStore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventStreamScreen(
    viewModel: EventStreamViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var selectedEvent by remember { mutableStateOf<IntelligenceEvent?>(null) }
    var isCalendarLoading by remember { mutableStateOf(false) }
    
    // Tab state
    var selectedTab by remember { mutableStateOf("All") }
    val tabs = listOf("All", "Active", "Locked", "Monitor")
    
    // Pull to refresh state
    val pullRefreshState = rememberPullToRefreshState()
    
    // Header collapse logic
    var previousIndex by remember { mutableIntStateOf(listState.firstVisibleItemIndex) }
    var previousScrollOffset by remember { mutableIntStateOf(listState.firstVisibleItemScrollOffset) }
    
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        
        if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset)) {
            // Scrolling down (content goes up)
            if (currentIndex > 0 || currentOffset > 100) {
                viewModel.setGlobalHeaderCollapse(1f)
            }
        } else {
            // Scrolling up (content goes down)
            if (currentIndex == 0 && currentOffset < 50) {
                viewModel.setGlobalHeaderCollapse(0f)
            }
        }
        previousIndex = currentIndex
        previousScrollOffset = currentOffset
    }
    
    // Reset collapse when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setGlobalHeaderCollapse(0f)
        }
    }
    
    // Sync pullRefreshState with uiState
    LaunchedEffect(uiState) {
        if (uiState !is EventStreamUiState.Loading && pullRefreshState.isRefreshing) {
            // We need to wait for the next frame to ensure the animation completes
            delay(500)
            // Note: In some versions of M3, you can't manually set isRefreshing. 
            // It depends on the internal state. But we can trigger a re-composition.
        }
    }

    // Create Mt5Service to fetch calendar data
    val mt5Service = remember {
        com.trading.app.data.Mt5Service(
            pcIpAddress = com.asc.markets.data.NetworkConfig.mt5Host(context),
            port = com.asc.markets.data.NetworkConfig.mt5Port(context),
            onHistoryUpdate = { _, _ -> },
            onQuoteUpdate = { _ -> },
            onCalendarUpdate = { calendarPayload ->
                android.util.Log.d("EventStreamScreen", "Calendar update received: ${calendarPayload.display.events.size} events")
                CalendarSnapshotStore.latestDisplayPayload = calendarPayload.display
                CalendarSnapshotStore.latestAiPayload = calendarPayload.ai
                isCalendarLoading = false
                viewModel.refresh()
            },
            onConnectionStatusUpdate = { connected ->
                android.util.Log.d("EventStreamScreen", "Mt5Service connected: $connected")
                if (connected) {
                    isCalendarLoading = true
                }
            }
        )
    }

    DisposableEffect(mt5Service) {
        android.util.Log.d("EventStreamScreen", "Connecting to Mt5Service...")
        mt5Service.connect()
        onDispose {
            android.util.Log.d("EventStreamScreen", "Disconnecting Mt5Service")
            mt5Service.disconnect()
        }
    }

    LaunchedEffect(Unit) {
        delay(2000)
        val today = java.time.LocalDate.now().toString()
        android.util.Log.d("EventStreamScreen", "Requesting calendar for: $today")
        isCalendarLoading = true
        mt5Service.requestCalendar(today)
    }

    // Handle pull to refresh trigger
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            val today = java.time.LocalDate.now().toString()
            mt5Service.requestCalendar(today)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF000000) // DeepBlack
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Transparent Navbar / TabRow
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                        color = IndigoAccent
                    )
                }
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = InterFontFamily
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                val filteredEvents = when (val state = uiState) {
                    is EventStreamUiState.Success -> {
                        when (selectedTab) {
                            "Active" -> state.events.filter { it.unlock_state == IntelligenceUnlockState.HARD_UNLOCK }
                            "Locked" -> state.events.filter { it.unlock_state == IntelligenceUnlockState.LOCKED }
                            "Monitor" -> state.events.filter { it.unlock_state == IntelligenceUnlockState.SOFT_UNLOCK }
                            else -> state.events
                        }
                    }
                    else -> emptyList()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Show content if we have events, even during loading (for seamless refresh)
                    if (filteredEvents.isNotEmpty()) {
                        EventList(
                            events = filteredEvents,
                            listState = listState,
                            onEventClick = { selectedEvent = it }
                        )
                    } else {
                        // Only show specific states if list is empty
                        // Wrap in verticalScroll so pull-to-refresh works on empty list
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            when (val state = uiState) {
                                is EventStreamUiState.Loading -> {
                                    CircularProgressIndicator(color = IndigoAccent)
                                }
                                is EventStreamUiState.Error -> {
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
                                            lineHeight = 18.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                is EventStreamUiState.Success -> {
                                    if (state.events.isEmpty()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Text(
                                                "No Events Available",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = InterFontFamily
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "No calendar events found for today.\nCheck back later for updates.",
                                                color = SlateText,
                                                fontSize = 13.sp,
                                                fontFamily = InterFontFamily,
                                                lineHeight = 18.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PullToRefreshContainer removed to prevent visual disturbance
                }
            }
        }
    }

    selectedEvent?.let { event ->
        EventDetailsModal(
            event = event,
            onClose = { selectedEvent = null }
        )
    }
}

@Composable
fun EventList(
    events: List<IntelligenceEvent>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onEventClick: (IntelligenceEvent) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { event ->
            EventStreamCard(
                event = event,
                onMoreInfoClick = { onEventClick(event) }
            )
        }
    }
}

@Composable
fun EventStreamCard(
    event: IntelligenceEvent,
    onMoreInfoClick: () -> Unit
) {
    val themeColor = when (event.asset_class) {
        AssetClass.macro -> Color(0xFFE53935) // Red
        AssetClass.forex -> Color(0xFF4CAF50) // Green
        AssetClass.stock -> Color(0xFFFFB300) // Yellow
        AssetClass.commodity -> Color(0xFF2196F3) // Blue
        else -> Color(0xFF9E9E9E) // Gray
    }

    val iconText = when (event.asset_class) {
        AssetClass.macro -> "🌐 MACRO"
        AssetClass.forex -> "💲 FOREX"
        AssetClass.stock -> "📊 STOCK"
        AssetClass.commodity -> "💧 COMMODITY"
        else -> "🔹 ${event.asset_class.name.uppercase()}"
    }

    val (statusLabel, statusColor, isLocked) = when (event.unlock_state) {
        IntelligenceUnlockState.LOCKED -> Triple("LOCKED", Color(0xFFE53935), true)
        IntelligenceUnlockState.SOFT_UNLOCK -> Triple("MONITOR", Color(0xFFFFB300), false)
        IntelligenceUnlockState.HARD_UNLOCK -> Triple("ACTIVE", Color(0xFF4CAF50), false)
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeStr = remember(event.timestamp_utc) { timeFormat.format(Date(event.timestamp_utc)) }

    var timerText by remember { mutableStateOf("") }
    
    LaunchedEffect(event.timestamp_utc) {
        if (event.unlock_state == IntelligenceUnlockState.LOCKED || event.timestamp_utc > System.currentTimeMillis()) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = event.timestamp_utc - now
                if (diff <= 0) {
                    timerText = ""
                    break
                }
                val hours = (diff / 3600000).toInt()
                val mins = ((diff % 3600000) / 60000).toInt()
                val secs = ((diff % 60000) / 1000).toInt()
                if (hours > 0) {
                    timerText = String.format("%02d:%02d:%02d", hours, mins, secs)
                } else {
                    timerText = String.format("%02d:%02d", mins, secs)
                }
                delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Removed clip and rounded corners to make it touch edges perfectly
            .background(Color(0xFF0F0F11)) // Dark card background
            .border(1.dp, Color(0xFF1E1E24))
    ) {
        // Left Colored Border Strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(themeColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = iconText,
                        color = themeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            val displayLabel = if (timerText.isNotEmpty() && isLocked) "LOCKED [$timerText]" else statusLabel
                            Text(
                                text = displayLabel,
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info, // Placeholder for clock
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeStr,
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title and Subtitle with Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.narrative_summary,
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onMoreInfoClick() }
                        .padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            Divider(color = Color(0xFF1E1E24), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(8.dp))

            // Middle Section: BIAS | POSTURE | CONFIDENCE
            val bias = event.strategy_context?.bias?.uppercase() ?: "NEUTRAL"
            val posture = event.strategy_context?.risk_posture?.uppercase() ?: "BALANCED"
            val confidence = event.confidence_score?.toInt() ?: 50
            
            val biasColor = when (bias) {
                "LONG" -> Color(0xFF4CAF50)
                "SHORT" -> Color(0xFFE53935)
                else -> Color(0xFFFFB300)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "BIAS", color = Color(0xFF757575), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = bias, color = biasColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF1E1E24)))
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "POSTURE", color = Color(0xFF757575), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = posture, color = Color(0xFFBDBDBD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF1E1E24)))
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1.5f)) {
                    Text(text = "CONFIDENCE", color = Color(0xFF757575), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$confidence%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        // Dashed Progress Bar
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            val activeSegments = (confidence / 10)
                            repeat(10) { i ->
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(3.dp)
                                        .background(if (i < activeSegments) biasColor else Color(0xFF2C2C35))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Section: Assets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    event.assets_affected.take(3).forEach { asset ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF2C2C35), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = asset,
                                color = Color(0xFF9E9E9E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
