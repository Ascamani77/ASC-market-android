package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun MtfAnalysisView(viewModel: ForexViewModel) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    val scrollState = rememberScrollState()
    val tfs = listOf("D1", "H4", "H1", "M30", "M15", "M5")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("MTF ANALYSIS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text("FRACTAL STRUCTURAL SYNC: ${selectedPair.symbol}", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, letterSpacing = 1.sp)
        
        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in tfs.indices step 2) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MtfPane(tfs[i], Modifier.weight(1f))
                    if (i + 1 < tfs.size) {
                        MtfPane(tfs[i + 1], Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        InfoBox(minHeight = 200.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("AGGREGATED NODE CONTEXT", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                Text("BULLISH NARRATIVE", color = EmeraldSuccess, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(top = 4.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(48.dp).background(EmeraldSuccess.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("⚡", fontSize = 20.sp)
                    }
                    Text(
                        "Internal fractal analysis confirms structural alignment on lower timeframes with the institutional H4 bias. Deterministic rule-sets favor buy-side delivery.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun MtfPane(tf: String, modifier: Modifier) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.height(180.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tf, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("BULLISH", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)).alpha(0.1f),
                contentAlignment = Alignment.Center
            ) {
                Text("NODE_TRACE_$tf", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}