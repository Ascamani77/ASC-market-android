package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.asc.markets.ui.terminal.theme.*

@Composable
fun IndicatorsModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSelectIndicator: (String) -> Unit
) {
    if (!isOpen) return

    val indicators = listOf(
        Indicator("rsi", "Relative Strength Index (RSI)", "Momentum oscillator", favorite = true),
        Indicator("ema", "Moving Average Exponential (Double)", "Fast and slow trend direction", favorite = true),
        Indicator("sma", "Moving Average Simple (Double)", "Long-term trend indicators", favorite = true),
        Indicator("vol", "Volume", "Confirms breakouts and liquidity"),
        Indicator("vwap", "VWAP", "Intraday institutional reference", favorite = true),
        Indicator("atr", "ATR (Average True Range)", "Volatility sizing"),
        Indicator("bb", "Bollinger Bands", "Volatility expansion/contraction", favorite = true),
        Indicator("ma", "Moving Average", "Simple moving average")
    )

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .height(450.dp),
            color = Color(0xFF1E1E1E),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF363A45)),
            shadowElevation = 24.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Indicators",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
                
                HorizontalDivider(color = Color(0xFF363A45))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(indicators) { indicator ->
                        IndicatorItem(indicator) {
                            onSelectIndicator(indicator.id)
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

data class Indicator(val id: String, val name: String, val description: String, val favorite: Boolean = false)

@Composable
private fun IndicatorItem(indicator: Indicator, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = indicator.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = indicator.description,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Star, 
                contentDescription = null, 
                tint = if (indicator.favorite) Color(0xFFF27D26) else Color.Gray, 
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}
