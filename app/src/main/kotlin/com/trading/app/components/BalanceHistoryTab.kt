package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.BalanceRecord
import com.trading.app.components.orderhistory.PaginationRow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BalanceHistoryTab(
    records: List<BalanceRecord> = emptyList(),
    labelColor: Color = Color(0xFF787B86)
) {
    // MT5 data is now live from mt5_bridge.py
    val displayRecords = records


    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(displayRecords) { record ->
                BalanceHistoryItem(record)
                Divider(color = Color(0xFF2A2E39), thickness = 1.dp)
            }
        }
        PaginationRow()
    }
}

@Composable
private fun BalanceHistoryItem(record: BalanceRecord) {
    val labelColor = Color(0xFF787B86)
    val valueColor = Color(0xFFD1D4DC)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Top right decoration (000)
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(11.dp)
                            .background(Color(0xFF2A2E39))
                    )
                }
            }
        }

        // Time
        BalanceDetailRow("Time", dateFormat.format(Date(record.time)))
        
        // Balance Before
        BalanceDetailRow("Balance Before", String.format(Locale.US, "%,.2f", record.balanceBefore))
        
        // Balance After
        BalanceDetailRow("Balance After", String.format(Locale.US, "%,.2f", record.balanceAfter))
        
        // Realized P&L
        val pnlColor = if (record.realizedPnl >= 0) Color(0xFF089981) else Color(0xFFF23645)
        val pnlText = String.format(Locale.US, "%s%,.2f USD", if (record.realizedPnl >= 0) "" else "", record.realizedPnl)
        BalanceDetailRow("Realized P&L", pnlText, pnlColor)
        
        // Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "Action",
                color = labelColor,
                fontSize = 15.sp,
                modifier = Modifier.width(125.dp)
            )
            Text(
                text = record.action,
                color = valueColor,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BalanceDetailRow(label: String, value: String, valueColor: Color = Color(0xFFD1D4DC)) {
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
