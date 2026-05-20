# AI Context Service - Design Document

## Overview

The AI Context Service is a centralized singleton that provides real-time AI intelligence to all pages in the application. It acts as a single source of truth for AI-generated insights, impact ratings, confidence scores, and market context.

## Problem Statement

Currently:
- Market Watch shows news with hardcoded/mock impact ratings
- Macro Stream (EventStreamScreen) displays all news as "LOW" impact
- Each page would need to independently fetch AI data, leading to:
  - Duplicate network calls
  - Inconsistent data across pages
  - No shared context between AI Chat and other features
  - Difficult state management

## Solution Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────────────┐
│                     AI Backend (Python)                      │
│              http://10.164.138.133:8000                      │
│                                                              │
│  Endpoints:                                                  │
│  - GET /latest-ai (AI decisions for all 24 assets)          │
│  - GET /news-impact (News with AI impact ratings)           │
│  - WebSocket /ws/ai-stream (Real-time updates - future)     │
└──────────────────────┬───────────────────────────────────────┘
                       │ HTTP/WebSocket
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              AIContextService (Kotlin Singleton)             │
│                                                              │
│  Responsibilities:                                           │
│  - Poll AI backend every 30 seconds                          │
│  - Parse and cache AI decisions                              │
│  - Expose StateFlow for reactive updates                     │
│  - Handle connection errors gracefully                       │
│  - Provide synchronous getters for immediate access          │
└──────────────────────┬───────────────────────────────────────┘
                       │ StateFlow
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Consumer Pages                            │
│                                                              │
│  - Market Watch (MarketOverviewTab)                          │
│  - Macro Stream (EventStreamScreen)                          │
│  - AI Chat (future integration)                              │
│  - Trading Panel (future integration)                        │
│  - Any other page needing AI context                         │
└─────────────────────────────────────────────────────────────┘
```

## Data Models

### 1. AI Decision (from `/latest-ai`)

```kotlin
data class AIDecision(
    val asset: String,                    // e.g., "EURUSD", "BTCUSDT"
    val direction: String,                // "LONG", "SHORT", "NEUTRAL"
    val confidence: Double,               // 0.0 to 1.0
    val score: Int,                       // 0 to 100
    val reason: String,                   // Human-readable explanation
    val deploymentBucket: String,         // "HIGH", "MEDIUM", "LOW"
    val timestamp: Long                   // Unix timestamp
)
```

### 2. News Impact (from `/news-impact` or enhanced `/latest-ai`)

```kotlin
data class NewsImpact(
    val headline: String,
    val source: String,
    val timestamp: String,                // "8h ago", "Just now"
    val assetType: String,                // "forex", "crypto", "stocks", etc.
    val assetSymbol: String,              // "EUR/USD", "BTC/USDT"
    val impact: ImpactLevel,              // HIGH, MEDIUM, LOW
    val affectedAssets: List<String>,     // ["EURUSD", "GBPUSD"]
    val aiConfidence: Double,             // 0.0 to 1.0
    val imageUrl: String = ""
)

