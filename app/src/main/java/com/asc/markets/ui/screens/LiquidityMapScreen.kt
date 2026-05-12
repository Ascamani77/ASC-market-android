package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.ui.theme.*
import java.util.Locale

@Composable
fun LiquidityMapScreen() {
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("LIQUIDITY MAP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("PRE-MOVE POOLS AND SWEEP PROBABILITY", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(candidates.take(8)) { candidate ->
                LiquidityCandidateUnit(
                    symbol = candidate.symbol,
                    magnet = candidate.liquidityMagnet,
                    sweep = candidate.sweepProbability,
                    buySide = candidate.buySideLiquidity,
                    sellSide = candidate.sellSideLiquidity
                )
            }
        }
    }
}

@Composable
fun LiquidityCandidateUnit(symbol: String, magnet: String, sweep: Int, buySide: Double, sellSide: Double) {
    Surface(
        color = Color.Black,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.height(170.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(magnet, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("$sweep%", color = if (sweep >= 70) EmeraldSuccess else IndigoAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("SWEEP PROBABILITY", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("BSL ${formatMiniLiquidityPrice(buySide)}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("SSL ${formatMiniLiquidityPrice(sellSide)}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatMiniLiquidityPrice(value: Double): String {
    return when {
        value >= 1000.0 -> String.format(Locale.US, "%,.2f", value)
        value >= 1.0 -> String.format(Locale.US, "%.5f", value)
        else -> String.format(Locale.US, "%.6f", value)
    }
}
