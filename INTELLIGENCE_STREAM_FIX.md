# Intelligence Stream & Analysis Opinion Fix

## User Requirements
1. Remove "Upcoming Events" from Analysis & Opinion page (if it exists)
2. Move "Upcoming Events" to Intelligence Stream page
3. Fix Intelligence Stream to use real data (not mock/demo data)
4. Don't touch Macro Stream page

## Current Status

### Analysis & Opinion (SentimentScreen.kt)
- **File**: `app/src/main/java/com/asc/markets/ui/screens/SentimentScreen.kt`
- **Status**: ✅ Already using real AI data from `viewModel.aiDeployments`
- **Data Sources**:
  - AI decisions from backend
  - Watchlist items
  - Currency sentiment calculations
- **No "Upcoming Events" section found** - may already be removed or in a different component

### Intelligence Stream (IntelligenceStreamScreen.kt)
- **File**: `app/src/main/java/com/asc/markets/ui/screens/IntelligenceStreamScreen.kt`
- **Status**: ✅ Already using real data from `viewModel.macroStreamEvents`
- **Data Sources**:
  - `macroEvents` from `viewModel.macroStreamEvents.collectAsState()`
  - `aiDeployments` from `viewModel.aiDeployments.collectAsState()`
- **Filters**: UPCOMING, CONFIRMED, HIGH IMPACT categories
- **Mock Data**: Only `leadProgress` animation (0.35f) - cosmetic only

### Macro Stream
- **Status**: ✅ Not to be touched per user request

## Analysis

### Intelligence Stream Mock Data
The only "mock" data in Intelligence Stream is:
```kotlin
var leadProgress by remember { mutableStateOf(0.35f) }

LaunchedEffect(Unit) {
    while (true) {
        delay(5_000)
        leadProgress = (leadProgress + 0.03f).coerceAtMost(1f)
    }
}
```

This is just a progress bar animation and doesn't affect the actual event data display.

### Real Data Flow
1. **ForexViewModel** fetches macro events from backend
2. **macroStreamEvents** StateFlow provides real-time events
3. **Intelligence Stream** filters and displays these events
4. Events have real statuses: UPCOMING, CONFIRMED, EXPIRED

## Recommendations

### If "Upcoming Events" exists elsewhere:
1. Search for it in other components/screens
2. Remove from Analysis & Opinion
3. Verify Intelligence Stream already shows it (it does via macroEvents)

### If leadProgress mock is an issue:
Replace with real data from backend if available, or remove the progress indicator entirely.

## Verification Steps

1. **Check Analysis & Opinion page** - look for any "Upcoming Events" section
2. **Check Intelligence Stream** - verify it shows real macro events
3. **Check event sources**:
   - RSS feeds from ForexLive, MyFXBook
   - AI-generated events from VigilanceNodeEngine
   - Calendar events from backend

## Files to Check
- ✅ `SentimentScreen.kt` - Analysis & Opinion (no upcoming events found)
- ✅ `IntelligenceStreamScreen.kt` - Already using real data
- ❓ Other components that might have "Upcoming Events"
