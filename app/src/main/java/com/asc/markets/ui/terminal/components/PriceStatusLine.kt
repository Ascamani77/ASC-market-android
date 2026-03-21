package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.terminal.models.StatusLineSettings
import com.asc.markets.ui.terminal.theme.*

@Composable
fun PriceStatusLine(
    symbol: String = "GBPUSD",
    timeframe: String = "5m",
    open: String = "61747.59",
    high: String = "62269.58",
    low: String = "61538.15",
    close: String = "62028.63",
    change: String = "+4.28 (+0.01%)",
    isPositive: Boolean = true,
    indicators: List<String> = emptyList(),
    onIndicatorAction: (String, String) -> Unit = { _, _ -> },
    onBuy: () -> Unit = {},
    onSell: () -> Unit = {},
    isBuyEnabled: Boolean = true,
    isSellEnabled: Boolean = true,
    settings: StatusLineSettings = StatusLineSettings()
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = symbol,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "· $timeframe",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        if (settings.showOhlc) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OHLCItem("O", open, TradingGreen)
                OHLCItem("H", high, TradingGreen)
                OHLCItem("L", low, TradingRed)
                OHLCItem("C", close, Color.White)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PriceItem("Bid", "62028.63000", Color.White)
            PriceItem("Ask", "62034.83286", Color.White)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            LargeBuySellButton("SELL", "0.00", Color(0xFFF23645), isLeft = true, onClick = onSell, enabled = isSellEnabled)
            QuantitySelector("40")
            LargeBuySellButton("BUY", "0.05", Color(0xFF2962FF), isLeft = false, onClick = onBuy, enabled = isBuyEnabled)
        }

        indicators.forEach { indicator ->
            IndicatorItem(
                name = indicator,
                onAction = { action -> onIndicatorAction(indicator, action) }
            )
        }
        
        if (indicators.isEmpty()) {
            IndicatorItem(
                name = "Volume",
                onAction = { }
            )
        }
    }
}

@Composable
private fun OHLCItem(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label ", color = TextSecondary, fontSize = 9.sp)
        Text(text = value, color = valueColor, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PriceItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label ", color = TextSecondary, fontSize = 10.sp)
        Text(text = value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LargeBuySellButton(label: String, value: String, color: Color, isLeft: Boolean, onClick: () -> Unit = {}, enabled: Boolean = true) {
    Surface(
        color = color,
        shape = if (isLeft) RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp) else RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
        modifier = Modifier
            .width(70.dp)
            .height(56.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuantitySelector(value: String) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(40.dp)
            .background(Color(0xFF131722))
            .background(Color.White.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IndicatorItem(name: String, onAction: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = name,
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(10.dp))
        
        Surface(
            color = Color(0xFF1E222D),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IndicatorIcon(Icons.Default.VisibilityOff, "Hide") { onAction("hide") }
                IndicatorIcon(Icons.Default.Settings, "Settings") { onAction("settings") }
                IndicatorIcon(Icons.Default.Delete, "Delete") { onAction("delete") }
            }
        }
    }
}

@Composable
private fun IndicatorIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}
