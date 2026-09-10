package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.remote.LatestDeploymentsResponse
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import java.util.Locale

data class TimeframeAnalysis(
    val timeframe: String,
    val preMoveScore: Int,
    val compressionScore: Int,
    val ignitionScore: Int,
    val bias: String,
    val state: String,
    val regime: String
)

data class TimeframeAlignment(
    val overallBias: String,
    val status: String,
    val strength: Int,
    val description: String
)

@Composable
fun MultiTimeframeAnalysisScreen() {
    val viewModel: ForexViewModel = viewModel()
    val selectedPair by viewModel.selectedPair.collectAsState()
    
    // AI DEPLOYMENTS REMOVED - NO LONGER USING AI BACKEND
    val aiDeployments: com.asc.markets.data.remote.LatestDeploymentsResponse? = null
    
    // Get all available pairs (EA only)
    val marketPairs by MarketDataStore.allPairs.collectAsState()
    val allPairs = marketPairs.distinctBy { it.symbol }
    
    // Timeframes to analyze
    val timeframes = listOf("M15", "M30", "H1", "H4", "D1")
    
    // Build analysis for each timeframe
    val timeframeAnalyses = remember(selectedPair.symbol, aiDeployments) {
        buildTimeframeAnalyses(selectedPair, aiDeployments, timeframes)
    }
    
    // Calculate overall alignment
    val alignment = remember(timeframeAnalyses) {
        calculateAlignment(timeframeAnalyses)
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(selectedPair)
        }
        
        item {
            AlignmentCard(alignment)
        }
        
        items(timeframeAnalyses) { analysis ->
            TimeframeAnalysisCard(analysis)
        }
        
        item {
            AssetSelectorCard(allPairs, selectedPair, viewModel)
        }
    }
}

