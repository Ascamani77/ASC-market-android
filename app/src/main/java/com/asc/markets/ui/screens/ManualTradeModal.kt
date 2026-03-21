package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ManualTradeModal(onDismiss: () -> Unit, accentColor: Color) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = Color(0xFF09090B),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "NEW SIMULATED ORDER",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "INSTITUTIONAL EXECUTION",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF1C1C1E), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Asset Info
                Surface(
                    color = Color(0xFF111111),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1C1C1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("₿", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("BTC/USDT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Bitcoin / Tether", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$64,209.78", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            Text("+1.24%", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Trade Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(48.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("BUY", color = accentColor, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                        }
                    }
                    Surface(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(48.dp),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF1C1C1E))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("SELL", color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                        }
                    }
                }

                // Parameters
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ManualTradeParameterField("ORDER TYPE", "MARKET EXECUTION")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ManualTradeParameterField("LOT SIZE", "0.10", Modifier.weight(1f))
                        ManualTradeParameterField("LEVERAGE", "1:100", Modifier.weight(1f))
                    }
                }

                // Execute Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        "OPEN SIMULATED POSITION",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualTradeParameterField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                value,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
