package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun StructuralContext(symbol: String) {
    InfoBox(modifier = Modifier, minHeight = 260.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("⚡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("STRUCTURAL CONTEXT", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.background(GhostWhite, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Text(
                        "The current price action on $symbol displays a sequence of HIGHER HIGHS. We detect institutional mitigation approaching the pivot zone.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ContextMetric("LIQUIDITY GRAB", "YES")
                ContextMetric("RETAIL TREND", "CROWDED")
                ContextMetric("VOL BAND", "NORMAL")
            }
        }
    }
}

@Composable
private fun ContextMetric(label: String, value: String) {
    Column {
        Text(label, color = SlateText, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}
