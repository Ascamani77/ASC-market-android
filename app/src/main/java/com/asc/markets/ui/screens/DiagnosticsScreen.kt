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
import com.asc.markets.data.BinanceDataStore
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.MicroJitterSnapshot
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.risk.DiagnosticsReport
import com.asc.markets.risk.RiskDiagnosticsEngine
import com.asc.markets.risk.SurfaceStats
import com.asc.markets.risk.TradeResult
import com.asc.markets.ui.components.DiagnosticsPanel
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.RoseError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

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
    val viewModel: ForexViewModel = viewModel()
    val selectedPair by viewModel.selectedPair.collectAsState()
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
    val binanceTimedHistory by BinanceDataStore.timedPriceHistory.collectAsState()
    val fallbackTimedHistory by com.asc.markets.data.CombinedFallbackDataStore.timedPriceHistory.collectAsState()
    val timedHistory = remember(marketTimedHistory, binanceTimedHistory, fallbackTimedHistory) {
        marketTimedHistory + binanceTimedHistory + fallbackTimedHistory
    }
    val snapshot = PreMoveIntelligenceStore.buildMicroJitterSnapshot(selectedPair, timedHistory)
    
    // Find AI decision for selected pair from aiDeployments
    val selectedDecision = remember(selectedPair, aiDeployments) {
        aiDeployments?.final_decision?.firstOrNull { 
            it.asset_1?.equals(selectedPair.symbol, ignoreCase = true) == true 
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { MicroJitterHeader(snapshot) }
        item { MicroJitterStateCard(snapshot) }
        item { MicroJitterSignalGrid(snapshot) }
        item { MicroJitterFeedValidityCard(snapshot) }
        
        // Add transition section
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { AiIntegrationTransition(snapshot, selectedDecision) }
        
        // Add AI Decision Diagnostics section with better integration
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { AiDecisionDiagnosticsHeader() }
        item { AiDecisionStatusCard(selectedDecision) }
        item { AiFeederGatesCard(selectedDecision) }
        item { AiRejectionReasonsCard(selectedDecision) }
    }
}

@Composable
private fun MicroJitterHeader(snapshot: MicroJitterSnapshot) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Micro-Jitter Monitor", color = Color.White, fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("${snapshot.symbol} pre-ignition tick instability detector", color = Color(0xFF00FF41), fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun MicroJitterStateCard(snapshot: MicroJitterSnapshot) {
    val accent = microJitterColor(snapshot.state)
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(snapshot.state, color = accent, fontFamily = InterFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(snapshot.detail, color = Color.White.copy(alpha = 0.72f), fontFamily = InterFontFamily, fontSize = 11.sp, lineHeight = 15.sp)
                }
                Text("${snapshot.preIgnitionScore}%", color = Color.White, fontFamily = InterFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            JitterProgressRow("Pre-Ignition Score", snapshot.preIgnitionScore, accent)
        }
    }
}

@Composable
private fun MicroJitterSignalGrid(snapshot: MicroJitterSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MicroJitterTile("Tick Burst", String.format(Locale.US, "%.2fx", snapshot.burstMultiplier), "baseline ${String.format(Locale.US, "%.2f", snapshot.baselineTicksPerSecond)}/s", Modifier.weight(1f))
            MicroJitterTile("Ticks/Sec", String.format(Locale.US, "%.2f", snapshot.ticksPerSecond), "${snapshot.averageIntervalMs}ms avg interval", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MicroJitterTile("Spread Jitter", "${snapshot.spreadJitter}%", "quote instability proxy", Modifier.weight(1f))
            MicroJitterTile("Micro Vol", "${snapshot.microVolatility}%", "short-window expansion", Modifier.weight(1f))
        }
        MicroJitterPressureCard(snapshot)
    }
}

@Composable
private fun MicroJitterPressureCard(snapshot: MicroJitterSnapshot) {
    val accent = if (snapshot.directionalPressure >= 55) EmeraldSuccess else if (snapshot.directionalPressure <= 45) RoseError else Color(0xFF00FF41)
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Directional Micro Pressure", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${snapshot.directionalPressure}%", color = accent, fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            JitterProgressRow("Bid/Ask Pressure Estimate", snapshot.directionalPressure, accent)
        }
    }
}

