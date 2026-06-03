# News Layout Fix - Timestamp on New Line

## Issue
The timestamp was appearing on the same line as the metadata chips (Impact, Source, Event Type, Confidence), causing layout crowding.

## What Was Fixed

### Before
```
[Impact 67%] [CNBC.COM] [MAJOR-DATA-RELEASE] [MEDIUM]  20 May 20:04
```
All items on one line, timestamp at the end.

### After
```
[Impact 67%] [CNBC.COM] [MAJOR-DATA-RELEASE] [MEDIUM]
20 May 20:04
```
Timestamp moved to its own line below the chips.

## Changes Made

Updated `AnalysisOpinionScreen.kt`:

1. **FeaturedInsightCard** (Top narratives section)
   - Removed `Box` wrapper with `Alignment.BottomEnd`
   - Changed to vertical layout with `Spacer`
   - Timestamp now appears on new line

2. **InsightArticleCard** (Broader coverage section)
   - Same changes as FeaturedInsightCard
   - Consistent layout across both card types

## Code Changes

**Before:**
```kotlin
Box(modifier = Modifier.fillMaxWidth()) {
    FlowRow(...) {
        // chips
    }
    Text(
        text = formatArticleTime(article.publishedAt),
        modifier = Modifier.align(Alignment.BottomEnd)  // ← On same line
    )
}
```

**After:**
```kotlin
FlowRow(...) {
    // chips
}
Spacer(modifier = Modifier.height(8.dp))
Text(
    text = formatArticleTime(article.publishedAt)  // ← On new line
)
```

## Visual Result

The timestamp now appears on its own line with proper spacing:
- 8dp space between chips and timestamp
- Cleaner, less crowded layout
- Better readability
- Consistent across all news cards

## Files Modified
- `app/src/main/java/com/researchcenter/ui/screens/AnalysisOpinionScreen.kt`

## Next Steps
Rebuild the app to see the layout fix in action!

---

✅ **Status**: Fixed and verified (no compilation errors)
