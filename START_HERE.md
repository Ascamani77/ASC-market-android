# 🚀 START HERE - Fix Pepperstone Chart

## The Problem
Your Pepperstone chart shows prices at the top but **no candles display**.

## The Solution
Switch from demo account to live account 1360716.

---

## 📋 What You Need
- ✅ Live account 1360716 (you have this)
- ✅ PowerShell (already installed on Windows)
- ✅ Python (already installed)
- ✅ 5-10 minutes of time

---

## 🎯 Three Simple Steps

### Step 1️⃣: Generate Token
```powershell
.\get_live_token.ps1
```

**What happens:**
1. Browser opens automatically
2. You login to cTrader
3. **SELECT ACCOUNT 1360716** ⚠️ IMPORTANT!
4. Click "Allow access"
5. Copy URL from browser
6. Paste into PowerShell (within 60 seconds)

**Result:** ✅ Token generated and saved

---

### Step 2️⃣: Get API Account ID
```powershell
python get_live_account_id.py
```

**What happens:**
- Script connects to cTrader API
- Shows your account information
- Displays the API account ID (different from 1360716)

**Result:** ✅ API account ID discovered

---

### Step 3️⃣: Start Bridge
```powershell
.\start_ctrader_bridge.ps1
```

**What happens:**
- Bridge connects to cTrader
- Authenticates with your live account
- Starts streaming data to your app

**Result:** ✅ Bridge running, candles display in app

---

## ✅ Success Looks Like This

### In PowerShell:
```
✅ Connected to cTrader Open API
✅ Application authenticated
✅ Account authenticated
✅ Symbols loaded
```

### In Your App:
- Candles display on chart
- Prices update in real-time
- Volume bars show
- Everything works!

---

## ⚠️ Important Notes

### When Generating Token (Step 1)
- **MUST select account 1360716** (your live account)
- **MUST paste code within 60 seconds** (it expires fast)
- If you see "Too Many Attempts", wait 5-10 minutes

### Account IDs
- **1360716** = What you see in cTrader (visible ID)
- **~47312778** = What the API uses (internal ID)
- The scripts handle this automatically

---

## 🆘 Need Help?

### Quick Troubleshooting

**Problem:** "Too Many Attempts"
**Solution:** Wait 5-10 minutes, then retry Step 1

**Problem:** "No accounts found" in Step 2
**Solution:** Retry Step 1, make sure you select account 1360716

**Problem:** Still getting errors in Step 3
**Solution:** Check `start_ctrader_bridge.ps1` has correct token and account ID

### Detailed Guides
- `SWITCH_TO_LIVE_ACCOUNT.md` - Full step-by-step guide
- `RUN_THESE_COMMANDS.md` - Quick command reference
- `PEPPERSTONE_FIX_SUMMARY.md` - Complete technical details

---

## 🎉 Ready to Start?

Open PowerShell in this folder and run:
```powershell
.\get_live_token.ps1
```

Then follow the prompts. It's that simple!

---

## 📊 Why This Works

**Before (Demo Account):**
- Demo account 5288664
- API ID: 47340965
- Getting CANT_ROUTE_REQUEST errors
- No candles display

**After (Live Account):**
- Live account 1360716
- API ID: ~47312778
- Stable API connection
- Candles display correctly

Live accounts have better API routing and stability!

---

## ⏱️ Time Estimate

- Step 1: 2 minutes
- Step 2: 1 minute
- Step 3: 1 minute
- Testing: 1 minute

**Total: ~5 minutes**

---

## 🔄 What If It Doesn't Work?

If you follow all steps and still have issues:

1. **Check Application Status**
   - Visit: https://id.ctrader.com/my/settings/openapi
   - Verify application is "Active"

2. **Contact Pepperstone**
   - Email: support@pepperstone.com
   - Subject: "API access for live account 1360716"

3. **Use Alternative**
   - Binance charts (already working in your app)
   - Deriv integration (available)

---

## 💡 Pro Tips

- Keep PowerShell window open while bridge runs
- Bridge must stay running for app to receive data
- Press Ctrl+C to stop bridge
- Token lasts 30 days, then regenerate

---

## 🚀 Let's Go!

Run the first command now:
```powershell
.\get_live_token.ps1
```

You're 3 commands away from working Pepperstone charts! 🎯
