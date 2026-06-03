# Watchlist Breakout Probability - Data Source

## Current Status: MOCK DATA ⚠️

The watchlist currently uses **hardcoded mock data** defined in `ForexViewModel.kt`. The breakout probability is **not calculated from real AI analysis** yet.

## Current Data Source

### Location
`app/src/main/java/com/asc/markets/logic/ForexViewModel.kt`

### Mock Data Example
```kotlin
WatchlistItem(
    assetName = "EURUSD",
    status = "Volatility Compression",
    confidence = 85,
    newsRisk = "High (CPI in 1h)",
    moveProbability = 76,  // ← HARDCODED
    priority = 1,
    preMoveSignal = "Accumulation",
    volatilityScore = 45,
    triggerEvent = "US CPI",
    timeToEvent = "42 mins",
    rationale = "AI detects tight range compression on H1 with accumulation signature. US CPI in 42 mins expected to catalyze directional expansion.",
    category = MarketCategory.FOREX
)
```

## What Breakout Probability SHOULD Use

Based on your AI system, breakout probability should be calculated from:

### 1. **REGIME_AI** (Market State)
- Compression vs Expansion detection
- Regime transitions
- Volatility cycles

### 2. **VOLATILITY_AI** 
- ATR ratios
- Volatility compression levels
- Historical volatility patterns
- Expansion probability

### 3. **INDICATOR_INTELLIGENCE_AI**
- Bollinger Band squeeze
- ATR compression
- Volume profile
- Support/resistance proximity

### 4. **CONFLUENCE_AI**
- Multiple timeframe alignment
- Technical confluence score
- Pattern recognition

### 5. **NEWS_INTELLIGENCE_AI**
- Upcoming high-impact events
- Time to event
- Event impact scores
- News risk assessment

### 6. **FINAL_TRADING_AI**
- Final trade score (0-100)
- Signal quality
- Execution readiness

## Recommended Calculation Formula

```python
breakout_probability = (
    regime_compression_score * 0.30 +      # 30% - Is market compressed?
    volatility_expansion_prob * 0.25 +     # 25% - Volatility ready to expand?
    indicator_confluence * 0.20 +          # 20% - Technical alignment
    news_catalyst_score * 0.15 +           # 15% - Upcoming catalyst
    final_trade_score * 0.10               # 10% - Overall signal quality
)
```

### Scoring Ranges
- **80-100%**: Extreme compression, high catalyst, strong confluence
- **70-79%**: Strong setup, good catalyst timing
- **60-69%**: Moderate setup, some confluence
- **50-59%**: Weak setup, low probability
- **<50%**: Not recommended for watchlist

## How to Integrate Real AI Data

### Step 1: Export AI Data for Watchlist
Create `export_watchlist_for_app.py`:
```python
import pandas as pd
from pathlib import Path

# Read AI outputs
regime_df = pd.read_parquet("AI_SYSTEM/ASC_AI/surfaces/D1_V1_regime_enriched.parquet")
volatility_df = pd.read_parquet("AI_SYSTEM/ASC_AI/surfaces/D1_V1_volatility_enriched.parquet")
final_df = pd.read_parquet("AI_SYSTEM/ASC_AI/surfaces/D1_V1_final_trading_enriched.parquet")
news_df = pd.read_parquet("AI_SYSTEM/ASC_AI/surfaces/D2_V1_news_intelligence_enriched.parquet")

# Calculate breakout probability for each asset
watchlist_items = []
for asset in ASSET_UNIVERSE:
    # Get latest data for asset
    regime = regime_df[regime_df['asset'] == asset].iloc[-1]
    volatility = volatility_df[volatility_df['asset'] == asset].iloc[-1]
    final = final_df[final_df['asset'] == asset].iloc[-1]
    
    # Calculate breakout probability
    breakout_prob = calculate_breakout_probability(regime, volatility, final, news_df, asset)
    
    # Only include if probability >= 60%
    if breakout_prob >= 60:
        watchlist_items.append({
            'asset': asset,
            'breakout_probability': breakout_prob,
            'status': regime['regime_state'],
            'confidence': final['signal_quality'],
            'volatility_score': volatility['volatility_score'],
            # ... other fields
        })

# Export to JSON
export_to_json(watchlist_items, "watchlist_ai_data.json")
```

### Step 2: Update Android App
Modify `ForexViewModel.kt` to load from JSON instead of hardcoded data:
```kotlin
private fun loadWatchlistFromAI() {
    val file = File(context.filesDir, "watchlist_ai_data.json")
    if (file.exists()) {
        val json = file.readText()
        val items = parseWatchlistJson(json)
        _watchlistItems.value = items
    }
}
```

### Step 3: Auto-Update in Production
Add to `start_production_live.ps1`:
```powershell
# After full pipeline
python export_watchlist_for_app.py
```

## Current Watchlist Fields

```kotlin
data class WatchlistItem(
    val assetName: String,              // e.g., "EURUSD"
    val status: String,                 // e.g., "Volatility Compression"
    val confidence: Int,                // 0-100
    val newsRisk: String,               // "High (CPI in 1h)"
    val moveProbability: Int,           // ← BREAKOUT PROBABILITY (0-100)
    val priority: Int,                  // 1-10
    val preMoveSignal: String,          // "Accumulation", "Expansion"
    val volatilityScore: Int,           // 0-100
    val triggerEvent: String,           // "US CPI"
    val timeToEvent: String,            // "42 mins"
    val price: Double,                  // Current price
    val changePercent: Double,          // % change
    val category: MarketCategory,       // FOREX, CRYPTO, etc.
    val rationale: String,              // AI explanation
    val isNew: Boolean                  // Show "NEW" badge
)
```

## Mapping AI Data to Watchlist Fields

| Watchlist Field | AI Data Source |
|----------------|----------------|
| `moveProbability` | **Calculated** from regime + volatility + confluence |
| `status` | `REGIME_AI.regime_state` |
| `confidence` | `FINAL_TRADING_AI.signal_quality` |
| `newsRisk` | `NEWS_INTELLIGENCE_AI.impact_score` + time to event |
| `preMoveSignal` | `REGIME_AI.regime_state` |
| `volatilityScore` | `VOLATILITY_AI.volatility_score` |
| `triggerEvent` | `NEWS_INTELLIGENCE_AI.event_type` |
| `timeToEvent` | Calculated from `NEWS_INTELLIGENCE_AI.published_at` |
| `rationale` | Generated from AI analysis |

## Summary

**Current State**: Watchlist uses hardcoded mock data with fake breakout probabilities.

**What It Should Use**: Real-time AI analysis from your 40 feeders, specifically:
- REGIME_AI (compression detection)
- VOLATILITY_AI (expansion probability)
- INDICATOR_INTELLIGENCE_AI (technical confluence)
- NEWS_INTELLIGENCE_AI (catalyst timing)
- FINAL_TRADING_AI (overall signal quality)

**Next Steps**:
1. Create `export_watchlist_for_app.py` script
2. Calculate breakout probability from AI data
3. Update Android app to load from JSON
4. Add to production pipeline

**Priority**: HIGH - This is a key feature showing fake data to users.
