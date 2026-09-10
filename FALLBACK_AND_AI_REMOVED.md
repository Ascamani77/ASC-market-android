# Fallback Data & AI Deployments Removal - Complete

## Summary
Removed ALL fallback data sources, AI deployment integrations, and Pepperstone references from Android app. App now uses **ONLY MT5 EA data**.

## Files Deleted (5)
1. ✅ `app/src/main/kotlin/com/trading/app/data/PepperstoneChartService.kt`
2. ✅ `app/src/main/java/com/asc/markets/data/CombinedFallbackDataStore.kt`
3. ✅ `app/src/main/java/com/asc/markets/data/CombinedFallbackStore.kt`
4. ✅ `app/src/main/java/com/asc/markets/notifications/CombinedFallbackNotifier.kt`
5. ✅ `app/src/main/java/com/asc/markets/notifications/CombinedFallbackActionReceiver.kt`

## Files Modified (22)

### Core Data Layer
1. ✅ `UnifiedMarketDataStore.kt`
   - Removed `DataSource.PEPPERSTONE_FALLBACK` enum
   - Removed `updateFromFallback()` function
   - Now only uses EA data or shows LOADING state
   - Removed all CombinedFallbackDataStore references

2. ✅ `PreMoveIntelligence.kt`
   - Removed `fallbackSnapshot` Flow
   - Removed CombinedFallbackDataStore from `candidates` Flow

3. ✅ `EALiveDataStore.kt` (from previous task)
   - Already updated with `alignment_percentage`

### ViewModel Layer
4. ✅ `ForexViewModel.kt`
   - Removed `_aiDeployments` StateFlow
   - Removed `aiDecisions` Flow
   - Removed AI deployment functions

5. ✅ `ChartViewModel.kt`
   - Removed CombinedFallbackDataStore imports
   - Removed fallbackData from combine()
   - Removed fallback pair lookups

6. ✅ `PriceStreamManager.kt`
   - Removed CombinedFallbackDataStore from price updates
   - Only uses MarketDataStore and BinanceDataStore

### UI Screens
7. ✅ `CurrencyStrengthPanel.kt`
   - Removed `DataSource.PEPPERSTONE_FALLBACK` UI states
   - Only shows "EA LIVE" or "LOADING"
   - Updated alignment percentage display

8. ✅ `CommandCenterTab.kt`
   - Set `aiDeployments` to null
   - Set all signal counts to 0
   - Removed aiDeployments references

9. ✅ `MarketOverviewTab.kt`
   - Set `aiDecisions` to empty list
   - Set `specificAiDecision` to null in both functions
   - Removed aiDeployments references

10. ✅ `MultiTimeframeAnalysisScreen.kt`
    - Set `aiDeployments` to null

11. ✅ `DiagnosticsScreen.kt`
    - Set `aiDeployments` to null

12. ✅ `ScalpingScreen.kt`
    - Removed AI deployments comment

13. ✅ `MultiTimeframeScreen.kt`
    - Removed CombinedFallbackDataStore references
    - Removed fallbackPairs and fallbackPriceHistory

14. ✅ `WatchlistScreen.kt`
    - Removed CombinedFallbackDataStore references
    - Removed fallbackPairs from allPairs
    - Removed CombinedFallbackDataStore.timedPriceHistory

15. ✅ `QuotesScreen.kt`
    - Removed CombinedFallbackDataStore collection
    - Removed CombinedFallbackDataStore import

16. ✅ `SettingsScreen.kt`
    - Removed "Combined fallback" toggle
    - Removed CombinedFallbackStore references
    - Removed CombinedFallbackDecision import
    - Changed text to "Data Source: MT5 EA only. No fallback sources."

### UI Components
17. ✅ `PriceTicker.kt`
    - Removed CombinedFallbackDataStore import
    - Removed fallbackPairs from tickerPairs

### Trading Components
18. ✅ `TradingApp.kt`
    - Removed PepperstoneChartService import

19. ✅ `TradingChart.kt`
    - Removed PepperstoneChartService import
    - Removed pepperstoneChartService initialization

