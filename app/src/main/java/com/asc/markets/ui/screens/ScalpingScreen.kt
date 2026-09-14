package com.asc.markets.ui.screens
import com.asc.markets.ui.components.AscRollingSpinner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.remote.ScalpingSignal
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.PureBlack
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScalpingScreen(viewModel: ForexViewModel = viewModel()) {
    var scalpingSignals by remember { mutableStateOf<List<ScalpingSignal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdateTime by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // AI DEPLOYMENTS REMOVED - NO LONGER USING AI BACKEND
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val allowedAssets = remember(aiDeployments) {
        aiDeployments?.final_decision?.mapNotNull { it.asset_1 }?.toSet() ?: emptySet()
    }

    // Use rememberCoroutineScope for manual refresh
    val scope = rememberCoroutineScope()

    // Fetch data function
    suspend fun fetchScalpingSignals() {
        try {
            val result = viewModel.aiRepository.getScalpingSignals()
            result.onSuccess { response ->
                if (response.success) {
                    // Filter to only show assets that are in the main AI deployment
                    val filtered = if (allowedAssets.isNotEmpty()) {
                        response.signals.filter { signal -> 
                            allowedAssets.contains(signal.asset)
                        }
                    } else {
                        response.signals
                    }
                    
                    // Only show assets with actual signals (not NONE or NO_SIGNAL)
                    // AND sort by confidence (best signals at the top)
                    scalpingSignals = filtered
                        .filter { signal ->
                            val direction = signal.signal?.uppercase() ?: "NONE"
                            direction !in setOf("NONE", "NO_SIGNAL", "WAIT", "NO_TRADE")
                        }
                        .sortedByDescending { signal ->
                            // Sort by confidence (highest first)
                            signal.confidence?.toDoubleOrNull() ?: 0.0
                        }
                    
                    errorMessage = null
                    lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                } else {
                    errorMessage = response.error ?: response.message ?: "Failed to load scalping signals"
                }
                isLoading = false
            }.onFailure { e ->
                errorMessage = e.message ?: "Network error: Unable to connect to backend"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Network error: Unable to connect to backend"
            isLoading = false
        }
    }

    // Auto-refresh every 5 seconds using LaunchedEffect with proper lifecycle
    LaunchedEffect(refreshTrigger) {
        while (true) {
            fetchScalpingSignals()
            delay(5000) // Refresh every 5 seconds
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Scalping Signals",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "M5/M15 • Fast Execution • 5s Updates",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            if (lastUpdateTime.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF00E676), androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            color = Color(0xFF00E676),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = lastUpdateTime,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AscRollingSpinner(color = Color.White)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Error loading signals",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            scalpingSignals.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active scalping signals",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Waiting for setup...",
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = scalpingSignals,
                        key = { signal -> signal.asset + signal.signal + signal.timeframe }
                    ) { signal ->
                        ScalpingSignalCard(signal)
                    }
                }
            }
        }
    }
}

