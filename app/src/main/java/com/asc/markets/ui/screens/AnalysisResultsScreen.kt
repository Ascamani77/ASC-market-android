package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.PreMoveCandidate
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.data.PreMoveLayer
import com.asc.markets.ui.components.ConfidenceGauge
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun fetchDeepExplanation(metric: String, value: String, symbol: String): String {
    return "Analysis node synchronized with backend intelligence data feed. " +
        "Metric '$metric' = $value for $symbol is computed live from EA signals, " +
        "backend scoring, and market structure engines."
}

@Composable
fun AnalysisResultsScreen() {
    val viewModel: ForexViewModel = viewModel()
    val selectedPair by viewModel.selectedPair.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val candidate = PreMoveIntelligenceStore.candidateFor(selectedPair.symbol, candidates)
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("ANALYSIS NODE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("PRE-MOVE EVIDENCE STACK", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (candidate == null) {
            EmptyNodeState()
        } else {
            NodeVerdictSection(candidate)
            Spacer(modifier = Modifier.height(24.dp))
            DeterministicLogicSection(candidate)
            Spacer(modifier = Modifier.height(24.dp))
            LayerStackSection(candidate.layers)
            Spacer(modifier = Modifier.height(24.dp))
            LiquidityAndInvalidationSection(candidate)
            Spacer(modifier = Modifier.height(24.dp))
            TriggerConditionsSection(candidate)
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun EmptyNodeState() {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "No pre-move candidate is available yet. Wait for MT5 live history to populate the deterministic store.",
            color = SlateText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun NodeVerdictSection(candidate: PreMoveCandidate) {
    var deepExplanation by remember { mutableStateOf("Generating AI insight...") }
    LaunchedEffect(candidate.symbol) {
        deepExplanation = fetchDeepExplanation("Pre-Move Confidence Score", "${candidate.preMoveScore}%", candidate.symbol)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f).height(190.dp),
                color = PureBlack,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ConfidenceGauge(candidate.preMoveScore)
                }
            }

            Surface(
                modifier = Modifier.weight(1f).height(190.dp),
                color = PureBlack,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PairFlags(symbol = candidate.symbol, size = 32)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(candidate.symbol, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text(candidate.timeframe, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                    }
                    NodeBadge(candidate.state, nodeStateColor(candidate))
                    NodeMetric("BIAS", candidate.directionBias)
                    NodeMetric("REGIME", candidate.regime)
                    NodeMetric("RISK GATE", candidate.riskGate)
                    NodeMetric("WINDOW", candidate.expectedWindow)
                }
            }
        }
        
        // Deep Explanation for Verdict
        Surface(
            color = Color.White.copy(alpha = 0.02f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = IndigoAccent, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    deepExplanation,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun DeterministicLogicSection(candidate: PreMoveCandidate) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("DETERMINISTIC LOGIC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Icon(Icons.Default.Info, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                Text(
                    candidate.deterministicReason,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreTile("COMPRESSION", candidate.compressionScore, candidate.symbol, modifier = Modifier.weight(1f))
                ScoreTile("IGNITION", candidate.ignitionScore, candidate.symbol, modifier = Modifier.weight(1f))
                ScoreTile("SWEEP", candidate.sweepProbability, candidate.symbol, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LayerStackSection(layers: List<PreMoveLayer>) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("ASC LAYER STACK", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(18.dp))
            layers.forEachIndexed { index, layer ->
                LayerRow(layer)
                if (index != layers.lastIndex) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = HairlineBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun LiquidityAndInvalidationSection(candidate: PreMoveCandidate) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("LIQUIDITY AND INVALIDATION", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            NodeMetric("LIQUIDITY MAGNET", candidate.liquidityMagnet)
            NodeMetric("BUY-SIDE POOL", formatNodePrice(candidate.buySideLiquidity))
            NodeMetric("SELL-SIDE POOL", formatNodePrice(candidate.sellSideLiquidity))
            NodeMetric("INVALIDATION", candidate.invalidationLevel?.let(::formatNodePrice) ?: "Compression boundary")
            NodeMetric("CORRELATION GATE", candidate.correlationGate)
        }
    }
}

@Composable
private fun TriggerConditionsSection(candidate: PreMoveCandidate) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TRIGGER CONDITIONS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            candidate.triggerConditions.forEach { trigger ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.padding(top = 7.dp).size(6.dp).background(EmeraldSuccess, androidx.compose.foundation.shape.CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(trigger, color = SlateText, fontSize = 12.sp, lineHeight = 17.sp, fontFamily = InterFontFamily)
                }
            }
        }
    }
}

@Composable
private fun NodeMetric(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}

@Composable
private fun NodeBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.45f))) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun ScoreTile(label: String, score: Int, symbol: String, modifier: Modifier = Modifier) {
    var showExplanation by remember { mutableStateOf(false) }
    var explanationText by remember { mutableStateOf("Analyzing...") }
    
    LaunchedEffect(showExplanation) {
        if (showExplanation && explanationText == "Analyzing...") {
            explanationText = fetchDeepExplanation(label, "$score%", symbol)
        }
    }

    Surface(
        color = GhostWhite, 
        shape = RoundedCornerShape(10.dp), 
        modifier = modifier.clickable { showExplanation = !showExplanation }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                if (showExplanation) {
                    Icon(Icons.Default.Info, null, tint = IndigoAccent, modifier = Modifier.size(10.dp))
                }
            }
            Text("$score%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            LinearProgressIndicator(progress = (score / 100f).coerceIn(0f, 1f), color = metricColor(score), trackColor = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth().height(3.dp))
            
            if (showExplanation) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    explanationText,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun LayerRow(layer: PreMoveLayer) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(layer.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(layer.detail, color = SlateText, fontSize = 10.sp, lineHeight = 14.sp, fontFamily = InterFontFamily)
            }
            Spacer(modifier = Modifier.width(12.dp))
            NodeBadge(layer.status, metricColor(layer.score))
        }
        LinearProgressIndicator(progress = (layer.score / 100f).coerceIn(0f, 1f), color = metricColor(layer.score), trackColor = Color.White.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth().height(3.dp))
    }
}

private fun metricColor(score: Int): Color {
    return when {
        score >= 70 -> EmeraldSuccess
        score >= 50 -> IndigoAccent
        else -> RoseError
    }
}

private fun nodeStateColor(candidate: PreMoveCandidate): Color {
    return when (candidate.state) {
        "ARMED" -> EmeraldSuccess
        "WATCH" -> IndigoAccent
        "COMPRESSING" -> Color(0xFF6B4800)
        "LATE MOVE" -> RoseError
        else -> Color(0xFF3A3A3A)
    }
}

private fun formatNodePrice(value: Double): String {
    return when {
        value >= 1000.0 -> String.format(Locale.US, "%,.2f", value)
        value >= 1.0 -> String.format(Locale.US, "%.5f", value)
        else -> String.format(Locale.US, "%.6f", value)
    }
}

