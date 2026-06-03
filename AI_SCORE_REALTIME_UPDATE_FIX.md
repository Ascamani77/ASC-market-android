# AI Pre-Move Score Real-Time Update Fix

## Problem
The AI pre-move scores were staying static and not updating in real-time because:

1. **No Polling Mechanism**: AI deployments were only fetched once during app initialization
2. **No Continuous Updates**: There was no mechanism to periodically refresh the AI decision data
3. **Static Data Flow**: The `aiDecisions` StateFlow only updated when `fetchLatestDeployments()` was explicitly called

## Root Cause
In `ForexViewModel.kt`, the AI deployments were fetched only once:
```kotlin
// Initial AI deployments fetch
aiRepository.fetchLatestDeployments()
```

After this single fetch, the data remained static until the app was restarted or a manual refresh was triggered.

## Solution Implemented

### 1. Added Periodic Polling
Added a continuous polling mechanism that fetches AI deployments every 5 seconds:

```kotlin
// Start periodic AI deployments polling for real-time updates
viewModelScope.launch(Dispatchers.IO) {
    while (isActive) {
        delay(AI_DEPLOYMENTS_POLL_INTERVAL_MS) // Poll every 5 seconds
        try {
            aiRepository.fetchLatestDeployments()
        } catch (e: Exception) {
            android.util.Log.e("ForexViewModel", "Error fetching AI deployments: ${e.message}")
        }
    }
}
```

### 2. Configurable Poll Interval
Added a constant for easy adjustment of the polling frequency:

```kotlin
private const val AI_DEPLOYMENTS_POLL_INTERVAL_MS = 5_000L // Poll AI deployments every 5 seconds
```

## How It Works

1. **Initial Fetch**: On app startup, AI deployments are fetched immediately
2. **Continuous Polling**: A background coroutine polls the AI API every 5 seconds
3. **Automatic Updates**: When new data arrives, the `aiDeployments` StateFlow emits the update
4. **UI Reactivity**: The `AiStatusTab` and `PreMoveAiMock` composables automatically recompose with new scores
5. **Error Handling**: Network errors are logged but don't crash the polling loop

## Benefits

✅ **Real-Time Updates**: AI scores now update every 5 seconds automatically  
✅ **No Manual Refresh**: Users don't need to manually refresh to see new scores  
✅ **Smooth UX**: Updates happen seamlessly in the background  
✅ **Error Resilient**: Network failures don't stop the polling mechanism  
✅ **Configurable**: Easy to adjust polling frequency if needed  

## Performance Considerations

- **Network Usage**: Polls every 5 seconds (720 requests/hour)
- **Battery Impact**: Minimal - lightweight API calls on IO dispatcher
- **Memory**: No memory leaks - coroutine is scoped to ViewModel lifecycle

## Adjusting Poll Frequency

To change the update frequency, modify the constant in `ForexViewModel.kt`:

```kotlin
// For faster updates (every 2 seconds)
private const val AI_DEPLOYMENTS_POLL_INTERVAL_MS = 2_000L

// For slower updates (every 10 seconds)
private const val AI_DEPLOYMENTS_POLL_INTERVAL_MS = 10_000L

// For very slow updates (every 30 seconds)
private const val AI_DEPLOYMENTS_POLL_INTERVAL_MS = 30_000L
```

## Alternative Approaches (Future Enhancements)

1. **WebSocket Connection**: For true real-time updates with lower overhead
2. **Server-Sent Events (SSE)**: Push-based updates from the server
3. **Adaptive Polling**: Adjust frequency based on market volatility
4. **Background WorkManager**: For updates even when app is in background

## Testing

To verify the fix is working:

1. Open the AI Status tab
2. Watch the pre-move scores - they should update every 5 seconds
3. Check logcat for "Error fetching AI deployments" if there are network issues
4. Verify scores change as the AI backend processes new market data

## Files Modified

- `app/src/main/java/com/asc/markets/logic/ForexViewModel.kt`
  - Added `AI_DEPLOYMENTS_POLL_INTERVAL_MS` constant
  - Added periodic polling coroutine in initialization block

## Date
May 24, 2026
