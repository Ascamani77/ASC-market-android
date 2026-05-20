# Pepperstone cTrader API Token Setup Guide

## Current Issue
```
[ERROR] CH_ACCESS_TOKEN_INVALID: Invalid access token
```

Your access token was revoked and needs to be regenerated.

---

## Step-by-Step Setup Process

### Step 1: Access Pepperstone cTrader Open API Portal

1. **Go to**: https://openapi.ctrader.com/
2. **Login** with your Pepperstone credentials
3. You should see your existing application or need to create one

---

### Step 2: Create or Access Your Application

#### If you DON'T have an application yet:
1. Click **"Create New Application"**
2. Fill in:
   - **Application Name**: `MyTradingApp` (or any name you prefer)
   - **Redirect URI**: `http://localhost:8080/callback` (required but not used for our bridge)
   - **Scope**: Select **"trading"** (full trading access)
3. Click **"Create"**

#### If you ALREADY have an application:
1. Click on your application name in the list
2. You'll see your application details

---

### Step 3: Get Your Credentials

You'll see three important values:

#### 1. **Client ID** (56 characters)
- Example: `7_5az7pj935owsss8kgokcco84wc8osk0g0gksw08ows4cg4s4`
- This is your `CTRADER_CLIENT_ID`

#### 2. **Client Secret** (50 characters)  
- Example: `49p1ub9n4esokwk8w08w4g888cwg8okcg08ow8gc4s8ck4`
- This is your `CTRADER_CLIENT_SECRET`
- ⚠️ **IMPORTANT**: Copy this immediately - it's only shown once!

#### 3. **Generate Access Token**
- Click **"Playground"** or **"Generate Token"** button
- Select your **Demo Account** (Account ID: 5287516)
- Click **"Authorize"**
- Copy the **Access Token** (43+ characters)
- Example: `CQABAAAAARjcj8zMzMzMzMzMzMzMzMzMzMzMzMz`
- This is your `CTRADER_ACCESS_TOKEN`

---

### Step 4: Update Your Environment Variables

Open PowerShell and run these commands (replace with YOUR actual values):

```powershell
# Set Client ID (56 characters)
[System.Environment]::SetEnvironmentVariable('CTRADER_CLIENT_ID', 'YOUR_CLIENT_ID_HERE', 'User')

# Set Client Secret (50 characters)
[System.Environment]::SetEnvironmentVariable('CTRADER_CLIENT_SECRET', 'YOUR_CLIENT_SECRET_HERE', 'User')

# Set Access Token (43+ characters)
[System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', 'YOUR_ACCESS_TOKEN_HERE', 'User')

# Set Account ID (your demo account)
[System.Environment]::SetEnvironmentVariable('CTRADER_ACCOUNT_ID', '5287516', 'User')

# Set Host Type (demo or live)
[System.Environment]::SetEnvironmentVariable('CTRADER_HOST_TYPE', 'demo', 'User')
```

---

### Step 5: Verify Environment Variables

Close and reopen PowerShell, then check:

```powershell
$env:CTRADER_CLIENT_ID
$env:CTRADER_CLIENT_SECRET
$env:CTRADER_ACCESS_TOKEN
$env:CTRADER_ACCOUNT_ID
$env:CTRADER_HOST_TYPE
```

All should display their values (not empty).

---

### Step 6: Restart the Bridge

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_ctrader_bridge.ps1
```

---

## Expected Success Output

```
========================================
Pepperstone cTrader Bridge Startup
========================================
✓ Environment variables configured
  - Host Type: demo
  - Account ID: 5287516
  - Bridge Port: 8082

✓ Python found: Python 3.14.2

Starting cTrader bridge...
Press Ctrl+C to stop the bridge
========================================

[cTrader] connected: Connected to cTrader Open API demo endpoint
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: Account 5287516 authenticated
[cTrader] symbols_loaded: Loaded 500+ symbols
✓ Bridge ready - accepting WebSocket connections on ws://0.0.0.0:8082
```

---

## Troubleshooting

### Error: "CANT_ROUTE_REQUEST"
- **Cause**: Wrong endpoint (demo vs live) or invalid Client ID/Secret
- **Fix**: Verify you're using the correct credentials from the portal

### Error: "CH_ACCESS_TOKEN_INVALID"
- **Cause**: Access token expired or revoked
- **Fix**: Generate a new access token from the Playground

### Error: "ACCOUNT_NOT_FOUND"
- **Cause**: Account ID doesn't match the access token
- **Fix**: Make sure you selected the correct account (5287516) when generating the token

### Error: "UNAUTHORIZED"
- **Cause**: Application not authorized for trading
- **Fix**: Check that "trading" scope is enabled in your application settings

---

## Important Notes

1. **Access Tokens Expire**: They typically last 30 days. You'll need to regenerate periodically.

2. **Demo vs Live**: 
   - Demo account: Use `demo` host type
   - Live account: Use `live` host type
   - Make sure your access token matches the account type!

3. **Security**: Never commit these credentials to Git. They're stored in environment variables only.

4. **Multiple Accounts**: If you have multiple accounts, generate separate tokens for each.

---

## Quick Reference

| Variable | Length | Example |
|----------|--------|---------|
| `CTRADER_CLIENT_ID` | 56 chars | `7_5az7pj935ow...` |
| `CTRADER_CLIENT_SECRET` | 50 chars | `49p1ub9n4esok...` |
| `CTRADER_ACCESS_TOKEN` | 43+ chars | `CQABAAAAARjcj...` |
| `CTRADER_ACCOUNT_ID` | 8 digits | `5287516` |
| `CTRADER_HOST_TYPE` | 4-5 chars | `demo` or `live` |

---

## Next Steps After Setup

Once the bridge is running successfully:

1. **Test Connection**: Open your Android app and navigate to Pepperstone charts
2. **Verify Data**: You should see live prices and candles
3. **Check Balance**: Account balance should show $50,000 (demo)
4. **Place Test Order**: Try placing a small test order to verify trading works

---

## Need Help?

If you're still having issues after following this guide:

1. Check the bridge logs for specific error messages
2. Verify all environment variables are set correctly
3. Try regenerating the access token
4. Make sure you're using the demo account (5287516)
5. Check that your Pepperstone account is active and not suspended

