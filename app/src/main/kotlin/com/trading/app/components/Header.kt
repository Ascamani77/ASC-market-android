package com.trading.app.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.ChartSettings
import com.trading.app.models.SymbolInfo

@Composable
fun Header(
    symbol: String,
    timeframe: String,
    chartStyle: String,
    onSymbolClick: () -> Unit,
    onTimeframeClick: (String) -> Unit,
    onStyleChange: (String) -> Unit,
    onIndicatorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSearchClick: () -> Unit,
    onRightPanelToggle: () -> Unit,
    isRightPanelVisible: Boolean,
    isAnalyzing: Boolean = false,
    onDownloadChart: () -> Unit = {},
    backgroundColor: Color = Color(0xFF08090C),
    settings: ChartSettings = ChartSettings(),
    isAtBottom: Boolean = true,
    onGoToClick: () -> Unit = {},
    onNewsClick: () -> Unit = {},
    onLayersClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onDrawingClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onTradeClick: () -> Unit = {},
    onSellClick: () -> Unit = {},
    onBuyClick: () -> Unit = {},
    onLotSizeChange: (String) -> Unit = {},
    onCurrencyClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showTimeframeMenu by remember { mutableStateOf(false) }
    
    val fontSize = (settings.canvas.headerFontSize + 4).sp
    val fontWeight = if (settings.canvas.headerFontBold) FontWeight.Bold else FontWeight.Medium
    val secondaryWhite = Color(0xFFD1D4DC)

    Column(modifier = Modifier.fillMaxWidth()) {
        /*if (isAtBottom) {
            Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFF2A2E39).copy(alpha = 0.3f))
        }*/
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.4.dp)
                .background(backgroundColor)
                .padding(start = 1.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. STATIC LEFT: Asset Pair Button and Timeframe selector
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Symbol button
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { onSymbolClick() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(symbol, color = secondaryWhite, fontWeight = FontWeight.Bold, fontSize = fontSize)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Timeframe Selection (Now static)
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showTimeframeMenu = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when(timeframe) {
                                "1D", "D" -> "D"
                                "1W", "W" -> "W"
                                "1M", "M" -> "M"
                                else -> timeframe
                            },
                            color = secondaryWhite,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            null,
                            tint = Color(0xFF787B86),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showTimeframeMenu,
                        onDismissRequest = { showTimeframeMenu = false },
                        modifier = Modifier.background(Color(0xFF121212)).width(220.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, null, tint = secondaryWhite, modifier = Modifier.size(26.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Add custom interval...", color = secondaryWhite, fontSize = 14.sp)
                                }
                            },
                            onClick = { showTimeframeMenu = false }
                        )

                        TimeframeHeader("TICKS")
                        TimeframeItem("1 tick", "1t", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("10 ticks", "10t", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("100 ticks", "100t", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("1000 ticks", "1000t", timeframe, onTimeframeClick) { showTimeframeMenu = false }

                        Divider(color = Color(0xFF2A2E39), modifier = Modifier.padding(vertical = 4.dp))

                        TimeframeHeader("SECONDS")
                        TimeframeItem("1 second", "1s", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("5 seconds", "5s", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("10 seconds", "10s", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("15 seconds", "15s", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("30 seconds", "30s", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("45 seconds", "45s", timeframe, onTimeframeClick) { showTimeframeMenu = false }

                        Divider(color = Color(0xFF2A2E39), modifier = Modifier.padding(vertical = 4.dp))

                        TimeframeHeader("MINUTES")
                        TimeframeItem("1 minute", "1m", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("2 minutes", "2m", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("3 minutes", "3m", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("5 minutes", "5m", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("10 minutes", "10m", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("15 minutes", "15m", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("30 minutes", "30m", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("45 minutes", "45m", timeframe, onTimeframeClick) { showTimeframeMenu = false }

                        Divider(color = Color(0xFF2A2E39), modifier = Modifier.padding(vertical = 4.dp))

                        TimeframeHeader("HOURS")
                        TimeframeItem("1 hour", "1h", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("2 hours", "2h", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("3 hours", "3h", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("4 hours", "4h", timeframe, onTimeframeClick) { showTimeframeMenu = false }

                        Divider(color = Color(0xFF2A2E39), modifier = Modifier.padding(vertical = 4.dp))

                        TimeframeHeader("DAYS")
                        TimeframeItem("1 day", "1D", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("1 week", "1W", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("1 month", "1M", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("3 months", "3M", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("6 months", "6M", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("12 months", "12M", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("Year to date", "YTD", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("5 years", "5Y", timeframe, onTimeframeClick) { showTimeframeMenu = false }

                        Divider(color = Color(0xFF2A2E39), modifier = Modifier.padding(vertical = 4.dp))

                        TimeframeHeader("RANGES")
                        TimeframeItem("1 range", "1r", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("10 ranges", "10r", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                        TimeframeItem("100 ranges", "100r", timeframe, onTimeframeClick) { showTimeframeMenu = false }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Currency Exchange Icon
                IconButton(onClick = onCurrencyClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.CurrencyExchange,
                        contentDescription = "Currency",
                        tint = secondaryWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. SCROLLABLE SECTION
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Extra space between timeframe and GoTo icon
                Spacer(modifier = Modifier.width(19.2.dp))

                IconButton(onClick = onGoToClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.DateRange, "GoTo", tint = secondaryWhite, modifier = Modifier.size(26.dp))
                }

                // HeaderDivider()

                // DRAWING ICON
                IconButton(onClick = onDrawingClick, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Outlined.Edit, "Drawings", tint = secondaryWhite, modifier = Modifier.size(26.dp))
                }

                // HeaderDivider()

                // MORE ICON WITH RED DOT
                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Default.MoreHoriz, "More", tint = secondaryWhite, modifier = Modifier.size(26.dp))
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, end = 10.dp)
                            .size(6.dp)
                            .background(Color(0xFFF23645), CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }

                // HeaderDivider()

                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Undo, null, tint = if(canUndo) secondaryWhite else Color(0xFF434651), modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Redo, null, tint = if(canRedo) secondaryWhite else Color(0xFF434651), modifier = Modifier.size(24.dp))
                }

                // HeaderDivider()

                IconButton(onClick = onToolSearchClick, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Search, null, tint = secondaryWhite, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onDownloadChart, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.CameraAlt, null, tint = secondaryWhite, modifier = Modifier.size(26.dp))
                }

                // HeaderDivider()

                // CHAT ICON
                IconButton(onClick = onChatClick, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Outlined.Chat, "Chat", tint = secondaryWhite, modifier = Modifier.size(26.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onTradeClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08090C)),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.4.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFF2A2E39))
                ) {
                    Text("Trade", color = secondaryWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        /*if (isAtBottom) {
            Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFF2A2E39).copy(alpha = 0.3f))
        }*/
    }
}

@Composable
fun TimeframeHeader(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.KeyboardArrowUp, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TimeframeItem(
    label: String,
    id: String,
    currentId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isSelected = currentId == id
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else Color(0xFFD1D4DC),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                Icon(
                    Icons.Default.StarOutline,
                    null,
                    tint = Color(0xFF787B86),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        onClick = {
            onSelect(id)
            onDismiss()
        },
        modifier = Modifier.background(if (isSelected) Color(0xFF2A2E39) else Color.Transparent)
    )
}

@Composable
fun HeaderDivider() {
    /*Divider(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(28.dp)
            .width(1.dp),
        color = Color(0xFF2A2E39).copy(alpha = 0.3f)
    )*/
}

@Composable
fun StyleMenuItem(
    label: String,
    styleId: String,
    icon: ImageVector,
    currentStyle: String,
    onStyleChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isSelected = currentStyle == styleId
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) Color(0xFF2962FF) else Color(0xFFD1D4DC),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    label,
                    color = if (isSelected) Color.White else Color(0xFFD1D4DC),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        },
        onClick = {
            onStyleChange(styleId)
            onDismiss()
        },
        modifier = Modifier.background(if (isSelected) Color(0xFF2A2E39) else Color.Transparent)
    )
}
