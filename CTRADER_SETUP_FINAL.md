# cTrader Live & Demo Bridges - Final Setup

## Summary

You now have **two separate cTrader bridges** that run independently:

| Bridge | Port | Account ID | Endpoint | Purpose |
|--------|------|------------|----------|---------|
| **LIVE** | 8082 | 47341092 | live.ctraderapi.com | Real trading |
| **DEMO** | 8083 | 5288664 | demo.ctraderapi.com | Testing |

## Shared Credentials

Both bridges use the **same** CLIENT_ID and CLIENT_SECRET:
- **Client ID**: `27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s`
- **Client Secret**: `loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty`

## Different Access Tokens

Each bridge needs its **own** ACCESS_TOKEN:
- **LIVE token**: For account 47341092 (get from live portal)
- **DEMO token**: For account 5288664 (get from demo portal)

## Quick Start

### Start Demo Bridge
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_demo_bridge.ps1
```

This will:
1. Prompt for DEMO access token
2. Start demo bridge on port 8083
3. Connect to demo.ctraderapi.com
4. Show authentication status

### Start Live Bridge
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_live_bridge.ps1
```

This will:
1. Prompt for LIVE access token
2. Start live bridge on port 8082
3. Connect to live.ctraderapi.com
4. Show authentication status

### Start Both
You can run both scripts to have both bridges running simultaneously!

## Getting Access Tokens

### For DEMO (Account 5288664)
1. Go to: https://openapi.ctrader.com/
2. **Sign in with DEMO credentials** (not live!)
3. Navigate to "Applications"
4. Select your application
5. Click "Generate Token" or "Refresh Token"
6. Copy the token
7. Run `.\start_demo_bridge.ps1` and paste the token

### For LIVE (Account 47341092)
1. Go to: https://openapi.ctrader.com/
2. **Sign in with LIVE credentials** (not demo!)
3. Navigate to "Applications"
4. Select your application
5. Click "Generate Token" or "Refresh Token"
6. Copy the token
7. Run `.\start_live_bridge.ps1` and paste the token

**Important**: Make sure you're signed into the correct account (live vs demo) when generating tokens!

## Connecting Your App

### Demo Data (Port 8083)
```kotlin
// In your Android app
val demoUrl = "ws://YOUR_PC_IP:8083"
cTraderDemoClient.connect(demoUrl)
```

### Live Data (Port 8082)
```kotlin
// In your Android app
val liveUrl = "ws://YOUR_PC_IP:8082"
cTraderLiveClient.connect(liveUrl)
```

Replace `YOUR_PC_IP` with your computer's IP address (e.g., `192.168.1.100`).

## Managing Bridges

### Check Status
```powershell
docker ps --filter "name=ctrader-bridge"
```

Expected output:
```
NAMES                    STATUS              PORTS
ctrader-bridge-live      Up 5 minutes        0.0.0.0:8082->8082/tcp
ctrader-bridge-demo      Up 3 minutes        0.0.0.0:8083->8083/tcp
```

### View Logs
```powershell
# Demo bridge
docker logs -f ctrader-bridge-demo

# Live bridge
docker logs -f ctrader-bridge-live
```

Look for these success messages:
```
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: cTrader account XXXXX authenticated
[cTrader] symbols_loaded: Loaded 1889 broker symbols
```

### Stop Bridges
```powershell
# Stop demo
docker stop ctrader-bridge-demo

# Stop live
docker stop ctrader-bridge-live

# Stop both
docker stop ctrader-bridge-demo ctrader-bridge-live
```

### Restart Bridges
```powershell
# Restart demo
docker restart ctrader-bridge-demo

# Restart live
docker restart ctrader-bridge-live
```

### Remove Bridges
```powershell
# Remove demo
docker stop ctrader-bridge-demo
docker rm ctrader-bridge-demo

# Remove live
docker stop ctrader-bridge-live
docker rm ctrader-bridge-live
```

## Troubleshooting

### Error: "Account auth timed out"
**Cause**: ACCESS_TOKEN expired

**Solution**:
1. Get a fresh token from Pepperstone portal
2. Stop the bridge: `docker stop ctrader-bridge-demo` (or live)
3. Run the start script again with the new token

### Error: "CANT_ROUTE_REQUEST"
**Cause**: Using wrong token for the endpoint

**Solution**:
- Demo bridge needs DEMO token (from demo portal)
- Live bridge needs LIVE token (from live portal)
- Don't mix them up!

### Error: "Port already in use"
**Cause**: Another container is using the port

**Solution**:
```powershell
# For demo (port 8083)
docker stop ctrader-bridge-demo
docker rm ctrader-bridge-demo

# For live (port 8082)
docker stop ctrader-bridge-live
docker rm ctrader-bridge-live
```

### No Data in App
**Checklist**:
1. ✅ Bridge is running: `docker ps`
2. ✅ Bridge is authenticated: `docker logs ctrader-bridge-demo | findstr authenticated`
3. ✅ App connected to correct port (8082 for live, 8083 for demo)
4. ✅ App using correct IP address (not localhost if on physical device)

### Token Keeps Expiring
**Solution**: Implement automatic token refresh using OAuth2 refresh tokens (advanced).

For now, just get a new token when it expires (usually 24-48 hours).

## Files Created

| File | Purpose |
|------|---------|
| `ctrader_bridge.py` | Updated bridge code (no endpoint switching) |
| `start_demo_bridge.ps1` | Start demo bridge (port 8083) |
| `start_live_bridge.ps1` | Start live bridge (port 8082) |
| `get_demo_access_token.ps1` | Helper to get demo token |
| `docker-compose.ctrader-both.yml` | Docker Compose for both bridges |
| `CTRADER_SETUP_FINAL.md` | This documentation |

## Architecture

```
┌─────────────────────────────────────────────────┐
│         Pepperstone cTrader API                 │
│                                                 │
│  ┌──────────────────┐  ┌──────────────────┐   │
│  │  LIVE Endpoint   │  │  DEMO Endpoint   │   │
│  │ live.ctrader...  │  │ demo.ctrader...  │   │
│  │  Account:        │  │  Account:        │   │
│  │  47341092        │  │  5288664         │   │
│  └────────┬─────────┘  └────────┬─────────┘   │
└───────────┼────────────────────┼───────────────┘
            │                    │
            │ LIVE Token         │ DEMO Token
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
    │                                     │
    │  Live Client  ←→  Demo Client       │
    │  (port 8082)      (port 8083)       │
    └─────────────────────────────────────┘
```

## Next Steps

1. **Get DEMO token** from Pepperstone demo portal
2. **Run**: `.\start_demo_bridge.ps1`
3. **Verify**: Check logs for "authenticated" messages
4. **Test**: Connect your app to `ws://YOUR_IP:8083`
5. **Get LIVE token** when ready for live trading
6. **Run**: `.\start_live_bridge.ps1`
7. **Connect**: Your app to `ws://YOUR_IP:8082`

## Important Notes

✅ **Same CLIENT_ID and SECRET** for both bridges
✅ **Different ACCESS_TOKENs** for each account
✅ **Different ports** (8082 for live, 8083 for demo)
✅ **No automatic switching** - each bridge is fixed to its endpoint
✅ **Independent operation** - one failing doesn't affect the other
✅ **Can run both** simultaneously for testing

---

**Status**: ✅ Setup complete, Docker image rebuilt
**Action**: Get demo token and run `.\start_demo_bridge.ps1`
