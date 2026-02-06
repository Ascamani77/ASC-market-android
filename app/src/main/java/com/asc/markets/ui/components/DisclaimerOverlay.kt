package com.asc.markets.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

/**
 * 1:1 Legal Parity with DisclaimerOverlay.tsx
 * Implements the same institutional risk disclosure logic and clinical aesthetics.
 */
@Composable
fun DisclaimerOverlay(onAccept: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.9f) // Parity: bg-black/90
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            // Background Watermark (Parity: absolute top-0 right-0 opacity-[0.02])
            Icon(
                imageVector = Icons.Default.ShieldMoon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.02f),
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-40).dp)
            )

            Surface(
                color = DeepBlack,
                shape = RoundedCornerShape(12.dp), // Parity: rounded-xl
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    // Header Parity
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.1f), // Parity: bg-amber-500/10
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.padding(12.dp).size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "RISK DISCLOSURE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Content Copy Parity
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Forex, Crypto, and CFD trading involve significant risk to your capital. The ASC Markets platform provides analytical intelligence and rule-based market mapping for situational awareness only.",
                            color = SlateText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            lineHeight = 18.sp
                        )
                        
                        Text(
                            text = "This application DOES NOT issue financial advice or direct trading or automated dispatch commands. The AI analytical desk summarizes historical alignment and technical confluence.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "Data is sourced from TradingView, Finnhub, and LMAX Hubs. Connectivity may be subject to sub-millisecond latency. Past performance is not indicative of future results.",
                            color = SlateText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            lineHeight = 18.sp
                        )
                    }

                    // Button Parity
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .padding(top = 32.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = "I ACKNOWLEDGE & ACCEPT RISK",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}