enum class ImpactLevel {
    HIGH,    // Red - Major market-moving event
    MEDIUM,  // Yellow - Moderate impact
    LOW      // Gray - Minor/informational
}
```

### 3. AI Context State

```kotlin
data class AIContextState(
    val decisions: Map<String, AIDecision>,     // Key: asset symbol
    val newsImpacts: List<NewsImpact>,
    val lastUpdated: Long,                      // Unix timestamp
    val isConnected: Boolean,
    val errorMessage: String? = null
)
```

## Component Design

### AIContextService.kt

```kotlin
package com.asc.markets.ai

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AIContextService {
    // Configuration
    private const val AI_BASE_URL = "http://10.164.138.133:8000"
    private const val POLL_INTERVAL_MS = 30_000L  // 30 seconds
    private const val REQUEST_TIMEOUT_SEC = 10L
    
    // State
    private val _contextState = MutableStateFlow(AIContextState(
        decisions = emptyMap(),
        newsImpacts = emptyList(),
        lastUpdated = 0L,
        isConnected = false
    ))
    val contextState: StateFlow<AIContextState> = _contextState.asStateFlow()
    
    // HTTP Client
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()
    
    // Coroutine scope
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    
    // Public API
    fun start() { /* Start polling */ }
    fun stop() { /* Stop polling */ }
    fun refresh() { /* Force immediate refresh */ }
    
    // Synchronous getters for immediate access
    fun getDecisionForAsset(symbol: String): AIDecision? { /* ... */ }
    fun getImpactForNews(headline: String): NewsImpact? { /* ... */ }
    fun getAllDecisions(): Map<String, AIDecision> { /* ... */ }
    fun getAllNewsImpacts(): List<NewsImpact> { /* ... */ }
    
    // Private methods
    private suspend fun pollAIBackend() { /* ... */ }
    private suspend fun fetchLatestAI(): Map<String, AIDecision> { /* ... */ }
    private suspend fun fetchNewsImpact(): List<NewsImpact> { /* ... */ }
    private fun parseAIDecisions(json: String): Map<String, AIDecision> { /* ... */ }
    private fun parseNewsImpacts(json: String): List<NewsImpact> { /* ... */ }
}
```

## Integration Points

### 1. Application Initialization (TradingApp.kt)

```kotlin
class TradingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Start AI Context Service
        AIContextService.start()
    }
    
    override fun onTerminate() {
        AIContextService.stop()
        super.onTerminate()
    }
}
```

### 2. Market Watch (MarketOverviewTab.kt)

```kotlin
@Composable
fun MarketOverviewTab(...) {
    // Observe AI context
    val aiContext by AIContextService.contextState.collectAsState()
    
    // Use AI decisions for bias/confidence
    val aiDecision = aiContext.decisions[selectedPair.symbol]
    
    // Use news impacts for proper ratings
    val newsWithImpact = aiContext.newsImpacts.filter { 
        it.assetType == assetCtxForNews.name.lowercase() 
    }
    
    // Display news with proper impact colors
    newsWithImpact.forEach { news ->
        NewsCard(
            headline = news.headline,
            impact = news.impact,  // HIGH/MEDIUM/LOW
            confidence = news.aiConfidence
        )
    }
}
```

### 3. Macro Stream (EventStreamScreen.kt)

```kotlin
@Composable
fun EventStreamScreen(...) {
    // Observe AI context
    val aiContext by AIContextService.contextState.collectAsState()
    
    // Enhance calendar events with AI impact
    val eventsWithAI = uiState.events.map { event ->
        val aiDecision = aiContext.decisions[event.assets_affected.firstOrNull()]
        event.copy(
            aiConfidence = aiDecision?.confidence,
            aiDirection = aiDecision?.direction,
            enhancedImpact = calculateEnhancedImpact(event, aiDecision)
        )
    }
    
    // Display with AI-enhanced impact ratings
    EventList(events = eventsWithAI)
}
```

### 4. AI Chat (Future)

```kotlin
@Composable
fun AIChatScreen(...) {
    val aiContext by AIContextService.contextState.collectAsState()
    
    // AI can reference what other pages show
    val chatContext = buildChatContext(
        currentDecisions = aiContext.decisions,
        recentNews = aiContext.newsImpacts,
        userQuery = userMessage
    )
    
    sendToAI(chatContext)
}
```

## Update Strategy

### Polling Approach (Phase 1)

- **Interval**: 30 seconds
- **Pros**: Simple, reliable, works with existing REST API
- **Cons**: 30-second latency for updates
- **Implementation**: `CoroutineScope` with `delay()`

```kotlin
private fun startPolling() {
    pollingJob = serviceScope.launch {
        while (isActive) {
            try {
                pollAIBackend()
                delay(POLL_INTERVAL_MS)
            } catch (e: Exception) {
                Log.e("AIContextService", "Polling error", e)
                delay(POLL_INTERVAL_MS) // Continue polling even on error
            }
        }
    }
}
```

### WebSocket Approach (Phase 2 - Future)

- **Latency**: Real-time (< 1 second)
- **Pros**: Instant updates, lower server load
- **Cons**: Requires WebSocket endpoint on AI backend
- **Implementation**: OkHttp WebSocket client

## Error Handling

### Connection Failures

```kotlin
private suspend fun pollAIBackend() {
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
    } catch (e: Exception) {
        Log.e("AIContextService", "Failed to fetch AI data", e)
        
        _contextState.update {
            it.copy(
                isConnected = false,
                errorMessage = e.message
            )
        }
        
        // Keep existing data, just mark as stale
    }
}
```

### Graceful Degradation

- If AI backend is offline, pages fall back to:
  - Last known AI decisions (cached)
  - Mock/default impact ratings
  - User is notified via UI indicator

### Retry Strategy

- Continuous polling (no exponential backoff)
- Each poll attempt is independent
- Failed polls don't stop the service

## Caching Strategy

### In-Memory Cache

- **Location**: `AIContextState` in `MutableStateFlow`
- **Lifetime**: Until app termination
- **Size**: ~24 AI decisions + ~50 news items = ~10KB
- **Invalidation**: Every 30 seconds (on successful poll)

### Disk Cache (Future Enhancement)

- Persist last known state to SharedPreferences
- Load on app startup for instant display
- Prevents empty state on cold start

## Performance Considerations

### Network Efficiency

- Single HTTP request every 30 seconds
- Gzip compression enabled
- Connection pooling via OkHttp
- **Estimated bandwidth**: ~5KB/30s = ~10KB/min = ~14MB/day

### Memory Footprint

- StateFlow: ~1KB overhead
- AI decisions: ~24 × 200 bytes = ~5KB
- News impacts: ~50 × 300 bytes = ~15KB
- **Total**: ~21KB (negligible)

### Thread Safety

- All state updates via `MutableStateFlow.update { }` (atomic)
- Network calls on `Dispatchers.IO`
- UI observation on main thread via `collectAsState()`

## Testing Strategy

### Unit Tests

```kotlin
class AIContextServiceTest {
    @Test
    fun `parseAIDecisions returns correct map`() { }
    
