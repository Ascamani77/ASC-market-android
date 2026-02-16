package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.theme.*

@Composable
fun DashboardOverviewTab(symbol: String) {
    val sessionData = rememberSessionData()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // 1. Session Progress Widget (Parity: DashboardOverview.tsx)
        item {
            InfoBox(minHeight = 160.dp) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ACTIVE SESSION", color = Color.White, fontSize = DashboardFontSizes.dashboardActiveSession, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(sessionData.hubName, color = EmeraldSuccess, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text(if (sessionData.isActive) "ACTIVE" else "INACTIVE", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                        Box(modifier = Modifier.background(GhostWhite, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("${sessionData.completionPercent}% COMPLETE", color = Color.White, fontSize = DashboardFontSizes.gridLabelTiny, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                    SessionProgressGauge(sessionData.completionPercent)
                }
            }
        }

        // 2. Vitals Grid (Parity: DashboardOverview.tsx Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VitalMiniBox("AVG SPREAD", sessionData.avgSpread, "INSTITUTIONAL", Modifier.weight(1f))
                    VitalMiniBox("VOLATILITY", sessionData.volatility, "STANDARD", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VitalMiniBox("NEXT EVENT", sessionData.nextEventTime, sessionData.nextEventLabel, Modifier.weight(1f))
                    VitalMiniBox("SAFETY GATE", sessionData.safetyGateStatus, "PROP GUARD", Modifier.weight(1f))
                }
            }
        }

        // 3. Global Regime Snapshot
        item {
            InfoBox(minHeight = 160.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(16.dp).background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                        Text("GLOBAL REGIME", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        sessionData.globalRegimeText,
                        color = SlateText,
                        fontSize = DashboardFontSizes.labelMedium,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        // 4. Gap Analysis Box
        item {
            GapAnalysisBox(sessionData.gapAnalysis)
        }

        // 5. Volume Analysis Box
        item {
            VolumeAnalysisBox(sessionData.volumeAnalysis)
        }

        // 6. AI Sentiment Box
        item {
            AISentimentBox(sessionData.aiSentiment)
        }

        // 7. Risk Level Box
        item {
            RiskLevelBox(sessionData.riskLevel)
        }

        // 8. News Catalyst Box
        item {
            NewsCatalystBox(sessionData.newsCatalyst)
        }

        // 9. Probability Score Box
        item {
            ProbabilityScoreBox(sessionData.probabilityScore)
        }
    }
}

@Composable
private fun SessionProgressGauge(progress: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.03f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = EmeraldSuccess,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$progress%",
            color = Color.White,
            fontSize = DashboardFontSizes.valueLarge,
            fontWeight = FontWeight.Black,
            fontFamily = InterFontFamily
        )
    }
}

@Composable
private fun VitalMiniBox(label: String, value: String, sub: String, modifier: Modifier) {
    InfoBox(modifier = modifier, height = 120.dp) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Column {
                Text(value, color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(sub, color = Color.Gray, fontSize = DashboardFontSizes.gridLabelTiny, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        }
    }
}

// ===== STRUCTURED ANALYSIS BOXES =====

@Composable
private fun GapAnalysisBox(gap: GapAnalysis) {
    InfoBox(minHeight = 140.dp) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(16.dp).background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Text("GAP ANALYSIS", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Gap Size", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(gap.gapSize, color = if (gap.direction == "UP") EmeraldSuccess else Color(0xFFFF6B6B), fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Column {
                        Text("Change %", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(gap.gapPercent, color = if (gap.direction == "UP") EmeraldSuccess else Color(0xFFFF6B6B), fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Text("Direction: ${gap.direction}", color = Color.Gray, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun VolumeAnalysisBox(volume: VolumeAnalysis) {
    InfoBox(minHeight = 140.dp) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFFFF9500).copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Text("VOLUME ANALYSIS", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Volume Level", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(volume.volumeLevel, color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Column {
                        Text("Trend", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(volume.trend, color = Color(0xFFFF9500), fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Text("Strength: ${volume.strength}", color = Color.Gray, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun AISentimentBox(sentiment: AISentiment) {
    InfoBox(minHeight = 140.dp) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFF7C3AED).copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Text("AI SENTIMENT", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Score", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text("${sentiment.sentimentScore}/100", color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Column {
                        Text("State", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(sentiment.sentimentState, color = when(sentiment.sentimentState) {
                            "BULLISH" -> EmeraldSuccess
                            "BEARISH" -> Color(0xFFFF6B6B)
                            else -> Color.Yellow
                        }, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Text("Confidence: ${sentiment.confidence}%", color = Color.Gray, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun RiskLevelBox(risk: RiskLevel) {
    InfoBox(minHeight = 140.dp) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Text("RISK LEVEL", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Score", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(risk.riskScore, color = when(risk.status) {
                            "SAFE" -> EmeraldSuccess
                            "CAUTION" -> Color(0xFFFF9500)
                            "DANGER" -> Color(0xFFFF6B6B)
                            else -> Color.White
                        }, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Column {
                        Text("Value", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(risk.riskValue, color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Text("Status: ${risk.status}", color = Color.Gray, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun NewsCatalystBox(catalyst: NewsCatalyst) {
    InfoBox(minHeight = 140.dp) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFF0EA5E9).copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Text("NEWS CATALYST", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(catalyst.catalystName, color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Impact", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(catalyst.impactLevel, color = when(catalyst.impactLevel) {
                            "LOW" -> Color.Gray
                            "MEDIUM" -> Color(0xFFFF9500)
                            "HIGH" -> Color(0xFFFF6B6B)
                            else -> Color.White
                        }, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Column {
                        Text("Time to Event", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(catalyst.timeToEvent, color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Text("Status: ${if (catalyst.isActive) "LIVE" else "SCHEDULED"}", color = if (catalyst.isActive) EmeraldSuccess else Color.Gray, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun ProbabilityScoreBox(probability: ProbabilityScore) {
    InfoBox(minHeight = 140.dp) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFF06B6D4).copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Text("PROBABILITY SCORE", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = probability.scoreValue / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    color = when {
                        probability.scoreValue >= 70 -> EmeraldSuccess
                        probability.scoreValue >= 50 -> Color(0xFFFF9500)
                        else -> Color(0xFFFF6B6B)
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Score", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text("${probability.scoreValue}%", color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Column {
                        Text("Prediction", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(probability.prediction, color = when(probability.prediction) {
                            "BULLISH" -> EmeraldSuccess
                            "BEARISH" -> Color(0xFFFF6B6B)
                            else -> Color.Yellow
                        }, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Text("Confidence: ${probability.confidenceLevel}", color = Color.Gray, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            }
        }
    }
}