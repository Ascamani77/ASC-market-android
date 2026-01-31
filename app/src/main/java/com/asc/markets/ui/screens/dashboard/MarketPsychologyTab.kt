package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*

@Composable
fun MarketPsychologyTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        item {
            InfoBox(height = 320.dp) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GLOBAL PSYCHOLOGY METER", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.weight(1f))
                    PsychologyGauge(72)
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SentimentStateBox("STATE", "GREED REGIME", EmeraldSuccess)
                        SentimentStateBox("VOLATILITY", "STABLE", Color.White)
                        SentimentStateBox("DXY BETA", "0.82 HIGH", Color.White)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoBox(modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp), height = 120.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("SMART MONEY", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text("BULLISH", color = EmeraldSuccess, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("INSTITUTIONAL_HOLD", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
                InfoBox(modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp), height = 120.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("RETAIL FLOW", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text("BEARISH", color = RoseError, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("CROWDED_EXIT", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("NODE SENTIMENT ADVISOR", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                FOREX_PAIRS.take(6).forEach { pair ->
                    SentimentAssetRow(pair)
                }
            }
        }
    }
}

@Composable
private fun SentimentAssetRow(pair: com.asc.markets.data.ForexPair) {
    val bullishVal = (50 + (pair.changePercent * 10)).coerceIn(5.0, 95.0).toInt()
    
    InfoBox {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PairFlags(pair.symbol, 24)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pair.symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(if (bullishVal > 60) "BULLISH BIAS" else if (bullishVal < 40) "BEARISH BIAS" else "NEUTRAL", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$bullishVal%", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("/", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    Text("${100 - bullishVal}%", color = RoseError, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(80.dp).height(2.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(1.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(bullishVal / 100f).height(2.dp).background(EmeraldSuccess, RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}

@Composable
private fun PsychologyGauge(value: Int) {
    val animatedValue by animateFloatAsState(targetValue = value / 100f, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "gauge")
    
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            drawArc(
                color = Color(0xFF1A1C23),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.horizontalGradient(listOf(RoseError, Color(0xFFF59E0B), EmeraldSuccess)),
                startAngle = 135f,
                sweepAngle = 270f * animatedValue,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text("SCORE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun SentimentStateBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}