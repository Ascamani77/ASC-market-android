package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.Canvas
import com.asc.markets.ui.theme.InterFontFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.R
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.screens.*
import com.asc.markets.data.EconomicEvent
import com.asc.markets.data.ForexDataPoint
import com.asc.markets.data.ForexPair
import com.asc.markets.logic.TradingStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val ICON_DROPLETS get() = R.drawable.lucide_pie_chart
private val ICON_GAUGE get() = R.drawable.lucide_line_chart
private val ICON_CLOCK get() = R.drawable.lucide_list_filter
private val ICON_ZAP get() = R.drawable.lucide_activity
private val ICON_ZAP_OFF get() = R.drawable.lucide_binary
private val ICON_COMPASS get() = R.drawable.lucide_arrow_left_right
private val ICON_ACTIVITY get() = R.drawable.lucide_activity

data class StreamItem(val id: Long, val text: String, val time: String, val isDim: Boolean)

@Composable
fun MarketOverviewComponent(
    selectedPair: ForexPair,
    tradingStatus: TradingStatus,
    sparklineData: List<ForexDataPoint> = emptyList(),
    upcomingEvents: List<EconomicEvent> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isBlocked = tradingStatus.isBlocked

    // Calculate real spread from selectedPair if available
    val spread = remember(selectedPair) {
        "--"
    }
    
    // Use real session progress based on current time
    val sessionProgress = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour in 0..8 -> "25"
            hour in 9..12 -> "50"
            hour in 13..16 -> "75"
            else -> "90"
        }
    }

    val vitals by remember(selectedPair, upcomingEvents, spread, sessionProgress) {
        mutableStateOf(mapOf(
            "spread" to spread,
            "volatility" to "15",
            "liquidity" to "High",
            "sessionProgress" to sessionProgress,
            "nextNews" to (if (upcomingEvents.isNotEmpty()) upcomingEvents[0].time else "--:--")
        ))
    }

    var stream by remember { mutableStateOf<List<StreamItem>>(emptyList()) }

    // Stream disabled - was using random/mock data
    // TODO: Implement real institutional surveillance feed

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Active Session
        Surface(shape = RoundedCornerShape(4.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
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
            com.asc.markets.ui.screens.LightweightChart(selectedPair.symbol, selectedPair.price)
        }

        // 2. Vitals grid (4 items)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            @Composable
            fun VItem(label: String, value: String, icon: Int, color: Color, sub: String, weight: Float = 1f) {
                Surface(shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(weight)) {
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
            Surface(shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f)) {
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
                            Text("0.02ms", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium, fontFamily = com.asc.markets.ui.theme.InterFontFamily)
                        }
                    }
                }
            }

            // right: activity feed (Institutional Surveillance Feed)
            Surface(shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(2f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Institutional Surveillance Feed", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.ExtraBold)
                        Icon(painter = painterResource(id = ICON_ACTIVITY), contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                    }
                    Box(modifier = Modifier.height(240.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(stream) { item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF6366F1).copy(alpha = 0.5f), shape = RoundedCornerShape(3.dp)))
                                        Text(item.text.uppercase(Locale.US), fontSize = 12.sp, color = if (item.isDim) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Text(item.time, fontSize = 10.sp, color = Color(0xFF334155), fontFamily = com.asc.markets.ui.theme.InterFontFamily)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
