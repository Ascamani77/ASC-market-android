package com.asc.markets.ui.screens

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
fun SwingTradingScreen(viewModel: ForexViewModel = viewModel()) {
    var swingSignals by remember { mutableStateOf<List<ScalpingSignal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdateTime by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Get AI deployments to filter swing signals
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val allowedAssets = remember(aiDeployments) {
        aiDeployments?.final_decision?.mapNotNull { it.asset_1 }?.toSet() ?: emptySet()
    }

    // Use rememberCoroutineScope for manual refresh
    val scope = rememberCoroutineScope()

    // Fetch data function
    suspend fun fetchSwingSignals() {
        try {
            val result = viewModel.aiRepository.getSwingSignals()
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
                    swingSignals = filtered.filter { signal ->
                        val direction = signal.signal?.uppercase() ?: "NONE"
                        direction !in setOf("NONE", "NO_SIGNAL", "WAIT", "NO_TRADE")
                    }
                    
                    errorMessage = null
                    lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                } else {
                    errorMessage = response.error ?: response.message ?: "Failed to load swing trading signals"
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

    // Auto-refresh every 30 seconds (swing trading doesn't need 5s updates)
    LaunchedEffect(refreshTrigger) {
        while (true) {
            fetchSwingSignals()
            delay(30000) // Refresh every 30 seconds
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
                    text = "Swing Trading",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "H1/H4 • Position Holds • 30s Updates",
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
                                .background(Color(0xFF64B5F6), androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            color = Color(0xFF64B5F6),
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
                    CircularProgressIndicator(color = Color.White)
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
            swingSignals.isEmpty() -> {
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
                            text = "No active swing trading signals",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Waiting for H1/H4 setup...",
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
                        items = swingSignals,
                        key = { signal -> signal.asset + signal.signal + signal.timeframe }
                    ) { signal ->
                        ScalpingSignalCard(signal)  // Reuse the same card component
                    }
                }
            }
        }
    }
}
