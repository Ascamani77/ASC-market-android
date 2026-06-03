# Get Demo Token - START HERE

## The Problem

The cTrader OAuth portal (https://openapi.ctrader.com/) is not accessible, so you can't manage your application settings or generate tokens through the web interface.

## The Solution

Use the **Direct OAuth Flow** method that doesn't require portal access.

---

## Quick Start

### Option 1: Check Ports First (Recommended)

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\check_ports.ps1
```

This will:
- Check which ports are available
- Show what's using port 8080
- Optionally kill the process using port 8080
- Recommend which script to use

### Option 2: Use Port 3000 (If 8080 is Busy)

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\get_demo_token_port3000.ps1
```

### Option 3: Use Port 8080 (If Available)

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\get_demo_token_direct.ps1
```

All scripts will:
1. ✅ Start a local HTTP server (no portal needed)
2. ✅ Open your browser for authorization
3. ✅ Automatically receive the OAuth callback
4. ✅ Exchange the code for an access token
5. ✅ Save the token to `ctrader_demo_token.json`
6. ✅ Optionally start the demo bridge

---

## What You'll Do

1. **Run the script** (command above)
2. **Browser opens** - Sign in with **DEMO credentials**
3. **Click "Allow access"** - Authorize the app
4. **Automatic redirect** - Script receives the token
5. **Done!** - Token saved and bridge can start

---

## Important Notes

### ⚠️ Use DEMO Credentials
When the browser opens, make sure you sign in with your **DEMO** account credentials, not your live account!

### ⚠️ Port 8080 Must Be Free
The script uses port 8080 for the local server. If you get an error, close any apps using that port.

### ⚠️ Complete Quickly
Authorization codes expire in ~10 minutes. Complete the process without long delays.

---

## Your Demo Account

- **Account ID**: `47340965` (ctidTraderAccountId)
- **Account Number**: `5288664` (display only)
- **Balance**: $50,000 (demo)
- **Bridge Port**: `8083`

---

## After Getting Token

The script will ask if you want to start the demo bridge. Say **yes** (y).

Then verify it's working:
```powershell
docker logs -f ctrader-bridge-demo
```

Look for:
```
[cTrader] application_authenticated
[cTrader] account_authenticated: cTrader account 47340965 authenticated
[cTrader] symbols_loaded: Loaded 1889 broker symbols
```

---

## If It Doesn't Work

See `ALTERNATIVE_TOKEN_METHODS.md` for other methods:
- Manual browser method (copy code from URL)
- Contact Pepperstone support
- Use cTrader desktop app
- Check for old token files

---

## Files You Have

| File | Purpose |
|------|---------|
| `check_ports.ps1` | **START HERE** - Check port availability |
| `get_demo_token_port3000.ps1` | Get token using port 3000 |
| `get_demo_token_direct.ps1` | Get token using port 8080 |
| `start_demo_bridge.ps1` | Start bridge with existing token |
| `check_demo_readiness.ps1` | Check if everything is ready |
| `ALTERNATIVE_TOKEN_METHODS.md` | Other ways to get token |
| `DEMO_TOKEN_QUICKSTART.md` | Quick start guide |
| `GET_DEMO_TOKEN_MANUAL.md` | Detailed manual |

---

## Ready?

**Step 1:** Check which ports are available:
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\check_ports.ps1
```

**Step 2:** Run the recommended script (port 3000 or 8080):
```powershell
.\get_demo_token_port3000.ps1
# OR
.\get_demo_token_direct.ps1
```

The script will guide you through the rest!

---

## Questions?

- **"What if port 8080 is in use?"** - Close apps using that port, or edit the script to use a different port
- **"What if the authorization fails?"** - Try again, make sure you're using DEMO credentials
- **"What if the token expires?"** - Run the script again to get a new token (tokens last ~24 hours)
- **"Can I use my live token?"** - No, you need a separate demo token for the demo account

---

**Status**: Ready to get your demo token
**Action**: Run `.\get_demo_token_direct.ps1`
