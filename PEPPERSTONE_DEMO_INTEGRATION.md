# Pepperstone Demo Integration - Implementation Guide

## Overview

Adding "Pepperstone Demo" as a completely separate broker option in the app, with its own services and configuration.

## Files Created/Modified

### 1. ChartFeedType.kt ✅ DONE
- Added `PEPPERSTONE_DEMO` enum value
- Added demo symbol mappings
- Updated symbol normalization logic

### 2. PepperstoneDemoChartService.kt ✅ DONE
- Created new chart service for demo account
- Connects to port 8083 (demo bridge)
- Publishes ticks with source "pepperstone_demo"
- Separate from live Pepperstone service

### 3. CTraderDemoService.kt - TODO
- Trading service for demo account
- Connects to demo bridge (port 8083)
- Handles demo account positions and orders

### 4. NetworkConfig.kt - TODO
- Add `cTraderDemoHost()` method
- Add `cTraderDemoPort()` method
- Read from CTRADER_DEMO_* properties

### 5. TradingApp.kt - TODO
- Add demo service initialization
- Add demo connection logic
- Add demo trading logic
- Update broker selection UI

## Configuration

### local.properties
```properties
# Live Account (Port 8082)
CTRADER_HOST_TYPE=live
CTRADER_ACCOUNT_ID=47341092
CTRADER_BRIDGE_HOST=10.164.138.133
CTRADER_BRIDGE_PORT=8082
CTRADER_ACCESS_TOKEN=bz5h7SrzyS_xb8udgcckRcGJU2D9FgIMCAkegWiRFeE

# Demo Account (Port 8083)
CTRADER_DEMO_HOST_TYPE=demo
CTRADER_DEMO_ACCOUNT_ID=47340965
CTRADER_DEMO_BRIDGE_HOST=10.164.138.133
CTRADER_DEMO_BRIDGE_PORT=8083
CTRADER_DEMO_ACCESS_TOKEN=F7iMSRvGWj7rfxPwjpPCCC5rf1mHrANVjcYjzYZvWJY
```

## Bridge Setup

### Live Bridge (Port 8082)
```bash
docker run -d --name ctrader-bridge-live -p 8082:8082 \
  -e CTRADER_ACCOUNT_ID=47341092 \
  -e CTRADER_HOST_TYPE=live \
  myrealapp-ctrader-bridge:latest
```

### Demo Bridge (Port 8083) ✅ RUNNING
```bash
docker run -d --name ctrader-bridge-demo -p 8083:8083 \
  -e CTRADER_ACCOUNT_ID=47340965 \
  -e CTRADER_HOST_TYPE=demo \
  myrealapp-ctrader-bridge:latest
```

## User Experience

When user selects broker in app:
- **Exness** → Exness MT5 (port 5555)
- **Pepperstone cTrader** → Live account (port 8082)
- **Pepperstone Demo** → Demo account (port 8083) ⭐ NEW
- **Binance** → Binance Futures
- **Binance Connect** → Binance view-only

## Next Steps

1. ✅ Create CTraderDemoService.kt
2. ✅ Update NetworkConfig.kt with demo methods
3. ✅ Update TradingApp.kt to handle PEPPERSTONE_DEMO
4. ✅ Test demo connection
5. ✅ Rebuild app

## Testing Checklist

- [ ] Demo bridge connects successfully
- [ ] Live prices flow from demo bridge
- [ ] Demo account balance shows $50,000
- [ ] Can place demo orders
- [ ] Demo positions update correctly
- [ ] Switching between live and demo works
- [ ] Both can run simultaneously

## Status

**Current**: Creating service files
**Next**: Update NetworkConfig and TradingApp
**ETA**: 5-10 minutes for complete implementation
