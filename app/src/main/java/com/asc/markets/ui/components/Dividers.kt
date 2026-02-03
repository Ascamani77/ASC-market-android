package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalDivider(color: Color = Color.White.copy(alpha = 0.05f), thickness: Dp = 1.dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(thickness).background(color))
}
