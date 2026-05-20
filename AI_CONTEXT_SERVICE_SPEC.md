# AI Context Service - Spec Created ✅

## What Was Created

I've created a complete **design-first spec** for the AI Context Service feature. The spec is located in:

```
.kiro/specs/ai-context-service/
├── README.md    (Overview and quick reference)
├── design.md    (Detailed architecture and design)
└── tasks.md     (11 implementation tasks)
```

## What Is the AI Context Service?

A **centralized singleton** that provides real-time AI intelligence to all pages in your app.

### Current Problem
- Market Watch shows news with hardcoded "LOW" impact ratings
- Macro Stream displays all events as "LOW" impact
- No centralized source of AI decisions
- Each page would need to independently fetch AI data (inefficient)

### Solution
```
AI Backend (Python) 
  ↓ HTTP (polls every 30 seconds)
AIContextService (Kotlin Singleton)
  ↓ StateFlow (reactive updates)
Market Watch | Macro Stream | AI Chat | Other Pages
```

## Key Features

1. **Single Source of Truth**: All pages consume the same AI data
2. **Reactive Updates**: StateFlow automatically updates UI when data changes
3. **Graceful Degradation**: App works even if AI backend is offline
4. **Low Overhead**: ~21KB memory, ~14MB/day network usage
5. **Simple Integration**: Just observe `AIContextService.contextState`

## Architecture Highlights

### Data Models

```kotlin
// AI Decision (from /latest-ai)
data class AIDecision(
    val asset: String,              // "EURUSD"
    val direction: String,          // "LONG", "SHORT", "NEUTRAL"
    val confidence: Double,         // 0.0 to 1.0
    val score: Int,                 // 0 to 100
    val reason: String,             // Explanation
    val deploymentBucket: String,   // "HIGH", "MEDIUM", "LOW"
    val timestamp: Long
)

// News Impact (from /news-impact or /latest-ai)
data class NewsImpact(
    val headline: String,
    val source: String,
    val timestamp: String,          // "8h ago"
    val assetType: String,          // "forex", "crypto", etc.
    val assetSymbol: String,        // "EUR/USD"
    val impact: ImpactLevel,        // HIGH, MEDIUM, LOW
    val affectedAssets: List<String>,
    val aiConfidence: Double,       // 0.0 to 1.0
    val imageUrl: String = ""
)

enum class ImpactLevel {
    HIGH,    // Red - Major market-moving event
    MEDIUM,  // Yellow - Moderate impact
    LOW      // Gray - Minor/informational
}

// Overall State
data class AIContextState(
    val decisions: Map<String, AIDecision>,
    val newsImpacts: List<NewsImpact>,
    val lastUpdated: Long,
    val isConnected: Boolean,
    val errorMessage: String? = null
)
```

### Service API

```kotlin
object AIContextService {
    // Reactive state
    val contextState: StateFlow<AIContextState>
    
    // Lifecycle
    fun start()
    fun stop()
    fun refresh()
    
    // Synchronous getters
    fun getDecisionForAsset(symbol: String): AIDecision?
    fun getImpactForNews(headline: String): NewsImpact?
    fun getAllDecisions(): Map<String, AIDecision>
    fun getAllNewsImpacts(): List<NewsImpact>
}
```

### Usage Example

```kotlin
@Composable
fun MarketOverviewTab() {
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
            confidence = news.aiConfidence,
            color = when(news.impact) {
                ImpactLevel.HIGH -> RoseError
                ImpactLevel.MEDIUM -> Color(0xFFFFB300)
                ImpactLevel.LOW -> SlateText
            }
        )
    }
}
```

## Implementation Tasks (11 Total)

### High Priority (7 tasks)
1. **Task 1**: Create core service structure (data models, StateFlow)
2. **Task 2**: Implement HTTP client and polling mechanism
3. **Task 3**: Implement JSON parsing
4. **Task 5**: Integrate with TradingApp (start/stop service)
5. **Task 6**: Integrate with Market Watch (news impact ratings)
6. **Task 7**: Integrate with Macro Stream (AI confidence)
7. **Task 10**: Testing and verification

### Medium Priority (3 tasks)
8. **Task 4**: Add synchronous getters
9. **Task 8**: Add error handling and logging
10. **Task 9**: Add symbol normalization utility

### Low Priority (1 task)
11. **Task 11**: Documentation and cleanup

**Estimated Time**: 6-8 hours

## Integration Points

### 1. Market Watch (MarketOverviewTab.kt)
**Changes**:
- Replace mock news with `aiContext.newsImpacts`
- Display proper impact colors (HIGH=Red, MEDIUM=Yellow, LOW=Gray)
- Show AI confidence percentages
- Filter news by current AssetContext

