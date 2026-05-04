package com.asc.markets.ui.screens.tradeDashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.asc.markets.ui.screens.tradeDashboard.ui.tabs.*
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.*
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel

@Composable
fun TradeDashboardApp(
    viewModel: DashboardViewModel = remember { 
        DashboardViewModel() 
    },
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isSettingsDialogOpen by remember { mutableStateOf(false) }

    val tabs = listOf("Market", "Opportunity", "Risk", "Execute", "Explain")
    val tabColors = listOf(
        Color(0xFF00C853),      // Market - Green
        Color(0xFF6366F1),      // Opportunity - Indigo
        Color(0xFFFF6B6B),      // Risk - Red
        Color(0xFFFAA61A),      // Execute - Amber
        Color(0xFF6366F1)       // Explain - Indigo
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (viewModel.isLoading && viewModel.accountInfo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00C853))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                DashboardHeader(
                    symbol = viewModel.selectedSymbol,
                    isConnected = true
                )

                HorizontalDivider(color = Color(0xFF151515), thickness = 1.dp)

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

                // Tab Navigation
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .height(48.dp),
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    divider = { HorizontalDivider(color = Color(0xFF151515), thickness = 1.dp) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.background(Color.Black),
                            text = {
                                Text(
                                    text = title.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) tabColors[index] else Color.Gray
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF151515), thickness = 1.dp)

                // Tab Content
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> MarketTab(viewModel, Modifier.fillMaxSize())
                        1 -> OpportunityTab(viewModel, Modifier.fillMaxSize())
                        2 -> RiskTab(viewModel, Modifier.fillMaxSize())
                        3 -> ExecutionTab(viewModel, Modifier.fillMaxSize())
                        4 -> ExplanationTab(viewModel, Modifier.fillMaxSize())
                    }
                }

                // Status Bar Footer
                HorizontalDivider(color = Color(0xFF151515), thickness = 1.dp)
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
