package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.SlateText

@Composable
fun VenueMap() {
    // Simulated SOR weighting that updates every 5s
    val venues = listOf("JPM", "DB", "BARC", "CTDL", "LMAX")
    val weights = remember { mutableStateListOf<Float>().apply { repeat(venues.size) { add(0f) } } }

    LaunchedEffect(Unit) {
        while (true) {
            // generate random weights between 10 and 45 and normalize
            val raw = List(venues.size) { kotlin.random.Random.nextFloat() * (45f - 10f) + 10f }
            val sum = raw.sum()
            raw.forEachIndexed { i, v -> weights[i] = (v / sum) * 100f }
            kotlinx.coroutines.delay(5000)
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(venues) { idx, code ->
            val weight = weights.getOrNull(idx) ?: 0f
            val latency = kotlin.random.Random.nextDouble(0.01, 0.5)
            val latencyMs = String.format("%.2fms", latency)
            val isActive = weight > 0.5f

            InfoBox(modifier = Modifier.widthIn(min = 280.dp).fillMaxWidth(), height = 120.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Top row: code + status dot (left) and weight percentage aligned to extreme right
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isActive) EmeraldSuccess else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Venue icon mapping
                            when (code) {
                                "JPM", "DB", "BARC" -> Icon(Icons.Filled.AccountBalance, contentDescription = "bank", tint = Color.White, modifier = Modifier.size(18.dp))
                                "CTDL" -> Icon(Icons.Filled.ShowChart, contentDescription = "ctdl", tint = Color.White, modifier = Modifier.size(18.dp))
                                "LMAX" -> Icon(Icons.Filled.TrendingUp, contentDescription = "lmax", tint = Color.White, modifier = Modifier.size(18.dp))
                                else -> Icon(Icons.Filled.AccountBalance, contentDescription = "venue", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(code, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text("WGT: ${String.format("%.1f%%", weight)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }

                    // Forex scale chips (small pair badges)
                    val pairsForVenue = when (code) {
                        "JPM" -> listOf("EUR/USD", "GBP/USD", "USD/JPY")
                        "DB" -> listOf("EUR/USD", "USD/CHF", "AUD/USD")
                        "BARC" -> listOf("GBP/USD", "EUR/GBP", "EUR/USD")
                        "CTDL" -> listOf("EUR/USD", "USD/JPY", "GBP/USD")
                        "LMAX" -> listOf("EUR/USD", "GBP/USD", "USD/JPY")
                        else -> listOf("EUR/USD", "GBP/USD", "USD/JPY")
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pairsForVenue.forEach { p ->
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1720))
                                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.ShowChart, contentDescription = null, tint = SlateText, modifier = Modifier.size(12.dp))
                                    Text(p, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Visual Depth Meter (scale)
                    Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF070707))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = (weight / 100f)).clip(RoundedCornerShape(6.dp)).background(Color(0xFF00C2FF))) {}
                    }

                    // Latency row: label left, latency value right
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Execution Latency", color = SlateText, fontSize = 10.sp)
                        }
                        Text(latencyMs, color = SlateText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
