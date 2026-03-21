package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.terminal.theme.*

@Composable
fun TradingToolbar(
    activeSymbol: String = "BTCUSD",
    currentTimeframe: String = "5m",
    currentStyle: String = "candles",
    onTimeframeChange: (String) -> Unit = {},
    onStyleChange: (String) -> Unit = {},
    onIndicatorsClick: () -> Unit = {},
    onIndicatorToggle: (String) -> Unit = {},
    onReplayClick: () -> Unit = {},
    onUndoClick: () -> Unit = {},
    onRedoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSymbolClick: () -> Unit = {},
    onTradeClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLayoutClick: (Int) -> Unit = {}
) {
    val timeframes = listOf("5m", "15m", "30m", "1h", "4h", "D", "W")

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            color = DarkSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFC62828), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .background(Color(0xFF131722), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                        .clickable { onSymbolClick() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(activeSymbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))
                LocalVerticalDivider()
                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .background(Color(0xFF131722), RoundedCornerShape(4.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    timeframes.take(5).forEach { tf ->
                        val isSelected = currentTimeframe == tf
                        Surface(
                            onClick = { onTimeframeChange(tf) },
                            color = if (isSelected) Color(0xFF2A2E39) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(tf, color = if (isSelected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
                LocalVerticalDivider()
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = { /* Style selection */ }) {
                    Icon(Icons.Default.BarChart, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }

                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .background(Color(0xFF131722), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                        .clickable { onIndicatorsClick() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Indicators", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onTradeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131722)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E39)),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Trade", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}

@Composable
private fun LocalVerticalDivider() {
    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2A2E39)))
}
