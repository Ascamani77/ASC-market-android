package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.state.mapCategoryToAssetContext
import com.asc.markets.ui.screens.dashboard.getExploreItemsForContext
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketsScreen(onSelectPair: (ForexPair) -> Unit, viewModel: ForexViewModel = viewModel()) {
    var activeCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("ALL", "FOREX", "CRYPTO", "INDICES", "COMMODITIES", "STOCKS")
    val listState = rememberLazyListState()

    // Scroll detection to hide/show global header with smooth height animation
    val collapseRange = 150f  // pixels to scroll before header fully collapses
    val collapseProgress by remember {
        derivedStateOf {
            // Calculate absolute scroll position (works across item boundaries)
            val absoluteScroll = (listState.firstVisibleItemIndex * 100f) + listState.firstVisibleItemScrollOffset
            (absoluteScroll / collapseRange).coerceIn(0f, 1f)
        }
    }
    
    LaunchedEffect(collapseProgress) {
        viewModel.setGlobalHeaderCollapse(collapseProgress)
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        LaunchedEffect(activeCategory) {
            // when the active category changes, scroll the list to top
            listState.animateScrollToItem(0)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Sticky Categories header
            stickyHeader {
                Surface(
                    color = DeepBlack,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(categories) { cat ->
                            val active = activeCategory == cat
                            
                            Surface(
                                color = if (active) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                                border = if (!active) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                                modifier = Modifier.height(32.dp).clickable { activeCategory = cat }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (active) Color.Black else Color(0xFF94a3b8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InterFontFamily,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search box as first content item
            item {
                Box(modifier = Modifier.padding(16.dp)) {
                    var isFocused by remember { mutableStateOf(false) }
                    
                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(androidx.compose.material.icons.autoMirrored.outlined.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.White, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusEvent { focusState ->
                                        isFocused = focusState.isFocused
                                    }
                            ) { inner ->
                                if (searchQuery.isEmpty() && !isFocused) {
                                    Text("FILTER SYMBOLS...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                inner()
                            }
                            // show clear icon when focused or when there's input
                            if (isFocused || searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Render market cards
            val base = getExploreItemsForContext(mapCategoryToAssetContext(activeCategory))
            val filtered = base.filter {
                val matchesSearch = it.symbol.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)
                matchesSearch
            }

            items(filtered) { pair ->
                MarketCard(pair, onSelectPair)
            }
        }
    }
}

@Composable
fun MarketCard(pair: ForexPair, onClick: (ForexPair) -> Unit) {
    val isUp = pair.change >= 0
    InfoBox(onClick = { onClick(pair) }, minHeight = 150.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top: Asset Info & Bias Parity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PairFlags(pair.symbol, 24)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(pair.symbol, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = InterFontFamily)
                        Text(pair.name.uppercase(), color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
                Surface(
                    color = (if (isUp) EmeraldSuccess else RoseError).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (if (isUp) EmeraldSuccess else RoseError).copy(alpha = 0.1f))
                ) {
                    Text(
                        text = if (isUp) "BULLISH" else "BEARISH",
                        color = if (isUp) EmeraldSuccess else RoseError,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 1.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mid: Price & Ticker Parity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = String.format(java.util.Locale.US, "%.5f", pair.price),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "${if (isUp) "+" else ""}${String.format(java.util.Locale.US, "%.2f", pair.changePercent)}%",
                        color = if (isUp) EmeraldSuccess else RoseError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SPREAD", color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("0.4", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
            Spacer(modifier = Modifier.height(12.dp))

            // Bottom: Grid Parity
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)).padding(6.dp)) {
                    Column {
                        Text("DAY HIGH", color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        Text(String.format(java.util.Locale.US, "%.5f", pair.price * 1.002), color = Color.Gray, fontSize = 9.sp, fontFamily = InterFontFamily)
                    }
                }
                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)).padding(6.dp)) {
                    Column {
                        Text("DAY LOW", color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        Text(String.format(java.util.Locale.US, "%.5f", pair.price * 0.998), color = Color.Gray, fontSize = 9.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}