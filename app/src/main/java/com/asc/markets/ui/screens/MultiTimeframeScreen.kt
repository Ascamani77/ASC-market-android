package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun MultiTimeframeScreen(symbol: String) {
    val tfs = listOf("D1", "H4", "H1", "M30", "M15", "M5")
    
    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("MTF ANALYSIS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text("FRACTAL STRUCTURAL SYNC: $symbol", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        modifier = Modifier.height(200.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tf, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("BULLISH", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            Spacer(modifier = Modifier.weight(1f))
            MiniChart(
                values = List(40) {
                    // simple mock series per timeframe — small random walk
                    1.0 + (it * 0.01) + (kotlin.random.Random.nextDouble(-0.02, 0.02))
                },
                modifier = Modifier.fillMaxWidth().height(60.dp)
            )
        }
    }
}