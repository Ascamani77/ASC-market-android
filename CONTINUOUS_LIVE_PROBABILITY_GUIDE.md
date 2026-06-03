# Continuous Live Probability Updates

## Two Approaches

### Approach 1: Scheduled Export (Easiest) ⭐

**How it works:**
- Export script runs every 5 minutes
- Updates JSON files automatically
- App loads updated JSON on refresh

**Pros:**
- ✅ Simple to set up
- ✅ No app changes needed
- ✅ Works with existing code

**Cons:**
- ⚠️ 5-minute delay between updates
- ⚠️ Need to rebuild app to see changes (or implement file watching)

**Setup:**

1. **Start continuous export** (in a new terminal):
```powershell
cd c:\Users\HP\Documents\NEW_ASC
.\continuous_watchlist_export.ps1
```

2. **Keep your AI running** (in original terminal)

3. **In Android app**, add a refresh button that reloads from JSON:
```kotlin
// In ForexViewModel.kt
fun refreshWatchlist() {
    _isWatchlistAnalyzing.value = true
    loadWatchlistFromAI()  // Already implemented!
    viewModelScope.launch {
        delay(1000)
        _isWatchlistAnalyzing.value = false
    }
}
```

4. **User pulls to refresh** → App reloads JSON → Gets latest probabilities

---

### Approach 2: Live API Endpoint (Real-time) 🚀

**How it works:**
- AI server exposes `/api/watchlist` endpoint
- App polls endpoint every 30 seconds
- Gets real-time probabilities

**Pros:**
- ✅ Real-time updates (30-second intervals)
- ✅ No file export needed
- ✅ Can query individual assets

**Cons:**
- ⚠️ Requires API server changes
- ⚠️ Requires Android app changes
- ⚠️ More complex setup

**Setup:**

#### Step 1: Add API Endpoint to Your AI Server

Find your AI API server file (likely `ai_api.py` or similar) and add:

```python
# Add this import at the top
from add_watchlist_api_endpoint import router as watchlist_router

# Add this line where you create your FastAPI app
app.include_router(watchlist_router)
```

Or copy the code from `add_watchlist_api_endpoint.py` directly into your API file.

#### Step 2: Test the Endpoint

```powershell
# Test in browser or curl
curl http://localhost:8000/api/watchlist

# Test single asset
curl http://localhost:8000/api/watchlist/EURUSD
```

Expected response:
```json
{
  "type": "live_watchlist",
  "timestamp": "2026-05-26T12:00:00",
  "source": "AI_SYSTEM_LIVE",
  "items": [
    {
      "asset": "EURUSD",
      "moveProbability": 55,
      "confidence": 50,
      "volatilityScore": 0,
      "status": "Ranging",
      "timestamp": "2026-05-26T12:00:00"
    }
  ]
}
```

#### Step 3: Update Android App to Poll API

Add to `ForexViewModel.kt`:

```kotlin
private val _liveWatchlistEnabled = MutableStateFlow(false)
val liveWatchlistEnabled = _liveWatchlistEnabled.asStateFlow()

private var watchlistPollingJob: Job? = null

fun enableLiveWatchlist() {
    _liveWatchlistEnabled.value = true
    startWatchlistPolling()
}

fun disableLiveWatchlist() {
    _liveWatchlistEnabled.value = false
    watchlistPollingJob?.cancel()
}

private fun startWatchlistPolling() {
    watchlistPollingJob?.cancel()
    watchlistPollingJob = viewModelScope.launch(Dispatchers.IO) {
        while (isActive && _liveWatchlistEnabled.value) {
            try {
                fetchLiveWatchlist()
            } catch (e: Exception) {
                android.util.Log.e("ForexViewModel", "Failed to fetch live watchlist", e)
            }
            delay(30_000) // Poll every 30 seconds
        }
    }
}

private suspend fun fetchLiveWatchlist() {
    try {
        val url = "http://localhost:8000/api/watchlist"
        val response = withContext(Dispatchers.IO) {
            java.net.URL(url).readText()
        }
        
        val watchlistData = parseWatchlistJson(response)
        _watchlistItems.value = watchlistData
        _lastWatchlistUpdate.value = System.currentTimeMillis()
        
        android.util.Log.i("ForexViewModel", "✅ Fetched live watchlist: ${watchlistData.size} items")
    } catch (e: Exception) {
        android.util.Log.e("ForexViewModel", "Failed to fetch live watchlist", e)
        // Fall back to JSON file
        loadWatchlistFromAI()
    }
}
```

Add toggle in Settings screen:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Live Watchlist Updates")
    Switch(
        checked = liveWatchlistEnabled,
        onCheckedChange = { enabled ->
            if (enabled) {
                viewModel.enableLiveWatchlist()
            } else {
                viewModel.disableLiveWatchlist()
            }
        }
    )
}
```

---

## Comparison

| Feature | Scheduled Export | Live API |
|---------|-----------------|----------|
| Update Frequency | 5 minutes | 30 seconds |
| Setup Complexity | Easy | Medium |
| App Changes | Minimal | Moderate |
| Server Load | Low | Medium |
| Real-time | No | Yes |
| Offline Support | Yes (cached JSON) | No |

---

## Recommended Approach

### For Most Users: **Scheduled Export**

1. Start continuous export:
```powershell
cd c:\Users\HP\Documents\NEW_ASC
.\continuous_watchlist_export.ps1
```

2. Keep AI running in another terminal

3. App loads updated JSON on refresh (already implemented!)

4. User pulls to refresh every few minutes

### For Power Users: **Live API**

1. Add API endpoint to your AI server
2. Update Android app to poll endpoint
3. Enable "Live Updates" toggle in settings
4. Get real-time probability updates every 30 seconds

---

## Quick Start (Scheduled Export)

**Right now, in a new PowerShell terminal:**

```powershell
cd c:\Users\HP\Documents\NEW_ASC
.\continuous_watchlist_export.ps1
```

**Output:**
```
Starting continuous watchlist export...
Exports every 5 minutes. Press Ctrl+C to stop.

[12:00:00] Export #1 - Running watchlist export...
[OK] EURUSD: 55% breakout probability
...
[12:00:05] ✓ Export successful
[12:00:05] Waiting 5 minutes until next export...

[12:05:00] Export #2 - Running watchlist export...
...
```

**In Android app:**
- Pull to refresh on Watchlist screen
- Gets latest probabilities from updated JSON

---

## Files Created

- `c:\Users\HP\Documents\NEW_ASC\continuous_watchlist_export.ps1` - Scheduled export script
- `c:\Users\HP\Documents\NEW_ASC\add_watchlist_api_endpoint.py` - API endpoint code
- `c:\Users\HP\AndroidStudioProjects\MyRealApp\CONTINUOUS_LIVE_PROBABILITY_GUIDE.md` - This guide

---

## Summary

**For continuous live probabilities:**

1. ✅ **Easiest**: Run `continuous_watchlist_export.ps1` (5-minute updates)
2. 🚀 **Real-time**: Add API endpoint + poll from app (30-second updates)

Both approaches work while your AI runs on port 8000!
