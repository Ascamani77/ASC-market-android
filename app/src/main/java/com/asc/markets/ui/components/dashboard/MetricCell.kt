package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.SlateText

@Composable
fun MetricCell(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = label.uppercase(), fontSize = 8.sp, color = SlateText, fontWeight = FontWeight.Black)
        Text(text = value, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Text(text = sub.uppercase(), fontSize = 7.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
    }
}
