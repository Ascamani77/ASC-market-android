package com.asc.markets.ui.screens.tradeDashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.asc.markets.ui.screens.tradeDashboard.model.*
import com.asc.markets.ui.screens.tradeDashboard.ui.components.*
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.*
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel

@Composable
fun TradeDashboardApp(
    viewModel: DashboardViewModel = remember { 
        DashboardViewModel() 
    },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Use ViewModel state directly
    val displayAccount = viewModel.accountInfo
    val displayPositions = viewModel.positions
    val displayPrice = viewModel.currentPrice
    val displayCandles = viewModel.candleData
    val displayAlerts = viewModel.alerts
    val displayAdvisory = viewModel.advisory
    val displayIntel = viewModel.marketIntel
    val displayCalendar = viewModel.calendarEvents
    val displayHistory = viewModel.closedPositions

    var isSettingsDialogOpen by remember { mutableStateOf(false) }
    val dividerColor = Color(0xFF151515)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (viewModel.isLoading && displayAccount == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00C853))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                DashboardHeader(
                    symbol = viewModel.selectedSymbol,
                    isConnected = true
                )

                HorizontalDivider(color = dividerColor, thickness = 1.dp)

                // Error Message
                viewModel.errorMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF43F5E).copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(text = msg, color = Color(0xFFF43F5E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                RiskWarningBanner(
                    atRiskPositions = displayPositions.filter { it.healthScore < 50 }.map { 
                        RiskInfo(it, "CRITICAL DRAWDOWN", displayAdvisory ?: AIAdvisory(Bias.NEUTRAL, 0, 0.0, 0.0, RiskLevel.LOW))
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // 1. Chart Section
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
                            data = displayCandles,
                            symbol = viewModel.selectedSymbol,
                            timeframe = viewModel.selectedTimeframe,
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            onTimeframeChange = { viewModel.onTimeframeSelected(it) }
                        )
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 2. AI Watch Section
                    AIWatchPanel(alerts = displayAlerts, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 3. Custom Monitors Section
                    CustomAlertsManager(
                        symbol = viewModel.selectedSymbol,
                        alerts = emptyList(),
                        onAddAlert = { alert -> /* handle new CustomAlert */ },
                        onRemoveAlert = { /* handle remove */ }
                    )

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 4. Open Positions Section
                    PositionsTable(
                        positions = displayPositions,
                        selectedSymbol = viewModel.selectedSymbol,
                        onAdjustSL = { ticket, newSL -> viewModel.adjustStopLoss(ticket, newSL) },
                        onAdjustTP = { ticket, newTP -> viewModel.adjustTakeProfit(ticket, newTP) },
                        onTradeClick = { viewModel.updateSelectedSymbol(it.symbol) }
                    )

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 5. Live Quote Section
                    displayPrice?.let { PriceCard(price = it) }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 6. Account Overview Section
                    displayAccount?.let { AccountSummary(account = it) }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 7. Market Intelligence Section
                    displayIntel?.let { AIMarketIntelligence(intel = it) }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 8. AI Advisory Section
                    displayAdvisory?.let { AIAdvisoryPanel(advisory = it) }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 9. AI Settings Section
                    AISettingsPanel(
                        settings = viewModel.aiSettings,
                        onSettingsChanged = { viewModel.updateAISettings(it) },
                        onOpenSettings = { isSettingsDialogOpen = true }
                    )

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 10. Economic Calendar Section
                    EconomicCalendar(events = displayCalendar)

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 11. History Section
                    TradeHistoryPanel(history = displayHistory)
                }
                
                // Status Bar Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF1A1A1A))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("SERVER: LD4-PROD-01", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("LATENCY: 12ms", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("UTC: 09:31:40", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("SYSTEMS NOMINAL", color = Color(0xFF00C853).copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    AISettingsDialog(
        isOpen = isSettingsDialogOpen,
        onClose = { isSettingsDialogOpen = false },
        settings = viewModel.aiSettings,
        onSettingsChanged = { viewModel.updateAISettings(it) }
    )
}
