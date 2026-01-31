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
import com.asc.markets.ui.theme.*

@Composable
fun StrategySignalsTab() {
    val signals = listOf(
        SignalData("EUR/USD", "BUY", 88, "FOCUS"),
        SignalData("GBP/JPY", "SELL", 72, "OBSERVE"),
        SignalData("XAU/USD", "NEUTRAL", 40, "WAIT")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        item {
            InfoBox(minHeight = 100.dp) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("⚡", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("OPPORTUNITY AWARENESS MATRIX", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("STRUCTURED INTELLIGENCE FEED", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        items(signals) { signal ->
            SignalCard(signal)
        }
    }
}

data class SignalData(val pair: String, val dir: String, val conf: Int, val status: String)

@Composable
private fun SignalCard(data: SignalData) {
    // use centralized SignalCardView
    com.asc.markets.ui.components.dashboard.SignalCardView(
        pair = data.pair,
        status = data.status,
        conf = data.conf,
        mainValue = "1.08420",
        delta = null,
        entryZone = "1.08420",
        rr = "RR 1:2.4"
    )
}