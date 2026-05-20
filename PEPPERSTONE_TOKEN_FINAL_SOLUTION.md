# Pepperstone cTrader Token Setup - Final Solution

## 🎯 The Problem

Pepperstone charts show prices moving at the top but no candles because:
1. Access token was invalid/expired
2. Account ID in token doesn't match the one in config

## 🔍 What We Learned

### Key Discovery
**The account ID you see in cTrader (e.g., 5288664) is NOT the same as the ctidTraderAccountId used by the API!**

When you authorize a token for account 5288664, the API assigns it a different internal ID (like 47312778). You must use the **API-assigned ID**, not the visible account number.

### How to Find the Correct ID

After generating a token, run:
```powershell
python list_accounts_clean.py
```

This will show the **actual ctidTraderAccountId** tied to your token.

## ✅ Correct Process

### Step 1: Generate Token for New Account (5288664)

Wait 5-10 minutes for rate limit to reset, then:

```powershell
.\get_token_curl.ps1
```

1. Open the authorization URL
2. **Select account 5288664** (your new demo)
3. Click "Allow access"
4. Copy the full URL immediately
5. Paste into PowerShell

### Step 2: Find the API Account ID

Update the token in `list_accounts_clean.py`:
```python
ACCESS_TOKEN = "YOUR_NEW_TOKEN_HERE"
```

Then run:
```powershell
python list_accounts_clean.py
```

Output will show:
```
Account 1:
  Account ID: XXXXXXXX  ← Use this number!
  Type: DEMO
  Broker: Unknown
```

### Step 3: Update Bridge Configuration

Edit `start_ctrader_bridge.ps1`:
```powershell
$env:CTRADER_ACCESS_TOKEN = "YOUR_NEW_TOKEN"
$env:CTRADER_ACCOUNT_ID = "XXXXXXXX"  # The ID from step 2
$env:CTRADER_HOST_TYPE = "demo"
```

### Step 4: Start Bridge

```powershell
.\start_ctrader_bridge.ps1
```

Expected output:
```
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: Account XXXXXXXX authenticated
[cTrader] symbols_loaded: Loaded 500+ symbols
✓ Bridge ready
```

### Step 5: Test in App

Open your Android app → Pepperstone charts → Should see candles!

## 📋 Current Status

- ✅ Token generation script working (`get_token_curl.ps1`)
- ✅ Account ID discovery script working (`list_accounts_clean.py`)
- ✅ Bridge script configured
- ⏳ Waiting for rate limit to reset (5-10 minutes)
- 🎯 Next: Generate token for account 5288664

## 🔧 Rate Limit Issue

cTrader API has rate limits. If you see "Too Many Attempts":
- **Wait 5-10 minutes**
- Don't retry immediately
- Use the time to prepare the next steps

## 📝 Quick Reference

| Step | Command | Purpose |
|------|---------|---------|
| 1 | `.\get_token_curl.ps1` | Generate production token |
| 2 | `python list_accounts_clean.py` | Find API account ID |
| 3 | Edit `start_ctrader_bridge.ps1` | Update token & account ID |
| 4 | `.\start_ctrader_bridge.ps1` | Start bridge |

## ⚠️ Important Notes

1. **Account ID ≠ ctidTraderAccountId**
   - What you see in cTrader: 5288664
   - What the API uses: Could be different (e.g., 47312778)
   - Always use the ID from `list_accounts_clean.py`

2. **Production vs Playground Tokens**
   - Playground tokens are for testing only
   - Use OAuth flow (`get_token_curl.ps1`) for production
   - Tokens expire after 30 days

3. **Demo vs Live Endpoint**
   - Demo accounts can be on either endpoint
   - Check which endpoint works during connection
   - Bridge auto-switches if needed

## 🎉 Success Criteria

When everything works:
- ✅ Bridge connects without errors
- ✅ Account authenticated
- ✅ Symbols loaded
- ✅ Android app shows candles (not just moving prices)
- ✅ Live price updates working

## 🕐 Next Action

**Wait 5-10 minutes**, then run:
```powershell
.\get_token_curl.ps1
```

Select account **5288664** during authorization.
