# TERMINAL DESK TROUBLESHOOTING GUIDE

**Date**: June 1, 2026  
**Issue**: Terminal Desk not working well  
**Status**: Investigating

---

## WHAT TERMINAL DESK DOES

Terminal Desk is a **direct AI system command interface** for:
1. **Surveillance Control**: ARM/DISARM surveillance system
2. **Account Status**: Request account data from Binance/Exness/Pepperstone
3. **AI Pipeline**: Run ASC AI pipeline manually
4. **System Queries**: Ask about deployments, AI status, live trades, balance, etc.
5. **Deep Commands**: Execute TradingAssistantEngine commands
6. **General AI Chat**: Ask questions about market state and get AI explanations

---

## CONFIGURATION CHECK

### ✅ GROQ API KEY
- **Status**: CONFIGURED
- **Location**: `local.properties`
- **Key**: `gsk_YOUR_GROQ_API_KEY_HERE`
- **Verification**: `GroqClient.isKeyConfigured()` should return `true`

### ✅ CODE STRUCTURE
- **Screen**: `TerminalScreen.kt` - UI and input handling
- **ViewModel**: `ForexViewModel.sendCommand()` - Command processing
- **AI Backend**: `AscAiTextExplainer.explain()` - AI text generation
- **API Client**: `GroqClient.chatCompletion()` - Groq API calls

---

## COMMON ISSUES & SOLUTIONS

### 1. **"ASC Engine v1 is offline: GROQ_API_KEY is not configured"**

**Cause**: BuildConfig.GROQ_API_KEY is empty at runtime

**Solutions**:
- Rebuild the app after adding GROQ_API_KEY to local.properties
- Clean build: `./gradlew clean build`
- Verify build.gradle.kts reads from local.properties correctly

**Check build.gradle.kts**:
```kotlin
android {
    defaultConfig {
        // Load from local.properties
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "GROQ_API_KEY", "\"${properties.getProperty("GROQ_API_KEY", "")}\"")
    }
}
```

---

### 2. **"ASC Engine v1 is unavailable: Groq is unreachable"**

**Cause**: Network connectivity issue

**Solutions**:
- Check internet connection
- Verify DNS can resolve `api.groq.com`
- Check firewall/antivirus blocking Groq API
- Test with: `ping api.groq.com` or `curl https://api.groq.com`

---

### 3. **"Groq rejected the API key" (401/403 error)**

**Cause**: Invalid or expired API key

**Solutions**:
- Verify API key is correct in local.properties
- Check Groq dashboard: https://console.groq.com/keys
- Generate new API key if needed
- Rebuild app after updating key

---

### 4. **"Groq rate limit reached" (429 error)**

**Cause**: Too many requests to Groq API

**Solutions**:
- Wait a few minutes before trying again
- Check Groq rate limits: https://console.groq.com/settings/limits
- Upgrade Groq plan if needed

---

### 5. **Terminal shows no response or hangs**

**Cause**: Command not recognized or AI backend timeout

**Solutions**:
- Check if command is in the supported list (see below)
- Increase timeout in GroqClient (currently 30 seconds)
- Check logs for exceptions

---

### 6. **Surveillance ARM/DISARM not working**

**Cause**: State not syncing with backend

**Solutions**:
- Check `SurveillanceStateManager.setArmed()` is called
- Verify `TradingAssistantEngine.armed` is updated
- Check `viewModel.isArmed` state flow

---

## SUPPORTED COMMANDS

### Surveillance Commands:
- `ARM` / `ARM SURVEILLANCE` / `ARM_SURVEILLANCE` → Arms surveillance
- `DISARM` / `DISARM SURVEILLANCE` / `DISARM_SURVEILLANCE` → Disarms surveillance

### Account Commands:
- `ACCOUNT` → Requests account status from active broker

### AI Pipeline Commands:
- `RUN AI` / `RUN ASC AI` / `RUN PIPELINE` / `RUN ASC PIPELINE` → Runs AI pipeline

