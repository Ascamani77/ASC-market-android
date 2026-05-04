package com.asc.markets.ui.components

import androidx.compose.runtime.Composable
import com.trading.app.components.AssetIcon
import com.trading.app.models.SymbolInfo

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

/**
 * Main instrument icon resolver
 * Automatically shows correct icon based on instrument type
 */
@Composable
fun InstrumentIcon(instrument: Instrument, size: Int = 28) {
    val cleanSymbol = instrument.symbol.replace("/", "")
    val typeString = when (instrument.type) {
        AssetType.FOREX -> "forex"
        AssetType.STOCK -> "stock"
        AssetType.INDEX -> "index"
        AssetType.COMMODITY -> "commodity"
    }
    
    val symbolInfo = SymbolInfo(
        ticker = cleanSymbol,
        name = instrument.name,
        type = typeString
    )
    
    AssetIcon(symbol = symbolInfo, size = size)
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
