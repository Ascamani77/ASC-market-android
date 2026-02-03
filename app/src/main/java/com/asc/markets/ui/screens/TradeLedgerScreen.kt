package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.MOCK_TRADES
import com.asc.markets.data.AutomatedTrade
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.components.DeepAuditModal
import com.asc.markets.ui.components.DashboardTrade
import com.asc.markets.ui.theme.*

@Composable
fun TradeLedgerScreen() {
    var selectedTrade by remember { mutableStateOf<com.asc.markets.data.AutomatedTrade?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text("REAL-TIME NODE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text("AUTONOMOUS AUDIT STREAM", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(MOCK_TRADES) { trade ->
            DashboardTrade(trade) { selectedTrade = it }
        }
    }

    if (selectedTrade != null) {
        DeepAuditModal(selectedTrade!!) { selectedTrade = null }
    }
}

// ContextMiniBox moved to ui.components.DashboardTrade