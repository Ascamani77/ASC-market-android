package com.trading.app.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.SymbolInfo
import com.trading.app.models.TradeNotification
import kotlinx.coroutines.delay

@Composable
fun TradeNotificationPopup(
    notification: TradeNotification,
    modifier: Modifier = Modifier,
    dismissTrigger: Boolean = false,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(isDismissing, dismissTrigger) {
        if (isDismissing || dismissTrigger) {
            visible = false
            delay(300)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        val backgroundColor = Color(0xFF252525)
        val title = when (notification.type) {
            "executed" -> "Market order executed on"
            "tp_placed" -> "Take Profit order placed on"
            "sl_placed" -> "Stop Loss order placed on"
            "tp_cancelled" -> "Take Profit order cancelled on"
            "sl_cancelled" -> "Stop Loss order cancelled on"
            "market_placed" -> "Market order placed on"
            else -> "Order executed on"
        }
        
        val icon = when (notification.type) {
            "executed" -> Icons.Default.Check
            "tp_cancelled", "sl_cancelled" -> Icons.Default.Close
            else -> Icons.Default.KeyboardArrowUp
        }
        
        val iconBgColor = if (notification.isBuy) Color(0xFF089981) else Color(0xFFF23645)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBgColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBgColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { isDismissing = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF787B86),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExchangeIcon(exchange = "exness", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val formattedSymbol = if (notification.symbol.contains("USD")) {
                            notification.symbol.uppercase()
                        } else {
                            "${notification.symbol.uppercase()}USD"
                        }
                        Text(
                            text = "EXNESS:$formattedSymbol",
                            color = Color(0xFF787B86),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val sideColor = if (notification.isBuy) Color(0xFF089981) else Color(0xFFF23645)
                    val sideText = if (notification.isBuy) "Buy" else "Sell"
                    val volumeText = String.format("%.0f", notification.volume)
                    
                    Text(
                        text = "$sideText $volumeText at ${notification.price}",
                        color = sideColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
