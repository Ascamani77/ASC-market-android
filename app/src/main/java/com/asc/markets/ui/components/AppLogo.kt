package com.asc.markets.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.asc.markets.R

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    animated: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val logoBitmap = remember(context, size) {
        val px = with(density) { size.roundToPx() }
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?.toBitmap(px, px)
            ?.asImageBitmap()
    }
    if (logoBitmap == null) return

    val scale = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "app_logo_pulse")
        val s by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        s
    } else {
        1f
    }

    Image(
        bitmap = logoBitmap,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}