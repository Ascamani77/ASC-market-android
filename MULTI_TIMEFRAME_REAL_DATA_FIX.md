# Multi-Timeframe Analysis - Real Data & Expandable Assets Fix

## Issues Fixed

### 1. ✅ "Show More Assets" Button Not Working
**Problem**: Clicking "Show 17 more assets" did nothing

**Solution**: 
- Added `showAllAssets` state variable with `remember { mutableStateOf(false) }`
- Made the button clickable with proper state toggle
- Button now expands/collapses the asset list
- Shows "Show less" when expanded

**Implementation**:
```kotlin
var showAllAssets by remember { mutableStateOf(false) }
val displayPairs = if (showAllAssets) allPairs else allPairs.take(10)
```

### 2. ✅ Using Real Backend Data Instead of Simulated
**Problem**: Timeframe scores were using random multipliers (simulated data)

**Solution**:
- Removed all `kotlin.random.Random` multipliers
- Now uses ACTUAL backend AI scores directly
- No simulation or variation - pure backend data
- All timeframes show the same backend analysis (H1 data)

**Before** (Simulated):
```kotlin
val tfMultiplier = when(tf) {
    "M15" -> 0.7 + (kotlin.random.Random.nextDouble() * 0.3)
    "M30" -> 0.8 + (kotlin.random.Random.nextDouble() * 0.2)
    // ... random variations
}
val preMoveScore = (basePreMove * tfMultiplier).toInt()
```

**After** (Real Data):
```kotlin
// Use actual backend scores (NO SIMULATION)
val preMoveScore = basePreMove
val compressionScore = baseCompression
val ignitionScore = baseIgnition
```

## Data Source

### Current Implementation:
- **Backend**: ASC AI `/latest-deployments` endpoint
- **Scores**: Real `pre_move_ai_score`, `ignition_probability`, `expansion_probability`
- **Bias**: Real `journal_direction` from backend
- **Timeframe**: Backend provides H1 analysis

### Display Logic:
- All timeframes (M15, M30, H1, H4, D1) show the **same backend data**
- Regime label indicates data source:
  - `"Expansion candidate (Backend)"` - H1 timeframe (matches backend)
  - `"Expansion candidate (H1 data)"` - Other timeframes (using H1 backend data)

### Future Enhancement:
When backend provides per-timeframe analysis:
- Each timeframe will show its own unique scores
- Backend should return separate analysis for M15, M30, H1, H4, D1
- No code changes needed - just backend API enhancement

## What's Real vs What's Not

### ✅ REAL DATA (From Backend):
- Pre-Move Score (0-100%)
- Compression Score (0-100%)
- Ignition Score (0-100%)
- Direction Bias (BULLISH/BEARISH/NEUTRAL)
- State (ARMED/WATCH/COMPRESSING/FILTERING)
- Regime (Expansion candidate, Compression, etc.)

### ⚠️ LIMITATION:
- Backend currently provides only H1 timeframe analysis
- All timeframes display the same H1 data
- This is clearly labeled in the UI: "(H1 data)" or "(Backend)"

## User Experience

### Asset Selector:
1. Shows first 10 assets by default
2. Click "Show X more assets" to expand full list
3. Click "Show less" to collapse back to 10
4. All assets are clickable to switch analysis

### Timeframe Cards:
1. Each card shows REAL backend scores
2. Regime label indicates data source
3. No random variations or simulations
4. Consistent scores across all timeframes (until backend provides per-TF data)

## Files Modified
- `app/src/main/java/com/asc/markets/ui/screens/MultiTimeframeAnalysisScreen.kt`
  - Added `showAllAssets` state for expandable asset list
  - Removed random multipliers from `buildTimeframeAnalyses`
  - Added backend data source labels to regime display
  - Made "Show more" button clickable with proper state management

## Testing
✅ Click "Show X more assets" - expands list  
✅ Click "Show less" - collapses list  
✅ All scores match backend data exactly  
✅ No random variations between page refreshes  
✅ Regime labels indicate data source clearly  

## Date
May 31, 2026
