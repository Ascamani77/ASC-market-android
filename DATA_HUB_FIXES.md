# Data Hub Fixes - MT5 Bridge Added & cTrader Demo Fixed

## Issues Fixed

### 1. ✅ MT5 Bridge Added to Data Hub
**Problem**: MT5 Bridge was missing from the Market Data Bus page.

**Solution**: 
- Added MT5 Bridge to the relay list in SystemTelemetry
- Added MT5 buffer and latency tracking
- MT5 Bridge now appears as the 3rd relay in the list

**Changes Made**:
- `SystemTelemetry.kt`: Added MT5 to calculateMetrics() function
- MT5 telemetry was already being recorded by Mt5Service.kt

### 2. ✅ cTrader Demo Not Showing Active
**Problem**: cTrader Demo had live data but showed as IDLE in Market Data Bus.

**Root Cause**: PepperstoneDemoChartService was recording telemetry with source name "PEPPERSTONE_DEMO_CHART", but SystemTelemetry was looking for "CTRADER_DEMO" or "DEMO".

**Solution**: 
- Changed telemetry source from "PEPPERSTONE_DEMO_CHART" to "CTRADER_DEMO"
- Updated all connection events to use consistent naming
- Added "DEMO" as an alternative source name in SystemTelemetry

**Changes Made**:
- `PepperstoneDemoChartService.kt`: Changed `SystemTelemetry.recordTick()` source name
- `SystemTelemetry.kt`: Added "DEMO" as fallback for ctraderDemoTps calculation

## Files Modified

### 1. SystemTelemetry.kt
**Location**: `app/src/main/java/com/asc/markets/data/SystemTelemetry.kt`

**Changes**:
```kotlin
// Added MT5 tracking in calculateMetrics()
val mt5Tps = sourceTickTimestamps["MT5"]?.size?.toDouble() ?: 0.0
mt5Buffer = (mt5Tps * 20.0).coerceIn(0.0, 100.0)

// Added MT5 to relay list
RelayData("MT5 Bridge", mt5Latency, mt5Buffer, "MT5"),

// Added DEMO as fallback for cTrader Demo
val ctraderDemoTps = sourceTickTimestamps["CTRADER_DEMO"]?.size?.toDouble() 
    ?: (sourceTickTimestamps["DEMO"]?.size?.toDouble() ?: 0.0)
```

### 2. PepperstoneDemoChartService.kt
**Location**: `app/src/main/kotlin/com/trading/app/data/PepperstoneDemoChartService.kt`

**Changes**:
```kotlin
// Changed telemetry source name
SystemTelemetry.recordTick("CTRADER_DEMO", 5.0)  // was "PEPPERSTONE_DEMO_CHART"

// Updated connection events
SystemTelemetry.recordConnectionEvent("CTRADER_DEMO", "WEBSOCKET_CONNECTED")
SystemTelemetry.recordConnectionEvent("CTRADER_DEMO", "CONNECTION_FAILED")
SystemTelemetry.recordConnectionEvent("CTRADER_DEMO", "CONNECTION_CLOSED")
```

## Market Data Bus - Updated Relay Order

The relays now appear in this order:

1. **Pepperstone cTrader Live** - Live trading account (Port 8082)
2. **Pepperstone cTrader Demo** - Demo account (Port 8083) ✅ NOW SHOWS ACTIVE
3. **MT5 Bridge** - MT5 platform bridge (Port 8081) ✅ NEWLY ADDED
4. **Binance USDT Futures** - Cryptocurrency pairs
5. **ASC AI Backend** - AI analysis system (Port 8000)

## How Telemetry Works

### Source Name Mapping
SystemTelemetry accepts multiple source name variants:

| Service | Accepted Source Names |
|---------|----------------------|
| cTrader Live | CTRADER_LIVE, CTRADER-LIVE, PEPPERSTONE_LIVE, MARKET |
| cTrader Demo | CTRADER_DEMO, CTRADER-DEMO, PEPPERSTONE_DEMO, DEMO |
| MT5 Bridge | MT5, MT5_BRIDGE, MT5-BRIDGE |
| Binance | BINANCE, BINANCE_USDT |
| ASC AI | ASC_AI, ASC-AI, ASC AI, BACKEND |

### Active Status Detection
A relay shows as **ACTIVE** when:
- `buffer > 0.0` OR `latency > 0.0`
- This means data has been received in the last second

A relay shows as **IDLE** when:
- `buffer == 0.0` AND `latency == 0.0`
- No data received for >1 second

### Telemetry Recording
Each service records telemetry when it receives data:

```kotlin
// Example from PepperstoneDemoChartService
SystemTelemetry.recordTick("CTRADER_DEMO", 5.0)  // 5ms latency
```

This:
1. Adds timestamp to tick queue
2. Records latency
3. Calculates throughput (ticks per second)
4. Updates buffer load (tps * 20%)

## Verification

After rebuilding and installing the app:

### Check Market Data Bus Page
You should see:
- ✅ Pepperstone cTrader Live: **ACTIVE** (if live data flowing)
- ✅ Pepperstone cTrader Demo: **ACTIVE** (if demo data flowing) - **FIXED**
- ✅ MT5 Bridge: **ACTIVE** or **IDLE** (depending on if MT5 is connected) - **NEWLY ADDED**
- ✅ Binance USDT Futures: **ACTIVE** (if Binance connected)
- ✅ ASC AI Backend: **ACTIVE** (if backend polling)

### Expected Behavior
- Green dot + "ACTIVE" = Data flowing
- Gray dot + "IDLE" = No data
- Latency shows actual ms values
- Buffer Load shows percentage (0-100%)
- Progress bars animate when active

## Testing

### Test cTrader Demo
1. Make sure cTrader Demo bridge is running (port 8083)
2. Open a chart with cTrader Demo feed
3. Check Market Data Bus - should show **ACTIVE**

### Test MT5 Bridge
1. Make sure MT5 bridge is running (port 8081)
2. Connect MT5 service
3. Check Market Data Bus - should show **ACTIVE**

### Verify Telemetry
Check logs for telemetry recording:
```
SystemTelemetry: PRICE_TICK via CTRADER_DEMO | LATENCY: 5ms
SystemTelemetry: PRICE_TICK via MT5 | LATENCY: 8ms
```

## Troubleshooting

### cTrader Demo Still Shows IDLE
1. Check if demo bridge is running: `netstat -an | findstr 8083`
2. Verify demo service is connected in app
3. Check logs for "Pepperstone Demo WebSocket connected"
4. Make sure you're viewing a chart with demo feed

### MT5 Bridge Shows IDLE
1. Check if MT5 bridge is running: `netstat -an | findstr 8081`
2. Verify MT5 service is connected
3. Check if MT5 is sending price updates
4. Look for "Parsed tick" messages in logs

### All Services Show IDLE
1. Verify IP address is correct (192.168.1.198)
2. Check all services are running
3. Restart the app
4. Check firewall isn't blocking connections

## Next Steps

1. ✅ Rebuild the app
2. ✅ Reinstall on Android device
3. ✅ Open Market Data Bus page
4. ✅ Verify all active services show green status
5. ✅ Check that MT5 Bridge appears in the list
6. ✅ Confirm cTrader Demo shows ACTIVE when data is flowing
