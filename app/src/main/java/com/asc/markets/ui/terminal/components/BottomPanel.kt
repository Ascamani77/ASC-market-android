package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.asc.markets.ui.terminal.theme.*

@Composable
fun BottomPanel(
    activeTab: String = "Trading Panel",
    onTabChange: (String) -> Unit = {}
) {
    val tabs = listOf("Stock Screener", "Pine Editor", "Strategy Tester", "Trading Panel")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        color = DarkSurface
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
            
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(activeTab),
                containerColor = DarkSurface,
                contentColor = AccentBlue,
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (tabs.indexOf(activeTab) != -1) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(activeTab)]),
                            color = AccentBlue,
                            height = 2.dp
                        )
                    }
                }
            ) {
                tabs.forEach { tab ->
                    val isSelected = activeTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { onTabChange(tab) },
                        text = {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    )
                }
            }
        }
    }
}
