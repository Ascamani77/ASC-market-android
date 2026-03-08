package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.risk.DiagnosticsReport

@Composable
fun DiagnosticsPanel(report: DiagnosticsReport, modifier: Modifier = Modifier) {
    val flags = listOf(
        report.winRateDrift,
        report.volatilityDrift,
        report.regimeInstability,
        report.correlationClustering,
        report.tailBreak
    )

    val trueCount = flags.count { it }

    val bgColor = when {
        trueCount >= 2 -> Color(0xFFB00020) // red
        trueCount == 1 -> Color(0xFFFFA000) // yellow
        else -> Color(0xFF1B5E20) // green
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor.copy(alpha = 0.08f),
        border = null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Diagnostics", fontSize = 14.sp, color = Color.White)
                // warning badge
                if (trueCount > 0) {
                    Text("Warnings: $trueCount", color = Color.White)
                } else {
                    Text("Healthy", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simple metric rows
            MetricRow("Win rate drift", report.winRateDrift)
            MetricRow("Volatility drift", report.volatilityDrift)
            MetricRow("Regime instability", report.regimeInstability)
            MetricRow("Correlation clustering", report.correlationClustering)
            MetricRow("Tail break", report.tailBreak)

            if (report.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes:", color = Color.White, fontSize = 12.sp)
                for (n in report.notes) {
                    Text("• $n", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, flag: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 12.sp)
        val color = if (flag) Color(0xFFFFC107) else Color(0xFF4CAF50)
        Surface(shape = RoundedCornerShape(12.dp), color = color) {
            Text(if (flag) "YES" else "NO", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.Black, fontSize = 12.sp)
        }
    }
}
