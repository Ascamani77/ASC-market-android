package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.EconomicEvent
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*

@Composable
fun EconomicCalendarScreen() {
    val events = listOf(
        EconomicEvent("Non-Farm Payrolls", "USD", "HIGH", "2024-06-12", "13:30", actual = "275K", estimate = "200K", previous = "229K"),
        EconomicEvent("CPI YoY", "USD", "HIGH", "2024-06-12", "12:30", actual = "3.2%", estimate = "3.1%", previous = "3.1%"),
        EconomicEvent("ECB Rate Decision", "EUR", "HIGH", "2024-06-12", "13:45", actual = "4.50%", estimate = "4.50%", previous = "4.50%")
    )

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("SCHEDULING", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text("HIGH IMPACT WINDOW MONITOR", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Text("WEDNESDAY, 12 JUNE", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(events) { event ->
                EventCard(event)
            }
        }
    }
}

@Composable
fun EventCard(event: EconomicEvent) {
    val isHigh = event.impact == "HIGH"
    
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Severity Stripe Parity
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(if (isHigh) RoseError else Color.DarkGray))
            
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PairFlags(event.currency, 24)
                        Column {
                            Text(event.event, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text("SOURCE: FOREXFACTORY", color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(event.time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(if (isHigh) "CRITICAL" else "MEDIUM", color = if (isHigh) RoseError else SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricColumn("ACTUAL", event.actual ?: "--", if (event.actual != event.estimate) IndigoAccent else Color.White)
                    MetricColumn("ESTIMATE", event.estimate ?: "--", Color.Gray)
                    MetricColumn("PREVIOUS", event.previous ?: "--", Color.DarkGray)
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, color: Color) {
    Column {
        Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}