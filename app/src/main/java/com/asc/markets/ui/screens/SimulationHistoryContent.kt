package com.asc.markets.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.Trade
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SimulationHistoryContent(trades: List<Trade>, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "FULL TRADE HISTORY",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { /* Export */ },
                    color = Color(0xFF142921),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPORT CSV",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Text(
                    text = "BACK TO SIMULATION",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header for list
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("ASSET", modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("TYPE", modifier = Modifier.weight(0.6f), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("RESULT", modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("TIME", modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 1.dp)
            
            if (trades.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trade history recorded.", color = Color.DarkGray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    items(trades) { trade ->
                        TradeHistoryItem(trade)
                        HorizontalDivider(color = Color(0xFF1C1C1E).copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun TradeHistoryItem(trade: Trade) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val time = dateFormat.format(Date(trade.timestamp))
    val isProfit = trade.profitLoss >= 0

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(trade.asset, modifier = Modifier.weight(1f), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            trade.type, 
            modifier = Modifier.weight(0.6f), 
            color = if (trade.type == "BUY") Color(0xFF10B981) else Color(0xFFEF4444), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Bold
        )
        Text(
            text = (if (isProfit) "+" else "") + String.format("%.2f", trade.profitLoss),
            modifier = Modifier.weight(1f),
            color = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(time, modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 11.sp)
    }
}
