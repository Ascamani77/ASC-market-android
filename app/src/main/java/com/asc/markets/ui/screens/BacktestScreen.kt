package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.InfoBox

@Composable
fun BacktestScreen() {
    val scrollState = rememberScrollState()
    var isRunning by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp).verticalScroll(scrollState)
    ) {
        Text("STRATEGY SIMULATION", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text("HISTORICAL AUDIT ENGINE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Calibration Desk Parity
        Surface(
            color = PureBlack,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("CALIBRATION DESK", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(32.dp))
                
                BacktestSlider("MA FAST", 20f)
                BacktestSlider("MA SLOW", 50f)
                BacktestSlider("RSI PERIOD", 14f)
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = { isRunning = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    if (isRunning) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 3.dp)
                    else Text("INITIATE HISTORICAL AUDIT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
                }
            }
        }

        if (isRunning) {
            Spacer(modifier = Modifier.height(24.dp))
            DetailedResultsGrid()
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun DetailedResultsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("WIN RATE", "68.4%", EmeraldSuccess, Modifier.weight(1f))
            MetricCard("PROFIT FACTOR", "2.14", IndigoAccent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("EXPECTANCY", "+14.2 Pips", Color.White, Modifier.weight(1f))
            MetricCard("SHARPE RATIO", "1.82", Color.White, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("RECOVERY", "4.1x", Color.White, Modifier.weight(1f))
            MetricCard("MAX DRAWDOWN", "-4.5%", RoseError, Modifier.weight(1f))
        }
        
        InfoBox(minHeight = 200.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("STRATEGY VERDICT", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("BULLISH EDGE FOUND", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Simulation confirms high statistical alignment with institutional order flow during London session liquidity windows. Sharpe ratio exceeds benchmark 1.5 threshold.",
                    color = SlateText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
fun BacktestSlider(label: String, value: Float) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text(value.toInt().toString(), color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Slider(
            value = value, 
            onValueChange = {}, 
            valueRange = 0f..200f, 
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = IndigoAccent, inactiveTrackColor = GhostWhite)
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}