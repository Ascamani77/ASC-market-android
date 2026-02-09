package com.asc.markets.data

import com.asc.markets.state.AssetContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory caches keyed by asset context. Call `invalidateAll()` when AssetContext changes.
 * This is intentionally tiny and synchronous — replace with app-wide cache manager if needed.
 */
object AssetDataCache {
    private val newsCache = ConcurrentHashMap<AssetContext, List<Any>>()
    private val exploreCache = ConcurrentHashMap<AssetContext, List<Any>>()

    fun <T> putNews(ctx: AssetContext, items: List<T>) {
        newsCache[ctx] = items as List<Any>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getNews(ctx: AssetContext): List<T>? = newsCache[ctx] as? List<T>

    fun <T> putExplore(ctx: AssetContext, items: List<T>) {
        exploreCache[ctx] = items as List<Any>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getExplore(ctx: AssetContext): List<T>? = exploreCache[ctx] as? List<T>

    fun invalidateAll() {
        newsCache.clear()
        exploreCache.clear()
    }
}
