package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.SlateText

@Composable
fun PortfolioManagerScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Portfolio management is currently unavailable.", color = SlateText, fontSize = 16.sp)
        }
    }
}
