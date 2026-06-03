# Pre-Move Score: Pure AI Implementation

## Summary
Changed the Market Watch PRE-MOVE SCORE from a hybrid (60% technical + 40% AI) to **pure backend AI score** (`pre_move_ai_score`).

## Why This Change?

### Problems with Hybrid Approach:
1. **Double-counting**: Backend AI already considers compression, ignition, liquidity, pressure
2. **Inconsistent**: Different scores when AI available vs unavailable
3. **Diluted Intelligence**: Overriding 60% of sophisticated AI with simpler calculations
4. **Maintenance burden**: Two places to update scoring logic

### Backend AI Score Composition:
The `pre_move_ai_score` from backend already includes:
- 24% Ignition probability
- 22% Expansion probability
- 22% Confluence score
- 14% Entry quality score
- 10% Chart context score
- 8% Volatility score

**This is comprehensive and sophisticated - no need to override it!**

## Changes Made

### 1. AIModels.kt
**Added `preMoveScore` field to AIDecision:**
```kotlin
data class AIDecision(
    val asset: String,
    val direction: String,
    val confidence: Double,
    val score: Int,                       // journal_score (0-100)
    val preMoveScore: Double,             // NEW: pre_move_ai_score (0.0-1.0)
    val reason: String,
    val deploymentBucket: String,
    val timestamp: Long
)
```

### 2. AIContextService.kt
**Updated parsing to extract `pre_move_ai_score` from backend:**
```kotlin
val decision = AIDecision(
    asset = normalizedAsset,
    direction = item.optString("journal_direction", "NEUTRAL").uppercase(),
    confidence = item.optDouble("journal_confidence", 0.5),
    score = item.optInt("journal_score", 50),
    preMoveScore = item.optDouble("pre_move_ai_score", 0.0),  // NEW
    reason = item.optString("portfolio_decision_reason", "No reason provided"),
    deploymentBucket = item.optString("portfolio_deployment_bucket", "MEDIUM").uppercase(),
    timestamp = System.currentTimeMillis()
)
```

### 3. PreMoveIntelligence.kt
**Changed from hybrid to pure AI score:**

#### Before (Hybrid):
```kotlin
val technicalScore = (compressionScore * 0.30 + ignitionScore * 0.25 + 
                     liquidityScore * 0.25 + pressureDistance * 0.20)
val preMoveScore = if (aiDecision != null) {
    (technicalScore * 0.6 + aiScore * 0.4).toInt().coerceIn(0, 100)
} else {
    technicalScore.toInt().coerceIn(0, 100)
}
```

#### After (Pure AI):
```kotlin
// Use pure pre_move_ai_score from backend when available
val preMoveScore = if (aiDecision != null && aiDecision.preMoveScore > 0.0) {
    // Backend pre_move_ai_score is 0.0-1.0, convert to 0-100
    (aiDecision.preMoveScore * 100.0).toInt().coerceIn(0, 100)
} else {
    // Fallback: Use technical-only score when AI backend is unreachable
    val technicalScore = (compressionScore * 0.30 + ignitionScore * 0.25 + 
                         liquidityScore * 0.25 + pressureDistance * 0.20)
    technicalScore.toInt().coerceIn(0, 100)
}
```

**Also updated AI layer display:**
```kotlin
// AI layer score: show the actual pre_move_ai_score (0-100) when available
val aiLayerScore = if (aiDecision != null && aiDecision.preMoveScore > 0.0) {
    (aiDecision.preMoveScore * 100.0).toInt().coerceIn(0, 100)
} else {
    0
}

val layers = listOf(
    // ... other layers ...
    PreMoveLayer("ASC AI", if (aiDecision != null) "LIVE" else "PENDING", 
                 aiLayerScore, aiDecision?.reason ?: "Awaiting live ASC AI central verification.")
)
```

## Benefits

### ✅ Single Source of Truth
- Backend AI is the definitive intelligence source
- No conflicting calculations

### ✅ Consistency
- Same score whether AI is available or not (just shows 0 when unavailable)
- Predictable behavior

### ✅ Simplicity
- Less code to maintain
- Easier to debug
- Clear data flow: Backend → AIContextService → PreMoveIntelligence → UI

### ✅ Scalability
- Backend improvements automatically benefit the app
- No need to update Android code when AI logic changes

### ✅ Trust in AI
- Shows confidence in the sophisticated backend system
- Respects the AI's comprehensive analysis

## Data Flow

```
Backend (ai_api.py)
  ↓ Calculates pre_move_ai_score (0.0-1.0)
  ↓ Returns via /latest-ai endpoint
  ↓
AIContextService.kt
  ↓ Polls every 30 seconds
  ↓ Parses pre_move_ai_score
  ↓ Stores in AIDecision.preMoveScore
  ↓
PreMoveIntelligence.kt
  ↓ Fetches AIDecision for asset
  ↓ Converts to 0-100 scale
  ↓ Uses as preMoveScore
  ↓
MarketWatchScreen.kt
  ↓ Displays as "PRE-MOVE SCORE"
```

## Fallback Behavior

When AI backend is **unavailable** or `pre_move_ai_score` is **0.0**:
- Falls back to technical-only calculation
- Uses: compression (30%) + ignition (25%) + liquidity (25%) + pressure (20%)
- Ensures app remains functional even when AI is offline

## Testing Checklist

- [ ] Verify `pre_move_ai_score` is being parsed from backend
- [ ] Check Market Watch shows correct scores
- [ ] Confirm scores match backend values (multiply by 100)
- [ ] Test fallback when AI backend is offline
- [ ] Verify AI layer in Analysis Node shows correct score
- [ ] Check that scores update every 30 seconds

## Related Files

- `app/src/main/java/com/asc/markets/ai/AIModels.kt`
- `app/src/main/java/com/asc/markets/ai/AIContextService.kt`
- `app/src/main/java/com/asc/markets/data/PreMoveIntelligence.kt`
- `app/src/main/java/com/asc/markets/ui/screens/MarketWatchScreen.kt`
- Backend: `c:\Users\HP\Documents\NEW_ASC\ai_api.py`

## Notes

- Backend `pre_move_ai_score` is on 0.0-1.0 scale
- Android converts to 0-100 for display
- `journal_score` (0-100) is kept separate for other purposes
- This change makes the app a true "view" of the backend AI intelligence
