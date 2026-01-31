package com.asc.markets.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.IndigoAccent

/**
 * Parity Component: Floating Chart Button
 * Matches FloatingChartButton.tsx aesthetics:
 * - bg-[#0F0F11]
 * - Draggable-ready overlay logic (integrated in MainActivity)
 * - Indigo accent glow at bottom
 * - Active scale transition
 */
@Composable
fun FloatingChartButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f)

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .shadow(
                elevation = 30.dp,
                shape = CircleShape,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(CircleShape)
            .background(Color(0xFF0F0F11))
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background glow parity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IndigoAccent.copy(alpha = 0.05f))
        )

        // The "Brand Icon" (A-shape path simulated)
        Text(
            text = "▲", 
            color = IndigoAccent, 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Black,
            modifier = Modifier.offset(y = (-2).dp)
        )

        // Sub-millisecond bottom accent (the "blue line" in JS)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.5f)
                .height(2.dp)
                .padding(bottom = 4.dp)
                .blur(2.dp)
                .background(IndigoAccent.copy(alpha = 0.3f))
        )
    }
}