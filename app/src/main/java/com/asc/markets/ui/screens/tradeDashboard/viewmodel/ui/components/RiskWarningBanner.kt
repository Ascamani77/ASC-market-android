package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.RiskInfo
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.*

@Composable
fun RiskWarningBanner(atRiskPositions: List<RiskInfo>, modifier: Modifier = Modifier) {
    if (atRiskPositions.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Rose500.copy(alpha = 0.1f))
            .border(1.dp, Rose500.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Rose500, modifier = Modifier.size(18.dp))
            
            Column {
                Text("⚠ ${atRiskPositions.size} POSITION${if (atRiskPositions.size > 1) "S" else ""} AT CRITICAL RISK", 
                    color = Rose500, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                atRiskPositions.forEachIndexed { index, risk ->
                    if (index < 2) {
                        Text(
                            "${risk.position.symbol}: Health ${risk.position.healthScore}% - ${risk.reason}",
                            color = Zinc400,
                            fontSize = 9.sp,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }
    }
}
