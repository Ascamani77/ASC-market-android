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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.*
import com.asc.markets.risk.DiagnosticsReport
import com.asc.markets.risk.RiskDiagnosticsEngine
import com.asc.markets.risk.SurfaceStats
import com.asc.markets.risk.TradeResult
import com.asc.markets.ui.components.DiagnosticsPanel
import com.asc.markets.ui.theme.InterFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsReportScreen(viewModel: ForexViewModel = viewModel()) {
    val engine = remember { RiskDiagnosticsEngine() }
    val repo = viewModel.tradeHistoryRepository

    var report by remember { mutableStateOf<com.asc.markets.risk.DiagnosticsReport?>(null) }

    LaunchedEffect(repo) {
        if (repo != null) {
            // surfaceStats / volatility / correlation should be provided by the system; placeholder for now
            val surfaceStats = SurfaceStats(winRate = 0.55, volatility = 0.015, tail05 = -0.02, regimeFrequency = 0.3)
            val realizedVolatility = 0.02
            val rollingCorrelation = 0.4

            val r = engine.generateReportFromRepository(repo, surfaceStats, realizedVolatility, rollingCorrelation)
            report = r
        } else {
            // no repo available — show empty report
            report = engine.generateReport(emptyList(), SurfaceStats(0.0, 0.0, 0.0, 0.0), 0.0, 0.0)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        report?.let {
            DiagnosticsPanel(report = it, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Diagnostics are observational only. No sizing or signals are changed.", color = androidx.compose.ui.graphics.Color.LightGray)
    }
}

@Composable
private fun KpiBoxComposable(label: String, value: String, valueColor: Color, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        modifier = modifier.height(64.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = accent.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = InterFontFamily)
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
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
            Text(symbol, color = statusColor, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isFresh) "FRESH" else "STALE", color = statusColor, fontFamily = InterFontFamily, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(6.dp).background(statusColor, RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
fun TextRowLabel(text: String) {
    Text(text, color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp)
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
            KpiGridSection(connState, metrics, terminalGreen, criticalRose)
        }
        item {
            RealTimeIngestionCard(feeds, terminalGreen)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            SafetyInterlockSection(context, terminalGreen)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            TextRowLabel("INGESTION BUFFER")
        }
        item {
            IngestionBufferList(feeds, terminalGreen)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            TextRowLabel("CONNECTIVITY MANAGER LOGS")
        }
        item {
            ConnectivityLogsTerminal(logs, terminalGreen)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            EnvironmentProfilerCard(metrics, terminalGreen)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            FooterAlertPanel()
        }
    }
}

@Composable
fun KpiGridSection(connState: ConnectionState, metrics: HealthMetrics, terminalGreen: Color, criticalRose: Color) {
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

@Composable
fun RealTimeIngestionCard(feedsList: List<FeedStatus>, terminalGreen: Color) {
    val now = System.currentTimeMillis()
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("REAL-TIME INGESTION", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("BUFFER", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("POLLING EVERY", color = terminalGreen.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 9.sp)
                Text("250MS", color = terminalGreen.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                if (feedsList.size >= 4) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..1).forEach { idx ->
                            val feed = feedsList[idx]
                            val isStale = (now - feed.lastTickAt) > 2000
                            val color = if (isStale) Color(0xFFFFA500) else terminalGreen
                            FeedTile(feed.symbol, color, modifier = Modifier.weight(1f), isFresh = !isStale)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (2..3).forEach { idx ->
                            val feed = feedsList[idx]
                            val isStale = (now - feed.lastTickAt) > 2000
                            val color = if (isStale) Color(0xFFFFA500) else terminalGreen
                            FeedTile(feed.symbol, color, modifier = Modifier.weight(1f), isFresh = !isStale)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyInterlockSection(context: android.content.Context, terminalGreen: Color) {
    var interlock by remember { mutableStateOf(false) }
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("🔒", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Text("SAFETY_INTERLOCK_LAYER", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Divider(color = terminalGreen.copy(alpha = 0.12f), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("INTEL INTERLOCK", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("DISABLE DISPATCHES IF RTT > 50MS", color = terminalGreen, fontFamily = InterFontFamily, fontSize = 10.sp)
                }
                Switch(checked = interlock, onCheckedChange = {
                    interlock = it
                    ConnectivityManager.toggleExecutionInterlock(it)
                })
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (interlock) {
                Surface(
                    color = terminalGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("INTERLOCK_ENGAGED", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
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
                Text("↻ FORCE RECONNECT", color = terminalGreen, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IngestionBufferList(feeds: List<FeedStatus>, terminalGreen: Color) {
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
                    Text(feed.symbol, color = terminalGreen, fontFamily = InterFontFamily, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(if (fresh) "FRESH" else "STALE", color = if (fresh) terminalGreen else Color(0xFFFFA500), fontFamily = InterFontFamily, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ConnectivityLogsTerminal(logs: List<String>, terminalGreen: Color) {
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
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EnvironmentProfilerCard(metrics: HealthMetrics, terminalGreen: Color) {
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, terminalGreen.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🔧 ENVIRONMENT_PROFILER", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = terminalGreen.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            ProfilerRow("VISIBILITY STATUS", "⊙ ${metrics.visibilityStatus}", terminalGreen)
            ProfilerRow("SYSTEM THROTTLING", "NONE", terminalGreen)
            ProfilerRow("TIMER ACCURACY", "NOMINAL", terminalGreen)
            Spacer(modifier = Modifier.height(12.dp))
            ProfilerRow("JS HEAP UTILIZATION", "${metrics.memoryUsageMb} / 21 MB", terminalGreen)
            Spacer(modifier = Modifier.height(8.dp))
            val heapPercent = (metrics.memoryUsageMb.toFloat() / 21f).coerceIn(0f, 1f)
            LinearProgressIndicator(progress = heapPercent, color = terminalGreen, trackColor = Color(0xFF111111), modifier = Modifier.fillMaxWidth().height(4.dp))
        }
    }
}

@Composable
fun ProfilerRow(label: String, value: String, terminalGreen: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = terminalGreen.copy(alpha = 0.8f), fontFamily = InterFontFamily, fontSize = 11.sp)
        Text(value, color = terminalGreen, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun FooterAlertPanel() {
    Surface(
        color = Color(0xFF2A1B00),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA500))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⚠", color = Color(0xFFFFA500), fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "HIGH TIMER DRIFT DETECTED DURING BACKGROUND\nPERIODS. INSTITUTIONAL DISPATCHES MAY BE DELAYED\nIF SURVEILLANCE MODE IS MINIMIZED ON VPS.",
                color = Color(0xFFFFA500),
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
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
            Text(label, color = accent.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = InterFontFamily)
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}
