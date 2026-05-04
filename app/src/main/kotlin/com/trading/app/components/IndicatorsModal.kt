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
    var currentView by remember { mutableStateOf("Main") } // "Main" or "Technicals"
    val indicators = listOf("EMA", "VWAP", "Bollinger Bands", "RSI", "ATR", "MACD", "Stochastic", "Volume")

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
                            IndicatorCategoryItem(Icons.Outlined.BookmarkBorder, "Editors' picks")
                            IndicatorCategoryItem(Icons.Outlined.TrendingUp, "Top")
                            IndicatorCategoryItem(Icons.Outlined.Whatshot, "Trending")
                        }
                    }
                } else {
                    // Indicators List (Filtered or Technicals sub-menu)
                    val displayList = if (searchQuery.isNotEmpty()) {
                        indicators.filter { it.contains(searchQuery, ignoreCase = true) }
                    } else {
                        indicators
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayList) { indicator ->
                            val isAiIndicator = indicator in listOf("RSI", "ATR", "MACD")
                            val textColor = if (isAiIndicator) Color(0xFF434651) else Color.White
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAiIndicator) { 
                                        onIndicatorSelect(indicator)
                                    }
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(indicator, color = textColor, fontSize = 16.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                if (isAiIndicator) {
                                    Surface(
                                        color = Color(0xFF131722),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            "AI ACTIVE",
                                            color = Color(0xFF434651),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Outlined.StarBorder, 
                                    null, 
                                    tint = if (isAiIndicator) Color(0xFF2A2E39) else Color(0xFF787B86)
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
