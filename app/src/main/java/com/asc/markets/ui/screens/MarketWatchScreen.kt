package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.AppView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.PreMoveCandidate
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.BinanceDataStore
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import java.util.Locale


@Composable
fun MarketWatchScreen() {
    val vm: ForexViewModel = viewModel()
    val aiDeployments by vm.aiDeployments.collectAsState()
    val aiDecisions = aiDeployments?.final_decision.orEmpty()
    
    // Convert AI decisions to PreMoveCandidates using backend data + live prices
    val candidates = aiDecisions.mapNotNull { decision ->
        val symbol = decision.asset_1 ?: return@mapNotNull null
        // Use only MarketDataStore and BinanceDataStore (no fallback sources)
        val livePair = MarketDataStore.pairSnapshot(symbol)
            ?: BinanceDataStore.pairSnapshot(symbol)
        
        // Debug logging
        android.util.Log.d("MarketWatch", "Processing symbol: $symbol, livePair found: ${livePair != null}, price: ${livePair?.price}, change: ${livePair?.changePercent}")
        
        decision.toPreMoveCandidate(livePair)
    }.filter { it.preMoveScore > 0 || it.ignitionScore > 0 } // Only show if backend has data
    
    // Debug logging for final candidates
    android.util.Log.d("MarketWatch", "Total AI decisions: ${aiDecisions.size}, Final candidates: ${candidates.size}")
    
    // Debug: Log all available pairs in MarketDataStore and BinanceDataStore
    val marketPairs by MarketDataStore.allPairs.collectAsState()
    val binancePairs by BinanceDataStore.allPairs.collectAsState()
    LaunchedEffect(marketPairs.size, binancePairs.size) {
        android.util.Log.d("MarketWatch", "MarketDataStore has ${marketPairs.size} pairs: ${marketPairs.map { "${it.symbol}=${it.price}" }.joinToString(", ")}")
        android.util.Log.d("MarketWatch", "BinanceDataStore has ${binancePairs.size} pairs: ${binancePairs.map { "${it.symbol}=${it.price}" }.joinToString(", ")}")
    }
    
    val samples = candidates
    
    val scrollState = rememberScrollState()
    
    // Track scroll direction and collapse header accordingly
    LaunchedEffect(scrollState.value) {
        val scrollPosition = scrollState.value
        
        // Hide header when scrolling down (scroll position increases)
        if (scrollPosition > 100) {
            vm.setGlobalHeaderCollapse(1f)
        } else {
            // Show header when at top or scrolling up
            val collapseProgress = (scrollPosition / 100f).coerceIn(0f, 1f)
            vm.setGlobalHeaderCollapse(collapseProgress)
        }
    }
    
    // Reset header visibility when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            vm.setGlobalHeaderCollapse(0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(scrollState)
            .padding(top = 16.dp)
    ) {
        // Removed: Top banner with icon, title, and filter
        // Removed: Engine card with scan status
        
        // Sample signals list — directly show the cards
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp), 
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            samples.forEach { candidate ->
                MarketWatchSignalCard(candidate)
            }
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun MarketWatchSignalCard(s: PreMoveCandidate) {
    val vm: ForexViewModel = viewModel()
    
    // Use backend reason directly
    val aiExplanation = s.deterministicReason
    
    InfoBox(minHeight = 180.dp, modifier = Modifier.fillMaxWidth().clickable {
        // Select the symbol in the ViewModel without changing view, then navigate to Analysis Node
        vm.selectPairBySymbolNoNavigate(s.symbol)
        vm.navigateTo(AppView.ANALYSIS_RESULTS)
    }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // small badge
                    PairFlags(symbol = s.symbol, size = 28)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(s.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                        Surface(color = stateColor(s), shape = RoundedCornerShape(6.dp)) {
                            Text(s.state.uppercase(Locale.getDefault()), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.preMoveScore}%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                    Text("PRE-MOVE SCORE", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
            }

            // Quote box with AI-generated explanation
            Surface(color = Color(0xFF080808), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(aiExplanation, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 14.sp, fontFamily = InterFontFamily)
            }

            // Metrics row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Momentum bar and label
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((s.compressionScore / 100f).coerceIn(0.05f, 1f)).background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("COMPRESSION", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                        Text("${s.compressionScore}%", color = Color.White, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IGNITION", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Text("${s.ignitionScore}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RISK GATE", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Text(s.riskGate, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Show price or loading indicator
                if (s.price > 0.0) {
                    Text(formatWatchPrice(s.price), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(String.format(Locale.US, "%s%.2f%%", if (s.changePercent >= 0.0) "+" else "", s.changePercent), color = if (s.changePercent >= 0.0) EmeraldSuccess else RoseError, fontSize = 12.sp, fontFamily = InterFontFamily)
                } else {
                    Text("Price loading...", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                }

                Spacer(modifier = Modifier.weight(1f))
                Text("OPEN NODE →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}

private fun stateColor(candidate: PreMoveCandidate): Color {
    return when (candidate.state) {
        "ARMED" -> EmeraldSuccess
        "WATCH" -> IndigoAccent
        "COMPRESSING" -> Color(0xFF6B4800)
        "LATE MOVE" -> RoseError
        else -> Color(0xFF3A3A3A)
    }
}

private fun formatWatchPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format(Locale.US, "%,.2f", price)
        price >= 1 -> String.format(Locale.US, "%.5f", price) // 5 decimals for forex (like MT5)
        else -> String.format(Locale.US, "%.6f", price)
    }
}

/**
 * Convert FinalDecisionItem (backend AI data) to PreMoveCandidate
 * Uses backend fields: pre_move_ai_score, ignition_probability, expansion_probability, feeder_risk_state
 * Merges with live pair data for price and changePercent
 */
private fun com.asc.markets.data.remote.FinalDecisionItem.toPreMoveCandidate(livePair: com.asc.markets.data.ForexPair?): PreMoveCandidate {
    val symbol = this.asset_1 ?: "UNKNOWN"
    val preMoveScore = ((this.pre_move_ai_score ?: 0.0) * 100).toInt().coerceIn(0, 100)
    val ignitionScore = ((this.ignition_probability ?: 0.0) * 100).toInt().coerceIn(0, 100)
    // Compression is inverse of expansion probability
    val compressionScore = ((1.0 - (this.expansion_probability ?: 0.0)) * 100).toInt().coerceIn(0, 100)
    val riskGate = when (this.feeder_risk_state) {
        "RISK_ON", "RISK_CLEAR" -> "PASS"
        "RISK_OFF", "CAPITAL_PRESERVATION" -> "BLOCKED"
        "RISK_ELEVATED" -> "WATCH"
        else -> "NO DATA"
    }
    val state = when {
        preMoveScore >= 78 && ignitionScore >= 60 -> "ARMED"
        preMoveScore >= 62 -> "WATCH"
        compressionScore >= 70 -> "COMPRESSING"
        else -> "FILTERING"
    }
    
    return PreMoveCandidate(
        symbol = symbol,
        name = symbol,
        category = livePair?.category ?: com.asc.markets.data.MarketCategory.FOREX,
        price = livePair?.price ?: 0.0,
        changePercent = livePair?.changePercent ?: 0.0,
        timeframe = this.source_timeframe ?: "H1",
        state = state,
        directionBias = this.journal_direction ?: "NEUTRAL",
        preMoveScore = preMoveScore,
        compressionScore = compressionScore,
        ignitionScore = ignitionScore,
        structuralPressure = ((this.structural_pressure_score ?: 0.0) * 100).toInt(),
        liquidityScore = 0, // Not available from backend
        sweepProbability = ((this.expansion_probability ?: 0.0) * 100).toInt(),
        regime = this.regime_state ?: "Unknown",
        liquidityMagnet = "Unknown",
        expectedWindow = this.entry_window ?: "Unknown",
        riskGate = riskGate,
        invalidationLevel = null,
        buySideLiquidity = 0.0,
        sellSideLiquidity = 0.0,
        liquidityPools = emptyList(),
        layers = emptyList(),
        deterministicReason = this.portfolio_decision_reason ?: "No reason provided",
        triggerConditions = emptyList(),
        correlationGate = this.correlation_regime ?: "NEUTRAL",
        correlations = emptyList(),
        trapRisk = this.correlation_warning ?: "LOW"
    )
}
