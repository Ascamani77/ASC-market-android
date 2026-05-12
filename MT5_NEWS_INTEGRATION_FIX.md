# MT5 FXStreet News Integration - Compilation Fix

## Problem
Compilation errors when integrating MT5 FXStreet news into the Research page:
1. `NewsViewModel.kt:70:17` - Missing `author` and `content` parameters
2. `TradingApp.kt:296:17` - Unresolved reference 'viewModel'

## Root Cause
`TradingApp()` is a Composable function that doesn't have access to `ForexViewModel`. The original approach tried to call `viewModel.updateMt5News()` directly from TradingApp, which doesn't work because:
- TradingApp doesn't receive ForexViewModel as a parameter
- TradingApp is called from multiple places without ViewModel context

## Solution: Singleton Store Pattern

Created `Mt5NewsStore` - a singleton object that acts as a bridge between TradingApp and NewsViewModel.

### Architecture Flow
```
MT5 Bridge (Python)
    ↓
Mt5Service.onNewsUpdate
    ↓
Mt5NewsStore.updateNews() [Singleton]
    ↓
NewsViewModel observes Mt5NewsStore.newsItems
    ↓
Converts to NewsArticle format
    ↓
Merges with existing articles
    ↓
Research page displays all news
```

## Files Changed

### 1. Created: `Mt5NewsStore.kt`
- Singleton object with StateFlow for news items
- Provides `updateNews()` and `clear()` methods
- Thread-safe storage accessible from anywhere

### 2. Fixed: `TradingApp.kt` (line 296)
**Before:**
```kotlin
onNewsUpdate = { newsPayload ->
    viewModel.updateMt5News(newsPayload.items) // ERROR: viewModel not available
    android.util.Log.d("TradingApp", "Received ${newsPayload.items.size} news items from MT5")
}
```

**After:**
```kotlin
onNewsUpdate = { newsPayload ->
    com.trading.app.data.Mt5NewsStore.updateNews(newsPayload.items)
    android.util.Log.d("TradingApp", "Received ${newsPayload.items.size} news items from MT5")
}
```

### 3. Fixed: `NewsViewModel.kt`
**Changes:**
- Removed `_mt5NewsItems` StateFlow (no longer needed)
- Added observer in `init` block to collect from `Mt5NewsStore.newsItems`
- Made `addMt5News()` private (only called internally)
- Automatically converts MT5 NewsItem to NewsArticle format

**New init block:**
```kotlin
init {
    fetchAllNews()
    
    // Observe MT5 news from singleton store
    viewModelScope.launch {
        com.trading.app.data.Mt5NewsStore.newsItems.collect { mt5Items ->
            if (mt5Items.isNotEmpty()) {
                addMt5News(mt5Items)
            }
        }
    }
}
```

### 4. Fixed: `MainScreen.kt`
**Removed:**
```kotlin
// Add MT5 news when available
LaunchedEffect(mt5NewsItems) {
    if (mt5NewsItems.isNotEmpty()) {
        viewModel.addMt5News(mt5NewsItems)
    }
}
```

**Reason:** NewsViewModel now automatically observes Mt5NewsStore, so manual triggering is unnecessary.

## Benefits of This Approach

1. **Decoupling**: TradingApp doesn't need ViewModel access
2. **Simplicity**: Single source of truth for MT5 news
3. **Reactive**: Automatic updates via StateFlow
4. **Testable**: Easy to mock Mt5NewsStore in tests
5. **Scalable**: Other components can observe the same store if needed

## Testing

Build completed successfully:
```
BUILD SUCCESSFUL in 12m 4s
37 actionable tasks: 6 executed, 31 up-to-date
```

## Next Steps

1. Test MT5 news integration on device
2. Verify FXStreet news appears in Research page
3. Confirm news merges correctly with existing articles
4. Check for duplicate news items (handled by `distinctBy { it.id }`)

## Data Flow Example

When MT5 Bridge sends news:
```json
{
  "items": [
    {
      "id": "12345",
      "title": "EUR/USD Technical Analysis",
      "category": "Technical Analysis",
      "countryCode": "EU",
      "isoDateTime": "2026-05-09T10:30:00Z",
      "detailsUrl": "https://fxstreet.com/..."
    }
  ]
}
```

Converted to NewsArticle:
```kotlin
NewsArticle(
    id = "mt5_12345",
    title = "EUR/USD Technical Analysis",
    summary = "Technical Analysis - EU",
    content = "EUR/USD Technical Analysis",
    author = "FXStreet",
    publishedAt = "2026-05-09T10:30:00Z",
    source = "FXStreet (MT5)",
    category = "fxstreet",
    url = "https://fxstreet.com/...",
    imageUrl = null,
    intelligence = null,
    savedContent = null
)
```

## Notes

- MT5 news items are prefixed with "mt5_" to avoid ID conflicts
- News is categorized as "fxstreet" for filtering
- Source is labeled "FXStreet (MT5)" to distinguish from other sources
- Duplicate detection uses `distinctBy { it.id }` when merging