@Composable
private fun HeaderCard(selectedPair: ForexPair) {
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("MULTI-TIMEFRAME ANALYSIS", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
            }
            Text("Pre-move intelligence across multiple timeframes", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current Asset
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ANALYZING", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text(selectedPair.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMtfPrice(selectedPair.price), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text(
                        String.format(Locale.US, "%s%.2f%%", if (selectedPair.changePercent >= 0) "+" else "", selectedPair.changePercent),
                        color = if (selectedPair.changePercent >= 0) EmeraldSuccess else RoseError,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun AlignmentCard(alignment: TimeframeAlignment) {
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("TIMEFRAME ALIGNMENT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("OVERALL BIAS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (alignment.overallBias == "BULLISH") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = when(alignment.overallBias) {
                                "BULLISH" -> EmeraldSuccess
                                "BEARISH" -> RoseError
                                else -> Color.White
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            alignment.overallBias,
                            color = when(alignment.overallBias) {
                                "BULLISH" -> EmeraldSuccess
                                "BEARISH" -> RoseError
                                else -> Color.White
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                    }
                }
                
                Surface(
                    color = when(alignment.status) {
                        "ALIGNED" -> EmeraldSuccess.copy(alpha = 0.2f)
                        "MIXED" -> IndigoAccent.copy(alpha = 0.2f)
                        else -> RoseError.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, when(alignment.status) {
                        "ALIGNED" -> EmeraldSuccess
                        "MIXED" -> IndigoAccent
                        else -> RoseError
                    })
                ) {
                    Text(
                        alignment.status,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            // Alignment strength
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ALIGNMENT STRENGTH", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("${alignment.strength}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((alignment.strength / 100f).coerceIn(0f, 1f)).background(
                        when {
                            alignment.strength >= 70 -> EmeraldSuccess
                            alignment.strength >= 40 -> IndigoAccent
                            else -> RoseError
                        }, RoundedCornerShape(4.dp)))
                }
            }
            
            Text(alignment.description, color = SlateText, fontSize = 11.sp, lineHeight = 15.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun TimeframeAnalysisCard(analysis: TimeframeAnalysis) {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = GhostWhite,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(analysis.timeframe, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(analysis.timeframe, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text(analysis.regime, color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                    }
                }
                
                Surface(
                    color = stateColorForState(analysis.state).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, stateColorForState(analysis.state))
                ) {
                    Text(analysis.state, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            
            // Bias
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("BIAS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Text(
                    analysis.bias,
                    color = when(analysis.bias) {
                        "BULLISH" -> EmeraldSuccess
                        "BEARISH" -> RoseError
                        else -> Color.White
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
            }
            
            // Pre-Move Score
            ScoreRow("PRE-MOVE", analysis.preMoveScore, IndigoAccent)
            
            // Compression Score
            ScoreRow("COMPRESSION", analysis.compressionScore, Color(0xFFFFA500))
            
            // Ignition Score
            ScoreRow("IGNITION", analysis.ignitionScore, EmeraldSuccess)
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text("$score%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((score / 100f).coerceIn(0f, 1f)).background(color, RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun AssetSelectorCard(allPairs: List<ForexPair>, selectedPair: ForexPair, viewModel: ForexViewModel) {
    var showAllAssets by remember { mutableStateOf(false) }
    
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CHANGE ASSET", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
            Text("Select a different asset to analyze across timeframes", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val displayPairs = if (showAllAssets) allPairs else allPairs.take(10)
            
            displayPairs.forEach { pair ->
                AssetSelectorRow(pair, pair.symbol == selectedPair.symbol) {
                    viewModel.selectPairNoNavigate(pair)
                }
            }
            
            if (allPairs.size > 10) {
                Surface(
                    color = IndigoAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { showAllAssets = !showAllAssets }
                ) {
                    Box(
                        modifier = Modifier.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (showAllAssets) "Show less" else "Show ${allPairs.size - 10} more assets",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetSelectorRow(pair: ForexPair, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) IndigoAccent.copy(alpha = 0.2f) else PureBlack,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) IndigoAccent else HairlineBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(pair.symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text(
                String.format(Locale.US, "%s%.2f%%", if (pair.changePercent >= 0) "+" else "", pair.changePercent),
                color = if (pair.changePercent >= 0) EmeraldSuccess else RoseError,
                fontSize = 11.sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

private fun buildTimeframeAnalyses(
    pair: ForexPair,
    aiDeployments: LatestDeploymentsResponse?,
    timeframes: List<String>
): List<TimeframeAnalysis> {
    // Get AI decision for this asset
    val aiDecision = aiDeployments?.final_decision?.firstOrNull { 
        it.asset_1?.equals(pair.symbol, ignoreCase = true) == true 
    }
    
    // Base scores from AI or defaults
    val basePreMove = ((aiDecision?.pre_move_ai_score ?: 0.0) * 100).toInt().coerceIn(0, 100)
    val baseIgnition = ((aiDecision?.ignition_probability ?: 0.0) * 100).toInt().coerceIn(0, 100)
    val baseCompression = ((1.0 - (aiDecision?.expansion_probability ?: 0.0)) * 100).toInt().coerceIn(0, 100)
    val baseBias = aiDecision?.journal_direction ?: if (pair.changePercent >= 0) "BULLISH" else "BEARISH"
    
    // Simulate timeframe-specific variations
    return timeframes.map { tf ->
        // Use actual backend scores (NO SIMULATION - REAL DATA ONLY)
        val preMoveScore = basePreMove
        val compressionScore = baseCompression
        val ignitionScore = baseIgnition
        
        val state = when {
            preMoveScore >= 78 && ignitionScore >= 60 -> "ARMED"
            preMoveScore >= 62 -> "WATCH"
            compressionScore >= 70 -> "COMPRESSING"
            else -> "FILTERING"
        }
        
        val regime = when {
            compressionScore >= 75 && ignitionScore >= 55 -> "Expansion candidate"
            compressionScore >= 70 -> "Compression"
            ignitionScore >= 65 -> "Transition"
            else -> "Idle"
        }
        
        // Note: Backend currently provides H1 analysis for all timeframes
        // In production, backend should provide per-timeframe analysis
        val backendTimeframe = aiDecision?.source_timeframe ?: "H1"
        val displayRegime = if (tf == backendTimeframe) {
            "$regime (Backend)"
        } else {
            "$regime (H1 data)"
        }
        
        TimeframeAnalysis(
            timeframe = tf,
            preMoveScore = preMoveScore,
            compressionScore = compressionScore,
            ignitionScore = ignitionScore,
            bias = baseBias,
            state = state,
            regime = displayRegime
        )
    }
}

private fun calculateAlignment(analyses: List<TimeframeAnalysis>): TimeframeAlignment {
    val bullishCount = analyses.count { it.bias == "BULLISH" }
    val bearishCount = analyses.count { it.bias == "BEARISH" }
    val total = analyses.size
    
    val overallBias = when {
        bullishCount > bearishCount -> "BULLISH"
        bearishCount > bullishCount -> "BEARISH"
        else -> "NEUTRAL"
    }
    
    val alignmentPercentage = (maxOf(bullishCount, bearishCount).toFloat() / total * 100).toInt()
    
    val status = when {
        alignmentPercentage >= 80 -> "ALIGNED"
        alignmentPercentage >= 60 -> "MIXED"
        else -> "CONFLICTED"
    }
    
    val description = when (status) {
        "ALIGNED" -> "$alignmentPercentage% of timeframes agree on $overallBias bias. Strong multi-timeframe confirmation."
        "MIXED" -> "$alignmentPercentage% of timeframes show $overallBias bias. Some timeframe divergence detected."
        else -> "Timeframes are conflicted. Wait for clearer alignment before deployment."
    }
    
    return TimeframeAlignment(
        overallBias = overallBias,
        status = status,
        strength = alignmentPercentage,
        description = description
    )
}

private fun stateColorForState(state: String): Color {
    return when (state) {
        "ARMED" -> EmeraldSuccess
        "WATCH" -> IndigoAccent
        "COMPRESSING" -> Color(0xFF6B4800)
        "LATE MOVE" -> RoseError
        else -> Color(0xFF3A3A3A)
    }
}

private fun formatMtfPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format(Locale.US, "%,.2f", price)
        price >= 1 -> String.format(Locale.US, "%.5f", price)
        else -> String.format(Locale.US, "%.6f", price)
    }
}
