package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.AIAdvisory
import com.asc.markets.ui.screens.tradeDashboard.model.Bias
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AIAdvisoryPanel(advisory: AIAdvisory, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var isExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Indigo400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI ADVISORY",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Surface(
                color = Rose500.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose500.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "${advisory.riskLevel} RISK",
                    color = Rose500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bias & Confidence Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Zinc950,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "AI BIAS", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (advisory.bias == Bias.BEARISH) "↘ " else if (advisory.bias == Bias.BULLISH) "↗ " else "→ ",
                            color = if (advisory.bias == Bias.BEARISH) Rose500 else if (advisory.bias == Bias.BULLISH) Emerald500 else Zinc400,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = advisory.bias.name,
                            color = if (advisory.bias == Bias.BEARISH) Rose500 else if (advisory.bias == Bias.BULLISH) Emerald500 else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CONFIDENCE", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${advisory.confidence}%",
                        color = Indigo400,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Suggested SL/TP
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // SL
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Rose500, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SUGGESTED SL", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "%.5f".format(advisory.suggestedSL), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            // TP
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Emerald500, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SUGGESTED TP", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "%.5f".format(advisory.suggestedTP), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Execute Button
        Button(
            onClick = {
                scope.launch {
                    isExecuting = true
                    delay(2000) // Simulate API call
                    isExecuting = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isExecuting,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "EXECUTE SUGGESTED TRADE",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "* AI suggestions are based on multi-timeframe analysis and sentiment data. Always perform your own due diligence.",
            color = Zinc500,
            fontSize = 9.sp,
            lineHeight = 12.sp
        )
    }
}
