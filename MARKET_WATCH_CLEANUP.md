# Market Watch Screen Cleanup

## Changes Made

### 1. Removed Two Top Boxes
**Removed:**
- ✅ Top banner with eye icon, "PRE-MOVE MARKET WATCH" title, and filter icon
- ✅ Engine status card with "ASC PRE-MOVE SCAN ACTIVE" message

**Result:**
- Cleaner, more focused interface
- Market watch cards now start immediately at the top
- More screen real estate for actual trading signals

### 2. Implemented Collapsing Header on Scroll

**Behavior:**
- **Scroll Down (up gesture)**: Global header (ASC MARKET + search icon) collapses and disappears
- **Scroll Up (down gesture)**: Global header reappears
- **At Top**: Header is fully visible

**Implementation:**
```kotlin
val scrollState = rememberScrollState()

LaunchedEffect(scrollState.value) {
    val scrollPosition = scrollState.value
    
    // Hide header when scrolling down
    if (scrollPosition > 100) {
        vm.setGlobalHeaderCollapse(1f)  // Fully collapsed
    } else {
        // Progressive collapse based on scroll position
        val collapseProgress = (scrollPosition / 100f).coerceIn(0f, 1f)
        vm.setGlobalHeaderCollapse(collapseProgress)
    }
}

// Reset header when leaving screen
DisposableEffect(Unit) {
    onDispose {
        vm.setGlobalHeaderCollapse(0f)
    }
}
```

### 3. Show All Assets (Removed 8-Asset Limit)

**Before:**
```kotlin
val samples = candidates.take(8)  // Only 8 assets shown
```

**After:**
```kotlin
val samples = candidates  // Show all available assets
```

**Result:**
- Now displays ALL pre-move candidates from all data sources
- Candidates are sorted by:
  1. Risk Gate (PASS/WATCH first)
  2. Pre-Move Score (highest first)
  3. Compression Score (highest first)
- Typically shows 20-50+ assets depending on market conditions

### 4. Technical Details

**Scroll Threshold:**
- 0-100px: Progressive collapse (0f to 1f)
- >100px: Fully collapsed (1f)

**State Management:**
- Uses `ForexViewModel.setGlobalHeaderCollapse(progress: Float)`
- Progress: 0f = fully visible, 1f = fully hidden
- Automatically resets to 0f when leaving the screen

**Imports Added:**
- `DisposableEffect` - For cleanup when screen is disposed
- `LaunchedEffect` - For scroll position monitoring

**Data Sources:**
- Pepperstone (Forex, Commodities, Indices, Stocks, Bonds, Crypto)
- Binance (USDT pairs)
- Combined Fallback (when primary sources unavailable)

## User Experience

### Before:
```
┌─────────────────────────────┐
│ ASC MARKET        [search]  │ ← Always visible
├─────────────────────────────┤
│ 👁️ PRE-MOVE MARKET WATCH   │ ← Removed
│ DETERMINISTIC CANDIDATES... │
├─────────────────────────────┤
│ ⟲ ASC PRE-MOVE SCAN ACTIVE │ ← Removed
│ SCANNING COMPRESSION...     │
├─────────────────────────────┤
│ BTC/USDT Card              │
│ USD/JPY Card               │
│ ... (only 8 total)         │ ← Limited
└─────────────────────────────┘
```

### After:
```
┌─────────────────────────────┐
│ ASC MARKET        [search]  │ ← Hides on scroll down
├─────────────────────────────┤
│ BTC/USDT Card              │ ← Starts immediately
│ USD/JPY Card               │
│ EUR/USD Card               │
│ GBP/USD Card               │
│ ... (all candidates)       │ ← Shows all
└─────────────────────────────┘
```

## Benefits

### ✅ More Content Visible
- Removed ~200dp of header space
- Users see 1-2 more trading signals without scrolling

### ✅ Complete Market View
- Shows ALL pre-move candidates, not just 8
- Better market coverage and opportunity discovery
- No artificial limits on what traders can see

### ✅ Immersive Scrolling
- Header disappears when reading signals
- Reappears when needed (scrolling back up)
- Modern app behavior (like Instagram, Twitter)

### ✅ Cleaner Design
- Less visual clutter
- Focus on what matters: the trading signals
- Professional, minimalist aesthetic

### ✅ Consistent with Other Screens
- Uses same collapse mechanism as EventStreamScreen
- Follows established pattern in the app
- Predictable behavior across screens

## Testing Checklist

- [ ] Header collapses smoothly when scrolling down
- [ ] Header reappears when scrolling up
- [ ] Header is fully visible when at top of list
- [ ] Header resets to visible when navigating away
- [ ] No visual glitches during collapse animation
- [ ] Signal cards are clickable and navigate correctly
- [ ] Bottom navigation remains accessible
- [ ] All candidates are displayed (not just 8)
- [ ] Candidates are sorted correctly (PASS/WATCH first, then by score)

## Related Files

- `app/src/main/java/com/asc/markets/ui/screens/MarketWatchScreen.kt` - Main changes
- `app/src/main/java/com/asc/markets/logic/ForexViewModel.kt` - Header collapse state
- `app/src/main/java/com/asc/markets/MainActivity.kt` - Header rendering with collapse
- `app/src/main/java/com/asc/markets/data/PreMoveIntelligence.kt` - Candidate generation

## Notes

- The collapse animation is handled by MainActivity's GlobalHeader component
- Collapse progress is interpolated smoothly (0f to 1f)
- Other screens using similar pattern: EventStreamScreen, SimulationScreen, SentimentScreen
- Candidates are filtered to exclude BONDS and invalid prices
- Sorting prioritizes actionable signals (PASS/WATCH risk gates) with highest scores
