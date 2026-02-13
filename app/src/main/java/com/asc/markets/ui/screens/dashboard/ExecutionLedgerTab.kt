package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.autoMirrored.outlined.Info
import androidx.compose.material.icons.autoMirrored.outlined.History
import androidx.compose.material.icons.autoMirrored.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.data.MOCK_TRADES
import com.asc.markets.data.AutomatedTrade
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.components.DeepAuditModal
import com.asc.markets.ui.components.DashboardTrade
import com.asc.markets.ui.theme.*

@Composable
fun ExecutionLedgerTab() {
    var selectedTx by remember { mutableStateOf<ExecutionTx?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)) {
        // Header for Post-Move Audit (Post-Move Efficiency Ledger)
        InfoBox(minHeight = 60.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Post-Move Audit", color = IndigoAccent, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
                    Text("Slippage & Fill Latency — Post-Move Audit", color = SlateText, fontSize = DashboardFontSizes.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ExecutionLedgerSection(onOpenCompliance = { selectedTx = it })
    }

    if (selectedTx != null) {
        DeepAuditModal(tx = selectedTx!!, onClose = { selectedTx = null })
    }
}
