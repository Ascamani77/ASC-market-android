package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.CandleData

@Composable
fun CandlestickChart(
    data: List<CandleData>,
    symbol: String = "",
    timeframe: String = "H1",
    modifier: Modifier = Modifier,
    onTimeframeChange: (String) -> Unit = {}
) {
    var selectedTimeframe by remember { mutableStateOf(timeframe) }
    val timeframes = listOf("M1", "M5", "M15", "H1", "H4", "D1")

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Timeframe header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = symbol,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    timeframes.forEach { tf ->
                        TimeframePill(
                            text = tf,
                            isSelected = selectedTimeframe == tf,
                            onClick = {
                                selectedTimeframe = tf
                                onTimeframeChange(tf)
                            }
                        )
                    }
                }
            }

            // Placeholder chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFF0B0B0B))
            ) {
                Text(
                    text = "${data.size} candles for $selectedTimeframe",
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TimeframePill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF10B981) else Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.Gray,
            fontSize = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
