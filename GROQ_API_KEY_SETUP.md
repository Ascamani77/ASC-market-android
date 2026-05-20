# Groq API Key Setup Guide

## Issue
Your AI Chat is showing "GROQ API not configured" because the `GROQ_API_KEY` is missing from your `local.properties` file.

## Solution

### Step 1: Get Your Groq API Key

1. Go to [https://console.groq.com](https://console.groq.com)
2. Sign up or log in
3. Navigate to **API Keys** section
4. Click **Create API Key**
5. Copy the key (it starts with `gsk_`)

### Step 2: Add Key to local.properties

I've added a placeholder in your `local.properties` file:

```properties
# Groq API Configuration
GROQ_API_KEY=gsk_YOUR_GROQ_API_KEY_HERE
```

**Replace `gsk_YOUR_GROQ_API_KEY_HERE` with your actual Groq API key.**

### Step 3: Rebuild the App

After adding your key:

1. **Sync Gradle** (File → Sync Project with Gradle Files)
2. **Clean Build** (Build → Clean Project)
3. **Rebuild** (Build → Rebuild Project)
4. **Run the app**

## Why This Happens

The app reads the `GROQ_API_KEY` from `local.properties` at **build time** and injects it into `BuildConfig.GROQ_API_KEY`. This means:

- ✅ The key is NOT stored in source code
- ✅ The key is NOT committed to Git
- ✅ Each developer can use their own key
- ❌ You MUST rebuild after changing the key

## Verification

After rebuilding, your AI Chat should work. You can verify by:

1. Open **AI Chat** (Chat screen)
2. Ask any question
3. You should get a response from Groq AI

## Troubleshooting

### Still showing "not configured"?
- Make sure you **rebuilt** the app (not just restarted)
- Check that the key starts with `gsk_`
- Make sure there are no extra spaces or quotes around the key

### Getting API errors?
- Verify your key is valid at [https://console.groq.com](https://console.groq.com)
- Check if you have API credits/quota remaining
- Make sure you're using the correct key (not a revoked one)

## Where Groq API is Used

Your app uses Groq API in:

1. **AI Chat** (`ChatScreen`) - Main AI assistant
2. **Trading Assistant** (`TradingAssistantEngine`) - Market analysis
3. **News Analysis** (`MarketOverviewTab`) - News sentiment
4. **AI Text Explainer** (`AscAiTextExplainer`) - Deployment explanations

## Free Tier Limits

Groq offers a generous free tier:
- **14,400 requests per day**
- **Rate limit**: 30 requests per minute
- **Models**: llama-3.3-70b-versatile (default)

This should be more than enough for personal use!

## Security Note

⚠️ **NEVER commit `local.properties` to Git!**

The file is already in `.gitignore`, but double-check:
```bash
git status
```

If you see `local.properties` listed, do NOT commit it!

## Next Steps

1. Get your Groq API key from [console.groq.com](https://console.groq.com)
2. Replace the placeholder in `local.properties`
3. Rebuild the app
4. Enjoy your AI-powered trading assistant! 🚀
