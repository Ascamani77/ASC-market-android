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
        return symbol
            .replace("/", "")
            .replace("-", "")
            .replace(" ", "")
            .replace(".", "")
            .uppercase()
            .trim()
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
