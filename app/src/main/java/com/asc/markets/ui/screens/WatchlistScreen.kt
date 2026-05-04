package com.asc.markets.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
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
    val allPairs by MarketDataStore.allPairs.collectAsState()

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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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

            MiniSparkline(
                history = MarketDataStore.historySnapshot(item.assetName),
                modifier = Modifier.width(60.dp).height(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (price > 100) String.format("%.2f", price) else String.format("%.4f", price),
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
                                text = if (price > 100) String.format("%.2f", price) else String.format("%.4f", price),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Sparkline + probability bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MiniSparkline(
                    history = MarketDataStore.historySnapshot(item.assetName),
                    modifier = Modifier.weight(1f).height(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(48.dp)) {
                    Text(
                        text = "${item.volatilityScore}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "VOL",
                        color = SlateText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val barColor = when {
                item.moveProbability >= 80 -> RoseError
                item.moveProbability >= 70 -> Color(0xFFF59E0B)
                item.moveProbability >= 60 -> Color.Yellow
                else -> EmeraldSuccess
            }
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "VIEW CHART", onClick = { onViewChart(item.assetName) })
                ActionButton(text = "SET ALERT", onClick = { onSetAlert(item.assetName) })
                ActionButton(text = "DEEP DIVE", onClick = { onDeepDive(item.assetName) })
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = GhostWhite,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
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
private fun MiniSparkline(history: List<Double>, modifier: Modifier = Modifier, color: Color = IndigoAccent) {
    if (history.size >= 2) {
        Canvas(modifier = modifier) {
            val min = history.minOrNull() ?: return@Canvas
            val max = history.maxOrNull() ?: return@Canvas
            val range = (max - min).takeIf { it > 0 } ?: 1.0
            val path = Path()
            history.forEachIndexed { index, value ->
                val x = size.width * (index / (history.size - 1).toFloat())
                val y = size.height - (size.height * ((value - min) / range).toFloat().coerceIn(0f, 1f))
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
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
