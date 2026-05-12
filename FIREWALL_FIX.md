# Fix AI Backend Connection - Windows Firewall

## Problem
Phone cannot connect to AI backend at `http://10.164.138.133:8000`
Error: `ERR_CONNECTION_TIMED_OUT`

## Root Cause
Windows Firewall is blocking incoming connections on port 8000

## Solution

### Step 1: Open PowerShell as Administrator
1. Press `Windows + X`
2. Select "Windows PowerShell (Admin)" or "Terminal (Admin)"

### Step 2: Add Firewall Rule for Port 8000
Run this command:
```powershell
New-NetFirewallRule -DisplayName "AI Backend Port 8000" -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow
```

### Step 3: Verify the Rule was Created
```powershell
Get-NetFirewallRule -DisplayName "AI Backend Port 8000"
```

### Step 4: Check if AI is Listening on Port 8000
```powershell
netstat -ano | findstr :8000
```

You should see something like:
```
TCP    0.0.0.0:8000           0.0.0.0:0              LISTENING       12345
```

### Step 5: Test from Phone Browser
Open browser on your phone and go to:
```
http://10.164.138.133:8000/health
```

You should see a JSON response like:
```json
{
  "status": "ok",
  "mode": "live"
}
```

### Step 6: Test from PC Browser (to confirm AI is running)
Open browser on your PC and go to:
```
http://localhost:8000/health
```

## Alternative: GUI Method to Add Firewall Rule

1. Press `Windows + R`, type `wf.msc`, press Enter
2. Click "Inbound Rules" on the left
3. Click "New Rule..." on the right
4. Select "Port", click Next
5. Select "TCP", enter "8000" in Specific local ports, click Next
6. Select "Allow the connection", click Next
7. Check all profiles (Domain, Private, Public), click Next
8. Name it "AI Backend Port 8000", click Finish

## Troubleshooting

### If still not working after firewall rule:

1. **Check if AI is actually running:**
   ```powershell
   Get-Process | Where-Object {$_.ProcessName -like "*python*"}
   ```

2. **Check if AI is binding to 0.0.0.0 (not 127.0.0.1):**
   - Look at your AI startup logs
   - Should say: `Uvicorn running on http://0.0.0.0:8000`
   - NOT: `Uvicorn running on http://127.0.0.1:8000`

3. **Verify your PC's IP hasn't changed:**
   ```powershell
   ipconfig | findstr IPv4
   ```
   - Should show `10.164.138.133`
   - If different, update `local.properties` in Android app

4. **Check if phone and PC are on same network:**
   - Both should be on same WiFi network
   - Some networks isolate devices (guest networks, public WiFi)

5. **Temporarily disable Windows Firewall to test:**
   ```powershell
   Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False
   ```
   
   **IMPORTANT:** Re-enable after testing:
   ```powershell
   Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled True
   ```

## After Fixing

Once the connection works:
1. The AI Terminal in your app should show green status
2. Click "RUN PIPELINE" to start AI processing
3. AI scores should update from 15-17% to real values
4. Market data will flow: App → Backend → Redis → AI → Decisions → App
