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
import com.asc.markets.ui.theme.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Institutional InfoBox: thin 1px hairline border, charcoal surface, rounded corners.
 * Matches provided design: 1.dp white border at 18% alpha, RoundedCornerShape(12.dp)
 * Background updated to DeepBlack (#121212) per design request.
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

    // Surround all boxes with the white hairline border (HairlineBorder = 18% alpha white)
    val borderColor = if (isPressed) HairlineHighlight else HairlineBorder

    // Do not force fillMaxWidth here. Let the caller decide sizing so
    // InfoBox can be used inside horizontal LazyRow items without
    // causing infinite width constraints.
    var boxModifier = modifier
    if (height != null) boxModifier = boxModifier.height(height)
    if (minHeight != null) boxModifier = boxModifier.heightIn(min = minHeight)

    Surface(
        color = DeepBlack,
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
        // Subtle vertical highlight to give the DeepBlack surface a slight "shine"
        val shineBrush = Brush.verticalGradient(
            colors = listOf(DeepBlack.copy(alpha = 0.88f), DeepBlack),
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
