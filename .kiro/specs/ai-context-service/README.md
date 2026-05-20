# AI Context Service Spec

## Overview

This spec defines the implementation of a **centralized AI Context Service** that provides real-time AI intelligence to all pages in the application.

## Problem

Currently:
- Market Watch shows news with hardcoded "LOW" impact ratings
- Macro Stream displays all events as "LOW" impact
- No centralized source of AI decisions
- Each page would need to independently fetch AI data (inefficient)

## Solution

Create a singleton `AIContextService` that:
- Polls AI backend every 30 seconds
- Caches AI decisions and news impacts
- Exposes StateFlow for reactive updates
- Provides synchronous getters for immediate access
- Handles errors gracefully (no crashes if AI is offline)

## Architecture

```
AI Backend (Python) 
  ↓ HTTP (30s polling)
AIContextService (Kotlin Singleton)
  ↓ StateFlow
Market Watch | Macro Stream | AI Chat | Other Pages
```

## Key Features

1. **Single Source of Truth**: All pages consume the same AI data
2. **Reactive Updates**: StateFlow automatically updates UI when data changes
3. **Graceful Degradation**: App works even if AI backend is offline
4. **Low Overhead**: ~21KB memory, ~14MB/day network usage
5. **Simple Integration**: Just observe `AIContextService.contextState`

## Files

- **design.md**: Detailed architecture, data models, and integration points
- **tasks.md**: 11 implementation tasks with acceptance criteria
- **README.md**: This file (overview and quick reference)

## Quick Start (After Implementation)

### Observe AI Context in Composable

```kotlin
@Composable
fun MyScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    // Use AI decisions
    val decision = aiContext.decisions["EURUSD"]
    Text("Direction: ${decision?.direction}, Confidence: ${decision?.confidence}")
    
    // Use news impacts
    aiContext.newsImpacts.forEach { news ->
        NewsCard(
            headline = news.headline,
            impact = news.impact,  // HIGH/MEDIUM/LOW
            color = when(news.impact) {
                ImpactLevel.HIGH -> RoseError
                ImpactLevel.MEDIUM -> Color.Yellow
                ImpactLevel.LOW -> Color.Gray
            }
        )
    }
}
```

### Synchronous Access

```kotlin
// Get decision for specific asset
val decision = AIContextService.getDecisionForAsset("EUR/USD")
if (decision != null) {
    println("${decision.asset}: ${decision.direction} (${decision.confidence})")
}

// Get all decisions
val allDecisions = AIContextService.getAllDecisions()
println("Total assets: ${allDecisions.size}")
```

## Data Models

### AIDecision
- `asset`: String (e.g., "EURUSD")
- `direction`: String ("LONG", "SHORT", "NEUTRAL")
- `confidence`: Double (0.0 to 1.0)
- `score`: Int (0 to 100)
- `reason`: String (explanation)
- `deploymentBucket`: String ("HIGH", "MEDIUM", "LOW")
- `timestamp`: Long

### NewsImpact
- `headline`: String
- `source`: String
- `timestamp`: String ("8h ago")
- `assetType`: String ("forex", "crypto", etc.)
- `assetSymbol`: String ("EUR/USD")
- `impact`: ImpactLevel (HIGH/MEDIUM/LOW)
- `affectedAssets`: List<String>
- `aiConfidence`: Double (0.0 to 1.0)

### AIContextState
- `decisions`: Map<String, AIDecision>
- `newsImpacts`: List<NewsImpact>
- `lastUpdated`: Long
- `isConnected`: Boolean
- `errorMessage`: String?

## Implementation Tasks

1. ✅ **Task 1**: Create core service structure (data models, StateFlow)
2. ✅ **Task 2**: Implement HTTP client and polling mechanism
3. ✅ **Task 3**: Implement JSON parsing
4. ✅ **Task 4**: Add synchronous getters
5. ✅ **Task 5**: Integrate with TradingApp (start/stop service)
6. ✅ **Task 6**: Integrate with Market Watch (news impact ratings)
7. ✅ **Task 7**: Integrate with Macro Stream (AI confidence)
8. ✅ **Task 8**: Add error handling and logging
9. ✅ **Task 9**: Add symbol normalization utility
10. ✅ **Task 10**: Testing and verification
11. ✅ **Task 11**: Documentation and cleanup

**Estimated Time**: 6-8 hours

## Integration Points

### Market Watch (MarketOverviewTab.kt)
- Replace mock news with `aiContext.newsImpacts`
- Display proper impact colors (HIGH=Red, MEDIUM=Yellow, LOW=Gray)
- Show AI confidence percentages

### Macro Stream (EventStreamScreen.kt)
- Enhance calendar events with AI decisions
- Display AI confidence in event cards
- Show AI direction badges (LONG/SHORT/NEUTRAL)

### AI Chat (Future)
- AI can reference what other pages show
- Provide context from current decisions and news

## Configuration

### AI Backend URL
- **Development**: `http://10.164.138.133:8000`
- **Production**: Configure in BuildConfig

### Poll Interval
- **Default**: 30 seconds
- **Configurable**: Via BuildConfig or runtime settings

### Timeout
- **Default**: 10 seconds
- **Configurable**: Via OkHttpClient builder

## Error Handling

- **Network Failure**: Service continues polling, preserves cached data
- **Timeout**: Logged, next poll in 30 seconds
- **Malformed JSON**: Logged, cached data preserved
- **AI Backend Offline**: App works with cached/fallback data

## Performance

- **Memory**: ~21KB (negligible)
- **Network**: ~14MB/day (5KB every 30 seconds)
- **CPU**: Minimal (background coroutine)
- **Battery**: Negligible impact

## Testing Strategy

### Unit Tests
- Data model parsing
- Symbol normalization
- Error handling

### Integration Tests
- Service lifecycle (start/stop)
- Network requests
- StateFlow updates

### Manual Tests
- Happy path (AI backend online)
- Error path (AI backend offline)
- Recovery (AI backend comes back online)

## Success Criteria

- ✅ Service starts automatically on app launch
- ✅ Polls AI backend every 30 seconds
- ✅ Market Watch shows proper news impact ratings
- ✅ Macro Stream shows AI confidence scores
- ✅ No crashes when AI backend is offline
- ✅ < 25KB memory footprint
- ✅ < 20MB/day network usage

## Future Enhancements (Phase 2)

- WebSocket support for real-time updates (< 1s latency)
- Disk caching for offline support
- User-configurable poll interval
- Historical AI decision tracking
- Predictive caching
- Asset correlation analysis

## Related Documentation

- `AI_INTEGRATION_FINAL_SUMMARY.md` - Overall AI integration status
- `AI_DATA_SOURCE_CLARIFICATION.md` - Asset breakdown and data sources
- `AI_PEPPERSTONE_INTEGRATION_COMPLETE.md` - Pepperstone integration details

## Questions?

See `design.md` for detailed architecture and `tasks.md` for implementation steps.

---

**Spec Status**: ✅ Ready for Implementation
**Created**: 2026-05-16
**Last Updated**: 2026-05-16
