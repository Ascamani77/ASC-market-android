# Quick Token Setup for Account 5287516

## ✅ All code updated to use account ID: **5287516**

---

## 🚀 Run This Now

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\get_token_simple.ps1
```

This script will:
1. Show you the URL to open in your browser
2. You login and authorize
3. You paste the code back
4. Script exchanges it for tokens
5. Tokens saved automatically

---

## What to Expect

### Step 1: Script shows URL
```
Copy this URL and paste it in your browser:
https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=...
```

### Step 2: Open URL in browser
- Login with Pepperstone credentials
- Select account **5287516**
- Click "Allow access"

### Step 3: Get the code
Browser redirects to:
```
http://localhost:8888/callback?code=ABC123XYZ_YOUR_CODE_HERE
```

Copy everything after `code=`

### Step 4: Paste code
```
Paste the authorization code here: ABC123XYZ_YOUR_CODE_HERE
```

### Step 5: Done!
```
[OK] Token received!
[OK] Environment variables set!
```

---

## Then Start Bridge

```powershell
.\start_ctrader_bridge.ps1
```

Expected output:
```
[cTrader] account_authenticated: Account 5287516 authenticated
[cTrader] symbols_loaded: Loaded 500+ symbols
✓ Bridge ready
```

---

## Troubleshooting

### "Page can't be displayed"
**This is normal!** The page won't load, but the URL contains your code. Just copy it from the address bar.

### "Invalid authorization code"
The code expires in 1 minute. Get a new one and paste it faster.

### "Invalid redirect_uri"
Add `http://localhost:8888/callback` to your app's redirect URIs at https://openapi.ctrader.com/

---

## All Changes Made

✅ Updated `start_ctrader_bridge.ps1` → Account ID: 5287516
✅ Updated `generate_ctrader_token.ps1` → Account ID: 5287516  
✅ Updated `generate_ctrader_token.py` → Account ID: 5287516
✅ Updated all documentation → Account ID: 5287516
✅ Created `get_token_simple.ps1` → Simpler token generator
✅ Fixed browser opening issue → Manual URL copy/paste

