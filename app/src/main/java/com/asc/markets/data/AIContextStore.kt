package com.asc.markets.data

import com.asc.markets.ai.AIContextService
import com.asc.markets.ai.AIDecision

/**
 * Compatibility layer for AI Context access in the data package.
 * Proxies calls to the central AIContextService.
 */
object AIContextStore {
    /**
     * Proxies to AIContextService.getDecisionForAsset
     */
    fun getAssetDecision(symbol: String): AIDecision? {
        return AIContextService.getDecisionForAsset(symbol)
    }
}