    @Test
    fun `getDecisionForAsset returns null when not found`() { }
    
    @Test
    fun `error state preserves existing data`() { }
}
```

### Integration Tests

```kotlin
class AIContextServiceIntegrationTest {
    @Test
    fun `service fetches real data from backend`() { }
    
    @Test
    fun `service handles network timeout gracefully`() { }
}
```

### Manual Testing

1. Start app → Verify AI service starts polling
2. Open Market Watch → Verify news shows proper impact ratings
3. Open Macro Stream → Verify events show AI confidence
4. Disconnect AI backend → Verify graceful degradation
5. Reconnect AI backend → Verify service recovers

## Configuration

### Build Config

```kotlin
// build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "AI_BASE_URL", "\"http://10.164.138.133:8000\"")
        buildConfigField("Long", "AI_POLL_INTERVAL_MS", "30000L")
    }
    
    buildTypes {
        debug {
            buildConfigField("String", "AI_BASE_URL", "\"http://10.164.138.133:8000\"")
        }
        release {
            buildConfigField("String", "AI_BASE_URL", "\"https://api.yourproduction.com\"")
        }
    }
}
```

### Runtime Configuration (Future)

```kotlin
// Allow user to configure AI backend URL in settings
fun updateAIBackendUrl(newUrl: String) {
    AIContextService.stop()
    AIContextService.configure(baseUrl = newUrl)
    AIContextService.start()
}
```

## Migration Path

### Phase 1: Core Service (This Spec)
- ✅ Create AIContextService singleton
- ✅ Implement polling mechanism
- ✅ Define data models
- ✅ Integrate with Market Watch
- ✅ Integrate with Macro Stream

### Phase 2: Enhanced Features
- WebSocket support for real-time updates
- Disk caching for offline support
- User-configurable poll interval
- AI Chat integration

### Phase 3: Advanced Intelligence
- Predictive caching (pre-fetch likely queries)
- Asset correlation analysis
- Multi-timeframe context
- Historical AI decision tracking

## Dependencies

### Required Libraries

```kotlin
// build.gradle.kts (app level)
dependencies {
    // Already included
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // For JSON parsing (already included)
    // Using org.json.JSONObject (built-in Android)
}
```

### No New Dependencies Required

All necessary libraries are already in the project.

## Security Considerations

### Network Security

- Use HTTPS in production
- Validate SSL certificates
- No sensitive data in AI requests (only asset symbols)

### Data Privacy

- AI decisions are market data (public information)
- No user PII sent to AI backend
- News headlines are public information

## Monitoring & Observability

### Logging

```kotlin
private const val TAG = "AIContextService"

Log.d(TAG, "Polling AI backend...")
Log.i(TAG, "Fetched ${decisions.size} AI decisions")
Log.w(TAG, "AI backend connection failed, using cached data")
Log.e(TAG, "Failed to parse AI response", exception)
```

### Metrics (Future)

- Poll success rate
- Average response time
- Cache hit rate
- Error frequency

## Success Criteria

### Functional Requirements

- ✅ Service starts automatically on app launch
- ✅ Polls AI backend every 30 seconds
- ✅ Exposes StateFlow for reactive updates
- ✅ Market Watch shows proper news impact ratings
- ✅ Macro Stream shows AI confidence scores
- ✅ Graceful error handling (no crashes)

### Non-Functional Requirements

- ✅ < 100ms latency for synchronous getters
- ✅ < 10KB memory footprint
- ✅ < 15MB/day network usage
- ✅ 99% uptime (continues working even if AI backend is down)

## Open Questions

1. **News Impact Endpoint**: Does `/latest-ai` already include news impact ratings, or do we need a separate `/news-impact` endpoint?
   - **Decision**: Check AI backend, add endpoint if needed

2. **Asset Symbol Normalization**: How to handle symbol variations (EUR/USD vs EURUSD vs EUR-USD)?
   - **Decision**: Create symbol normalization utility

3. **Impact Calculation**: If AI backend doesn't provide impact ratings, should we calculate them client-side?
   - **Decision**: Prefer server-side calculation, fallback to client-side heuristics

4. **Offline Behavior**: Should we persist AI decisions to disk for offline access?
   - **Decision**: Phase 2 enhancement, not required for MVP

## Next Steps

After design approval:
1. Create `AIContextService.kt` with core polling logic
2. Define data models in `com.asc.markets.ai` package
3. Integrate with Market Watch (update news display)
4. Integrate with Macro Stream (add AI confidence)
5. Test with real AI backend
6. Document usage for future pages

---

**Design Status**: ✅ Ready for Review
**Estimated Implementation Time**: 4-6 hours
**Risk Level**: Low (isolated service, graceful degradation)
