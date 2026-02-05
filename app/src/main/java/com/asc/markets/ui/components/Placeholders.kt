package com.asc.markets.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement

private fun Context.isReducedMotion(): Boolean {
    return try {
        val scale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        scale <= 0.5f
    } catch (t: Throwable) {
        false
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier, corner: Dp = 8.dp, baseColor: Color = Color(0xFF0B0B0D), highlightColor: Color = Color(0xFF1C1C1E)) {
    val ctx = LocalContext.current
    val reduced = ctx.isReducedMotion()

    val animState = if (reduced) {
        animateFloatAsState(targetValue = 0f, animationSpec = tween(durationMillis = 1))
    } else {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing))
        )
    }
    val anim by animState

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset.Zero,
        end = Offset(200f * (1 + anim), 200f * (1 + anim)),
        tileMode = TileMode.Clamp
    )

    Box(modifier = modifier
        .clip(RoundedCornerShape(corner))
        .background(brush))
}

@Composable
fun SkeletonColumn(lines: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(lines) {
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(6.dp)))
        }
    }
}

@Composable
fun TooltipIcon(text: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(18.dp).clickable { visible = !visible }, tint = Color(0xFF9E9E9E))
        if (visible) {
            Surface(modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(8.dp)), shape = RoundedCornerShape(8.dp), color = Color(0xFF111215)) {
                Text(text, modifier = Modifier.padding(8.dp), color = Color.White)
            }
        }
    }
}

@Composable
fun SpotlightMask(modifier: Modifier = Modifier, radiusDp: Dp = 120.dp, color: Color = Color(0x99000000)) {
    // Subtle overlay radial gradient to simulate vignette/spotlight
    Box(modifier = modifier.background(Brush.radialGradient(listOf(Color.Transparent, color), radius = radiusDp.toPxCompat()))) {}
}

// conversion helper since Dp.toPx requires density; keep small util
@Composable
private fun Dp.toPxCompat(): Float = with(LocalContext.current.resources.displayMetrics) { this@toPxCompat.value * density }

@Composable
fun responsivePadding(): PaddingValues {
    val cfg = LocalConfiguration.current
    val w = cfg.screenWidthDp
    return when {
        w < 420 -> PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        w < 700 -> PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        else -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    }
}
