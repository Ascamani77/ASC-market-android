package com.asc.markets.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal to control whether microstructure UI should be shown.
 * MacroStream will set this to false so downstream components can hide order-book/DOM UIs.
 */
val LocalShowMicrostructure = compositionLocalOf { true }
