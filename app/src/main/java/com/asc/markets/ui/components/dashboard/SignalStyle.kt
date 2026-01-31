package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SignalStyleBox(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    mainValue: String,
    delta: String? = null,
    statusLabel: String? = null,
    confidence: Int? = null,
    entryZone: String? = null,
    rr: String? = null
) {
    // compact SignalStyleBox: reduced inner padding to 8.dp
    InfoBox(modifier = modifier, height = 120.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (statusLabel != null) {
                        val statusColor = when(statusLabel) {
                            "FOCUS" -> IndigoAccent
                            "OBSERVE" -> Color(0xFFF59E0B)
                            else -> RoseError
                        }
                        Box(modifier = Modifier
                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Column {
                        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        if (subtitle.isNotEmpty()) Text(subtitle, color = SlateText, fontSize = 9.sp)
                    }
                }

                if (confidence != null) {
                    Text("${confidence}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }

            // bottom area
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(mainValue, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    if (delta != null) Text(delta, color = if (delta.startsWith("+")) EmeraldSuccess else RoseError, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }

                if (confidence != null) {
                    // compact confidence bar: 4.dp height to match SignalCardView
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxWidth(confidence / 100f).fillMaxHeight().background(IndigoAccent, RoundedCornerShape(2.dp))) {}
                    }
                }

                if (entryZone != null || rr != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (entryZone != null) Text("ENTRY: $entryZone", color = SlateText, fontSize = 9.sp)
                        if (rr != null) Text(rr, color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

// Centralized SignalCardView reused by StrategySignalsTab and MarketOverview
@Composable
fun SignalCardView(
    modifier: Modifier = Modifier,
    pair: String,
    status: String? = null,
    conf: Int,
    mainValue: String? = null,
    delta: String? = null,
    entryZone: String? = null,
    rr: String? = null,
    onClick: (() -> Unit)? = null
) {
    val statusColor = when(status) {
        "FOCUS" -> IndigoAccent
        "OBSERVE" -> Color(0xFFF59E0B)
        else -> RoseError
    }

    InfoBox(modifier = modifier, height = 120.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), onClick = onClick) {
        // tighter inner padding for compact cards
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status != null) {
                        Box(modifier = Modifier
                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        }
                    }
                    Text(pair, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    if (!delta.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(delta, color = if (delta.startsWith("+")) EmeraldSuccess else RoseError, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }

                // right side: only confidence percent
                Column(horizontalAlignment = Alignment.End) {
                    Text("${conf}%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // compact scale (4.dp height)
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(conf / 100f).height(4.dp).background(statusColor, RoundedCornerShape(2.dp)))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TECHNICAL CONFLUENCE", color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Text("SAFETY CLEARANCE", color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // compact entry display: show the provided entryZone or mainValue in monospace
                    val entryDisplay = entryZone ?: mainValue ?: ""
                    Text(entryDisplay, color = SlateText, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                if (!rr.isNullOrEmpty()) {
                    Text(rr, color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }
        }
    }
}

// Preview for quick visual inspection inside Android Studio
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
fun SignalCardPreview() {
    Column(modifier = Modifier.padding(8.dp)) {
        SignalCardView(
            modifier = Modifier.width(320.dp),
            pair = "DXY",
            status = "FOCUS",
            conf = 82,
            mainValue = "104.22",
            delta = "+0.12%",
            entryZone = "104.00",
            rr = "RR 1:2.4"
        )
        Spacer(modifier = Modifier.height(8.dp))
        SignalCardView(
            modifier = Modifier.width(320.dp),
            pair = "ES1!",
            status = "OBSERVE",
            conf = 68,
            mainValue = "5210.45",
            delta = "+0.23%",
            entryZone = "5205.00",
            rr = "RR 1:1.8"
        )
    }
}
