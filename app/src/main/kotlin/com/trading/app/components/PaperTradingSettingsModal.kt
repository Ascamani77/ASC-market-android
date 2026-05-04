package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight

data class PaperTradingVisibility(
    val side: Boolean = true,
    val qty: Boolean = true,
    val avgFillPrice: Boolean = true,
    val takeProfit: Boolean = true,
    val stopLoss: Boolean = true,
    val lastPrice: Boolean = true,
    val unrealizedPnl: Boolean = true,
    val unrealizedPnlPercentage: Boolean = true,
    val tradeValue: Boolean = true,
    val marketValue: Boolean = true,
    val leverage: Boolean = true,
    val margin: Boolean = true,
    val expirationDate: Boolean = true,
    // For orders specifically
    val type: Boolean = true,
    val fillPrice: Boolean = true,
    val status: Boolean = true,
    val placingTime: Boolean = true,
    val orderId: Boolean = true,
    val expiry: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradingSettingsModal(
    visibility: PaperTradingVisibility,
    onVisibilityChange: (PaperTradingVisibility) -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
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
                .verticalScroll(rememberScrollState())
        ) {
            val items = listOf(
                "Side" to visibility.side,
                "Qty" to visibility.qty,
                "Avg Fill Price" to visibility.avgFillPrice,
                "Take Profit" to visibility.takeProfit,
                "Stop Loss" to visibility.stopLoss,
                "Last Price" to visibility.lastPrice,
                "Unrealized P&L" to visibility.unrealizedPnl,
                "Unrealized P&L %" to visibility.unrealizedPnlPercentage,
                "Trade Value" to visibility.tradeValue,
                "Market Value" to visibility.marketValue,
                "Leverage" to visibility.leverage,
                "Margin" to visibility.margin,
                "Expiration Date" to visibility.expirationDate
            )

            items.forEach { (label, isChecked) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newVisibility = when (label) {
                                "Side" -> visibility.copy(side = !isChecked)
                                "Qty" -> visibility.copy(qty = !isChecked)
                                "Avg Fill Price" -> visibility.copy(avgFillPrice = !isChecked)
                                "Take Profit" -> visibility.copy(takeProfit = !isChecked)
                                "Stop Loss" -> visibility.copy(stopLoss = !isChecked)
                                "Last Price" -> visibility.copy(lastPrice = !isChecked)
                                "Unrealized P&L" -> visibility.copy(unrealizedPnl = !isChecked)
                                "Unrealized P&L %" -> visibility.copy(unrealizedPnlPercentage = !isChecked)
                                "Trade Value" -> visibility.copy(tradeValue = !isChecked)
                                "Market Value" -> visibility.copy(marketValue = !isChecked)
                                "Leverage" -> visibility.copy(leverage = !isChecked)
                                "Margin" -> visibility.copy(margin = !isChecked)
                                "Expiration Date" -> visibility.copy(expirationDate = !isChecked)
                                else -> visibility
                            }
                            onVisibilityChange(newVisibility)
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            uncheckedColor = Color(0xFF787B86),
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
