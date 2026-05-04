package com.trading.app.components.orderhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.trading.app.models.Order

@Composable
fun OrderItemHeader(order: Order, labelColor: Color = Color(0xFF787B86)) {
    val symbolUpper = order.symbol.uppercase()
    
    Box(modifier = Modifier.fillMaxWidth()) {
        // Triple Bar Icon at the top right (|||), reduced boldness and height by 20%
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 0.dp).size(22.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(3.2.dp)
                        .height(14.4.dp)
                        .clip(RoundedCornerShape(1.6.dp))
                        .background(Color(0xFF2A2E39))
                )
            }
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ticker Box
                val isBlueBox = symbolUpper.contains("EXNESS") || symbolUpper.contains("XAU")
                val boxBg = if (isBlueBox) Color(0xFF2962FF) else Color(0xFF2A2E39)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(boxBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val displaySymbol = symbolUpper.replace("PEPPERSTONE", "EXNESS")
                    Text(
                        text = displaySymbol, 
                        color = Color.White, 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Status Icon (Red circle with white exclamation mark)
                if (order.status.lowercase() == "rejected" || order.status.lowercase() == "cancelled") {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF23645)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Description
            val description = when {
                symbolUpper.contains("XAU") -> "GOLD VS US DOLLAR"
                symbolUpper.contains("GOLD") -> "GOLD (US$/OZ)"
                symbolUpper.contains("META") -> "META PLATFORMS INC CLASS A"
                else -> "ASSET DESCRIPTION"
            }
            
            if (description.isNotEmpty()) {
                Text(
                    text = description, 
                    color = labelColor, 
                    fontSize = 13.sp, 
                    modifier = Modifier.padding(top = 4.dp, start = 0.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryDetailRow(label: String, value: String, valueColor: Color = Color(0xFFD1D4DC)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            color = Color(0xFF787B86), 
            fontSize = 15.sp, 
            modifier = Modifier.width(125.dp)
        )
        Text(
            text = value, 
            color = valueColor, 
            fontSize = 15.sp, 
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun PaginationRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp), 
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ChevronLeft, null, tint = Color(0xFF787B86), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A2E39)), 
            contentAlignment = Alignment.Center
        ) {
            Text("1", color = Color.White, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("2", color = Color(0xFF787B86), fontSize = 14.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text("3", color = Color(0xFF787B86), fontSize = 14.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF787B86), modifier = Modifier.size(24.dp))
    }
}
