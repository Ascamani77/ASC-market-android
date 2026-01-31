package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun GlobalErrorBoundary(content: @Composable () -> Unit) {
    var hasError by remember { mutableStateOf(false) }

    if (hasError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(RoseError, androidx.compose.foundation.shape.CircleShape))
                    Text(
                        "CRITICAL_NODE_FAILURE",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "DATA TEMPORARILY UNAVAILABLE",
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        content()
    }
}