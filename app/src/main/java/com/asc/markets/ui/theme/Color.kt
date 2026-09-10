
package com.asc.markets.ui.theme

import androidx.compose.ui.graphics.Color

// Global Body Calibration: #000000
val DeepBlack = Color(0xFF000000)
// Component Surface Calibration: #000000
val PureBlack = Color(0xFF000000)
val ErieBlack = Color(0xFF181818)
// Active sidebar highlight
val ActiveHighlight = Color(0xFF222222)

// TradingView / Bloomberg-like bright cyan accent - matches market overview navbar
val IndigoAccent = Color(0xFF6366F1)
val EmeraldSuccess = Color(0xFF10B981)
val RoseError = Color(0xFFF43F5E)
val SlateText = Color(0xFF94A3B8)
val SlateMuted = Color(0xFF4B5563)

// Alias for dashboard colors
val LuminousBlue = Color(0xFF6366F1)
val GreenProfit = Color(0xFF10B981)

// Hairline Border Logic Parity: Reduced brightness from 18% to 8% alpha white
val HairlineBorder = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val HairlineHighlight = Color(0xFFFFFFFF).copy(alpha = 0.20f)
val GhostWhite = Color(0xFFFFFFFF).copy(alpha = 0.05f)

// Loading/Grey shades
val LoadingGrey900 = Color(0xFF111827) // Very dark grey (Tailwind gray-900)
