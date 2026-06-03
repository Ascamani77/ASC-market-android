# AI Watchlist Integration Guide

## Overview
This guide explains how to integrate real AI-calculated breakout probabilities into your Android app's watchlist.

## What Was Created

### 1. Export Script
**File**: `c:\Users\HP\Documents\NEW_ASC\export_watchlist_for_app.py`

**What it does**:
- Reads data from all AI feeders (REGIME, VOLATILITY, INDICATOR, CONFLUENCE, NEWS, FINAL)
- Calculates breakout probability using weighted formula:
  - Regime compression: 30%
  - Volatility expansion: 25%
  - Technical confluence: 20%
  - News catalyst: 15%
  - Final trade score: 10%
- Exports high-probability setups (≥60%) to JSON
- Copies to app's assets folder

**Output**: `ai_watchlist.json`

### 2. Test Script
**File**: `c:\Users\HP\Documents\NEW_ASC\test_watchlist_export.py`

**Usage**:
```powershell
cd c:\Users\HP\Documents\NEW_ASC
python test_watchlist_export.py
```

### 3. Production Integration
**File**: `start_production_live.ps1` (updated)

Now automatically exports watchlist after full pipeline runs.

## JSON Structure

```json
{
  "type": "ai_watchlist",
  "timestamp": "2026-05-26T12:00:00+00:00",
  "source": "AI_SYSTEM",
  "version": "v1",
  "items": [
    {
      "id": "ai_EURUSD_1716724800",
      "assetName": "EURUSD",
      "status": "Volatility Compression",
      "confidence": 85,
      "newsRisk": "High (CPI in 42m)",
      "moveProbability": 76,
      "priority": 1,
      "preMoveSignal": "Compression",
      "volatilityScore": 35,
      "triggerEvent": "Major Data Release",
      "timeToEvent": "42 mins",
      "price": 0.0,
      "changePercent": 0.0,
      "category": "FOREX",
      "rationale": "AI detects tight range compression with accumulation signature. Low volatility (35/100) suggests imminent expansion. Major Data Release in 42 mins expected to catalyze directional move. High-probability setup (76%).",
      "isNew": true,
      "addedAt": 1716724800000
    }
  ]
}
```

## Android App Integration

### Step 1: Update ForexViewModel

**File**: `app/src/main/java/com/asc/markets/logic/ForexViewModel.kt`

Replace the hardcoded watchlist with AI data loader:

```kotlin
// Add at top of class
private val context: Context  // Inject via constructor

// Replace hardcoded _watchlistItems with:
private val _watchlistItems = MutableStateFlow<List<WatchlistItem>>(emptyList())

init {
    loadWatchlistFromAI()
}

private fun loadWatchlistFromAI() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // Try loading from assets first
            val jsonString = try {
                context.assets.open("ai_watchlist.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                // Fallback to external storage
                val file = File(context.filesDir, "ai_watchlist.json")
                if (file.exists()) file.readText() else null
            }
            
            if (jsonString != null) {
                val watchlistData = parseWatchlistJson(jsonString)
                _watchlistItems.value = watchlistData
                _lastWatchlistUpdate.value = System.currentTimeMillis()
                Log.i("ForexViewModel", "✅ Loaded ${watchlistData.size} items from AI watchlist")
            } else {
                Log.w("ForexViewModel", "⚠️ AI watchlist not found, using fallback data")
                loadFallbackWatchlist()
            }
        } catch (e: Exception) {
            Log.e("ForexViewModel", "Failed to load AI watchlist", e)
            loadFallbackWatchlist()
        }
    }
}

private fun parseWatchlistJson(json: String): List<WatchlistItem> {
    val jsonObject = JSONObject(json)
    val itemsArray = jsonObject.getJSONArray("items")
    val items = mutableListOf<WatchlistItem>()
    
    for (i in 0 until itemsArray.length()) {
        val item = itemsArray.getJSONObject(i)
        items.add(
            WatchlistItem(
                id = item.getString("id"),
                assetName = item.getString("assetName"),
                status = item.getString("status"),
                confidence = item.getInt("confidence"),
                newsRisk = item.getString("newsRisk"),
                moveProbability = item.getInt("moveProbability"),
                priority = item.getInt("priority"),
                preMoveSignal = item.getString("preMoveSignal"),
                volatilityScore = item.getInt("volatilityScore"),
                triggerEvent = item.optString("triggerEvent", ""),
                timeToEvent = item.optString("timeToEvent", ""),
                price = item.optDouble("price", 0.0),
                changePercent = item.optDouble("changePercent", 0.0),
                category = MarketCategory.valueOf(item.getString("category")),
                rationale = item.optString("rationale", ""),
                isNew = item.optBoolean("isNew", false),
                addedAt = item.optLong("addedAt", System.currentTimeMillis())
            )
        )
    }
    
    return items
}

private fun loadFallbackWatchlist() {
    // Keep current hardcoded data as fallback
    _watchlistItems.value = listOf(
        // ... existing hardcoded items
    )
}

fun refreshWatchlist() {
    _isWatchlistAnalyzing.value = true
    _lastWatchlistUpdate.value = System.currentTimeMillis()
    
    // Reload from AI
    loadWatchlistFromAI()
    
    viewModelScope.launch {
        delay(1000)  // Simulate analysis
        _isWatchlistAnalyzing.value = false
    }
}
```

