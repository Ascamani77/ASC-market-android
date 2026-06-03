# Liquidity Page Navigation Fix

## Issue
When clicking any asset in the Liquidity page, the app would navigate to the homepage (DASHBOARD) first, then when returning to the Liquidity page, the asset would be selected. This caused an unwanted navigation interruption.

## Root Cause
The `LiquidityHubScreen` was calling `viewModel.selectPair(pair)` which internally sets:
```kotlin
_currentView.value = AppView.DASHBOARD
```

This caused the navigation to DASHBOARD view whenever an asset was selected.

## Solution
1. **Added new function in ForexViewModel**: `selectPairNoNavigate(pair: ForexPair)`
   - This function selects a pair without changing the current view
   - Similar to existing `selectPairBySymbolNoNavigate` but accepts a `ForexPair` object directly

2. **Updated LiquidityHubScreen**: Changed from `viewModel.selectPair(pair)` to `viewModel.selectPairNoNavigate(pair)`

## Files Modified
- `app/src/main/java/com/asc/markets/logic/ForexViewModel.kt`
  - Added `selectPairNoNavigate(pair: ForexPair)` function
  
- `app/src/main/java/com/asc/markets/ui/screens/LiquidityHubScreen.kt`
  - Updated AssetRow onClick to use `selectPairNoNavigate` instead of `selectPair`

## Result
✅ Clicking assets in Liquidity page now updates the selection WITHOUT navigating away
✅ The selected asset is immediately highlighted and its liquidity data is displayed
✅ User stays on the Liquidity page as expected

## Date
May 31, 2026
