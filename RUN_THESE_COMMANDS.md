# Fix Pepperstone Chart - Run These Commands

## The Problem
- Demo account 5288664 (API ID: 47340965) is getting CANT_ROUTE_REQUEST errors
- Solution: Switch to live account 1360716

## The Solution (3 Commands)

### Command 1: Generate Token for Live Account
```powershell
.\get_live_token.ps1
```

**What to do:**
1. Browser will open automatically
2. Login to cTrader
3. **SELECT ACCOUNT 1360716** (LIVE account)
4. Click "Allow access"
5. Copy the full URL from browser
6. Paste into PowerShell **within 60 seconds**

---

### Command 2: Get API Account ID
```powershell
python get_live_account_id.py
```

**What to look for:**
```
API Account ID (ctidTraderAccountId): 47312778
Type: LIVE
```

**Copy the API Account ID** (will be different from 1360716)

---

### Command 3: Start Bridge
```powershell
.\start_ctrader_bridge.ps1
```

**Expected output:**
```
✅ Connected to cTrader Open API
✅ Application authenticated
✅ Account authenticated
✅ Symbols loaded
```

---

## If Command 2 Shows Wrong Account

The token might still be for the demo account. If you see:
```
Type: DEMO
```

Then repeat Command 1 and make absolutely sure you select account **1360716** (LIVE).

---

## Success = Candles Display in App

Once all 3 commands succeed:
1. Open your Android app
2. Go to Pepperstone chart
3. You should see candles displaying with real-time updates

---

## Quick Troubleshooting

**"Too Many Attempts"**: Wait 5-10 minutes, then retry Command 1

**"No accounts found"**: Token wasn't authorized correctly, retry Command 1

**"CANT_ROUTE_REQUEST"**: Check that `start_ctrader_bridge.ps1` has:
- Correct API Account ID (from Command 2)
- HOST_TYPE = "live"
- New access token (from Command 1)

---

## That's It!

Just run these 3 commands in order. The scripts will automatically update your configuration.
