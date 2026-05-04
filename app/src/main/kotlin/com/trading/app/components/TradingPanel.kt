package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TradingPanel(
    activeTab: String,
    onTabChange: (String) -> Unit,
    analysisContent: String,
    isAnalyzing: Boolean,
    onRefreshAnalysis: () -> Unit,
    onClose: () -> Unit,
    backgroundColor: Color = Color(0xFF131722)
) {
    val tabs = listOf("Stock Screener", "Pine Editor", "Strategy Tester", "Trading Panel", "AI Analysis")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(backgroundColor)
            .padding(top = 1.dp)
    ) {
        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(horizontal = 0.dp), // Changed from 8.dp to 0.dp to let first item content be 16.dp from edge
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = activeTab == tab
                Box(
                    modifier = Modifier
                        .clickable { onTabChange(tab) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .drawBehind {
                            if (isActive) {
                                drawLine(
                                    color = Color(0xFF2962FF),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isActive) Color.White else Color(0xFF787B86),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.Close,
                null,
                tint = Color(0xFF787B86),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClose() }
                    .padding(4.dp)
            )
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                "AI Analysis" -> {
                    AISentiment(
                        analysisContent = analysisContent,
                        isAnalyzing = isAnalyzing,
                        onRefreshAnalysis = onRefreshAnalysis
                    )
                }
                "Trading Panel" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inbox, null, tint = Color(0xFF363A45), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No open positions", color = Color(0xFF787B86), fontSize = 14.sp)
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No data available for $activeTab", color = Color(0xFF787B86), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
