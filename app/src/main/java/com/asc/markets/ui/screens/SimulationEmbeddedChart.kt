package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.components.ChartSettingsBottomSheet
import com.trading.app.components.TradingChart2
import com.trading.app.models.CanvasSettings
import com.trading.app.models.ChartSettings
import com.trading.app.models.Drawing
import com.trading.app.models.Position
import com.trading.app.models.QuickActionsSettings
import com.trading.app.models.ScalesSettings
import com.trading.app.models.StatusLineSettings

@Stable
class SimulationEmbeddedChartState(
    initialSymbol: String,
    initialTimeframe: String
) {
    var symbol by mutableStateOf(initialSymbol)
    var timeframe by mutableStateOf(initialTimeframe)
    var chartStyle by mutableStateOf("candles")
    var chartSettings by mutableStateOf(defaultSimulationChartSettings())
    var showSettingsSheet by mutableStateOf(false)
    var selectedIndicatorId by mutableStateOf<String?>(null)
    val drawings = mutableStateListOf<Drawing>()
    val positions = mutableStateListOf<Position>()
}

@Composable
fun rememberEmbeddedSimulationChartState(
    symbol: String = "BTCUSD",
    timeframe: String = "1d"
): SimulationEmbeddedChartState {
    return remember(symbol, timeframe) {
        SimulationEmbeddedChartState(
            initialSymbol = symbol,
            initialTimeframe = timeframe
        )
    }
}

@Composable
fun EmbeddedSimulationChartSection(
    chartState: SimulationEmbeddedChartState,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var symbolMenuExpanded by remember { mutableStateOf(false) }
    val toolbarBackground = Color(0xFF0F1116)
    val toolbarBorder = Color(0xFF1E222D)
    val actionColor = Color(0xFFB8BCC7)

    if (compact) {
        // Compact mode - just the mini chart, no toolbar
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = Color(0xFF0B0D12),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, toolbarBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Use MiniChart for compact baseline view
                val mockData = remember(chartState.symbol) {
                    generateBaselineData(chartState.symbol)
                }
                com.asc.markets.ui.components.MiniChart(
                    values = mockData,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0B0D12),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, toolbarBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(toolbarBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { symbolMenuExpanded = true },
                    color = Color(0xFF161A22),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, toolbarBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = chartState.symbol,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = symbolMenuExpanded,
                    onDismissRequest = { symbolMenuExpanded = false }
                ) {
                    listOf("BTCUSD", "AAPL", "EURUSD", "XAUUSD").forEach { symbol ->
                        DropdownMenuItem(
                            text = { Text(symbol, color = Color.White) },
                            onClick = {
                                chartState.symbol = symbol
                                symbolMenuExpanded = false
                            }
                        )
                    }
                }

                ChartToolbarIconButton(
                    icon = Icons.Default.Add,
                    tint = actionColor,
                    onClick = { symbolMenuExpanded = true }
                )

                ChartToolbarTimeframeChip(
                    label = "1m",
                    isSelected = chartState.timeframe == "1m",
                    onClick = { chartState.timeframe = "1m" }
                )
                ChartToolbarTimeframeChip(
                    label = "30m",
                    isSelected = chartState.timeframe == "30m",
                    onClick = { chartState.timeframe = "30m" }
                )
                ChartToolbarTimeframeChip(
                    label = "1h",
                    isSelected = chartState.timeframe == "1h",
                    onClick = { chartState.timeframe = "1h" }
                )
                ChartToolbarTimeframeChip(
                    label = "D",
                    isSelected = chartState.timeframe == "1d",
                    onClick = { chartState.timeframe = "1d" }
                )

                ChartToolbarIconButton(
                    icon = Icons.Default.SwapHoriz,
                    tint = actionColor,
                    onClick = {
                        chartState.symbol = when (chartState.symbol) {
                            "BTCUSD" -> "AAPL"
                            "AAPL" -> "EURUSD"
                            "EURUSD" -> "XAUUSD"
                            else -> "BTCUSD"
                        }
                    }
                )
                ChartToolbarIconButton(
                    icon = Icons.Default.Tune,
                    tint = actionColor,
                    onClick = { chartState.showSettingsSheet = true }
                )
                ChartToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    tint = actionColor,
                    onClick = {
                        chartState.chartStyle = if (chartState.chartStyle == "candles") "line" else "candles"
                    }
                )
                ChartToolbarIconButton(
                    icon = Icons.Default.PhotoCamera,
                    tint = actionColor,
                    onClick = { chartState.showSettingsSheet = true }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(Color.Black)
            ) {
                TradingChart2(
                    symbol = chartState.symbol,
                    timeframe = chartState.timeframe,
                    style = chartState.chartStyle,
                    chartSettings = chartState.chartSettings,
                    drawings = chartState.drawings,
                    onDrawingUpdate = { updatedDrawing ->
                        val existingIndex = chartState.drawings.indexOfFirst { it.id == updatedDrawing.id }
                        if (existingIndex >= 0) {
                            chartState.drawings[existingIndex] = updatedDrawing
                        } else {
                            chartState.drawings.add(updatedDrawing)
                        }
                    },
                    activeTool = "cursor",
                    onToolReset = {},
                    showVolume = true,
                    showVolumeMa = false,
                    isLocked = false,
                    isVisible = true,
                    selectedCurrency = currencyForSymbol(chartState.symbol),
                    onCurrencyClick = {},
                    onLongPress = { chartState.showSettingsSheet = true },
                    onSettingsClick = { chartState.showSettingsSheet = true },
                    selectedTimeZone = "UTC",
                    positions = chartState.positions,
                    onPositionUpdate = {},
                    onPositionDelete = {},
                    selectedIndicatorId = chartState.selectedIndicatorId,
                    onSelectedIndicatorIdChange = { chartState.selectedIndicatorId = it }
                )
            }
        }
    }

    if (chartState.showSettingsSheet) {
        ChartSettingsBottomSheet(
            settings = chartState.chartSettings,
            onUpdate = { chartState.chartSettings = it },
            onDismissRequest = { chartState.showSettingsSheet = false },
            onMoreSettingsClick = {}
        )
    }
}

