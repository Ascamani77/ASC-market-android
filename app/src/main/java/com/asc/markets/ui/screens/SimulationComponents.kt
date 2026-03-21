package com.asc.markets.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MarketTickerHeaderV4() {
    var prices by remember {
        mutableStateOf(
            mapOf(
                "BTC/USD" to 64209.78,
                "ETH/USD" to 3453.76,
                "EUR/USD" to 1.08245,
                "GBP/USD" to 1.2654,
                "GOLD" to 2156.40,
                "OIL" to 78.32
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            prices = prices.mapValues { (_, price) ->
                price * (1.0 + (Random.nextDouble() - 0.5) * 0.001)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        // Live status indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
            Spacer(modifier = Modifier.width(6.dp))
            Text("LIVE", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        
        prices.forEach { (pair, price) ->
            TickerItemV4(pair, String.format("%.3f", price), if (Random.nextBoolean()) Color(0xFF10B981) else Color(0xFFEF4444))
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun TickerItemV4(pair: String, price: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(pair, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(price, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
    }
}

@Composable
fun StatCardV3(label: String, value: String, subValue: String? = null, color: Color, drawdownValue: String? = null, isNegativeTrend: Boolean = false) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                
                if (label == "MAX DRAWDOWN") {
                    Icon(
                        if (isNegativeTrend) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (isNegativeTrend) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                }
            }
            
            if (subValue != null) {
                Text(subValue, color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(30.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    path.moveTo(0f, size.height * 0.5f)
                    path.quadraticBezierTo(size.width * 0.3f, size.height * 0.4f, size.width * 0.5f, size.height * 0.6f)
                    path.quadraticBezierTo(size.width * 0.8f, size.height * 0.3f, size.width, size.height * 0.5f)
                    drawPath(path, color = color, style = Stroke(width = 1.5.dp.toPx()))
                }
                
                if (drawdownValue != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        color = if (isNegativeTrend) Color(0xFF2C0B0B) else Color(0xFF142921),
                        shape = CircleShape
                    ) {
                        Text(
                            drawdownValue, 
                            color = if (isNegativeTrend) Color(0xFFEF4444) else Color(0xFF10B981), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeToggleButtonV2(label: String, isSelected: Boolean, accentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) accentColor else Color(0xFF18181B),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) Color.Black else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LiveAnalysisChartSectionV3(
    accentColor: Color,
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit,
    activeIndicators: Set<String>,
    onIndicatorToggle: (String) -> Unit
) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF142921), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = accentColor, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("MARKET ANALYSIS: BTC/USDT", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timeframe Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1M", "5M", "15M", "1H", "4H", "1D").forEach { tf ->
                    val isSelected = tf == selectedTimeframe
                    Surface(
                        color = if(isSelected) Color(0xFF142921) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        onClick = { onTimeframeChange(tf) }
                    ) {
                        Text(
                            text = tf, 
                            color = if(isSelected) accentColor else Color.Gray, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Indicators Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("INDICATORS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                listOf("MA20", "MA50", "RSI", "MACD").forEach { indicator ->
                    IndicatorTag(
                        label = indicator,
                        color = when(indicator) {
                            "MA20" -> Color(0xFF3B82F6)
                            "MA50" -> Color(0xFFF59E0B)
                            "RSI" -> Color(0xFF10B981)
                            "MACD" -> Color(0xFF6366F1)
                            else -> Color.Gray
                        },
                        isActive = activeIndicators.contains(indicator),
                        onClick = { onIndicatorToggle(indicator) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("CURRENT PRICE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$50,813.099", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(12.dp))
                Surface(color = Color(0xFF142921), shape = RoundedCornerShape(4.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BULLISH", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(Color.Black)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = Color(0xFF1C1C1E)
                    for (i in 0..6) {
                        val y = size.height * (i / 6f)
                        drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y))
                    }
                    
                    if (activeIndicators.contains("MA20")) {
                        val p1 = Path()
                        p1.moveTo(0f, size.height * 0.7f)
                        p1.cubicTo(size.width * 0.3f, size.height * 0.8f, size.width * 0.6f, size.height * 0.2f, size.width, size.height * 0.4f)
                        drawPath(p1, color = Color(0xFF3B82F6), style = Stroke(width = 2.dp.toPx()))
                    }
                    
                    if (activeIndicators.contains("MA50")) {
                        val p2 = Path()
                        p2.moveTo(0f, size.height * 0.75f)
                        p2.cubicTo(size.width * 0.4f, size.height * 0.85f, size.width * 0.7f, size.height * 0.3f, size.width, size.height * 0.45f)
                        drawPath(p2, color = Color(0xFFF59E0B), style = Stroke(width = 2.dp.toPx()))
                    }
                }
                
                // Labels on chart
                Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartLabel("ENTRY", Color(0xFF3B82F6), "51200.00")
                    if (activeIndicators.contains("MA20")) ChartLabel("MA20", Color(0xFF3B82F6), "51156.06")
                    if (activeIndicators.contains("MA50")) ChartLabel("MA50", Color(0xFFF59E0B), "51006.06")
                    ChartLabel("SL", Color(0xFFEF4444), "50800.00")
                }
            }
        }
    }
}

@Composable
fun ChartLabel(label: String, color: Color, value: String) {
    Surface(color = color, shape = RoundedCornerShape(2.dp)) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, color = Color.White, fontSize = 9.sp)
        }
    }
}

@Composable
fun IndicatorTag(label: String, color: Color, isActive: Boolean = true, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = if (isActive) color.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (isActive) color.copy(alpha = 0.4f) else Color(0xFF1C1C1E))
    ) {
        Text(
            text = label,
            color = if (isActive) color else Color.Gray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AIReasoningSectionV4(accentColor: Color) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("AI REASONING & TECHNICAL ANALYSIS", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            listOf(
                "Price is currently testing the 0.618 Fibonacci retracement level after a strong bullish impulse.",
                "RSI is showing hidden bullish divergence on the 15m timeframe, suggesting continuation of the primary trend.",
                "Volume profile shows high volume node (HVN) support at 50,800, which aligns with our Stop Loss placement.",
                "The 20-period EMA has recently crossed above the 50-period EMA, confirming short-term bullish momentum.",
                "Liquidity sweep of the previous day's low was successful, clearing out weak long positions before this move."
            ).forEach { point ->
                Row(modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(modifier = Modifier.padding(top = 6.dp).size(4.dp).clip(CircleShape).background(accentColor))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(point, color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun ManualTradeForm(accentColor: Color, onExecute: (type: String, entry: String, sl: String, tp: String) -> Unit) {
    var type by remember { mutableStateOf("BUY") }
    var entry by remember { mutableStateOf("64209.78") }
    var sl by remember { mutableStateOf("63800.00") }
    var tp by remember { mutableStateOf("65500.00") }

    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("MANUAL TRADE ENTRY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { type = "BUY" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if(type == "BUY") Color(0xFF10B981) else Color(0xFF18181B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("BUY", color = if(type == "BUY") Color.Black else Color.Gray)
                }
                Button(
                    onClick = { type = "SELL" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if(type == "SELL") Color(0xFFEF4444) else Color(0xFF18181B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SELL", color = if(type == "SELL") Color.White else Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TradeInputField("ENTRY", entry, Modifier.weight(1f)) { entry = it }
                TradeInputField("TP", tp, Modifier.weight(1f)) { tp = it }
                TradeInputField("SL", sl, Modifier.weight(1f)) { sl = it }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onExecute(type, entry, sl, tp) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("OPEN SIMULATED POSITION", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TradeInputField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF1C1C1E), RoundedCornerShape(6.dp))
                .padding(8.dp)
        )
    }
}

@Composable
fun EquityCurveSectionV3(accentColor: Color) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Equity Curve", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p1 = Path()
                    p1.moveTo(0f, size.height * 0.9f)
                    p1.cubicTo(size.width * 0.2f, size.height * 0.7f, size.width * 0.6f, size.height * 0.8f, size.width, size.height * 0.2f)
                    drawPath(p1, color = accentColor, style = Stroke(width = 2.dp.toPx()))
                    
                    val p2 = Path()
                    p2.moveTo(0f, size.height * 0.85f)
                    p2.cubicTo(size.width * 0.3f, size.height * 0.9f, size.width * 0.7f, size.height * 0.4f, size.width, size.height * 0.5f)
                    drawPath(p2, color = Color(0xFF3B82F6), style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun ConfidenceCalibrationSectionV3() {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Confidence Calibration", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val orange = Color(0xFFF59E0B)
                    drawCircle(orange, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.7f))
                    drawCircle(orange, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.6f))
                    drawCircle(orange, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.3f))
                    drawCircle(orange, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.2f))
                    
                    drawLine(Color.Gray, start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.8f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.1f), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                }
                
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
                    color = Color(0xFF18181B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("90", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Actual Win Rate : 88", color = Color(0xFFF59E0B), fontSize = 11.sp)
                        Text("Perfect Calibration : 90", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSimulatedTradesSectionV2(accentColor: Color, onNewTrade: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ACTIVE SIMULATED TRADES", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Surface(color = Color(0xFF142921), shape = RoundedCornerShape(4.dp)) {
                Text("0 OPEN", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = onNewTrade,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("NEW TRADE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            listOf("ASSET", "ENTRY", "SL / TP", "SIZE", "TIME").forEach {
                Text(it, modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("No active simulated trades.", color = Color.DarkGray, fontSize = 12.sp)
        }
        HorizontalDivider(color = Color(0xFF1C1C1E))
    }
}

@Composable
fun SimulationControlsSectionV2(engineEnabled: Boolean, onEngineToggle: (Boolean) -> Unit, tradingMode: String, onModeToggle: (String) -> Unit, accentColor: Color) {
    Surface(color = Color(0xFF111111), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF1C1C1E))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("SIMULATION CONTROLS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Engine Status", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Enable AI signal generation", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(checked = engineEnabled, onCheckedChange = onEngineToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("TRADING MODE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TradingModeButton(Icons.Default.Bolt, "Auto Trade", tradingMode == "auto", Modifier.weight(1f)) { onModeToggle("auto") }
                TradingModeButton(Icons.Default.NotificationsNone, "Prompt Trade", tradingMode == "prompt", Modifier.weight(1f), accentColor) { onModeToggle("prompt") }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MARKET REPLAY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Surface(color = Color(0xFF18181B), shape = RoundedCornerShape(4.dp)) {
                    Text("REPLAY OFF", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun RiskManagementSectionV2() {
    Surface(color = Color(0xFF111111), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF1C1C1E))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("RISK MANAGEMENT", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            
            RiskSliderV2("Max Drawdown Limit", "10%", 0.2f, Color(0xFFF43F5E))
            RiskSliderV2("Win Rate Threshold", "40%", 0.4f, Color(0xFFF59E0B), "Triggers kill switch if win rate falls below this after 10 trades.")
            RiskSliderV2("Risk per Trade", "1%", 0.15f, Color(0xFF10B981))
            RiskSliderV2("Lookback Period", "100 Candles", 0.5f, Color(0xFFF59E0B), "Historical context for AI signal generation.")
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Confidence Threshold: ", color = Color.Gray, fontSize = 11.sp)
                Text("85%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RiskSliderV2(label: String, value: String, progress: Float, color: Color, hint: String? = null) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (hint != null) {
            Text(hint, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(1.dp))) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(color, RoundedCornerShape(1.dp)))
            Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = (300 * progress).dp).size(10.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
fun TradingModeButton(icon: ImageVector, label: String, isSelected: Boolean, modifier: Modifier = Modifier, accentColor: Color = Color(0xFF10B981), onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color.Transparent else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0xFF1C1C1E)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isSelected) accentColor else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecentHistorySection(onViewAll: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("RECENT HISTORY", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "VIEW ALL", 
                color = Color(0xFF10B981), 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("No recent trades.", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}
