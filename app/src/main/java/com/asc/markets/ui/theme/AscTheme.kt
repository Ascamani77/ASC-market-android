package com.asc.markets.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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

private val LightColorScheme = lightColorScheme(
    primary = IndigoAccent,
    onPrimary = Color.White,
    secondary = IndigoAccent,
    background = Color(0xFFF5F5F7),
    surface = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF0F0F5),
    outline = Color(0xFFE0E0E0)
)

enum class AscThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun AscTheme(
    themeMode: AscThemeMode = AscThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AscThemeMode.LIGHT -> false
        AscThemeMode.DARK -> true
        AscThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
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
