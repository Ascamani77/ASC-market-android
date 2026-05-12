# cTrader Pepperstone Quick Start Guide

## Step 1: Get Your cTrader Open API Credentials

### 1.1 Register Your Application
1. Go to https://openapi.ctrader.com/
2. Click "Sign In" and log in with your Pepperstone account
3. Click "Applications" → "Create New Application"
4. Fill in the form:
   - **Name**: "My Trading App" (or any name you want)
   - **Redirect URI**: `http://localhost:8088/callback`
   - **Scopes**: Select "Trading" (allows trading operations)
5. Click "Create"

### 1.2 Get Your Credentials
After creating the application, you'll see:
- **Client ID**: A long string like `7_5az7pj935owsss8kgokcco84wc8osk0g0gksw08ow4s4ocwwgc`
- **Client Secret**: Another long string like `49u5vd6j5z0kgccs0kkw8w4w4kc8w0s4gkw0w4o4s8s4c0wkwg`

**Copy these values!** You'll need them in the next step.

## Step 2: Set Environment Variables

Open PowerShell and run these commands (replace with your actual values):

```powershell
# Set your actual Client ID and Secret
$env:CTRADER_CLIENT_ID="paste_your_actual_client_id_here"
$env:CTRADER_CLIENT_SECRET="paste_your_actual_client_secret_here"

# Optional: Set redirect URI (default is http://localhost:8088/callback)
$env:CTRADER_REDIRECT_URI="http://localhost:8088/callback"

# Optional: Set host type (default is demo)
$env:CTRADER_HOST_TYPE="demo"
```

**Example with real values:**
```powershell
$env:CTRADER_CLIENT_ID="7_5az7pj935owsss8kgokcco84wc8osk0g0gksw08ow4s4ocwwgc"
$env:CTRADER_CLIENT_SECRET="49u5vd6j5z0kgccs0kkw8w4w4kc8w0s4gkw0w4o4s8s4c0wkwg"
```

## Step 3: Run the OAuth Setup Script

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
python .\ctrader_oauth_setup.py
```

### What Happens:
1. The script will open your browser
2. You'll be asked to authorize the application
3. **IMPORTANT**: Select your **Pepperstone demo account** when prompted
4. After authorization, you'll be redirected back
5. The script will display:
   - Your **Access Token**
   - Your **Account ID(s)**

### Example Output:
```
ACCESS TOKEN
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ...

REFRESH TOKEN
def50200-1234-5678-90ab-cdef12345678

EXPIRES IN: 3600

AUTHORIZED ACCOUNTS
1. CTRADER_ACCOUNT_ID=12345678 broker=Pepperstone live=False login=1234567
```

## Step 4: Update local.properties

Copy the values from the script output and update your `local.properties` file:

```properties
# cTrader Pepperstone Configuration
CTRADER_HOST_TYPE=demo
CTRADER_CLIENT_ID=7_5az7pj935owsss8kgokcco84wc8osk0g0gksw08ow4s4ocwwgc
CTRADER_CLIENT_SECRET=49u5vd6j5z0kgccs0kkw8w4w4kc8w0s4gkw0w4o4s8s4c0wkwg
CTRADER_ACCESS_TOKEN=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ...
CTRADER_ACCOUNT_ID=12345678
CTRADER_BRIDGE_PORT=8082
```

## Step 5: Rebuild Your App

After updating `local.properties`:
1. Open Android Studio
2. Click "Build" → "Rebuild Project"
3. The new configuration will be available in `BuildConfig`

## Troubleshooting

### Error: "Set your real CTRADER_CLIENT_ID and CTRADER_CLIENT_SECRET"
- You forgot to set the environment variables in PowerShell
- Run the commands from Step 2 again

### Error: "Redirect URI mismatch"
- The redirect URI in your cTrader app settings doesn't match
- Go back to https://openapi.ctrader.com/
- Edit your application
- Make sure Redirect URI is exactly: `http://localhost:8088/callback`

### Error: "No accounts were returned"
- You didn't select your Pepperstone account during authorization
- Run the script again and make sure to check the box for your demo account

### Browser doesn't open
- The script will print the authorization URL
- Copy and paste it into your browser manually
- After authorizing, copy the full redirect URL and paste it back into PowerShell

### Access Token Expired
- Access tokens expire after a certain time (usually 1 hour)
- Run the OAuth setup script again to get a new token
- Or implement token refresh in your app (advanced)

## Security Reminders

⚠️ **NEVER commit `local.properties` to git!**
- It contains sensitive credentials
- It's already in `.gitignore`
- Keep it that way!

⚠️ **Use demo account for testing**
- Always test with `CTRADER_HOST_TYPE=demo`
- Only switch to `live` when you're ready for real trading

## Next Steps

After completing these steps:
1. Your Android app can access cTrader credentials via `BuildConfig`
2. You can create a `CTraderService` to connect to the API
3. Start implementing live trading features

## Useful Links

- [cTrader Open API Portal](https://openapi.ctrader.com/)
- [cTrader API Documentation](https://help.ctrader.com/open-api/)
- [Pepperstone cTrader](https://pepperstone.com/en/trading-platforms/ctrader)
