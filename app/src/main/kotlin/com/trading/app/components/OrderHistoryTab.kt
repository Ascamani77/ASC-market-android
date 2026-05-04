package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
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
import com.trading.app.components.orderhistory.*

@Composable
fun OrderHistoryTab(
    orders: List<Order>,
    labelColor: Color = Color(0xFF787B86)
) {
    var activeSubTab by remember { mutableStateOf("All") }
    val subTabs = listOf("All", "Filled", "Cancelled", "Rejected")

    // MT5 data is now live from mt5_bridge.py
    val baseOrders = orders


    // Dynamic counts
    val allCount = baseOrders.size
    val filledCount = baseOrders.count { it.status.equals("Filled", ignoreCase = true) }
    val cancelledCount = baseOrders.count { it.status.equals("Cancelled", ignoreCase = true) }
    val rejectedCount = baseOrders.count { it.status.equals("Rejected", ignoreCase = true) }

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
                val isSelected = activeSubTab == subTab
                val count = when (subTab) {
                    "All" -> allCount
                    "Filled" -> filledCount
                    "Cancelled" -> cancelledCount
                    "Rejected" -> rejectedCount
                    else -> 0
                }
                
                Text(
                    text = "$subTab $count",
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
            "Filled" -> baseOrders.filter { it.status.equals("Filled", ignoreCase = true) }
            "Cancelled" -> baseOrders.filter { it.status.equals("Cancelled", ignoreCase = true) }
            "Rejected" -> baseOrders.filter { it.status.equals("Rejected", ignoreCase = true) }
            else -> baseOrders
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredOrders) { order ->
                when (activeSubTab) {
                    "All" -> AllOrdersItem(order)
                    "Filled" -> FilledOrderItem(order)
                    "Cancelled" -> CancelledOrderItem(order)
                    "Rejected" -> RejectedOrderItem(order)
                    else -> AllOrdersItem(order)
                }
                Divider(color = Color(0xFF2A2E39), thickness = 1.dp)
            }
        }
        
        PaginationRow()
    }
}
