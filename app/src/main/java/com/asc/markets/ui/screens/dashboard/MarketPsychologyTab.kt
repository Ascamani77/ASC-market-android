package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.screens.dashboard.getExploreItemsForContext
import com.asc.markets.state.AssetContext
import com.asc.markets.state.AssetContextStore
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*

@Composable
fun MarketPsychologyTab() {
    val ctx by AssetContextStore.context.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // If asset context is FOREX, show the detailed psychology meter; otherwise show an asset-specific summary
        if (ctx == AssetContext.FOREX) {
            item {
                InfoBox(height = 320.dp) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GLOBAL PSYCHOLOGY METER", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.weight(1f))
                        PsychologyGauge(72)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SentimentStateBox("STATE", "GREED REGIME", EmeraldSuccess)
                            SentimentStateBox("VOLATILITY", "STABLE", Color.White)
                            SentimentStateBox("DXY BETA", "0.82 HIGH", Color.White)
                        }
                    }
                }
            }
        } else {
            item {
                InfoBox(height = 160.dp) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${ctx.name} Psychology Summary", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black)
                        val events = getMacroEventsForContext(ctx)
                        events.take(3).forEach { (t, txt) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(t, color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                                Text(txt, color = Color.White, fontSize = DashboardFontSizes.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Institutional bias and flow summary available for ${ctx.name}.", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel)
                    }
                }
            }
        }
        // Global Sentiment Header (dual-pill)
        item {
            InfoBox {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = com.asc.markets.R.drawable.lucide_binary), contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GLOBAL SENTIMENT INDEX", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(999.dp), color = EmeraldSuccess.copy(alpha = 0.06f), modifier = Modifier.padding(end = 4.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("SMART MONEY", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("BULLISH", color = EmeraldSuccess, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                            Surface(shape = RoundedCornerShape(999.dp), color = RoseError.copy(alpha = 0.06f)) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("RETAIL FLOW", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("BEARISH", color = RoseError, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7-Day Momentum area chart
        item {
            InfoBox {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("7-Day Momentum", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.Transparent)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val points = listOf(0.4f, 0.45f, 0.5f, 0.6f, 0.55f, 0.7f, 0.72f)
                            val step = w / (points.size - 1)
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, h)
                                points.forEachIndexed { i, v ->
                                    lineTo(i * step, h - (v * h))
                                }
                                lineTo(w, h)
                                close()
                            }
                            drawPath(path, brush = Brush.verticalGradient(listOf(RoseError, IndigoAccent, EmeraldSuccess)))
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SENTIMENT REGISTRY", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                var selectedPair by remember { mutableStateOf<com.asc.markets.data.ForexPair?>(null) }
                if (ctx == AssetContext.FOREX) {
                    val list = getExploreItemsForContext(ctx)
                    list.forEach { pair ->
                        SentimentAssetRow(pair) { selectedPair = it }
                    }

                    if (selectedPair != null) {
                        InstitutionalContextModal(pair = selectedPair!!, onClose = { selectedPair = null })
                    }
                } else {
                    // Per-asset sentiment placeholder when specific registry is unavailable
                    Text("Sentiment registry not available for ${ctx.name}. See Overview for concise bias and flow.", color = SlateText, fontSize = DashboardFontSizes.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SentimentAssetRow(pair: com.asc.markets.data.ForexPair, onOpenContext: (com.asc.markets.data.ForexPair) -> Unit) {
    val bullishVal = (50 + (pair.changePercent * 10)).coerceIn(5.0, 95.0).toInt()
    
    InfoBox(modifier = Modifier.fillMaxWidth().clickable { onOpenContext(pair) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Layer 1: Identity row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PairFlags(pair.symbol, 28)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                Text(pair.symbol, color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(String.format("%.2f", pair.price), color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(String.format("%+.2f%%", pair.changePercent), color = if (pair.changePercent >= 0) EmeraldSuccess else RoseError, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }

            // Layer 2: Split bar with labels
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${bullishVal}% Long", color = EmeraldSuccess, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black)
                    Text("${100 - bullishVal}% Short", color = RoseError, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black)
                }
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(bullishVal / 100f).fillMaxHeight().background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier.fillMaxWidth().offset(x = 0.dp).fillMaxHeight().background(RoseError.copy(alpha = 0.0f))) {}
                }
            }

            // Layer 3: Bias badge + flow descriptor
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val yellow = Color(0xFFF59E0B)
                val badgeBg = when {
                    bullishVal > 60 -> EmeraldSuccess.copy(alpha = 0.08f)
                    bullishVal < 40 -> RoseError.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
                val badgeTextColor = when {
                    bullishVal > 60 -> EmeraldSuccess
                    bullishVal < 40 -> RoseError
                    else -> yellow
                }

                Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                    Text(if (bullishVal > 60) "BULLISH" else if (bullishVal < 40) "BEARISH" else "NEUTRAL", color = badgeTextColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), fontWeight = FontWeight.Black)
                }
                Text(if (bullishVal > 70) "Aggressive Buying" else if (bullishVal < 30) "Capitulation" else "Mixed Flow", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel)
            }
        }
    }
}

@Composable
private fun InstitutionalContextModal(pair: com.asc.markets.data.ForexPair, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.86f)) {
                Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Institutional Context — ${pair.symbol}", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black)
                        Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                    }

                    Text("Why", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    Text("High Greed derived from decreasing VIX premiums and concentrated flows into larger lot sizes. Retail proxies decoupled from volume on delta.", color = Color.White)

                    Text("Supporting Data", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Volume Divergence", color = SlateText)
                            Text("+12%", color = Color.White, fontFamily = InterFontFamily)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Positioning Multiplier", color = SlateText)
                            Text("2.4x", color = Color.White, fontFamily = InterFontFamily)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Retail Alignment", color = SlateText)
                            Text("Extreme", color = RoseError, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PsychologyGauge(value: Int) {
    val animatedValue by animateFloatAsState(targetValue = value / 100f, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "gauge")
    
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            drawArc(
                color = Color(0xFF1A1C23),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.horizontalGradient(listOf(RoseError, Color(0xFFF59E0B), EmeraldSuccess)),
                startAngle = 135f,
                sweepAngle = 270f * animatedValue,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Color.White, fontSize = DashboardFontSizes.sentimentScore, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text("SCORE", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun SentimentStateBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.DarkGray, fontSize = DashboardFontSizes.gridLabelTiny, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Text(value, color = color, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}