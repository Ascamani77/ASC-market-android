package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import com.trading.app.models.ChartSettings
import com.trading.app.models.ScalesSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartSettingsBottomSheet(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onDismissRequest: () -> Unit,
    onMoreSettingsClick: () -> Unit,
    onResetScale: () -> Unit = {},
    onAutoToggle: (Boolean) -> Unit = {},
    onScaleTypeChange: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    var currentPage by remember { mutableStateOf("Main") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF121212), // Charcoal black
        dragHandle = {
            if (currentPage == "Main") {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .width(42.dp)
                        .height(5.dp)
                        .background(Color(0xFF434651), shape = MaterialTheme.shapes.extraLarge)
                )
            }
        },
        windowInsets = WindowInsets(0),
        modifier = Modifier.padding(bottom = AppBottomNavHeight)
    ) {
        when (currentPage) {
            "Main" -> {
                val scales = settings.scales
                val isScaleModeLocked = scales.lockRatio
                val activeCheckColor = Color(0xFFD1D4DC)
                val disabledCheckColor = Color(0xFF787B86)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 30.dp)
                ) {
                    BottomSheetItem(
                        label = "Reset price scale",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            onUpdate(
                                settings.copy(
                                    scales = scales.copy(
                                        autoScale = true,
                                        lockRatio = false
                                    )
                                )
                            )
                            onAutoToggle(true)
                            onResetScale()
                            onDismissRequest()
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    BottomSheetItem(
                        label = "Auto (fits data to screen)",
                        enabled = !isScaleModeLocked,
                        trailing = {
                            if (scales.autoScale) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (isScaleModeLocked) disabledCheckColor else activeCheckColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            val nextAuto = !scales.autoScale
                            onUpdate(settings.copy(scales = scales.copy(autoScale = nextAuto)))
                            onAutoToggle(nextAuto)
                        }
                    )
                    BottomSheetItem(
                        label = "Lock price to bar ratio",
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(scales.lockRatioValue, color = Color(0xFF787B86), fontSize = 15.sp)
                                if (scales.lockRatio) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Check, null, tint = activeCheckColor, modifier = Modifier.size(24.dp))
                                }
                            }
                        },
                        onClick = {
                            val nextLock = !scales.lockRatio
                            onUpdate(
                                settings.copy(
                                    scales = scales.copy(
                                        lockRatio = nextLock,
                                        autoScale = if (nextLock) false else scales.autoScale
                                    )
                                )
                            )
                        }
                    )
                    BottomSheetItem(
                        label = "Scale price chart only",
                        trailing = {
                            if (scales.scalePriceChartOnly) {
                                Icon(Icons.Default.Check, null, tint = activeCheckColor, modifier = Modifier.size(24.dp))
                            }
                        },
                        onClick = {
                            onUpdate(
                                settings.copy(
                                    scales = scales.copy(scalePriceChartOnly = !scales.scalePriceChartOnly)
                                )
                            )
                        }
                    )
                    BottomSheetItem(
                        label = "Invert scale",
                        trailing = {
                            if (scales.invertScale) {
                                Icon(Icons.Default.Check, null, tint = activeCheckColor, modifier = Modifier.size(24.dp))
                            }
                        },
                        onClick = {
                            onUpdate(settings.copy(scales = scales.copy(invertScale = !scales.invertScale)))
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    BottomSheetItem(
                        label = "Hide header pane",
                        trailing = {
                            if (scales.hideHeaderPane) Icon(Icons.Default.Check, null, tint = activeCheckColor, modifier = Modifier.size(24.dp))
                        },
                        onClick = { onUpdate(settings.copy(scales = scales.copy(hideHeaderPane = !scales.hideHeaderPane))) }
                    )
                    BottomSheetItem(
                        label = "Hide asset lastviewed pane",
                        trailing = {
                            if (scales.hideAssetLastViewedPane) Icon(Icons.Default.Check, null, tint = activeCheckColor, modifier = Modifier.size(24.dp))
                        },
                        onClick = { onUpdate(settings.copy(scales = scales.copy(hideAssetLastViewedPane = !scales.hideAssetLastViewedPane))) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    BottomSheetItem(
                        label = "Regular",
                        enabled = !isScaleModeLocked,
                        trailing = {
                            if (scales.scaleType == "Regular") {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (isScaleModeLocked) disabledCheckColor else activeCheckColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            onUpdate(settings.copy(scales = scales.copy(scaleType = "Regular")))
                            onScaleTypeChange("Regular")
                        }
                    )
                    BottomSheetItem(
                        label = "Percent",
                        enabled = !isScaleModeLocked,
                        trailing = {
                            if (scales.scaleType == "Percent") {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (isScaleModeLocked) disabledCheckColor else activeCheckColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            onUpdate(settings.copy(scales = scales.copy(scaleType = "Percent")))
                            onScaleTypeChange("Percent")
                        }
                    )
                    BottomSheetItem(
                        label = "Indexed to 100",
                        enabled = !isScaleModeLocked,
                        trailing = {
                            if (scales.scaleType == "Indexed to 100") {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (isScaleModeLocked) disabledCheckColor else activeCheckColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            onUpdate(settings.copy(scales = scales.copy(scaleType = "Indexed to 100")))
                            onScaleTypeChange("Indexed to 100")
                        }
                    )
                    BottomSheetItem(
                        label = "Logarithmic",
                        enabled = !isScaleModeLocked,
                        trailing = {
                            if (scales.scaleType == "Logarithmic") {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (isScaleModeLocked) disabledCheckColor else activeCheckColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            onUpdate(settings.copy(scales = scales.copy(scaleType = "Logarithmic")))
                            onScaleTypeChange("Logarithmic")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    BottomSheetItem(
                        label = "Move scale to left",
                        trailing = {
                            if (scales.scalesPlacement == "Left") {
                                Icon(Icons.Default.Check, null, tint = activeCheckColor, modifier = Modifier.size(24.dp))
                            }
                        },
                        onClick = {
                            val nextPlacement = if (scales.scalesPlacement == "Left") "Right" else "Left"
                            onUpdate(settings.copy(scales = scales.copy(scalesPlacement = nextPlacement)))
                        }
                    )
                    BottomSheetItem(
                        label = "Labels", 
                        trailing = { Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF787B86), modifier = Modifier.size(22.dp)) },
                        onClick = { currentPage = "Labels" }
                    )
                    BottomSheetItem(
                        label = "Lines", 
                        trailing = { Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF787B86), modifier = Modifier.size(22.dp)) },
                        onClick = { currentPage = "Lines" }
                    )
                    BottomSheetItem(
                        label = "Plus button",
                        trailing = {
                            if (scales.plusButton) {
                                Icon(Icons.Default.Check, null, tint = activeCheckColor, modifier = Modifier.size(24.dp))
                            }
                        },
                        onClick = {
                            onUpdate(settings.copy(scales = scales.copy(plusButton = !scales.plusButton)))
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    BottomSheetItem(
                        label = "More settings...",
                        icon = Icons.Default.Settings,
                        onClick = onMoreSettingsClick
                    )
                }
            }
            "Labels" -> {
                LabelsPage(
                    scales = settings.scales,
                    onUpdate = { onUpdate(settings.copy(scales = it)) },
                    onBack = { currentPage = "Main" }
                )
            }
            "Lines" -> {
                LinesPage(
                    scales = settings.scales,
                    onUpdate = { onUpdate(settings.copy(scales = it)) },
                    onBack = { currentPage = "Main" }
                )
            }
        }
    }
}

@Composable
fun LabelsPage(
    scales: ScalesSettings,
    onUpdate: (ScalesSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 30.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack, 
                    contentDescription = "Back",
                    tint = Color(0xFFD1D4DC),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Labels",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Options
        LabelOption(
            "Symbol name label", 
            isChecked = scales.symbolNameLabel,
            onClick = { onUpdate(scales.copy(symbolNameLabel = !scales.symbolNameLabel)) }
        )
        LabelOption(
            "Symbol last price label", 
            isChecked = scales.symbolLastPriceLabel,
            onClick = { onUpdate(scales.copy(symbolLastPriceLabel = !scales.symbolLastPriceLabel)) }
        )
        LabelOption(
            "Symbol previous day close price label", 
            isChecked = scales.symbolPrevCloseLabel,
            onClick = { onUpdate(scales.copy(symbolPrevCloseLabel = !scales.symbolPrevCloseLabel)) }
        )
        LabelOption(
            "Pre/post market price label", 
            isChecked = scales.prePostMarketPriceLabel,
            enabled = true,
            onClick = { onUpdate(scales.copy(prePostMarketPriceLabel = !scales.prePostMarketPriceLabel)) }
        )
        LabelOption(
            "High and low price labels", 
            isChecked = scales.highLowPriceLabels,
            onClick = { onUpdate(scales.copy(highLowPriceLabels = !scales.highLowPriceLabels)) }
        )
        LabelOption(
            "Bid and ask labels", 
            isChecked = scales.bidAskLabels,
            onClick = { onUpdate(scales.copy(bidAskLabels = !scales.bidAskLabels)) }
        )
        LabelOption(
            "Indicators and financials name labels", 
            isChecked = scales.indicatorsAndFinancialsNameLabels,
            onClick = { onUpdate(scales.copy(indicatorsAndFinancialsNameLabels = !scales.indicatorsAndFinancialsNameLabels)) }
        )
        LabelOption(
            "Indicators and financials value labels", 
            isChecked = scales.indicatorsAndFinancialsValueLabels,
            onClick = { onUpdate(scales.copy(indicatorsAndFinancialsValueLabels = !scales.indicatorsAndFinancialsValueLabels)) }
        )
        LabelOption(
            "Countdown to bar close", 
            isChecked = scales.countdown,
            onClick = { onUpdate(scales.copy(countdown = !scales.countdown)) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))
        
        LabelOption(
            "No overlapping labels", 
            isChecked = scales.noOverlappingLabels,
            onClick = { onUpdate(scales.copy(noOverlappingLabels = !scales.noOverlappingLabels)) }
        )
    }
}

@Composable
fun LinesPage(
    scales: ScalesSettings,
    onUpdate: (ScalesSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 30.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack, 
                    contentDescription = "Back",
                    tint = Color(0xFFD1D4DC),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Lines",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Options
        LabelOption(
            "Price line", 
            isChecked = scales.symbolLastPriceLine,
            onClick = { onUpdate(scales.copy(symbolLastPriceLine = !scales.symbolLastPriceLine)) }
        )
        LabelOption(
            "Previous day close price line", 
            isChecked = scales.symbolPrevCloseLine,
            onClick = { onUpdate(scales.copy(symbolPrevCloseLine = !scales.symbolPrevCloseLine)) }
        )
        LabelOption(
            "Pre/post market price line", 
            isChecked = scales.prePostMarketPriceLine,
            onClick = { onUpdate(scales.copy(prePostMarketPriceLine = !scales.prePostMarketPriceLine)) }
        )
        LabelOption(
            "High and low price lines", 
            isChecked = scales.highLowPriceLines,
            onClick = { onUpdate(scales.copy(highLowPriceLines = !scales.highLowPriceLines)) }
        )
        LabelOption(
            "Bid and ask lines", 
            isChecked = scales.bidAskLines,
            onClick = { onUpdate(scales.copy(bidAskLines = !scales.bidAskLines)) }
        )
    }
}

@Composable
fun LabelOption(label: String, isChecked: Boolean = false, enabled: Boolean = true, onClick: () -> Unit = {}) {
    val contentColor = if (enabled) Color(0xFFD1D4DC) else Color(0xFF787B86).copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 19.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(36.dp)) {
            if (isChecked) {
                Icon(
                    Icons.Default.Check, 
                    null, 
                    tint = contentColor, 
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = label,
            color = contentColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BottomSheetItem(
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    val contentColor = if (enabled) Color(0xFFD1D4DC) else Color(0xFF787B86).copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 19.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(20.dp))
        }
        Text(
            text = label,
            color = contentColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        }
    }
}
