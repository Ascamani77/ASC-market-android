# cTrader Demo Telemetry Fix - Now Shows Active Independently

## Problem
cTrader Demo was showing as **IDLE** in Market Data Bus even though it had live data flowing.

## Root Cause
Both cTrader services were using incorrect telemetry source names:
- **PepperstoneCTraderChartService** (Live): Recording as "PEPPERSTONE_CTRADER_CHART"
- **PepperstoneDemoChartService** (Demo): Recording as "CTRADER_DEMO" ✅ (correct)

SystemTelemetry was looking for:
- **CTRADER_LIVE** for Live service
- **CTRADER_DEMO** for Demo service

The Live service wasn't matching, so neither was showing properly.

## Solution
Updated both services to use the correct telemetry source names that match SystemTelemetry's expectations.

## Files Modified

### 1. PepperstoneCTraderChartService.kt (Live)
**Location**: `app/src/main/kotlin/com/trading/app/data/PepperstoneCTraderChartService.kt`

**Changes**:
```kotlin
// Tick recording - changed source name
SystemTelemetry.recordTick("CTRADER_LIVE", 5.0)  
// was: "PEPPERSTONE_CTRADER_CHART"

// Connection events - changed source name
SystemTelemetry.recordConnectionEvent("CTRADER_LIVE", "WEBSOCKET_CONNECTED")
SystemTelemetry.recordConnectionEvent("CTRADER_LIVE", "CONNECTION_FAILED")
SystemTelemetry.recordConnectionEvent("CTRADER_LIVE", "CONNECTION_CLOSED")
// were all: "PEPPERSTONE_CTRADER_CHART"
```

### 2. PepperstoneDemoChartService.kt (Demo)
**Location**: `app/src/main/kotlin/com/trading/app/data/PepperstoneDemoChartService.kt`

**Already Fixed** (from previous update):
```kotlin
// Tick recording
SystemTelemetry.recordTick("CTRADER_DEMO", 5.0)

// Connection events
SystemTelemetry.recordConnectionEvent("CTRADER_DEMO", "WEBSOCKET_CONNECTED")
SystemTelemetry.recordConnectionEvent("CTRADER_DEMO", "CONNECTION_FAILED")
SystemTelemetry.recordConnectionEvent("CTRADER_DEMO", "CONNECTION_CLOSED")
```

## How It Works Now

### Independent Tracking
Each service now records telemetry independently:

| Service | Source Name | Port | Shows Active When |
|---------|-------------|------|-------------------|
| cTrader Live | CTRADER_LIVE | 8082 | Live bridge connected & sending data |
| cTrader Demo | CTRADER_DEMO | 8083 | Demo bridge connected & sending data |

### Telemetry Flow

**When Live Service Receives Data**:
1. PepperstoneCTraderChartService receives quote
2. Records: `SystemTelemetry.recordTick("CTRADER_LIVE", 5.0)`
3. SystemTelemetry updates `ctraderLiveLatency` and `ctraderLiveBuffer`
4. Market Data Bus shows "Pepperstone cTrader Live" as **ACTIVE**

**When Demo Service Receives Data**:
1. PepperstoneDemoChartService receives quote
2. Records: `SystemTelemetry.recordTick("CTRADER_DEMO", 5.0)`
3. SystemTelemetry updates `ctraderDemoLatency` and `ctraderDemoBuffer`
4. Market Data Bus shows "Pepperstone cTrader Demo" as **ACTIVE**

### Both Can Be Active Simultaneously
- If you have both Live and Demo bridges running
- And both are sending data
- Both will show as **ACTIVE** independently
- Each with their own latency and buffer load metrics

## Expected Behavior After Fix

### Scenario 1: Only Live Bridge Running
- ✅ Pepperstone cTrader Live: **ACTIVE** (green dot, progress bars)
- ⚪ Pepperstone cTrader Demo: **IDLE** (gray dot, no progress bars)

### Scenario 2: Only Demo Bridge Running
- ⚪ Pepperstone cTrader Live: **IDLE** (gray dot, no progress bars)
- ✅ Pepperstone cTrader Demo: **ACTIVE** (green dot, progress bars)

### Scenario 3: Both Bridges Running
- ✅ Pepperstone cTrader Live: **ACTIVE** (green dot, progress bars)
- ✅ Pepperstone cTrader Demo: **ACTIVE** (green dot, progress bars)

## Verification Steps

### 1. Check Demo Bridge is Running
```powershell
netstat -an | findstr 8083
# Should show: TCP 0.0.0.0:8083 ... LISTENING
```

### 2. Check Demo Service is Connected
Look for logs:
```
Pepperstone Demo WebSocket connected
```

### 3. Open a Chart with Demo Feed
- Select a symbol
- Choose "Pepperstone Demo" as feed type
- Chart should load with data

### 4. Check Market Data Bus
- Navigate to Market Data Bus page
- "Pepperstone cTrader Demo" should show:
  - Green dot + "ACTIVE" status
  - Latency: ~5ms (or actual value)
  - Buffer Load: percentage based on tick rate
  - Progress bars visible and animating

## Troubleshooting

### Demo Still Shows IDLE

**Check 1: Is Demo Bridge Running?**
```powershell
netstat -an | findstr 8083
```
If not listening, start the demo bridge.

**Check 2: Is Demo Service Connected?**
Check logs for "Pepperstone Demo WebSocket connected"

**Check 3: Is Data Flowing?**
Look for logs:
```
SystemTelemetry: PRICE_TICK via CTRADER_DEMO | LATENCY: 5ms
```

**Check 4: Rebuild Required?**
After code changes, you must:
1. Clean and rebuild the app
2. Uninstall old version from device
3. Install new version

### Live Shows IDLE (But Was Working Before)

This fix also corrected the Live service telemetry. If Live now shows IDLE:

**Check 1: Is Live Bridge Running?**
```powershell
netstat -an | findstr 8082
```

**Check 2: Is Live Service Connected?**
Check logs for "Pepperstone cTrader WebSocket connected"

**Check 3: Rebuild App**
The source name change requires rebuilding the app.

## Testing Checklist

After rebuilding and installing:

- [ ] Open Market Data Bus page
- [ ] Check cTrader Live status (should match bridge state)
- [ ] Check cTrader Demo status (should match bridge state)
- [ ] Open a chart with Live feed - Live should show ACTIVE
- [ ] Open a chart with Demo feed - Demo should show ACTIVE
- [ ] Verify progress bars animate for active services
- [ ] Verify latency values update in real-time
- [ ] Verify buffer load percentages change

## Summary

**Before Fix**:
- cTrader Live: Using wrong source name → Not tracked properly
- cTrader Demo: Using correct source name → But Live wasn't working

**After Fix**:
- cTrader Live: Uses "CTRADER_LIVE" → Tracked independently ✅
- cTrader Demo: Uses "CTRADER_DEMO" → Tracked independently ✅
- Both can be active simultaneously
- Each shows its own metrics (latency, buffer load)
- Progress bars work for both

Rebuild the app and both services will now show their status independently! 🎉
