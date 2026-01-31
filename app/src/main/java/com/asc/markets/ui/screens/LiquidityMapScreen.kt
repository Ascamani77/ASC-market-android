package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun LiquidityMapScreen() {
    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("LIQUIDITY MAP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("CROSS-ASSET CORRELATION", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val matrix = listOf("EUR/USD" to 0.82, "USD/JPY" to -0.74, "XAU/USD" to 0.12, "BTC/USD" to 0.45)
            items(matrix) { (pair, coeff) ->
                CorrelationUnit(pair, coeff)
            }
        }
    }
}

@Composable
fun CorrelationUnit(pair: String, coeff: Double) {
    Surface(
        color = Color.Black,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(pair, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(coeff.toString(), color = if (coeff > 0.5) EmeraldSuccess else IndigoAccent, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}