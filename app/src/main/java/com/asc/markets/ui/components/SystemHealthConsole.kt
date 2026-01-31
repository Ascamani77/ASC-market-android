package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.HealthMonitor
import com.asc.markets.logic.ConnectivityManager
import com.asc.markets.ui.theme.*

@Composable
fun SystemHealthConsole() {
    val metrics by HealthMonitor.metrics.collectAsState()
    val logs by ConnectivityManager.logs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Diagnostic Vitals Parity
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("MEM_USAGE", "${metrics.memoryUsageMb}MB", Modifier.weight(1f))
            MetricBox("CLOCK_DRIFT", "${metrics.clockDriftMs}ms", Modifier.weight(1f))
            MetricBox("UPTIME", "${metrics.uptimePercent}%", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "CONNECTIVITY_LOGS", 
            color = Color(0xFF00FF41).copy(alpha = 0.5f), 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            color = PureBlack,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF41).copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                items(logs) { log ->
                    Text(
                        text = "> $log",
                        color = Color(0xFF00FF41),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}