### Step 2: Update ViewModel Constructor

Add Context parameter:

```kotlin
class ForexViewModel(
    private val context: Context
) : ViewModel() {
    // ... rest of class
}
```

### Step 3: Create ViewModel Factory

```kotlin
class ForexViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForexViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ForexViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### Step 4: Update Composables

In screens that use ForexViewModel:

```kotlin
@Composable
fun WatchlistScreen(
    viewModel: ForexViewModel = viewModel(
        factory = ForexViewModelFactory(LocalContext.current)
    ),
    // ... rest of parameters
) {
    // ... rest of composable
}
```

## Testing

### 1. Test Export Script
```powershell
cd c:\Users\HP\Documents\NEW_ASC
python test_watchlist_export.py
```

**Expected output**:
```
✅ File created: C:\Users\HP\AndroidStudioProjects\MyRealApp\ai_watchlist.json
✅ Type: ai_watchlist
✅ Source: AI_SYSTEM
✅ Items: 5
```

### 2. Verify JSON Structure
```powershell
cd c:\Users\HP\AndroidStudioProjects\MyRealApp
python -c "import json; print(json.dumps(json.load(open('ai_watchlist.json')), indent=2)[:500])"
```

### 3. Test in App
1. Rebuild Android app
2. Open Watchlist screen
3. Check logcat for:
   ```
   ✅ Loaded X items from AI watchlist
   ```
4. Verify breakout probabilities are realistic (60-100%)
5. Check that rationale text makes sense

## Production Workflow

### Manual Export
```powershell
cd c:\Users\HP\Documents\NEW_ASC
python export_watchlist_for_app.py
```

### Automatic (Production Mode)
```powershell
cd c:\Users\HP\Documents\NEW_ASC
.\start_production_live.ps1
```

This will:
1. Run full AI pipeline
2. Export news intelligence
3. Export watchlist
4. Repeat every 15 minutes

## Troubleshooting

### Issue: No items in watchlist
**Cause**: No assets meet minimum 60% breakout probability

**Solution**:
1. Check AI feeders have generated data
2. Lower `MIN_BREAKOUT_PROBABILITY` in export script
3. Run full AI pipeline first

### Issue: App shows "AI watchlist not found"
**Cause**: JSON file not in assets folder

**Solution**:
1. Run export script
2. Rebuild app to include updated assets
3. Check file exists in `app/src/main/assets/`

### Issue: Breakout probabilities seem wrong
**Cause**: AI data may be stale or incomplete

**Solution**:
1. Run full AI pipeline: `python run_full_ai_system.py`
2. Re-export watchlist: `python export_watchlist_for_app.py`
3. Check AI feeder outputs exist

## Breakout Probability Formula

```
breakout_probability = (
    regime_compression_score * 0.30 +      # Is market compressed?
    volatility_expansion_prob * 0.25 +     # Ready to expand?
    technical_confluence * 0.20 +          # Technical alignment?
    news_catalyst_score * 0.15 +           # Upcoming catalyst?
    final_trade_score * 0.10               # Overall quality?
) * 100
```

### Scoring Examples

**High Probability (80%+)**:
- Tight compression (0.95)
- Low volatility (0.90)
- Strong confluence (0.85)
- High-impact news in <1h (0.95)
- Strong final score (0.80)

**Medium Probability (70-79%)**:
- Moderate compression (0.75)
- Moderate volatility (0.70)
- Good confluence (0.70)
- Medium-impact news (0.65)
- Good final score (0.70)

**Moderate Probability (60-69%)**:
- Some compression (0.60)
- Moderate volatility (0.60)
- Some confluence (0.60)
- Low-impact news (0.50)
- Moderate final score (0.60)

## Files Created/Modified

### Created
- `c:\Users\HP\Documents\NEW_ASC\export_watchlist_for_app.py`
- `c:\Users\HP\Documents\NEW_ASC\test_watchlist_export.py`
- `c:\Users\HP\AndroidStudioProjects\MyRealApp\ai_watchlist.json`
- `app\src\main\assets\ai_watchlist.json`

### Modified
- `c:\Users\HP\Documents\NEW_ASC\start_production_live.ps1`

### To Modify (Android App)
- `app\src\main\java\com\asc\markets\logic\ForexViewModel.kt`

## Next Steps

1. ✅ Export script created
2. ✅ Production script updated
3. ⏳ Test export script
4. ⏳ Update Android app to load from JSON
5. ⏳ Test in app
6. ⏳ Deploy to production

---

**Status**: Export script ready. Android app integration pending.
