package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.ChartSettings

@Composable
fun SettingsModal(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onTimeZoneClick: () -> Unit,
    onClose: () -> Unit,
    initialTab: String? = null
) {
    var activeSubModal by remember { mutableStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    val settingsOptions = listOf(
        Triple("Symbol", Icons.Default.CandlestickChart, "Symbol"),
        Triple("Status line", Icons.Default.Notes, "Status line"),
        Triple("Scales and lines", Icons.Default.Straighten, "Scales and lines"),
        Triple("Canvas", Icons.Default.Edit, "Canvas"),
        Triple("Trading", Icons.Default.TrendingUp, "Trading"),
        Triple("Alerts", Icons.Default.NotificationsNone, "Alerts"),
        Triple("Events", Icons.Default.CalendarToday, "Events")
    )

    // Comprehensive search list with all sub-items
    val allSearchItems = listOf(
        // Symbol Settings
        SearchItem("Body", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("Borders", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("Wick", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("Open", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("High", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("Low", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("Close", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("Precision", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        SearchItem("Timezone", "Symbol", "Symbol", Icons.Default.CandlestickChart),
        
        // Status Line Settings
        SearchItem("Logo", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Symbol display", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Open market status", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Chart values", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("OHLC", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Bar change values", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Volume", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Last day change", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Indicator titles", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Indicator inputs", "Status line", "Status line", Icons.Default.Notes),
        SearchItem("Indicator values", "Status line", "Status line", Icons.Default.Notes),
        
        // Scales and Lines Settings
        SearchItem("Grid", "Scales and lines", "Scales and lines", Icons.Default.Straighten),
        SearchItem("Crosshair", "Scales and lines", "Scales and lines", Icons.Default.Straighten),
        SearchItem("Scale", "Scales and lines", "Scales and lines", Icons.Default.Straighten),
        
        // Canvas Settings
        SearchItem("Background", "Canvas", "Canvas", Icons.Default.Edit),
        SearchItem("Grid color", "Canvas", "Canvas", Icons.Default.Edit),
        SearchItem("Margins", "Canvas", "Canvas", Icons.Default.Edit),
        SearchItem("Pane", "Canvas", "Canvas", Icons.Default.Edit),
        
        // Trading Settings
        SearchItem("One-tap trading", "Trading", "Trading", Icons.Default.TrendingUp),
        SearchItem("Buy/Sell buttons", "Trading", "Trading", Icons.Default.TrendingUp),
        SearchItem("Rejection notifications", "Trading", "Trading", Icons.Default.TrendingUp),
        
        // Alerts Settings
        SearchItem("Price alerts", "Alerts", "Alerts", Icons.Default.NotificationsNone),
        
        // Events Settings
        SearchItem("News", "Events", "Events", Icons.Default.CalendarToday),
        SearchItem("Earnings", "Events", "Events", Icons.Default.CalendarToday),
        SearchItem("Splits", "Events", "Events", Icons.Default.CalendarToday),
        SearchItem("Dividends", "Events", "Events", Icons.Default.CalendarToday)
    )

    val filteredOptions = if (searchQuery.isEmpty()) {
        settingsOptions.map { SearchItem(it.first, it.first, it.third, it.second) }
    } else {
        allSearchItems.filter { 
            it.label.contains(searchQuery, ignoreCase = true) || 
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (showSearchResults && searchQuery.isNotEmpty()) {
                    val searchResults = allSearchItems.filter { 
                        it.label.contains(searchQuery, ignoreCase = true) || 
                        it.category.contains(searchQuery, ignoreCase = true)
                    }
                    SearchResultsPage(
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        onResultSelect = { result ->
                            activeSubModal = result.categoryKey
                            showSearchResults = false
                        },
                        onClose = { showSearchResults = false }
                    )
                } else if (activeSubModal == null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Settings",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onClose) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF787B86),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Search Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF787B86),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search settings",
                                            color = Color(0xFF787B86),
                                            fontSize = 14.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                        cursorBrush = SolidColor(Color.White),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { 
                                            if (searchQuery.isNotEmpty()) {
                                                showSearchResults = true
                                            }
                                        }),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF787B86),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }

                        // Search Suggestions Dropdown
                        if (searchQuery.isNotEmpty() && filteredOptions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(8.dp)),
                                color = Color(0xFF121212),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    filteredOptions.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { activeSubModal = item.categoryKey }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                item.icon,
                                                contentDescription = null,
                                                tint = Color(0xFF2962FF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.label, color = Color.White, fontSize = 14.sp)
                                                Text(item.category, color = Color(0xFF787B86), fontSize = 11.sp)
                                            }
                                        }
                                        if (item != filteredOptions.last()) {
                                            Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Scrollable Settings List
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (searchQuery.isEmpty()) {
                                // Show all main categories when search is empty
                                settingsOptions.forEach { (label, icon, key) ->
                                    SettingsItem(label, icon) { activeSubModal = key }
                                }
                            } else if (filteredOptions.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No results found", color = Color(0xFF787B86))
                                }
                            }
                        }

                        // Footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp, 36.dp)
                                    .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(8.dp))
                                    .clickable { },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = onClose,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, Color(0xFF2A2E39)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Cancel", color = Color.White, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = onClose,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Ok", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    when (activeSubModal) {
                        "Symbol" -> SymbolSettingsModal(settings, onUpdate, onTimeZoneClick) { activeSubModal = null }
                        "Status line" -> StatusLineSettingsModal(settings, onUpdate) { activeSubModal = null }
                        "Scales and lines" -> ScalesAndLinesSettingsModal(settings, onUpdate) { activeSubModal = null }
                        "Canvas" -> CanvasSettingsModal(settings, onUpdate) { activeSubModal = null }
                        "Trading" -> TradingSettingsModal(settings, onUpdate) { activeSubModal = null }
                        "Alerts" -> AlertsSettingsModal(settings, onUpdate) { activeSubModal = null }
                        "Events" -> EventsSettingsModal(settings, onUpdate) { activeSubModal = null }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFFD1D4DC), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
    }
}
