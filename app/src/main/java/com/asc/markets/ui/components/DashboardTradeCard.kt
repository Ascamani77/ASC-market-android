package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AutomatedTrade
import com.asc.markets.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement

@Composable
fun DashboardTradeCard(trade: AutomatedTrade, onGenerateCompliance: (AutomatedTrade) -> Unit) {
    val isWon = trade.status == "WON"
    InfoBox {
        Column(modifier = Modifier.padding(16.dp)) {
            // three-column layout: Financial | Reasoning | Metadata
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Column A: Financial Verification
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PairFlags(trade.pair, 28)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(trade.pair, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text("#${trade.id}", color = SlateText, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${trade.side} DISPATCH", color = if (trade.side == "BUY") EmeraldSuccess else RoseError, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("REALIZED DELTA", color = SlateText, fontSize = 9.sp)
                    Text(trade.pnl ?: "OPEN", color = if (isWon) EmeraldSuccess else Color.White, fontSize = 20.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Black)

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { onGenerateCompliance(trade) }, colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)) {
                        Text("Generate Compliance Report", color = Color.White)
                    }
                }

                // Column B: Intelligence Rationale
                Column(modifier = Modifier.weight(2f)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.02f),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContextMiniBox("INTERNAL CONTEXT", trade.preTradeContext, Modifier.weight(1f))
                        ContextMiniBox("OUTCOME PROFILE", trade.postTradeOutcome, Modifier.weight(1f))
                    }
                }

                // Column C: Transmission Metadata
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("RELAY: ${trade.relayId}", color = SlateText, fontSize = 10.sp)
                    Text("TS: ${trade.timestamp}", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LATENCY: ${String.format("%.3fms", trade.latencyMs)}", color = Color.DarkGray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        }
    }
}

// ContextMiniBox moved to ui.components.ContextMiniBox
