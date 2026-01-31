package com.asc.markets.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.asc.markets.ui.theme.PureBlack
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Institutional InfoBox: thin 1px hairline border, pure black surface, rounded corners.
 * Matches provided design: 1.dp white border at 18% alpha, RoundedCornerShape(12.dp)
 */
@Composable
fun InfoBox(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    minHeight: Dp? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(1.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderColor = if (isPressed) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.18f)

    // Do not force fillMaxWidth here. Let the caller decide sizing so
    // InfoBox can be used inside horizontal LazyRow items without
    // causing infinite width constraints.
    var boxModifier = modifier
    if (height != null) boxModifier = boxModifier.height(height)
    if (minHeight != null) boxModifier = boxModifier.heightIn(min = minHeight)

    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = boxModifier.then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
    ) {
        // Subtle vertical highlight to give the PureBlack surface a slight "shine"
        val shineBrush = Brush.verticalGradient(
            colors = listOf(PureBlack.copy(alpha = 0.88f), PureBlack),
            startY = 0f,
            endY = 400f
        )

        // Default shiny text style for writeups inside InfoBox (brighter white)
        val shinyTextStyle = LocalTextStyle.current.merge(
            TextStyle(
                color = Color.White.copy(alpha = 0.98f)
            )
        )

        Box(contentAlignment = Alignment.TopStart) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = shineBrush, shape = RoundedCornerShape(12.dp))
                    .padding(contentPadding)
            ) {
                CompositionLocalProvider(LocalTextStyle provides shinyTextStyle) {
                    content()
                }
            }
        }
    }
}