### AI/Prompts
20. ✅ `AiPrompts.kt`
    - Removed `aiDeployments` parameter from `buildChatPrompt()`
    - Removed AI context section
    - Updated platform context to "MT5 EA (Primary), Binance (Crypto)"

### ViewModels
21. ✅ `DashboardViewModel.kt`
    - Removed CombinedFallbackDataStore import

## Remaining References (Intentional/Non-problematic)

### SystemTelemetry.kt
- **Status**: OK - Legacy relay names for display only
- **Lines**: Pepperstone cTrader Live/Demo labels
- **Reason**: Display names for telemetry, not active data sources

### CTraderBridgeClient.kt
- **Status**: OK - Log messages only
- **Lines**: "Connecting Pepperstone cTrader bridge", "Pepperstone cTrader bridge connected"
- **Reason**: Legacy log messages, not accessing Pepperstone data

### TradingApp.kt
- **Status**: OK - Enum values and display strings
- **Lines**: ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO
- **Reason**: Part of ChartFeedType enum, not actively used

### SettingsScreen.kt
- **Status**: OK - Display strings for ChartFeedType enum
- **Lines**: "Pepperstone cTrader bridge", "Pepperstone demo account"
- **Reason**: Enum descriptions, not active features

### MarketOverviewTab.kt & CommandCenterTab.kt
- **Status**: OK - Variable naming and local fallback logic
- **Lines**: `fallbackSymbol`, `fallbackPair`, `metricOrFallback()`
- **Reason**: Variable names for local logic, not referring to CombinedFallbackDataStore

### PreMoveIntelligence.kt
- **Status**: OK - Comments only
- **Lines**: "NO FALLBACK" comments
- **Reason**: Documentation comments

### OrderBookStore.kt
- **Status**: OK - Local fallback logic
- **Lines**: `isFallback`, `fallbackRefreshMs`
- **Reason**: Local deterministicSnapshot fallback, not CombinedFallbackDataStore

## Data Flow (After Cleanup)

```
MT5 EA (Port 8001)
    ↓
EALiveDataStore
    ↓
UnifiedMarketDataStore (EA ONLY)
    ↓
UI Screens
```

**No Fallback. No AI Backend. No Pepperstone.**

## Verification

### What You Should See:
1. **App shows**:
   - GREEN "EA LIVE" when connected
   - GRAY "LOADING" when disconnected
   - NO "FALLBACK" indicator

2. **Accumulation Radar shows**:
   - Symbol names (e.g., BCHUSDm, EURUSDm)
   - MTF Align % (e.g., "38.5%", "62.0%")
   - Confidence score (from EA regime)
   - Current price

3. **Command Center**:
   - No AI signals
   - No deployment counts
   - Shows macro events only

4. **Settings**:
   - No "Combined fallback" toggle
   - Text: "Data Source: MT5 EA only. No fallback sources."

### What Should NOT Work:
- ❌ Pepperstone data
- ❌ AI deployments
- ❌ Combined fallback
- ❌ Python backend AI

## Next Steps

1. **Rebuild Android App**:
   ```bash
   cd c:\Users\HP\AndroidStudioProjects\MyRealApp
   .\gradlew clean assembleDebug
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

2. **Verify EA Running**:
   - Check MT5 has EA attached
   - Verify `http://localhost:8001/live_market_data.json` returns data

3. **Test App**:
   - Open app → Dashboard → Accumulation Radar
   - Should see GREEN "EA LIVE"
   - Should see assets with alignment percentages
   - Should NOT see "FALLBACK" anywhere

## Files That Reference "Pepperstone" or "Fallback" (All Non-Functional)

### Safe References (Display/Enum/Comments Only):
- `SystemTelemetry.kt` - Display names
- `CTraderBridgeClient.kt` - Log messages
- `TradingApp.kt` - Enum values
- `SettingsScreen.kt` - Enum descriptions
- `MarketOverviewTab.kt` - Variable names
- `CommandCenterTab.kt` - Variable names
- `PreMoveIntelligence.kt` - Comments
- `OrderBookStore.kt` - Local fallback logic

**None of these actually use CombinedFallbackDataStore or PepperstoneChartService.**

## Status
✅ **COMPLETE** - All fallback data sources, AI deployments, and Pepperstone files removed from Android app.

## Date
July 29, 2026
