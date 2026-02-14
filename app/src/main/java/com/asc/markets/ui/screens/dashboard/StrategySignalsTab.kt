package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun StrategySignalsTab() {
    // Delegate to the new DashboardSignals implementation which implements
    // the Opportunity Awareness Matrix (Deterministic Weighing Engine, responsive grid, modal, footer)
    DashboardSignals()
}

data class SignalData(val pair: String, val dir: String, val conf: Int, val status: String)

@Composable
private fun SignalCard(data: SignalData) {
    val signalData = rememberStrategySignal()
    
    // use centralized SignalCardView
    com.asc.markets.ui.components.dashboard.SignalCardView(
        pair = signalData.pair,
        status = signalData.status,
        conf = signalData.confidence,
        mainValue = signalData.entryPrice,
        delta = null,
        entryZone = signalData.entryPrice,
        rr = signalData.riskReward
    )
}