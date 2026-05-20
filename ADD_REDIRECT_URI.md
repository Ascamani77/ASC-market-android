# Fix: Add Redirect URI to cTrader Application

## Error
```
Application authentication failed: Provided application does not contain provided URI.
```

This means the redirect URI `http://localhost:8888/callback` is not registered in your cTrader application.

---

## 🔧 Fix Steps

### 1. Go to cTrader Open API Portal
**https://openapi.ctrader.com/**

### 2. Login with your Pepperstone credentials

### 3. Find your application
- You should see your application in the list
- Click the **"Edit"** button (or pencil icon)

### 4. Scroll to "Redirect URIs" section

### 5. Add the redirect URI
- Find an empty text field (or click "Add redirect URI")
- Enter: `http://localhost:8888/callback`
- Click **"Save"** at the bottom

### 6. Verify it's added
You should now see:
```
Redirect URIs:
1. https://openapi.ctrader.com/apps/playground (default)
2. http://localhost:8888/callback (your new one)
```

---

## ✅ Then Try Again

After adding the redirect URI, run the token generator again:

```powershell
.\get_token_simple.ps1
```

This time it should work!

---

## Alternative: Use a Different Redirect URI

If you can't add `http://localhost:8888/callback`, you can use any URI that's already registered in your app.

### Check what URIs are registered:
1. Go to https://openapi.ctrader.com/
2. View your application
3. Look at the "Redirect URIs" section

### Common default URIs:
- `https://openapi.ctrader.com/apps/playground`
- `http://localhost:5000/callback`
- `http://127.0.0.1:8080/callback`

### Use an existing URI:
If you see a different URI, update the script to use it:

```powershell
# Edit get_token_simple.ps1
# Change this line:
$REDIRECT_URI = "http://localhost:8888/callback"

# To match your registered URI, for example:
$REDIRECT_URI = "https://openapi.ctrader.com/apps/playground"
```

---

## Screenshot Guide

### Step 1: Applications Page
![Applications](https://i.imgur.com/example1.png)
- Click "Edit" on your application

### Step 2: Edit Application
![Edit](https://i.imgur.com/example2.png)
- Scroll down to "Redirect URIs"

### Step 3: Add URI
![Add URI](https://i.imgur.com/example3.png)
- Enter: `http://localhost:8888/callback`
- Click "Save"

---

## Important Notes

⚠️ **The first redirect URI** is always for the Playground - don't delete it!

✅ **You can have multiple redirect URIs** - add as many as you need

🔒 **Redirect URIs must match exactly** - including http/https, port, and path

---

## After Adding URI

Once you've added the redirect URI and saved:

1. **Wait 30 seconds** for changes to propagate
2. **Run the script again**: `.\get_token_simple.ps1`
3. **Follow the prompts** to get your token

The error should be gone! 🎉