@Composable
fun ScalpingSignalCard(signal: ScalpingSignal) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val signalColor = when (signal.signal) {
        "LONG" -> Color(0xFF00C853)
        "SHORT" -> Color(0xFFFF1744)
        else -> Color.Gray
    }

    val confidenceColor = when (signal.confidence) {
        "VERY_HIGH" -> Color(0xFF00E676)
        "HIGH" -> Color(0xFF69F0AE)
        "MEDIUM" -> Color(0xFFFFD600)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ===== ALWAYS VISIBLE HEADER (Red marked area) =====
            
            // Header: Asset + Signal Direction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = signal.asset,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = signalColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (signal.signal == "LONG") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = signal.signal,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signal Validity (Compact version)
            SignalValidityIndicator(signal)

            Spacer(modifier = Modifier.height(14.dp))

            // Confidence + Timeframe row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = confidenceColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val confidencePercent = (signal.confidence?.toDoubleOrNull() ?: 0.0) * 100
                    Text(
                        text = "${confidencePercent.toInt()}%",
                        color = confidenceColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = signal.timeframe,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Strategy Display
            if (!signal.strategy.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Strategy",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = signal.strategy,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // ===== EXPANDABLE DETAILS =====
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))

                // AI Phase Indicator
                AIPhaseIndicator(signal)

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Momentum Indicators (for transparency)
                if (signal.immediate_momentum_pct != null || signal.short_momentum_pct != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Real-Time Momentum",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            signal.immediate_momentum_pct?.let { imm ->
                                MomentumPill(
                                    label = "Now",
                                    value = imm,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            signal.short_momentum_pct?.let { short ->
                                MomentumPill(
                                    label = "3min",
                                    value = short,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            signal.medium_momentum_pct?.let { med ->
                                MomentumPill(
                                    label = "10min",
                                    value = med,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Key Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        label = "Volatility",
                        value = signal.feeder_volatility_score?.let { "${(it * 100).toInt()}%" } ?: "N/A",
                        state = signal.feeder_volatility_state,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Momentum",
                        value = signal.feeder_indicator_score?.let { "${(it * 100).toInt()}%" } ?: "N/A",
                        state = signal.feeder_indicator_bias,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        label = "Liquidity",
                        value = signal.feeder_liquidity_score?.let { "${(it * 100).toInt()}%" } ?: "N/A",
                        state = signal.feeder_liquidity_bias,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Risk Level",
                        value = signal.feeder_risk_score?.let { "${(it * 100).toInt()}%" } ?: "N/A",
                        state = signal.feeder_risk_state,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Collapsed: Show expand hint
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to see details",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AIPhaseIndicator(signal: ScalpingSignal) {
    // Extract metrics from signal - use actual values, not defaults
    val confidence = signal.confidence?.toDoubleOrNull() ?: 0.5
    val volatility = signal.volatility ?: 0.0001
    val volatilityState = signal.feeder_volatility_state?.uppercase() ?: "NORMAL"
    val confluenceScore = signal.confluence_score ?: confidence
    val expansionProb = signal.expansion_probability ?: 0.0
    
    // DEBUG: Log values to understand what we're getting
    android.util.Log.d("ScalpingPhase", "Asset: ${signal.asset}")
    android.util.Log.d("ScalpingPhase", "  Confidence: $confidence")
    android.util.Log.d("ScalpingPhase", "  Volatility: $volatility")
    android.util.Log.d("ScalpingPhase", "  Volatility State: $volatilityState")
    android.util.Log.d("ScalpingPhase", "  Confluence: $confluenceScore")
    android.util.Log.d("ScalpingPhase", "  Expansion Prob: $expansionProb")
    
    // Calculate AI Progressive Scale using REALISTIC thresholds for scalping
    // Scalping is fast-moving, so we need lower thresholds than swing trading
    
    val (phaseLabel, phasePosition, phaseColor, phaseIcon) = when {
        // EXPANSION: Very high confidence + high volatility OR explosive state OR very high expansion
        (confidence >= 0.85 && volatility >= 0.0004) || 
        volatilityState in listOf("EXPLOSIVE", "BURST") ||
        expansionProb >= 0.7 -> 
            Tuple4("EXPANSION", 1.0f, Color(0xFF00FF88), "🚀")
        
        // PRE-MOVE: High confidence + expanding OR high confluence + good expansion
        (confidence >= 0.75 && volatilityState == "EXPANDING") ||
        (confluenceScore >= 0.75 && expansionProb >= 0.5) ||
        (confidence >= 0.8 && volatility >= 0.0002) ->
            Tuple4("PRE-MOVE", 0.75f, Color(0xFF00E676), "💥")
        
        // COMPRESSION: Medium confidence + compressed state OR building up
        (volatilityState == "COMPRESSED") ||
        (confidence >= 0.65 && volatility < 0.0002 && expansionProb < 0.4) ||
        (confluenceScore >= 0.65 && expansionProb < 0.4) ->
            Tuple4("COMPRESSION", 0.5f, Color(0xFFFF6B6B), "🔒")
        
        // STRUCTURE: Moderate confidence, some structure forming
        (confidence >= 0.55 && confluenceScore >= 0.6) ||
        (volatilityState == "NORMAL" && confidence >= 0.6) ->
            Tuple4("STRUCTURE", 0.25f, Color(0xFFFFD600), "🏗️")
        
        // NOISE: Low confidence, dead/choppy market
        else -> 
            Tuple4("NOISE", 0.1f, Color(0xFF666666), "📊")
    }
    
    android.util.Log.d("ScalpingPhase", "  -> PHASE: $phaseLabel")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = phaseIcon,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI Progressive Scale",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = phaseLabel,
                    color = phaseColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                // Show key metric that determined the phase
                Text(
                    text = when (phaseLabel) {
                        "EXPANSION" -> "Vol: ${(volatility * 10000).toInt()}bp"
                        "PRE-MOVE" -> "Conf: ${(confidence * 100).toInt()}%"
                        "COMPRESSION" -> "Exp: ${(expansionProb * 100).toInt()}%"
                        "STRUCTURE" -> "Conf: ${(confluenceScore * 100).toInt()}%"
                        else -> "Low Activity"
                    },
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progressive Scale with 5 stages (like Market Overview)
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background track with 5 gradient sections
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // NOISE zone
                Box(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight()
                        .background(
                            Color(0xFF666666).copy(alpha = 0.3f),
                            RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                        )
                )
                // STRUCTURE zone
                Box(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight()
                        .background(Color(0xFFFFD600).copy(alpha = 0.3f))
                )
                // COMPRESSION zone
                Box(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight()
                        .background(Color(0xFFFF6B6B).copy(alpha = 0.3f))
                )
                // PRE-MOVE zone
                Box(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight()
                        .background(Color(0xFF00E676).copy(alpha = 0.3f))
                )
                // EXPANSION zone
                Box(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight()
                        .background(
                            Color(0xFF00FF88).copy(alpha = 0.3f),
                            RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                )
            }
            
            // Current phase indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth(phasePosition)
                    .height(24.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(phaseColor, androidx.compose.foundation.shape.CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Phase labels (5 stages)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "NOISE",
                color = Color(0xFF666666),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "STRUCTURE",
                color = Color(0xFFFFD600),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "COMPRESSION",
                color = Color(0xFFFF6B6B),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "PRE-MOVE",
                color = Color(0xFF00E676),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "EXPANSION",
                color = Color(0xFF00FF88),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MomentumPill(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = value >= 0
    val color = if (isPositive) Color(0xFF00C853) else Color(0xFFFF1744)
    val displayValue = "${if (isPositive) "+" else ""}${String.format("%.2f", value)}%"
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayValue,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper data class for tuple return
private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun SignalValidityIndicator(signal: ScalpingSignal) {
    // Calculate signal age from timestamp
    val signalTimestamp = remember(signal.generated_at) {
        try {
            java.time.Instant.parse(signal.generated_at)
        } catch (e: Exception) {
            java.time.Instant.now()
        }
    }
    
    // Get current time WITHOUT remember so it updates on each recomposition
    val currentTime = java.time.Instant.now()
    val signalAgeSeconds = java.time.Duration.between(signalTimestamp, currentTime).seconds
    val signalAgeMinutes = signalAgeSeconds / 60
    
    // Determine signal stability based on age
    val (stabilityLabel, stabilityColor, stabilityIcon) = when {
        signalAgeMinutes >= 5 -> Triple("STABLE", Color(0xFF00E676), Icons.Default.CheckCircle)
        signalAgeMinutes >= 3 -> Triple("MATURING", Color(0xFF69F0AE), Icons.Default.Schedule)
        signalAgeMinutes >= 1 -> Triple("ACTIVE", Color(0xFFFFD600), Icons.Default.TrendingUp)
        else -> Triple("NEW", Color(0xFFFF9800), Icons.Default.FiberNew)
    }
    
    val confidence = signal.confidence?.toDoubleOrNull() ?: 0.0
    val volatility = signal.volatility ?: 0.0
    
    // Calculate validity strength combining age and confidence
    val validityStrength = when {
        signalAgeMinutes >= 3 && confidence >= 0.8 -> "VERY_STRONG"
        signalAgeMinutes >= 2 && confidence >= 0.7 -> "STRONG"
        signalAgeMinutes >= 1 && confidence >= 0.6 -> "MODERATE"
        confidence >= 0.5 -> "DEVELOPING"
        else -> "WEAK"
    }
    
    val validityColor = when (validityStrength) {
        "VERY_STRONG" -> Color(0xFF00E676)
        "STRONG" -> Color(0xFF69F0AE)
        "MODERATE" -> Color(0xFFFFD600)
        "DEVELOPING" -> Color(0xFFFF9800)
        else -> Color(0xFFFF6B6B)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(validityColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                stabilityIcon,
                contentDescription = null,
                tint = stabilityColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = stabilityLabel,
                    color = stabilityColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${signalAgeMinutes}m ${signalAgeSeconds % 60}s old",
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = validityStrength.replace("_", " "),
                color = validityColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Signal Strength",
                color = Color.Gray,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    state: String?,
    modifier: Modifier = Modifier
) {
    val stateColor = when (state?.uppercase()) {
        "BULLISH", "EXPANDING", "BURST", "EXPLOSIVE", "HIGH", "RISK_ON" -> Color(0xFF00C853)
        "BEARISH", "COMPRESSED", "DEAD", "LOW", "RISK_OFF" -> Color(0xFFFF1744)
        else -> Color.Gray
    }

    Surface(
        color = Color(0xFF2A2A2A),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (!state.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.take(12),
                    color = stateColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
