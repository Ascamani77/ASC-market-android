package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight

data class ChartTypeItem(
    val id: String,
    val name: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartTypeModal(
    currentStyle: String,
    onStyleChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showFavoritesTip by remember { mutableStateOf(true) }

    val chartTypes = listOf(
        ChartTypeItem("bars", "Bars", Icons.Default.Reorder),
        ChartTypeItem("candles", "Candles", Icons.Default.BarChart),
        ChartTypeItem("hollow_candles", "Hollow candles", Icons.Default.BarChart),
        ChartTypeItem("volume_candles", "Volume candles", Icons.Default.BarChart),
        ChartTypeItem("line", "Line", Icons.Default.ShowChart),
        ChartTypeItem("line_markers", "Line with markers", Icons.Default.ShowChart),
        ChartTypeItem("step_line", "Step line", Icons.Default.StackedLineChart),
        ChartTypeItem("area", "Area", Icons.Default.AreaChart),
        ChartTypeItem("hlc_area", "HLC area", Icons.Default.AreaChart),
        ChartTypeItem("baseline", "Baseline", Icons.Default.HorizontalRule),
        ChartTypeItem("columns", "Columns", Icons.Default.BarChart),
        ChartTypeItem("high_low", "High-low", Icons.Default.VerticalAlignBottom),
        ChartTypeItem("volume_footprint", "Volume footprint", Icons.Default.FormatAlignLeft),
        ChartTypeItem("tpo", "Time Price Opportunity", Icons.Default.GridView),
        ChartTypeItem("svp", "Session volume profile", Icons.Default.AlignHorizontalLeft),
        ChartTypeItem("heikin_ashi", "Heikin Ashi", Icons.Default.BarChart),
        ChartTypeItem("renko", "Renko", Icons.Default.GridView),
        ChartTypeItem("line_break", "Line break", Icons.Default.FormatAlignLeft),
        ChartTypeItem("kagi", "Kagi", Icons.Default.ShowChart),
        ChartTypeItem("point_figure", "Point & Figure", Icons.Default.Close),
        ChartTypeItem("range", "Range", Icons.Default.Height)
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF131722),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363A45))
            )
        },
        windowInsets = WindowInsets(0),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Chart type",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (showFavoritesTip) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF363A45), RoundedCornerShape(12.dp))
                        .background(Color(0xFF121212).copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TouchApp,
                            null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Use a long tap to add the chart type to your favorites",
                            color = Color(0xFFD1D4DC),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showFavoritesTip = false }) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(chartTypes) { type ->
                    val isActive = currentStyle == type.id
                    Box(
                        modifier = Modifier
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) Color.White else Color(0xFF121212))
                            .clickable { 
                                onStyleChange(type.id)
                                onClose()
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = type.icon,
                                contentDescription = null,
                                tint = if (isActive) Color.Black else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = type.name,
                                color = if (isActive) Color.Black else Color(0xFFD1D4DC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
