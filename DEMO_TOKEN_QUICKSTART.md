# Demo Token Quick Start Guide

## What You Need

You need a **DEMO access token** to connect your demo bridge to Pepperstone cTrader.

Your demo account details:
- **Account ID**: `47340965` (ctidTraderAccountId)
- **Account Number**: `5288664` (display only)
- **Balance**: $50,000 (demo)

## Quick Start (3 Steps)

### Step 1: Add Redirect URI

1. Go to: https://id.ctrader.com/my/settings/openapi/applications
2. Sign in with your **DEMO** credentials
3. Find your application (Client ID starts with `27391_...`)
4. Add redirect URI: `http://localhost:8080`
5. Save

### Step 2: Get Token

Run this script:
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\get_demo_token_simple.ps1
```

The script will:
1. Ask for your redirect URI (enter: `http://localhost:8080`)
2. Open a browser for you to authorize
3. **Sign in with DEMO credentials** (important!)
4. Click "Allow access"
5. Copy the redirect URL from browser
6. Paste it back into PowerShell
7. Get your access token
8. Optionally start the demo bridge

### Step 3: Verify

Check if the bridge is running:
```powershell
docker logs -f ctrader-bridge-demo
```

Look for:
```
[cTrader] application_authenticated
[cTrader] account_authenticated: cTrader account 47340965 authenticated
[cTrader] symbols_loaded: Loaded 1889 broker symbols
```

## If You Already Have a Token

If you already have a demo access token from somewhere else:

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_demo_bridge.ps1
```

Paste your token when prompted.

## Troubleshooting

### "Invalid redirect URI"
- Make sure you added `http://localhost:8080` to your app settings
- Use the exact same URI in the script

### "Authorization code expired"
- Authorization codes expire in ~10 minutes
- Run the script again and complete it quickly

### "Account auth timed out"
- Your token may be expired
- Get a new token using the script above

### "Can't find application"
- Make sure you're signed in with **DEMO** credentials
- Not live credentials!

## What Happens Next

Once the demo bridge is running:
1. It connects to `demo.ctraderapi.com`
2. Authenticates with account `47340965`
3. Loads all available symbols
4. Serves live demo data on port `8083`

Your Android app can connect to:
```
ws://YOUR_PC_IP:8083
```

Replace `YOUR_PC_IP` with your computer's IP address (e.g., `192.168.1.100`).

## Files Created

- `ctrader_demo_token.json` - Your access token (keep this safe!)
- Docker container: `ctrader-bridge-demo` (port 8083)

## Useful Commands

```powershell
# View logs
docker logs -f ctrader-bridge-demo

# Check status
docker ps | findstr demo

# Restart bridge
docker restart ctrader-bridge-demo

# Stop bridge
docker stop ctrader-bridge-demo

# Remove bridge
docker stop ctrader-bridge-demo
docker rm ctrader-bridge-demo
```

## Token Lifespan

- Access tokens expire after ~24 hours
- When expired, get a new token using `.\get_demo_token_simple.ps1`
- Or implement refresh token logic (advanced)

## Next Steps

After demo bridge is working:
1. ✅ Test with your Android app
2. ✅ Verify live demo data is flowing
3. ✅ When ready, get a LIVE token for real trading
4. ✅ Run `.\start_live_bridge.ps1` for live data

---

**Ready?** Run `.\get_demo_token_simple.ps1` to get started!