### Status Commands:
- `ASC` / `ASC STATUS` / `AI STATUS` / `DEPLOYMENTS` / `REFRESH AI` → Shows AI deployment summary

### Deep Commands:
- Any command recognized by `TradingAssistantEngine.handleInput()`

### General Queries:
- Any question about market state, trades, balance, etc.
- Examples:
  - "How many trades are open?"
  - "What's my balance?"
  - "What's the current price of BTCUSDT?"
  - "Show me the AI deployments"

---

## TESTING TERMINAL DESK

### Test 1: Simple Greeting
**Command**: `hello`  
**Expected**: "This is ASC Engine v1. What can I do for you?"

### Test 2: ARM Surveillance
**Command**: `ARM`  
**Expected**: "Surveillance is armed now."  
**Verify**: Status bar shows "SURVEILLANCE_ARMED" with white pulsing dot

### Test 3: DISARM Surveillance
**Command**: `DISARM`  
**Expected**: "Surveillance is disarmed now."  
**Verify**: Status bar shows "SURVEILLANCE_LOCKED" with gray dot

### Test 4: Account Status
**Command**: `ACCOUNT`  
**Expected**: "I'm requesting [Broker] account status now."

### Test 5: AI Status
**Command**: `ASC STATUS`  
**Expected**: Summary of AI deployments with asset, direction, label, etc.

### Test 6: General Query
**Command**: `What's my balance?`  
**Expected**: AI response with balance from app state snapshot

---

## DEBUGGING STEPS

### Step 1: Check BuildConfig
Add this to TerminalScreen or ForexViewModel:
```kotlin
Log.d("TerminalDesk", "GROQ_API_KEY configured: ${GroqClient.isKeyConfigured()}")
Log.d("TerminalDesk", "BuildConfig.GROQ_API_KEY length: ${BuildConfig.GROQ_API_KEY.length}")
```

### Step 2: Check Network
Add this to GroqClient.chatCompletion():
```kotlin
Log.d("GroqClient", "Sending request to Groq API...")
Log.d("GroqClient", "Response code: $code")
Log.d("GroqClient", "Response: ${respText.take(200)}")
```

### Step 3: Check Command Processing
Add this to ForexViewModel.sendCommand():
```kotlin
Log.d("TerminalDesk", "Command received: $text")
Log.d("TerminalDesk", "Command uppercase: $upper")
Log.d("TerminalDesk", "Response: $response")
```

### Step 4: Check Terminal Logs
Add this to TerminalScreen:
```kotlin
LaunchedEffect(logs) {
    Log.d("TerminalDesk", "Terminal logs count: ${logs.size}")
    logs.forEach { log ->
        Log.d("TerminalDesk", "Log: ${log.role} - ${log.content.take(100)}")
    }
}
```

---

## WHAT TO TELL ME

To help diagnose the issue, please provide:

1. **What happens when you type a command?**
   - Does it show in the terminal?
   - Do you get a response?
   - What error message do you see?

2. **Which commands have you tried?**
   - ARM/DISARM?
   - ACCOUNT?
   - ASC STATUS?
   - General questions?

3. **What's the exact error message?**
   - "ASC Engine v1 is offline"?
   - "ASC Engine v1 is unavailable"?
   - No response at all?
   - Something else?

4. **Does ARM/DISARM work?**
   - Does the status bar change?
   - Does the button toggle?

5. **Have you rebuilt the app recently?**
   - After adding GROQ_API_KEY to local.properties?
   - Clean build?

---

## QUICK FIX CHECKLIST

- [ ] GROQ_API_KEY is in local.properties
- [ ] App has been rebuilt after adding key
- [ ] Internet connection is working
- [ ] Can access api.groq.com
- [ ] Groq API key is valid (check console.groq.com)
- [ ] Not hitting rate limits
- [ ] ARM/DISARM buttons work
- [ ] Terminal shows user input
- [ ] Terminal shows AI responses

---

**Next Steps**: Please describe what's not working, and I'll help you fix it.
