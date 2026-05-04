package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.IndigoAccent
import com.trading.app.components.AssetIcon
import com.trading.app.models.SymbolInfo
import java.util.Locale

fun guessAssetType(symbol: String): String {
    val s = symbol.uppercase(Locale.US)
    return when {
        isCryptoSymbol(s) -> "crypto"
        s.contains("/") -> "forex"
        s.startsWith("XAU") || s.startsWith("XAG") || s == "GOLD" || s == "SILVER" || s.contains("OIL") || s == "NGAS" -> "commodity"
        s.length <= 5 && !s.contains("-") -> "stock"
        else -> "index"
    }
}

@Composable
fun PairFlags(symbol: String, size: Int = 20) {
    val cleanSymbol = cryptoIconTicker(symbol) ?: symbol.replace("/", "")
    val type = guessAssetType(symbol)
    val symbolInfo = SymbolInfo(
        ticker = cleanSymbol,
        name = symbol,
        type = type
    )
    
    AssetIcon(symbol = symbolInfo, size = size)
}

private fun isCryptoSymbol(symbol: String): Boolean {
    val normalized = symbol.replace("/", "").replace("-", "").replace("_", "")
    return normalized.startsWith("BTC") ||
        normalized.startsWith("ETH") ||
        normalized.startsWith("SOL") ||
        normalized.startsWith("USDT") ||
        normalized.endsWith("USDT")
}

private fun cryptoIconTicker(symbol: String): String? {
    val normalized = symbol
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .uppercase(Locale.US)
    return when {
        normalized == "BTCUSDT" -> "BTC"
        normalized == "ETHUSDT" -> "ETH"
        else -> null
    }
}
