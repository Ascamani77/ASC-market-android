# cTrader Live and Demo Bridges Setup

## Overview

You now have **two separate cTrader bridges** that can run simultaneously:
- **Live Bridge** (port 8082) - Connects to Pepperstone LIVE endpoint
- **Demo Bridge** (port 8083) - Connects to Pepperstone DEMO endpoint

Each bridge is **fixed to its endpoint** - no more automatic switching between live and demo.

## What Changed

### 1. Removed Endpoint Switching
- ❌ Removed automatic fallback from demo to live
- ❌ Removed `reconnect_with_endpoint()` function
- ✅ Each bridge is now fixed to its configured endpoint
- ✅ Clearer error messages when credentials don't match endpoint

### 2. Separate Containers
- **ctrader-bridge-live**: Port 8082, connects to `live.ctraderapi.com`
- **ctrader-bridge-demo**: Port 8083, connects to `demo.ctraderapi.com`

### 3. Independent Credentials
Each bridge uses its own set of credentials:
- Live bridge needs LIVE account credentials
- Demo bridge needs DEMO account credentials

## Quick Start

### Option 1: Interactive Script (Recommended)
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_ctrader_live_and_demo.ps1
```

This script will:
1. Prompt for LIVE credentials (or skip)
2. Prompt for DEMO credentials (or skip)
3. Start the selected bridges
4. Show status and logs

### Option 2: Manual Docker Commands

**Start LIVE bridge:**
```powershell
docker run -d --name ctrader-bridge-live --restart unless-stopped -p 8082:8082 `
  -e CTRADER_CLIENT_ID="YOUR_LIVE_CLIENT_ID" `
  -e CTRADER_CLIENT_SECRET="YOUR_LIVE_CLIENT_SECRET" `
  -e CTRADER_ACCESS_TOKEN="YOUR_LIVE_ACCESS_TOKEN" `
  -e CTRADER_ACCOUNT_ID="YOUR_LIVE_ACCOUNT_ID" `
  -e CTRADER_HOST_TYPE="live" `
  -e CTRADER_BRIDGE_HOST="0.0.0.0" `
  -e CTRADER_BRIDGE_PORT="8082" `
  myrealapp-ctrader-bridge:latest
```

**Start DEMO bridge:**
```powershell
docker run -d --name ctrader-bridge-demo --restart unless-stopped -p 8083:8083 `
  -e CTRADER_CLIENT_ID="YOUR_DEMO_CLIENT_ID" `
  -e CTRADER_CLIENT_SECRET="YOUR_DEMO_CLIENT_SECRET" `
  -e CTRADER_ACCESS_TOKEN="YOUR_DEMO_ACCESS_TOKEN" `
  -e CTRADER_ACCOUNT_ID="YOUR_DEMO_ACCOUNT_ID" `
  -e CTRADER_HOST_TYPE="demo" `
  -e CTRADER_BRIDGE_HOST="0.0.0.0" `
  -e CTRADER_BRIDGE_PORT="8083" `
  myrealapp-ctrader-bridge:latest
```

## Getting Credentials

### For LIVE Account
1. Go to: https://openapi.ctrader.com/
2. Sign in with your Pepperstone LIVE credentials
3. Navigate to "Applications"
4. Create or select your application
5. Copy:
   - Client ID
   - Client Secret
   - Access Token
   - Account ID

### For DEMO Account
1. Go to: https://openapi.ctrader.com/
2. Sign in with your Pepperstone DEMO credentials
3. Follow same steps as above
4. Make sure you're using DEMO credentials, not LIVE

**Important**: LIVE and DEMO credentials are completely separate!

## Managing Bridges

### Check Status
```powershell
docker ps --filter "name=ctrader-bridge"
```

### View Logs
```powershell
# Live bridge
docker logs -f ctrader-bridge-live

# Demo bridge
docker logs -f ctrader-bridge-demo
```

### Stop Bridges
```powershell
# Stop both
docker stop ctrader-bridge-live ctrader-bridge-demo

# Stop only live
docker stop ctrader-bridge-live

# Stop only demo
docker stop ctrader-bridge-demo
```

### Restart Bridges
```powershell
# Restart both
docker restart ctrader-bridge-live ctrader-bridge-demo

# Restart only live
docker restart ctrader-bridge-live

