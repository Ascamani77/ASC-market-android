package com.asc.markets.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AssetContext { ALL, FOREX, CRYPTO, COMMODITIES, INDICES, STOCKS, FUTURES, BONDS }

object AssetContextStore {
    // default to ALL so modules can show aggregated view until user picks a lens
    private val _context = MutableStateFlow(AssetContext.ALL)
    val context: StateFlow<AssetContext> = _context.asStateFlow()
    fun set(ctx: AssetContext) { _context.value = ctx }
    fun setAndInvalidate(ctx: AssetContext) {
        _context.value = ctx
        // Clear any cached asset-scoped data when the active context changes
        com.asc.markets.data.AssetDataCache.invalidateAll()
    }
    fun get(): AssetContext = _context.value
    fun aiPromptPrefix(): String = when (_context.value) {
        AssetContext.ALL -> "Analyze global markets across all assets"
        AssetContext.FOREX -> "Analyze Forex macro conditions"
        AssetContext.CRYPTO -> "Analyze Crypto market conditions"
        AssetContext.COMMODITIES -> "Analyze Commodities supply/demand conditions"
        AssetContext.INDICES -> "Analyze Equity indices breadth and sector rotation"
        AssetContext.STOCKS -> "Analyze individual equity performance and sector movers"
        AssetContext.FUTURES -> "Analyze futures market and contract trends"
        AssetContext.BONDS -> "Analyze bond market and yield curve"
    }
}

fun mapCategoryToAssetContext(category: String): AssetContext = when (category.trim().lowercase()) {
    "all", "all assets", "global" -> AssetContext.ALL
    "commodities", "commodity", "energy" -> AssetContext.COMMODITIES
    "stocks", "stock" -> AssetContext.STOCKS
    "crypto", "cryptocurrency", "cryptos" -> AssetContext.CRYPTO
    "futures", "future" -> AssetContext.FUTURES
    "forex", "fx" -> AssetContext.FOREX
    "bonds", "bond" -> AssetContext.BONDS
    else -> AssetContext.ALL
}
