package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.WatchlistItem
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun WatchlistScreen(viewModel: ForexViewModel) {
    val watchlistItems by viewModel.watchlistItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(top = 16.dp)
    ) {
        // Full Watchlist Table/List - Sorted by Move Probability (Priority ranking)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(watchlistItems.sortedByDescending { it.moveProbability }) { item ->
                WatchlistAssetCard(item)
            }
        }
    }
}

@Composable
fun WatchlistAssetCard(item: WatchlistItem) {
    InfoBox(
        modifier = Modifier.fillMaxWidth(),
        minHeight = 140.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Asset Name + Breakout Probability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.assetName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BREAKOUT PROBABILITY",
                        color = SlateText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.moveProbability}%",
                        color = when {
                            item.moveProbability >= 80 -> RoseError
                            item.moveProbability >= 70 -> Color(0xFFF59E0B)
                            item.moveProbability >= 60 -> Color.Yellow
                            else -> EmeraldSuccess
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Pressure Level Bar
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PRESSURE LEVEL", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                val barColor = when {
                    item.moveProbability >= 80 -> RoseError // Red: move imminent
                    item.moveProbability >= 70 -> Color(0xFFF59E0B) // Orange: high probability
                    item.moveProbability >= 60 -> Color.Yellow // Yellow: building
                    else -> EmeraldSuccess // Green: normal
                }
                LinearProgressIndicator(
                    progress = item.moveProbability / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Details Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                // Column 1: Status & Trigger
                Column(modifier = Modifier.weight(1f)) {
                    DetailItem(label = "STATUS", value = item.status)
                    if (item.triggerEvent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailItem(label = "TRIGGER", value = item.triggerEvent, valueColor = IndigoAccent)
                    }
                }
                
                // Column 2: Risk & Timing
                Column(modifier = Modifier.weight(1f)) {
                    val riskColor = when {
                        item.newsRisk.contains("High", ignoreCase = true) -> RoseError
                        item.newsRisk.contains("Medium", ignoreCase = true) -> Color(0xFFF59E0B)
                        else -> EmeraldSuccess
                    }
                    DetailItem(label = "NEWS RISK", value = item.newsRisk, valueColor = riskColor)
                    if (item.timeToEvent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailItem(label = "TIME TO EVENT", value = item.timeToEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, valueColor: Color = Color.White) {
    Column {
        Text(
            text = label,
            color = SlateText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily
        )
    }
}
