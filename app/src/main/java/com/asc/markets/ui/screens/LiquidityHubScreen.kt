package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.LiquidityPool
import com.asc.markets.data.PreMoveCandidate
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.screens.dashboard.OrderBookSplit
import com.asc.markets.ui.theme.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

data class NetDeltaData(val currency: String, val bias: String, val delta: Int, val confidence: Int)

@Composable
fun LiquidityHubScreen() {
    val viewModel: ForexViewModel = viewModel()
    val selectedPair by viewModel.selectedPair.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val selectedCandidate = PreMoveIntelligenceStore.candidateFor(selectedPair.symbol, candidates) ?: candidates.firstOrNull()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            InfoBox {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LIQUIDITY MAP", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("PRE-MOVE LIQUIDITY ATTRACTION AND SWEEP MODEL", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, letterSpacing = 0.5.sp)
                }
            }
        }
        if (selectedCandidate == null) {
            item { EmptyLiquidityState() }
        } else {
            item { LiquidityOverviewCard(selectedCandidate) }
            item { LiquidityPoolsCard(selectedCandidate) }
            item { SweepProbabilityCard(selectedCandidate) }
            item { CorrelationGateCard(selectedCandidate) }
        }
    }
}

@Composable
private fun EmptyLiquidityState() {
    InfoBox {
        Text(
            "No liquidity candidate is available yet. Wait for MT5 live history to populate buy-side and sell-side pools.",
            color = SlateText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LiquidityOverviewCard(candidate: PreMoveCandidate) {
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(candidate.symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text(candidate.deterministicReason, color = SlateText, fontSize = 11.sp, lineHeight = 15.sp, fontFamily = InterFontFamily)
                }
                LiquidityBadge(candidate.riskGate, riskGateColor(candidate.riskGate))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidityMetricTile("MAGNET", candidate.liquidityMagnet, modifier = Modifier.weight(1f))
                LiquidityMetricTile("TRAP RISK", candidate.trapRisk, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidityMetricTile("REGIME", candidate.regime, modifier = Modifier.weight(1f))
                LiquidityMetricTile("WINDOW", candidate.expectedWindow, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LiquidityPoolsCard(candidate: PreMoveCandidate) {
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ACTIVE LIQUIDITY POOLS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
            candidate.liquidityPools.forEach { pool ->
                LiquidityPoolRow(pool)
            }
        }
    }
}

@Composable
private fun SweepProbabilityCard(candidate: PreMoveCandidate) {
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("SWEEP PROBABILITY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
            LiquidityProgressRow("Buy-side draw", if (candidate.liquidityMagnet == "Buy-side liquidity") candidate.sweepProbability else 100 - candidate.sweepProbability)
            LiquidityProgressRow("Sell-side draw", if (candidate.liquidityMagnet == "Sell-side liquidity") candidate.sweepProbability else 100 - candidate.sweepProbability)
            LiquidityProgressRow("Compression near pool", candidate.compressionScore)
        }
    }
}

@Composable
private fun CorrelationGateCard(candidate: PreMoveCandidate) {
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CORRELATION GATE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                LiquidityBadge(candidate.correlationGate, riskGateColor(candidate.correlationGate))
            }
            if (candidate.correlations.isEmpty()) {
                Text("Waiting for enough cross-asset history to validate correlation pressure.", color = SlateText, fontSize = 11.sp, lineHeight = 15.sp, fontFamily = InterFontFamily)
            } else {
                candidate.correlations.forEach { signal ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(signal.symbol, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Text(signal.alignment, color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                        }
                        Text(String.format(Locale.US, "%.2f", signal.coefficient), color = if (signal.alignment == "CONFLICT") RoseError else EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidityPoolRow(pool: LiquidityPool) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(pool.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text("${pool.side} · ${String.format(Locale.US, "%.2f", pool.distancePercent)}% away", color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                }
                Text(formatLiquidityPrice(pool.level), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            LiquidityProgressRow("Magnet strength", pool.strength)
        }
    }
}

@Composable
private fun LiquidityProgressRow(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text("$score%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((score / 100f).coerceIn(0f, 1f)).background(riskGateColor(if (score >= 70) "PASS" else "WATCH"), RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun LiquidityMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = GhostWhite, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun LiquidityBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.45f))) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

private fun riskGateColor(label: String): Color {
    return when (label) {
        "PASS", "SUPPORT", "LOW" -> EmeraldSuccess
        "WATCH", "NEUTRAL", "MEDIUM" -> IndigoAccent
        else -> RoseError
    }
}

private fun formatLiquidityPrice(value: Double): String {
    return when {
        value >= 1000.0 -> String.format(Locale.US, "%,.2f", value)
        value >= 1.0 -> String.format(Locale.US, "%.5f", value)
        else -> String.format(Locale.US, "%.6f", value)
    }
}

@Composable
fun ExpandedNetDeltaRow(data: NetDeltaData) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: Currency, Bias Badge, Delta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(data.currency, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val (biasColor, biasTextColor) = when (data.bias) {
                        "LONG" -> EmeraldSuccess to Color.Black
                        "SHORT" -> RoseError to Color.Black
                        else -> Color.White.copy(alpha = 0.4f) to Color.White
                    }
                    
                    Surface(
                        color = biasColor,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .wrapContentSize()
                            .height(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(data.bias, color = biasTextColor, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                }

                // Delta amount
                Text(
                    text = "${if (data.delta >= 0) "+" else ""}${String.format("%,d", data.delta)}",
                    color = when (data.bias) {
                        "LONG" -> EmeraldSuccess
                        "SHORT" -> RoseError
                        else -> Color.White
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Institutional Weighting + Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INSTITUTIONAL WEIGHTING", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("${data.confidence}% CONFIDENCE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            val barColor = when (data.bias) {
                "LONG" -> EmeraldSuccess
                "SHORT" -> RoseError
                else -> Color.White.copy(alpha = 0.3f)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(data.confidence / 100f)
                        .background(barColor, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun NetDeltaRow(currency: String, delta: Int) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).background(GhostWhite, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text(currency.take(1), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(currency, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
                    Text("BIAS: ${if (delta >= 0) "LONG" else "SHORT"}", color = if (delta >= 0) EmeraldSuccess else RoseError, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "${if (delta >= 0) "+" else ""}${String.format("%,d", delta)}",
                color = if (delta >= 0) EmeraldSuccess else RoseError,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CorrelationMatrixTable() {
    // Correlation matrix data: assets and their correlations
    val assets = listOf("SYM", "EUR", "GBP", "USD", "AUD", "XAU", "BTC")
    val correlations = mapOf(
        "EUR" to listOf(1.00, 0.85, -0.82, 0.49, 0.26, 0.88, -0.73),
        "GBP" to listOf(0.24, 1.00, -0.19, -0.64, 0.91, -0.29, 0.22),
        "USD" to listOf(-0.14, -0.2, 1.00, 0.36, -0.21, 0.55, 0.62),
        "AUD" to listOf(-0.91, 0.42, -0.1, 1.00, -0.35, 0.76, -0.41),
        "XAU" to listOf(0.88, -0.29, -0.76, -0.18, 0.91, 1.00, 0.08),
        "BTC" to listOf(-0.73, 0.22, 0.62, 0.97, -0.8, 0.08, 1.00)
    )

    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            assets.forEachIndexed { index, asset ->
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(asset, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                if (index < assets.size - 1) Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Data rows
        correlations.forEach { (rowAsset, values) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Row header
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(rowAsset, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Data cells
                values.forEachIndexed { colIndex, value ->
                    val bgColor = when {
                        value >= 0.99 -> Color.White.copy(alpha = 0.3f) // Diagonal
                        value >= 0.75 -> IndigoAccent.copy(alpha = 0.4f) // High positive
                        value >= 0.4 -> IndigoAccent.copy(alpha = 0.2f) // Medium positive
                        value >= -0.4 -> Color.White.copy(alpha = 0.05f) // Neutral
                        value >= -0.75 -> RoseError.copy(alpha = 0.2f) // Medium negative
                        else -> RoseError.copy(alpha = 0.4f) // High negative
                    }

                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(40.dp)
                            .background(bgColor, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%.2f", value),
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                    }

                    if (colIndex < values.size - 1) Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CorrelationHeatmapCard(pair: String, coeff: Double) {
    val backgroundColor = when {
        coeff > 0.8 -> IndigoAccent.copy(alpha = 0.6f)
        coeff > 0.4 -> IndigoAccent.copy(alpha = 0.25f)
        coeff < -0.8 -> RoseError.copy(alpha = 0.6f)
        coeff < -0.4 -> RoseError.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.size(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(pair, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(
                text = String.format(java.util.Locale.US, "%.2f", coeff),
                color = Color.White,
                fontSize = 16.sp, 
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
        }
    }
}