@Composable
private fun MicroJitterFeedValidityCard(snapshot: MicroJitterSnapshot) {
    val live = snapshot.feedStatus == "MT5 LIVE"
    val accent = if (live) Color(0xFF00FF41) else Color(0xFFFFA500)
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Feed Validity", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            ProfilerRow("MT5 Tick State", snapshot.feedStatus, accent)
            ProfilerRow("Last Tick Age", if (snapshot.lastTickAgeMs < 0L) "NO TICKS" else "${snapshot.lastTickAgeMs}ms", accent)
            ProfilerRow("Bridge Validity", snapshot.bridgeLatencyLabel, accent)
        }
    }
}

@Composable
private fun MicroJitterTile(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF41).copy(alpha = 0.14f)),
        modifier = modifier.height(92.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF00FF41), fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontFamily = InterFontFamily, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(caption, color = Color(0xFF00FF41).copy(alpha = 0.65f), fontFamily = InterFontFamily, fontSize = 9.sp)
        }
    }
}

@Composable
private fun JitterProgressRow(label: String, score: Int, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = accent.copy(alpha = 0.8f), fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$score%", color = accent, fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((score / 100f).coerceIn(0f, 1f)).background(accent, RoundedCornerShape(4.dp)))
        }
    }
}

private fun microJitterColor(state: String): Color {
    return when (state) {
        "IGNITION" -> EmeraldSuccess
        "UNSTABLE" -> RoseError
        "BUILDING" -> Color(0xFFFFA500)
        "FEED STALE", "INSUFFICIENT TICKS" -> Color(0xFFFFA500)
        else -> Color(0xFF00FF41)
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

// AI Decision Diagnostics Components
@Composable
private fun AiIntegrationTransition(snapshot: MicroJitterSnapshot, decision: com.asc.markets.data.remote.FinalDecisionItem?) {
    val jitterState = snapshot.state
    val aiState = decision?.final_trade_state?.uppercase(Locale.US) ?: "NO_DATA"
    val alignment = when {
        jitterState == "IGNITION" && aiState == "TRADE_CANDIDATE" -> "ALIGNED"
        jitterState in listOf("IGNITION", "UNSTABLE") && aiState != "TRADE_CANDIDATE" -> "DIVERGENT"
        jitterState == "CALM" && aiState == "TRADE_CANDIDATE" -> "AI_LEADING"
        else -> "MONITORING"
    }
    
    val alignmentColor = when(alignment) {
        "ALIGNED" -> EmeraldSuccess
        "DIVERGENT" -> RoseError
        "AI_LEADING" -> Color(0xFFFFA500)
        else -> Color(0xFF00FF41)
    }
    
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, alignmentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔗", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Micro-Jitter ↔ AI Alignment", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            Divider(color = alignmentColor.copy(alpha = 0.2f), thickness = 1.dp)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jitter State", color = Color.White.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 9.sp)
                    Text(jitterState, color = microJitterColor(jitterState), fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Alignment", color = Color.White.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 9.sp)
                    Text(alignment, color = alignmentColor, fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("AI State", color = Color.White.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 9.sp)
                    Text(aiState, color = alignmentColor, fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Surface(
                color = alignmentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when(alignment) {
                        "ALIGNED" -> "Micro-jitter ignition confirms AI trade candidate. Strong pre-move signal."
                        "DIVERGENT" -> "Jitter shows instability but AI hasn't confirmed trade. Wait for alignment."
                        "AI_LEADING" -> "AI sees opportunity but jitter is calm. AI may be early or jitter lagging."
                        else -> "Both systems monitoring. No actionable signal yet."
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = InterFontFamily,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun AiDecisionDiagnosticsHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("AI Decision Diagnostics", color = Color.White, fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("Final Trading AI • 40 Feeder Pipeline Status", color = Color(0xFF00FF41), fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun AiDecisionStatusCard(decision: com.asc.markets.data.remote.FinalDecisionItem?) {
    val finalState = decision?.final_trade_state?.uppercase(Locale.US) ?: "NO_DATA"
    val finalDirection = decision?.final_trade_direction?.uppercase(Locale.US) ?: "NONE"
    val finalScore = decision?.final_trade_score?.let { (it * 100f).toFloat() } ?: 0f
    val finalLabel = decision?.final_trade_label?.uppercase(Locale.US) ?: "NO_TRADE"
    val finalConfidence = decision?.final_trade_confidence?.uppercase(Locale.US) ?: "LOW"
    
    val stateColor = when (finalState) {
        "TRADE_CANDIDATE" -> when (finalDirection) {
            "LONG" -> EmeraldSuccess
            "SHORT" -> RoseError
            else -> Color(0xFF00FF41)
        }
        "MANUAL_REVIEW" -> Color(0xFFFFAA00)
        "REJECTED" -> Color(0xFF666666)
        else -> Color(0xFF444444)
    }
    
    val stateIcon = when (finalState) {
        "TRADE_CANDIDATE" -> "✓"
        "MANUAL_REVIEW" -> "⚠"
        "REJECTED" -> "✗"
        else -> "?"
    }
    
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stateIcon, color = stateColor, fontFamily = InterFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(finalState, color = stateColor, fontFamily = InterFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(finalLabel, color = Color.White.copy(alpha = 0.72f), fontFamily = InterFontFamily, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${finalScore.toInt()}%", color = Color.White, fontFamily = InterFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(finalConfidence, color = stateColor, fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            if (finalDirection != "NONE") {
                Divider(color = stateColor.copy(alpha = 0.2f), thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Direction", color = Color.White.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(finalDirection, color = stateColor, fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun AiFeederGatesCard(decision: com.asc.markets.data.remote.FinalDecisionItem?) {
    val gates = listOf(
        FeederGate("ENTRY", decision?.entry_state ?: "NO_ENTRY", decision?.entry_state == "READY"),
        FeederGate("CONFLUENCE", decision?.confluence_state ?: "NO_CONFLUENCE", decision?.confluence_state == "TRADEABLE_SETUP"),
        FeederGate("PLAN", decision?.plan_state ?: "NO_PLAN", decision?.plan_state == "PLAN_READY"),
        FeederGate("EXECUTION", decision?.execution_status ?: "BLOCKED", decision?.execution_status == "READY"),
        FeederGate("SIGNAL QUALITY", decision?.signal_quality_state ?: "NO_SIGNAL_QUALITY", decision?.signal_quality_state in listOf("STRONG_SIGNAL", "ELITE_SIGNAL")),
        FeederGate("RISK", decision?.feeder_risk_state ?: "RISK_OFF", decision?.feeder_risk_state != "RISK_OFF")
    )
    
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF41).copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Feeder Gate Status", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Divider(color = Color(0xFF00FF41).copy(alpha = 0.2f), thickness = 1.dp)
            
            gates.forEach { gate ->
                FeederGateRow(gate)
            }
        }
    }
}

@Composable
private fun AiRejectionReasonsCard(decision: com.asc.markets.data.remote.FinalDecisionItem?) {
    val reason = decision?.final_trade_reason ?: "NO_DECISION_DATA"
    val reasons = reason.split(" | ").filter { it.isNotBlank() }
    
    Surface(
        color = Color(0xFF000000),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF41).copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Decision Reasoning", color = Color.White, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Divider(color = Color(0xFF00FF41).copy(alpha = 0.2f), thickness = 1.dp)
            
            if (reasons.isEmpty()) {
                Text("No decision data available", color = Color.White.copy(alpha = 0.5f), fontFamily = InterFontFamily, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            } else {
                reasons.forEach { reasonItem ->
                    RejectionReasonRow(reasonItem)
                }
            }
        }
    }
}

@Composable
private fun FeederGateRow(gate: FeederGate) {
    val gateColor = if (gate.passed) EmeraldSuccess else RoseError
    val gateIcon = if (gate.passed) "✓" else "✗"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(gateIcon, color = gateColor, fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(gate.name, color = Color.White.copy(alpha = 0.8f), fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(gate.state, color = gateColor, fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RejectionReasonRow(reason: String) {
    Surface(
        color = Color(0xFF00FF41).copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF41).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▸", color = Color(0xFF00FF41), fontFamily = InterFontFamily, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                reason,
                color = Color.White.copy(alpha = 0.9f),
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

private data class FeederGate(
    val name: String,
    val state: String,
    val passed: Boolean
)
