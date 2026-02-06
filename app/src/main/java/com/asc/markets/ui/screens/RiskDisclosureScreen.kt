package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.SlateText

@Composable
fun RiskDisclosureScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("RISK DISCLOSURE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("LEGAL PROTOCOL V1.0", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        
        Text(
            "Forex, Crypto, and CFD trading involve significant risk to your capital. The ASC Markets platform provides analytical intelligence and rule-based market mapping for situational awareness only.\n\n" +
            "This application DOES NOT issue financial advice or direct trading or automated dispatch commands. The AI analytical desk summarizes historical alignment and technical confluence.\n\n" +
            "Data is sourced from TradingView, Finnhub, and LMAX Hubs. Connectivity may be subject to sub-millisecond latency. Past performance is not indicative of future results.",
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}