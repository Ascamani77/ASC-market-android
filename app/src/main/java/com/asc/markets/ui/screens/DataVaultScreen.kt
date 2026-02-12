package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                        "NODE DATA VAULT",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content: Code-block aesthetics with Intelligence Packets
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("INTELLIGENCE PACKETS", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Feature Sets
                listOf(
                    Triple("SIGNAL_SET", listOf("RSI_14", "MACD", "VWAP"), 78),
                    Triple("MACRO_CONTEXT", listOf("INFLATION", "GDP_TREND", "SENTIMENT"), 85),
                    Triple("OUTCOME_SET", listOf("ENTRY_PROFIT", "EXIT_TIME", "CORRELATION"), 92)
                ).forEach { (featureName, features, vectorization) ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0B0B), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                "{\n  \"feature_set\": \"$featureName\",",
                                color = Color(0xFF2EE08A),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            features.forEach { feature ->
                                Text(
                                    "    \"field\": \"$feature\",",
                                    color = Color(0xFF90CAF9),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "  \"vectorization\": ",
                                    color = Color(0xFFFFC700),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                LinearProgressIndicator(
                                    progress = vectorization / 100f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .padding(horizontal = 4.dp),
                                    color = Color(0xFF2EE08A),
                                    trackColor = Color(0xFF1B1B1B)
                                )
                                Text(
                                    "$vectorization%",
                                    color = SlateText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                "}",
                                color = Color(0xFF2EE08A),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("LEARNING WEIGHTS", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B0B0B), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        listOf(
                            Pair("train_loss", 0.0234f),
                            Pair("val_loss", 0.0312f),
                            Pair("accuracy", 0.9467f)
                        ).forEach { (metric, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(metric, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(String.format("%.4f", value), color = Color(0xFF2EE08A), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
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
