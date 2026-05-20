# AI Context Service - Implementation Tasks

## Task 1: Create Core Service Structure
**Status**: completed
**Assignee**: unassigned
**Priority**: high

### Description
Create the `AIContextService.kt` singleton with basic structure, data models, and StateFlow setup.

### Acceptance Criteria
- [ ] Create `com.asc.markets.ai` package
- [ ] Create `AIContextService.kt` object (singleton)
- [ ] Define `AIDecision` data class
- [ ] Define `NewsImpact` data class with `ImpactLevel` enum
- [ ] Define `AIContextState` data class
- [ ] Create `MutableStateFlow<AIContextState>` with initial empty state
- [ ] Expose public `StateFlow<AIContextState>` for observation
- [ ] Add `start()`, `stop()`, and `refresh()` methods (empty implementations)

### Files to Create
- `app/src/main/java/com/asc/markets/ai/AIContextService.kt`
- `app/src/main/java/com/asc/markets/ai/AIModels.kt`

### Dependencies
None

---

## Task 2: Implement HTTP Client and Polling
**Status**: pending
**Assignee**: unassigned
**Priority**: high

### Description
Implement the HTTP client, polling mechanism, and network request logic.

### Acceptance Criteria
- [ ] Create OkHttpClient with 10-second timeout
- [ ] Implement `pollAIBackend()` coroutine function
- [ ] Implement `fetchLatestAI()` to call `/latest-ai` endpoint
- [ ] Implement `fetchNewsImpact()` to call `/news-impact` endpoint (or parse from `/latest-ai`)
- [ ] Create polling loop with 30-second interval
- [ ] Implement `start()` to begin polling
- [ ] Implement `stop()` to cancel polling job
- [ ] Implement `refresh()` to force immediate poll
- [ ] Add proper error handling with try-catch
- [ ] Log all network operations

### Files to Modify
- `app/src/main/java/com/asc/markets/ai/AIContextService.kt`

### Dependencies
- Task 1 must be completed

### Testing
```kotlin
// Manual test
AIContextService.start()
delay(5000)
val state = AIContextService.contextState.value
assert(state.isConnected)
assert(state.decisions.isNotEmpty())
```

---

## Task 3: Implement JSON Parsing
**Status**: pending
**Assignee**: unassigned
**Priority**: high

### Description
Parse JSON responses from AI backend into Kotlin data classes.

### Acceptance Criteria
- [ ] Implement `parseAIDecisions(json: String): Map<String, AIDecision>`
- [ ] Implement `parseNewsImpacts(json: String): List<NewsImpact>`
- [ ] Handle missing/null fields gracefully
- [ ] Normalize asset symbols (EUR/USD → EURUSD)
- [ ] Map AI confidence to impact levels (HIGH/MEDIUM/LOW)
- [ ] Add error handling for malformed JSON
- [ ] Log parsing errors with details

### Files to Modify
- `app/src/main/java/com/asc/markets/ai/AIContextService.kt`

### Dependencies
- Task 2 must be completed

### Example AI Response Format
```json
{
  "final_decision": [
    {
      "asset_1": "EURUSD",
      "journal_direction": "LONG",
      "journal_score": 85,
      "portfolio_decision_reason": "Strong momentum...",
      "portfolio_deployment_bucket": "HIGH"
    }
  ]
}
```

### Testing
```kotlin
val json = """{"final_decision": [...]}"""
val decisions = AIContextService.parseAIDecisions(json)
assert(decisions["EURUSD"]?.direction == "LONG")
assert(decisions["EURUSD"]?.score == 85)
```

---

## Task 4: Add Synchronous Getters
**Status**: pending
**Assignee**: unassigned
**Priority**: medium

### Description
Implement synchronous getter methods for immediate access to cached data.

