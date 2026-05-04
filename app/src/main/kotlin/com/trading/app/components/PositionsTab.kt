package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.Position
import com.trading.app.models.SymbolInfo
import java.util.Locale

@Composable
fun PositionsTab(
    positions: List<Position>,
    currentPrice: Float,
    selectedPositionId: String? = null,
    visibility: PaperTradingVisibility = PaperTradingVisibility(),
    onPositionClick: (Position) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    labelColor: Color = Color(0xFF787B86)
) {
    if (positions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "There are no open positions in your trading account yet",
                color = labelColor,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(positions) { position ->
                PositionItem(
                    position, 
                    currentPrice, 
                    isSelected = position.id == selectedPositionId,
                    visibility = visibility,
                    onClick = { onPositionClick(position) },
                    onSettingsClick = onSettingsClick
                )
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFF2A2E39), 
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
private fun PositionItem(
    position: Position, 
    lastPrice: Float, 
    isSelected: Boolean,
    visibility: PaperTradingVisibility,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val labelColor = Color(0xFF787B86)
    val itemBackground = if (isSelected) Color(0xFF121212) else Color.Transparent
    
    // Calculations
    val isBuy = position.type == "buy"
    val pnl = (lastPrice - position.entryPrice) * position.volume * (if (isBuy) 1f else -1f)
    val pnlPercentage = (pnl / (position.entryPrice * position.volume)) * 100
    val tradeValue = position.entryPrice * position.volume
    val marketValue = lastPrice * position.volume
    val margin = tradeValue / 500.0f
    
    val pnlColor = if (pnl >= 0) Color(0xFF089981) else Color(0xFFF23645)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBackground)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(
                symbol = SymbolInfo(
                    ticker = position.symbol.split(":").last(),
                    name = "",
                    type = "forex"
                ),
                size = 24,
                modifier = Modifier.padding(end = 8.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2962FF))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "EXNESS:${position.symbol.uppercase()}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Custom 3-line vertical drag handle (|||), reduced boldness and height by 20%
            Row(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onSettingsClick() },
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(3.2.dp)
                            .height(20.4.dp)
                            .clip(RoundedCornerShape(1.6.dp))
                            .background(labelColor)
                    )
                }
            }
        }
        
        Text(
            "${position.symbol.uppercase()} VS US DOLLAR",
            color = labelColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, start = 0.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(start = 0.dp)) {
            if (visibility.side) PositionDetailRow("Side", if (isBuy) "Long" else "Short", if (isBuy) Color(0xFF2962FF) else Color(0xFFF23645))
            if (visibility.qty) PositionDetailRow("Qty", String.format(Locale.US, "%.2f", position.volume))
            if (visibility.avgFillPrice) PositionDetailRow("Avg Fill Price", String.format(Locale.US, "%,.2f", position.entryPrice))
            if (visibility.takeProfit) PositionDetailRow("Take Profit", position.tp?.let { String.format(Locale.US, "%,.2f", it) } ?: "")
            if (visibility.stopLoss) PositionDetailRow("Stop Loss", position.sl?.let { String.format(Locale.US, "%,.2f", it) } ?: "")
            if (visibility.lastPrice) PositionDetailRow("Last Price", String.format(Locale.US, "%,.2f", lastPrice))
            if (visibility.unrealizedPnl) PositionDetailRow("Unrealized P&L", String.format(Locale.US, "%s%,.2f USD", if (pnl >= 0) "" else "", pnl), pnlColor)
            if (visibility.unrealizedPnlPercentage) PositionDetailRow("Unrealized P&L %", String.format(Locale.US, "%.2f%%", pnlPercentage), pnlColor)
            if (visibility.tradeValue) PositionDetailRow("Trade Value", String.format(Locale.US, "%,.2f USD", tradeValue))
            if (visibility.marketValue) PositionDetailRow("Market Value", String.format(Locale.US, "%,.2f USD", marketValue))
            if (visibility.leverage) PositionDetailRow("Leverage", position.leverage)
            if (visibility.margin) PositionDetailRow("Margin", String.format(Locale.US, "%.2f USD", position.margin))
            if (visibility.expirationDate) PositionDetailRow("Expiration Date", "—")
        }
    }
}

@Composable
private fun PositionDetailRow(label: String, value: String, valueColor: Color = Color(0xFFD1D4DC)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF787B86),
            fontSize = 15.sp,
            modifier = Modifier.width(160.dp) // Increased width to add more space between label and value
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Start
        )
    }
}
