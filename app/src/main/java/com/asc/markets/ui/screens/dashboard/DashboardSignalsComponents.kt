package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SignalCardItem(signal: Signal, modifier: Modifier = Modifier, onTap: (Signal) -> Unit) {
    val safetyClosed = isSafetyGateClosed()
    val safetyScore = if (safetyClosed) 0 else 100
    val combined = ((signal.technicalScore * 0.6) + (safetyScore * 0.4)).roundToInt()
    val state = when {
        safetyClosed -> "WAIT"
        combined >= 75 -> "FOCUS"
        combined >= 50 -> "OBSERVE"
        else -> "WAIT"
    }

    InfoBox(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().clickable { onTap(signal) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val stateColor = when (state) { "FOCUS" -> IndigoAccent; "OBSERVE" -> IndigoAccent.copy(alpha = 0.7f); else -> RoseError }
                    Text(state, color = stateColor, fontSize = DashboardFontSizes.labelLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                    Text(signal.symbol, color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
                }
                Text(signal.timeframe, color = SlateText, fontSize = DashboardFontSizes.labelLarge)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone A", color = IndigoAccent, fontSize = DashboardFontSizes.labelLarge, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Validated 04:12 UTC", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("✓ Order Block", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    Text("✓ Liquidity Sweep", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                }

                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone B", color = IndigoAccent, fontSize = DashboardFontSizes.labelLarge, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ATR: 8 pips", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    Text("VOLUME: ↑ 1.3x", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Support: 1.0835 — Resist: 1.0865", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                }

                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone C", color = IndigoAccent, fontSize = DashboardFontSizes.labelLarge, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(28.dp).background(RoseError.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            Text("🛡️", fontSize = DashboardFontSizes.emojiIcon)
                        }
                        Column { Text("Shield: OK", color = SlateText, fontSize = DashboardFontSizes.labelMedium); Text("BrainCircuit: Stable", color = SlateText, fontSize = DashboardFontSizes.labelMedium) }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val dirIsBuy = signal.technicalScore >= 60
                Text(if (dirIsBuy) "BUY" else "SELL", color = if (dirIsBuy) EmeraldSuccess else RoseError, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val confidence = combined / 100f
                    val animated = animateFloatAsState(targetValue = confidence, animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = FastOutSlowInEasing)).value
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().width((animated * 100).dp).background(IndigoAccent, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Confidence: ${ (confidence * 100).roundToInt()}% — 60% Tech / 40% Safety", color = SlateText, fontSize = DashboardFontSizes.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        signal.rationale.forEach { r -> Text("• $r", color = SlateText, fontSize = DashboardFontSizes.labelLarge) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Entry", color = SlateText, fontSize = DashboardFontSizes.labelMedium); Text(signal.entry, color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black) }
                Column { Text("Invalidation", color = SlateText, fontSize = DashboardFontSizes.labelMedium); Text(signal.invalidation, color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black) }
            }

            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Prop Guard • Intelligence Audit: Monitoring ${signal.timeframe}", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(20.dp).background(IndigoAccent.copy(alpha = 0.12f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("🧠", fontSize = DashboardFontSizes.labelMedium) }
                    Box(modifier = Modifier.size(20.dp).background(IndigoAccent.copy(alpha = 0.12f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("🛡️", fontSize = DashboardFontSizes.labelMedium) }
                }
            }
        }
    }
}

@Composable
fun SignalModalComponent(signal: Signal, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "ASC",
                        color = Color.White.copy(alpha = 0.04f),
                        fontSize = DashboardFontSizes.signalZoneEmoji,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${signal.symbol} — Tactical Overlay", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black)
                            Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                        }

                        Text("The Why:", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
                        Text("Institutional logic: ${signal.rationale.joinToString(", ")}. Model weighs technical confluence and safety clearing to produce actionable awareness.", color = Color.White, fontSize = DashboardFontSizes.labelLarge)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Post-Analysis Data", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
                        Column { Text("• VIX Input: 19.8", color = SlateText); Text("• DXY Beta: +0.42%", color = SlateText); Text("• Retail Alignment: Neutral", color = SlateText) }

                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text("ASC — Intelligence Audit", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun timeStr(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}
