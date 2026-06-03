# TradingApp.kt When Expression Exhaustive Fix

## Problem
The `TradingApp.kt` file had 8 compilation errors due to non-exhaustive `when` expressions. The `ChartFeedType` enum has 5 values, but several `when` expressions were only handling 4 of them, missing the `PEPPERSTONE_DEMO` branch.

## ChartFeedType Enum Values
```kotlin
enum class ChartFeedType {
    EXNESS,
    PEPPERSTONE_CTRADER,
    PEPPERSTONE_DEMO,      // ← This was missing in when expressions
    BINANCE,
    BINANCE_CONNECT
}
```

## Fixes Applied

### 1. `liveTradeSourceName()` - Line 359
**Added:**
```kotlin
ChartFeedType.PEPPERSTONE_DEMO -> "Pepperstone Demo"
```

### 2. `liveTradeDefaultAccountLabel()` - Line 367
**Added:**
```kotlin
ChartFeedType.PEPPERSTONE_DEMO -> "Pepperstone Demo"
```

### 3. `LaunchedEffect` (watchlist subscription) - Line 507
**Changed:**
```kotlin
// Before
ChartFeedType.PEPPERSTONE_CTRADER -> { ... }

// After
ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO -> { ... }
```
Both Pepperstone variants use the same quote service, so they share the same branch.

### 4. Connection Management - Line 750
**Changed:**
```kotlin
// Before
ChartFeedType.PEPPERSTONE_CTRADER -> { ... }

// After
ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO -> { ... }
```

### 5. `placeStreamOrder()` - Line 978
**Changed:**
```kotlin
// Before
ChartFeedType.PEPPERSTONE_CTRADER -> { ... }

// After
ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO -> { ... }
```

### 6. `closeStreamPosition()` - Line 1159
**Changed:**
```kotlin
// Before
ChartFeedType.PEPPERSTONE_CTRADER -> { ... }

// After
ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO -> { ... }
```

### 7. Chart Component Selection - Line 1807
**Changed:**
```kotlin
// Before
ChartFeedType.PEPPERSTONE_CTRADER -> { ... }

// After
ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO -> { ... }
```

### 8. Provider Label - Line 2067
**Added:**
```kotlin
ChartFeedType.PEPPERSTONE_DEMO -> "Pepperstone Demo"
```

**Also updated:**
```kotlin
// Before
preferSnapshotStats = chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER

// After
preferSnapshotStats = chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER || 
                      chartFeedType == ChartFeedType.PEPPERSTONE_DEMO
```

## Strategy Used

For most cases, `PEPPERSTONE_DEMO` was combined with `PEPPERSTONE_CTRADER` using Kotlin's multi-branch syntax:
```kotlin
ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO -> { ... }
```

This makes sense because:
- Both use the same cTrader service
- Both use the same quote subscription logic
- Both use the same trading operations
- The only difference is the account type (live vs demo)

## Verification

After fixes:
- ✅ All 8 compilation errors resolved
- ✅ No diagnostics found in TradingApp.kt
- ✅ All `when` expressions are now exhaustive
- ✅ PEPPERSTONE_DEMO is properly handled throughout

## Files Modified

- `app/src/main/kotlin/com/trading/app/TradingApp.kt`

## Related Files

- `app/src/main/kotlin/com/trading/app/data/ChartFeedType.kt` - Enum definition

## Notes

- The `PEPPERSTONE_DEMO` enum value was added to support demo trading accounts
- It shares most functionality with `PEPPERSTONE_CTRADER` (live accounts)
- The distinction is mainly for UI labeling and account management
- No functional differences in quote handling or trading operations
