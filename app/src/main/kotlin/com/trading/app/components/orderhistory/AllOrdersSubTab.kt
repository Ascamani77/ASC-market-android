package com.trading.app.components.orderhistory

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trading.app.models.Order
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AllOrdersItem(order: Order) {
    val statusLower = order.status.lowercase()
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        OrderItemHeader(order)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.padding(start = 44.dp)) {
            val isBuy = order.type.lowercase() == "buy"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            // Side
            HistoryDetailRow("Side", if (isBuy) "Buy" else "Sell", if (isBuy) Color(0xFF2962FF) else Color(0xFFF23645))
            
            // Type
            HistoryDetailRow("Type", order.orderType)
            
            // Qty
            HistoryDetailRow("Qty", String.format(Locale.US, "%.2f", order.volume))
            
            // Limit Price (Only show for Limit or Stop orders, or Filled Market as seen in screenshot)
            if (order.orderType.lowercase().contains("limit") || order.orderType.lowercase().contains("stop") || statusLower == "filled") {
                val label = if (order.orderType.lowercase().contains("stop")) "Stop Price" else "Limit Price"
                HistoryDetailRow(label, String.format(Locale.US, "%,.2f", order.price))
            }
            
            // Fill Price (Only for filled)
            if (statusLower == "filled") {
                HistoryDetailRow("Fill Price", String.format(Locale.US, "%,.2f", if (order.averagePrice > 0) order.averagePrice else order.price))
            }
            
            // Status
            val statusColor = when (statusLower) {
                "filled" -> Color(0xFF089981)
                "rejected", "cancelled" -> Color(0xFFF23645)
                "working", "inactive" -> Color(0xFFEBC147)
                else -> Color(0xFFD1D4DC)
            }
            HistoryDetailRow("Status", order.status, statusColor)
            
            // Placing Time
            val placingTimeStr = dateFormat.format(Date(order.time))
            HistoryDetailRow("Placing Time", placingTimeStr)
            
            // Closing Time
            val closingTimeStr = if (order.closingTime != null) {
                dateFormat.format(Date(order.closingTime))
            } else if (statusLower == "filled" || statusLower == "cancelled" || statusLower == "rejected") {
                placingTimeStr
            } else {
                "—"
            }
            HistoryDetailRow("Closing Time", closingTimeStr)
            
            // Order ID
            HistoryDetailRow("Order ID", order.id)
            
            // Leverage
            HistoryDetailRow("Leverage", order.leverage)
            
            // Margin
            HistoryDetailRow("Margin", String.format(Locale.US, "%.2f USD", order.margin))
        }
    }
}
