package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.EconomicEvent
import com.asc.markets.ui.screens.tradeDashboard.model.Impact

@Composable
fun EconomicCalendar(events: List<EconomicEvent>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ECONOMIC CALENDAR",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "UTC+0",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Event List
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            events.take(3).forEach { event ->
                EconomicEventCard(event)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Footer
        HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "VIEW FULL CALENDAR",
            color = Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EconomicEventCard(event: EconomicEvent) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080808), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF151515), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Column: Time and Impact
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = event.time,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))
                ImpactBadge(impact = event.impact)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Column: Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.currency,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1A1A1A),
                        thickness = 1.dp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.event,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actual / Forecast / Previous Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    EventDataPoint("ACTUAL", event.actual ?: "—", Modifier.weight(1f))
                    EventDataPoint("FORECAST", event.forecast ?: "—", Modifier.weight(1f))
                    EventDataPoint("PREVIOUS", event.previous ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ImpactBadge(impact: Impact) {
    val (bgColor, textColor) = when (impact) {
        Impact.HIGH -> Color(0xFF331114) to Color(0xFFF43F5E)
        Impact.MEDIUM -> Color(0xFF332205) to Color(0xFFF59E0B)
        Impact.LOW -> Color(0xFF1A1A1A) to Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = impact.name,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun EventDataPoint(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label, 
            color = Color(0xFF404040), 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = Color.Gray,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
