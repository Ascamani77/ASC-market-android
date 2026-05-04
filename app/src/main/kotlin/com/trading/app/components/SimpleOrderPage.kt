package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import com.trading.app.models.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleOrderPage(
    symbol: String,
    bidPrice: Float,
    askPrice: Float,
    priceChange: Float,
    onClose: () -> Unit,
    onPlaceOrder: (Position, String, Float?) -> Unit,
    initialSide: String = "buy"
) {
    var selectedSide by remember { mutableStateOf(if (initialSide.lowercase() == "sell") "sell" else "buy") }
    
    val tabs = listOf(
        "Market Execution",
        "Buy Limit",
        "Sell Limit",
        "Buy Stop",
        "Sell Stop",
        "Buy Stop Limit",
        "Sell Stop Limit"
    )
    var selectedTab by remember { mutableStateOf(tabs[0]) }
    
    val currentMarketPrice = if (selectedSide == "buy") askPrice else bidPrice
    
    var priceInput by remember { mutableStateOf(formatPriceValue(currentMarketPrice, symbol).replace(",", "")) }
    var unitsInput by remember { mutableStateOf("1") }
    
    // Update priceInput when side or price changes, but only if not edited yet or for Market Execution
    LaunchedEffect(selectedSide, askPrice, bidPrice) {
        if (selectedTab == "Market Execution") {
            priceInput = formatPriceValue(if (selectedSide == "buy") askPrice else bidPrice, symbol).replace(",", "")
        }
    }

    var tpEnabled by remember { mutableStateOf(false) }
    var tpInput by remember { mutableStateOf(formatPriceValue(currentMarketPrice * 1.01f, symbol).replace(",", "")) }
    var slEnabled by remember { mutableStateOf(false) }
    var slInput by remember { mutableStateOf(formatPriceValue(currentMarketPrice * 0.99f, symbol).replace(",", "")) }
    
    var timeInForce by remember { mutableStateOf("Week") }

    var showExitOptions by remember { mutableStateOf(false) }
    var exitOptionsTarget by remember { mutableStateOf("") } // "tp" or "sl"
    var showUnitOptions by remember { mutableStateOf(false) }
    var showTimeInForceOptions by remember { mutableStateOf(false) }
    var unitMode by remember { mutableStateOf("units") }
    var tpMode by remember { mutableStateOf("price") }
    var tpSecondaryMode by remember { mutableStateOf("ticks") }
    var slMode by remember { mutableStateOf("price") }
    var slSecondaryMode by remember { mutableStateOf("ticks") }
    var showSecondaryExitOptions by remember { mutableStateOf(false) }
    var exitsExpanded by remember { mutableStateOf(true) }

    // Real-time calculations
    val entryPrice = if (selectedTab == "Market Execution") (if (selectedSide == "buy") askPrice else bidPrice) else (priceInput.toFloatOrNull() ?: 0f)
    val units = unitsInput.toFloatOrNull() ?: 0f
    val leverageRatio = 500f
    val tradeValue = units * entryPrice
    val marginValue = if (leverageRatio > 0) tradeValue / leverageRatio else 0f
    
    val tpPrice = tpInput.toFloatOrNull() ?: 0f
    val slPrice = slInput.toFloatOrNull() ?: 0f
    
    val rewardUsd = if (selectedSide == "buy") (tpPrice - entryPrice) * units else (entryPrice - tpPrice) * units
    val riskUsd = if (selectedSide == "buy") (entryPrice - slPrice) * units else (slPrice - entryPrice) * units
    
    val rewardPercent = if (entryPrice > 0) (rewardUsd / tradeValue) * 100f else 0f
    val riskPercent = if (entryPrice > 0) (riskUsd / tradeValue) * 100f else 0f

    // Dynamic secondary text based on real inputs
    val tpSecondaryText = when(tpSecondaryMode) {
        "price" -> "Price"
        "ticks" -> {
            val ticks = if (symbol.contains("BTC")) (rewardUsd / units).toInt() else (rewardUsd / units * 10000).toInt()
            "${if (tpEnabled) ticks else 76} ticks"
        }
        "percent" -> "${String.format("%.2f", rewardPercent)}%"
        "reward_usd" -> "${String.format("%.2f", rewardUsd)} USD"
        "reward_percent" -> "${String.format("%.2f", rewardPercent / 10)}%" // Simulating % of balance
        else -> "76 ticks"
    }

    val slSecondaryText = when(slSecondaryMode) {
        "price" -> "Price"
        "ticks" -> {
            val ticks = if (symbol.contains("BTC")) (riskUsd / units).toInt() else (riskUsd / units * 10000).toInt()
            "${if (slEnabled) ticks else 811} ticks"
        }
        "percent" -> "${String.format("%.2f", riskPercent)}%"
        "reward_usd" -> "${String.format("%.2f", riskUsd)} USD"
        "reward_percent" -> "${String.format("%.2f", riskPercent / 10)}%"
        else -> "811 ticks"
    }

    val charcoalBlack = Color(0xFF171719) 
    val bodyBg = Color(0xFF000000)
    val inputBg = Color(0xFF121212)
    val borderColor = Color(0xFF2A2E39)
    val labelColor = Color(0xFF9BA0AB)
    val buyColor = Color(0xFF2962FF)
    val sellColor = Color(0xFFF23645)
    val headerSymbolTextSize = 20.sp
    val headerPriceTextSize = 18.sp
    val subtitleTextSize = 12.sp
    val tabTextSize = 14.sp
    val labelTextSize = 14.sp
    val valueTextSize = 16.sp
    val priceValueTextSize = 18.sp
    val sectionTitleTextSize = 16.sp
    val actionTextSize = 18.sp

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = bodyBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(charcoalBlack)
                    .padding(top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF787B86))
                )
            }
        },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        windowInsets = WindowInsets(0),
        modifier = Modifier.padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {

            // Top strip above white divider (charcoal black)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(charcoalBlack)
                    .height(8.dp)
            )

            // White Line Divider
            Divider(
                color = Color.White,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Header Section (Below White Line)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bodyBg)
            ) {
                // Symbol and Price Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            symbol.split(":").last(),
                            color = Color.White,
                            fontSize = headerSymbolTextSize,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.SwapVert,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Row {
                        val isBullish = priceChange >= 0
                        val priceColor = if (isBullish) Color(0xFF089981) else Color(0xFFF23645)
                        
                        Text(
                            formatPriceValue(bidPrice, symbol),
                            color = priceColor,
                            fontSize = headerPriceTextSize,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            formatPriceValue(askPrice, symbol),
                            color = priceColor,
                            fontSize = headerPriceTextSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    getFullSymbolName(symbol),
                    color = labelColor,
                    fontSize = subtitleTextSize,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Order Type Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    items(tabs) { tab ->
                        val isSelected = selectedTab == tab
                        Text(
                            tab,
                            color = if (isSelected) Color.White else labelColor,
                            fontSize = tabTextSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable { 
                                selectedTab = tab
                                if (tab.contains("Buy")) selectedSide = "buy"
                                if (tab.contains("Sell")) selectedSide = "sell"
                            }
                        )
                    }
                }
            }

            // Body content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(bodyBg)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab != "Market Execution") {
                    // Price Field
                    Text("Price", color = labelColor, fontSize = labelTextSize)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(inputBg, RoundedCornerShape(4.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            textStyle = TextStyle(color = Color.White, fontSize = priceValueTextSize, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Icon(Icons.Default.SwapHoriz, null, tint = labelColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ask + 107", color = labelColor, fontSize = labelTextSize)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Units Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showUnitOptions = true }
                ) {
                    Text(
                        when(unitMode) {
                            "units" -> "Units"
                            "margin_usd" -> "Margin USD"
                            "percent_balance" -> "% balance"
                            "risk_usd" -> "Risk, USD"
                            "risk_percent" -> "Risk, % balance"
                            else -> "Units"
                        }, 
                        color = labelColor, 
                        fontSize = labelTextSize
                    )
                    Icon(
                        if (showUnitOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = labelColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(inputBg, RoundedCornerShape(4.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = unitsInput,
                        onValueChange = { unitsInput = it },
                        textStyle = TextStyle(color = Color.White, fontSize = valueTextSize, fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Icon(Icons.Default.SwapHoriz, null, tint = labelColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showUnitOptions = true }
                    ) {
                        Text("0.20 USD", color = labelColor, fontSize = labelTextSize)
                        Icon(
                            if (showUnitOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint = labelColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Exits Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exitsExpanded = !exitsExpanded }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exits", color = Color.White, fontSize = sectionTitleTextSize, fontWeight = FontWeight.Bold)
                    Icon(
                        if (exitsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                if (exitsExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Take Profit
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                exitOptionsTarget = "tp"
                                showExitOptions = true 
                            }
                        ) {
                            Text(
                                when(tpMode) {
                                    "price" -> "Take profit, price"
                                    "ticks" -> "Take profit, ticks"
                                    "percent" -> "Take profit, % price"
                                    "reward_usd" -> "Take profit, Reward USD"
                                    "reward_percent" -> "Take profit, Reward % balance"
                                    else -> "Take profit"
                                }, 
                                color = if (tpEnabled) Color.White else labelColor, 
                                fontSize = labelTextSize
                            )
                            Icon(
                                if (showExitOptions && exitOptionsTarget == "tp") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = labelColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = tpEnabled,
                            onCheckedChange = { tpEnabled = it },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF434651),
                                uncheckedThumbColor = labelColor,
                                uncheckedTrackColor = Color(0xFF2A2E39)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (tpEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(inputBg, RoundedCornerShape(4.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = tpInput,
                                onValueChange = { tpInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = priceValueTextSize, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Icon(Icons.Default.SwapHoriz, null, tint = labelColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    exitOptionsTarget = "tp"
                                    showSecondaryExitOptions = true
                                }
                            ) {
                                Text(
                                    tpSecondaryText,
                                    color = labelColor,
                                    fontSize = labelTextSize
                                )
                                Icon(
                                    if (showSecondaryExitOptions && exitOptionsTarget == "tp") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = labelColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(inputBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tpInput, color = Color.White.copy(alpha = 0.7f), fontSize = valueTextSize, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    exitOptionsTarget = "tp"
                                    showSecondaryExitOptions = true
                                }
                            ) {
                                Text(tpSecondaryText, color = Color.White.copy(alpha = 0.7f), fontSize = labelTextSize)
                                Icon(
                                    if (showSecondaryExitOptions && exitOptionsTarget == "tp") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stop Loss
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                exitOptionsTarget = "sl"
                                showExitOptions = true 
                            }
                        ) {
                            Text(
                                when(slMode) {
                                    "price" -> "Stop loss, price"
                                    "ticks" -> "Stop loss, ticks"
                                    "percent" -> "Stop loss, % price"
                                    "reward_usd" -> "Stop loss, Reward USD"
                                    "reward_percent" -> "Stop loss, Reward % balance"
                                    else -> "Stop loss"
                                }, 
                                color = if (slEnabled) Color.White else labelColor, 
                                fontSize = labelTextSize
                            )
                            Icon(
                                if (showExitOptions && exitOptionsTarget == "sl") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = labelColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = slEnabled,
                            onCheckedChange = { slEnabled = it },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF434651),
                                uncheckedThumbColor = labelColor,
                                uncheckedTrackColor = Color(0xFF2A2E39)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (slEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(inputBg, RoundedCornerShape(4.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = slInput,
                                onValueChange = { slInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = priceValueTextSize, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Icon(Icons.Default.SwapHoriz, null, tint = labelColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    exitOptionsTarget = "sl"
                                    showSecondaryExitOptions = true
                                }
                            ) {
                                Text(
                                    slSecondaryText,
                                    color = labelColor,
                                    fontSize = labelTextSize
                                )
                                Icon(
                                    if (showSecondaryExitOptions && exitOptionsTarget == "sl") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = labelColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(inputBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(slInput, color = Color.White.copy(alpha = 0.7f), fontSize = valueTextSize, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    exitOptionsTarget = "sl"
                                    showSecondaryExitOptions = true
                                }
                            ) {
                                Text(slSecondaryText, color = Color.White.copy(alpha = 0.7f), fontSize = labelTextSize)
                                Icon(
                                    if (showSecondaryExitOptions && exitOptionsTarget == "sl") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Extra Settings
                Text("Extra settings", color = Color.White, fontSize = sectionTitleTextSize, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Time in force", color = labelColor, fontSize = labelTextSize)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(inputBg, RoundedCornerShape(4.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .clickable { showTimeInForceOptions = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(timeInForce, color = Color.White, fontSize = valueTextSize, modifier = Modifier.weight(1f))
                    Icon(
                        if (showTimeInForceOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = labelColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Order Info
                Text("Order info", color = Color.White, fontSize = sectionTitleTextSize, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRowSimple("Margin", "${String.format("%.2f", marginValue)} / 108,231.58", hasInfo = true)
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2A2E39))) {
                    val progress = (marginValue / 1000f).coerceIn(0f, 1f) // Assuming 1000 limit for visualization
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White))
                }
                
                InfoRowSimple("Leverage", "500:1")
                InfoRowSimple("Tick value", "0.01 USD")
                InfoRowSimple("Trade value", "${formatPriceValue(tradeValue, symbol)} USD")
                
                if (tpEnabled || slEnabled) {
                    if (tpEnabled) {
                        InfoRowSimple("Reward", "${String.format("%.2f", rewardPercent)}% / ${String.format("%.2f", rewardUsd)} USD")
                    }
                    if (slEnabled) {
                        InfoRowSimple("Risk", "${String.format("%.2f", riskPercent)}% / ${String.format("%.2f", riskUsd)} USD")
                    }
                } else {
                    InfoRowSimple("Reward", "0.00% / 0.00 USD")
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            if (showTimeInForceOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showTimeInForceOptions = false },
                    containerColor = charcoalBlack,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = null,
                    windowInsets = WindowInsets(0),
                    modifier = Modifier.padding(bottom = AppBottomNavHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        ExitOptionItem("Day", isSelected = timeInForce == "Day") {
                            timeInForce = "Day"
                            showTimeInForceOptions = false
                        }
                        ExitOptionItem("Week", isSelected = timeInForce == "Week") {
                            timeInForce = "Week"
                            showTimeInForceOptions = false
                        }
                        ExitOptionItem("Month", isSelected = timeInForce == "Month") {
                            timeInForce = "Month"
                            showTimeInForceOptions = false
                        }
                        ExitOptionItem("GTD", isSelected = timeInForce == "GTD") {
                            timeInForce = "GTD"
                            showTimeInForceOptions = false
                        }
                    }
                }
            }

            if (showUnitOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showUnitOptions = false },
                    containerColor = charcoalBlack,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = null,
                    windowInsets = WindowInsets(0),
                    modifier = Modifier.padding(bottom = AppBottomNavHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        ExitOptionItem("Units", isSelected = unitMode == "units") {
                            unitMode = "units"
                            showUnitOptions = false
                        }
                        ExitOptionItem("Margin USD", isSelected = unitMode == "margin_usd", hasInfo = true) {
                            unitMode = "margin_usd"
                            showUnitOptions = false
                        }
                        ExitOptionItem("% balance", isSelected = unitMode == "percent_balance", hasInfo = true) {
                            unitMode = "percent_balance"
                            showUnitOptions = false
                        }
                        ExitOptionItem("Risk, USD", isSelected = unitMode == "risk_usd", hasInfo = true) {
                            unitMode = "risk_usd"
                            showUnitOptions = false
                        }
                        ExitOptionItem("Risk, % balance", isSelected = unitMode == "risk_percent", hasInfo = true) {
                            unitMode = "risk_percent"
                            showUnitOptions = false
                        }
                    }
                }
            }

            if (showExitOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showExitOptions = false },
                    containerColor = charcoalBlack,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = null,
                    windowInsets = WindowInsets(0),
                    modifier = Modifier.padding(bottom = AppBottomNavHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        ExitOptionItem("Price", isSelected = if (exitOptionsTarget == "tp") tpMode == "price" else slMode == "price") {
                            if (exitOptionsTarget == "tp") tpMode = "price" else slMode = "price"
                            showExitOptions = false
                        }
                        ExitOptionItem("Ticks", isSelected = if (exitOptionsTarget == "tp") tpMode == "ticks" else slMode == "ticks") {
                            if (exitOptionsTarget == "tp") tpMode = "ticks" else slMode = "ticks"
                            showExitOptions = false
                        }
                        ExitOptionItem("% price", isSelected = if (exitOptionsTarget == "tp") tpMode == "percent" else slMode == "percent", hasInfo = true) {
                            if (exitOptionsTarget == "tp") tpMode = "percent" else slMode = "percent"
                            showExitOptions = false
                        }
                        ExitOptionItem("Reward, USD", isSelected = if (exitOptionsTarget == "tp") tpMode == "reward_usd" else slMode == "reward_usd") {
                            if (exitOptionsTarget == "tp") tpMode = "reward_usd" else slMode = "reward_usd"
                            showExitOptions = false
                        }
                        ExitOptionItem("Reward, % balance", isSelected = if (exitOptionsTarget == "tp") tpMode == "reward_percent" else slMode == "reward_percent", hasInfo = true) {
                            if (exitOptionsTarget == "tp") tpMode = "reward_percent" else slMode = "reward_percent"
                            showExitOptions = false
                        }
                    }
                }
            }

            if (showSecondaryExitOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showSecondaryExitOptions = false },
                    containerColor = charcoalBlack,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = null,
                    windowInsets = WindowInsets(0),
                    modifier = Modifier.padding(bottom = AppBottomNavHeight)
                ) {
                    val currentMode = if (exitOptionsTarget == "tp") tpSecondaryMode else slSecondaryMode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        ExitOptionItem(
                            "Ticks", 
                            value = if (exitOptionsTarget == "tp") "76" else "31,057", 
                            isSelected = currentMode == "ticks"
                        ) {
                            if (exitOptionsTarget == "tp") tpSecondaryMode = "ticks" else slSecondaryMode = "ticks"
                            showSecondaryExitOptions = false
                        }
                        ExitOptionItem(
                            "% price", 
                            value = if (exitOptionsTarget == "tp") "0.15" else "6.54", 
                            isSelected = currentMode == "percent",
                            hasInfo = true
                        ) {
                            if (exitOptionsTarget == "tp") tpSecondaryMode = "percent" else slSecondaryMode = "percent"
                            showSecondaryExitOptions = false
                        }
                        ExitOptionItem(
                            if (exitOptionsTarget == "tp") "Reward, USD" else "Risk, USD", 
                            value = if (exitOptionsTarget == "tp") "0.76" else "310.57", 
                            isSelected = currentMode == "reward_usd"
                        ) {
                            if (exitOptionsTarget == "tp") tpSecondaryMode = "reward_usd" else slSecondaryMode = "reward_usd"
                            showSecondaryExitOptions = false
                        }
                        ExitOptionItem(
                            if (exitOptionsTarget == "tp") "Reward, % balance" else "Risk, % balance", 
                            value = if (exitOptionsTarget == "tp") "0.01" else "0.29",
                            isSelected = currentMode == "reward_percent",
                            hasInfo = true
                        ) {
                            if (exitOptionsTarget == "tp") tpSecondaryMode = "reward_percent" else slSecondaryMode = "reward_percent"
                            showSecondaryExitOptions = false
                        }
                    }
                }
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Text("Discard", color = Color.White, fontSize = valueTextSize)
                }
                Button(
                    onClick = {
                        val finalPrice = if (selectedTab == "Market Execution") (if (selectedSide == "buy") askPrice else bidPrice) else (priceInput.toFloatOrNull() ?: 0f)
                        onPlaceOrder(
                            Position(
                                symbol = symbol,
                                type = selectedSide,
                                entryPrice = finalPrice,
                                volume = unitsInput.toFloatOrNull() ?: 0f,
                                time = System.currentTimeMillis(),
                                tp = if (tpEnabled) tpInput.toFloatOrNull() else null,
                                sl = if (slEnabled) slInput.toFloatOrNull() else null
                            ),
                            selectedTab,
                            null
                        )
                        onClose()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedSide == "buy") buyColor else sellColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (selectedSide == "buy") "Buy" else "Sell", color = Color.White, fontSize = actionTextSize, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InfoRowSimple(label: String, value: String, hasInfo: Boolean = false) {
    val labelColor = Color(0xFF9BA0AB)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = labelColor, fontSize = 14.sp)
            if (hasInfo) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Help, null, tint = labelColor, modifier = Modifier.size(16.dp))
            }
        }
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    }
}

@Composable
fun ExitOptionItem(
    label: String,
    value: String? = null,
    isSelected: Boolean,
    hasInfo: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            if (value != null) {
                Text(
                    value,
                    color = if (isSelected) Color.Black else Color(0xFF787B86),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (hasInfo) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = if (isSelected) Color.Black else Color(0xFF787B86),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatPriceValue(price: Float, symbol: String = ""): String {
    val uppercaseSymbol = symbol.uppercase()
    val isBitcoin = uppercaseSymbol.contains("BTC")
    val isForex = uppercaseSymbol.length == 6 || uppercaseSymbol.contains("/")
    val pattern = when {
        isBitcoin -> "%,.0f"
        isForex -> "%,.5f"
        else -> "%,.2f"
    }
    return String.format(pattern, price)
}
