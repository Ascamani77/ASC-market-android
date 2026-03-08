package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.HistoricalTrade
import com.asc.markets.ui.screens.tradeDashboard.model.TradeType

@Composable
fun TradeHistoryPanel(history: List<HistoricalTrade> = emptyList(), modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TRADE HISTORY",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "${history.size} Closed Trades",
                color = Color.DarkGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

        // Scrollable Table
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            Column {
                // Table Headers
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistoryHeaderItem("SYMBOL / TYPE", 140.dp)
                    HistoryHeaderItem("VOLUME", 100.dp)
                    HistoryHeaderItem("EXECUTION", 220.dp)
                    HistoryHeaderItem("SWAP/COMM", 120.dp)
                    HistoryHeaderItem("PROFIT", 100.dp, Alignment.End)
                }

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(680.dp)
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "NO CLOSED TRADES", color = Color.DarkGray, fontSize = 12.sp)
                    }
                } else {
                    history.forEach { trade ->
                        TradeHistoryRow(trade)
                        HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeaderItem(text: String, width: Dp, alignment: Alignment.Horizontal = Alignment.Start) {
    Box(modifier = Modifier.width(width), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            color = Color(0xFF404040),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(if (alignment == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart)
        )
    }
}

@Composable
private fun TradeHistoryRow(trade: HistoricalTrade) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SYMBOL / TYPE
        Row(modifier = Modifier.width(140.dp), verticalAlignment = Alignment.CenterVertically) {
            // Small placeholder for flag/icon
            Box(modifier = Modifier.size(16.dp).background(Color.DarkGray, androidx.compose.foundation.shape.CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = trade.symbol,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = trade.type.name,
                    color = if (trade.type == TradeType.BUY) Color(0xFF00C853) else Color(0xFFFF5252),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // VOLUME
        Text(
            text = String.format("%.2f", trade.volume),
            modifier = Modifier.width(100.dp),
            color = Color.Gray,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )

        // EXECUTION
        Column(modifier = Modifier.width(220.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trade.closeTime,
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${trade.openPrice} → ${trade.closePrice}",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // SWAP / COMM
        Column(modifier = Modifier.width(120.dp)) {
            Row {
                Text(text = "S: ", color = Color.DarkGray, fontSize = 10.sp)
                Text(
                    text = String.format("%.2f", trade.swap),
                    color = if (trade.swap < 0) Color(0xFFFF5252) else if (trade.swap > 0) Color(0xFF00C853) else Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row {
                Text(text = "C: ", color = Color.DarkGray, fontSize = 10.sp)
                Text(
                    text = String.format("%.2f", trade.commission),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // PROFIT
        val profitStr = String.format("%s%.2f", if (trade.profit >= 0) "+" else "", trade.profit)
        Text(
            text = profitStr,
            modifier = Modifier.width(100.dp),
            color = if (trade.profit >= 0) Color(0xFF00C853) else Color(0xFFFF5252),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}
