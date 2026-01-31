package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.NewsItem
import com.asc.markets.ui.theme.*

@Composable
fun NewsItemCard(item: NewsItem, onClick: (NewsItem) -> Unit) {
    val impactText = item.impact?.uppercase() ?: "LOW"
    val impactColor = when (impactText) {
        "HIGH" -> RoseError
        "MEDIUM" -> Color(0xFFF59E0B)
        else -> EmeraldSuccess
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(impactColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$impactText IMPACT",
                    color = impactColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(item.timestamp.toString(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(item.headline, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.source, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            // No category field on NewsItem; keep source and optional URL marker
            item.url?.let { Text(" • ", color = Color.DarkGray); Text("LINK", color = Color.Gray, fontSize = 11.sp) }
        }
    }
}