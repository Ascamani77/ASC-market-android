package com.asc.markets.ui.screens.dashboard

import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.theme.*

@Composable
fun FullAssetHeader(pair: ForexPair) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = pair.symbol, 
                    color = Color.White, 
                    fontSize = DashboardFontSizes.dashboardActiveSession,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily,
                    letterSpacing = (-1).sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(EmeraldSuccess, CircleShape))
                    Text(
                        text = "NY4 EQUINIX / FEED: TOP-OF-BOOK", 
                        color = SlateText, 
                        fontSize = DashboardFontSizes.microCaption, 
                        fontWeight = FontWeight.Black, 
                        fontFamily = InterFontFamily,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = "14ms", 
                    color = IndigoAccent, 
                    fontSize = DashboardFontSizes.labelMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "LMAX DIRECT", 
                    color = Color.DarkGray, 
                    fontSize = DashboardFontSizes.microCaption, 
                    fontWeight = FontWeight.Black, 
                    fontFamily = InterFontFamily
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            val precision = if (pair.symbol.contains("JPY") || pair.symbol.contains("BTC") || pair.symbol.contains("XAU")) 2 else 5
            Text(
                text = String.format(java.util.Locale.US, "%.${precision}f", pair.price),
                color = Color.White,
                fontSize = DashboardFontSizes.emojiIcon,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "${if (pair.change >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.2f", pair.changePercent)}%",
                color = if (pair.change >= 0) EmeraldSuccess else RoseError,
                fontSize = DashboardFontSizes.bodyTiny,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
        }
    }
}