// Generate deterministic baseline data based on symbol
private fun generateBaselineData(symbol: String): List<Double> {
    val seed = symbol.hashCode()
    val basePrice = when {
        symbol.contains("BTC", ignoreCase = true) -> 65000.0
        symbol.contains("ETH", ignoreCase = true) -> 3500.0
        symbol.contains("EUR", ignoreCase = true) -> 1.08
        symbol.contains("GBP", ignoreCase = true) -> 1.26
        symbol.contains("AAPL", ignoreCase = true) -> 180.0
        else -> 100.0
    }
    
    return List(50) { index ->
        val trend = kotlin.math.sin((index + seed) * 0.2) * 0.02
        val noise = kotlin.math.cos((index + seed) * 0.5) * 0.01
        basePrice * (1 + trend + noise + (index * 0.0005))
    }
}

@Composable
private fun ChartToolbarTimeframeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF2A2F39) else Color.Transparent,
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF8C92A0),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ChartToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun defaultSimulationChartSettings(): ChartSettings {
    return ChartSettings(
        statusLine = StatusLineSettings(
            symbol = true,
            titleMode = "Description",
            openMarketStatus = false,
            ohlc = false,
            volume = true
        ),
        scales = ScalesSettings(
            symbolLastPriceLabel = true,
            symbolPrevCloseLine = false,
            highLowPriceLines = false
        ),
        canvas = CanvasSettings(
            background = "#0B0D12",
            fullChartColor = "Pure Black",
            gridOpacity = 18,
            scaleTextColor = "#AEB4C0",
            scaleLineColor = "#232834",
            marginTop = 18,
            marginBottom = 4,
            marginRight = 12,
            headerVisible = false
        ),
        quickActions = QuickActionsSettings(isSidebarVisible = false)
    )
}

private fun currencyForSymbol(symbol: String): String {
    val normalized = symbol.uppercase()
    return when {
        normalized.endsWith("USDT") -> "USDT"
        normalized.endsWith("JPY") -> "JPY"
        normalized.endsWith("GBP") -> "GBP"
        normalized.endsWith("EUR") -> "EUR"
        else -> "USD"
    }
}
