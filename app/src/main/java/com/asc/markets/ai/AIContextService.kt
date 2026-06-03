package com.asc.markets.ai

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Centralized AI Context Service that provides real-time AI intelligence to all pages.
 * 
 * This singleton:
 * - Polls AI backend every 30 seconds
 * - Caches AI decisions and news impacts
 * - Exposes StateFlow for reactive updates
 * - Provides synchronous getters for immediate access
 * - Handles errors gracefully (no crashes if AI is offline)
 * 
 * Usage in Composables:
 * ```
 * val aiContext by AIContextService.contextState.collectAsState()
 * val decision = aiContext.decisions["EURUSD"]
 * ```
 * 
 * Usage for synchronous access:
 * ```
 * val decision = AIContextService.getDecisionForAsset("EUR/USD")
 * ```
 */
object AIContextService {
    private const val TAG = "AIContextService"
    
    // Configuration
    private const val AI_BASE_URL = "http://10.164.138.133:8000"
    private const val POLL_INTERVAL_MS = 30_000L  // 30 seconds
    private const val REQUEST_TIMEOUT_SEC = 10L
    
    // State
    private val _contextState = MutableStateFlow(AIContextState.initial())
    
    /**
     * Observable state of AI context.
     * Collect this in Composables for reactive updates.
     */
    val contextState: StateFlow<AIContextState> = _contextState.asStateFlow()
    
    // HTTP Client
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()
    
    // Coroutine scope
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    
    // ========== Public API ==========
    
