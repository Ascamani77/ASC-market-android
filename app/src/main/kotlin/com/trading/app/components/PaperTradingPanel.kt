package com.trading.app.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.data.PaperTradingSnapshotStore
import com.trading.app.models.Position
import com.trading.app.models.Order
import com.trading.app.data.Mt5Service
import com.trading.app.data.ChartFeedType
import java.util.Locale

@Composable
fun PaperTradingPanel(
    onClose: () -> Unit,
    onPositionClick: (Position) -> Unit = {},
    positions: List<Position> = emptyList(),
    selectedPositionId: String? = null,
    orders: List<Order> = emptyList(),
    orderHistory: List<Order> = emptyList(),
    balanceHistory: List<com.trading.app.models.BalanceRecord> = emptyList(),
    currentPrice: Float = 0f,
    quotePriceForSymbol: ((String) -> Float?)? = null,
    preferSnapshotStats: Boolean = false,
    providerLabel: String = "LIVE",
    balance: Double = 0.0,
    accountInfo: Mt5Service.AccountInfo? = null,
    sourceName: String = "Live Trade",
    accountLabel: String = "No account connected",
    isBrokerConnected: Boolean = accountInfo != null,
    backgroundColor: Color = Color(0xFF08090C),
    onMarketTypeChange: ((String) -> Unit)? = null,
    onAccountChange: ((ChartFeedType) -> Unit)? = null,
    currentMarketType: String = "spot",
    onRefresh: (() -> Unit)? = null
) {
    var activeTab by remember { mutableStateOf("Positions") }
    val tabs = listOf("Positions", "Orders", "Order History", "Balance History", "Trading Journal")
    
    var showVisibilitySettings by remember { mutableStateOf(false) }
    var visibilitySettings by remember { mutableStateOf(PaperTradingVisibility()) }
    var showMarketTypeDropdown by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    val labelColor = Color(0xFF787B86)
    val horizontalMargin = 16.dp
    val snapshot = PaperTradingSnapshotStore.snapshot
    val useSnapshotStats = preferSnapshotStats && snapshot.hasLiveTradeData && snapshot.activeTrades > 0 && snapshot.currentTradeSymbol != null

    // Calculations for Header Stats
    val totalUnrealizedPnl = if (useSnapshotStats) {
        snapshot.floatingPnl
    } else accountInfo?.unrealizedPnl ?: positions.sumOf {
        ((currentPrice - it.entryPrice) * it.volume * (if (it.type == "buy") 1f else -1f)).toDouble()
    }
    val displayBalance = if (useSnapshotStats) snapshot.balance else accountInfo?.balance ?: balance
    val equity = if (useSnapshotStats) snapshot.equity else accountInfo?.equity ?: (displayBalance + totalUnrealizedPnl)
    val totalMargin = if (useSnapshotStats) snapshot.margin else accountInfo?.margin ?: positions.sumOf { (it.entryPrice * it.volume * 0.01f).toDouble() }
    val availableFunds = if (useSnapshotStats) snapshot.freeMargin else accountInfo?.availableFunds ?: (equity - totalMargin)
    val marginBuffer = if (useSnapshotStats) snapshot.marginLevel else accountInfo?.marginBuffer ?: (if (equity > 0) (availableFunds / equity) * 100 else 100.0)
    val realizedPnl = if (useSnapshotStats) snapshot.realizedPnl else accountInfo?.realizedPnl ?: 0.0
    val ordersMargin = if (useSnapshotStats) snapshot.ordersMargin else accountInfo?.ordersMargin ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.clickable { 
                if (onAccountChange != null) {
                    showAccountDropdown = true 
                }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sourceName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (onAccountChange != null) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                
                DropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false },
                    modifier = Modifier.background(Color(0xFF1E222D))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Pepperstone cTrader Live",
                                    color = if (sourceName.contains("Pepperstone")) Color(0xFF2962FF) else Color.White,
                                    fontSize = 14.sp
                                )
                                if (sourceName.contains("Pepperstone")) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF2962FF), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            onAccountChange?.invoke(ChartFeedType.PEPPERSTONE_CTRADER)
                            showAccountDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Exness Live",
                                    color = if (sourceName.contains("Exness")) Color(0xFF2962FF) else Color.White,
                                    fontSize = 14.sp
                                )
                                if (sourceName.contains("Exness")) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF2962FF), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            onAccountChange?.invoke(ChartFeedType.EXNESS)
                            showAccountDropdown = false
                        }
                    )
                }

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            if (onMarketTypeChange != null) {
                                showMarketTypeDropdown = !showMarketTypeDropdown
                            }
                        }
                    ) {
                        Text(accountLabel, color = labelColor, fontSize = 12.sp)
                        if (onMarketTypeChange != null) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = labelColor, modifier = Modifier.size(14.dp))
                        }
                    }
                    
                    // Dropdown menu for market type selection
                    DropdownMenu(
                        expanded = showMarketTypeDropdown,
                        onDismissRequest = { showMarketTypeDropdown = false },
                        modifier = Modifier.background(Color(0xFF1E222D))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Binance Spot ${if (accountLabel.contains("Demo")) "Demo" else "Live"}",
                                        color = if (currentMarketType == "spot") Color(0xFF2962FF) else Color.White,
                                        fontSize = 14.sp
                                    )
                                    if (currentMarketType == "spot") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2962FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onMarketTypeChange?.invoke("spot")
                                showMarketTypeDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Binance Futures ${if (accountLabel.contains("Demo")) "Demo" else "Live"}",
                                        color = if (currentMarketType == "futures") Color(0xFF2962FF) else Color.White,
                                        fontSize = 14.sp
                                    )
                                    if (currentMarketType == "futures") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2962FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onMarketTypeChange?.invoke("futures")
                                showMarketTypeDropdown = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

    val isConnected = isBrokerConnected
    
    fun formatValue(value: Double, pattern: String = "%,.2f", showSign: Boolean = false): String {
        if (!isConnected) return "---"
        val sign = if (showSign && value >= 0) "+" else ""
        return sign + String.format(Locale.US, pattern, value)
    }

    // Account Stats Grid
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AccountStatItem("Account balance", formatValue(displayBalance), Modifier.weight(1f))
            AccountStatItem("Equity", formatValue(equity), Modifier.weight(1f))
            val realizedColor = if (!isConnected) Color(0xFFD1D4DC) else if (realizedPnl >= 0) Color(0xFF089981) else Color(0xFFF23645)
            AccountStatItem(
                "Realized P&L", 
                formatValue(realizedPnl, showSign = true), 
                Modifier.weight(1f), 
                realizedColor
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            val unrealizedColor = if (!isConnected) Color(0xFFD1D4DC) else if (totalUnrealizedPnl >= 0) Color(0xFF089981) else Color(0xFFF23645)
            AccountStatItem(
                "Unrealized P&L", 
                formatValue(totalUnrealizedPnl, showSign = true), 
                Modifier.weight(1f), 
                unrealizedColor
            )
            AccountStatItem("Account margin", formatValue(totalMargin), Modifier.weight(1f), showInfo = true)
            AccountStatItem("Free margin", formatValue(availableFunds), Modifier.weight(1f), showInfo = true)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            AccountStatItem("Orders margin", formatValue(ordersMargin), Modifier.weight(1f), showInfo = true)
            AccountStatItem("Margin level", formatValue(marginBuffer, "%.2f%%"), Modifier.weight(1f), showInfo = true)
            Spacer(modifier = Modifier.weight(1f))
        }
    }

        Spacer(modifier = Modifier.height(16.dp))

        // Navbar: Items
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = horizontalMargin),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = activeTab == tab
                    val count = when (tab) {
                        "Positions" -> if (positions.isNotEmpty()) positions.size else null
                        "Orders" -> {
                            val activeOrdersCount = orders.size
                            if (activeOrdersCount > 0) activeOrdersCount else null
                        }
                        "Order History" -> if (orderHistory.isNotEmpty()) orderHistory.size else null
                        "Balance History" -> if (balanceHistory.isNotEmpty()) balanceHistory.size else null
                        else -> null
                    }
                    
                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                activeTab = tab 
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else labelColor,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (count != null) {
                                Text(
                                    text = " $count",
                                    color = labelColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        // Indicator line
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.White)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
            Divider(modifier = Modifier.padding(horizontal = horizontalMargin), color = Color(0xFF2A2E39), thickness = 4.dp)
        }

        // Independent Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                "Positions" -> PositionsTab(
                    positions = positions, 
                    currentPrice = currentPrice,
                    selectedPositionId = selectedPositionId,
                    quotePriceForSymbol = quotePriceForSymbol,
                    providerLabel = providerLabel,
                    visibility = visibilitySettings,
                    onPositionClick = onPositionClick,
                    onSettingsClick = { showVisibilitySettings = true }
                )
                "Orders" -> OrdersTab(
                    orders = orders,
                    visibility = visibilitySettings,
                    onSettingsClick = { showVisibilitySettings = true }
                )
                "Order History" -> OrderHistoryTab(orderHistory)
                "Balance History" -> BalanceHistoryTab(balanceHistory)
                "Trading Journal" -> TradingJournalTab()
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available for $activeTab", color = labelColor)
                    }
                }
            }
        }
    }

    if (showVisibilitySettings) {
        PaperTradingSettingsModal(
            visibility = visibilitySettings,
            onVisibilityChange = { visibilitySettings = it },
            onClose = { showVisibilitySettings = false }
        )
    }
}

@Composable
fun AccountStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    showInfo: Boolean = false
) {
    val labelColor = Color(0xFF787B86)
    val defaultValColor = Color(0xFFD1D4DC)
    
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label, 
                color = labelColor, 
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (showInfo) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.Info,
                    null,
                    tint = labelColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            value,
            color = valueColor ?: defaultValColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
