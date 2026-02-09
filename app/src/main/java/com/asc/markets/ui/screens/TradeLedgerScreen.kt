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

    Column(modifier = Modifier.fillMaxSize()) {
        // Separator between main menu and subheader
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color.White.copy(alpha = 0.06f))

        // Static subheader that does not scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("REAL-TIME NODE AUTONOMOUS AUDIT STREAM", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Area between subheader and page content uses DeepBlack background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepBlack)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color.White.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DeepBlack),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Full-width execution quality banner (touches screen edges)
            item {
                InfoBox(minHeight = 220.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF101010)) {
                            Box(contentAlignment = Alignment.Center) { Text("🔒", color = Color.White, fontSize = 20.sp) }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("EXECUTION", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Text("QUALITY", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("POST-MOVE EFFICIENCY\nLEDGER (10%)", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("AVG SLIPPAGE", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("0.12 PIPS", color = EmeraldSuccess, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("SYSTEM EFFICIENCY", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("96.8% VALIDATED", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            }

            items(MOCK_TRADES) { trade ->
                DashboardTrade(trade) { selectedTrade = it }
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }

    if (selectedTrade != null) {
        DeepAuditModal(selectedTrade!!) { selectedTrade = null }
    }
}

// ContextMiniBox moved to ui.components.DashboardTrade