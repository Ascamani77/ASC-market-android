# How to Get the Authorization Code

## ⚠️ "ERR_CONNECTION_REFUSED" is NORMAL!

When you see this error, **don't panic** - this is expected behavior!

---

## 📋 What's Happening

After you click "Allow access" on the Pepperstone page, your browser tries to redirect to:
```
http://localhost:8888/callback?code=ABC123XYZ...
```

Since there's no server running on localhost:8888, you get:
```
ERR_CONNECTION_REFUSED
This site can't be reached
```

**This is perfectly fine!** The authorization code is still in the URL.

---

## ✅ How to Get the Code

### Step 1: Look at the browser's address bar

Even though the page shows an error, the URL bar contains your code:

```
http://localhost:8888/callback?code=ABC123XYZ_YOUR_CODE_HERE
```

### Step 2: Copy everything after `code=`

Example URL:
```
http://localhost:8888/callback?code=0ssdgds98as9_QSF56FVC_22dfdf
```

Copy this part:
```
0ssdgds98as9_QSF56FVC_22dfdf
```

### Step 3: Paste it into PowerShell

Go back to your PowerShell window and paste the code when prompted.

---

## 🖼️ Visual Guide

### What you see in browser:
```
┌─────────────────────────────────────────────────────────┐
│ ← → ⟳  http://localhost:8888/callback?code=ABC123XYZ   │ ← COPY FROM HERE
├─────────────────────────────────────────────────────────┤
│                                                         │
│  This site can't be reached                             │
│  localhost refused to connect.                          │
│                                                         │
│  ERR_CONNECTION_REFUSED                                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### What to copy:
```
ABC123XYZ  ← Just the code part after "code="
```

---

## 📝 Step-by-Step Example

### 1. Browser shows error with this URL:
```
http://localhost:8888/callback?code=0ssdgds98as9_QSF56FVC_22dfdf
```

### 2. Select and copy the code part:
```
0ssdgds98as9_QSF56FVC_22dfdf
```

### 3. PowerShell is waiting:
```
Paste the authorization code here: _
```

### 4. Paste the code:
```
Paste the authorization code here: 0ssdgds98as9_QSF56FVC_22dfdf
```

### 5. Press Enter

### 6. Script exchanges it for token:
```
[OK] Token received!
Access Token:  mos8Bw3D4EG0fRPd4Eqq0JxaFT...
```

---

## ⚡ Quick Tips

✅ **Don't worry about the error page** - it's expected
✅ **The code is in the URL** - that's all you need
✅ **Copy only the code part** - everything after `code=`
✅ **The code expires in 1 minute** - be quick!
✅ **If it expires** - just run the script again

---

## 🔄 If You Need to Start Over

If the code expires or you make a mistake:

1. Press `Ctrl+C` in PowerShell to cancel
2. Run the script again: `.\get_token_playground.ps1`
3. Get a new authorization code
4. Paste it faster this time

---

## 🎯 Alternative: Use Playground Page

If you're using `get_token_playground.ps1`, the redirect goes to the Playground page instead of localhost, so you won't see the error. The code will be displayed on the page.

---

## ❓ Still Confused?

Here's the simplest explanation:

1. **Browser redirects** → URL contains code
2. **Page shows error** → Ignore it!
3. **Look at URL bar** → Copy the code
4. **Paste in PowerShell** → Done!

The error is just because there's no web server running. The code is still valid and in the URL! 🎉

