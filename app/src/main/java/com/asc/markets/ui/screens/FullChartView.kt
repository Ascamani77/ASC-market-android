package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.ui.theme.*

@Composable
fun FullChartView(pair: ForexPair, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        // Header Hub parity
        Surface(
            color = Color(0xFF080808),
            modifier = Modifier.fillMaxWidth().height(72.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("✕", color = Color.White, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(pair.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("NY4 EQUINIX / FEED: TOP-OF-BOOK", color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.weight(1f))
                
                // Tool Hub parity
                Row(modifier = Modifier.background(GhostWhite, RoundedCornerShape(8.dp)).padding(4.dp)) {
                    ChartTool("⚲")
                    ChartTool("━")
                    ChartTool("▱")
                }
            }
        }

        // Chart Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.04f)) {
                Text("ASC", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black)
                Text("market", color = IndigoAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Footer Bar parity
        Surface(
            color = Color(0xFF080808),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("INTERACTIVE LAYER L14", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text("ASC MARKETS TERMINAL CORE", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ChartTool(icon: String) {
    Box(
        modifier = Modifier.size(36.dp).clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, color = Color.Gray, fontSize = 18.sp)
    }
}