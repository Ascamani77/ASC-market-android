package com.asc.markets.ui.screens.dashboard

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
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.EquityCurveGraph
import com.asc.markets.ui.theme.*

@Composable
fun AnalyticalQualityTab() {
    // Delegate to the new DashboardQuality implementation (Institutional Audit Hub)
    DashboardQuality()
}

@Composable
private fun QualityMetricCard(label: String, value: String, sub: String, modifier: Modifier) {
    InfoBox(modifier = modifier, height = 110.dp) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Column {
                Text(value, color = IndigoAccent, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(sub, color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun OutcomeRow(label: String, value: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text("$value%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(1.dp))) {
            Box(modifier = Modifier.fillMaxWidth(value/100f).height(2.dp).background(color, RoundedCornerShape(1.dp)))
        }
    }
}