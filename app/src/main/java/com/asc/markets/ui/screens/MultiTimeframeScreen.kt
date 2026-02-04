package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.MiniChart
import kotlin.random.Random

@Composable
fun MultiTimeframeScreen(symbol: String) {
    val tfs = listOf("D1", "H4", "H1", "M30", "M15", "M5")
    
    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Text("MTF ANALYSIS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text("FRACTAL STRUCTURAL SYNC: $symbol", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        
        Spacer(modifier = Modifier.height(24.dp))

        // One box per row, edge-to-edge
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tfs) { tf ->
                MiniChartContainer(tf)
            }
        }
    }
}

@Composable
fun MiniChartContainer(tf: String) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tf, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("BULLISH", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            Spacer(modifier = Modifier.weight(1f))
            MiniChart(
                values = generateSeries(tf),
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }
    }
}

private fun generateSeries(tf: String): List<Double> {
    val points = when (tf) {
        "D1" -> 200
        "H4" -> 150
        "H1" -> 120
        "M30" -> 100
        "M15" -> 80
        "M5" -> 60
        else -> 80
    }

    val base = when (tf) {
        "D1" -> 1.2345
        "H4" -> 1.2000
        "H1" -> 1.2100
        "M30" -> 1.2150
        "M15" -> 1.2175
        "M5" -> 1.2185
        else -> 1.21
    }

    val vol = when (tf) {
        "D1" -> 0.005
        "H4" -> 0.003
        "H1" -> 0.0015
        "M30" -> 0.001
        "M15" -> 0.0007
        "M5" -> 0.0005
        else -> 0.001
    }

    var price = base
    return List(points) {
        // random walk with slight drift
        val change = Random.nextDouble(-vol, vol) + (Random.nextDouble(-vol, vol) * 0.05)
        price = (price + change).coerceAtLeast(0.0001)
        price
    }
}