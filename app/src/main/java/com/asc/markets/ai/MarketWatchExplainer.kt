package com.asc.markets.ai

import android.util.Log
import com.asc.markets.backend.GroqClient
import com.asc.markets.data.PreMoveCandidate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Generates AI-powered explanations for Market Watch candidates using Groq.
 * 
 * Features:
 * - Real-time AI explanations based on actual market data
 * - Caching to avoid excessive API calls
 * - Automatic refresh every 5 minutes
 * - Fallback to technical explanation if AI unavailable
 */
object MarketWatchExplainer {
    private const val TAG = "MarketWatchExplainer"
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Cache: symbol -> (explanation, timestamp)
    private val explanationCache = mutableMapOf<String, Pair<String, Long>>()
    
    // State for tracking generation progress
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    /**
     * Get explanation for a candidate.
     * Returns cached explanation if available and fresh, otherwise generates new one.
     */
    suspend fun getExplanation(candidate: PreMoveCandidate): String {
        val cached = explanationCache[candidate.symbol]
        val now = System.currentTimeMillis()
        
        // Return cached if fresh (less than 5 minutes old)
        if (cached != null && (now - cached.second) < CACHE_DURATION_MS) {
            return cached.first
        }
        
        // Generate new explanation
        return generateExplanation(candidate)
    }
    
    /**
     * Generate AI explanation for a candidate using Groq.
     */
    private suspend fun generateExplanation(candidate: PreMoveCandidate): String = withContext(Dispatchers.IO) {
        try {
            if (!GroqClient.isKeyConfigured()) {
                Log.w(TAG, "Groq API key not configured, using fallback")
                return@withContext generateFallbackExplanation(candidate)
            }
            
            _isGenerating.value = true
            
            val prompt = buildPrompt(candidate)
            val response = GroqClient.chatCompletion(prompt, model = "llama-3.3-70b-versatile")
            
            // Cache the result
            explanationCache[candidate.symbol] = Pair(response.trim(), System.currentTimeMillis())
            
            Log.d(TAG, "Generated AI explanation for ${candidate.symbol}")
            return@withContext response.trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate AI explanation for ${candidate.symbol}: ${e.message}", e)
            return@withContext generateFallbackExplanation(candidate)
        } finally {
            _isGenerating.value = false
        }
    }
    
    /**
     * Build prompt for Groq to generate market watch explanation.
     */
    private fun buildPrompt(candidate: PreMoveCandidate): String {
        return """
You are ASC AI, a professional trading intelligence system. Generate a concise, single-sentence explanation for why ${candidate.symbol} is on the pre-move watch list.

MARKET DATA:
- Symbol: ${candidate.symbol}
- Pre-Move Score: ${candidate.preMoveScore}%
- State: ${candidate.state}
- Direction Bias: ${candidate.directionBias}
- Regime: ${candidate.regime}
- Compression Score: ${candidate.compressionScore}%
- Ignition Score: ${candidate.ignitionScore}%
- Liquidity Score: ${candidate.liquidityScore}%
- Risk Gate: ${candidate.riskGate}
- Price: ${candidate.price}
- Change: ${String.format("%.2f", candidate.changePercent)}%
- Liquidity Magnet: ${candidate.liquidityMagnet}
- Expected Window: ${candidate.expectedWindow}

INSTRUCTIONS:
1. Write ONE sentence (max 120 characters)
2. Focus on the most important factor (compression, ignition, or AI signal)
3. Use professional trading language
4. Mention the timeframe (${candidate.timeframe})
5. Include directional bias if strong
6. DO NOT use phrases like "This asset" or "The pair" - start directly with the reason

EXAMPLES:
- "Expansion candidate on H1 with Sell-side liquidity nearest, BULLISH structural pressure and WATCH risk gate."
- "AI High Conviction regime with 85% ignition probability and PASS risk gate on H1."
- "Compression on H1 with strong directional pressure building toward buy-side liquidity."

Generate the explanation now:
        """.trimIndent()
    }
    
    /**
     * Generate fallback explanation when AI is unavailable.
     */
    private fun generateFallbackExplanation(candidate: PreMoveCandidate): String {
        return "${candidate.regime} on ${candidate.timeframe} with ${candidate.liquidityMagnet} nearest, ${candidate.directionBias} structural pressure and ${candidate.riskGate} risk gate."
    }
    
    /**
     * Pre-generate explanations for multiple candidates in batch.
     * Useful for warming up the cache when Market Watch loads.
     */
    fun preGenerateExplanations(candidates: List<PreMoveCandidate>) {
        scope.launch {
            candidates.take(10).forEach { candidate ->
                try {
                    getExplanation(candidate)
                    delay(500) // Rate limiting: 2 requests per second
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pre-generate explanation for ${candidate.symbol}: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Clear the explanation cache.
     * Useful when market conditions change significantly.
     */
    fun clearCache() {
        explanationCache.clear()
        Log.d(TAG, "Explanation cache cleared")
    }
    
    /**
     * Get cache statistics for debugging.
     */
    fun getCacheStats(): String {
        val now = System.currentTimeMillis()
        val fresh = explanationCache.count { (now - it.value.second) < CACHE_DURATION_MS }
        val stale = explanationCache.size - fresh
        return "Cache: $fresh fresh, $stale stale, ${explanationCache.size} total"
    }
}
