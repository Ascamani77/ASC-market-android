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
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel

/**
 * MARKET TAB - "What is the market doing?"
 * Shows: Chart, Market Intelligence, Economic Calendar, Account Health
 */
@Composable
fun MarketTab(
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
        // 1. Live Market Visualization
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE MARKET VISUALIZATION",
                    color = Color(0xFF00C853),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            CandlestickChart(
                data = viewModel.candleData,
                symbol = viewModel.selectedSymbol,
                timeframe = viewModel.selectedTimeframe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                onTimeframeChange = { viewModel.onTimeframeSelected(it) }
            )
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 2. Live Quote
        viewModel.currentPrice?.let { PriceCard(price = it) }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 3. Account Overview
        viewModel.accountInfo?.let { AccountSummary(account = it) }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 4. Market Intelligence
        viewModel.marketIntel?.let { AIMarketIntelligence(intel = it) }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 5. Economic Calendar
        EconomicCalendar(events = viewModel.calendarEvents)
    }
}
