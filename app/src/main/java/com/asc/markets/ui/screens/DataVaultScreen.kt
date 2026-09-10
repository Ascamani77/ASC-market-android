package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.SlateText

@Composable
fun VaultCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PureBlack,
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        color = PureBlack,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun DataVaultScreen(viewModel: ForexViewModel = viewModel()) {
    val aiResponse by viewModel.aiDeployments.collectAsState(initial = null)
    val commandStatus by viewModel.commandCenterStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.syncDataVaultNow()
    }

    val indexedRecords = aiResponse?.count?.toString() ?: "0"
    val decisions = aiResponse?.final_decision ?: emptyList()
    val avgScore = if (decisions.isNotEmpty()) decisions.mapNotNull { it.journal_score }.average() else 0.0
    val trainingWeight = if (avgScore > 0) String.format("%.3f", avgScore / 100.0) else "0.000"
    val lastVector = aiResponse?.last_updated?.takeLast(8)?.replace(":", "")?.let { "PKT-$it" } ?: "PENDING"
    val lastSyncText = aiResponse?.last_updated ?: "NO ASC AI SYNC"
    val backendStatusColor = when {
        commandStatus.isLoading -> Color(0xFFFFC700)
        commandStatus.isConnected == true -> Color(0xFF2EE08A)
        commandStatus.isConnected == false -> Color(0xFFEF4444)
        aiResponse != null -> IndigoAccent
        else -> SlateText
    }
    val backendStatusText = when {
        commandStatus.isLoading -> "SYNCING"
        commandStatus.isConnected == true -> "ASC AI ONLINE"
        commandStatus.isConnected == false -> "ASC AI OFFLINE"
        aiResponse != null -> "ASC AI CACHED"
        else -> "ASC AI UNKNOWN"
    }
    val backendMessage = if (commandStatus.lastMessage == "Idle" && aiResponse != null) {
        "Vault loaded ${aiResponse?.count ?: 0} ASC AI packets"
    } else {
        commandStatus.lastMessage
    }
    val ingestionStatusText = when {
        commandStatus.isLoading -> "NODE SYNC\nRUNNING"
        commandStatus.isConnected == true && decisions.isNotEmpty() -> "NODE HARVESTING\nACTIVE"
        commandStatus.isConnected == true -> "NODE ONLINE\nAWAITING DATA"
        commandStatus.isConnected == false -> "NODE LINK\nOFFLINE"
        else -> "NODE STATUS\nUNKNOWN"
    }
    val emptyStreamMessage = when {
        commandStatus.isConnected == false -> backendMessage
        commandStatus.isLoading -> backendMessage
        else -> "Awaiting ASC AI Live Data..."
    }

    val streamData = decisions.mapIndexed { index, item ->
        val finalState = item.final_trade_state?.uppercase() ?: "REJECTED"
        val confidenceVal = if (finalState == "TRADE_CANDIDATE") {
            (item.final_trade_score?.takeIf { it.isFinite() }?.let { it * 100 } ?: 0.0)
        } else {
            (item.pre_move_ai_score?.takeIf { it.isFinite() }?.let { it * 100 } ?: item.journal_score ?: 0.0)
        }
        val confStr = "${confidenceVal.toInt()}%"
        val statusStr = if (confidenceVal >= 75) "VERIFIED" else if (confidenceVal >= 50) "PENDING" else "REJECT"
        val color = if (confidenceVal >= 75) Color(0xFF2EE08A) else if (confidenceVal >= 50) Color(0xFFFFC700) else Color(0xFF5E7A9E)
        
        TableRow(
            packetId = "PKT-${1000 + index}",
            eventFeature = "${item.journal_label ?: "EVENT"}\n:: ${item.asset_1 ?: "GLOBAL"}",
            confidence = confStr,
            status = statusStr,
            statusColor = color
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card
                item {
                    VaultCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Database icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = Color(0xFF0F3A7D),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "🗄️",
                                        fontSize = 28.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "ASC AI DATA VAULT",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "INTELLIGENCE INDEX &\nAI TRAINING SETS",
                                    color = SlateText,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp
                                )
                            }

                            Column(
                                modifier = Modifier.padding(start = 12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Surface(
                                    color = backendStatusColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(1.dp, backendStatusColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        backendStatusText,
                                        color = backendStatusColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "LAST SYNC",
                                    color = SlateText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    lastSyncText.take(19),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = backendStatusColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, backendStatusColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(backendStatusColor, RoundedCornerShape(5.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        backendStatusText,
                                        color = backendStatusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        backendMessage,
                                        color = SlateText,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Force Vector Sync Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncDataVaultNow() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !commandStatus.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    disabledContainerColor = Color.White.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "⚖️ FORCE VECTOR SYNC",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { viewModel.checkAiHealthNow() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !commandStatus.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureBlack,
                                    disabledContainerColor = PureBlack.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, backendStatusColor.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    "CHECK NODE",
                                    color = backendStatusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.syncCalendarEventsNow() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !commandStatus.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6A00),
                                disabledContainerColor = Color(0xFFFF6A00).copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "📅 SYNC CALENDAR EVENTS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.runAiPipelineNow() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !commandStatus.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoAccent,
                                disabledContainerColor = IndigoAccent.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "RUN ASC AI PIPELINE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Stats Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .background(color = PureBlack, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            color = PureBlack,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(">_", color = IndigoAccent, fontSize = 12.sp, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(indexedRecords, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("INDEXED", color = SlateText, fontSize = 9.sp)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .background(color = PureBlack, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            color = PureBlack,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ϕ", color = Color(0xFF2EE08A), fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(trainingWeight, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("WEIGHT", color = SlateText, fontSize = 9.sp)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .background(color = PureBlack, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            color = PureBlack,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⏱️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(lastVector, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("VECTOR", color = SlateText, fontSize = 9.sp)
                            }
                        }
                    }
                }

                // Live Ingestion Stream
                item {
                    VaultCard {
                        // Header with status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "LIVE INGESTION\nSTREAM",
                                    color = SlateText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = backendStatusColor,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    ingestionStatusText,
                                    color = backendStatusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Divider line above table header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = Color.White.copy(alpha = 0.3f))
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "PACKET\nID",
                                color = SlateText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp,
                                modifier = Modifier.width(60.dp)
                            )
                            Text(
                                "EVENT\nFEATURE",
                                color = SlateText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "CONFIDENCE",
                                color = SlateText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(70.dp)
                            )
                            Text(
                                "TRAIN\nSTA",
                                color = SlateText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp,
                                modifier = Modifier.width(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = Color.White.copy(alpha = 0.3f))
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Table Rows
                        if (streamData.isEmpty()) {
                            Text(
                                emptyStreamMessage,
                                color = if (commandStatus.isConnected == false) Color(0xFFEF4444) else SlateText,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            streamData.forEach { row ->
                                StreamTableRow(row)
                            }
                        }
                    }
                }

                // Vectorization Disclosure
                item {
                    VaultCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🛡️", fontSize = 18.sp)
                            Text(
                                "VECTORIZATION DISCLOSURE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "THE NODE DATA VAULT ARCHIVES CROSS-ASSET TECHNICAL AND FUNDAMENTAL FEATURE SETS FROM ASC AI. THESE PACKETS SERVE AS THE PRIMARY TRUTH SOURCE FOR LOCAL MODEL REFINEMENT. NO PERSONALLY IDENTIFIABLE TRADING DATA IS VECTORIZED; THE SYSTEM STRICTLY PROCESSES ANONYMIZED STRUCTURAL FOOTPRINTS TO ENHANCE PREDICTIVE ALIGNMENT.",
                            color = SlateText,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

data class TableRow(
    val packetId: String,
    val eventFeature: String,
    val confidence: String,
    val status: String,
    val statusColor: Color
)

@Composable
fun StreamTableRow(row: TableRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            row.packetId,
            color = IndigoAccent,
            fontSize = 10.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )

        Text(
            row.eventFeature,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = InterFontFamily,
            lineHeight = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            row.confidence,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp)
        )

        Text(
            row.status,
            color = row.statusColor,
            fontSize = 9.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
    }
}
