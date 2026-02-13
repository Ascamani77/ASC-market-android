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
import com.asc.markets.logic.CalendarService
import com.asc.markets.ui.theme.*

@Composable
fun FundamentalScreen() {
    val events = listOf(
        EventDisplay("NFP", "USD", 275.0, 200.0, 229.0, "HIGH"),
        EventDisplay("CPI", "USD", 3.2, 3.1, 3.1, "HIGH"),
        EventDisplay("GDP", "EUR", 0.1, 0.1, -0.3, "MEDIUM")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("MACRO INTEL", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("FUNDAMENTAL MAGNITUDE ENGINE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
        }

        items(events) { event ->
            val surprise = CalendarService.calculateSurprise(event.actual, event.estimate)
            NewsEventCard(event, surprise)
        }
    }
}

data class EventDisplay(
    val title: String, 
    val currency: String, 
    val actual: Double, 
    val estimate: Double, 
    val previous: Double,
    val impact: String
)

@Composable
fun NewsEventCard(event: EventDisplay, surprise: com.asc.markets.logic.SurpriseMetadata?) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text(event.currency, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Text(event.title, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 12.dp))
                Spacer(modifier = Modifier.weight(1f))
                if (surprise?.detected == true) {
                    val color = if (surprise.level == "high") RoseError else IndigoAccent
                    Text(surprise.level.uppercase() + " SURPRISE", color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricColumn("ACTUAL", event.actual.toString(), if (surprise?.detected == true) IndigoAccent else Color.White)
                MetricColumn("ESTIMATE", event.estimate.toString(), Color.Gray)
                MetricColumn("PREVIOUS", event.previous.toString(), Color.Gray)
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, color: Color) {
    Column {
        Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}