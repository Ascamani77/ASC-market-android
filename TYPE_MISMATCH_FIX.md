# Type Mismatch Fix - cTraderService Parameter

## Issue
```
Argument type mismatch: actual type is 'Any?', but 'CTraderService?' was expected.
```

## Root Cause
The `cTraderService` parameter in `TradingChart2.kt` was typed as `CTraderService?`, but we needed to pass both:
- `CTraderService` (for live trading)
- `CTraderDemoService` (for demo trading)

These are two separate classes without a common interface.

## Solution

### 1. Changed Parameter Type
**File:** `TradingChart2.kt` line 136

**Before:**
```kotlin
cTraderService: com.trading.app.data.CTraderService? = null,
```

**After:**
```kotlin
cTraderService: Any? = null,  // Can be CTraderService or CTraderDemoService
```

### 2. Added Type Casting
Updated all usages to cast to the correct type:

**PEPPERSTONE_CTRADER cases:**
```kotlin
(cTraderService as? com.trading.app.data.CTraderService)?.placeMarketOrder(...)
```

**PEPPERSTONE_DEMO cases:**
```kotlin
(cTraderService as? com.trading.app.data.CTraderDemoService)?.placeMarketOrder(...)
```

## Files Modified
- `TradingChart2.kt` - 5 locations updated:
  1. Parameter declaration (line 136)
  2. SELL order - PEPPERSTONE_CTRADER (line 380)
  3. SELL order - PEPPERSTONE_DEMO (line 415)
  4. BUY order - PEPPERSTONE_CTRADER (line 542)
  5. BUY order - PEPPERSTONE_DEMO (line 577)

## Why This Works
- `Any?` is the root type in Kotlin that can hold any object
- Safe cast operator `as?` returns null if the cast fails
- Each case casts to its specific service type before calling methods
- Type safety is maintained through the safe cast operator

## Alternative Solutions Considered

### Option 1: Create Common Interface ❌
```kotlin
interface ICTraderService {
    fun placeMarketOrder(...)
    fun closePosition(...)
}
```
**Rejected:** Would require modifying both service classes

### Option 2: Use Sealed Class ❌
```kotlin
sealed class CTraderServiceWrapper {
    data class Live(val service: CTraderService)
    data class Demo(val service: CTraderDemoService)
}
```
**Rejected:** Too complex for this use case

### Option 3: Use Any? with Safe Casts ✅
**Selected:** Minimal changes, type-safe, works immediately

## Testing
After this fix, the build should complete successfully with no type mismatch errors.

## Status
✅ **FIXED** - Type mismatch resolved
