package com.asc.markets.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AiScreen(
    viewModel: AiViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "AI PIPELINE TERMINAL",
            color = Color(0xFF00C853),
            style = MaterialTheme.typography.headlineSmall
        )

        // Run Button
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.runAi() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853)
                )
            ) {
                Text("RUN PIPELINE", color = Color.Black)
            }

            Button(
                onClick = { viewModel.fetchLatest() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E1E2E)
                )
            ) {
                Text("FETCH LATEST", color = Color.White)
            }
        }

        // Loading State
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00C853))
            }
        }

        // Message
        if (state.message.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
            ) {
                Text(
                    text = state.message,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Error
        state.error?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6B2222))
            ) {
                Text(
                    text = "Error: $it",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFFF6B6B)
                )
            }
        }

        // Decisions List
        Text(
            text = "FINAL DECISIONS",
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall
        )

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.decisions) { item ->
                DecisionCard(item)
            }

            if (state.decisions.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = "No decisions yet. Click 'RUN AI PIPELINE' to start.",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionCard(item: com.asc.markets.data.remote.FinalDecisionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C853))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Asset:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text(item.asset_1 ?: "-", color = Color(0xFF00C853))
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Direction:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text(item.journal_direction ?: "-", color = Color.White)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Label:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text(item.portfolio_decision_label ?: "-", color = Color.White)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Bucket:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text(item.portfolio_deployment_bucket ?: "-", color = Color.White)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Risk %:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text("${item.final_risk_pct ?: 0.0}%", color = Color(0xFFFF6B6B))
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Risk Amount:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text("${item.final_risk_amount ?: 0.0}", color = Color(0xFFFF6B6B))
            }
            item.portfolio_decision_reason?.let {
                Text(
                    text = "Reason: $it",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2
                )
            }
        }
    }
}
