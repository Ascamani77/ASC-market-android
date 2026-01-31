package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

/**
 * Institutional Chart Container
 * Mimics TradingViewWidget.tsx script injection results.
 */
@Composable
fun TradingViewWidget(symbol: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color.White.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ADVANCED_CHART_WIDGET_V4",
                color = Color.White.copy(alpha = 0.1f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "CONNECTED: $symbol",
                color = IndigoAccent.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Copyright Bar Parity
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = "$symbol price by TradingView",
                color = Color(0xFF2962FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}