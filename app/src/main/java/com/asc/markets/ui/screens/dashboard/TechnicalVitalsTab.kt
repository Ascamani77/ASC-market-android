package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.CandlestickChart
import com.asc.markets.ui.theme.*

@Composable
fun TechnicalVitalsTab() {
    val mockPriceData = listOf(1.0840, 1.0845, 1.0842, 1.0850, 1.0848, 1.0855, 1.0860, 1.0852)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            InfoBox(height = 200.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("INTRADAY STRUCTURE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    CandlestickChart(mockPriceData)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalsBox("SPREAD", "0.4", EmeraldSuccess, Modifier.weight(1f))
                VitalsBox("VOLATILITY", "HIGH", RoseError, Modifier.weight(1f))
            }
        }
        item {
            MarketDepthLadder()
        }
    }
}

@Composable
fun MarketDepthLadder() {
    InfoBox {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("MARKET DEPTH (L2)", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            repeat(5) { i ->
                DepthRow(price = "1.084${50-i}", size = "${100 + i * 20}", color = RoseError.copy(alpha = 0.6f - (i * 0.1f)))
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)).padding(vertical = 4.dp))
            repeat(5) { i ->
                DepthRow(price = "1.084${40-i}", size = "${150 - i * 15}", color = EmeraldSuccess.copy(alpha = 0.1f + (i * 0.1f)))
            }
        }
    }
}

@Composable
private fun DepthRow(price: String, size: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(size, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(40.dp))
        Box(modifier = Modifier.weight(1f).height(16.dp).background(color, RoundedCornerShape(2.dp)))
        Text(price, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
private fun VitalsBox(label: String, value: String, color: Color, modifier: Modifier) {
    InfoBox(modifier = modifier, height = 100.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}