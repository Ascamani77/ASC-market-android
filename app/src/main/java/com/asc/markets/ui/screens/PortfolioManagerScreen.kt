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
fun PortfolioManagerScreen(viewModel: ForexViewModel = viewModel()) {
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
                                    // Briefcase icon
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
                                            "💼",
                                            fontSize = 28.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        "INVENTORY\nMANAGER",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 22.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        "OPERATIONAL EXPOSURE HUB",
                                        color = SlateText,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "UNREALIZED PNL",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "+$19.36",
                                        color = Color(0xFF2EE08A),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "MARGIN USED",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "4.2%",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Inventory Card
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "📈",
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "LIVE INVENTORY",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    "1 NODES DISPATCHED",
                                    color = SlateText,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Position Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFF1A1A2E),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                color = Color(0xFF1A1A2E),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Linked pairs icon
                                        Text(
                                            "🔗",
                                            fontSize = 18.sp
                                        )

                                        Column {
                                            Text(
                                                "EUR/USD",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                "BUY 1.00\nLOT",
                                                color = Color(0xFF2EE08A),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column {
                                            Text(
                                                "ENTRY",
                                                color = SlateText,
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                "1.0842",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Column {
                                            Text(
                                                "NET PNL",
                                                color = SlateText,
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                "+$142.00",
                                                color = Color(0xFF2EE08A),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Close button
                                    Text(
                                        "✕",
                                        color = SlateText,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // NET USD DELTA Card
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
                            // Header with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚖️",
                                    fontSize = 18.sp
                                )
                                Text(
                                    "NET USD DELTA",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Institutional Load
                            Text(
                                "INSTITUTIONAL LOAD",
                                color = SlateText,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = 0.4f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp),
                                    color = Color(0xFF5E9cff),
                                    trackColor = Color(0xFF1B1B2F)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    "0.0L",
                                    color = IndigoAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Leverage and Exposure
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "LEVERAGE",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "1.2x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column {
                                    Text(
                                        "EXPOSURE",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "LOW",
                                        color = Color(0xFF2EE08A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // PROTOCOL GUARD Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF1A0A0A),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        color = Color(0xFF1A0A0A),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header with warning icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚠️",
                                    fontSize = 18.sp
                                )
                                Text(
                                    "PROTOCOL GUARD",
                                    color = Color(0xFFE53935),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "EMERGENCY VETO WILL DISARM ALL AUTOMATED DISPATCHES IF UNREALIZED DRAWDOWN EXCEEDS 2.5% OF LOCAL COLLATERAL.",
                                color = SlateText,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "KILL-SWITCH ENABLED",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // INVENTORY MANAGEMENT POLICY Card
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
                            // Header with info icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "ℹ️",
                                    fontSize = 18.sp
                                )
                                Text(
                                    "INVENTORY MANAGEMENT POLICY",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "INVENTORY DISPLAYS THEORETICAL POSITIONING FOR THE CURRENT NODE SESSION. REAL-WORLD LIQUIDITY, SLIPPAGE, AND SPREAD FLUCTUATIONS ARE MONITORED BUT NOT CONTROLLED BY THIS DASHBOARD. RISK GOVERNANCE IS MAINTAINED BY THE PROP GUARD INTERLOCK.",
                                color = SlateText,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
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
