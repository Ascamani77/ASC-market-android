package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.api.*
import com.asc.markets.logic.AscBackendViewModel
import com.asc.markets.logic.BackendConnectionState
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.formatTimestamp
import java.text.SimpleDateFormat
import java.util.*

/**
 * BackendDashboardScreen - Main UI for ASC Backend API Integration
 * 
 * This screen displays real-time AI signals from the ASC Backend API.
 * It shows market overview, individual signals, and trading opportunities.
 */

enum class BackendTab {
    OVERVIEW,
    SIGNALS,
    SCANNER,
    SETTINGS
}

@Composable
fun BackendDashboardScreen(
    viewModel: AscBackendViewModel = viewModel { AscBackendViewModel() }
) {
    var activeTab by remember { mutableStateOf(BackendTab.OVERVIEW) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setAutoRefresh(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setAutoRefresh(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Top Bar
        BackendTopBar(
            connectionState = uiState.connectionState,
            onRefreshClick = { viewModel.refreshAll() },
            onReconnectClick = { viewModel.connectToBackend() }
        )

        // Tab Navigation
        BackendTabRow(
            activeTab = activeTab,
            onTabSelected = { activeTab = it }
        )

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                BackendTab.OVERVIEW -> OverviewTab(uiState, viewModel)
                BackendTab.SIGNALS -> SignalsTab(uiState, viewModel)
                BackendTab.SCANNER -> ScannerTab(uiState, viewModel)
                BackendTab.SETTINGS -> SettingsTab(viewModel)
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LuminousBlue)
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun BackendTopBar(
    connectionState: BackendConnectionState,
    onRefreshClick: () -> Unit,
    onReconnectClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = Color.White.copy(alpha = 0.035f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title
            Column {
                Text(
                    text = "ASC Backend",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "AI Signal Dashboard",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily
                )
            }

            // Connection Status & Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status Indicator
                ConnectionStatusChip(connectionState)

                // Refresh Button
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LuminousBlue.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = LuminousBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Reconnect Button (shown only when disconnected)
                if (connectionState is BackendConnectionState.Disconnected ||
                    connectionState is BackendConnectionState.Error
                ) {
                    IconButton(
                        onClick = onReconnectClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RoseError.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = "Reconnect",
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusChip(connectionState: BackendConnectionState) {
    val (text, color) = when (connectionState) {
        is BackendConnectionState.Disconnected -> "Disconnected" to Color.Gray
        is BackendConnectionState.Connecting -> "Connecting..." to Color(0xFFF59E0B)
        is BackendConnectionState.Connected -> {
            "Connected (${connectionState.health.active_feeders} AI)" to GreenProfit
        }
        is BackendConnectionState.Error -> "Error" to RoseError
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
fun BackendTabRow(
    activeTab: BackendTab,
    onTabSelected: (BackendTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.035f)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(BackendTab.entries.toTypedArray()) { tab ->
                val active = activeTab == tab
                val label = when (tab) {
                    BackendTab.OVERVIEW -> "Overview"
                    BackendTab.SIGNALS -> "All Signals"
                    BackendTab.SCANNER -> "Scanner"
                    BackendTab.SETTINGS -> "Settings"
                }

                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (active) Color.White else Color(0xFF8E8E8E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(60.dp)
                            .background(if (active) Color.White else Color.Transparent)
                    )
                }
            }
        }
    }
}

// ============================================================================
// OVERVIEW TAB
// ============================================================================

@Composable
fun OverviewTab(uiState: com.asc.markets.logic.BackendUiState, viewModel: AscBackendViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Market Overview Card
        item {
            uiState.marketOverview?.let { overview ->
                MarketOverviewCard(overview)
            } ?: EmptyStateCard("No market data available")
        }

        // Top Opportunities
        item {
            Text(
                text = "Top Opportunities",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        }

        val topSignals = uiState.tradingOpportunities.take(5)
        if (topSignals.isNotEmpty()) {
            items(topSignals) { signal ->
                SignalCard(signal, onClick = {
                    // Navigate to detail or show dialog
                })
            }
        } else {
            item {
                EmptyStateCard("No high-probability setups found")
            }
        }
    }
}

@Composable
fun MarketOverviewCard(overview: MarketOverview) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Market Overview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total", overview.total_signals.toString(), Color.White)
                StatItem("Buy", overview.buy_signals.toString(), GreenProfit)
                StatItem("Sell", overview.sell_signals.toString(), RoseError)
                StatItem("High Conf", overview.high_confidence_count.toString(), LuminousBlue)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Market Condition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Market Condition",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (overview.market_condition) {
                        "Trending" -> GreenProfit.copy(alpha = 0.2f)
                        "Volatile" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = overview.market_condition,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Last Updated
            val formattedTime = remember(overview.last_updated) { 
                formatTimestamp(overview.last_updated) 
            }
            Text(
                text = "Updated: $formattedTime",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontFamily = InterFontFamily
        )
    }
}

// ============================================================================
// SIGNALS TAB
// ============================================================================

@Composable
fun SignalsTab(uiState: com.asc.markets.logic.BackendUiState, viewModel: AscBackendViewModel) {
    var filterDirection by remember { mutableStateOf<String?>(null) }
    var minConfidence by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filters
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Filters",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )

                // Direction Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "All",
                        selected = filterDirection == null,
                        onClick = {
                            filterDirection = null
                            viewModel.loadAllSignals()
                        }
                    )
                    FilterChip(
                        label = "BUY",
                        selected = filterDirection == "BUY",
                        onClick = {
                            filterDirection = "BUY"
                            viewModel.loadAllSignals(direction = "BUY")
                        }
                    )
                    FilterChip(
                        label = "SELL",
                        selected = filterDirection == "SELL",
                        onClick = {
                            filterDirection = "SELL"
                            viewModel.loadAllSignals(direction = "SELL")
                        }
                    )
                }

                // Confidence Filter
                Column {
                    Text(
                        text = "Min Confidence: ${minConfidence.toInt()}%",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily
                    )
                    Slider(
                        value = minConfidence,
                        onValueChange = { minConfidence = it },
                        valueRange = 0f..100f,
                        onValueChangeFinished = {
                            viewModel.loadAllSignals(
                                minConfidence = minConfidence.toDouble(),
                                direction = filterDirection
                            )
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = LuminousBlue,
                            activeTrackColor = LuminousBlue,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Signals List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val signals = uiState.allSignals
            if (signals.isNotEmpty()) {
                items(signals) { signal ->
                    SignalCard(signal, onClick = {
                        // Navigate to detail
                    })
                }
            } else {
                item {
                    EmptyStateCard("No signals match your filters")
                }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) LuminousBlue.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
        border = if (selected) BorderStroke(1.dp, LuminousBlue) else null
    ) {
        Text(
            text = label,
            color = if (selected) LuminousBlue else Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ============================================================================
// SCANNER TAB
// ============================================================================

@Composable
fun ScannerTab(uiState: com.asc.markets.logic.BackendUiState, viewModel: AscBackendViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "High-Probability Setups",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
            Text(
                text = "Min 70% confidence • EXPANSION or PRE-MOVE phase",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontFamily = InterFontFamily
            )
        }

        val opportunities = uiState.tradingOpportunities
        if (opportunities.isNotEmpty()) {
            items(opportunities) { signal ->
                SignalCard(signal, onClick = {
                    // Navigate to detail
                })
            }
        } else {
            item {
                EmptyStateCard("No high-probability setups found")
            }
        }
    }
}

// ============================================================================
// SIGNAL CARD (Reusable)
// ============================================================================

@Composable
fun SignalCard(signal: MarketSignal, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            1.dp,
            when (signal.direction) {
                "BUY" -> GreenProfit.copy(alpha = 0.3f)
                "SELL" -> RoseError.copy(alpha = 0.3f)
                else -> Color.White.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Asset & Signal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = signal.asset,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Direction Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (signal.direction) {
                            "BUY" -> GreenProfit
                            "SELL" -> RoseError
                            else -> Color.Gray
                        }
                    ) {
                        Text(
                            text = signal.direction,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Setup Type
                    Text(
                        text = signal.setup_type,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily
                    )
                }

                // Phase & Volatility
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = signal.phase,
                        color = LuminousBlue,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = signal.volatility_state,
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Right: Scores
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Score
                ScoreCircle(
                    score = signal.ai_score,
                    label = "AI",
                    color = LuminousBlue
                )

                // Confidence
                ScoreCircle(
                    score = signal.confidence,
                    label = "Conf",
                    color = when {
                        signal.confidence >= 80 -> GreenProfit
                        signal.confidence >= 60 -> Color(0xFFF59E0B)
                        else -> RoseError
                    }
                )
            }
        }
    }
}

@Composable
fun ScoreCircle(score: Double, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${score.toInt()}",
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontFamily = InterFontFamily
        )
    }
}

// ============================================================================
// SETTINGS TAB
// ============================================================================

@Composable
fun SettingsTab(viewModel: AscBackendViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Backend Settings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        }

        item {
            SettingCard(
                title = "Backend URL",
                description = "Currently: ${com.asc.markets.data.NetworkConfig.DEFAULT_BACKEND_URL}",
                icon = Icons.Default.Cloud
            )
        }

        item {
            SettingCard(
                title = "Auto-Refresh",
                description = "Updates every 5 seconds",
                icon = Icons.Default.Refresh
            )
        }

        item {
            SettingCard(
                title = "Connection Test",
                description = "Test backend connectivity",
                icon = Icons.Default.SignalCellularAlt,
                onClick = { viewModel.connectToBackend() }
            )
        }
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LuminousBlue,
                modifier = Modifier.size(32.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily
                )
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ============================================================================
// EMPTY STATE
// ============================================================================

@Composable
fun EmptyStateCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ============================================================================
// UTILITIES
// ============================================================================
// Moved to com.asc.markets.ui.components.SharedComponents
