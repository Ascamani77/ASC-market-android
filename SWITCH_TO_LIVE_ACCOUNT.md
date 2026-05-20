# Switch to Live Account 1360716 - Complete Guide

## Problem Summary

You were trying to use **demo account 5288664** (API ID: 47340965), but getting `CANT_ROUTE_REQUEST` errors. The solution is to switch to your **live account 1360716** instead.

## Why This Will Work

- Live accounts are more stable and have better API routing
- Your live account 1360716 exists on the live endpoint
- The CANT_ROUTE_REQUEST error was likely due to demo account routing issues

## Step-by-Step Instructions

### Step 1: Generate Token for Live Account

Run this command:
```powershell
.\get_live_token.ps1
```

**CRITICAL**: When the browser opens:
1. Login to cTrader
2. **SELECT ACCOUNT 1360716** (your live account)
3. Verify it shows as "LIVE" account (not demo)
4. Click "Allow access"
5. Copy the full URL from the browser
6. Paste it into the PowerShell window **IMMEDIATELY** (within 60 seconds)

**Expected Output:**
```
[SUCCESS] Live account token received!
Access Token:  mW5eKJ0xZygSj4BnNvqlteOc...
[OK] Updated start_ctrader_bridge.ps1
```

### Step 2: Get API Account ID

The visible account ID (1360716) is different from the API's internal ID. Run:
```powershell
python get_live_account_id.py
```

**Expected Output:**
```
Account 1:
  API Account ID (ctidTraderAccountId): 47312778
  Type: LIVE
  Broker: Pepperstone

🎯 LIVE ACCOUNT FOUND!
✅ USE THIS IN YOUR CONFIG: 47312778
```

**IMPORTANT**: Copy the API Account ID number (it will be different from 1360716).

### Step 3: Update Configuration

Edit `start_ctrader_bridge.ps1` and update these lines:

```powershell
$env:CTRADER_ACCESS_TOKEN = "YOUR_NEW_TOKEN_HERE"
$env:CTRADER_ACCOUNT_ID = "API_ACCOUNT_ID_FROM_STEP_2"
$env:CTRADER_HOST_TYPE = "live"
```

**Example:**
```powershell
$env:CTRADER_ACCESS_TOKEN = "mW5eKJ0xZygSj4BnNvqlteOc_opHJ0EM9Qtcx8RvOfo"
$env:CTRADER_ACCOUNT_ID = "47312778"
$env:CTRADER_HOST_TYPE = "live"
```

### Step 4: Start Bridge

```powershell
.\start_ctrader_bridge.ps1
```

**Expected Success Output:**
```
[cTrader] connected: Connected to cTrader Open API
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: cTrader account 47312778 authenticated
[cTrader] symbols_loaded: Loaded 50+ broker symbols
```

### Step 5: Test in Your App

1. Open your Android app
2. Navigate to Pepperstone chart
3. You should now see:
   - ✅ Candles displaying
   - ✅ Price updates at top
   - ✅ Volume bars
   - ✅ Real-time data streaming

## Troubleshooting

### Issue: "Too Many Attempts" when generating token

**Solution**: Wait 5-10 minutes before trying again. cTrader rate-limits token requests.

### Issue: No accounts found in Step 2

**Cause**: Token was not authorized for the live account.

**Solution**: 
1. Repeat Step 1
2. Make absolutely sure you select account **1360716** when authorizing
3. Verify it shows as "LIVE" account

### Issue: Still getting CANT_ROUTE_REQUEST

**Possible Causes**:
1. Wrong API account ID - verify you used the ID from Step 2, not 1360716
2. Token expired - tokens expire in 60 seconds during authorization
3. Application not approved - check https://id.ctrader.com/my/settings/openapi

**Solution**:
1. Verify configuration in `start_ctrader_bridge.ps1`
2. Ensure HOST_TYPE is "live"
3. Ensure ACCOUNT_ID is the API ID (from Step 2), not 1360716
4. Generate fresh token if needed

### Issue: Application auth fails

**Cause**: Client ID or Secret is invalid.

**Solution**: Verify credentials at https://id.ctrader.com/my/settings/openapi

## Important Notes

### Live vs Demo Accounts

- **Live Account 1360716**: Real money account, stable API access
- **Demo Account 5288664**: Practice account, may have routing issues

### Account ID Mapping

- **Visible ID**: What you see in cTrader (e.g., 1360716)
- **API ID**: Internal ID used by API (e.g., 47312778)
- Always use the **API ID** in your configuration

### Token Expiration

- Authorization codes: 60 seconds
- Access tokens: 30 days
- Refresh tokens: Can be used to get new access tokens

## Quick Reference

### Files Created
- `get_live_token.ps1` - Generate token for live account
- `get_live_account_id.py` - Find API account ID
- `start_ctrader_bridge.ps1` - Bridge startup script (update this)

### Configuration Location
- Bridge config: `start_ctrader_bridge.ps1`
- Token storage: `ctrader_tokens_live.json`

### Endpoints
- Live endpoint: `live.ctraderapi.com:5035`
- Demo endpoint: `demo.ctraderapi.com:5035`

## Success Checklist

- [ ] Generated token for account 1360716
- [ ] Found API account ID (different from 1360716)
- [ ] Updated `start_ctrader_bridge.ps1` with new token and API ID
- [ ] Set HOST_TYPE to "live"
- [ ] Bridge starts without errors
- [ ] Application authenticated
- [ ] Account authenticated
- [ ] Symbols loaded
- [ ] Candles display in app

## Next Steps After Success

Once the bridge is working:

1. **Test Trading Functions**
   - Place test orders (small amounts!)
   - Close positions
   - Verify balance updates

2. **Monitor Stability**
   - Check for disconnections
   - Verify data accuracy
   - Monitor error logs

3. **Set Up Auto-Restart**
   - Create Windows service or scheduled task
   - Handle reconnections gracefully
   - Log errors for debugging

## Support

If you still have issues after following all steps:

1. **Check Application Status**
   - Visit: https://id.ctrader.com/my/settings/openapi
   - Verify application is "Active" or "Approved"

2. **Contact Pepperstone**
   - Email: support@pepperstone.com
   - Mention: "API access for live account 1360716"

3. **Alternative Solution**
   - Use Binance charts (already working in your app)
   - Use Deriv integration (available)
