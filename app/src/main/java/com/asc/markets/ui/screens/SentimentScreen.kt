package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun SentimentScreen() {
    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("PSYCHOLOGY", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("GLOBAL FEAR & GREED INDEX", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Large Meter Mock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(PureBlack, RoundedCornerShape(100.dp))
                .border(1.dp, HairlineBorder, RoundedCornerShape(100.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("72", color = EmeraldSuccess, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Text("GREED REGIME", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        SentimentMetric("RETAIL POSITIONING", "82% LONG", RoseError)
        Spacer(modifier = Modifier.height(12.dp))
        SentimentMetric("INSTITUTIONAL FLOW", "AGGRESSIVE BUY", EmeraldSuccess)
    }
}

@Composable
fun SentimentMetric(label: String, value: String, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}