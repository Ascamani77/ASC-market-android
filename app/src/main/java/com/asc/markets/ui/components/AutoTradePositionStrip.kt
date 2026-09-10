package com.asc.markets.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.AutoTradeEngine
import com.asc.markets.logic.EnginePosition
import com.asc.markets.logic.PriceStreamManager
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText

/**
 * Renders a live auto-trade position as a price-scaled chart strip showing the
 * ENTRY / SL / TP levels and the current price, driven by [AutoTradeEngine].
 * Hidden entirely when there is no open auto position for [symbol].
 */
@Composable
fun AutoTradeStripForSymbol(symbol: String, modifier: Modifier = Modifier) {
    val positions by AutoTradeEngine.positions.collectAsState()
    val position = rememberPositionFor(positions, symbol)
    if (position != null) {
        AutoTradePositionStrip(position = position, modifier = modifier)
    }
}

@Composable
private fun rememberPositionFor(positions: List<EnginePosition>, symbol: String): EnginePosition? {
    val keys = listOf(
        symbol,
        symbol.replace("/", ""),
        symbol.uppercase().removeSuffix("M"),
        symbol.uppercase().removeSuffix("M") + "M"
    ).map { it.uppercase() }.toSet()
    return positions.firstOrNull { it.symbol.uppercase() in keys || keys.any { k ->
        it.symbol.uppercase().replace("/", "").removeSuffix("M") == k.replace("/", "").removeSuffix("M")
    } }
}

@Composable
fun AutoTradePositionStrip(
    position: EnginePosition,
    modifier: Modifier = Modifier
) {
    val livePrice = resolveLivePrice(position.symbol)

    Surface(
        color = Color(0xFF0B1220),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, positionColor(position.type).copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = positionColor(position.type).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (position.type == "buy") "\u25B2 BUY" else "\u25BC SELL",
                            color = positionColor(position.type),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        position.symbol.ifBlank { "?" },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    Text("AUTO #${position.ticket}", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("${String.format("%.2f", position.volume)} lots", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (position.profit >= 0) "+$${String.format("%.2f", position.profit)}" else "-$${String.format("%.2f", -position.profit)}",
                        color = if (position.profit >= 0) EmeraldSuccess else RoseError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    livePrice?.let {
                        Text(
                            "NOW ${String.format("%.5f", it)}",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            PriceLevelBand(position = position, livePrice = livePrice)
        }
    }
}

@Composable
private fun PriceLevelBand(position: EnginePosition, livePrice: Double?) {
    val textMeasurer = rememberTextMeasurer()

    val values = mutableListOf(position.entryPrice)
    position.sl?.let { values.add(it) }
    position.tp?.let { values.add(it) }

    // price-based span across entry / SL / TP (+ live price when available)
    val levelLow = values.minOrNull() ?: 0.0
    val levelHigh = values.maxOrNull() ?: 1.0
    val lo = if (livePrice != null) minOf(levelLow, livePrice) else levelLow
    val hi = if (livePrice != null) maxOf(levelHigh, livePrice) else levelHigh
    val full = (hi - lo).let { if (it <= 0.0) 1.0 else it }
    val pad = full * 0.10

    fun yFor(v: Double): Float {
        val t = ((v - (lo - pad)) / (full + 2 * pad)).toFloat().coerceIn(0f, 1f)
        return t
    }

    val accent = positionColor(position.type)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
            .padding(0.dp)
    ) {
        val w = size.width
        val h = size.height

        fun drawLevelLabel(text: String, y: Float, color: Color) {
            val layout = textMeasurer.measure(
                text = text,
                style = TextStyle(color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            )
            val labelX = (w - layout.size.width).coerceAtLeast(0f) - 2.dp.toPx()
            val labelY = (y - layout.size.height / 2f).coerceIn(0f, h - layout.size.height)
            drawText(layout, color = color, topLeft = Offset(labelX, labelY))
        }

        // mid gridline
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1f
        )

        // SL (red dashed)
        position.sl?.let {
            val y = yFor(it) * h
            drawLine(
                color = RoseError,
                start = Offset(0f, y),
                end = Offset(w * 0.62f, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
            drawLevelLabel("SL ${formatLevel(it)}", y, RoseError)
        }

        // TP (green dashed)
        position.tp?.let {
            val y = yFor(it) * h
            drawLine(
                color = EmeraldSuccess,
                start = Offset(0f, y),
                end = Offset(w * 0.62f, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
            drawLevelLabel("TP ${formatLevel(it)}", y, EmeraldSuccess)
        }

        // Entry (solid accent)
        val entryY = yFor(position.entryPrice) * h
        drawLine(
            color = accent,
            start = Offset(0f, entryY),
            end = Offset(w, entryY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLevelLabel("ENTRY ${formatLevel(position.entryPrice)}", entryY, accent)

        // Current price (white)
        livePrice?.let {
            val y = yFor(it) * h
            drawLine(
                color = Color.White,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f))
            )
            drawLevelLabel("● ${formatLevel(it)}", y, Color.White)
        }
    }
}

private fun positionColor(type: String): Color =
    if (type == "buy") EmeraldSuccess else RoseError

private fun formatLevel(v: Double): String =
    if (v >= 100.0) String.format("%.2f", v) else String.format("%.4f", v)

private fun resolveLivePrice(symbol: String): Double? {
    if (symbol.isBlank()) return null
    val candidates = listOf(
        symbol,
        symbol.replace("/", ""),
        symbol.uppercase().removeSuffix("M") + "M",
        symbol.uppercase().removeSuffix("M") + "m"
    ).distinct()
    for (c in candidates) {
        PriceStreamManager.getPrice(c)?.let { if (it > 0.0) return it }
    }
    return null
}