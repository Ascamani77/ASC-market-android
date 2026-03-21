package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.Position
import com.asc.markets.ui.screens.tradeDashboard.model.TradeType
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.DeepBlack

@Composable
fun PositionsTable(
    positions: List<Position>,
    selectedSymbol: String? = null,
    onAdjustSL: (ticketId: String, newSL: Double) -> Unit = { _, _ -> },
    onAdjustTP: (ticketId: String, newTP: Double) -> Unit = { _, _ -> },
    onTradeClick: (Position) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepBlack)
            .padding(vertical = 8.dp)
    ) {
        // Main Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OPEN POSITIONS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${positions.size} Active Orders",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        HorizontalDivider(color = Color(0xFF444444), thickness = 1.dp)

        // Scrollable Table Content
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            Column {
                // Table Headers
                Row(
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderItem("TICKET / MAGIC", 120.dp)
                    HeaderItem("SYMBOL", 100.dp)
                    HeaderItem("TYPE", 80.dp)
                    HeaderItem("VOLUME", 70.dp)
                    HeaderItem("AI HEALTH", 120.dp)
                    HeaderItem("S/L", 150.dp)
                    HeaderItem("T/P", 150.dp)
                    HeaderItem("SWAP / COMM", 100.dp)
                    HeaderItem("PROFIT", 100.dp, Alignment.End)
                }

                HorizontalDivider(color = Color(0xFF444444), thickness = 1.dp)

                if (positions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(990.dp) // Sum of column widths + padding
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "NO ACTIVE TRADES", color = Color.DarkGray, fontSize = 12.sp)
                    }
                } else {
                    positions.forEach { pos ->
                        val isSelected = pos.symbol == selectedSymbol
                        PositionRow(pos, isSelected, onAdjustSL, onAdjustTP, onTradeClick)
                        HorizontalDivider(color = Color(0xFF444444), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderItem(text: String, width: Dp, alignment: Alignment.Horizontal = Alignment.Start) {
    Box(modifier = Modifier.width(width), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(if (alignment == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart)
        )
    }
}

@Composable
private fun PositionRow(
    position: Position,
    isSelected: Boolean,
    onAdjustSL: (ticketId: String, newSL: Double) -> Unit,
    onAdjustTP: (ticketId: String, newTP: Double) -> Unit,
    onTradeClick: (Position) -> Unit
) {
    Row(
        modifier = Modifier
            .background(if (isSelected) Color(0xFF00C853).copy(alpha = 0.1f) else DeepBlack)
            .clickable { onTradeClick(position) }
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TICKET / MAGIC
        Column(modifier = Modifier.width(120.dp)) {
            Text(text = "#${position.ticketId}", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            if (position.magicNumber != null) {
                Text(text = "M:${position.magicNumber}", color = Color.DarkGray, fontSize = 10.sp)
            }
        }

        // SYMBOL
        Text(
            text = position.symbol,
            modifier = Modifier.width(100.dp),
            color = if (isSelected) Color(0xFF00C853) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // TYPE
        val isBuy = position.type == TradeType.BUY
        Box(
            modifier = Modifier
                .width(80.dp)
                .padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (isBuy) Color(0xFF003311) else Color(0xFF331111),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        0.5.dp,
                        if (isBuy) Color(0xFF00C853).copy(alpha = 0.3f) else Color(0xFFFF5252).copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isBuy) "↗" else "↘",
                        color = if (isBuy) Color(0xFF00C853) else Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBuy) "BUY" else "SELL",
                        color = if (isBuy) Color(0xFF00C853) else Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // VOLUME
        Text(
            text = String.format("%.2f", position.volume),
            modifier = Modifier.width(70.dp),
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )

        // AI HEALTH
        Column(modifier = Modifier.width(120.dp).padding(end = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (position.healthScore > 50) "STABLE" else "CRITICAL",
                    color = if (position.healthScore > 50) Color(0xFF00C853) else Color(0xFFFF5252),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${position.healthScore}%",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            HealthBar(score = position.healthScore)
        }

        // S/L
        PriceAdjuster(
            value = position.sl,
            onValueSaved = { onAdjustSL(position.ticketId, it) },
            width = 150.dp,
            color = Color(0xFFFF5252)
        )

        // T/P
        PriceAdjuster(
            value = position.tp,
            onValueSaved = { onAdjustTP(position.ticketId, it) },
            width = 150.dp,
            color = Color(0xFF00C853)
        )

        // SWAP / COMM
        Column(modifier = Modifier.width(100.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "S: ", color = Color.Gray, fontSize = 9.sp)
                Text(
                    text = String.format("%.2f", position.swap),
                    color = if (position.swap < 0) Color(0xFFFF5252) else Color(0xFF00C853),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "C: ", color = Color.Gray, fontSize = 9.sp)
                Text(
                    text = String.format("%.2f", position.commission),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // PROFIT
        val profitStr = String.format("%s%.2f", if (position.profit >= 0) "+" else "", position.profit)
        Text(
            text = profitStr,
            modifier = Modifier.width(100.dp),
            color = if (position.profit >= 0) Color(0xFF00C853) else Color(0xFFFF5252),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PriceAdjuster(
    value: Double?,
    onValueSaved: (Double) -> Unit,
    width: Dp,
    color: Color
) {
    var textValue by remember(value) { mutableStateOf(value?.toString() ?: "") }
    
    Row(
        modifier = Modifier.width(width).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Minus Button
        Text(
            text = "−",
            color = Color.Gray,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable {
                    val current = textValue.toDoubleOrNull() ?: 0.0
                    val next = current - 0.0001
                    textValue = String.format("%.5f", next)
                    onValueSaved(next)
                }
                .padding(horizontal = 8.dp)
        )
        
        // Editable Text Box
        Box(
            modifier = Modifier
                .background(Color(0xFF111111), RoundedCornerShape(4.dp))
                .border(0.5.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { 
                    textValue = it
                    it.toDoubleOrNull()?.let { validated ->
                        onValueSaved(validated)
                    }
                },
                textStyle = TextStyle(
                    color = color,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(color),
                modifier = Modifier.width(IntrinsicSize.Min).minWidth(60.dp)
            )
            if (textValue.isEmpty()) {
                Text(text = "−.−−−−", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
        
        // Plus Button
        Text(
            text = "+",
            color = Color.Gray,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable {
                    val current = textValue.toDoubleOrNull() ?: 0.0
                    val next = current + 0.0001
                    textValue = String.format("%.5f", next)
                    onValueSaved(next)
                }
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun HealthBar(score: Int) {
    val barColor = when {
        score > 80 -> Color(0xFF00C853)
        score > 40 -> Color(0xFFFFD600)
        else -> Color(0xFFFF5252)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(score / 100f)
                .background(barColor, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun Modifier.minWidth(minWidth: Dp) = this.then(
    Modifier.widthIn(min = minWidth)
)
