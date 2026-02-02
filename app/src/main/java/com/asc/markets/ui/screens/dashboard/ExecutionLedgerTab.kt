package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.autoMirrored.outlined.Info
import androidx.compose.material.icons.autoMirrored.outlined.History
import androidx.compose.material.icons.autoMirrored.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ExecutionLedgerTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // Ledger Header
        item {
            InfoBox(minHeight = 140.dp) {
                Box(modifier = Modifier.padding(24.dp)) {
                    Icon(
                        History, null,
                        tint = Color.White.copy(alpha = 0.02f),
                        modifier = Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(Shield, null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("REAL-TIME NODE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("AUTONOMOUS AUDIT STREAM", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        // Immutable Trade Cards Parity
        items(MOCK_TRADES) { trade ->
            LedgerTradeCard(trade)
        }

        // Disclosure Parity
        item {
            Surface(
                color = IndigoAccent.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Icon(Info, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "All ledger entries are immutable. This audit trail is provided for diagnostic review ensuring the engine adheres to policy-driven parameters.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerTradeCard(trade: AutomatedTrade) {
    val isWon = trade.status == "WON"
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
                    color = if (isWon) EmeraldSuccess.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isWon) EmeraldSuccess.copy(alpha = 0.1f) else HairlineBorder)
                ) {
                    Text(trade.status, color = if (isWon) EmeraldSuccess else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Institutional Reasoning Block Parity
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

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContextMiniBox("PRE-TRADE CONTEXT", trade.preTradeContext, Modifier.weight(1f))
                ContextMiniBox("OUTCOME PROFILE", trade.postTradeOutcome, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("REALIZED DELTA: ${trade.pnl}", color = if (isWon) EmeraldSuccess else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
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
