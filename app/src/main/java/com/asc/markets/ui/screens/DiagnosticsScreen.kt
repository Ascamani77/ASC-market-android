package com.asc.markets.ui.screens

import androidx.compose.foundation.background
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
import com.asc.markets.logic.ConnectionState
import com.asc.markets.ui.theme.*

@Composable
fun DiagnosticsScreen() {
    val health by HealthMonitor.metrics.collectAsState()
    val connState by ConnectivityManager.state.collectAsState()
    val logs by ConnectivityManager.logs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        // Vitals Grid Parity
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiagnosticMetric("LATENCY", "12ms", EmeraldSuccess, Modifier.weight(1f))
            DiagnosticMetric("CLOCK DRIFT", "${health.clockDriftMs}ms", if (health.clockDriftMs > 100) RoseError else IndigoAccent, Modifier.weight(1f))
            DiagnosticMetric("UPTIME", "${health.uptimePercent}%", EmeraldSuccess, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Relay Status Parity
        Surface(
            color = PureBlack,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RELAY NODE STATUS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(if (connState == ConnectionState.LIVE) EmeraldSuccess else RoseError, RoundedCornerShape(4.dp)))
                    Text(connState.name, color = Color.White, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Log Terminal Parity: Matrix Green (#00FF41)
        Text("SYSTEM_EVENT_STREAM", color = Color(0xFF00FF41).copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
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
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun DiagnosticMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}