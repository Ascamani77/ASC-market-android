package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import com.trading.app.models.Position
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionActionsModal(
    position: Position,
    lastPrice: Float,
    onClose: () -> Unit,
    onModify: () -> Unit,
    onClosePosition: () -> Unit,
    onNewOrder: () -> Unit,
    onViewChart: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val labelColor = Color(0xFF787B86)
    val isBuy = position.type.equals("buy", ignoreCase = true)
    val pnl = (lastPrice - position.entryPrice) * position.volume * (if (isBuy) 1f else -1f)
    val pnlColor = if (pnl >= 0) Color(0xFF2962FF) else Color(0xFFF23645)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212), // Charcoal black
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2A2E39))
            )
        },
        windowInsets = WindowInsets(0),
        modifier = Modifier.padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header Info (Matching the screenshot style)
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${position.symbol.split(":").last()}m, ",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBuy) "buy ${position.volume}" else "sell ${position.volume}",
                                color = if (isBuy) Color(0xFF2962FF) else Color(0xFFF23645),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%,.2f → %,.2f", position.entryPrice, lastPrice),
                            color = labelColor,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f", pnl),
                        color = pnlColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "#${position.id}", color = labelColor, fontSize = 12.sp)
                        Text(text = "S / L:", color = labelColor, fontSize = 12.sp)
                        Text(text = "T / P:", color = labelColor, fontSize = 12.sp)
                    }
                    Column(modifier = Modifier.weight(1.5f)) {
                        val dateFormat = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US)
                        Text(text = "Open:    ${dateFormat.format(java.util.Date(System.currentTimeMillis()))}", color = labelColor, fontSize = 12.sp)
                        Text(text = "Swap:    0.00", color = labelColor, fontSize = 12.sp)
                    }
                }
            }

            Divider(color = Color(0xFF2A2E39), thickness = 1.dp)

            // Action List
            val actions = listOf(
                "Close position" to onClosePosition,
                "Modify position" to onModify,
                "New order" to onNewOrder,
                "Chart" to onViewChart,
                "Bulk Operations..." to {}
            )

            actions.forEach { (label, action) ->
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            action()
                            onClose()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }
    }
}