### Acceptance Criteria
- [ ] Implement `getDecisionForAsset(symbol: String): AIDecision?`
- [ ] Implement `getImpactForNews(headline: String): NewsImpact?`
- [ ] Implement `getAllDecisions(): Map<String, AIDecision>`
- [ ] Implement `getAllNewsImpacts(): List<NewsImpact>`
- [ ] Handle symbol normalization in getters (EUR/USD → EURUSD)
- [ ] Return null for missing data (don't throw exceptions)
- [ ] Add KDoc comments for all public methods

### Files to Modify
- `app/src/main/java/com/asc/markets/ai/AIContextService.kt`

### Dependencies
- Task 3 must be completed

### Testing
```kotlin
val decision = AIContextService.getDecisionForAsset("EUR/USD")
assert(decision != null)
assert(decision.asset == "EURUSD")
```

---

## Task 5: Integrate with TradingApp
**Status**: pending
**Assignee**: unassigned
**Priority**: high

### Description
Start AIContextService when the app launches and stop it when the app terminates.

### Acceptance Criteria
- [ ] Call `AIContextService.start()` in `TradingApp.onCreate()`
- [ ] Call `AIContextService.stop()` in `TradingApp.onTerminate()`
- [ ] Add log statements for service lifecycle
- [ ] Verify service starts before any UI is rendered
- [ ] Handle service initialization errors gracefully

### Files to Modify
- `app/src/main/kotlin/com/trading/app/TradingApp.kt`

### Dependencies
- Task 2 must be completed

### Testing
1. Launch app
2. Check logcat for "AIContextService started"
3. Wait 30 seconds
4. Check logcat for "Polling AI backend..."
5. Close app
6. Check logcat for "AIContextService stopped"

---

## Task 6: Integrate with Market Watch
**Status**: pending
**Assignee**: unassigned
**Priority**: high

### Description
Update Market Watch (MarketOverviewTab) to use AI Context Service for news impact ratings.

### Acceptance Criteria
- [ ] Observe `AIContextService.contextState` in MarketOverviewTab
- [ ] Replace mock news with `aiContext.newsImpacts`
- [ ] Display impact level with proper colors (HIGH=Red, MEDIUM=Yellow, LOW=Gray)
- [ ] Show AI confidence percentage next to impact
- [ ] Filter news by current AssetContext
- [ ] Handle empty/loading state gracefully
- [ ] Add connection status indicator
- [ ] Update news card UI to show impact badge

### Files to Modify
- `app/src/main/java/com/asc/markets/ui/screens/dashboard/MarketOverviewTab.kt`

### UI Changes
```kotlin
// Before
NewsCard(headline = "Dollar elevated...", impact = "LOW")

// After
val aiContext by AIContextService.contextState.collectAsState()
val newsWithImpact = aiContext.newsImpacts.filter { 
    it.assetType == assetCtxForNews.name.lowercase() 
}

newsWithImpact.forEach { news ->
    NewsCard(
        headline = news.headline,
        impact = news.impact,  // ImpactLevel.HIGH/MEDIUM/LOW
        confidence = news.aiConfidence,
        color = when(news.impact) {
            ImpactLevel.HIGH -> RoseError
            ImpactLevel.MEDIUM -> Color(0xFFFFB300)
            ImpactLevel.LOW -> SlateText
        }
    )
}
```

### Dependencies
- Task 4 must be completed
- Task 5 must be completed

### Testing
1. Open Market Watch
2. Verify news items show proper impact colors
3. Verify impact ratings change based on AI data
4. Switch asset contexts (Forex → Crypto)
5. Verify news filters correctly

---

## Task 7: Integrate with Macro Stream
**Status**: pending
**Assignee**: unassigned
**Priority**: high

### Description
Update Macro Stream (EventStreamScreen) to show AI confidence and enhanced impact ratings.

### Acceptance Criteria
- [ ] Observe `AIContextService.contextState` in EventStreamScreen
- [ ] Enhance calendar events with AI decisions
- [ ] Display AI confidence in event cards
- [ ] Show AI direction (LONG/SHORT/NEUTRAL) as badge
- [ ] Update confidence meter to use AI data
- [ ] Add "AI Enhanced" indicator when AI data is available
- [ ] Handle events without AI data gracefully
- [ ] Update event card colors based on AI confidence

### Files to Modify
- `app/src/main/java/com/asc/markets/ui/screens/EventStreamScreen.kt`

### UI Changes
```kotlin
// Enhance events with AI
val aiContext by AIContextService.contextState.collectAsState()

val eventsWithAI = events.map { event ->
    val aiDecision = aiContext.decisions[event.assets_affected.firstOrNull()]
    event.copy(
        aiConfidence = aiDecision?.confidence,
        aiDirection = aiDecision?.direction,
        aiScore = aiDecision?.score
    )
}

// Display in card
EventStreamCard(
    event = event,
    aiConfidence = event.aiConfidence,  // Show in confidence meter
    aiDirection = event.aiDirection,    // Show as badge
    showAIIndicator = event.aiConfidence != null
)
```

### Dependencies
- Task 4 must be completed
- Task 5 must be completed

### Testing
1. Open Macro Stream
2. Verify events show AI confidence percentages
3. Verify AI direction badges appear (LONG/SHORT)
4. Verify confidence meter uses AI data
5. Verify "AI Enhanced" indicator appears

---

## Task 8: Add Error Handling and Logging
**Status**: pending
**Assignee**: unassigned
**Priority**: medium

### Description
Implement comprehensive error handling, logging, and graceful degradation.

### Acceptance Criteria
- [ ] Log all network requests with timestamps
- [ ] Log successful polls with data summary
- [ ] Log errors with full exception details
- [ ] Update `isConnected` flag on connection failures
- [ ] Preserve existing data when polls fail
- [ ] Add `errorMessage` to state on failures
- [ ] Display connection status in UI (optional indicator)
- [ ] Add retry logic (continuous polling, no backoff)
- [ ] Handle JSON parsing errors gracefully
- [ ] Handle timeout errors gracefully

### Files to Modify
- `app/src/main/java/com/asc/markets/ai/AIContextService.kt`
- `app/src/main/java/com/asc/markets/ui/screens/dashboard/MarketOverviewTab.kt` (optional status indicator)

### Logging Examples
```kotlin
Log.d("AIContextService", "Starting AI Context Service")
Log.d("AIContextService", "Polling AI backend at $AI_BASE_URL")
Log.i("AIContextService", "Fetched ${decisions.size} decisions, ${news.size} news items")
Log.w("AIContextService", "AI backend unreachable, using cached data")
Log.e("AIContextService", "Failed to parse AI response", exception)
```

### Dependencies
- Task 3 must be completed

### Testing
1. Start app with AI backend running → Verify success logs
2. Stop AI backend → Verify error logs and cached data preserved
3. Restart AI backend → Verify service recovers automatically
4. Send malformed JSON → Verify parsing error logged

---

## Task 9: Add Symbol Normalization Utility
**Status**: pending
**Assignee**: unassigned
**Priority**: medium

### Description
Create utility to normalize asset symbols for consistent matching across different formats.

### Acceptance Criteria
- [ ] Create `SymbolNormalizer` object
- [ ] Implement `normalize(symbol: String): String` function
- [ ] Handle variations: EUR/USD, EURUSD, EUR-USD → EURUSD
- [ ] Handle crypto: BTC/USDT, BTCUSDT → BTCUSDT
- [ ] Handle stocks: NVDA, NVDA.US → NVDA
- [ ] Handle commodities: XAU/USD, XAUUSD → XAUUSD
- [ ] Add unit tests for all variations
- [ ] Use in AIContextService getters
- [ ] Use in Market Watch asset matching

### Files to Create
- `app/src/main/java/com/asc/markets/ai/SymbolNormalizer.kt`

### Files to Modify
- `app/src/main/java/com/asc/markets/ai/AIContextService.kt`

### Implementation
```kotlin
object SymbolNormalizer {
    fun normalize(symbol: String): String {
        return symbol
            .replace("/", "")
            .replace("-", "")
            .replace(" ", "")
            .uppercase()
            .trim()
    }
    
    fun matches(symbol1: String, symbol2: String): Boolean {
        return normalize(symbol1) == normalize(symbol2)
    }
}
```

### Dependencies
None (can be done in parallel)

### Testing
```kotlin
assert(SymbolNormalizer.normalize("EUR/USD") == "EURUSD")
assert(SymbolNormalizer.normalize("BTC/USDT") == "BTCUSDT")
assert(SymbolNormalizer.matches("EUR/USD", "EURUSD"))
```

---

## Task 10: Testing and Verification
**Status**: pending
**Assignee**: unassigned
**Priority**: high

### Description
Comprehensive testing of the entire AI Context Service integration.

### Acceptance Criteria
- [ ] Unit tests for data model parsing
- [ ] Unit tests for symbol normalization
- [ ] Integration test: Service starts and polls successfully
- [ ] Integration test: Service handles network errors
- [ ] Integration test: Market Watch displays AI data
- [ ] Integration test: Macro Stream displays AI data
- [ ] Manual test: Full app flow with AI backend running
- [ ] Manual test: Full app flow with AI backend offline
- [ ] Performance test: Memory usage < 25KB
- [ ] Performance test: Network usage < 20MB/day

### Test Scenarios

#### Scenario 1: Happy Path
1. Start app with AI backend running
2. Wait 5 seconds
3. Open Market Watch → Verify news shows proper impact ratings
4. Open Macro Stream → Verify events show AI confidence
5. Wait 30 seconds → Verify data refreshes

#### Scenario 2: AI Backend Offline
1. Stop AI backend
2. Start app
3. Verify app doesn't crash
4. Verify Market Watch shows cached/fallback data
5. Verify connection status indicator shows "Offline"
6. Start AI backend
7. Wait 30 seconds → Verify service recovers

#### Scenario 3: Network Timeout
1. Configure AI backend with 20-second delay
2. Start app
3. Verify request times out after 10 seconds
4. Verify error is logged
5. Verify app continues functioning

#### Scenario 4: Malformed Response
1. Configure AI backend to return invalid JSON
2. Start app
3. Verify parsing error is logged
4. Verify app doesn't crash
5. Verify cached data is preserved

### Dependencies
- All previous tasks must be completed

### Success Criteria
- ✅ All unit tests pass
- ✅ All integration tests pass
- ✅ All manual test scenarios pass
- ✅ No crashes or ANRs
- ✅ Memory usage within limits
- ✅ Network usage within limits

---

## Task 11: Documentation and Cleanup
**Status**: pending
**Assignee**: unassigned
**Priority**: low

### Description
Add documentation, code comments, and create usage guide for future developers.

### Acceptance Criteria
- [ ] Add KDoc comments to all public methods
- [ ] Add inline comments for complex logic
- [ ] Create `AI_CONTEXT_SERVICE_USAGE.md` guide
- [ ] Document AI backend API contract
- [ ] Document data model mappings
- [ ] Add example code snippets for common use cases
- [ ] Update `AI_INTEGRATION_FINAL_SUMMARY.md` with new service
- [ ] Remove any debug/test code
- [ ] Format code consistently

### Files to Create
- `AI_CONTEXT_SERVICE_USAGE.md`

### Files to Modify
- `AI_INTEGRATION_FINAL_SUMMARY.md`
- All files in `com.asc.markets.ai` package

### Documentation Sections
1. Overview
2. Quick Start
3. Observing AI Context in Composables
4. Synchronous Data Access
5. Error Handling
6. Configuration
7. Troubleshooting
8. API Reference

### Dependencies
- All previous tasks must be completed

---

## Summary

**Total Tasks**: 11
**Estimated Time**: 6-8 hours
**Priority Breakdown**:
- High: 7 tasks
- Medium: 3 tasks
- Low: 1 task

**Critical Path**:
1. Task 1 → Task 2 → Task 3 → Task 4 → Task 5
2. Task 5 → Task 6 (Market Watch)
3. Task 5 → Task 7 (Macro Stream)
4. Task 9 (Symbol Normalizer) can be done in parallel
5. Task 8 (Error Handling) after Task 3
6. Task 10 (Testing) after all implementation tasks
7. Task 11 (Documentation) last

**Recommended Order**:
1. Task 1 (Core Structure)
2. Task 9 (Symbol Normalizer) - parallel
3. Task 2 (HTTP & Polling)
4. Task 3 (JSON Parsing)
5. Task 8 (Error Handling)
6. Task 4 (Synchronous Getters)
7. Task 5 (TradingApp Integration)
8. Task 6 (Market Watch Integration)
9. Task 7 (Macro Stream Integration)
10. Task 10 (Testing)
11. Task 11 (Documentation)
