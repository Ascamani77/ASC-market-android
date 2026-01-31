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
import com.asc.markets.ui.theme.RoseError

@Composable
fun NewsTicker() {
    val infiniteTransition = rememberInfiniteTransition(label = "ticker")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse"
    )
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1000f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "scroll"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color(0xFF8C0000).copy(alpha = alpha))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFB91C1C))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("HIGH IMPACT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.width(20.dp))

            Row(modifier = Modifier.offset(x = scrollOffset.dp)) {
                repeat(10) {
                    Text(
                        " [CRITICAL] US Non-Farm Payrolls (NFP) — EXPECT HEAVY SLIPPAGE • ",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}