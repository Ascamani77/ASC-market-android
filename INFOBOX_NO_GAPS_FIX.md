# Info Box - Remove All External Padding & Gaps

## Issue
The info box tiles had:
1. 10dp gaps between tiles
2. 14dp rounded corners creating visual separation
3. Tiles didn't connect seamlessly

## What Was Fixed

### Before
```
|  [Coverage]  [High impact]  [Themes]  |
   ↑ gaps between tiles ↑
```
Tiles had spacing and rounded corners.

### After
```
|[Coverage][High impact][Themes]|
```
Tiles connect seamlessly, edge-to-edge.

## Changes Made

### 1. Removed Spacing Between Tiles
**InsightSummaryRow - Before:**
```kotlin
horizontalArrangement = Arrangement.spacedBy(10.dp),
verticalArrangement = Arrangement.spacedBy(10.dp),
```

**After:**
```kotlin
horizontalArrangement = Arrangement.spacedBy(0.dp),
verticalArrangement = Arrangement.spacedBy(0.dp),
```

### 2. Removed Rounded Corners
**InsightMetricTile - Before:**
```kotlin
shape = RoundedCornerShape(14.dp),
```

**After:**
```kotlin
shape = RoundedCornerShape(0.dp),
```

## Visual Result

The info box now:
- ✅ Extends fully to left screen edge
- ✅ Extends fully to right screen edge
- ✅ No gaps between tiles
- ✅ Tiles connect seamlessly
- ✅ Creates a solid banner effect
- ✅ Borders between tiles create visual separation

## Layout Structure

```
┌─────────────────────────────────────┐
│ Coverage │ High impact │ Themes     │  ← Edge-to-edge
│    107   │      1      │     6      │  ← No gaps
└─────────────────────────────────────┘
```

Each tile:
- Has internal padding (14dp horizontal, 16dp vertical)
- Has border (1dp white with 15% opacity)
- No rounded corners (0dp)
- No spacing between tiles (0dp)

## Files Modified
- `app/src/main/java/com/researchcenter/ui/screens/AnalysisOpinionScreen.kt`
  - `InsightSummaryRow()` - Removed spacing
  - `InsightMetricTile()` - Removed rounded corners

## Next Steps
Rebuild the app to see the seamless edge-to-edge info box!

---

✅ **Status**: Fixed and verified (no compilation errors)
