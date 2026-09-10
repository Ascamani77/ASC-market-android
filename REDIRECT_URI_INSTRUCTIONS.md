# How to Add Redirect URI to cTrader Application

## The Issue

You're getting this error:
```
Application authentication failed: Provided application does not contain provided URI.
```

This means the redirect URI `http://localhost:9999/callback` is not registered in your cTrader application settings.

## Solution: Add the Redirect URI

You mentioned you saw "Redirect URLs" settings. Here's how to add the URI:

### Step 1: Access Application Settings

Try these URLs to access your application settings:

1. **Main Portal**: https://openapi.ctrader.com/
2. **Application Management**: https://id.ctrader.com/my/settings/openapi/applications
3. **Direct App Link**: https://id.ctrader.com/my/settings/openapi/applications/27391

Sign in with your **DEMO** credentials.

### Step 2: Find Your Application

Look for your application with:
- **Client ID**: `YOUR_CTRADER_CLIENT_ID`

### Step 3: Add Redirect URI

In the "Redirect URLs" or "Redirect URIs" section:

1. Click "Add" or "+" button
2. Enter: `http://localhost:9999/callback`
3. Click "Save" or "Submit"

### Step 4: Verify

Make sure you see:
```
http://localhost:9999/callback
```

in the list of redirect URIs.

### Step 5: Run the Script Again

Once the redirect URI is added:

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\get_demo_token_easy.ps1
```

---

## If You Can't Access the Portal

If none of the URLs above work, you have two options:

### Option A: Contact Pepperstone Support

See `CONTACT_PEPPERSTONE_FOR_TOKEN.md` for instructions on how to contact support and request a token directly.

### Option B: Use an Existing Redirect URI

If you can see what redirect URIs are already configured, tell me what they are, and I'll create a script that uses one of them.

Common redirect URIs that might already be registered:
- `http://localhost:8080`
- `http://localhost:8080/callback`
- `http://localhost:3000`
- `http://localhost:3000/callback`
- `http://127.0.0.1:8080`
- `https://localhost:8080`

---

## What You Saw Earlier

You mentioned seeing "Redirect URLs (read more)" in the settings. Can you:

1. Go back to that page
2. Take a screenshot or copy what redirect URLs are listed
3. Share them with me

Then I can create a script that uses one of those existing URIs.

---

## Alternative: Try Common Ports

Let me create scripts for common redirect URIs that might already be configured:

### Try Port 8080
```powershell
.\get_demo_token_port8080.ps1
```

### Try Port 3000
```powershell
.\get_demo_token_port3000.ps1
```

### Try Port 5000
```powershell
.\get_demo_token_port5000.ps1
```

I'll create these scripts for you to try different ports.

---

## Why This Matters

The OAuth2 flow requires:
1. Your application has a list of **allowed redirect URIs**
2. When you authorize, cTrader redirects to one of these URIs
3. The redirect URI in the authorization request **must match exactly** one in the list

If the URI isn't in the list, you get the "does not contain provided URI" error.

---

## Next Steps

**Option 1** (If you can access the portal):
1. Go to application settings
2. Add `http://localhost:9999/callback` to redirect URIs
3. Run `.\get_demo_token_easy.ps1`

**Option 2** (If you can see existing URIs):
1. Tell me what redirect URIs are already configured
2. I'll create a script using one of them
3. Run the new script

**Option 3** (If portal doesn't work):
1. Contact Pepperstone support
2. Request a demo access token directly
3. Use `.\start_demo_bridge.ps1` with the token

---

**What redirect URIs do you see in your application settings?**
