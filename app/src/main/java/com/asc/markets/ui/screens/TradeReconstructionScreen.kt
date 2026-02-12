package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun TradeReconstructionScreen(viewModel: ForexViewModel = viewModel()) {
    val selectedTrade = remember { mutableStateOf<Int?>(null) }
    val trades = listOf(
        Triple("2024-02-12 14:32:45.123", "EUR/USD LONG +2.35M", "Closed +$1,245"),
        Triple("2024-02-11 09:15:22.456", "GBP/USD SHORT -1.82M", "Closed +$847"),
        Triple("2024-02-10 16:47:10.789", "BTC/USDT LONG +0.5M", "Active")
    )

    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
            // Header
            Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "DEEP AUDIT",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTrade.value == null) {
                    item {
                        Text("BLACK BOX RECONSTRUCTION", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    items(trades.indices.toList()) { index ->
                        val (timestamp, info, status) = trades[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0B0B), shape = RoundedCornerShape(6.dp))
                                .clickable { selectedTrade.value = index }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(timestamp, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(info, color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(status, color = if (status == "Active") Color(0xFFFFC700) else Color(0xFF2EE08A), fontSize = 10.sp)
                        }
                    }
                } else {
                    val (timestamp, info, status) = trades[selectedTrade.value!!]
                    item {
                        // Market Snapshot
                        Text("MARKET SNAPSHOT @ $timestamp", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0B0B), shape = RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Market Bias", color = Color.White, fontSize = 10.sp)
                                Text("BULLISH", color = Color(0xFF2EE08A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("News Guard", color = Color.White, fontSize = 10.sp)
                                Text("GREEN", color = Color(0xFF2EE08A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Volatility Heatmap", color = Color.White, fontSize = 10.sp)
                                Text("MODERATE", color = Color(0xFFFFC700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Text("EXECUTION LOGIC LOGS", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0B0B), shape = RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            listOf(
                                "[14:32:45.123] Signal triggered: RSI oversold detected",
                                "[14:32:45.234] News Guard check: PASS (no major events)",
                                "[14:32:45.456] Entry condition verified: LONG setup confirmed",
                                "[14:32:45.678] Position opened: 2.35M @ 1.0945"
                            ).forEach { log ->
                                Text(
                                    log,
                                    color = Color(0xFF90CAF9),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedTrade.value = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(32.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2d2d2d)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("BACK TO LIST", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(),
    shape: androidx.compose.foundation.shape.RoundedCornerShape,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        shape = shape,
        content = content
    )
}
