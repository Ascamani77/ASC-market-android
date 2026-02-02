package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.asc.markets.R
import com.asc.markets.ui.theme.IndigoAccent

// Asset type classification
enum class AssetType {
    FOREX,
    STOCK,
    INDEX,
    COMMODITY
}

// Instrument data class
data class Instrument(
    val symbol: String,
    val name: String,
    val type: AssetType
)

// Currency flag emoji mapping (text-based)
private val currencyFlagEmoji = mapOf(
    "USD" to "🇺🇸",
    "EUR" to "🇪🇺",
    "GBP" to "🇬🇧",
    "JPY" to "🇯🇵",
    "CHF" to "🇨🇭",
    "AUD" to "🇦🇺",
    "CAD" to "🇨🇦",
    "NZD" to "🇳🇿",
    "CNY" to "🇨🇳"
)

/**
 * Main instrument icon resolver
 * Automatically shows correct icon based on instrument type
 */
@Composable
fun InstrumentIcon(instrument: Instrument, size: Int = 28) {
    when (instrument.type) {
        AssetType.FOREX -> ForexIcon(instrument.symbol, size)
        AssetType.STOCK -> StockIcon(instrument.symbol, size)
        AssetType.INDEX -> IndexIcon(instrument.symbol, size)
        AssetType.COMMODITY -> CommodityIcon(instrument.symbol, size)
    }
}

/**
 * Forex icon: shows pair of currency flags using emoji (larger size)
 */
@Composable
fun ForexIcon(pair: String, size: Int = 28) {
    if (pair.length < 6) {
        // Fallback text badge if pair format is invalid
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(IndigoAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pair.take(2),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val base = pair.substring(0, 3)
    val quote = pair.substring(3, 6)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.size(size.dp)
    ) {
        val baseFlag = currencyFlagEmoji[base]
        val quoteFlag = currencyFlagEmoji[quote]

        if (baseFlag != null) {
            Text(
                text = baseFlag,
                fontSize = 20.sp,
                modifier = Modifier.size((size / 1.5).dp)
            )
        } else {
            FallbackCurrencyBadge(base, (size / 1.5).dp)
        }

        if (quoteFlag != null) {
            Text(
                text = quoteFlag,
                fontSize = 20.sp,
                modifier = Modifier.size((size / 1.5).dp)
            )
        } else {
            FallbackCurrencyBadge(quote, (size / 1.5).dp)
        }
    }
}

/**
 * Stock icon: text badge with symbol
 */
@Composable
fun StockIcon(symbol: String, size: Int = 28) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color(0xFF2d2d2d), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.take(2),
            color = Color.White,
            fontSize = (size / 3).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Index icon: text badge with symbol
 */
@Composable
fun IndexIcon(code: String, size: Int = 28) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color(0xFF2d2d2d), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code.take(3),
            color = Color.White,
            fontSize = (size / 4).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Commodity icon: text badge with symbol
 */
@Composable
fun CommodityIcon(symbol: String, size: Int = 28) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color(0xFF2d2d2d), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.take(3),
            color = Color.White,
            fontSize = (size / 4).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Fallback badge for currencies without flag assets
 */
@Composable
private fun FallbackCurrencyBadge(code: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(IndigoAccent.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code.take(1),
            color = Color.White,
            fontSize = (size.value / 2).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Classify a pair symbol into AssetType
 */
fun classifyAsset(symbol: String): AssetType {
    return when {
        symbol.contains("/") -> AssetType.FOREX
        symbol.startsWith("XAU") || symbol.startsWith("XAG") || 
        symbol.contains("OIL") || symbol == "NGAS" -> AssetType.COMMODITY
        symbol.length <= 5 && !symbol.contains("-") -> AssetType.STOCK
        else -> AssetType.INDEX
    }
}
