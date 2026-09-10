package com.asc.markets.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*

@Composable
fun PostMoveAuditHeader(
    showMainHeader: MutableState<Boolean>,
    filterState: MutableState<String>,
    viewModel: ForexViewModel,
    showFilters: MutableState<Boolean>,
    filterDirection: MutableState<String?>,
    filterOutcome: MutableState<String?>,
    onExportCsv: () -> Unit,
    onMarkAllReviewed: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val headerTopPad = (configuration.screenHeightDp * 0.02f).dp

    var mainHeaderHeightPx by remember { mutableStateOf(0) }
    val targetOffset = if (showMainHeader.value) 0 else -mainHeaderHeightPx
    val animatedOffset by animateIntAsState(targetValue = targetOffset, animationSpec = tween(180))

    var moreOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.offset { IntOffset(0, animatedOffset) }.background(DeepBlack)) {
        // Title bar
        Surface(color = PureBlack, modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { mainHeaderHeightPx = it.size.height }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = headerTopPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("POST-MOVE AUDIT", color = Color.White, style = TerminalTypography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily))
                }
                Row(modifier = Modifier.wrapContentWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showFilters.value = !showFilters.value }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (showFilters.value) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                            contentDescription = "Filter",
                            tint = if (filterDirection.value != null || filterOutcome.value != null) EmeraldSuccess else Color.White
                        )
                    }
                    Box {
                        IconButton(onClick = { moreOpen = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(text = { Text("Export CSV") }, onClick = { onExportCsv(); moreOpen = false })
                            DropdownMenuItem(text = { Text("Mark All Reviewed") }, onClick = { onMarkAllReviewed(); moreOpen = false })
                            DropdownMenuItem(text = { Text("Clear Ledger") }, onClick = { viewModel.clearAuditLedger(); moreOpen = false })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter tabs — market overview style (plain labels + white underline on active)
        val tabs = listOf("ALL", "TRADES", "AI OUTCOMES", "TARGET HIT", "INVALIDATED", "UNRESOLVED")
        val tabLabel = mapOf(
            "ALL" to "All",
            "TRADES" to "Trades",
            "AI OUTCOMES" to "AI Outcomes",
            "TARGET HIT" to "Target hit",
            "INVALIDATED" to "Invalidated",
            "UNRESOLVED" to "Unresolved"
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs.size) { index ->
                val label = tabs[index]
                val isSelected = label == filterState.value
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { filterState.value = label }
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabLabel[label] ?: label,
                        color = if (isSelected) Color.White else Color(0xFF8E8E8E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(if (isSelected) Color.White else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(6.dp))
    }
}
