package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.NewsItem
import com.asc.markets.ui.theme.*

@Composable
fun NewsInAppBrowser(news: NewsItem, onBack: () -> Unit) {
    var showAiSummary by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = PureBlack,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Text("←", color = Color.White, fontSize = 20.sp) }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(news.source, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(news.url ?: "", color = Color.Gray, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }

            // Webview Placeholder
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White), contentAlignment = Alignment.Center) {
                Text(news.headline, color = Color.Black, modifier = Modifier.padding(40.dp), fontWeight = FontWeight.Bold)
            }

            // Footer Control
            Surface(
                color = PureBlack,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text("EXIT READER", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { showAiSummary = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.3f))
                    ) {
                        Text("✦ MARKET SUMMARY", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // AI Summary Sheet
        if (showAiSummary) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.5f),
                color = Color.Black,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("AI MARKET ANALYSIS", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        IconButton(onClick = { showAiSummary = false }) { Text("✕", color = Color.Gray) }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "SUMMARY: This report suggests institutional rebalancing ahead of the NY session. DXY strength is acting as a major headwind for high-beta currencies.",
                        color = Color.White, fontSize = 14.sp, lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).background(GhostWhite, RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Column {
                                Text("SENTIMENT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                Text("BULLISH", color = EmeraldSuccess, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).background(GhostWhite, RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Column {
                                Text("TRADE IDEA", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                Text("Wait for sweep", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}