# Pepperstone Chart Fix - Checklist

## Pre-Flight Check

Before starting, verify you have:
- [ ] Live account 1360716 exists and is active
- [ ] PowerShell access
- [ ] Python installed
- [ ] Internet connection
- [ ] 5-10 minutes available

---

## Step 1: Generate Token

### Command
```powershell
.\get_live_token.ps1
```

### Checklist
- [ ] Script runs without errors
- [ ] Browser opens automatically
- [ ] Logged into cTrader successfully
- [ ] **Selected account 1360716 (LIVE)** ⚠️ CRITICAL
- [ ] Clicked "Allow access"
- [ ] Copied full URL from browser
- [ ] Pasted into PowerShell within 60 seconds
- [ ] Saw "[SUCCESS] Live account token received!"
- [ ] File `ctrader_tokens_live.json` created
- [ ] Script updated `start_ctrader_bridge.ps1`

### If Failed
- [ ] Got "Too Many Attempts"? → Wait 5-10 minutes
- [ ] Token expired? → Retry, paste faster (within 60 seconds)
- [ ] Wrong account selected? → Retry, select 1360716

---

## Step 2: Get API Account ID

### Command
```powershell
python get_live_account_id.py
```

### Checklist
- [ ] Script runs without errors
- [ ] Connected to cTrader API
- [ ] Application authenticated
- [ ] Account list received
- [ ] **Account type shows "LIVE"** ⚠️ CRITICAL
- [ ] API Account ID displayed (e.g., 47312778)
- [ ] Copied the API Account ID number

### Expected Output
```
Account 1:
  API Account ID (ctidTraderAccountId): 47312778
  Type: LIVE
  Broker: Pepperstone

🎯 LIVE ACCOUNT FOUND!
```

### If Failed
- [ ] Shows "DEMO" instead of "LIVE"? → Retry Step 1, select account 1360716
- [ ] No accounts found? → Token not authorized, retry Step 1
- [ ] Connection error? → Check internet connection

---

## Step 3: Verify Configuration

### File to Check
`start_ctrader_bridge.ps1`

### Verify These Lines
- [ ] `$env:CTRADER_ACCESS_TOKEN` = new token from Step 1
- [ ] `$env:CTRADER_ACCOUNT_ID` = API ID from Step 2 (NOT 1360716)
- [ ] `$env:CTRADER_HOST_TYPE` = "live"
- [ ] `$env:CTRADER_CLIENT_ID` = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
- [ ] `$env:CTRADER_CLIENT_SECRET` = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"

### Common Mistakes
- [ ] Using 1360716 instead of API ID? → Use API ID from Step 2
- [ ] HOST_TYPE set to "demo"? → Change to "live"
- [ ] Old token still there? → Should be updated by Step 1 script

---

## Step 4: Start Bridge

### Command
```powershell
.\start_ctrader_bridge.ps1
```

