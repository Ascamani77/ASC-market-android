package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.R
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.screens.LightweightChart
import com.asc.markets.ui.screens.MacroBasketGrid
import com.asc.markets.data.EconomicEvent
import com.asc.markets.data.ForexDataPoint
import com.asc.markets.data.ForexPair
import com.asc.markets.logic.TradingStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val ICON_DROPLETS = R.drawable.lucide_pie_chart
private val ICON_GAUGE = R.drawable.lucide_line_chart
private val ICON_CLOCK = R.drawable.lucide_list_filter
private val ICON_ZAP = R.drawable.lucide_activity
private val ICON_ZAP_OFF = R.drawable.lucide_binary
private val ICON_COMPASS = R.drawable.lucide_arrow_left_right
private val ICON_ACTIVITY = R.drawable.lucide_activity

data class TapeItem(val id: Long, val text: String, val time: String, val isDim: Boolean)

@Composable
fun MarketOverviewComponent(
    selectedPair: ForexPair,
    tradingStatus: TradingStatus,
    sparklineData: List<ForexDataPoint> = emptyList(),
    upcomingEvents: List<EconomicEvent> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isBlocked = tradingStatus.isBlocked

    val vitals by remember(selectedPair, upcomingEvents) {
        mutableStateOf(mapOf(
            "spread" to String.format(Locale.US, "%.1f", (Math.random() * 0.5 + 0.1)),
            "volatility" to ( (Math.random() * 40 + 10).toInt().toString() ),
            "liquidity" to "High",
            "sessionProgress" to "65",
            "nextNews" to (if (upcomingEvents.isNotEmpty()) upcomingEvents[0].time else "--:--")
        ))
    }

    var tape by remember { mutableStateOf<List<TapeItem>>(emptyList()) }

    // periodic tape updates every 5s
    LaunchedEffect(Unit) {
        val actions = listOf(
            "Institutional Buy Program Detected",
            "Internal Range Liquidity Swept",
            "FVG Mitigation in progress",
            "Volume Profile: High Value Area Hold",
            "Order Block Validation: Confirmed",
            "Safety Gate Cleared"
        )
        while (true) {
            delay(5000)
            val text = actions.random()
            val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val item = TapeItem(System.currentTimeMillis(), text, now, Math.random() > 0.5)
            tape = listOf(item) + tape
            if (tape.size > 10) tape = tape.take(10)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Active Session
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ACTIVE SESSION", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("London Hub", color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Text("ACTIVE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.padding(top = 6.dp)) {
                        Text("${vitals["sessionProgress"]}% Complete", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }

                // Circular progress
                val progress = (vitals["sessionProgress"]?.toFloatOrNull() ?: 65f) / 100f
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 8.dp.toPx()
                        val radius = size.minDimension / 2f - stroke / 2f
                        drawCircle(color = Color.White.copy(alpha = 0.03f), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                        drawArc(color = Color(0xFF10B981), startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round))
                    }
                    Text(text = "${(progress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Chart area (LightweightChart)
        InfoBox(height = 350.dp) {
            LightweightChart(selectedPair.symbol, selectedPair.price)
        }

        // 2. Vitals grid (4 items)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            @Composable
            fun VItem(label: String, value: String, icon: Int, color: Color, sub: String, weight: Float = 1f) {
                Surface(shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(weight)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(label.uppercase(Locale.US), fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Black)
                            Icon(painter = painterResource(id = icon), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(value.uppercase(Locale.US), fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text(sub.uppercase(Locale.US), fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            VItem("Avg Spread", "${vitals["spread"]} pips", ICON_DROPLETS, Color(0xFF6D28D9), "Institutional")
            VItem("Volatility", "${vitals["volatility"]} p/h", ICON_GAUGE, Color(0xFFF59E0B), "Standard")
            VItem("Next Event", vitals["nextNews"] ?: "--:--", ICON_CLOCK, Color(0xFFFB7185), "UTC Window")
            VItem("Safety Gate", if (isBlocked) "Locked" else "Armed", if (isBlocked) ICON_ZAP_OFF else ICON_ZAP, if (isBlocked) Color(0xFFFB7185) else Color(0xFF10B981), "Prop Guard")
        }

        // 3. Operational snapshot
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // left
            Surface(shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painter = painterResource(id = ICON_COMPASS), contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                        Text("Global Regime", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Current environment is defined by ", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Node Health", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.ExtraBold)
                            Text("99.9%", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Latency", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.ExtraBold)
                            Text("0.02ms", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
            }

            // right: activity tape
            Surface(shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(2f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Institutional Background Activity", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.ExtraBold)
                        Icon(painter = painterResource(id = ICON_ACTIVITY), contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                    }
                    Box(modifier = Modifier.height(240.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(tape) { item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF6366F1).copy(alpha = 0.5f), shape = RoundedCornerShape(3.dp)))
                                        Text(item.text.uppercase(Locale.US), fontSize = 12.sp, color = if (item.isDim) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Text(item.time, fontSize = 10.sp, color = Color(0xFF334155), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
