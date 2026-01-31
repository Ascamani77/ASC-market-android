package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.IndigoAccent

@Composable
fun PairFlags(symbol: String, size: Int = 20) {
    val parts = symbol.split("/")
    val isSingle = parts.size == 1

    Row(verticalAlignment = Alignment.CenterVertically) {
        CurrencyCircle(parts[0], size)
        if (!isSingle) {
            Spacer(modifier = Modifier.width((- (size / 2.5)).dp))
            CurrencyCircle(parts[1], size)
        }
    }
}

@Composable
private fun CurrencyCircle(code: String, size: Int) {
    val color = when (code.uppercase()) {
        "USD" -> Color(0xFF1E3A8A)
        "EUR" -> Color(0xFF1E40AF)
        "GBP" -> Color(0xFF7C3AED)
        "JPY" -> Color(0xFFDC2626)
        "BTC" -> Color(0xFFF59E0B)
        "XAU" -> Color(0xFFEAB308)
        else -> Color(0xFF334155)
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code.take(1).uppercase(),
            color = Color.White,
            fontSize = (size / 2.5).sp,
            fontWeight = FontWeight.Black
        )
    }
}