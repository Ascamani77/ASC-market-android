package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun IndicatorsModal(
    onClose: () -> Unit,
    onIndicatorSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentView by remember { mutableStateOf("Main") } // "Main", "Technicals", "EditorsPicks"
    val indicators = listOf("EMA", "VWAP", "Bollinger Bands", "RSI", "ATR", "MACD", "Stochastic", "Volume")
    val editorsPicks = listOf(
        "Premium & Discount Delta Volume [BigBeluga]",
        "Fair Value Gap [LuxAlgo]",
        "Supply and Demand Daily [LuxAlgo]",
        "OTE visible chart [twingall]",
        "Auto Fib Retracement",
        "Confluence FVG Finder"
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentView != "Main") {
                            IconButton(onClick = { currentView = "Main" }) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                            }
                        }
                        Text(
                            text = if (currentView == "Main") "Indicators, metrics, and strategies" else "Technicals",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search", color = Color(0xFF787B86), fontSize = 16.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF787B86)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF131722),
                        unfocusedContainerColor = Color(0xFF131722),
                        focusedBorderColor = Color(0xFF363A45),
                        unfocusedBorderColor = Color(0xFF363A45),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                if (currentView == "Main" && searchQuery.isEmpty()) {
                    // Main Categories View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        IndicatorCategorySection("PERSONAL") {
                            IndicatorCategoryItem(Icons.Outlined.StarBorder, "Favorites")
                            IndicatorCategoryItem(Icons.Outlined.People, "Invite-only")
                        }

                        IndicatorCategorySection("BUILT-IN") {
                            IndicatorCategoryItem(Icons.Outlined.ShowChart, "Technicals") {
                                currentView = "Technicals"
                            }
                            IndicatorCategoryItem(Icons.Outlined.BarChart, "Fundamentals")
                        }

                        IndicatorCategorySection("COMMUNITY") {
                            IndicatorCategoryItem(Icons.Outlined.BookmarkBorder, "Editors' picks") {
                                currentView = "EditorsPicks"
                            }
                            IndicatorCategoryItem(Icons.Outlined.TrendingUp, "Top")
                            IndicatorCategoryItem(Icons.Outlined.Whatshot, "Trending")
                        }
                    }
                } else {
                    // Determine which list to show
                    val isEditorsView = currentView == "EditorsPicks"
                    val baseList = when {
                        isEditorsView -> editorsPicks
                        searchQuery.isNotEmpty() -> indicators.filter { it.contains(searchQuery, ignoreCase = true) } + editorsPicks.filter { it.contains(searchQuery, ignoreCase = true) }
                        else -> indicators
                    }
                    val displayList = baseList

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (isEditorsView) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text("Editors' picks", color = Color(0xFF787B86), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Curated by TradingView editors — as published (CC BY-NC-SA 4.0). BigBeluga Premium & Discount • LuxAlgo FVG • LuxAlgo Supply/Demand Daily • twingall OTE visible chart.", color = Color(0xFF787B86), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                        items(displayList) { indicator ->
                            val isEditorsPick = indicator in editorsPicks

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onIndicatorSelect(indicator)
                                    }
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(indicator, color = Color.White, fontSize = 16.sp)
                                    if (isEditorsPick) {
                                        val sub = when {
                                            indicator.contains("Confluence", ignoreCase = true) -> "Multi-TF Confluence FVG Finder • overlay=true • Merged 60/120/240m Zones • Strength Rating + Session Labels"
                                            indicator.contains("Auto Fib", ignoreCase = true) -> "TradingView • Auto Fib Retracement • overlay=true • ATR-deviation ZigZag • 0-1 Fib Levels"
                                            indicator.contains("OTE", ignoreCase = true) -> "twingall • OTE visible chart • overlay=true • BasicVisibleChart • 61.8-78.6% Fib Box"
                                            indicator.contains("Supply and Demand", ignoreCase = true) -> "LuxAlgo • Supply and Demand Daily • overlay=true • max 500 boxes/lines"
                                            indicator.contains("LuxAlgo", ignoreCase = true) -> "LuxAlgo • Fair Value Gap • overlay=true • max 500 boxes/lines"
                                            else -> "BigBeluga • Premium & Discount Delta Volume • overlay=true"
                                        }
                                        Text(sub, color = Color(0xFF787B86), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                                if (isEditorsPick) {
                                    Surface(
                                        color = Color(0xFF2962FF).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            "PREMIUM",
                                            color = Color(0xFF2962FF),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Outlined.StarBorder,
                                    null,
                                    tint = Color(0xFF787B86)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorCategorySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            color = Color(0xFF787B86),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun IndicatorCategoryItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
    }
}
