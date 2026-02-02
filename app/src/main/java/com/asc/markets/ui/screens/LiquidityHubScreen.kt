package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.asc.markets.ui.theme.*

@Composable
fun LiquidityHubScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text("LIQUIDITY MAP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text("CROSS-ASSET CORRELATION MATRIX", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
        }
        
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(260.dp)
            ) {
                val matrix = listOf(
                    "EUR/USD" to 0.82, "GBP/USD" to 0.74, "USD/JPY" to -0.74,
                    "XAU/USD" to 0.12, "BTC/USD" to 0.45, "NAS100" to 0.91,
                    "US30" to 0.88, "ETH/USD" to 0.76, "UKOIL" to 0.32
                )
                items(matrix) { (pair, coeff) ->
                    CorrelationHeatmapCard(pair, coeff)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("NET CURRENCY DELTA", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                listOf("USD" to -452000, "EUR" to 284000, "GBP" to 142000).forEach { (curr, delta) ->
                    NetDeltaRow(curr, delta)
                }
            }
        }

        item {
            InfoBox {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("INSTITUTIONAL DISCLOSURE", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Risk Hub coefficients are calculated using 1,000 bar rolling windows. Correlation does not imply causation. Net Delta reflects theoretical units across the node.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun NetDeltaRow(currency: String, delta: Int) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(GhostWhite, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text(currency.take(1), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(currency, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = InterFontFamily)
            }
            Text(
                text = "${if (delta >= 0) "+" else ""}${delta}",
                color = if (delta >= 0) EmeraldSuccess else RoseError,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun CorrelationHeatmapCard(pair: String, coeff: Double) {
    val backgroundColor = when {
        coeff > 0.8 -> IndigoAccent.copy(alpha = 0.6f)
        coeff > 0.4 -> IndigoAccent.copy(alpha = 0.25f)
        coeff < -0.8 -> RoseError.copy(alpha = 0.6f)
        coeff < -0.4 -> RoseError.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(pair, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(
                text = String.format(java.util.Locale.US, "%.2f", coeff),
                color = Color.White,
                fontSize = 18.sp, 
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}