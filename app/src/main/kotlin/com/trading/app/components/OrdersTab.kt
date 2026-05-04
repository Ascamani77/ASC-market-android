package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.Order
import com.trading.app.models.SymbolInfo
import java.util.Locale

@Composable
fun OrdersTab(
    orders: List<Order>,
    visibility: PaperTradingVisibility = PaperTradingVisibility(),
    onOrderClick: (Order) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    labelColor: Color = Color(0xFF787B86)
) {
    var activeSubTab by remember { mutableStateOf("All") }
    val subTabs = listOf("All", "Working", "Inactive", "Filled", "Cancelled", "Rejected")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            subTabs.forEach { subTab ->
                val isSelected = activeSubTab.equals(subTab, ignoreCase = true)
                val count = when (subTab) {
                    "All" -> orders.size
                    "Working" -> orders.count { it.status.equals("Working", ignoreCase = true) }
                    "Inactive" -> orders.count { it.status.equals("Inactive", ignoreCase = true) }
                    "Filled" -> orders.count { it.status.equals("Filled", ignoreCase = true) }
                    "Cancelled" -> orders.count { it.status.equals("Cancelled", ignoreCase = true) }
                    "Rejected" -> orders.count { it.status.equals("Rejected", ignoreCase = true) }
                    else -> 0
                }
                val displayText = if (count > 0) "$subTab $count" else subTab
                
                Text(
                    text = displayText,
                    color = if (isSelected) Color.White else labelColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF2A2E39) else Color.Transparent)
                        .clickable { activeSubTab = subTab }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Divider(color = Color(0xFF2A2E39), thickness = 1.dp)

        val filteredOrders = when (activeSubTab) {
            "All" -> orders
            else -> orders.filter { it.status.equals(activeSubTab, ignoreCase = true) }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredOrders) { order ->
                OrderItemComponent(
                    order = order,
                    visibility = visibility,
                    showStatus = activeSubTab.equals("All", ignoreCase = true),
                    onSettingsClick = onSettingsClick
                )
                Divider(color = Color(0xFF2A2E39), thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun OrderItemComponent(
    order: Order, 
    visibility: PaperTradingVisibility,
    showStatus: Boolean,
    onSettingsClick: () -> Unit
) {
    val labelColor = Color(0xFF787B86)
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(
                symbol = SymbolInfo(
                    ticker = order.symbol.split(":").last(),
                    name = "",
                    type = "forex"
                ),
                size = 24,
                modifier = Modifier.padding(end = 8.dp)
            )
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2962FF)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(text = "EXNESS:${order.symbol.uppercase().split(":").last()}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        Text(text = "GOLD VS US DOLLAR", color = labelColor, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, start = 0.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.padding(start = 0.dp)) {
            val isBuy = order.type.equals("buy", ignoreCase = true)
            if (visibility.side) OrderDetailRow("Side", if (isBuy) "Buy" else "Sell", if (isBuy) Color(0xFF2962FF) else Color(0xFFF23645))
            if (visibility.type) OrderDetailRow("Type", order.orderType)
            if (visibility.qty) OrderDetailRow("Qty", order.volume.toInt().toString())
            if (visibility.fillPrice) OrderDetailRow("Fill Price", String.format(Locale.US, "%,.2f", order.averagePrice))
            if (visibility.takeProfit) OrderDetailRow("Take Profit", "")
            if (visibility.stopLoss) OrderDetailRow("Stop Loss", "")
            
            if (showStatus && visibility.status) {
                val statusColor = when (order.status.lowercase()) {
                    "filled" -> Color(0xFF089981)
                    "rejected" -> Color(0xFFF23645)
                    "working", "inactive" -> Color(0xFFEBC147)
                    else -> Color(0xFFD1D4DC)
                }
                OrderDetailRow("Status", order.status.uppercase(), statusColor)
            }

            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            if (visibility.placingTime) OrderDetailRow("Placing Time", dateFormat.format(java.util.Date(order.time)))
            if (visibility.orderId) OrderDetailRow("Order ID", order.id)
            if (visibility.expiry) OrderDetailRow("Expiry", order.expiry?.let { dateFormat.format(java.util.Date(it)) } ?: "—")
            if (visibility.leverage) OrderDetailRow("Leverage", order.leverage)
            if (visibility.margin) OrderDetailRow("Margin", String.format(Locale.US, "%.2f USD", order.margin))
        }
    }
}

@Composable
private fun OrderDetailRow(label: String, value: String, valueColor: Color = Color(0xFFD1D4DC)) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = Color(0xFF787B86), fontSize = 15.sp, modifier = Modifier.width(130.dp))
        Text(text = value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
