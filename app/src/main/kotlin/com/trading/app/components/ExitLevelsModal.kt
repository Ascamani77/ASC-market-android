package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import com.trading.app.models.SymbolInfo
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitLevelsModal(
    symbol: String,
    orderType: String,
    entryPrice: Float,
    initialUnits: String,
    initialTp: String,
    initialSl: String,
    onClose: () -> Unit,
    onConfirm: (List<ExitLevel>) -> Unit
) {
    var levels by remember { 
        mutableStateOf(
            listOf(
                ExitLevel(
                    id = 1, 
                    units = initialUnits, 
                    tp = initialTp, 
                    sl = initialSl
                )
            )
        ) 
    }
    val scrollState = rememberScrollState()

    val totalExitUnits = levels.sumOf { it.units.toDoubleOrNull() ?: 0.0 }
    val orderUnits = initialUnits.toDoubleOrNull() ?: 1.0
    val protectedPercent = if (orderUnits != 0.0) (totalExitUnits / orderUnits) * 100.0 else 0.0
    val topSectionBackground = Color(0xFF0B0E14)
    val topSectionHeight = 8.dp
    val contentHorizontalPadding = 10.dp

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(
                    "Exit levels",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Icon(
                    Icons.Outlined.HelpOutline,
                    null,
                    tint = Color(0xFF787B86),
                    modifier = Modifier.size(18.dp).padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = contentHorizontalPadding)
            ) {
                // Asset Info
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetIcon(SymbolInfo(ticker = symbol, name = "", type = "Crypto"), size = 32)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "$orderType Market $initialUnits @ ${formatPrice(entryPrice, symbol)}",
                            color = Color(0xFF787B86),
                            fontSize = 13.sp
                        )
                    }
                }

                // Protected size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Protected size", color = Color(0xFF787B86), fontSize = 14.sp)
                    Text(" • ${String.format("%.2f%%", protectedPercent)}", color = Color.White, fontSize = 14.sp)
                    if (protectedPercent > 100.0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF2A52C), modifier = Modifier.size(14.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2A2E39))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((protectedPercent / 100.0).toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(if (protectedPercent > 100.0) Color(0xFFF2A52C) else Color(0xFF089981))
                    )
                }

                Text(
                    "Exits • Risk/Reward 3.24",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Render Levels
                levels.forEachIndexed { index, level ->
                    LevelItem(
                        symbol = symbol,
                        entryPrice = entryPrice,
                        level = level,
                        index = index,
                        isLast = index == levels.size - 1,
                        onUpdate = { updated ->
                            levels = levels.toMutableList().apply { set(index, updated) }
                        },
                        onDelete = {
                            if (levels.size > 1) {
                                levels = levels.toMutableList().apply { removeAt(index) }
                            }
                        }
                    )
                }

                // Add level button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp)
                        .clickable {
                            levels = levels + ExitLevel(
                                id = levels.size + 1, 
                                units = "1",
                                tp = formatPrice(entryPrice * 1.01f, symbol),
                                sl = formatPrice(entryPrice * 0.99f, symbol)
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+ Add level", color = Color(0xFF2962FF), fontSize = 14.sp)
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Discard", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onConfirm(levels) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LevelItem(
    symbol: String,
    entryPrice: Float,
    level: ExitLevel,
    index: Int,
    isLast: Boolean,
    onUpdate: (ExitLevel) -> Unit,
    onDelete: () -> Unit
) {
    val tickSize = if (symbol.uppercase().contains("BTC")) 1f else if (symbol.length == 6 || symbol.contains("/")) 0.00001f else 0.01f
    val fieldBackground = Color(0xFF121212)
    val fieldBorder = Color(0xFF2A2E39)
    
    val tpVal = level.tp.replace(",", "").toFloatOrNull() ?: entryPrice
    val slVal = level.sl.replace(",", "").toFloatOrNull() ?: entryPrice
    
    val tpTicks = abs(tpVal - entryPrice) / tickSize
    val slTicks = abs(slVal - entryPrice) / tickSize

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Level ${index + 1} • 100.00%", color = Color(0xFF787B86), fontSize = 13.sp)
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = Color(0xFF787B86), modifier = Modifier.size(18.dp))
            }
        }

        if (!isLast) {
            // Summary view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(fieldBackground, RoundedCornerShape(4.dp))
                    .border(1.dp, fieldBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TP", color = Color(0xFF089981), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(" ${level.units} @ ${level.tp}", color = Color.White, fontSize = 14.sp)
                Text(" • ", color = Color(0xFF787B86))
                Text("SL", color = Color(0xFFF23645), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(" ${level.units} @ ${level.sl}", color = Color.White, fontSize = 14.sp)
            }
        } else {
            // Expanded editable view
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Units
                Text("Units", color = Color(0xFF787B86), fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(fieldBackground, RoundedCornerShape(4.dp))
                        .border(1.dp, fieldBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = level.units,
                        onValueChange = { onUpdate(level.copy(units = it)) },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF787B86), modifier = Modifier.size(18.dp))
                    Text("100.00%", color = Color(0xFF787B86), fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }

                // Take Profit
                Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Take profit, price", color = Color(0xFF787B86), fontSize = 12.sp)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(14.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(fieldBackground, RoundedCornerShape(4.dp))
                        .border(1.dp, fieldBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = level.tp,
                        onValueChange = { onUpdate(level.copy(tp = it)) },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF787B86), modifier = Modifier.size(18.dp))
                    Text(String.format("%.0f ticks", tpTicks), color = Color(0xFF787B86), fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(14.dp))
                }

                // Stop Loss
                Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Stop loss, price", color = Color(0xFF787B86), fontSize = 12.sp)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(14.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(fieldBackground, RoundedCornerShape(4.dp))
                        .border(1.dp, fieldBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = level.sl,
                        onValueChange = { onUpdate(level.copy(sl = it)) },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF787B86), modifier = Modifier.size(18.dp))
                    Text(String.format("%.0f ticks", slTicks), color = Color(0xFF787B86), fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private fun formatPrice(price: Float, symbol: String = ""): String {
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

data class ExitLevel(
    val id: Int,
    val units: String = "1",
    val tp: String = "",
    val sl: String = ""
)
