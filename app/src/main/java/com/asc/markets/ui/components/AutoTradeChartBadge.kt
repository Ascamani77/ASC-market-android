package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.AutoTradeEngine
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.InterFontFamily

/**
 * Overlay badge shown on the trading chart when the engine is auto-trading the
 * currently displayed symbol. Safe on any chart surface because it never
 * touches the chart's own pointer input.
 */
@Composable
fun AutoTradeChartBadge(symbol: String, modifier: Modifier = Modifier) {
    val activeSymbols by AutoTradeEngine.activeSymbols.collectAsState()
    val key = remember(symbol) { AutoTradeEngine.normalizeKey(symbol) }
    if (key.isEmpty() || key !in activeSymbols) return

    Surface(
        color = EmeraldSuccess.copy(alpha = 0.95f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
            Text(
                "AUTO TRADE ON",
                color = Color(0xFF0A3D2B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily,
                letterSpacing = 1.sp
            )
        }
    }
}