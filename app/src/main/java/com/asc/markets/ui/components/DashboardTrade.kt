package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AutomatedTrade
import com.asc.markets.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke

/**
 * DashboardTrade - High-fidelity audit row component.
 * Left: Financials | Center: Intelligence Rationale | Right: Transmission Metadata
 */
@Composable
fun DashboardTrade(trade: AutomatedTrade, onGenerateCompliance: (AutomatedTrade) -> Unit) {
    // keep status checks inline where needed

    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            // parse numeric fields safely
            fun parsePrice(p: String?): Double? = p?.replace(",", "")?.toDoubleOrNull()
            val entry = parsePrice(trade.entryPrice)
            val exit = parsePrice(trade.exitPrice)
            val slippage = if (entry != null && exit != null) kotlin.math.abs(entry - exit) else null
            val efficiency = if (trade.pnlAmount != null && entry != null && entry != 0.0) (trade.pnlAmount / entry) * 100.0 else null

            // Top: Pair (left) with BUY/SELL audit beneath, TX id at right
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PairFlags(trade.pair, 36)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(trade.pair, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (trade.side == "BUY") "BUY AUDIT" else "SELL AUDIT", color = if (trade.side == "BUY") EmeraldSuccess else RoseError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(trade.relayId, color = SlateText, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slippage and Efficiency rows (label left, value right)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SLIPPAGE AUDIT", color = SlateText, fontSize = 12.sp)
                Text(slippage?.let { String.format("%.2f Pips", it) } ?: "N/A", color = if (slippage != null && slippage > 0.0) RoseError else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("EFFICIENCY", color = SlateText, fontSize = 12.sp)
                Text(efficiency?.let { String.format("%.1f%%", it) } ?: "N/A", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Centered, wide audit button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = { onGenerateCompliance(trade) }, modifier = Modifier.fillMaxWidth(0.9f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painter = painterResource(id = com.asc.markets.R.drawable.lucide_book_open), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("VIEW FULL QUALITY AUDIT")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Spacer(modifier = Modifier.height(16.dp))

            // Post-Execution Analytics header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = com.asc.markets.R.drawable.lucide_binary), contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("POST-EXECUTION ANALYTICS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reasoning quote box
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = trade.reasoning.takeIf { it.isNotBlank() } ?: "No post-execution commentary.",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini cards: Signal Latency + Execution Fill
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("SIGNAL LATENCY", color = SlateText, fontSize = 12.sp)
                        Text(String.format("%.0fms RESPONSE", trade.latencyMs), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("EXECUTION FILL", color = SlateText, fontSize = 12.sp)
                        Text(if (trade.status == "WON") "ST_FILL OK" else "ST_FILL CHECK", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fill Venue and audit badge
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("FILL VENUE", color = SlateText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("NY4-EQUINIX", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = SlateText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("09:50", color = SlateText, fontSize = 12.sp)
                        }
                    }

                    Surface(color = EmeraldSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Text("AUDIT PASSED", color = EmeraldSuccess, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}
