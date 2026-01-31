package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.EquityCurveGraph
import com.asc.markets.ui.theme.*

@Composable
fun AnalyticalQualityTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QualityMetricCard("SYSTEM ACCURACY", "76.4%", "H1/H4 BIAS VALID", Modifier.weight(1f))
                    QualityMetricCard("MODEL RELIABILITY", "84.2%", "DECISION QUALITY", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QualityMetricCard("WAIT EFFECTIVE", "92.5%", "NOISE PREVENTION", Modifier.weight(1f))
                    QualityMetricCard("SAFETY SUCCESS", "98.1%", "NEWS BLOCK ACC", Modifier.weight(1f))
                }
            }
        }

        item {
            InfoBox(height = 300.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("INSTITUTIONAL EQUITY CURVE", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text("THEORETICAL DISPATCH PROGRESSION", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        }
                        Text("$108,420", color = EmeraldSuccess, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    EquityCurveGraph(modifier = Modifier.fillMaxSize())
                }
            }
        }

        item {
            InfoBox {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("NEWS SAFETY GATE PERFORMANCE", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(20.dp))
                    OutcomeRow("VOLATILITY SPIKE AVOIDANCE", 85, EmeraldSuccess)
                    OutcomeRow("SPREAD WIDENING PROTECTION", 12, IndigoAccent)
                    OutcomeRow("DIRECTIONLESS MARKET AVOIDED", 3, Color.DarkGray)
                }
            }
        }
        
        item {
            InfoBox(minHeight = 80.dp) {
                Text(
                    text = "PERFORMANCE METRICS REFLECT DETERMINISTIC NODE ALIGNMENT. NO CLOUD DEPENDENCY DETECTED.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.DarkGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun QualityMetricCard(label: String, value: String, sub: String, modifier: Modifier) {
    InfoBox(modifier = modifier, height = 110.dp) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Column {
                Text(value, color = IndigoAccent, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(sub, color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun OutcomeRow(label: String, value: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text("$value%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(1.dp))) {
            Box(modifier = Modifier.fillMaxWidth(value/100f).height(2.dp).background(color, RoundedCornerShape(1.dp)))
        }
    }
}