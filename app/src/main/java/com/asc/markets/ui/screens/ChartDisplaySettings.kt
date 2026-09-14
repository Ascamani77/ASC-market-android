package com.asc.markets.ui.screens
import com.asc.markets.ui.components.AscRollingSpinner

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import kotlinx.coroutines.launch

data class ChartDisplayItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: String
)

@Composable
fun ChartDisplaySettingsScreen(viewModel: ForexViewModel = viewModel()) {
    var displaySettings by remember { 
        mutableStateOf(mapOf(
            // Core Labels
            "regime_label" to true,
            "confidence_label" to true,
            "pattern_info" to false,
            "mtf_alignment" to true,
            "volume_info" to true,
            
            // Institutional Dashboard
            "institutional_dashboard" to true,
            "dispatch_status" to true,
            "timing_convergence" to true,
            "market_pulse" to true,
            "volatility_pulse" to true,
            "confluence_matrix" to true,
            
            // Market Context
            "market_phase_label" to true,
            "session_details" to true,
            "news_label" to false,
            
            // Trading Tools
            "fvg_zones" to true,
            "spread_monitor" to true,
            "risk_reward_calc" to true,
            "daily_pnl" to true,
            "time_filter" to true,
            "win_probability" to true,
            
            // Additional
            "smc_details" to true,
            "momentum_indicator" to true,
            "market_structure" to true,
            "correlation_alert" to false,
            "cross_correlation" to false
        ))
    }
    
    var isSaving by remember { mutableStateOf(false) }
    var lastSaved by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    val displayItems = listOf(
        // Core Labels Category
        ChartDisplayItem(
            "regime_label",
            "Regime Label",
            "Shows BULLISH/BEARISH/RANGE market state",
            Icons.Default.TrendingUp,
            "Core Labels"
        ),
        ChartDisplayItem(
            "confidence_label",
            "Confidence Score",
            "Signal confidence percentage",
            Icons.Default.Psychology,
            "Core Labels"
        ),
        ChartDisplayItem(
            "pattern_info",
            "Pattern Information",
            "Detected chart patterns",
            Icons.Default.AutoGraph,
            "Core Labels"
        ),
        ChartDisplayItem(
            "mtf_alignment",
            "Multi-Timeframe Alignment",
            "Shows alignment across H1/H4/D1",
            Icons.Default.Layers,
            "Core Labels"
        ),
        ChartDisplayItem(
            "volume_info",
            "Volume Information",
            "Volume analysis and trends",
            Icons.Default.BarChart,
            "Core Labels"
        ),
        
        // Institutional Dashboard
        ChartDisplayItem(
            "institutional_dashboard",
            "Institutional Dashboard",
            "Complete 5-feature institutional panel",
            Icons.Default.Dashboard,
            "Institutional"
        ),
        ChartDisplayItem(
            "dispatch_status",
            "Dispatch Status",
            "Smart money accumulation/distribution",
            Icons.Default.Rocket,
            "Institutional"
        ),
        ChartDisplayItem(
            "timing_convergence",
            "Timing Convergence",
            "Multi-timeframe timing alignment",
            Icons.Default.Schedule,
            "Institutional"
        ),
        ChartDisplayItem(
            "market_pulse",
            "Market Pulse",
            "Price compression and expansion",
            Icons.Default.FavoriteBorder,
            "Institutional"
        ),
        ChartDisplayItem(
            "volatility_pulse",
            "Volatility Pulse",
            "Volatility expansion timing",
            Icons.Default.ShowChart,
            "Institutional"
        ),
        ChartDisplayItem(
            "confluence_matrix",
            "Confluence Matrix",
            "7-factor confluence analysis",
            Icons.Default.GridOn,
            "Institutional"
        ),
        
        // Market Context
        ChartDisplayItem(
            "market_phase_label",
            "Market Phase",
            "ACCUMULATION/EXPANSION/DRY ZONE",
            Icons.Default.AccessTime,
            "Market Context"
        ),
        ChartDisplayItem(
            "session_details",
            "Session Details",
            "Trading session information",
            Icons.Default.Public,
            "Market Context"
        ),
        ChartDisplayItem(
            "news_label",
            "News Alerts",
            "Economic news and events",
            Icons.Default.Newspaper,
            "Market Context"
        ),
        
        // Trading Tools
        ChartDisplayItem(
            "fvg_zones",
            "FVG Zones",
            "Fair Value Gap zones (LuxAlgo style)",
            Icons.Default.Splitscreen,
            "Trading Tools"
        ),
        ChartDisplayItem(
            "spread_monitor",
            "Spread Monitor",
            "Current spread with alerts",
            Icons.Default.CompareArrows,
            "Trading Tools"
        ),
        ChartDisplayItem(
            "risk_reward_calc",
            "Risk/Reward Calculator",
            "SL/TP and R:R display",
            Icons.Default.Calculate,
            "Trading Tools"
        ),
        ChartDisplayItem(
            "daily_pnl",
            "Daily P&L Tracker",
            "Today's profit/loss summary",
            Icons.Default.AttachMoney,
            "Trading Tools"
        ),
        ChartDisplayItem(
            "time_filter",
            "Time Filter",
            "Time-based filter with countdown",
            Icons.Default.Timer,
            "Trading Tools"
        ),
        ChartDisplayItem(
            "win_probability",
            "Win Probability",
            "Estimated win probability %",
            Icons.Default.LocalFireDepartment,
            "Trading Tools"
        ),
        
        // Smart Money Concepts
        ChartDisplayItem(
            "smc_details",
            "SMC Details",
            "Smart Money Concepts breakdown",
            Icons.Default.Lightbulb,
            "Smart Money"
        ),
        ChartDisplayItem(
            "momentum_indicator",
            "Momentum Indicator",
            "Momentum strength summary",
            Icons.Default.Speed,
            "Smart Money"
        ),
        ChartDisplayItem(
            "market_structure",
            "Market Structure",
            "HH/HL/LH/LL structure display",
            Icons.Default.AccountTree,
            "Smart Money"
        ),
        ChartDisplayItem(
            "correlation_alert",
            "Correlation Alert",
            "Currency correlation warnings",
            Icons.Default.Warning,
            "Risk Management"
        ),
        ChartDisplayItem(
            "cross_correlation",
            "Cross-Correlation",
            "Cross-pair divergence detector",
            Icons.Default.SwapHoriz,
            "Risk Management"
        )
    )
    
    // Group items by category
    val groupedItems = displayItems.groupBy { it.category }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF18181B),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Chart Display Settings",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Control what shows on your MT5 chart",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            displaySettings = displaySettings.mapValues { true }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All On", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = {
                            displaySettings = displaySettings.mapValues { false }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF27272A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Off", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                // Save settings via API
                                viewModel.saveChartDisplaySettings(displaySettings)
                                lastSaved = "Saved at ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}"
                                isSaving = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            AscRollingSpinner(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply", fontSize = 12.sp)
                    }
                }
                
                lastSaved?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Settings List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedItems.forEach { (category, items) ->
                item {
                    // Category Header
                    Text(
                        text = category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items.forEach { item ->
                    item {
                        DisplaySettingItem(
                            item = item,
                            isEnabled = displaySettings[item.id] ?: false,
                            onToggle = { enabled ->
                                displaySettings = displaySettings + (item.id to enabled)
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Info Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF18181B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How It Works",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "1. Toggle items ON/OFF based on what you want to see\n" +
                                   "2. Tap 'Apply' to save settings\n" +
                                   "3. Settings are sent to your EA\n" +
                                   "4. Chart display updates in real-time\n" +
                                   "5. Your EA must be running for changes to apply",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplaySettingItem(
    item: ChartDisplayItem,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF18181B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isEnabled) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF27272A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!isEnabled) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                color = if (isEnabled) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFF27272A),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (isEnabled) Color(0xFF10B981) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) Color.White else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Toggle Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF27272A)
                )
            )
        }
    }
}
