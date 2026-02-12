package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.SlateText

@Composable
fun DataVaultScreen(viewModel: ForexViewModel = viewModel()) {
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
                                        "NODE DATA VAULT",
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
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Force Vector Sync Button
                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "⚖️ FORCE VECTOR SYNC",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Stats Cards
                item {
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
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                ">_",
                                color = IndigoAccent,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "42,401",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "INDEXED RECORDS",
                                color = SlateText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
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
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "ϕ",
                                color = Color(0xFF2EE08A),
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "0.942",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "TRAINING WEIGHT",
                                color = SlateText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
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
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "⏱️",
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "PKT-942",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "LAST VECTOR",
                                color = SlateText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Live Ingestion Stream
                item {
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
                                                color = Color(0xFF2EE08A),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Text(
                                        "NODE HARVESTING\nACTIVE",
                                        color = Color(0xFF2EE08A),
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
                                    modifier = Modifier.width(70.dp)
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
                                    modifier = Modifier.width(80.dp)
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
                            listOf(
                                TableRow("PKT-942", "SMC_BOS\n:: EUR/USD", "92%", "VERIFIED", Color(0xFF2EE08A)),
                                TableRow("PKT-941", "LIQ_SWEEP\n:: XAU/USD", "84%", "PENDING", Color(0xFFFFC700)),
                                TableRow("PKT-940", "FVG_GAP\n:: GBP/USD", "71%", "REJECT", Color(0xFF5E7A9E))
                            ).forEach { row ->
                                StreamTableRow(row)
                            }
                        }
                    }
                }

                // Vectorization Disclosure
                item {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "🛡️",
                                    fontSize = 18.sp
                                )
                                Text(
                                    "VECTORIZATION DISCLOSURE",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "THE NODE DATA VAULT ARCHIVES CROSS-ASSET TECHNICAL AND FUNDAMENTAL FEATURE SETS. THESE PACKETS SERVE AS THE PRIMARY TRUTH SOURCE FOR LOCAL MODEL REFINEMENT. NO PERSONALLY IDENTIFIABLE TRADING DATA IS VECTORIZED; THE SYSTEM STRICTLY PROCESSES ANONYMIZED STRUCTURAL FOOTPRINTS TO ENHANCE PREDICTIVE ALIGNMENT.",
                                color = SlateText,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
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
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp)
        )

        Text(
            row.eventFeature,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            row.confidence,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )

        Text(
            row.status,
            color = row.statusColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
    }
}
