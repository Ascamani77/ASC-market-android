package com.asc.markets.ui.screens.tradeDashboard.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.*
import com.asc.markets.ui.screens.tradeDashboard.ui.components.*
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel

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
                    onClick = { /* Execute ASC recommendation */ },
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
            ManualTradeEntryForm(onClose = { manualTradeEntry = false })
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

@Composable
private fun ManualTradeEntryForm(onClose: () -> Unit) {
    var entry by remember { mutableStateOf("") }
    var stopLoss by remember { mutableStateOf("") }
    var takeProfit by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "MANUAL ORDER ENTRY",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Simplified form - integrate with real order placement
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entry:", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterVertically))
            Text(entry.ifEmpty { "—" }, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("S/L:", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterVertically))
            Text(stopLoss.ifEmpty { "—" }, color = Color(0xFFFF6B6B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("T/P:", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterVertically))
            Text(takeProfit.ifEmpty { "—" }, color = Color(0xFF00C853), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF434651)),
            modifier = Modifier
                .align(Alignment.End)
                .height(32.dp)
        ) {
            Text("CLOSE", fontSize = 9.sp)
        }
    }
}
