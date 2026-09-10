package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.SlateText

@Composable
fun NewAISimulationScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Box(modifier = Modifier.fillMaxSize().background(DeepBlack), contentAlignment = Alignment.Center) {
            Text("AI Simulation is currently unavailable.", color = SlateText, fontSize = 16.sp)
        }
    }
}
