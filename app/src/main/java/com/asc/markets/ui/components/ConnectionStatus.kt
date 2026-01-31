package com.asc.markets.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ConnectivityManager
import com.asc.markets.logic.ConnectionState
import com.asc.markets.ui.theme.*

@Composable
fun ConnectionStatus() {
    val state by ConnectivityManager.state.collectAsState()
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    val color = when (state) {
        ConnectionState.LIVE -> EmeraldSuccess
        ConnectionState.STALE -> Color(0xFFF59E0B)
        else -> RoseError
    }

    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(if (state == ConnectionState.LIVE) pulseAlpha else 1f)
                    .background(color, androidx.compose.foundation.shape.CircleShape)
            )
            Text(
                text = state.name,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text("📡", fontSize = 10.sp, modifier = Modifier.alpha(0.5f))
        }
    }
}