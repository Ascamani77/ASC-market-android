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
fun RejectedOrderItem(order: Order) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        OrderItemHeader(order)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.padding(start = 40.dp)) {
            val isBuy = order.type.lowercase() == "buy"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            HistoryDetailRow("Side", if (isBuy) "Buy" else "Sell", if (isBuy) Color(0xFF2962FF) else Color(0xFFF23645))
            HistoryDetailRow("Type", order.orderType)
            HistoryDetailRow("Qty", String.format(Locale.US, "%.2f", order.volume))
            
            val placingTimeStr = dateFormat.format(Date(order.time))
            HistoryDetailRow("Placing Time", placingTimeStr)
            
            val closingTimeStr = if (order.closingTime != null) dateFormat.format(Date(order.closingTime)) else placingTimeStr
            HistoryDetailRow("Closing Time", closingTimeStr)
            
            HistoryDetailRow("Order ID", order.id)
            HistoryDetailRow("Leverage", order.leverage)
            HistoryDetailRow("Margin", String.format(Locale.US, "%.2f USD", order.margin))
        }
    }
}
