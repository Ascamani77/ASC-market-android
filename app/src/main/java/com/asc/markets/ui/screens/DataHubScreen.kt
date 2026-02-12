package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
fun DataHubScreen(viewModel: ForexViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
            // Main scrollable content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Unified Data Bus Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = PureBlack,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Network icon representation
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
                                        "⚡",
                                        fontSize = 28.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "UNIFIED DATA\nBUS",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "CORE REPOSITORY\nHEARTBEAT",
                                    color = SlateText,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp
                                )
                            }

                            Column(
                                modifier = Modifier.padding(start = 16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "TOTAL THROUGHPUT",
                                    color = SlateText,
                                    fontSize = 10.sp
                                )

                                Text(
                                    "6,022.571",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "t/s",
                                    color = SlateText,
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "AGG. LATENCY",
                                    color = SlateText,
                                    fontSize = 10.sp
                                )

                                Text(
                                    "12ms",
                                    color = IndigoAccent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Relay Cards
                listOf(
                    RelayData("LMAX NY4 RELAY", 10.32, 20.5, "LMAX"),
                    RelayData("BINANCE AGGREGATOR", 7.31, 99.1, "BINANCE"),
                    RelayData("MT5 BRIDGE", 42.81, 4.3, "MT5")
                ).forEach { relay ->
                    item {
                        RelayCard(relay)
                    }
                }

                // Repository Flow Monitor
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "REPOSITORY FLOW MONITOR",
                        color = IndigoAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF0B0B0B),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        color = Color(0xFF0B0B0B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LogLine("[INFO]", "BUS_SYNCHRONIZED: LMAX_NY4 -> LOCAL_BUFFER", Color(0xFF90CAF9))
                            LogLine("[DATA]", "PRICE_TICK: EUR/USD @ 1.08451 | SIZE: 12.5M", Color(0xFF2EE08A))
                            LogLine("[DATA]", "PRICE_TICK: GBP/USD @ 1.26342 | SIZE: 4.2M", Color(0xFF2EE08A))
                            LogLine("[FLOW]", "BUFFER_REBALANCE: FLUSHING_OLD_FRAMES...", Color(0xFFFFC700))
                            LogLine("[INFO]", "INTEGRITY_OK: ALL_NODES_ALIGNED", Color(0xFF90CAF9))
                            LogLine("[WAIT]", "LISTENING_FOR_EVENT_REDUX...", Color(0xFF9E9E9E))
                        }
                    }
                }

                // Throughput Pulse
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = PureBlack,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "THROUGHPUT PULSE",
                                color = IndigoAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Circular progress indicator
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .drawBehind {
                                        // Outer circle (track)
                                        drawCircle(
                                            color = Color(0xFF2A2A4E),
                                            radius = 60.dp.toPx()
                                        )
                                        // Inner circle (progress)
                                        val sweepAngle = 260f // 72% of 360
                                        drawArc(
                                            color = IndigoAccent,
                                            startAngle = -90f,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            size = androidx.compose.ui.geometry.Size(120.dp.toPx(), 120.dp.toPx())
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "72%",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "BUFFER LOAD",
                                        color = SlateText,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "NODE CAPACITY OPTIMIZED FOR 100K\nEVENTS/SEC. CURRENT LOAD IS NOMINAL\nFOR STANDARD LIQUIDITY SESSIONS.",
                                color = SlateText,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
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

data class RelayData(
    val title: String,
    val latency: Double,
    val buffer: Double,
    val id: String
)

@Composable
fun RelayCard(relay: RelayData) {
    val bufferColor = when {
        relay.buffer > 80 -> Color(0xFFFFC700)
        else -> Color(0xFF2EE08A)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PureBlack,
                shape = RoundedCornerShape(12.dp)
            ),
        color = PureBlack,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
            // Header with title and status dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    relay.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Green status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color(0xFF00D050),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Latency
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LATENCY",
                    color = SlateText,
                    fontSize = 10.sp
                )

                Text(
                    "${relay.latency}MS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Latency progress bar
            LinearProgressIndicator(
                progress = (relay.latency / 50).coerceAtMost(1.0).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFF5E9cff),
                trackColor = Color(0xFF1B1B2F)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Buffer Saturation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SATURATING BUFFER",
                    color = SlateText,
                    fontSize = 10.sp
                )

                Text(
                    "${relay.buffer}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Buffer progress bar
            LinearProgressIndicator(
                progress = (relay.buffer / 100).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = bufferColor,
                trackColor = Color(0xFF1B1B2F)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer with ID and ACTIVE RELAY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ID  ${relay.id}",
                    color = SlateText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    "ACTIVE RELAY",
                    color = SlateText,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun LogLine(tag: String, message: String, tagColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            tag,
            color = tagColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )

        Text(
            message,
            color = SlateText,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}
