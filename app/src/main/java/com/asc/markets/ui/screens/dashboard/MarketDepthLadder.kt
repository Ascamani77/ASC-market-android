package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.OrderBookLevel
import com.asc.markets.data.OrderBookSnapshot
import com.asc.markets.data.OrderBookStore
import com.asc.markets.data.OrderTradeSide
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.HairlineBorder
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private data class DepthItem(
    val price: Double,
    val volume: Double,
    val type: String
)

enum class DepthExplanationMode {
    STRUCTURE,
    EXECUTION
}

@Composable
fun OrderBookMirror(
    selectedPair: ForexPair,
    modifier: Modifier = Modifier,
    showExplanation: Boolean = false,
    explanationMode: DepthExplanationMode = DepthExplanationMode.EXECUTION
) {
    val livePair = rememberLivePair(selectedPair)
    val snapshot = rememberOrderBookSnapshot(livePair)
    val rowCount = max(snapshot.bidLevels.size, snapshot.askLevels.size)
    val maxCumulative = snapshot.maxCumulative()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureBlack)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Order Book", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("Trades", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Info", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .width(88.dp)
                .height(2.dp)
                .background(Color(0xFFD7B645), RoundedCornerShape(999.dp))
        )

        DepthChartCumulative(
            pair = livePair,
            bids = snapshot.bidLevels,
            asks = snapshot.askLevels,
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .padding(top = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quantity", color = SlateText, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("Buy Price", color = SlateText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("Sell Price", color = SlateText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("Quantity", color = SlateText, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }

        repeat(rowCount) { index ->
            val bid = snapshot.bidLevels.getOrNull(index)
            val ask = snapshot.askLevels.getOrNull(index)
            val bidRatio = ((bid?.cumulativeQuantity ?: 0.0) / maxCumulative).toFloat().coerceIn(0f, 1f)
            val askRatio = ((ask?.cumulativeQuantity ?: 0.0) / maxCumulative).toFloat().coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bid?.let { formatQuantity(it.quantity) } ?: "-",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((bidRatio * 0.92f).coerceAtLeast(0.04f))
                            .background(EmeraldSuccess.copy(alpha = 0.24f))
                    )
                    Text(
                        text = bid?.let { formatPriceForPair(it.price, livePair) } ?: "-",
                        color = EmeraldSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((askRatio * 0.92f).coerceAtLeast(0.04f))
                            .background(RoseError.copy(alpha = 0.24f))
                    )
                    Text(
                        text = ask?.let { formatPriceForPair(it.price, livePair) } ?: "-",
                        color = RoseError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    text = ask?.let { formatQuantity(it.quantity) } ?: "-",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text("Buy", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text("Sell", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (showExplanation) {
            Spacer(modifier = Modifier.height(12.dp))
            DepthExplanation(snapshot = snapshot, pair = livePair, mode = explanationMode)
        }
    }
}

@Composable
fun OrderBookSplit(
    selectedPair: ForexPair,
    modifier: Modifier = Modifier,
    showExplanation: Boolean = false,
    explanationMode: DepthExplanationMode = DepthExplanationMode.STRUCTURE
) {
    val livePair = rememberLivePair(selectedPair)
    val snapshot = rememberOrderBookSnapshot(livePair)
    val maxCumulative = snapshot.maxCumulative()
    val rowCount = max(snapshot.bidLevels.size, snapshot.askLevels.size)
    val tickSizeLabel = formatStepLabel(inferTickSize(snapshot, livePair))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureBlack)
            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        DepthChartSplit(
            pair = livePair,
            bids = snapshot.bidLevels,
            asks = snapshot.askLevels,
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(EmeraldSuccess, RoundedCornerShape(2.dp)))
            Text(" Buy", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(24.dp))
            Box(modifier = Modifier.size(10.dp).background(RoseError, RoundedCornerShape(2.dp)))
            Text(" Sell", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Order Book", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(78.dp)
                        .height(2.dp)
                        .background(Color(0xFF5EA0FF), RoundedCornerShape(999.dp))
                )
            }
            Spacer(modifier = Modifier.width(22.dp))
            Text("Recent Trades", color = SlateText, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.weight(1f))
            Surface(color = Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    tickSizeLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("", color = SlateText, fontSize = 11.sp, modifier = Modifier.width(22.dp))
            Text("Size", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Price", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1.45f))
            Text("Size", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text("", color = SlateText, fontSize = 11.sp, modifier = Modifier.width(22.dp))
        }

        repeat(rowCount) { index ->
            SplitDepthRow(
                rowNumber = index + 1,
                bid = snapshot.bidLevels.getOrNull(index),
                ask = snapshot.askLevels.getOrNull(index),
                pair = livePair,
                maxCumulative = maxCumulative,
                highlight = index == 0
            )
        }

        if (showExplanation) {
            Spacer(modifier = Modifier.height(12.dp))
            DepthExplanation(snapshot = snapshot, pair = livePair, mode = explanationMode)
        }
    }
}

@Composable
fun OrderBookLadder(
    selectedPair: ForexPair,
    modifier: Modifier = Modifier
) {
    val livePair = rememberLivePair(selectedPair)
    val snapshot = rememberOrderBookSnapshot(livePair)
    val levels = remember(snapshot) {
        buildList {
            snapshot.askLevels.asReversed().forEach { add(DepthItem(it.price, it.quantity, "ASK")) }
            add(DepthItem(snapshot.midPrice, 0.0, "MID"))
            snapshot.bidLevels.forEach { add(DepthItem(it.price, it.quantity, "BID")) }
        }
    }
    val maxQuantity = snapshot.maxCumulative()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PureBlack)
            .border(1.dp, HairlineBorder, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        DepthLadderProfile(
            pair = livePair,
            snapshot = snapshot,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight()
        ) {
            levels.forEach { item ->
                if (item.type == "MID") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${formatPriceForPair(item.price, livePair)} MID",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                    }
                } else {
                    val isAsk = item.type == "ASK"
                    val widthFraction = (item.volume / maxQuantity).toFloat().coerceIn(0.05f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(29.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(widthFraction)
                                .align(Alignment.CenterStart)
                                .background(if (isAsk) RoseError.copy(alpha = 0.36f) else EmeraldSuccess.copy(alpha = 0.36f))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatPriceForPair(item.price, livePair),
                                color = if (isAsk) RoseError else EmeraldSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = formatQuantity(item.volume),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitDepthRow(
    rowNumber: Int,
    bid: OrderBookLevel?,
    ask: OrderBookLevel?,
    pair: ForexPair,
    maxCumulative: Double,
    highlight: Boolean
) {
    val bidRatio = ((bid?.cumulativeQuantity ?: 0.0) / maxCumulative).toFloat().coerceIn(0f, 1f)
    val askRatio = ((ask?.cumulativeQuantity ?: 0.0) / maxCumulative).toFloat().coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(if (highlight) Color.White.copy(alpha = 0.03f) else Color.Transparent)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$rowNumber", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Text(
            text = bid?.let { formatQuantity(it.quantity) } ?: "-",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .weight(1.45f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth((bidRatio * 0.5f).coerceAtLeast(if (bid != null) 0.04f else 0f))
                    .background(EmeraldSuccess.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth((askRatio * 0.5f).coerceAtLeast(if (ask != null) 0.04f else 0f))
                    .background(RoseError.copy(alpha = 0.22f))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bid?.let { formatPriceForPair(it.price, pair) } ?: "-",
                    color = if (bid != null) EmeraldSuccess else SlateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ask?.let { formatPriceForPair(it.price, pair) } ?: "-",
                    color = if (ask != null) RoseError else SlateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = ask?.let { formatQuantity(it.quantity) } ?: "-",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text("$rowNumber", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(20.dp))
    }
}

@Composable
private fun DepthExplanation(
    snapshot: OrderBookSnapshot,
    pair: ForexPair,
    mode: DepthExplanationMode
) {
    val explanation = remember(snapshot, pair, mode) { buildDepthExplanation(snapshot, pair, mode) }

    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                if (mode == DepthExplanationMode.STRUCTURE) "What structure is showing" else "What execution is showing",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                explanation,
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DepthChartCumulative(
    pair: ForexPair,
    bids: List<OrderBookLevel>,
    asks: List<OrderBookLevel>,
    modifier: Modifier
) {
    val maxCumulative = max(
        bids.lastOrNull()?.cumulativeQuantity ?: 0.0,
        asks.lastOrNull()?.cumulativeQuantity ?: 0.0
    ).coerceAtLeast(1.0)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midX = width / 2f
            val bidSeries = bids.asReversed()
            val askSeries = asks

            val bidFill = Path().apply {
                moveTo(0f, height)
                bidSeries.forEachIndexed { index, level ->
                    val x = if (bidSeries.size == 1) 0f else (index.toFloat() / (bidSeries.size - 1)) * midX
                    lineTo(x, chartY(level.cumulativeQuantity, maxCumulative, height))
                }
                lineTo(midX, height)
                close()
            }
            val askFill = Path().apply {
                moveTo(midX, height)
                askSeries.forEachIndexed { index, level ->
                    val x = midX + if (askSeries.size == 1) 0f else (index.toFloat() / (askSeries.size - 1)) * midX
                    lineTo(x, chartY(level.cumulativeQuantity, maxCumulative, height))
                }
                lineTo(width, height)
                close()
            }
            val bidLine = Path().apply {
                bidSeries.forEachIndexed { index, level ->
                    val x = if (bidSeries.size == 1) 0f else (index.toFloat() / (bidSeries.size - 1)) * midX
                    val y = chartY(level.cumulativeQuantity, maxCumulative, height)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val askLine = Path().apply {
                askSeries.forEachIndexed { index, level ->
                    val x = midX + if (askSeries.size == 1) 0f else (index.toFloat() / (askSeries.size - 1)) * midX
                    val y = chartY(level.cumulativeQuantity, maxCumulative, height)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(bidFill, EmeraldSuccess.copy(alpha = 0.30f), style = Fill)
            drawPath(askFill, RoseError.copy(alpha = 0.30f), style = Fill)
            drawPath(bidLine, EmeraldSuccess.copy(alpha = 0.85f), style = Stroke(width = 2.dp.toPx()))
            drawPath(askLine, RoseError.copy(alpha = 0.85f), style = Stroke(width = 2.dp.toPx()))
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                cumulativeAxisLabels(maxCumulative).forEach { label ->
                    Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Text(
                    formatPriceForPair(snapshotMidPrice(bids, asks), pair),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DepthChartSplit(
    pair: ForexPair,
    bids: List<OrderBookLevel>,
    asks: List<OrderBookLevel>,
    modifier: Modifier
) {
    val maxCumulative = max(
        bids.lastOrNull()?.cumulativeQuantity ?: 0.0,
        asks.lastOrNull()?.cumulativeQuantity ?: 0.0
    ).coerceAtLeast(1.0)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midX = width / 2f
            val bidSeries = bids.asReversed()
            val askSeries = asks

            val bidFill = Path().apply {
                moveTo(0f, height)
                bidSeries.forEachIndexed { index, level ->
                    val x = if (bidSeries.size == 1) 0f else (index.toFloat() / (bidSeries.size - 1)) * midX
                    lineTo(x, chartY(level.cumulativeQuantity, maxCumulative, height))
                }
                lineTo(midX, height)
                close()
            }
            val askFill = Path().apply {
                moveTo(midX, height)
                askSeries.forEachIndexed { index, level ->
                    val x = midX + if (askSeries.size == 1) 0f else (index.toFloat() / (askSeries.size - 1)) * midX
                    lineTo(x, chartY(level.cumulativeQuantity, maxCumulative, height))
                }
                lineTo(width, height)
                close()
            }
            val bidLine = Path().apply {
                bidSeries.forEachIndexed { index, level ->
                    val x = if (bidSeries.size == 1) 0f else (index.toFloat() / (bidSeries.size - 1)) * midX
                    val y = chartY(level.cumulativeQuantity, maxCumulative, height)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val askLine = Path().apply {
                askSeries.forEachIndexed { index, level ->
                    val x = midX + if (askSeries.size == 1) 0f else (index.toFloat() / (askSeries.size - 1)) * midX
                    val y = chartY(level.cumulativeQuantity, maxCumulative, height)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(bidFill, EmeraldSuccess.copy(alpha = 0.20f), style = Fill)
            drawPath(askFill, RoseError.copy(alpha = 0.20f), style = Fill)
            drawPath(bidLine, EmeraldSuccess.copy(alpha = 0.88f), style = Stroke(width = 2.dp.toPx()))
            drawPath(askLine, RoseError.copy(alpha = 0.88f), style = Stroke(width = 2.dp.toPx()))
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            cumulativeAxisLabels(maxCumulative).forEach { label ->
                Text(label, color = Color.White.copy(alpha = 0.74f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatPriceForPair(bids.lastOrNull()?.price ?: pair.price, pair), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(formatPriceForPair(snapshotMidPrice(bids, asks), pair), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(formatPriceForPair(asks.lastOrNull()?.price ?: pair.price, pair), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DepthLadderProfile(
    pair: ForexPair,
    snapshot: OrderBookSnapshot,
    modifier: Modifier
) {
    val maxCumulative = snapshot.maxCumulative()

    Box(modifier = modifier.background(Color(0xFF070A14))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val axisX = width * 0.62f
            val askLevels = snapshot.askLevels.asReversed()
            val bidLevels = snapshot.bidLevels

            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(axisX, 0f),
                end = Offset(axisX, height),
                strokeWidth = 1.dp.toPx()
            )

            val askPath = Path().apply {
                moveTo(width, 0f)
                askLevels.forEachIndexed { index, level ->
                    val y = if (askLevels.isEmpty()) 0f else (index.toFloat() / askLevels.size) * midY
                    val x = axisX + (((level.cumulativeQuantity / maxCumulative).toFloat().coerceIn(0.08f, 1f)) * (width - axisX))
                    lineTo(x, y)
                }
                lineTo(axisX, midY)
                lineTo(width, midY)
                close()
            }
            val bidPath = Path().apply {
                moveTo(axisX, midY)
                bidLevels.forEachIndexed { index, level ->
                    val y = midY + if (bidLevels.isEmpty()) 0f else ((index.toFloat() / bidLevels.size) * midY)
                    val x = axisX + (((level.cumulativeQuantity / maxCumulative).toFloat().coerceIn(0.08f, 1f)) * (width - axisX))
                    lineTo(x, y)
                }
                lineTo(width, height)
                lineTo(width, midY)
                close()
            }

            drawPath(askPath, RoseError.copy(alpha = 0.28f), style = Fill)
            drawPath(bidPath, EmeraldSuccess.copy(alpha = 0.28f), style = Fill)
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            ladderAxisPrices(snapshot, pair).forEach { priceLabel ->
                Text(priceLabel, color = Color.White.copy(alpha = 0.76f), fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun rememberOrderBookSnapshot(livePair: ForexPair): OrderBookSnapshot {
    val seedSnapshot = remember(livePair.symbol, livePair.price, livePair.change, livePair.category) {
        OrderBookStore.seedSnapshot(livePair)
    }
    val snapshot by OrderBookStore.snapshotFlow(livePair.symbol).collectAsState(initial = seedSnapshot)

    LaunchedEffect(livePair.symbol, livePair.category) {
        OrderBookStore.subscribe(livePair)
    }
    DisposableEffect(livePair.symbol, livePair.category) {
        onDispose { OrderBookStore.unsubscribe(livePair.symbol) }
    }

    return snapshot ?: seedSnapshot
}

@Composable
private fun rememberLivePair(selectedPair: ForexPair): ForexPair {
    val observedPair by MarketDataStore.pairFlow(selectedPair.symbol)
        .collectAsState(initial = MarketDataStore.pairSnapshot(selectedPair.symbol) ?: selectedPair)
    return observedPair ?: selectedPair
}

private fun chartY(cumulativeQuantity: Double, maxCumulative: Double, height: Float): Float {
    val fraction = (cumulativeQuantity / maxCumulative).toFloat().coerceIn(0f, 1f)
    return height - (fraction * height * 0.92f)
}

private fun snapshotMidPrice(bids: List<OrderBookLevel>, asks: List<OrderBookLevel>): Double {
    val bid = bids.firstOrNull()?.price ?: 0.0
    val ask = asks.firstOrNull()?.price ?: bid
    return if (bid == 0.0 && ask == 0.0) 0.0 else (bid + ask) / 2.0
}

private fun OrderBookSnapshot.maxCumulative(): Double {
    return max(
        bidLevels.lastOrNull()?.cumulativeQuantity ?: 0.0,
        askLevels.lastOrNull()?.cumulativeQuantity ?: 0.0
    ).coerceAtLeast(1.0)
}

private fun cumulativeAxisLabels(maxCumulative: Double): List<String> {
    return listOf(1.0, 0.8, 0.6, 0.4, 0.2).map { multiplier ->
        formatQuantity(maxCumulative * multiplier)
    }
}

private fun ladderAxisPrices(snapshot: OrderBookSnapshot, pair: ForexPair): List<String> {
    val levels = listOfNotNull(
        snapshot.askLevels.lastOrNull()?.price,
        snapshot.askLevels.getOrNull(snapshot.askLevels.size / 2)?.price,
        snapshot.midPrice,
        snapshot.bidLevels.getOrNull(snapshot.bidLevels.size / 2)?.price,
        snapshot.bidLevels.lastOrNull()?.price
    )

    return levels.distinct().ifEmpty { listOf(pair.price) }.map { price -> formatPriceForPair(price, pair) }
}

private fun inferTickSize(snapshot: OrderBookSnapshot, pair: ForexPair): Double {
    val bidDiffs = snapshot.bidLevels.zipWithNext { left, right -> abs(left.price - right.price) }
    val askDiffs = snapshot.askLevels.zipWithNext { left, right -> abs(left.price - right.price) }
    val increments = (bidDiffs + askDiffs).filter { it > 0.0 }
    val fallback = when {
        pair.symbol.contains("JPY", ignoreCase = true) -> 0.01
        pair.symbol.contains("USDT", ignoreCase = true) && pair.price >= 1_000.0 -> 0.1
        pair.symbol.contains("/", ignoreCase = true) && pair.price < 10.0 -> 0.0001
        pair.price >= 100.0 -> 0.05
        pair.price >= 1.0 -> 0.01
        else -> 0.0001
    }
    return increments.minOrNull()?.coerceAtLeast(fallback) ?: fallback
}

private fun buildDepthExplanation(
    snapshot: OrderBookSnapshot,
    pair: ForexPair,
    mode: DepthExplanationMode
): String {
    val spreadText = if (snapshot.midPrice > 0.0) {
        val bps = (snapshot.spread / snapshot.midPrice) * 10_000.0
        "Spread is ${formatPriceForPair(snapshot.spread, pair)} (${String.format(Locale.US, "%.2f", bps)} bps)"
    } else {
        "Spread is not available"
    }
    val imbalancePct = abs(snapshot.imbalance) * 100.0
    val imbalanceText = when {
        imbalancePct < 4.0 -> "displayed liquidity is balanced between bids and asks"
        snapshot.imbalance > 0.0 -> "bids control ${String.format(Locale.US, "%.1f", imbalancePct)}% of the displayed imbalance"
        else -> "asks control ${String.format(Locale.US, "%.1f", imbalancePct)}% of the displayed imbalance"
    }
    val buyTradeQuantity = snapshot.recentTrades.filter { it.side == OrderTradeSide.BUY }.sumOf { it.quantity }
    val sellTradeQuantity = snapshot.recentTrades.filter { it.side == OrderTradeSide.SELL }.sumOf { it.quantity }
    val tapeText = when {
        snapshot.recentTrades.isEmpty() -> "recent tape is quiet"
        abs(buyTradeQuantity - sellTradeQuantity) < (buyTradeQuantity + sellTradeQuantity) * 0.08 -> "recent prints are balanced"
        buyTradeQuantity > sellTradeQuantity -> "recent prints lean buy, which means market orders are lifting offers"
        else -> "recent prints lean sell, which means market orders are hitting bids"
    }
    val sourceText = when {
        snapshot.isFallback -> "Live venue depth is unavailable for this asset, so the ladder is being derived deterministically from the app's live quote stream."
        snapshot.isStale -> "The last confirmed venue snapshot is being held while the next live book refresh is pending."
        snapshot.venueSymbol != null -> "Depth and recent trades are coming from ${snapshot.source} for ${snapshot.venueSymbol}."
        else -> "Depth and recent trades are coming from ${snapshot.source}."
    }

    return when (mode) {
        DepthExplanationMode.STRUCTURE -> {
            val structureLead = when {
                abs(snapshot.imbalance) < 0.04 -> "This tactical book is for locating balance, not forcing a directional read."
                snapshot.imbalance > 0.0 -> "This tactical book is showing thicker passive demand below price."
                else -> "This tactical book is showing thicker passive supply above price."
            }
            val structureCue = when {
                snapshot.imbalance > 0.0 -> "That usually marks areas where pullbacks may find support if buyers keep defending the bid ladder."
                snapshot.imbalance < 0.0 -> "That usually marks areas where rallies may stall if sellers keep refreshing the offer ladder."
                else -> "Balanced ladders usually mean the market is waiting for new flow before choosing direction."
            }
            "$structureLead $spreadText and $imbalanceText. $structureCue $sourceText"
        }

        DepthExplanationMode.EXECUTION -> {
            val executionLead = when {
                snapshot.recentTrades.isEmpty() -> "This execution book is for reading fill conditions at the top of book."
                buyTradeQuantity > sellTradeQuantity -> "This execution book shows buyers being the more aggressive side right now."
                else -> "This execution book shows sellers being the more aggressive side right now."
            }
            val executionCue = when {
                snapshot.spread == 0.0 -> "A flat displayed spread means price discovery is happening inside a very tight top-of-book."
                snapshot.spread / snapshot.midPrice > 0.0015 -> "The wider spread means execution cost is elevated, so market orders will slip more easily."
                else -> "The relatively tight spread means execution cost is controlled as long as size stays near the best levels."
            }
            "$executionLead $tapeText. $spreadText. $executionCue $sourceText"
        }
    }
}

private fun formatPriceForPair(price: Double, pair: ForexPair): String {
    return when {
        pair.symbol.contains("USDT", ignoreCase = true) && price >= 1_000.0 -> String.format(Locale.US, "%,.2f", price)
        pair.symbol.contains("USDT", ignoreCase = true) && price >= 1.0 -> String.format(Locale.US, "%.3f", price)
        pair.symbol.contains("/", ignoreCase = true) && price < 10.0 -> String.format(Locale.US, "%.5f", price)
        pair.symbol.contains("JPY", ignoreCase = true) -> String.format(Locale.US, "%.3f", price)
        pair.price >= 1_000.0 -> String.format(Locale.US, "%,.2f", price)
        pair.price >= 100.0 -> String.format(Locale.US, "%.2f", price)
        pair.price >= 1.0 -> String.format(Locale.US, "%.4f", price)
        else -> String.format(Locale.US, "%.6f", price)
    }
}

private fun formatQuantity(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1_000_000_000.0)
        value >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.2fK", value / 1_000.0)
        value >= 1.0 -> String.format(Locale.US, "%.4f", value)
        else -> String.format(Locale.US, "%.6f", value)
    }
}

private fun formatStepLabel(step: Double): String {
    val formatted = when {
        step >= 1.0 -> String.format(Locale.US, "%.2f", step)
        step >= 0.01 -> String.format(Locale.US, "%.4f", step)
        else -> String.format(Locale.US, "%.6f", step)
    }
    return "$formatted \u25BE"
}
