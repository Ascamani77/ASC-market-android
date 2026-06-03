package com.asc.markets.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import com.asc.markets.data.BinanceDataStore
import com.asc.markets.data.CombinedFallbackDataStore
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.TimedPrice
import com.asc.markets.data.WatchlistItem
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun WatchlistScreen(
    viewModel: ForexViewModel,
    onViewChart: (String) -> Unit = {},
    onSetAlert: (String) -> Unit = {},
    onDeepDive: (String) -> Unit = {}
) {
    val watchlistItems by viewModel.watchlistItems.collectAsState()
    val sortMode by viewModel.watchlistSortMode.collectAsState()
    val categoryFilter by viewModel.watchlistCategoryFilter.collectAsState()
    val hiddenIds by viewModel.hiddenWatchlistIds.collectAsState()
    val compactMode by viewModel.watchlistCompactMode.collectAsState()
    val isAnalyzing by viewModel.isWatchlistAnalyzing.collectAsState()
    val lastUpdate by viewModel.lastWatchlistUpdate.collectAsState()
    
    var activeTimeframe by remember { mutableStateOf("H1") }
    var showTimeframeDropdown by remember { mutableStateOf(false) }
    val marketPairs by MarketDataStore.allPairs.collectAsState()
    val binancePairs by BinanceDataStore.allPairs.collectAsState()
    val fallbackPairs by CombinedFallbackDataStore.allPairs.collectAsState()
    val allPairs = remember(marketPairs, binancePairs, fallbackPairs) {
        (marketPairs + binancePairs + fallbackPairs).distinctBy { it.symbol }
    }

    val categoryCounts = remember(watchlistItems) {
        watchlistItems.groupingBy { it.category }.eachCount()
    }

    val filteredItems = remember(watchlistItems, sortMode, categoryFilter, hiddenIds) {
        watchlistItems
            .filter { !hiddenIds.contains(it.id) && (categoryFilter == null || it.category == categoryFilter) }
            .sortedWith(
                when (sortMode) {
                    ForexViewModel.WatchlistSortMode.PROBABILITY -> compareByDescending { it.moveProbability }
                    ForexViewModel.WatchlistSortMode.CONFIDENCE -> compareByDescending { it.confidence }
                    ForexViewModel.WatchlistSortMode.VOLATILITY -> compareByDescending { it.volatilityScore }
                    ForexViewModel.WatchlistSortMode.TIME_TO_EVENT -> compareBy { parseTimeToEventMinutes(it.timeToEvent) }
                }
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI WATCHLIST",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
                val ago = remember(lastUpdate) { formatTimeAgo(lastUpdate) }
                Text(
                    text = "Updated $ago  •  ${filteredItems.size} assets",
                    color = SlateText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Timeframe Dropdown
                Box {
                    Text(
                        text = activeTimeframe,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GhostWhite)
                            .clickable { showTimeframeDropdown = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    DropdownMenu(
                        expanded = showTimeframeDropdown,
                        onDismissRequest = { showTimeframeDropdown = false },
                        modifier = Modifier.background(ActiveHighlight)
                    ) {
                        listOf("M15", "M30", "H1", "H4", "D1").forEach { tf ->
                            DropdownMenuItem(
                                text = { Text(tf, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    activeTimeframe = tf
                                    showTimeframeDropdown = false
                                    viewModel.refreshWatchlist()
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (compactMode) "LIST" else "CARD",
                    color = if (compactMode) Color.White else SlateText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { viewModel.toggleWatchlistCompactMode() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.refreshWatchlist() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isAnalyzing) IndigoAccent else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Analyzing indicator
        AnimatedVisibility(visible = isAnalyzing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = IndigoAccent,
                trackColor = Color.Transparent
            )
        }

        // Category filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip(
                    label = "ALL",
                    count = watchlistItems.size,
                    selected = categoryFilter == null,
                    onClick = { viewModel.setWatchlistCategoryFilter(null) }
                )
            }
            items(MarketCategory.values().toList()) { cat ->
                val count = categoryCounts[cat] ?: 0
                if (count > 0) {
                    CategoryChip(
                        label = cat.name,
                        count = count,
                        selected = categoryFilter == cat,
                        onClick = { viewModel.setWatchlistCategoryFilter(cat) }
                    )
                }
            }
        }

        // Sort bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ForexViewModel.WatchlistSortMode.values().forEach { mode ->
                SortChip(
                    label = mode.name.replace("_", " "),
                    selected = sortMode == mode,
                    onClick = { viewModel.setWatchlistSort(mode) }
                )
            }
        }

        // Content
        when {
            isAnalyzing && filteredItems.isEmpty() -> AnalyzingState()
            filteredItems.isEmpty() -> EmptyWatchlistState()
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        if (compactMode) {
                            WatchlistCompactCard(
                                item = item,
                                allPairs = allPairs,
                                onViewChart = onViewChart,
                                onSetAlert = onSetAlert,
                                onDeepDive = onDeepDive,
                                onHide = { viewModel.hideWatchlistItem(item.id) }
                            )
                        } else {
                            WatchlistExpandedCard(
                                item = item,
                                allPairs = allPairs,
                                activeTimeframe = activeTimeframe,
                                onViewChart = onViewChart,
                                onSetAlert = onSetAlert,
                                onDeepDive = onDeepDive,
                                onHide = { viewModel.hideWatchlistItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) IndigoAccent else GhostWhite,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (selected) Color.Black else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                color = if (selected) Color.Black.copy(alpha = 0.7f) else SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) IndigoAccent else SlateText,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = InterFontFamily,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

@Composable
private fun AnalyzingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = IndigoAccent, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "AI scanning markets for high-probability setups...",
                color = SlateText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun EmptyWatchlistState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NO ASSETS MATCH CURRENT CRITERIA",
                color = SlateText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The AI has not flagged any instruments matching your filter. Adjust category or sort settings, or refresh to re-scan.",
                color = SlateText.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun WatchlistCompactCard(
    item: WatchlistItem,
    allPairs: List<com.asc.markets.data.ForexPair>,
    onViewChart: (String) -> Unit,
    onSetAlert: (String) -> Unit,
    onDeepDive: (String) -> Unit,
    onHide: () -> Unit
) {
    val livePrice = remember(allPairs, item.assetName) {
        allPairs.find { MarketDataStore.matchesSymbol(it.symbol, item.assetName) }
    }
    val price = livePrice?.price ?: item.price
    val change = livePrice?.changePercent ?: item.changePercent

    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PairFlags(symbol = item.assetName, size = 27)
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.assetName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    if (item.isNew) {
                        Spacer(modifier = Modifier.width(6.dp))
                        NewBadge()
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.status,
                    color = SlateText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (price > 100) String.format("%.2f", price) else String.format("%.5f", price), // 5 decimals for forex (like MT5)
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                val changeColor = if (change >= 0) EmeraldSuccess else RoseError
                val sign = if (change >= 0) "+" else ""
                Text(
                    text = "$sign${String.format("%.2f", change)}%",
                    color = changeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${item.moveProbability}%",
                color = when {
                    item.moveProbability >= 80 -> RoseError
                    item.moveProbability >= 70 -> Color(0xFFF59E0B)
                    item.moveProbability >= 60 -> Color.Yellow
                    else -> EmeraldSuccess
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily,
                modifier = Modifier.width(36.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onHide, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Hide", tint = SlateText, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WatchlistExpandedCard(
    item: WatchlistItem,
    allPairs: List<com.asc.markets.data.ForexPair>,
    activeTimeframe: String = "H1",
    onViewChart: (String) -> Unit,
    onSetAlert: (String) -> Unit,
    onDeepDive: (String) -> Unit,
    onHide: () -> Unit
) {
    val livePrice = remember(allPairs, item.assetName) {
        allPairs.find { MarketDataStore.matchesSymbol(it.symbol, item.assetName) }
    }
    val price = livePrice?.price ?: item.price
    val change = livePrice?.changePercent ?: item.changePercent

    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: Name + NEW badge + hide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PairFlags(symbol = item.assetName, size = 32)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.assetName,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                            if (item.isNew) {
                                Spacer(modifier = Modifier.width(6.dp))
                                NewBadge()
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (price > 100) String.format("%.2f", price) else String.format("%.5f", price), // 5 decimals for forex (like MT5)
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val changeColor = if (change >= 0) EmeraldSuccess else RoseError
                            val sign = if (change >= 0) "+" else ""
                            Text(
                                text = "$sign${String.format("%.2f", change)}%",
                                color = changeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "BREAKOUT PROBABILITY",
                            color = SlateText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.moveProbability}%",
                            color = when {
                                item.moveProbability >= 80 -> RoseError
                                item.moveProbability >= 70 -> Color(0xFFF59E0B)
                                item.moveProbability >= 60 -> Color.Yellow
                                else -> EmeraldSuccess
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onHide, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Hide", tint = SlateText, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val barColor = when {
                item.moveProbability >= 80 -> RoseError
                item.moveProbability >= 70 -> Color(0xFFF59E0B)
                item.moveProbability >= 60 -> Color.Yellow
                else -> EmeraldSuccess
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROBABILITY SCORE",
                    color = SlateText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${item.moveProbability}%",
                    color = barColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = item.moveProbability / 100f,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = Color.White.copy(alpha = 0.05f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rationale
            if (item.rationale.isNotBlank()) {
                Text(
                    text = "AI RATIONALE",
                    color = SlateText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.rationale,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))

            // Details grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailItem(label = "STATUS", value = item.status)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailItem(
                        label = "DIRECTIONAL BIAS",
                        value = item.preMoveSignal,
                        valueColor = if (item.preMoveSignal.contains("Accumulation") || item.preMoveSignal.contains("Expansion")) EmeraldSuccess else Color(0xFFF59E0B)
                    )
                    if (item.triggerEvent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailItem(label = "TRIGGER", value = item.triggerEvent, valueColor = IndigoAccent)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    val riskColor = when {
                        item.newsRisk.contains("High", ignoreCase = true) -> RoseError
                        item.newsRisk.contains("Medium", ignoreCase = true) -> Color(0xFFF59E0B)
                        else -> EmeraldSuccess
                    }
                    DetailItem(label = "NEWS RISK", value = item.newsRisk, valueColor = riskColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailItem(label = "VOLATILITY SCORE", value = "${item.volatilityScore}/100")
                    if (item.timeToEvent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailItem(label = "TIME TO EVENT", value = item.timeToEvent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().offset(x = (-14).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(icon = Icons.Default.BarChart, contentDescription = "VIEW CHART", onClick = { onViewChart(item.assetName) })
                ActionButton(icon = Icons.Default.NotificationsActive, contentDescription = "SET ALERT", onClick = { onSetAlert(item.assetName) })
                ActionButton(icon = Icons.Default.Lightbulb, contentDescription = "DEEP DIVE", onClick = { onDeepDive(item.assetName) })
            }
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = contentDescription,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun NewBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = RoseError
    ) {
        Text(
            text = "NEW",
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun WatchlistSparkline(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val lineWidth = 2.dp.toPx()
        val glowWidth = 5.dp.toPx()
        val drawableHeight = size.height * 0.92f
        val topPadding = size.height * 0.02f
        val stepX = size.width / (points.lastIndex.coerceAtLeast(1))
        val isRed = color.red > color.green
        val fillTop = if (isRed) color.copy(alpha = 0.38f) else color.copy(alpha = 0.28f)
        val fillMid = if (isRed) color.copy(alpha = 0.18f) else color.copy(alpha = 0.11f)

        fun pt(index: Int): Offset {
            val n = points[index].coerceIn(0.03f, 0.97f)
            return Offset(index * stepX, topPadding + ((1f - n) * drawableHeight))
        }

        val linePath = Path()
        val fillPath = Path()
        val first = pt(0)
        linePath.moveTo(first.x, first.y)
        fillPath.moveTo(first.x, size.height)
        fillPath.lineTo(first.x, first.y)

        for (i in 1 until points.size) {
            val prev = pt(i - 1)
            val curr = pt(i)
            val ctrl = stepX * 0.45f
            linePath.cubicTo(prev.x + ctrl, prev.y, curr.x - ctrl, curr.y, curr.x, curr.y)
            fillPath.cubicTo(prev.x + ctrl, prev.y, curr.x - ctrl, curr.y, curr.x, curr.y)
        }

        val last = pt(points.lastIndex)
        fillPath.lineTo(last.x, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillTop, fillMid, Color.Transparent),
                startY = 0f, endY = size.height
            )
        )
        drawPath(path = linePath, color = color.copy(alpha = if (isRed) 0.18f else 0.14f), style = Stroke(width = glowWidth, cap = StrokeCap.Round))
        drawPath(path = linePath, color = color, style = Stroke(width = lineWidth, cap = StrokeCap.Round))
        // Blinking outer glow
        drawCircle(color = color.copy(alpha = blinkAlpha * 0.35f), radius = 7.dp.toPx(), center = last)
        // Solid inner dot
        drawCircle(color = color.copy(alpha = blinkAlpha), radius = 3.5.dp.toPx(), center = last)
    }
}

private fun generateWatchlistSparklinePoints(symbol: String, changePercent: Double): List<Float> {
    val points = 80
    val random = Random(symbol.hashCode())
    // Always ramp from ~0.15 (bottom-left) to ~0.85 (top-right) as the base trend.
    // changePercent modifies the final destination: positive = higher end, negative = lower end.
    val endTarget = (0.75f + (changePercent / 10.0).coerceIn(-0.30, 0.20).toFloat())
        .coerceIn(0.45f, 0.92f)
    val startValue = 0.15f
    // Larger wave amplitudes so the line looks alive, not flat
    val phaseA = random.nextFloat() * (2f * Math.PI.toFloat())
    val phaseB = random.nextFloat() * (2f * Math.PI.toFloat())
    val phaseC = random.nextFloat() * (2f * Math.PI.toFloat())
    val values = MutableList(points) { 0f }
    repeat(points) { index ->
        val progress = index / (points - 1f)
        // Strong linear ramp from startValue to endTarget
        val base = startValue + progress * (endTarget - startValue)
        // Visible oscillations layered on top
        val macroWave = Math.sin((progress * 8.0f + phaseA).toDouble()).toFloat() * 0.12f
        val mediumWave = Math.sin((progress * 20.0f + phaseB).toDouble()).toFloat() * 0.07f
        val microWave = Math.sin((progress * 38.0f + phaseC).toDouble()).toFloat() * 0.035f
        val noise = (random.nextFloat() - 0.5f) * 0.04f
        values[index] = (base + macroWave + mediumWave + microWave + noise).coerceIn(0.10f, 0.92f)
    }
    // Smooth pass
    return values.mapIndexed { i, v ->
        val prev = values.getOrElse(i - 1) { v }
        val next = values.getOrElse(i + 1) { v }
        ((prev * 0.2f) + (v * 0.6f) + (next * 0.2f)).coerceIn(0.10f, 0.92f)
    }
}

/**
 * Aggregate tick data into 1-hour candles
 * Takes the last 24 hours of data and creates hourly close prices
 */
private fun aggregateToHourlyCandles(history: List<Double>): List<Double> {
    if (history.isEmpty()) return emptyList()
    
    // If we have less than 24 points, return as-is (already sparse)
    if (history.size <= 24) return history
    
    // Calculate how many ticks per hour based on total history
    // Assume history represents last 24 hours of tick data
    val ticksPerHour = history.size / 24
    
    // Group into hourly buckets and take the last (close) price of each hour
    val hourlyCandles = mutableListOf<Double>()
    for (i in 0 until 24) {
        val startIdx = i * ticksPerHour
        val endIdx = minOf((i + 1) * ticksPerHour, history.size)
        if (startIdx < history.size) {
            // Take the last price in this hour (close price)
            hourlyCandles.add(history[endIdx - 1])
        }
    }
    
    return hourlyCandles
}

private fun watchlistHistorySnapshot(symbol: String, timeframe: String = "H1"): List<Double> {
    val merged = MarketDataStore.timedPriceHistory.value +
        BinanceDataStore.timedPriceHistory.value +
        CombinedFallbackDataStore.timedPriceHistory.value
    return bucketTimedHistory(symbol, timeframe, merged)
}

private fun bucketTimedHistory(
    symbol: String,
    timeframe: String,
    timedHistoryMap: Map<String, List<TimedPrice>>
): List<Double> {
    val timedHistory: List<TimedPrice> = timedHistoryMap.entries
        .filter { (key, _) -> MarketDataStore.matchesSymbol(key, symbol) }
        .flatMap { it.value }
        .sortedBy { it.timestampMillis }

    if (timedHistory.size < 2) return emptyList()

    val bucketMillis = when (timeframe) {
        "M15" -> 15 * 60 * 1000L
        "M30" -> 30 * 60 * 1000L
        "H1"  -> 60 * 60 * 1000L
        "H4"  -> 4 * 60 * 60 * 1000L
        "D1"  -> 24 * 60 * 60 * 1000L
        else  -> 60 * 60 * 1000L
    }

    val maxCandles = 40
    val now = timedHistory.last().timestampMillis
    val windowStart = now - bucketMillis * maxCandles
    val filtered = timedHistory.filter { it.timestampMillis >= windowStart }
    if (filtered.isEmpty()) return emptyList()

    val firstTs = filtered.first().timestampMillis
    val buckets = mutableMapOf<Long, MutableList<Double>>()
    filtered.forEach { tp ->
        val bucketKey = (tp.timestampMillis - firstTs) / bucketMillis
        buckets.getOrPut(bucketKey) { mutableListOf() }.add(tp.price)
    }

    return buckets.keys.sorted().map { key -> buckets[key]!!.last() }
}

@Composable
private fun DetailItem(label: String, value: String, valueColor: Color = Color.White) {
    Column {
        Text(
            text = label,
            color = SlateText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily
        )
    }
}

private fun parseTimeToEventMinutes(time: String): Int {
    val num = time.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
    return num
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        else -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
    }
}
