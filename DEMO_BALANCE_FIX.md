# Demo Balance Fix - Issue & Solution

## Problem

Your demo chart shows live data but balance/equity shows $0.00 instead of $50,000.

## Root Cause

In `TradingApp.kt` (line 607-608), there's only ONE service for both Live and Demo:

```kotlin
val cTraderTradingService = remember {
    CTraderService(  // <-- Always uses Live service!
        onQuoteUpdate = { quote ->
            if (chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER) {
```

This service connects to:
- Port 8082 (Live bridge) 
- Account 47341092 (Live account with $0 balance)

Even when `chartFeedType == ChartFeedType.PEPPERSTONE_DEMO`, it still uses the live service.

## Why Charts Work But Balance Doesn't

- **Tick data**: Works because both bridges broadcast to all connected clients
- **Account balance**: Shows $0 because it's getting data from the LIVE account (47341092) which is empty
- **Demo balance** ($50,000 from account 47340965) is being broadcast by the demo bridge, but your app isn't listening to it

## Solution

You need to create TWO separate services and connect to the correct one based on feed type:

```kotlin
val cTraderLiveService = remember {
    CTraderService(
        onQuoteUpdate = { /* ... */ },
        onAccountUpdate = { accountInfo ->
            if (chartFeedType == ChartFeedType.PEPPERSTONE_CTRADER) {
                mt5AccountInfo = accountInfo
            }
        },
        // ... other callbacks
    )
}

val cTraderDemoService = remember {
    CTraderDemoService(
        onQuoteUpdate = { /* ... */ },
        onAccountUpdate = { accountInfo ->
            if (chartFeedType == ChartFeedType.PEPPERSTONE_DEMO) {
                mt5AccountInfo = accountInfo  // This will have $50,000
            }
        },
        // ... other callbacks
    )
}

// Then connect to the right service
when (chartFeedType) {
    ChartFeedType.PEPPERSTONE_CTRADER -> {
        cTraderDemoService.disconnect()
        cTraderLiveService.connect()
    }
    ChartFeedType.PEPPERSTONE_DEMO -> {
        cTraderLiveService.disconnect()
        cTraderDemoService.connect()
    }
}
```

## Verification

After the fix, when you select "Pepperstone Demo":

1. **Demo Bridge Logs** should show:
   ```
   [DEBUG] Broadcasting to 1 clients
   [DEBUG] Broadcasting account message: {..., "balance": 50000.0, "equity": 50000.0, ...}
   ```

2. **Your App** should show:
   - Balance: $50,000.00
   - Equity: $50,000.00
   - Account: 47340965

3. **Live Bridge** should not be connected (save on rate limits)

## Quick Test

To verify the demo bridge is working correctly:

```powershell
cd c:\Users\HP\Documents\NEW_ASC
podman logs ctrader-bridge-demo --tail 50
```

You should see:
```
Account Balance: $50,000.00, Equity: $50,000.00
[DEBUG] Broadcasting account message: {"type": "ACCOUNT", "accountId": 47340965, "balance": 50000.0, "equity": 50000.0, ...}
```

This confirms the demo bridge IS sending the correct balance - your app just needs to connect to it properly!

## Current Status

✅ **Live Bridge (8082)**
- Connected to account 47341092
- Balance: $0.00 (empty live account)
- Working correctly for live trading

✅ **Demo Bridge (8083)**
- Connected to account 47340965  
- Balance: $50,000.00 (demo balance)
- Working correctly, broadcasting account data
- **BUT: Your app is not listening to it**

## Action Required

Update `TradingApp.kt` to:
1. Create both `CTraderService` (live) and `CTraderDemoService` (demo)
2. Connect/disconnect the appropriate service based on `chartFeedType`
3. Make sure `onAccountUpdate` callback updates `mt5AccountInfo` for the active service

This will ensure demo mode shows $50,000 and live mode shows the actual live account balance.
