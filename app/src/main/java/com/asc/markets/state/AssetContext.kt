package com.asc.markets.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AssetContext { FOREX, CRYPTO, COMMODITIES, INDICES, BONDS }

object AssetContextStore {
    private val _context = MutableStateFlow(AssetContext.FOREX)
    val context: StateFlow<AssetContext> = _context.asStateFlow()
    fun set(ctx: AssetContext) { _context.value = ctx }
}

fun mapCategoryToAssetContext(category: String): AssetContext = when (category.trim().lowercase()) {
    "commodities" -> AssetContext.COMMODITIES
    "stocks" -> AssetContext.INDICES
    "crypto" -> AssetContext.CRYPTO
    "futures" -> AssetContext.COMMODITIES
    "forex" -> AssetContext.FOREX
    "bonds" -> AssetContext.BONDS
    else -> AssetContext.FOREX
}
