package com.asc.markets.ui.screens.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*
import java.util.Locale

@Composable
fun AiStatusTab(viewModel: ForexViewModel) {
    val aiDecisions by viewModel.aiDecisions.collectAsState()
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    
    val tradeReady = aiDecisions.count { it.final_trade_state == "TRADE_CANDIDATE" }
    val manualReview = aiDecisions.count { it.final_trade_state == "MANUAL_REVIEW" }
    val rejected = aiDecisions.count { it.final_trade_state == "REJECTED" }
    
    // Track last update time for debugging
    val lastUpdateTime = remember(aiDeployments) { 
        aiDeployments?.last_updated ?: "Never"
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AiStatusHeader(
                total = aiDecisions.size,
                tradeReady = tradeReady,
                manualReview = manualReview,
                rejected = rejected,
                lastUpdate = lastUpdateTime
            )
        }
        
        items(aiDecisions) { decision ->
            AiStatusAssetCard(decision)
        }
    }
}

@Composable
private fun AiStatusHeader(
    total: Int,
    tradeReady: Int,
    manualReview: Int,
    rejected: Int,
    lastUpdate: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "AI PIPELINE STATUS",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily
        )
        
        Text(
            "$total Assets Analyzed • $tradeReady Trade Ready",
            color = TextGray,
            fontSize = 12.sp,
            fontFamily = InterFontFamily
        )
        
        // Show last update time for debugging
        Text(
            "Last Update: $lastUpdate",
            color = TextGray.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontFamily = InterFontFamily
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip("✓ Ready", tradeReady, PreMoveGreen, Modifier.weight(1f))
            StatChip("⚠ Review", manualReview, Color(0xFFFFAA00), Modifier.weight(1f))
            StatChip("✗ Rejected", rejected, TextGray, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(),
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
            Text(
                label,
                color = color,
                fontSize = 10.sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun AiStatusAssetCard(decision: FinalDecisionItem) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val asset = decision.asset_1 ?: "UNKNOWN"
    val finalState = decision.final_trade_state?.uppercase(Locale.US) ?: "REJECTED"
    
    // For TRADE_CANDIDATE, use final_trade_score; for REJECTED, use pre_move_ai_score
    // Keep decimal precision for real-time granular updates
    val finalScore = if (finalState == "TRADE_CANDIDATE") {
        decision.final_trade_score?.let { (it * 100) } ?: 0.0
    } else {
        decision.pre_move_ai_score?.let { (it * 100) } ?: 0.0
    }
    
    val finalDirection = decision.final_trade_direction?.uppercase(Locale.US) ?: "NONE"
    val finalLabel = decision.final_trade_label?.uppercase(Locale.US) ?: "NO_TRADE"
    val reason = decision.final_trade_reason ?: ""
    val reasons = reason.split(" | ").filter { it.isNotBlank() }
    
    val stateColor = when (finalState) {
        "TRADE_CANDIDATE" -> when (finalDirection) {
            "LONG" -> PreMoveGreen
            "SHORT" -> NoiseRed
            else -> Color(0xFF00FF41)
        }
        "MANUAL_REVIEW" -> Color(0xFFFFAA00)
        else -> TextGray
    }
    
    val stateIcon = when (finalState) {
        "TRADE_CANDIDATE" -> "✓"
        "MANUAL_REVIEW" -> "⚠"
        else -> "✗"
    }
    
    val phase = when {
        finalScore >= 80 -> "EXPANSION"
        finalScore >= 60 -> "PRE-MOVE"
        finalScore >= 45 -> "COMPRESSION"
        finalScore >= 30 -> "STRUCTURE"
        else -> "NOISE"
    }
    
    // Extract gate statuses from decision
    val entryPassed = decision.entry_state == "READY"
    val confluencePassed = decision.confluence_state == "TRADEABLE_SETUP"
    val planPassed = reason.contains("PLAN_STATE=PLAN_READY")
    val executionPassed = decision.execution_status == "READY"
    val signalPassed = decision.signal_quality_state in listOf("STRONG_SIGNAL", "ELITE_SIGNAL")
    val riskPassed = decision.feeder_risk_state != "RISK_OFF" && decision.feeder_risk_state != "CAPITAL_PRESERVATION"
    
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header: Asset name and state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    asset,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stateIcon,
                        color = stateColor,
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        finalState,
                        color = stateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            }
            
            // Score and Phase row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Score: ${String.format("%.1f", finalScore)}%",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily
                )
                Text(
                    "Phase: $phase",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily
                )
            }
            
            // Divider
            HorizontalDivider(color = stateColor.copy(alpha = 0.2f), thickness = 1.dp)
            
            // Critical Gates Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "CRITICAL GATES:",
                    color = TextGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                
                // Display gates in 2x3 grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    GateIndicator("ENTRY", entryPassed, Modifier.weight(1f))
                    GateIndicator("CONFLUENCE", confluencePassed, Modifier.weight(1f))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    GateIndicator("PLAN", planPassed, Modifier.weight(1f))
                    GateIndicator("EXECUTION", executionPassed, Modifier.weight(1f))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    GateIndicator("SIGNAL", signalPassed, Modifier.weight(1f))
                    GateIndicator("RISK", riskPassed, Modifier.weight(1f))
                }
            }
            
            // Blocking Factors Section
            if (reasons.isNotEmpty()) {
                HorizontalDivider(color = stateColor.copy(alpha = 0.2f), thickness = 1.dp)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "BLOCKING FACTORS",
                        color = TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    val displayReasons = if (isExpanded) reasons else reasons.take(3)
                    
                    displayReasons.forEach { reasonItem ->
                        // Parse the reason to extract label and value
                        val parts = reasonItem.split("=", "|").map { it.trim() }
                        val label = parts.getOrNull(0)?.replace("_", " ") ?: reasonItem
                        val value = if (parts.size > 1) parts[1].replace("_", " ") else ""
                        
                        Surface(
                            color = Color(0xFF1A0A0A),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoiseRed.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        "▸",
                                        color = NoiseRed,
                                        fontSize = 10.sp,
                                        fontFamily = InterFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        label,
                                        color = TextGray,
                                        fontSize = 10.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }
                                
                                if (value.isNotEmpty()) {
                                    Text(
                                        value,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 10.sp,
                                        fontFamily = InterFontFamily,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Show expand button only if not expanded and there are more than 3 reasons
                    if (!isExpanded && reasons.size > 3) {
                        Text(
                            "+ ${reasons.size - 3} more factors",
                            color = stateColor,
                            fontSize = 9.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontFamily = InterFontFamily,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { isExpanded = true }
                        )
                    }
                }
            }
            
            // Key Feeder States Section
            if (isExpanded) {
                HorizontalDivider(
                    color = stateColor.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "KEY FEEDER STATES",
                        color = TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Surface(
                        color = Color(0xFF0D0D0D),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FeederStateRow("REGIME", (decision.regime_state ?: "Unknown").replace("_", " "))
                            FeederStateRow("VOLATILITY", (decision.feeder_volatility_state ?: "Unknown").replace("_", " "))
                            FeederStateRow("STRUCTURE", (decision.structure_state ?: "Unknown").replace("_", " "))
                            FeederStateRow("TREND", (decision.trend_state ?: "Unknown").replace("_", " "))
                            FeederStateRow("LIQUIDITY", (decision.confluence_state ?: "Unknown").replace("_", " "))
                            FeederStateRow("INDICATOR", (decision.chart_context_state ?: "Unknown").replace("_", " "))
                        }
                    }
                    
                    // Show "Show less" button after feeder states
                    Text(
                        "Show less",
                        color = stateColor,
                        fontSize = 9.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = InterFontFamily,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { isExpanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeederStateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = TextGray,
            fontSize = 10.sp,
            fontFamily = InterFontFamily
        )
        Text(
            value,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontFamily = InterFontFamily,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}


@Composable
private fun GateIndicator(label: String, passed: Boolean, modifier: Modifier = Modifier) {
    val gateColor = if (passed) PreMoveGreen else NoiseRed
    val gateIcon = if (passed) "✓" else "✗"
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            gateIcon,
            color = gateColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontFamily = InterFontFamily
        )
    }
}
