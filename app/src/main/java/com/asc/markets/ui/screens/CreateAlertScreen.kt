package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import com.asc.markets.data.AppView
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.VigilanceNode
import com.asc.markets.logic.VigilanceNodeEngine
import com.asc.markets.ui.theme.*

@Composable
fun CreateAlertScreen(viewModel: ForexViewModel) {
    val scrollState = rememberScrollState()
    val createdAlerts = remember { mutableStateListOf<VigilanceNode>() }
    val selectedPair by viewModel.selectedPair.collectAsState()
    
    var chosenAsset by remember { mutableStateOf(selectedPair.symbol) }
    LaunchedEffect(selectedPair.symbol) { chosenAsset = selectedPair.symbol }
    
    val allAssets = listOf("EUR/USD","GBP/USD","USD/JPY","AUD/USD","USD/CAD","USD/CHF","NZD/USD","EUR/GBP","EUR/JPY","GBP/JPY",
        "AAPL","MSFT","GOOGL","AMZN","TSLA",
        "SPX/500","NAS100","DOW30",
        "XAU/USD","XAG/USD","WTI",
        "BTC/USDT","ETH/USDT","BNB/USDT")
    var assetExpanded by remember { mutableStateOf(false) }

    val scaleStages = remember {
        listOf("Noise", "Structure", "Compression", "Pre move", "Expansion")
    }
    val selectedStages = remember {
        mutableStateMapOf(
            "Noise" to false,
            "Structure" to false,
            "Compression" to false,
            "Pre move" to false,
            "Expansion" to false
        )
    }

    fun registerAlert(node: VigilanceNode) {
        createdAlerts.add(0, node)
        viewModel.registerVigilanceNode(node, prefix = "Alert created")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AddAlert, null, tint = IndigoAccent, modifier = Modifier.size(22.dp))
                }
            }
            Column {
                Text("CREATE ALERT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("AI PROGRESSIVE SCALE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = Color.White.copy(alpha = 0.02f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SELECTED ASSET", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { assetExpanded = true },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(chosenAsset, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = assetExpanded,
                        onDismissRequest = { assetExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E)).fillMaxHeight(0.5f)
                    ) {
                        allAssets.forEach { asset ->
                            DropdownMenuItem(
                                text = { Text(asset, color = Color.White, fontSize = 12.sp, fontFamily = InterFontFamily) },
                                onClick = {
                                    chosenAsset = asset
                                    assetExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = Color.White.copy(alpha = 0.02f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AI PROGRESSIVE SCALE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("Tick any scale stage below. When progression reaches a selected stage, the alert is armed for that level.", color = SlateText, fontSize = 10.sp, lineHeight = 15.sp, fontFamily = InterFontFamily)

                scaleStages.forEach { stage ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStages[stage] = !(selectedStages[stage] ?: false)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = selectedStages[stage] ?: false,
                            onCheckedChange = { checked -> selectedStages[stage] = checked },
                            colors = CheckboxDefaults.colors(
                                checkedColor = IndigoAccent,
                                uncheckedColor = SlateText,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(stage, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    }
                }

                Button(
                    onClick = {
                        scaleStages.filter { selectedStages[it] == true }.forEach { stage ->
                            val node = VigilanceNodeEngine.createSimpleAlert(
                                pair = chosenAsset,
                                trigger = "AI_PROGRESSIVE_SCALE",
                                timeframe = "H1",
                                phaseState = stage,
                                cooldownMinutes = 15
                            )
                            registerAlert(node)
                        }
                    },
                    enabled = scaleStages.any { selectedStages[it] == true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
                ) {
                    Text("CREATE ALERT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppView.ALERTS) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text("VIEW FULL VIGILANCE SETUP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
                }
            }
        }

        if (createdAlerts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                color = EmeraldSuccess.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(EmeraldSuccess, RoundedCornerShape(4.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${createdAlerts.size} ALERT${if (createdAlerts.size > 1) "S" else ""} CREATED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("View and manage in My Alerts", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
