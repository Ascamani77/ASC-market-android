# Complete AI Backend Removal Plan

## Current Status
- ✅ EA LIVE data working
- ✅ 47 assets from MT5 EA
- ❌ Accumulation Radar empty (depends on AI backend)
- ❌ Multiple screens still reference `aiDeployments` from backend

## Files That Reference AI Backend

### Critical (Causes Accumulation Radar to be empty):
1. ✅ **CurrencyStrengthPanel.kt** - FIXED (removed aiDeployments parameter)

### Other Screens (Less critical, but should be cleaned up):
2. **CommandCenterTab.kt** - Uses aiDeployments
3. **DashboardOverviewTab.kt** - Uses aiDeployments  
4. **DashboardQuality.kt** - Uses aiDeployments
5. **DashboardSignals.kt** - Uses aiDeployments
6. **MarketOverviewTab.kt** - Uses aiDeployments
7. **TechnicalVitalsTab.kt** - Uses aiDeployments
8. **DataVaultScreen.kt** - Uses aiDeployments
9. **DiagnosticsScreen.kt** - Uses aiDeployments
10. **HomeAlertsScreen.kt** - Uses aiDeployments
11. **MacroStreamScreen.kt** - Uses aiDeployments
12. **MarketWatchScreen.kt** - Uses aiDeployments
13. **MultiTimeframeAnalysisScreen.kt** - Uses aiDeployments
14. **NewAISimulationScreen.kt** - Uses aiDeployments
15. **ScalpingScreen.kt** - Uses aiDeployments
16. **SentimentScreen.kt** - Uses aiDeployments
17. **SwingTradingScreen.kt** - Uses aiDeployments

## Immediate Fix Applied

### CurrencyStrengthPanel.kt
- ✅ Removed `aiDeployments` parameter from `buildAccumulationRadarItems()`
- ✅ Removed ViewModel AI deployment dependency
- ✅ Simplified to pure EA price action scoring
- ✅ Added logging for debugging

## Test Now

**Rebuild app:**
```
Build > Clean Project
Build > Rebuild Project
Run on phone
```

**Check Logcat:**
Filter: `CurrencyStrength`

**Expected:**
```
D/CurrencyStrength: Building accumulation radar with 47 unique pairs (EA mode)
D/CurrencyStrength: Top 5 accumulation items: [EURUSDm(0.78), BTCUSDm(0.65), ...]
```

**If still empty**, check:
- Is `scopedPairs` empty? (filtered too aggressively)
- Is `priceHistory` empty? (EA data not loading)
- Is `accumulationRadarScore()` returning 0 for all?

## Next Steps (Optional - Clean Up Other Screens)

These screens won't crash, but they won't show AI data either. You can:

**Option 1:** Leave them as-is (they'll just show empty AI sections)

**Option 2:** Remove AI sections from each screen individually

**Option 3:** Hide/disable screens that heavily depend on AI backend

## Recommendation

**For now:** Test if Accumulation Radar works after rebuild. If it does, you're good to go!

**Later:** Gradually clean up other screens to remove AI backend references as needed.
