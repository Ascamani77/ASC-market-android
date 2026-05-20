# Quick Fix Steps for CANT_ROUTE_REQUEST Error

## Current Situation

**Error**: `CANT_ROUTE_REQUEST: Cannot route request`
- ✅ Application auth works (Client ID/Secret valid)
- ❌ Account auth fails (Cannot route to account 47340965)

## Root Cause

The account 47340965 might not exist on the endpoint you're connecting to. Demo accounts can exist on either:
- `demo.ctraderapi.com:5035` (demo endpoint)
- `live.ctraderapi.com:5035` (live endpoint)

You're currently connecting to **live** endpoint, but the account might be on **demo** endpoint.

## Quick Fix (3 Steps)

### Step 1: Find Which Endpoint Has Your Account

Run this command:
```powershell
python list_accounts_clean.py
```

This will show:
- Which accounts are accessible with your token
- The correct account ID to use
- Whether it's on demo or live endpoint

**Expected Output:**
```
Authorized Accounts:
Account 1:
  Account ID: 47340965
  Type: DEMO
  Broker: Pepperstone
```

### Step 2: Update Configuration

Based on Step 1 results, edit `start_ctrader_bridge.ps1`:

**If account is on DEMO endpoint:**
```powershell
$env:CTRADER_HOST_TYPE = "demo"
```

**If account is on LIVE endpoint:**
```powershell
$env:CTRADER_HOST_TYPE = "live"
```

### Step 3: Restart Bridge

```powershell
.\start_ctrader_bridge.ps1
```

## Alternative: Try Demo Endpoint First

Since you created a demo account (5288664), it's most likely on the demo endpoint. Try this quick fix:

1. Edit `start_ctrader_bridge.ps1`
2. Change line 7 to:
   ```powershell
   $env:CTRADER_HOST_TYPE = "demo"
   ```
3. Save and run: `.\start_ctrader_bridge.ps1`

## If Still Failing

### Option A: Verify Token/Account Match

The token was authorized for account **5288664**, but you're trying to use account **47340965**. These should be the same account (visible ID vs API ID), but verify:

```powershell
python list_accounts_clean.py
```

Check if the output shows account 47340965. If not, use the account ID that appears.

### Option B: Generate Fresh Token

If the account list is empty or doesn't show 47340965:

1. Run: `.\get_token_curl.ps1`
2. When authorizing, select account **5288664**
3. Complete authorization within 60 seconds
4. The script will auto-update the configuration
5. Run: `.\start_ctrader_bridge.ps1`

### Option C: Check Application Status

Visit: https://id.ctrader.com/my/settings/openapi

Verify:
- Application status is "Active" or "Approved" (not "Pending")
- Redirect URI includes: `http://localhost:8888/callback`
- Application has "trading" scope enabled

## Expected Success Output

When working correctly, you should see:
```
[cTrader] connected: Connected to cTrader Open API
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: cTrader account 47340965 authenticated
[cTrader] symbols_loaded: Loaded 50+ broker symbols
```

## Still Not Working?

If you've tried all steps and still get CANT_ROUTE_REQUEST:

1. **Contact Pepperstone Support**
   - Email: support@pepperstone.com
   - Mention: "CANT_ROUTE_REQUEST error when authenticating cTrader API account 47340965"
   - Ask them to verify the account is accessible via API

2. **Use Alternative Broker (Temporary)**
   - Your app already has working Binance charts
   - Deriv integration is available
   - Use these while resolving Pepperstone issues

## Technical Details

### Current Configuration
```
Client ID: 27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s
Access Token: mW5eKJ0xZygSj4BnNvqlteOc_opHJ0EM9Qtcx8RvOfo
Account (visible): 5288664
Account (API): 47340965
Host Type: live (currently set)
```

### What CANT_ROUTE_REQUEST Means

This error occurs when:
1. The cTrader API server receives your request
2. It validates your credentials (passes ✅)
3. It tries to route the request to account 47340965
4. The routing fails because:
   - Account doesn't exist on that endpoint
   - Account is not linked to your access token
   - Account is disabled or archived
   - Internal routing configuration issue

The fact that **application auth succeeds** but **account auth fails** confirms the credentials are valid, but there's an account-specific routing problem.
