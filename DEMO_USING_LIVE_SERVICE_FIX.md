# Demo Was Using Live Service - FIXED

## Problem Discovered
You were absolutely right! When selecting "Pepperstone Demo" feed:
- ✅ You got live data (because it was working)
- ❌ Demo showed as IDLE in Market Data Bus
- ❌ Demo service was never actually being used

**Root Cause**: The app was using the Live service (`TradingChartPepperstoneCTrader`) for BOTH Live and Demo feeds!

## The Bug

### In TradingApp.kt (Line 1826)
```kotlin
// BEFORE (WRONG):
ChartFeedType.PEPPERSTONE_CTRADER, ChartFeedType.PEPPERSTONE_DEMO -> {
    TradingChartPepperstoneCTrader(  // ❌ Using LIVE component for both!
        symbol = symbol,
        timeframe = timeframe
    ) { resolvedChartFeedType, providerChartData ->
        renderProviderChart(resolvedChartFeedType, BinanceMarketType.FUTURES, providerChartData)
    }
}
```

This meant:
- When you selected "Pepperstone cTrader Live" → Used Live service ✅
- When you selected "Pepperstone Demo" → **ALSO used Live service** ❌

That's why:
- Demo showed as IDLE (Demo service never started)
- But you still got data (from Live service)
- Live showed as ACTIVE (it was handling both feeds)

## The Fix

### Separated the Feed Types
```kotlin
// AFTER (CORRECT):
ChartFeedType.PEPPERSTONE_CTRADER -> {
    TradingChartPepperstoneCTrader(  // ✅ Live uses Live component
        symbol = symbol,
        timeframe = timeframe
    ) { resolvedChartFeedType, providerChartData ->
        renderProviderChart(resolvedChartFeedType, BinanceMarketType.FUTURES, providerChartData)
    }
}
ChartFeedType.PEPPERSTONE_DEMO -> {
    TradingChartPepperstoneDemo(  // ✅ Demo uses Demo component
        symbol = symbol,
        timeframe = timeframe
    ) { resolvedChartFeedType, providerChartData ->
        renderProviderChart(resolvedChartFeedType, BinanceMarketType.FUTURES, providerChartData)
    }
}
```

Now:
- "Pepperstone cTrader Live" → Uses `TradingChartPepperstoneCTrader` → Connects to port 8082
- "Pepperstone Demo" → Uses `TradingChartPepperstoneDemo` → Connects to port 8083

## Files Modified

### TradingApp.kt
**Location**: `app/src/main/kotlin/com/trading/app/TradingApp.kt`

**Change**: Separated PEPPERSTONE_CTRADER and PEPPERSTONE_DEMO into independent chart components.

## How It Works Now

### When You Select "Pepperstone cTrader Live"
1. App creates `TradingChartPepperstoneCTrader` component
2. Component creates `PepperstoneCTraderChartService`
3. Service connects to `192.168.1.198:8082` (Live bridge)
4. Service records telemetry as "CTRADER_LIVE"
5. Market Data Bus shows "Pepperstone cTrader Live" as **ACTIVE**

### When You Select "Pepperstone Demo"
1. App creates `TradingChartPepperstoneDemo` component
2. Component creates `PepperstoneDemoChartService`
3. Service connects to `192.168.1.198:8083` (Demo bridge)
4. Service records telemetry as "CTRADER_DEMO"
5. Market Data Bus shows "Pepperstone cTrader Demo" as **ACTIVE**

## Expected Behavior After Fix

### Scenario 1: Using Live Feed
- Open a chart
- Select "Pepperstone cTrader Live" feed
- **Market Data Bus**:
  - ✅ Pepperstone cTrader Live: **ACTIVE** (green, progress bars)
  - ⚪ Pepperstone cTrader Demo: **IDLE** (gray, no bars)

### Scenario 2: Using Demo Feed
- Open a chart
- Select "Pepperstone Demo" feed
- **Market Data Bus**:
  - ⚪ Pepperstone cTrader Live: **IDLE** (gray, no bars)
  - ✅ Pepperstone cTrader Demo: **ACTIVE** (green, progress bars)

### Scenario 3: Using Both Feeds
- Open chart 1 with "Pepperstone cTrader Live"
- Open chart 2 with "Pepperstone Demo"
- **Market Data Bus**:
  - ✅ Pepperstone cTrader Live: **ACTIVE** (green, progress bars)
  - ✅ Pepperstone cTrader Demo: **ACTIVE** (green, progress bars)

## Verification Steps

### 1. Rebuild and Install App
```bash
./gradlew clean assembleDebug
```
Then install on your device.

### 2. Test Live Feed
1. Open a chart
2. Select "Pepperstone cTrader Live" from feed selector
3. Go to Market Data Bus
4. Verify "Pepperstone cTrader Live" shows **ACTIVE**
5. Verify "Pepperstone cTrader Demo" shows **IDLE**

### 3. Test Demo Feed
1. Open a chart (or switch existing chart)
2. Select "Pepperstone Demo" from feed selector
3. Go to Market Data Bus
4. Verify "Pepperstone cTrader Demo" shows **ACTIVE** ✅ **THIS SHOULD NOW WORK!**
5. Verify "Pepperstone cTrader Live" shows **IDLE** (if no other chart using it)

### 4. Check Logs
When using Demo feed, you should see:
```
TradingChartPepperstoneDemo: Creating new independent DEMO service instance
TradingChartPepperstoneDemo: Starting DEMO stream for EURUSD, timeframe: H1
Pepperstone Demo WebSocket connected
SystemTelemetry: PRICE_TICK via CTRADER_DEMO | LATENCY: 5ms
```

## Why This Matters

### Before Fix
- Demo account data was being mixed with Live account
- You couldn't tell if Demo bridge was working
- Demo service was never tested/used
- Potential confusion about which account you're trading on

### After Fix
- Live and Demo are completely independent
- Each uses its own bridge (port 8082 vs 8083)
- Each uses its own account credentials
- Market Data Bus accurately shows which service is active
- No risk of mixing Live and Demo data

## Account Differences

### Live Account (Port 8082)
- Real money account
- Account ID: 47341092
- Uses production cTrader credentials
- Connects to live market data

### Demo Account (Port 8083)
- Virtual $50,000 balance
- Account ID: 47340965 or 5288664
- Uses demo cTrader credentials
- Connects to demo market data
- Safe for testing strategies

## Troubleshooting

### Demo Still Shows IDLE After Fix

**Check 1: Did you rebuild the app?**
This code change requires rebuilding and reinstalling.

**Check 2: Are you actually using Demo feed?**
- Open chart
- Check feed selector shows "Pepperstone Demo"
- Not "Pepperstone cTrader Live"

**Check 3: Is Demo bridge running?**
```powershell
netstat -an | findstr 8083
# Should show: TCP 0.0.0.0:8083 ... LISTENING
```

**Check 4: Check logs**
Look for "Creating new independent DEMO service instance"
If you don't see this, the Demo component isn't being used.

### Both Show ACTIVE When Using Only One

This is actually correct if:
- You have multiple charts open
- One chart uses Live, another uses Demo
- Both services are legitimately active

## Summary

**The Mystery Solved**:
- You were getting data when selecting Demo ✅
- But Demo showed as IDLE ❌
- **Because**: Demo selection was actually using the Live service!

**The Fix**:
- Separated Live and Demo into independent chart components
- Each now uses its own service
- Each records its own telemetry
- Market Data Bus now accurately reflects which service is active

Rebuild the app and Demo will finally show as **ACTIVE** when you use it! 🎉
