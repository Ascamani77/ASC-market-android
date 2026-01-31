package com.asc.markets.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Colors are defined in Color.kt to avoid duplicate declarations and ambiguity.
// See: com.asc.markets.ui.theme.Color.kt

private val DarkColorScheme = darkColorScheme(
    primary = IndigoAccent,
    onPrimary = PureBlack,
    secondary = IndigoAccent,
    background = PureBlack,
    surface = PureBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = GhostWhite,
    outline = HairlineBorder
)

@Composable
fun AscTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ),
        content = content
    )
}
