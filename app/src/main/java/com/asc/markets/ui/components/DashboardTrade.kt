package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.label
import com.asc.markets.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import java.util.Locale

@Composable
fun DashboardTrade(case: PostMoveAuditCase, onGenerateCompliance: (PostMoveAuditCase) -> Unit) {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PairFlags(case.symbol, 36)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(case.symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${case.direction} POST-MOVE", color = if (case.direction == "SHORT") RoseError else EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(case.source.label(), color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(case.status, color = outcomeColor(case), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SLIPPAGE AUDIT", color = SlateText, fontSize = 12.sp)
                Text(case.slippagePips?.let { String.format("%.2f PIPS", it) } ?: "NOT CAPTURED", color = if (case.slippagePips != null && case.slippagePips > 0.0) RoseError else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("OUTCOME SCORE", color = SlateText, fontSize = 12.sp)
                Text(case.modelAccuracyScore?.let { "$it%" } ?: "PENDING", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Metrics Section
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💰", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FINANCIAL METRICS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("ENTRY PRICE", color = SlateText, fontSize = 10.sp)
                            Text(
                                PostMoveAuditStore.formatPrice(case.entryPrice),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("EXIT PRICE", color = SlateText, fontSize = 10.sp)
                            Text(
                                PostMoveAuditStore.formatPrice(case.exitPrice),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("REALIZED PNL", color = SlateText, fontSize = 10.sp)
                        Text(
                            case.pnl?.let { String.format(Locale.US, "%+,.2f", it) } ?: "NOT CAPTURED",
                            color = if (case.pnl != null && case.pnl > 0) EmeraldSuccess else RoseError,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Scores at Entry Section
            if (case.preMoveScoreAtEntry != null || case.compressionScoreAtEntry != null || case.ignitionScoreAtEntry != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI SCORES AT ENTRY", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            case.preMoveScoreAtEntry?.let { score ->
                                ScoreBox(
                                    label = "PRE-MOVE",
                                    score = score,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            case.compressionScoreAtEntry?.let { score ->
                                ScoreBox(
                                    label = "COMPRESSION",
                                    score = score,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            case.ignitionScoreAtEntry?.let { score ->
                                ScoreBox(
                                    label = "IGNITION",
                                    score = score,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        case.liquidityTarget?.let { target ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("LIQUIDITY TARGET", color = SlateText, fontSize = 10.sp)
                                Text(target, color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Deployment & Risk Section
            if (case.deploymentLabel != null || case.confidence != null || case.riskPct != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DEPLOYMENT & RISK", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            case.deploymentLabel?.let { label ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DEPLOYMENT", color = SlateText, fontSize = 10.sp)
                                    Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            case.confidence?.let { conf ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("CONFIDENCE", color = SlateText, fontSize = 10.sp)
                                    Text("$conf%", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            case.riskPct?.let { risk ->
                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                    Text("RISK %", color = SlateText, fontSize = 10.sp)
                                    Text(
                                        String.format(Locale.US, "%.2f%%", risk * 100),
                                        color = if (risk > 0.02) RoseError else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Market Context Section (from reconstruction lines)
            if (case.reconstructionLines.isNotEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📈", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MARKET CONTEXT", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        case.reconstructionLines.forEach { line ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("• ", color = IndigoAccent, fontSize = 12.sp)
                                Text(
                                    line,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = { onGenerateCompliance(case) }, modifier = Modifier.fillMaxWidth(0.9f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painter = painterResource(id = com.asc.markets.R.drawable.lucide_book_open), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("VIEW POST-MOVE RECONSTRUCTION")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = com.asc.markets.R.drawable.lucide_binary), contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("POST-EXECUTION ANALYTICS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = case.postMoveOutcome,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MFE / MAE", color = SlateText, fontSize = 12.sp)
                        Text("${PostMoveAuditStore.formatPercent(case.maxFavorableExcursionPct)} / ${PostMoveAuditStore.formatPercent(case.maxAdverseExcursionPct)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MOVE", color = SlateText, fontSize = 12.sp)
                        Text(PostMoveAuditStore.formatPercent(case.actualMovePct), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            case.timeToTargetMs?.let { timeMs ->
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TIME TO TARGET", color = SlateText, fontSize = 11.sp)
                        }
                        Text(
                            formatDuration(timeMs),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("POST-MOVE SOURCE", color = SlateText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(case.nodeId, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = SlateText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(case.id, color = SlateText, fontSize = 12.sp)
                        }
                    }

                    Surface(color = outcomeColor(case).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Text(case.status, color = outcomeColor(case), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

private fun outcomeColor(case: PostMoveAuditCase): Color {
    return when {
        case.targetHit == true -> EmeraldSuccess
        case.invalidationHit == true -> RoseError
        case.status == "UNRESOLVED" -> SlateText
        else -> IndigoAccent
    }
}

@Composable
private fun ScoreBox(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 75 -> EmeraldSuccess
        score >= 50 -> IndigoAccent
        score >= 25 -> Color(0xFFFFD600)
        else -> RoseError
    }
    
    Surface(
        color = scoreColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = SlateText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$score",
                color = scoreColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
