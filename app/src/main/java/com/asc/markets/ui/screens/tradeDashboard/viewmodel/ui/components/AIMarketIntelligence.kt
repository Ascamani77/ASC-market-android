@file:OptIn(ExperimentalLayoutApi::class)

package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.AIMarketIntelligence as MarketIntel
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.DeepBlack

@Composable
fun AIMarketIntelligence(intel: MarketIntel?, modifier: Modifier = Modifier) {
    if (intel == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepBlack)
            .padding(16.dp)
    ) {
        // MARKET INTELLIGENCE Header
        Text(
            text = "MARKET INTELLIGENCE",
            color = Color(0xFF00C853),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Top Score Boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScoreBox(
                label = "TREND STRENGTH",
                value = "${intel.trendStrength}%",
                score = intel.trendStrength,
                icon = Icons.Default.BarChart,
                modifier = Modifier.weight(1f)
            )
            ScoreBox(
                label = "VOLATILITY SCORE",
                value = "${intel.volatilityScore}/100",
                score = intel.volatilityScore,
                icon = Icons.Default.Timeline,
                modifier = Modifier.weight(1f)
            )
            ScoreBox(
                label = "MOMENTUM SCORE",
                value = "${intel.momentumScore}%",
                score = intel.momentumScore,
                icon = Icons.AutoMirrored.Filled.ShowChart,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // TREND BY TIMEFRAME Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TREND BY TIMEFRAME",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Check if trends exist and render them
            val trends = intel.timeframeTrends
            if (trends != null) {
                TimeframeRow("M5", trends.m5)
                TimeframeRow("M15", trends.m15)
                TimeframeRow("M30", trends.m30)
                TimeframeRow("H1", trends.h1)
                TimeframeRow("H4", trends.h4)
                TimeframeRow("D1", trends.d1)
            } else {
                // Fallback UI if data is missing
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trend data available", color = Color.DarkGray, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // VOLATILITY DRIVERS Section Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "VOLATILITY DRIVERS",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wrapped layout for volatility drivers chips
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            intel.volatilityDrivers.forEach { driver ->
                DriverChip(driver)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // MARKET PHASE (Accumulation/Distribution) Box
        MarketPhaseBox(
            phase = intel.marketPhase,
            description = intel.phaseDescription ?: "Smart money is building positions. Price is consolidating within a range with increasing bullish pressure."
        )
    }
}

@Composable
private fun MarketPhaseBox(phase: String, description: String) {
    val emeraldColor = Color(0xFF00C853)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, emeraldColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color(0xFF04140D), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Green vertical indicator
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(12.dp)
                                .background(emeraldColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CURRENT MARKET PHASE",
                            color = emeraldColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = phase.uppercase(),
                        color = emeraldColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Stack icon in a styled box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(emeraldColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, emeraldColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = emeraldColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Description box with info icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = emeraldColor,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = description,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreBox(
    label: String, 
    value: String, 
    score: Int, 
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        LineScale(score = score, height = 3.dp)
    }
}

@Composable
private fun TimeframeRow(label: String, score: Int) {
    Column(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "$score%",
                color = if (score >= 50) Color(0xFF00C853) else Color(0xFFFF5252),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LineScale(score = score, height = 4.dp)
    }
}

@Composable
private fun DriverChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF111111), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text, 
            color = Color.LightGray, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LineScale(score: Int, height: Dp) {
    val barColor = if (score >= 50) Color(0xFF00C853) else Color(0xFFFF5252)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(score / 100f)
                .background(barColor, RoundedCornerShape(2.dp))
        )
    }
}
