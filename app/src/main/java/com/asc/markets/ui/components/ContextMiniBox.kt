package com.asc.markets.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.SlateText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text

@Composable
fun ContextMiniBox(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(value.uppercase(), color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, lineHeight = 14.sp)
    }
}
