package com.asc.markets.ai

/**
 * Utility for normalizing asset symbols to ensure consistent matching
 * across different formats (EUR/USD, EURUSD, EUR-USD, etc.)
 */
object SymbolNormalizer {
    
    /**
     * Normalizes a symbol to uppercase without separators.
     * 
     * Examples:
     * - "EUR/USD" → "EURUSD"
     * - "BTC/USDT" → "BTCUSDT"
     * - "XAU/USD" → "XAUUSD"
     * - "NVDA.US" → "NVDAUS"
     * - "nvda" → "NVDA"
     * 
     * @param symbol The symbol to normalize
     * @return Normalized symbol (uppercase, no separators)
     */
    fun normalize(symbol: String): String {
        var normalized = symbol
            .replace("/", "")
            .replace("-", "")
            .replace(" ", "")
            .replace(".", "")
            .uppercase()
            .trim()
        
        // Strip common trading suffixes to ensure "MSFT.US-24" matches "MSFT"
        val suffixes = listOf("US24", "US", "F", "M", "PRO", "ECN", "S", "SPOT", "P")
        for (suffix in suffixes) {
            if (normalized.endsWith(suffix) && normalized.length > suffix.length) {
                // Only strip if it's a suffix and leaves a valid base
                // Special case: don't strip 'F' from 'USDCHF' or 'US' from 'EURUSD'
                if (suffix == "F" && (normalized.endsWith("CHF") || normalized.endsWith("XAU") || normalized.endsWith("XAG"))) continue
                if (suffix == "US" && (normalized.startsWith("EUR") || normalized.startsWith("GBP") || normalized.startsWith("AUD"))) continue
                
                normalized = normalized.substring(0, normalized.length - suffix.length)
                break
            }
        }

        // Special mappings for Indices and Bonds
        return when (normalized) {
            "USTN10YR" -> "US10Y"
            "USTN2YR" -> "US02Y"
            "NAS100" -> "NAS100"
            "US30" -> "US30"
            "SPX500" -> "SPX500"
            "GER40" -> "GER40"
            else -> normalized
        }
    }
    
    /**
     * Checks if two symbols match after normalization.
     * 
     * Examples:
     * - matches("EUR/USD", "EURUSD") → true
     * - matches("BTC/USDT", "btcusdt") → true
     * - matches("NVDA", "NVDA.US") → false (different after normalization)
     * 
     * @param symbol1 First symbol
     * @param symbol2 Second symbol
     * @return true if symbols match after normalization
     */
    fun matches(symbol1: String, symbol2: String): Boolean {
        return normalize(symbol1) == normalize(symbol2)
    }
    
    /**
     * Finds a matching symbol in a collection.
     * 
     * @param target The symbol to find
     * @param symbols Collection of symbols to search
     * @return The matching symbol from the collection, or null if not found
     */
    fun findMatch(target: String, symbols: Collection<String>): String? {
        val normalizedTarget = normalize(target)
        return symbols.firstOrNull { normalize(it) == normalizedTarget }
    }
}