    /**
     * Starts the AI Context Service.
     * Begins polling the AI backend every 30 seconds.
     * Call this in Application.onCreate().
     */
    fun start() {
        Log.d(TAG, "Starting AI Context Service")
        
        if (pollingJob?.isActive == true) {
            Log.w(TAG, "Service already running")
            return
        }
        
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    pollAIBackend()
                    delay(POLL_INTERVAL_MS)
                } catch (e: CancellationException) {
                    Log.d(TAG, "Polling cancelled")
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error: ${e.message}", e)
                    delay(POLL_INTERVAL_MS) // Continue polling even on error
                }
            }
        }
        
        Log.i(TAG, "AI Context Service started (polling every ${POLL_INTERVAL_MS / 1000}s)")
    }
    
    /**
     * Stops the AI Context Service.
     * Cancels the polling job.
     * Call this in Application.onTerminate().
     */
    fun stop() {
        Log.d(TAG, "Stopping AI Context Service")
        pollingJob?.cancel()
        pollingJob = null
        Log.i(TAG, "AI Context Service stopped")
    }
    
    /**
     * Forces an immediate refresh of AI data.
     * Useful for pull-to-refresh or manual refresh buttons.
     */
    fun refresh() {
        Log.d(TAG, "Manual refresh requested")
        serviceScope.launch {
            try {
                pollAIBackend()
            } catch (e: Exception) {
                Log.e(TAG, "Manual refresh failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Gets the AI decision for a specific asset.
     * Handles symbol normalization (EUR/USD → EURUSD).
     * 
     * @param symbol Asset symbol (e.g., "EUR/USD", "EURUSD", "BTC/USDT")
     * @return AI decision or null if not found
     */
    fun getDecisionForAsset(symbol: String): AIDecision? {
        val normalized = SymbolNormalizer.normalize(symbol)
        return _contextState.value.decisions[normalized]
    }
    
    /**
     * Gets the news impact for a specific headline.
     * 
     * @param headline News headline to search for
     * @return News impact or null if not found
     */
    fun getImpactForNews(headline: String): NewsImpact? {
        return _contextState.value.newsImpacts.firstOrNull { 
            it.headline.equals(headline, ignoreCase = true) 
        }
    }
    
    /**
     * Gets all AI decisions.
     * 
     * @return Map of normalized symbol to AI decision
     */
    fun getAllDecisions(): Map<String, AIDecision> {
        return _contextState.value.decisions
    }
    
    /**
     * Gets all news impacts.
     * 
     * @return List of news impacts
     */
    fun getAllNewsImpacts(): List<NewsImpact> {
        return _contextState.value.newsImpacts
    }
    
    // ========== Private Methods ==========
    
    /**
     * Polls the AI backend for latest data.
     * Updates the state on success, preserves cached data on failure.
     */
    private suspend fun pollAIBackend() {
        Log.d(TAG, "Polling AI backend at $AI_BASE_URL")
        
        try {
            val decisions = fetchLatestAI()
            val news = fetchNewsImpact()
            
            _contextState.update { 
                it.copy(
                    decisions = decisions,
                    newsImpacts = news,
                    lastUpdated = System.currentTimeMillis(),
                    isConnected = true,
                    errorMessage = null
                )
            }
            
            Log.i(TAG, "Fetched ${decisions.size} AI decisions, ${news.size} news items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch AI data: ${e.message}, preserving platform context")
            
            _contextState.update {
                it.copy(
                    isConnected = false,
                    errorMessage = e.message
                )
            }
            
            // Keep existing data, just mark as stale
            Log.w(TAG, "AI backend unreachable, using cached data")
        }
    }
    
    /**
     * Fetches latest AI decisions from /latest-ai endpoint.
     * 
     * @return Map of normalized symbol to AI decision
     */
    private suspend fun fetchLatestAI(): Map<String, AIDecision> = withContext(Dispatchers.IO) {
        val url = "$AI_BASE_URL/latest-ai"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }
        
        val body = response.body?.string() ?: throw Exception("Empty response body")
        parseAIDecisions(body)
    }
    
    /**
     * Fetches news impacts from /news-impact endpoint.
     * Falls back to parsing from /latest-ai if endpoint doesn't exist.
     * 
     * @return List of news impacts
     */
    private suspend fun fetchNewsImpact(): List<NewsImpact> = withContext(Dispatchers.IO) {
        try {
            val url = "$AI_BASE_URL/news-impact"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "/news-impact endpoint not available, using fallback")
                return@withContext generateFallbackNews()
            }
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            parseNewsImpacts(body)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch news impacts: ${e.message}, using fallback")
            generateFallbackNews()
        }
    }
    
    /**
     * Parses AI decisions from JSON response.
     * 
     * Expected format:
     * ```json
     * {
     *   "final_decision": [
     *     {
     *       "asset_1": "EURUSD",
     *       "journal_direction": "LONG",
     *       "journal_score": 85,
     *       "portfolio_decision_reason": "Strong momentum...",
     *       "portfolio_deployment_bucket": "HIGH"
     *     }
     *   ]
     * }
     * ```
     * 
     * @param json JSON response string
     * @return Map of normalized symbol to AI decision
     */
    private fun parseAIDecisions(json: String): Map<String, AIDecision> {
        try {
            val jsonObject = JSONObject(json)
            val finalDecision = jsonObject.optJSONArray("final_decision") ?: JSONArray()
            
            val decisions = mutableMapOf<String, AIDecision>()
            
            for (i in 0 until finalDecision.length()) {
                val item = finalDecision.getJSONObject(i)
                
                val asset = item.optString("asset_1", "").trim()
                if (asset.isEmpty()) continue
                
                val normalizedAsset = SymbolNormalizer.normalize(asset)
                
                val decision = AIDecision(
                    asset = normalizedAsset,
                    direction = item.optString("journal_direction", "NEUTRAL").uppercase(),
                    confidence = item.optDouble("journal_confidence", 0.5),
                    score = item.optInt("journal_score", 50),
                    preMoveScore = item.optDouble("pre_move_ai_score", 0.0),
                    reason = item.optString("portfolio_decision_reason", "No reason provided"),
                    deploymentBucket = item.optString("portfolio_deployment_bucket", "MEDIUM").uppercase(),
                    timestamp = System.currentTimeMillis()
                )
                
                decisions[normalizedAsset] = decision
            }
            
            Log.d(TAG, "Parsed ${decisions.size} AI decisions")
            return decisions
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI decisions: ${e.message}", e)
            return emptyMap()
        }
    }
    
    /**
     * Parses news impacts from JSON response.
     * 
     * Expected format:
     * ```json
     * {
     *   "news": [
     *     {
     *       "headline": "Dollar elevated...",
     *       "source": "Reuters",
     *       "timestamp": "8h ago",
     *       "assetType": "forex",
     *       "assetSymbol": "EUR/USD",
     *       "impact": "HIGH",
     *       "affectedAssets": ["EURUSD", "GBPUSD"],
     *       "aiConfidence": 0.85
     *     }
     *   ]
     * }
     * ```
     * 
     * @param json JSON response string
     * @return List of news impacts
     */
    private fun parseNewsImpacts(json: String): List<NewsImpact> {
        try {
            val jsonObject = JSONObject(json)
            val newsArray = jsonObject.optJSONArray("news") ?: JSONArray()
            
            val impacts = mutableListOf<NewsImpact>()
            
            for (i in 0 until newsArray.length()) {
                val item = newsArray.getJSONObject(i)
                
                val headline = item.optString("headline", "").trim()
                if (headline.isEmpty()) continue
                
                val impactStr = item.optString("impact", "LOW").uppercase()
                val impact = when (impactStr) {
                    "HIGH" -> ImpactLevel.HIGH
                    "MEDIUM" -> ImpactLevel.MEDIUM
                    else -> ImpactLevel.LOW
                }
                
                val affectedAssetsArray = item.optJSONArray("affectedAssets") ?: JSONArray()
                val affectedAssets = mutableListOf<String>()
                for (j in 0 until affectedAssetsArray.length()) {
                    affectedAssets.add(affectedAssetsArray.getString(j))
                }
                
                val newsImpact = NewsImpact(
                    headline = headline,
                    source = item.optString("source", "Market"),
                    timestamp = item.optString("timestamp", "Just now"),
                    assetType = item.optString("assetType", "forex"),
                    assetSymbol = item.optString("assetSymbol", ""),
                    impact = impact,
                    affectedAssets = affectedAssets,
                    aiConfidence = item.optDouble("aiConfidence", 0.5),
                    imageUrl = item.optString("imageUrl", "")
                )
                
                impacts.add(newsImpact)
            }
            
            Log.d(TAG, "Parsed ${impacts.size} news impacts")
            return impacts
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse news impacts: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Generates fallback news when /news-impact endpoint is unavailable.
     * Uses AI decisions to infer impact levels.
     * 
     * @return List of fallback news impacts
     */
    private fun generateFallbackNews(): List<NewsImpact> {
        val decisions = _contextState.value.decisions
        
        // Generate news based on AI decisions with high confidence
        val highConfidenceDecisions = decisions.values.filter { it.confidence > 0.7 }
        
        return highConfidenceDecisions.take(10).map { decision ->
            val assetType = inferAssetType(decision.asset)
            val impact = when {
                decision.confidence > 0.85 -> ImpactLevel.HIGH
                decision.confidence > 0.65 -> ImpactLevel.MEDIUM
                else -> ImpactLevel.LOW
            }
            
            NewsImpact(
                headline = "${decision.asset}: ${decision.direction} signal detected",
                source = "AI Analysis",
                timestamp = "Just now",
                assetType = assetType,
                assetSymbol = decision.asset,
                impact = impact,
                affectedAssets = listOf(decision.asset),
                aiConfidence = decision.confidence,
                imageUrl = ""
            )
        }
    }
    
    /**
     * Infers asset type from symbol.
     * 
     * @param symbol Normalized symbol
     * @return Asset type string
     */
    private fun inferAssetType(symbol: String): String {
        return when {
            symbol.contains("USD") && symbol.length <= 6 -> "forex"
            symbol.contains("BTC") || symbol.contains("ETH") -> "crypto"
            symbol.contains("XAU") || symbol.contains("XAG") || symbol.contains("OIL") -> "commodities"
            symbol.contains("SPX") || symbol.contains("NAS") || symbol.contains("DXY") -> "indices"
            symbol.contains("US") && symbol.contains("Y") -> "bonds"
            else -> "stocks"
        }
    }
}
