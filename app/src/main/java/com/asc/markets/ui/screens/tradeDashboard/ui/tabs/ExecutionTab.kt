package com.asc.markets.ui.screens.tradeDashboard.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.ui.screens.tradeDashboard.model.*
import com.asc.markets.ui.screens.tradeDashboard.ui.components.*
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel

import java.util.Locale

/**
 * EXECUTION TAB - "What should I execute?"
 * Shows: Ready Trades, One-Click Execution, Order Entry, Trade History
 */
@Composable
fun ExecutionTab(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val dividerColor = Color(0xFF151515)
    var manualTradeEntry by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 48.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXECUTION READY",
                color = Color(0xFF00C853),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 1. Quick Execution Buttons
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ONE-CLICK ACTIONS",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { executeSignalTrade(viewModel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("EXECUTE SIGNAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { manualTradeEntry = !manualTradeEntry },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("MANUAL ORDER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 2. Manual Trade Entry (conditional)
        if (manualTradeEntry) {
            ManualTradeEntryForm(onClose = { manualTradeEntry = false }, viewModel = viewModel)
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
        }

        // 3. Open Positions Table - Current executions
        PositionsTable(
            positions = viewModel.positions,
            selectedSymbol = viewModel.selectedSymbol,
            onAdjustSL = { ticket, newSL -> viewModel.adjustStopLoss(ticket, newSL) },
            onAdjustTP = { ticket, newTP -> viewModel.adjustTakeProfit(ticket, newTP) },
            onTradeClick = { viewModel.updateSelectedSymbol(it.symbol) }
        )

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 4. Trade History - Recent executions
        TradeHistoryPanel(history = viewModel.closedPositions)
    }
}

private fun executeSignalTrade(viewModel: DashboardViewModel) {
    val price = viewModel.currentPrice?.bid ?: return
    val isBuy = true // ASC signal direction logic here
    val entryPrice = price
    val sl = entryPrice * 0.995
    val tp = entryPrice * 1.01
    val pnl = tp - entryPrice
    viewModel.saveTrade(TradeEntity(
        asset = viewModel.selectedSymbol,
        regimeStack = "AI_AUTO",
        direction = if (isBuy) "LONG" else "SHORT",
        entryPrice = entryPrice,
        exitPrice = tp,
        pnl = pnl,
        win = true,
        entryVolatility = 0.0,
        entryCorrelation = 0.0,
        timestamp = System.currentTimeMillis()
    ))
}

@Composable
private fun ManualTradeEntryForm(onClose: () -> Unit, viewModel: DashboardViewModel) {
    var entry by remember { mutableStateOf("") }
    var stopLoss by remember { mutableStateOf("") }
    var takeProfit by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("BUY") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "MANUAL ORDER ENTRY",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Direction selector
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BUY" to Color(0xFF00C853), "SELL" to Color(0xFFFF6B6B)).forEach { (dir, col) ->
                Surface(
                    onClick = { direction = dir },
                    modifier = Modifier.weight(1f).height(40.dp),
                    color = if (direction == dir) col.copy(alpha = 0.2f) else Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (direction == dir) col else Color(0xFF333333))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(dir, color = if (direction == dir) col else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Entry price input
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Entry:", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterVertically).width(60.dp))
            androidx.compose.material3.TextField(
                value = entry,
                onValueChange = { entry = it },
                modifier = Modifier.weight(1f).height(40.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                placeholder = { Text("Market price", color = Color(0xFF888888)) },
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Stop Loss input
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("S/L:", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterVertically).width(60.dp))
            androidx.compose.material3.TextField(
                value = stopLoss,
                onValueChange = { stopLoss = it },
                modifier = Modifier.weight(1f).height(40.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                placeholder = { Text("Stop loss", color = Color(0xFF888888)) },
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Take Profit input
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("T/P:", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterVertically).width(60.dp))
            androidx.compose.material3.TextField(
                value = takeProfit,
                onValueChange = { takeProfit = it },
                modifier = Modifier.weight(1f).height(40.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                placeholder = { Text("Take profit", color = Color(0xFF888888)) },
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF434651)),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    val entryPrice = entry.toDoubleOrNull() ?: return@Button
                    val sl = stopLoss.toDoubleOrNull() ?: return@Button
                    val tp = takeProfit.toDoubleOrNull() ?: return@Button
                    val isBuy = direction == "BUY"
                    val pnl = if (isBuy) tp - entryPrice else entryPrice - tp
                    val win = pnl > 0
                    viewModel.saveTrade(TradeEntity(
                        asset = viewModel.selectedSymbol,
                        regimeStack = "MANUAL",
                        direction = if (isBuy) "LONG" else "SHORT",
                        entryPrice = entryPrice,
                        exitPrice = if (win) tp else sl,
                        pnl = pnl,
                        win = win,
                        entryVolatility = 0.0,
                        entryCorrelation = 0.0,
                        timestamp = System.currentTimeMillis()
                    ))
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("SUBMIT ORDER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
