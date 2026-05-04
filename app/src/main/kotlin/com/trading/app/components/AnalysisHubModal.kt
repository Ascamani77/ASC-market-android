package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisHubModal(
    onClose: () -> Unit,
    onIndicatorClick: () -> Unit = {},
    onCompareClick: () -> Unit = {},
    onAlertClick: () -> Unit = {},
    onReplayClick: () -> Unit = {},
    onObjectTreeClick: () -> Unit = {},
    onChartTypeClick: () -> Unit = {},
    onNewsClick: () -> Unit = {},
    onLayersClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
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
            .fillMaxHeight(0.93f)
            .padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Analysis hub",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // TOOLS Section
            HubSectionHeader("TOOLS")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubToolButton(
                    icon = Icons.Default.Insights,
                    label = "Indicators",
                    onClick = onIndicatorClick,
                    modifier = Modifier.weight(1f)
                )
                HubToolButton(
                    icon = Icons.Default.Add,
                    label = "Compare",
                    onClick = onCompareClick,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubToolButton(
                    icon = Icons.Default.AccessTime,
                    label = "Alerts",
                    onClick = onAlertClick,
                    modifier = Modifier.weight(1f),
                    hasDot = true
                )
                HubToolButton(
                    icon = Icons.Default.KeyboardDoubleArrowLeft,
                    label = "Bar Replay",
                    onClick = onReplayClick,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubToolButton(
                    icon = Icons.Default.BarChart,
                    label = "Chart type",
                    onClick = onChartTypeClick,
                    modifier = Modifier.weight(1f),
                    hasDot = false
                )
            }
        }
    }
}

@Composable
fun HubSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF787B86),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun HubToolButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    hasDot: Boolean = false
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
            .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
                if (hasDot) {
                    Box(
                        modifier = Modifier
                            .offset(x = 22.dp, y = (-2).dp)
                            .size(6.dp)
                            .background(Color(0xFFF23645), RoundedCornerShape(3.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color(0xFFD1D4DC),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}
