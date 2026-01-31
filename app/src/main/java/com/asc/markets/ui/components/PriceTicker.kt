package com.asc.markets.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.PureBlack

@Composable
fun PriceTicker() {
    val infiniteTransition = rememberInfiniteTransition(label = "ticker")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(PureBlack)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.offset(x = offset.dp)) {
            repeat(10) {
                TickerItem("EUR/USD", "1.0845", "+0.11%")
                TickerItem("BTC/USD", "67,432", "+1.87%")
                TickerItem("XAU/USD", "2,342", "-0.22%")
                TickerItem("NAS100", "18,240", "+0.79%")
            }
        }
    }
}

@Composable
fun TickerItem(pair: String, price: String, change: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(pair, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.width(6.dp))
        Text(price, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            change,
            color = if (change.startsWith("+")) EmeraldSuccess else RoseError,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}