# Restart only demo
docker restart ctrader-bridge-demo
```

### Remove Bridges
```powershell
# Remove both (stops first if running)
docker stop ctrader-bridge-live ctrader-bridge-demo
docker rm ctrader-bridge-live ctrader-bridge-demo
```

## Connecting Your App

### Live Data (Port 8082)
```kotlin
val cTraderLiveUrl = "ws://YOUR_PC_IP:8082"
// Connect to this for live trading data
```

### Demo Data (Port 8083)
```kotlin
val cTraderDemoUrl = "ws://YOUR_PC_IP:8083"
// Connect to this for demo/testing data
```

Your app can connect to **both simultaneously** if needed!

## Troubleshooting

### Error: "Account auth timed out"
**Cause**: ACCESS_TOKEN expired or wrong credentials for endpoint

**Solution**:
1. Get fresh ACCESS_TOKEN from Pepperstone portal
2. Make sure you're using LIVE credentials for live bridge
3. Make sure you're using DEMO credentials for demo bridge
4. Restart the bridge with new token

### Error: "CANT_ROUTE_REQUEST"
**Cause**: Using wrong credentials for the endpoint

**Solution**:
- If using live bridge: Use LIVE credentials
- If using demo bridge: Use DEMO credentials
- Don't mix live and demo credentials!

### Error: "Port already in use"
**Cause**: Another container or process is using the port

**Solution**:
```powershell
# Check what's using the port
netstat -ano | findstr :8082
netstat -ano | findstr :8083

# Stop the old container
docker stop ctrader-bridge-live
docker rm ctrader-bridge-live
```

### Bridge Keeps Disconnecting
**Cause**: Rate limiting or network issues

**Solution**:
- Heartbeat interval is set to 120s (should be fine)
- Check your internet connection
- Check Pepperstone API status
- Wait 5 minutes if rate limited

### No Live Data in App
**Checklist**:
1. ✅ Bridge container is running: `docker ps`
2. ✅ Bridge is authenticated: `docker logs ctrader-bridge-live | findstr "authenticated"`
3. ✅ App is connected: `docker logs ctrader-bridge-live | findstr "Android client connected"`
4. ✅ Symbols subscribed: `docker logs ctrader-bridge-live | findstr "subscribed"`
5. ✅ App is using correct IP and port

## Files Created

- `ctrader_bridge.py` - Updated bridge (no endpoint switching)
- `docker-compose.ctrader-both.yml` - Compose file for both bridges
- `start_ctrader_live_and_demo.ps1` - Interactive setup script
- `CTRADER_LIVE_AND_DEMO_SETUP.md` - This documentation

## Architecture

```
┌─────────────────────────────────────────────────┐
│           Pepperstone cTrader API               │
│                                                 │
│  ┌──────────────────┐  ┌──────────────────┐   │
│  │  LIVE Endpoint   │  │  DEMO Endpoint   │   │
│  │ live.ctrader...  │  │ demo.ctrader...  │   │
│  └────────┬─────────┘  └────────┬─────────┘   │
└───────────┼────────────────────┼───────────────┘
            │                    │
            │                    │
    ┌───────▼────────┐   ┌───────▼────────┐
    │  Live Bridge   │   │  Demo Bridge   │
    │   Port 8082    │   │   Port 8083    │
    │   (Docker)     │   │   (Docker)     │
    └───────┬────────┘   └───────┬────────┘
            │                    │
            │                    │
    ┌───────▼────────────────────▼────────┐
    │         Your Android App            │
    │  (Can connect to both bridges)      │
    └─────────────────────────────────────┘
```

## Benefits

✅ **No More Confusion**: Each bridge is clearly live or demo
✅ **No Automatic Switching**: Predictable behavior
✅ **Run Both**: Test with demo while live is running
✅ **Independent**: One bridge failing doesn't affect the other
✅ **Clear Errors**: Know immediately if credentials are wrong
✅ **Easy Management**: Separate containers, separate logs

## Next Steps

1. **Get your credentials** from Pepperstone portal (both live and demo)
2. **Run the setup script**: `.\start_ctrader_live_and_demo.ps1`
3. **Verify connection**: Check logs for "authenticated" messages
4. **Update your app**: Connect to the appropriate bridge (8082 or 8083)
5. **Test**: Verify live data is flowing

---

**Status**: ✅ Code updated, Docker image rebuilt
**Action Required**: Get credentials and start bridges
