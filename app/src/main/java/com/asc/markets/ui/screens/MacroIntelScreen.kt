package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun MacroIntelScreen() {
    var activeCategory by remember { mutableStateOf("NEWS") }
    val tabs = listOf("WATCHLIST", "PORTFOLIO", "NEWS")
    
    val newsItems = listOf(
        NewsStory("Fed Maintains Interest Rates, Signals Fewer Cuts", "REUTERS", "10m ago", "HIGH", "ECONOMY", true),
        NewsStory("NFP Exceeds Estimates by 75k Units", "BLOOMBERG", "45m ago", "HIGH", "FOREX", true),
        NewsStory("Nvidia Rally Reaches Critical Overextension", "CNBC", "1h ago", "MEDIUM", "STOCKS", false)
    )

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Top Navigation Tab Bar Parity
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { tab ->
                    val active = activeCategory == tab
                    Box(modifier = Modifier.height(56.dp).clickable { activeCategory = tab }, contentAlignment = Alignment.Center) {
                        Text(
                            text = tab, 
                            color = if (active) Color.White else SlateText, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily,
                            letterSpacing = 2.sp
                        )
                        if (active) {
                            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(IndigoAccent))
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(newsItems) { story ->
                NewsStoryCard(story)
            }
        }
    }
}

data class NewsStory(val title: String, val source: String, val time: String, val impact: String, val category: String, val isSurprise: Boolean)

@Composable
fun NewsStoryCard(story: NewsStory) {
    val impactColor = if (story.impact == "HIGH") RoseError else Color(0xFFF59E0B)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp)
            .background(Color.Transparent)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.background(impactColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("${story.impact} IMPACT", color = impactColor, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                if (story.isSurprise) {
                    Box(modifier = Modifier.background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("MAJOR SURPRISE", color = IndigoAccent, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
            }
            Text(story.time, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        Text(story.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, lineHeight = 24.sp)
        
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(story.source, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text(" • ", color = Color.DarkGray)
            Text(story.category, color = Color.Gray, fontSize = 11.sp, fontFamily = InterFontFamily)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
    }
}