package com.asc.markets.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.asc.markets.logic.*

@Composable
private fun KpiBoxComposable(label: String, value: String, valueColor: Color, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        modifier = modifier.height(64.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = accent.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun FeedTile(symbol: String, statusColor: Color, modifier: Modifier = Modifier, isFresh: Boolean = true) {
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.12f)),
        modifier = modifier.height(80.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(symbol, color = statusColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isFresh) "FRESH" else "STALE", color = statusColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(6.dp).background(statusColor, RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
fun TextRowLabel(text: String) {
    Text(text, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun DiagnosticsScreen() {
    val metrics by HealthMonitor.metrics.collectAsState()
    val connState by ConnectivityManager.state.collectAsState()
    val logs by ConnectivityManager.logs.collectAsState()
    val feeds by FeedMonitor.feeds.collectAsState()

    val terminalGreen = Color(0xFF00FF41)
    val criticalRose = Color(0xFFEF476F)
    val bg = Color.Black

    // lifecycle visibility detection (approximate browser Visibility API)
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    HealthMonitor.updateProfiler(metrics.memoryUsageMb, metrics.clockDriftMs, metrics.timerAccuracyMs, "HIDDEN")
                    ConnectivityManager.logDiagnostic("VISIBILITY: HIDDEN - background throttling likely")
                }
                Lifecycle.Event.ON_START -> {
                    HealthMonitor.updateProfiler(metrics.memoryUsageMb, metrics.clockDriftMs, metrics.timerAccuracyMs, "VISIBLE")
                    ConnectivityManager.logDiagnostic("VISIBILITY: VISIBLE - resuming high-precision timers")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(bg).padding(12.dp)) {
        item {
            // Top KPI 2x2 grid (2 rows x 2 cols)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiBoxComposable("RELAY STATUS", connState.name, if (connState == ConnectionState.LIVE) terminalGreen else criticalRose, terminalGreen, modifier = Modifier.weight(1f))
                    KpiBoxComposable("RTT", "${(metrics.timerAccuracyMs.coerceAtLeast(1))}ms", terminalGreen, terminalGreen, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiBoxComposable("CLOCK DRIFT", "${metrics.clockDriftMs}ms", if (metrics.clockDriftMs > 100) criticalRose else terminalGreen, terminalGreen, modifier = Modifier.weight(1f))
                    KpiBoxComposable("INTEGRITY", metrics.integrityStatus, if (metrics.integrityStatus.contains("CORRUPT")) criticalRose else terminalGreen, terminalGreen, modifier = Modifier.weight(1f))
                }
            }

        }
        item {
            // Real-Time Ingestion Buffer Card
            val feedsList = FeedMonitor.feeds.collectAsState().value
            val now = System.currentTimeMillis()
            
            Surface(
                color = Color(0xFF000000),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("REAL-TIME INGESTION", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("BUFFER", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("POLLING EVERY", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        Text("250MS", color = terminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // 2x2 Grid of feed tiles (show first 4 feeds with dynamic freshness)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (feedsList.size >= 4) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val feed0 = feedsList[0]
                                val age0 = now - feed0.lastTickAt
                                val isStale0 = age0 > 2000  // > 2 seconds = stale
                                val color0 = if (isStale0) Color(0xFFFFA500) else terminalGreen
                                FeedTile(feed0.symbol, color0, modifier = Modifier.weight(1f), isFresh = !isStale0)
                                
                                val feed1 = feedsList[1]
                                val age1 = now - feed1.lastTickAt
                                val isStale1 = age1 > 2000
                                val color1 = if (isStale1) Color(0xFFFFA500) else terminalGreen
                                FeedTile(feed1.symbol, color1, modifier = Modifier.weight(1f), isFresh = !isStale1)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val feed2 = feedsList[2]
                                val age2 = now - feed2.lastTickAt
                                val isStale2 = age2 > 2000
                                val color2 = if (isStale2) Color(0xFFFFA500) else terminalGreen
                                FeedTile(feed2.symbol, color2, modifier = Modifier.weight(1f), isFresh = !isStale2)
                                
                                val feed3 = feedsList[3]
                                val age3 = now - feed3.lastTickAt
                                val isStale3 = age3 > 2000
                                val color3 = if (isStale3) Color(0xFFFFA500) else terminalGreen
                                FeedTile(feed3.symbol, color3, modifier = Modifier.weight(1f), isFresh = !isStale3)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            // Safety Interlock Layer Box
            var interlock by remember { mutableStateOf(false) }
            Surface(
                color = Color(0xFF000000),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with lock icon
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("🔒", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("SAFETY_INTERLOCK_LAYER", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(color = terminalGreen.copy(alpha = 0.12f), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
                    
                    // Intel Interlock Row
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("INTEL INTERLOCK", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("DISABLE DISPATCHES IF RTT > 50MS", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Switch(checked = interlock, onCheckedChange = {
                            interlock = it
                            ConnectivityManager.toggleExecutionInterlock(it)
                        })
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Interlock Engaged Status (shown when toggle is ON)
                    if (interlock) {
                        Surface(
                            color = terminalGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🛡️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                                Text("INTERLOCK_ENGAGED", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Force Reconnect Button (full width)
                    Button(
                        onClick = {
                            try {
                                val v = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                                if (v != null) {
                                    if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)) else v.vibrate(50)
                                }
                            } catch (_: Exception) {}
                            ConnectivityManager.forceReconnect()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = terminalGreen.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, terminalGreen)
                    ) {
                        Text("↻ FORCE RECONNECT", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            // Ingestion Buffer (freshness grid)
            TextRowLabel("INGESTION BUFFER")
        }
        item {
            Surface(
                color = Color(0xFF000000),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    feeds.forEach { feed ->
                        val age = System.currentTimeMillis() - feed.lastTickAt
                        val fresh = age <= 5000
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(if (fresh) terminalGreen else Color(0xFFFFA500), RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feed.symbol, color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(if (fresh) "FRESH" else "STALE", color = if (fresh) terminalGreen else Color(0xFFFFA500), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            // Connectivity Manager Logs (terminal)
            TextRowLabel("CONNECTIVITY MANAGER LOGS")
        }
        item {
            Surface(
                color = Color(0xFF000000),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().height(220.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(logs) { log ->
                        Text(
                            text = "> $log",
                            color = terminalGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            // Environment Profiler
            Surface(
                color = Color(0xFF000000),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header with divider
                    Text("🔧 ENVIRONMENT_PROFILER", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = terminalGreen.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Visibility Status row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("VISIBILITY STATUS", color = terminalGreen.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("⊙ ${metrics.visibilityStatus}", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // System Throttling row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SYSTEM THROTTLING", color = terminalGreen.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("NONE", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Timer Accuracy row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TIMER ACCURACY", color = terminalGreen.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("NOMINAL", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // JS Heap Utilization label and value
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("JS HEAP UTILIZATION", color = terminalGreen.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("${metrics.memoryUsageMb} / 21 MB", color = terminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar for heap utilization
                    val heapPercent = (metrics.memoryUsageMb.toFloat() / 21f).coerceIn(0f, 1f)
                    Surface(
                        color = Color(0xFF111111),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = terminalGreen,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(heapPercent)
                            ) {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            // Footer Alert Triage (amber)
            Surface(
                color = Color(0xFF2A1B00),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA500))
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠", color = Color(0xFFFFA500), fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "HIGH TIMER DRIFT DETECTED DURING BACKGROUND\nPERIODS. INSTITUTIONAL DISPATCHES MAY BE DELAYED\nIF SURVEILLANCE MODE IS MINIMIZED ON VPS.",
                        color = Color(0xFFFFA500),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun KpiBox(label: String, value: String, valueColor: Color, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        modifier = modifier.height(64.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = accent.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}