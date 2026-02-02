@file:Suppress("DEPRECATION")
package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import java.util.Locale

@Composable
fun InstitutionalLevelsGrid(price: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LevelBox("RESISTANCE", price + 0.0015, Modifier.weight(1f))
            LevelBox("SUPPORT", price - 0.0012, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LevelBox("DAILY HIGH", price + 0.0021, Modifier.weight(1f))
            LevelBox("DAILY LOW", price - 0.0008, Modifier.weight(1f))
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun LevelBox(label: String, value: Double, modifier: Modifier) {
    InfoBox(modifier = modifier, minHeight = 110.dp) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // choose an icon based on the label and color it and size it larger
                val (iconImageVector, iconTint) = when {
                    label.contains("RESISTANCE", ignoreCase = true) -> Pair(Icons.Filled.TrendingUp, RoseError)
                    label.contains("SUPPORT", ignoreCase = true) -> Pair(Icons.Filled.TrendingDown, EmeraldSuccess)
                    label.contains("DAILY HIGH", ignoreCase = true) -> Pair(Icons.Filled.ArrowUpward, IndigoAccent)
                    label.contains("DAILY LOW", ignoreCase = true) -> Pair(Icons.Filled.ArrowDownward, RoseError)
                    else -> Pair(Icons.Filled.TrendingUp, SlateText)
                }

                // Icon with rounded background (like NET USD EXPOSURE)
                Box(modifier = Modifier.size(32.dp).background(iconTint.copy(alpha = 0.10f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(iconImageVector, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
                }

                Text(label, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }

            Text(
                String.format(Locale.US, "%.5f", value),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
