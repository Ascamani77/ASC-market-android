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

        // 4. Dynamic Additional Metrics (Flexible for any injected data)
        items(sessionData.additionalMetrics.size) { index ->
            RenderDynamicInfoBox(sessionData.additionalMetrics[index])
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