package com.asc.markets.ui.screens.tradeDashboard.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.MyApp
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.ui.screens.tradeDashboard.ui.components.*
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * OPPORTUNITY TAB - "What trade is available?"
 * Shows: Live AI decisions from backend (PRIMARY_DEPLOYMENT trades)
 */
@Composable
fun OpportunityTab(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val dividerColor = Color(0xFF151515)
    val context = LocalContext.current
    val app = context.applicationContext as MyApp
    val aiRepository = app.aiRepository
    
    // Observe global deployments
    val deploymentsResponse by aiRepository.deployments.collectAsState()
    val trades = deploymentsResponse?.final_decision ?: emptyList()
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Function to refresh trades (manual pipeline run)
    fun runPipeline() {
        scope.launch {
            isLoading = true
            aiRepository.runAiPipeline()
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 48.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRADE OPPORTUNITIES",
                    color = Color(0xFF00C853),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "PRIMARY_DEPLOYMENT",
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${trades.size}",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { runPipeline() },
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh trades",
                        tint = if (isLoading) Color.Gray else Color(0xFF00C853)
                    )
                }
            }
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // Loading State
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00C853))
            }
        }

        // Trades List
        if (trades.isNotEmpty()) {
            trades.forEach { trade ->
                TradeOpportunityCard(trade)
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }
        } else if (!isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trades available",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // AI Watch Panel (static)
        AIWatchPanel(alerts = viewModel.alerts, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TradeOpportunityCard(trade: FinalDecisionItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Asset + Direction
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trade.asset_1 ?: "-",
                color = Color(0xFF00C853),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = trade.journal_direction ?: "-",
                color = if (trade.journal_direction == "BUY") Color(0xFF00C853) else Color(0xFFFF6B6B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Decision Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Decision:", color = Color.Gray, fontSize = 9.sp)
            Text(
                text = trade.portfolio_decision_label ?: "-",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Position Scale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Size:", color = Color.Gray, fontSize = 9.sp)
            Text(
                text = "${trade.final_position_scale ?: 0.0}",
                color = Color.White,
                fontSize = 9.sp
            )
        }

        // Risk Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Risk %:", color = Color.Gray, fontSize = 9.sp)
            Text(
                text = "${trade.final_risk_pct ?: 0.0}%",
                color = Color(0xFFFF6B6B),
                fontSize = 9.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Risk Amt:", color = Color.Gray, fontSize = 9.sp)
            Text(
                text = "${trade.final_risk_amount ?: 0.0}",
                color = Color(0xFFFF6B6B),
                fontSize = 9.sp
            )
        }

        // Reason (if available)
        trade.portfolio_decision_reason?.let {
            Text(
                text = it,
                color = Color.Gray,
                fontSize = 8.sp,
                maxLines = 2
            )
        }

        // Bucket
        Text(
            text = "Bucket: ${trade.portfolio_deployment_bucket ?: "-"}",
            color = Color(0xFF6366F1),
            fontSize = 8.sp
        )
    }
}

