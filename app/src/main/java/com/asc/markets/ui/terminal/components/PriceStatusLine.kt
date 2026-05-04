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
import java.util.Locale

@Composable
fun PriceStatusLine(
    symbol: String = "GBPUSD",
    timeframe: String = "5m",
    price: Double = 0.0,
    changePercent: Double = 0.0,
    priceHistory: List<Double> = emptyList(),
    indicators: List<String> = emptyList(),
    onIndicatorAction: (String, String) -> Unit = { _, _ -> },
    onBuy: () -> Unit = {},
    onSell: () -> Unit = {},
    isBuyEnabled: Boolean = true,
    isSellEnabled: Boolean = true,
    settings: StatusLineSettings = StatusLineSettings()
) {
    val effectiveHistory = remember(price, priceHistory) {
        when {
            priceHistory.isNotEmpty() -> priceHistory
            price > 0.0 -> List(20) { price }
            else -> emptyList()
        }
    }
    val currentPrice = price.takeIf { it > 0.0 } ?: effectiveHistory.lastOrNull() ?: 0.0
    val openValue = effectiveHistory.firstOrNull() ?: currentPrice
    val highValue = effectiveHistory.maxOrNull() ?: currentPrice
    val lowValue = effectiveHistory.minOrNull() ?: currentPrice
    val bidValue = currentPrice
    val askValue = currentPrice + computeSpread(currentPrice)

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
                OHLCItem("O", formatPrice(openValue), TradingGreen)
                OHLCItem("H", formatPrice(highValue), TradingGreen)
                OHLCItem("L", formatPrice(lowValue), TradingRed)
                OHLCItem("C", formatPrice(currentPrice), Color.White)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PriceItem("Bid", formatPrice(bidValue), Color.White)
            PriceItem("Ask", formatPrice(askValue), Color.White)
            PriceItem("Chg", formatChange(changePercent), if (changePercent >= 0.0) TradingGreen else TradingRed)
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

private fun computeSpread(price: Double): Double {
    return when {
        price >= 10_000 -> 1.0
        price >= 100 -> 0.05
        price >= 1 -> 0.0002
        else -> 0.00001
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format(Locale.US, "%,.2f", price)
        price >= 1 -> String.format(Locale.US, "%.4f", price)
        else -> String.format(Locale.US, "%.6f", price)
    }
}

private fun formatChange(changePercent: Double): String {
    return String.format(
        Locale.US,
        "%s%.2f%%",
        if (changePercent >= 0.0) "+" else "",
        changePercent
    )
}
