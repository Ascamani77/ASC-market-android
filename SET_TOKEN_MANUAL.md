# Manual Token Setup (Easiest Method)

The OAuth flow keeps timing out. Let's use the Playground to get a production token (it's the same token, just easier to get).

## 🎯 Steps

### 1. Go to Playground
https://openapi.ctrader.com/

### 2. Click "Playground" on your application

### 3. Select Settings
- **Scope**: Select **"trading"** (full access)
- **Account**: Select **5287516** (your demo account)

### 4. Click "Get token"

### 5. Click "Authorize"

### 6. Copy the tokens
You'll see:
- **Access Token**: (long string)
- **Refresh Token**: (long string)

Copy the **Access Token**

### 7. Run this in PowerShell

```powershell
# Paste your access token here (replace YOUR_TOKEN_HERE)
$token = "YOUR_ACCESS_TOKEN_HERE"

# Set environment variable
[System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', $token, 'User')

# Update bridge script
$script = Get-Content "start_ctrader_bridge.ps1" -Raw
$script = $script -replace 'CTRADER_ACCESS_TOKEN = ".*"', "CTRADER_ACCESS_TOKEN = `"$token`""
$script | Set-Content "start_ctrader_bridge.ps1" -Encoding UTF8

Write-Host "[OK] Token configured!" -ForegroundColor Green
Write-Host "Now run: .\start_ctrader_bridge.ps1" -ForegroundColor Yellow
```

---

## Why This Works

The Playground token is **NOT** just for testing - it's a real production token with the same:
- ✅ 30-day expiration
- ✅ Full trading permissions
- ✅ Works with your demo account
- ✅ Can be refreshed

The only difference is **how** you get it (UI vs OAuth flow). The token itself is identical.

---

## After Setting Token

Run the bridge:
```powershell
.\start_ctrader_bridge.ps1
```

You should see:
```
[cTrader] account_authenticated: Account 5287516 authenticated
[cTrader] symbols_loaded: Loaded 500+ symbols
✓ Bridge ready
```

Then your Pepperstone charts will show candles! 🎉

