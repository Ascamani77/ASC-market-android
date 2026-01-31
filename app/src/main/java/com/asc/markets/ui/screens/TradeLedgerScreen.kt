package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.asc.markets.data.MOCK_TRADES
import com.asc.markets.data.AutomatedTrade
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*

@Composable
fun TradeLedgerScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text("REAL-TIME NODE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text("AUTONOMOUS AUDIT STREAM", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(MOCK_TRADES) { trade ->
            LedgerTradeCard(trade)
        }
    }
}

@Composable
fun LedgerTradeCard(trade: AutomatedTrade) {
    InfoBox {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PairFlags(trade.pair, 32)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(trade.pair, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("${trade.side} DISPATCH", color = if (trade.side == "BUY") EmeraldSuccess else RoseError, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
                Surface(
                    color = if (trade.status == "WON") EmeraldSuccess.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (trade.status == "WON") EmeraldSuccess.copy(alpha = 0.1f) else HairlineBorder)
                ) {
                    Text(trade.status, color = if (trade.status == "WON") EmeraldSuccess else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Institutional Reasoning Parity: Indigo border-left
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(IndigoAccent))
                    Text(
                        text = trade.reasoning, 
                        color = Color.White, 
                        fontSize = 13.sp, 
                        lineHeight = 20.sp, 
                        modifier = Modifier.padding(16.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContextMiniBox("INTERNAL CONTEXT", trade.preTradeContext, Modifier.weight(1f))
                ContextMiniBox("OUTCOME PROFILE", trade.postTradeOutcome, Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("REALIZED DELTA: ${trade.pnl}", color = if (trade.status == "WON") EmeraldSuccess else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("LATENCY: 0.02ms", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ContextMiniBox(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(value.uppercase(), color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, lineHeight = 14.sp)
    }
}