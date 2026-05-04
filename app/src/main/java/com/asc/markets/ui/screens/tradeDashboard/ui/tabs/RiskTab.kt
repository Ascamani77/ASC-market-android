package com.asc.markets.ui.screens.tradeDashboard.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.*
import com.asc.markets.ui.screens.tradeDashboard.ui.components.*
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel

/**
 * RISK TAB - "What risk should I take?"
 * Shows: Risk Warning Banner, Open Positions with Health Scores, Position Sizing
 */
@Composable
fun RiskTab(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val dividerColor = Color(0xFF151515)

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
                text = "RISK ASSESSMENT",
                color = Color(0xFFFF6B6B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 1. Risk Warning Banner - Critical alerts
        RiskWarningBanner(
            atRiskPositions = viewModel.positions.filter { it.healthScore < 50 }.map {
                RiskInfo(
                    it,
                    "CRITICAL DRAWDOWN",
                    viewModel.advisory ?: AIAdvisory(Bias.NEUTRAL, 0, 0.0, 0.0, RiskLevel.LOW)
                )
            }
        )

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 2. Open Positions Table - Current risk exposure
        PositionsTable(
            positions = viewModel.positions,
            selectedSymbol = viewModel.selectedSymbol,
            onAdjustSL = { ticket, newSL -> viewModel.adjustStopLoss(ticket, newSL) },
            onAdjustTP = { ticket, newTP -> viewModel.adjustTakeProfit(ticket, newTP) },
            onTradeClick = { viewModel.updateSelectedSymbol(it.symbol) }
        )

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 3. Portfolio Risk Overview
        viewModel.accountInfo?.let {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PORTFOLIO RISK METRICS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Max Risk:", color = Color.Gray, fontSize = 9.sp)
                        Text(
                            "${(it.balance * 0.02).toInt()} USD",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Exposure:", color = Color.Gray, fontSize = 9.sp)
                        Text(
                            "${viewModel.positions.sumOf { p -> p.volume * p.openPrice }.toInt()} USD",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Portfolio Heat:", color = Color.Gray, fontSize = 9.sp)
                        Text(
                            "${(viewModel.positions.size * 100 / 10)}%",
                            color = if (viewModel.positions.size > 5) Color(0xFFFF6B6B) else Color(0xFF00C853),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
