package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun ExposureHub() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // compact InfoBoxes: outer padding reduced, inner content padding reduced from 24.dp -> 8.dp
        InfoBox(modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 6.dp), height = 120.dp) {
            // Title above, then icon and value on the same line
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("NET USD EXPOSURE", color = SlateText, fontSize = 10.8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("∑", color = IndigoAccent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LONG 4.2 Lots", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }
        }

        InfoBox(modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 6.dp), height = 120.dp) {
            // Title above, then icon and value on the same line
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL ACTIVE", color = SlateText, fontSize = 10.8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(GhostWhite, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("¤", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("12.5 L", color = IndigoAccent, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }
        }
    }
}