**Before**:
```kotlin
val newsItems = getMockAscNews()  // All show "LOW" impact
```

**After**:
```kotlin
val aiContext by AIContextService.contextState.collectAsState()
val newsWithImpact = aiContext.newsImpacts.filter { 
    it.assetType == assetCtxForNews.name.lowercase() 
}
// Now shows proper HIGH/MEDIUM/LOW impact with colors
```

### 2. Macro Stream (EventStreamScreen.kt)
**Changes**:
- Enhance calendar events with AI decisions
- Display AI confidence in event cards
- Show AI direction badges (LONG/SHORT/NEUTRAL)
- Update confidence meter to use AI data

**Before**:
```kotlin
// Events show generic confidence
val confidence = event.confidence_score ?: 50
```

**After**:
```kotlin
val aiContext by AIContextService.contextState.collectAsState()
val aiDecision = aiContext.decisions[event.assets_affected.firstOrNull()]
val confidence = aiDecision?.confidence ?: event.confidence_score ?: 50
val direction = aiDecision?.direction  // Show as badge
```

### 3. AI Chat (Future)
**Changes**:
- AI can reference what other pages show
- Provide context from current decisions and news

```kotlin
val aiContext by AIContextService.contextState.collectAsState()
val chatContext = buildChatContext(
    currentDecisions = aiContext.decisions,
    recentNews = aiContext.newsImpacts,
    userQuery = userMessage
)
```

## Configuration

### AI Backend
- **Development**: `http://10.164.138.133:8000`
- **Production**: Configure in BuildConfig

### Polling
- **Interval**: 30 seconds (configurable)
- **Timeout**: 10 seconds
- **Retry**: Continuous (no exponential backoff)

### Endpoints
- `GET /latest-ai` - AI decisions for all 24 assets
- `GET /news-impact` - News with AI impact ratings (or parse from `/latest-ai`)

## Error Handling

### Network Failures
- Service continues polling
- Preserves cached data
- Updates `isConnected` flag to `false`
- Logs error with details

### Graceful Degradation
- If AI backend is offline:
  - Pages use last known AI decisions (cached)
  - Pages fall back to mock/default impact ratings
  - User sees connection status indicator
  - App continues functioning normally

### Recovery
- Service automatically recovers when AI backend comes back online
- Next successful poll updates all data
- No manual intervention required

## Performance

| Metric | Value |
|--------|-------|
| Memory | ~21KB (negligible) |
| Network | ~14MB/day (5KB every 30s) |
| CPU | Minimal (background coroutine) |
| Battery | Negligible impact |
| Latency | < 100ms for synchronous getters |

## Success Criteria

- ✅ Service starts automatically on app launch
- ✅ Polls AI backend every 30 seconds
- ✅ Market Watch shows proper news impact ratings (HIGH/MEDIUM/LOW)
- ✅ Macro Stream shows AI confidence scores
- ✅ No crashes when AI backend is offline
- ✅ < 25KB memory footprint
- ✅ < 20MB/day network usage
- ✅ 99% uptime (continues working even if AI backend is down)

## Next Steps

### Option 1: Review and Approve Spec
1. Review `design.md` for detailed architecture
2. Review `tasks.md` for implementation breakdown
3. Approve spec and proceed to implementation

### Option 2: Start Implementation Immediately
Since the design is complete, you can start implementing:

```bash
# Start with Task 1: Core Service Structure
# Create AIContextService.kt with data models
```

I can help you implement any or all of the 11 tasks. Just let me know if you want to:
- Review the spec first
- Start implementing immediately
- Modify the design
- Ask questions about the architecture

## Files Created

1. `.kiro/specs/ai-context-service/README.md` - Overview and quick reference
2. `.kiro/specs/ai-context-service/design.md` - Detailed architecture (50+ sections)
3. `.kiro/specs/ai-context-service/tasks.md` - 11 implementation tasks with acceptance criteria
4. `AI_CONTEXT_SERVICE_SPEC.md` - This summary document

## Related Documentation

- `AI_INTEGRATION_FINAL_SUMMARY.md` - Overall AI integration status
- `AI_DATA_SOURCE_CLARIFICATION.md` - Asset breakdown (24 assets)
- `AI_PEPPERSTONE_INTEGRATION_COMPLETE.md` - Pepperstone integration

---

**Spec Status**: ✅ Ready for Implementation
**Approach**: Design-First (as recommended)
**Estimated Time**: 6-8 hours
**Risk Level**: Low (isolated service, graceful degradation)

**What do you want to do next?**
1. Review the spec
2. Start implementing (I can do all 11 tasks)
3. Modify the design
4. Ask questions
