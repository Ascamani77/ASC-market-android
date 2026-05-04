package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import com.trading.app.models.OHLCData
import com.trading.app.models.Position
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderModal(
    symbol: String,
    bidPrice: Float,
    askPrice: Float,
    priceChange: Float,
    chartData: List<OHLCData>,
    onClose: () -> Unit,
    onPlaceOrder: (Position, String, Float?) -> Unit,
    onTradingSettingsClick: () -> Unit,
    showMarketSideButtons: Boolean = true,
    initialSide: String = "buy"
) {
    val normalizedInitialSide = if (initialSide.equals("sell", ignoreCase = true)) "sell" else "buy"
    var selectedType by remember(initialSide) { mutableStateOf(normalizedInitialSide) }
    val orderTypes = listOf(
        "Market Execution",
        "Buy Limit",
        "Sell Limit",
        "Buy Stop",
        "Sell Stop",
        "Buy Stop Limit",
        "Sell Stop Limit"
    )
    var selectedTab by remember { mutableStateOf("Market Execution") }
    
    // Inputs
    var unitsInput by remember { mutableStateOf("0.1") }
    var limitPriceInput by remember { mutableStateOf("") }
    var stopLimitPriceInput by remember { mutableStateOf("") }
    var tpEnabled by remember { mutableStateOf(false) }
    var tpPriceInput by remember { mutableStateOf("") }
    var slEnabled by remember { mutableStateOf(false) }
    var slPriceInput by remember { mutableStateOf("") }
    var expiration by remember { mutableStateOf("GTC") }
    var commentInput by remember { mutableStateOf("") }
    
    // Derived side from tab
    LaunchedEffect(selectedTab) {
        if (selectedTab.contains("Buy")) selectedType = "buy"
        else if (selectedTab.contains("Sell")) selectedType = "sell"
    }
    
    // Default values logic - Runs when entering a tab or changing side
    LaunchedEffect(selectedTab, selectedType, symbol) {
        val currentMarketPrice = if (selectedType == "buy") askPrice else bidPrice
        if (limitPriceInput.isEmpty()) {
            limitPriceInput = formatPriceValue(currentMarketPrice, symbol).replace(",", "")
        }
        if (stopLimitPriceInput.isEmpty()) {
            stopLimitPriceInput = formatPriceValue(currentMarketPrice, symbol).replace(",", "")
        }
        if (tpPriceInput.isEmpty()) {
            val tpOffset = if (selectedType == "buy") 1.05f else 0.95f
            tpPriceInput = formatPriceValue(currentMarketPrice * tpOffset, symbol).replace(",", "")
        }
        if (slPriceInput.isEmpty()) {
            val slOffset = if (selectedType == "buy") 0.95f else 1.05f
            slPriceInput = formatPriceValue(currentMarketPrice * slOffset, symbol).replace(",", "")
        }
    }

    // Modes
    var unitsMode by remember { mutableStateOf("Units") }
    var secondaryUnitsMode by remember { mutableStateOf("Margin USD") }
    var tpMode by remember { mutableStateOf("Price") }
    var secondaryTpMode by remember { mutableStateOf("Ticks") }
    var slMode by remember { mutableStateOf("Price") }
    var secondarySlMode by remember { mutableStateOf("Ticks") }
    var timeInForce by remember { mutableStateOf("Week") }

    // UI State
    var showModeSelector by remember { mutableStateOf<String?>(null) }
    var showPresets by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var slEnablesQuantityInRisk by remember { mutableStateOf(false) }
    var showExitLevelsModal by remember { mutableStateOf(false) }

    // Constants & Derived Data
    val tickSize = when {
        symbol.uppercase().contains("BTC") -> 1f
        symbol.length == 6 || symbol.contains("/") -> 0.00001f
        else -> 0.01f
    }
    val leverage = 500f
    val balance = 102721.68f // Simulated balance

    // Current Values
    val marketPrice = if (selectedType == "buy") askPrice else bidPrice
    val limitPrice = limitPriceInput.replace(",", "").toFloatOrNull() ?: marketPrice
    val entryPrice = if (selectedTab == "Market Execution") marketPrice else limitPrice
    val units = unitsInput.toFloatOrNull() ?: 0f
    
    val isPriceValid = when (selectedTab) {
        "Buy Limit" -> limitPrice < askPrice
        "Sell Limit" -> limitPrice > bidPrice
        "Buy Stop" -> limitPrice > askPrice
        "Sell Stop" -> limitPrice < bidPrice
        "Buy Stop Limit" -> limitPrice > askPrice
        "Sell Stop Limit" -> limitPrice < bidPrice
        else -> true
    }

    val tpPrice = tpPriceInput.replace(",", "").toFloatOrNull() ?: entryPrice
    val slPrice = slPriceInput.replace(",", "").toFloatOrNull() ?: entryPrice
    
    // Calculations
    val tradeValue = units * entryPrice
    val margin = if (leverage > 0) tradeValue / leverage else 0f
    val tpTicks = abs(tpPrice - entryPrice) / tickSize
    val slTicks = abs(slPrice - entryPrice) / tickSize
    
    val riskAmount = units * abs(entryPrice - slPrice)
    val riskPercent = if (balance > 0) (riskAmount / balance) * 100f else 0f
    val tpProfitAmount = units * abs(tpPrice - entryPrice)
    val tpProfitPercent = if (balance > 0) (tpProfitAmount / balance) * 100f else 0f
    val topSectionBackground = Color(0xFF0B0E14)
    val topSectionHeight = 8.dp
    val contentHorizontalPadding = 10.dp
    val isBullish = priceChange >= 0
    val livePriceColor = if (isBullish) Color(0xFF089981) else Color(0xFFF23645)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topSectionBackground)
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
        containerColor = Color.Black,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0),
        modifier = Modifier.padding(bottom = AppBottomNavHeight)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topSectionBackground)
                        .height(topSectionHeight)
                )

                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    thickness = 1.dp
                )

                // Header
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = contentHorizontalPadding, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.SwapVert, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatPriceValue(bidPrice, symbol),
                            color = livePriceColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            formatPriceValue(askPrice, symbol),
                            color = livePriceColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(getFullSymbolName(symbol), color = Color(0xFF787B86), fontSize = 12.sp)
                }

                // Tabs - Scrollable like MT5
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    orderTypes.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val textColor = if (isSelected) {
                            if (tab.contains("Sell") || (tab == "Market Execution" && selectedType == "sell")) Color(0xFFF23645) else Color.White
                        } else Color(0xFF787B86)

                        Text(
                            text = tab,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Content
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = contentHorizontalPadding)) {
                    // Volume
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        BasicTextField(
                            value = unitsInput,
                            onValueChange = { unitsInput = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.width(80.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                            Text("Lots", color = Color.White, fontSize = 16.sp)
                            Icon(Icons.Default.UnfoldMore, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
                        }
                    }

                    // Volume Slider
                    Slider(
                        value = unitsInput.toFloatOrNull() ?: 0.1f,
                        onValueChange = { unitsInput = String.format("%.1f", it) },
                        valueRange = 0.1f..10f,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF2A2E39)
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Margin = ${String.format("%.2f", margin)} USD", color = Color.White, fontSize = 12.sp)
                        Text("Free: ${String.format("%,.2f", balance)} USD", color = Color(0xFF787B86), fontSize = 12.sp)
                    }

                    if (selectedTab != "Market Execution") {
                        // Price Field
                        Text("Price", color = Color(0xFF787B86), fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                                .border(1.dp, if (!isPriceValid) Color(0xFFF23645) else if (selectedType == "sell") Color(0xFFF23645).copy(alpha = 0.4f) else Color(0xFF2A2E39), RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = limitPriceInput,
                                onValueChange = { limitPriceInput = it },
                                textStyle = TextStyle(color = if (!isPriceValid) Color(0xFFF23645) else if (selectedType == "sell") Color(0xFFF23645) else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.UnfoldMore, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
                        }

                        // Stop Limit Price Field
                        if (selectedTab.contains("Stop Limit")) {
                            Text("Stop Limit Price", color = Color(0xFF787B86), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                                    .border(1.dp, if (selectedType == "sell") Color(0xFFF23645).copy(alpha = 0.4f) else Color(0xFF2A2E39), RoundedCornerShape(4.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = stopLimitPriceInput,
                                    onValueChange = { stopLimitPriceInput = it },
                                    textStyle = TextStyle(color = if (selectedType == "sell") Color(0xFFF23645) else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.UnfoldMore, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Add Stop Levels Button
                    Button(
                        onClick = { showExitLevelsModal = true },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFF2A2E39))
                    ) {
                        Text("Add Stop Levels", color = Color.White, fontSize = 16.sp)
                    }

                    // Expiration
                    if (selectedTab != "Market Execution") {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Expiration: ", color = Color.White, fontSize = 14.sp)
                            Text(expiration, color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { })
                            Icon(Icons.Default.UnfoldMore, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Fill policy: ", color = Color.White, fontSize = 14.sp)
                            Text("Fill or Kill", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { })
                            Icon(Icons.Default.UnfoldMore, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Comment
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Comment: ", color = Color.White, fontSize = 14.sp)
                        if (commentInput.isEmpty()) {
                            Text("Add comment", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { commentInput = "Order" })
                        } else {
                            BasicTextField(value = commentInput, onValueChange = { commentInput = it }, textStyle = TextStyle(color = Color.White, fontSize = 14.sp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PendingOrderLineChartPanel(
                        symbol = symbol,
                        candles = chartData,
                        lineColor = if (selectedType == "sell") Color(0xFFF23645) else Color(0xFF2962FF),
                        marketPrice = marketPrice,
                        entryPreview = if (selectedTab == "Market Execution") null else limitPriceInput.replace(",", "").toFloatOrNull(),
                        stopLimitPreview = if (selectedTab.contains("Stop Limit")) stopLimitPriceInput.replace(",", "").toFloatOrNull() else null,
                        tpPreview = if (tpEnabled) tpPriceInput.replace(",", "").toFloatOrNull() else null,
                        slPreview = if (slEnabled) slPriceInput.replace(",", "").toFloatOrNull() else null,
                        isEntryValid = isPriceValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom Buttons
                val isMarketExecution = selectedTab == "Market Execution"
                if (isMarketExecution && showMarketSideButtons) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onPlaceOrder(
                                    Position(
                                        symbol = symbol,
                                        type = "sell",
                                        entryPrice = bidPrice,
                                        volume = units,
                                        time = System.currentTimeMillis()
                                    ),
                                    "Market Execution",
                                    null
                                )
                                onClose()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF23645)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sell", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onPlaceOrder(
                                    Position(
                                        symbol = symbol,
                                        type = "buy",
                                        entryPrice = askPrice,
                                        volume = units,
                                        time = System.currentTimeMillis()
                                    ),
                                    "Market Execution",
                                    null
                                )
                                onClose()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Buy", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val isActionEnabled = if (isMarketExecution) true else isPriceValid
                    Button(
                        onClick = { 
                            if (isMarketExecution) {
                                val marketEntryPrice = if (selectedType == "buy") askPrice else bidPrice
                                onPlaceOrder(
                                    Position(
                                        symbol = symbol,
                                        type = selectedType,
                                        entryPrice = marketEntryPrice,
                                        volume = units,
                                        time = System.currentTimeMillis(),
                                        tp = if (tpEnabled) tpPrice else null,
                                        sl = if (slEnabled) slPrice else null
                                    ),
                                    "Market Execution",
                                    null
                                )
                                onClose()
                            } else if (isPriceValid) {
                                onPlaceOrder(
                                    Position(
                                        symbol = symbol,
                                        type = selectedType,
                                        entryPrice = entryPrice,
                                        volume = units,
                                        time = System.currentTimeMillis(),
                                        tp = if (tpEnabled) tpPrice else null,
                                        sl = if (slEnabled) slPrice else null
                                    ),
                                    selectedTab,
                                    stopLimitPriceInput.toFloatOrNull()
                                )
                                onClose()
                            }
                        },
                        enabled = isActionEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding, vertical = 16.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "buy") Color(0xFF2962FF) else Color(0xFFF23645),
                            disabledContainerColor = Color(0xFF2A2E39)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val actionText = when {
                            isMarketExecution -> "Place ${if (selectedType == "buy") "Buy" else "Sell"} Market"
                            isPriceValid -> "Place $selectedTab"
                            else -> "Invalid Price"
                        }
                        Text(
                            actionText,
                            color = if (isActionEnabled) Color.White else Color(0xFF787B86),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Selector Overlays (Units, SecondaryUnits, TP, SL, TIF)
            if (showModeSelector != null) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { showModeSelector = null }) {
                    Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp), color = Color(0xFF121212), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            val currentMode = when(showModeSelector) { "Units" -> unitsMode; "SecondaryUnits" -> secondaryUnitsMode; "TP" -> tpMode; "SecondaryTP" -> secondaryTpMode; "SL" -> slMode; "SecondarySL" -> secondarySlMode; else -> timeInForce }
                            val options = when(showModeSelector) {
                                "Units" -> listOf(Triple("Units", "", false), Triple("Margin USD", "", true), Triple("% balance", "", true), Triple("Risk, USD", "", true), Triple("Risk, % balance", "", true))
                                "SecondaryUnits" -> listOf(Triple("Margin USD", String.format("%.2f", margin), false), Triple("% balance", String.format("%.2f", (margin/balance)*100), false), Triple("Risk, USD", String.format("%.2f", riskAmount), true), Triple("Risk, % balance", String.format("%.2f", riskPercent), true))
                                "TIF" -> listOf(Triple("Day", "", false), Triple("Week", "", false), Triple("Month", "", false), Triple("GTD", "", false))
                                "SecondaryTP" -> listOf(Triple("Ticks", String.format("%,.0f", tpTicks), false), Triple("% price", String.format("%.2f%%", (abs(tpPrice - entryPrice)/entryPrice)*100), false), Triple("Risk, USD", String.format("%,.2f", tpProfitAmount), false), Triple("Risk, % balance", String.format("%.2f", tpProfitPercent), false))
                                "SecondarySL" -> listOf(Triple("Ticks", String.format("%,.0f", slTicks), false), Triple("% price", String.format("%.2f%%", (abs(slPrice - entryPrice)/entryPrice)*100), false), Triple("Risk, USD", String.format("%,.2f", riskAmount), false), Triple("Risk, % balance", String.format("%.2f", riskPercent), false))
                                else -> listOf(Triple("Price", "", false), Triple("Ticks", "", false), Triple("% price", "", true), Triple("Risk, USD", "", true), Triple("Risk, % balance", "", true))
                            }
                            options.forEach { (label, value, hasInfo) ->
                                val isSelected = label == currentMode
                                Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color.White else Color.Transparent).clickable { 
                                    when(showModeSelector) { "Units" -> unitsMode = label; "SecondaryUnits" -> secondaryUnitsMode = label; "TP" -> tpMode = label; "SecondaryTP" -> secondaryTpMode = label; "SL" -> slMode = label; "SecondarySL" -> secondarySlMode = label; "TIF" -> timeInForce = label }
                                    showModeSelector = null 
                                }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (hasInfo) { Spacer(modifier = Modifier.width(4.dp)); Icon(Icons.Outlined.Info, null, tint = if (isSelected) Color.Black else Color(0xFF787B86), modifier = Modifier.size(18.dp)) }
                                    }
                                    if (value.isNotEmpty()) Text(value, color = if (isSelected) Color.Black else Color(0xFF787B86), fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
            
            // Presets and More menu...
            if (showPresets) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { showPresets = false }) {
                    Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp), color = Color(0xFF121212), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                        Column(modifier = Modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.RocketLaunch, null, tint = Color(0xFF787B86), modifier = Modifier.size(64.dp))
                                Icon(Icons.Outlined.Edit, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp).offset(y = 12.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp)); Text("No order presets created yet", color = Color(0xFF787B86), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(24.dp)); Divider(color = Color(0xFF2A2E39), modifier = Modifier.fillMaxWidth())
                            Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CloudUpload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp)); Text("Save order preset...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            if (showMoreMenu) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { showMoreMenu = false }) {
                    Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp), color = Color(0xFF121212), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { showMoreMenu = false; onTradingSettingsClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp)); Text("Trading settings...", color = Color.White, fontSize = 16.sp)
                            }
                            Divider(color = Color(0xFF2A2E39), modifier = Modifier.fillMaxWidth())
                            Row(modifier = Modifier.fillMaxWidth().clickable { slEnablesQuantityInRisk = !slEnablesQuantityInRisk }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = slEnablesQuantityInRisk, onCheckedChange = { slEnablesQuantityInRisk = it }, colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black))
                                Spacer(modifier = Modifier.width(12.dp)); Text("SL enables quantity in risk", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showExitLevelsModal) {
        ExitLevelsModal(
            symbol = symbol,
            orderType = if (selectedTab == "Market Execution") (if (selectedType == "buy") "Buy" else "Sell") else selectedTab,
            entryPrice = entryPrice,
            initialUnits = unitsInput,
            initialTp = tpPriceInput,
            initialSl = slPriceInput,
            onClose = { showExitLevelsModal = false },
            onConfirm = { confirmedLevels ->
                val appliedLevel = confirmedLevels.lastOrNull()
                if (appliedLevel != null) {
                    val sanitizedUnits = appliedLevel.units.trim()
                    val sanitizedTp = appliedLevel.tp.replace(",", "").trim()
                    val sanitizedSl = appliedLevel.sl.replace(",", "").trim()

                    if (sanitizedUnits.isNotEmpty()) {
                        unitsInput = sanitizedUnits
                    }
                    tpPriceInput = sanitizedTp
                    slPriceInput = sanitizedSl
                    tpEnabled = sanitizedTp.toFloatOrNull() != null
                    slEnabled = sanitizedSl.toFloatOrNull() != null
                }
                showExitLevelsModal = false
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 14.sp)
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
