package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.Trade

@Composable
fun SimulationDashboardContent(
    accentColor: Color,
    engineEnabled: Boolean,
    onEngineToggle: (Boolean) -> Unit,
    tradingMode: String,
    onModeToggle: (String) -> Unit,
    onExecuteManualTrade: (type: String, entry: String, sl: String, tp: String) -> Unit,
    onViewAllHistory: () -> Unit = {}
) {
    var chartMode by remember { mutableStateOf("ai") } // "ai" or "manual"
    var selectedTimeframe by remember { mutableStateOf("15M") }
    var activeIndicators by remember { mutableStateOf(setOf("MA20", "MA50")) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "STRATEGY EVALUATION ENGINE", 
            color = Color.Gray, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Box(modifier = Modifier.size(6.dp).background(accentColor, androidx.compose.foundation.shape.CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REAL-TIME ANALYSIS", 
                color = accentColor, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stat Cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardV3(
                label = "WIN RATE", 
                value = "0.0%", 
                subValue = "0 wins / 0 trades", 
                color = accentColor
            )
            StatCardV3(
                label = "PROFIT FACTOR", 
                value = "0.00", 
                color = Color(0xFF6366F1)
            )
            StatCardV3(
                label = "MAX DRAWDOWN", 
                value = "0.0%", 
                color = Color(0xFFEF4444),
                drawdownValue = "0%",
                isNegativeTrend = true
            )
            StatCardV3(
                label = "SHARPE RATIO", 
                value = "0.00", 
                color = Color(0xFFF59E0B)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Simulation Mode Toggle (AI vs Manual)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF18181B), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            ModeToggleButtonV2("AI Simulation", chartMode == "ai", accentColor, Modifier.weight(1f)) { chartMode = "ai" }
            ModeToggleButtonV2("My Simulation", chartMode == "manual", accentColor, Modifier.weight(1f)) { chartMode = "manual" }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Shared Analysis Chart
        LiveAnalysisChartSectionV3(
            accentColor = accentColor,
            selectedTimeframe = selectedTimeframe,
            onTimeframeChange = { selectedTimeframe = it },
            activeIndicators = activeIndicators,
            onIndicatorToggle = { indicator ->
                activeIndicators = if (activeIndicators.contains(indicator)) {
                    activeIndicators - indicator
                } else {
                    activeIndicators + indicator
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (chartMode == "ai") {
            // AI Specific Sections
            AIReasoningSectionV4(accentColor)
            Spacer(modifier = Modifier.height(24.dp))
            EquityCurveSectionV3(accentColor)
            Spacer(modifier = Modifier.height(24.dp))
            ConfidenceCalibrationSectionV3()
            Spacer(modifier = Modifier.height(24.dp))
            SimulationControlsSectionV2(
                engineEnabled = engineEnabled,
                onEngineToggle = onEngineToggle,
                tradingMode = tradingMode,
                onModeToggle = onModeToggle,
                accentColor = accentColor
            )
        } else {
            // User Specific Manual Form
            ManualTradeForm(accentColor = accentColor, onExecute = onExecuteManualTrade)
            Spacer(modifier = Modifier.height(24.dp))
            MySimulationDashboard(accentColor)
        }

        Spacer(modifier = Modifier.height(24.dp))
        RiskManagementSectionV2()
        Spacer(modifier = Modifier.height(24.dp))
        RecentHistorySection(onViewAll = onViewAllHistory)
    }
}

@Composable
fun MySimulationDashboard(accentColor: Color) {
    Column {
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF142921), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = accentColor, modifier = Modifier.padding(8.dp).size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("My Simulation Progress", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Simplified Chart for User
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path()
                        path.moveTo(0f, size.height * 0.8f)
                        path.lineTo(size.width * 0.2f, size.height * 0.7f)
                        path.lineTo(size.width * 0.4f, size.height * 0.75f)
                        path.lineTo(size.width * 0.6f, size.height * 0.5f)
                        path.lineTo(size.width * 0.8f, size.height * 0.55f)
                        path.lineTo(size.width, size.height * 0.3f)
                        drawPath(path, color = accentColor, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MY CONTROLS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { /* Export */ },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT PERFORMANCE DATA", color = Color.White, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { /* Reset */ },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C0B0C)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESET SIMULATION", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        }
    }
}