### Checklist
- [ ] Script starts without errors
- [ ] Python found message appears
- [ ] Port 8082 available (or old process stopped)
- [ ] "Pepperstone cTrader bridge listening" message
- [ ] Connected to cTrader API
- [ ] Application authenticated ✅
- [ ] Account authenticated ✅
- [ ] Symbols loaded ✅
- [ ] No error messages
- [ ] Bridge stays running (doesn't disconnect)

### Expected Output
```
========================================
  Pepperstone cTrader Bridge Startup
========================================
OK Environment variables configured
  - Host Type: live
  - Account ID: 47312778
  - Bridge Port: 8082

OK Python found: Python 3.14.2
Starting cTrader bridge...

Pepperstone cTrader bridge listening on ws://0.0.0.0:8082
[cTrader] connected: Connected to cTrader Open API
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: cTrader account 47312778 authenticated
[cTrader] symbols_loaded: Loaded 50+ broker symbols
```

### If Failed
- [ ] CANT_ROUTE_REQUEST error? → Check account ID is API ID, not 1360716
- [ ] Connection timeout? → Check internet connection
- [ ] Application auth fails? → Check Client ID/Secret
- [ ] Account auth fails? → Verify token and account ID

---

## Step 5: Test in App

### Open Your Android App
- [ ] App launches successfully
- [ ] Navigate to Pepperstone chart
- [ ] Chart loads without errors

### Verify Chart Display
- [ ] Candles display on chart ✅
- [ ] Price updates at top ✅
- [ ] Volume bars show ✅
- [ ] Real-time data streaming ✅
- [ ] No "connecting..." message
- [ ] No error messages
- [ ] Chart responds to touch/zoom

### Test Multiple Symbols
- [ ] EURUSD displays correctly
- [ ] GBPUSD displays correctly
- [ ] XAUUSD displays correctly
- [ ] Other symbols work

### Test Timeframes
- [ ] 1-minute chart works
- [ ] 5-minute chart works
- [ ] 1-hour chart works
- [ ] Daily chart works

---

## Final Verification

### Bridge Status
- [ ] Bridge running in PowerShell
- [ ] No error messages in console
- [ ] Connection stable (no disconnects)
- [ ] Data streaming continuously

### App Status
- [ ] Charts display correctly
- [ ] Real-time updates working
- [ ] No lag or freezing
- [ ] All features functional

### Configuration Saved
- [ ] Token saved in `ctrader_tokens_live.json`
- [ ] Configuration updated in `start_ctrader_bridge.ps1`
- [ ] Can restart bridge anytime

---

## Success Criteria

All of these must be true:
- [x] Token generated for live account 1360716
- [x] API account ID obtained
- [x] Bridge starts without errors
- [x] Application authenticated
- [x] Account authenticated
- [x] Symbols loaded
- [x] Candles display in app
- [x] Real-time data streaming
- [x] No error messages

---

## If Any Step Fails

### Step 1 Failed
→ See `SWITCH_TO_LIVE_ACCOUNT.md` - "Troubleshooting" section

### Step 2 Failed
→ Retry Step 1, ensure you select account 1360716

### Step 3 Failed
→ Manually edit `start_ctrader_bridge.ps1` with correct values

### Step 4 Failed
→ See `QUICK_FIX_STEPS.md` - "If Still Failing" section

### Step 5 Failed
→ Check bridge is running, check app connection settings

---

## Maintenance Checklist

### Daily
- [ ] Bridge running
- [ ] No error messages
- [ ] Data streaming correctly

### Weekly
- [ ] Check token expiration (30 days)
- [ ] Review error logs
- [ ] Test all symbols

### Monthly
- [ ] Regenerate token (before expiration)
- [ ] Update dependencies if needed
- [ ] Backup configuration

---

## Emergency Contacts

### Pepperstone Support
- Email: support@pepperstone.com
- Subject: "API access for live account 1360716"

### cTrader Support
- Portal: https://id.ctrader.com/my/settings/openapi
- Docs: https://help.ctrader.com/open-api/

---

## Alternative Solutions

If all else fails:
- [ ] Use Binance charts (already working)
- [ ] Use Deriv integration (available)
- [ ] Contact Pepperstone support
- [ ] Wait for cTrader API status update

---

## Notes Section

Use this space to track your progress:

**Date Started:** _______________

**Step 1 Completed:** _______________
- Token: _______________
- Time: _______________

**Step 2 Completed:** _______________
- API Account ID: _______________
- Time: _______________

**Step 3 Completed:** _______________
- Configuration verified: _______________

**Step 4 Completed:** _______________
- Bridge started: _______________
- Time: _______________

**Step 5 Completed:** _______________
- Charts working: _______________
- Time: _______________

**Issues Encountered:**
- _______________
- _______________
- _______________

**Resolution:**
- _______________
- _______________
- _______________

---

## Completion

When all checkboxes are marked:
- [x] **PEPPERSTONE CHART IS WORKING!** 🎉

Total time taken: _______________

Next steps: Monitor stability, test trading features
