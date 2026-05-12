# Pepperstone cTrader Bridge Status Checker
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Pepperstone Bridge Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if bridge is running
Write-Host "1. Checking if bridge is running..." -ForegroundColor Yellow
$pythonProcesses = Get-Process python -ErrorAction SilentlyContinue | Where-Object {
    $_.MainWindowTitle -like "*ctrader*" -or 
    (Get-NetTCPConnection -OwningProcess $_.Id -ErrorAction SilentlyContinue | Where-Object LocalPort -eq 8082)
}

if ($pythonProcesses) {
    Write-Host "   OK Bridge process found (PID: $($pythonProcesses.Id -join ', '))" -ForegroundColor Green
} else {
    Write-Host "   ERROR Bridge is NOT running!" -ForegroundColor Red
    Write-Host "   Solution: Run .\start_ctrader_bridge.ps1" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Check if port 8082 is listening
Write-Host ""
Write-Host "2. Checking if port 8082 is listening..." -ForegroundColor Yellow
$port8082 = Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue
if ($port8082) {
    Write-Host "   OK Port 8082 is listening" -ForegroundColor Green
} else {
    Write-Host "   ERROR Port 8082 is NOT listening!" -ForegroundColor Red
    Write-Host "   The bridge may have failed to start" -ForegroundColor Yellow
}

# Check environment variables
Write-Host ""
Write-Host "3. Checking environment variables..." -ForegroundColor Yellow
$requiredVars = @(
    "CTRADER_CLIENT_ID",
    "CTRADER_CLIENT_SECRET", 
    "CTRADER_ACCESS_TOKEN",
    "CTRADER_HOST_TYPE"
)

$allVarsSet = $true
foreach ($var in $requiredVars) {
    $value = [Environment]::GetEnvironmentVariable($var)
    if ($value) {
        $masked = if ($value.Length -gt 10) { $value.Substring(0, 10) + "..." } else { $value }
        Write-Host "   OK $var = $masked" -ForegroundColor Green
    } else {
        Write-Host "   ERROR $var is NOT set!" -ForegroundColor Red
        $allVarsSet = $false
    }
}

if (-not $allVarsSet) {
    Write-Host ""
    Write-Host "   Solution: Environment variables are only set in the bridge process" -ForegroundColor Yellow
    Write-Host "   This is normal if you're checking from a different terminal" -ForegroundColor Gray
}

# Test WebSocket connection
Write-Host ""
Write-Host "4. Testing WebSocket connection to bridge..." -ForegroundColor Yellow
try {
    $ws = New-Object System.Net.WebSockets.ClientWebSocket
    $uri = [System.Uri]::new("ws://localhost:8082")
    $cts = New-Object System.Threading.CancellationTokenSource
    $cts.CancelAfter(5000) # 5 second timeout
    
    $connectTask = $ws.ConnectAsync($uri, $cts.Token)
    $connectTask.Wait()
    
    if ($ws.State -eq 'Open') {
        Write-Host "   OK WebSocket connection successful!" -ForegroundColor Green
        
        # Send a status request
        $statusMsg = '{"action":"status"}'
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($statusMsg)
        $segment = [System.ArraySegment[byte]]::new($bytes)
        $sendTask = $ws.SendAsync($segment, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cts.Token)
        $sendTask.Wait()
        
        Write-Host "   Sent status request, waiting for response..." -ForegroundColor Gray
        
        # Try to receive response
        $buffer = New-Object byte[] 4096
        $segment = [System.ArraySegment[byte]]::new($buffer)
        $receiveTask = $ws.ReceiveAsync($segment, $cts.Token)
        
        $timeout = New-TimeSpan -Seconds 3
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        
        while (-not $receiveTask.IsCompleted -and $sw.Elapsed -lt $timeout) {
            Start-Sleep -Milliseconds 100
        }
        
        if ($receiveTask.IsCompleted) {
            $result = $receiveTask.Result
            $response = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count)
            Write-Host "   Response: $response" -ForegroundColor Cyan
            
            # Parse response
            try {
                $json = $response | ConvertFrom-Json
                Write-Host ""
                Write-Host "   Bridge Status:" -ForegroundColor Yellow
                Write-Host "   - State: $($json.state)" -ForegroundColor $(if ($json.state -eq 'connected') { 'Green' } else { 'Red' })
                Write-Host "   - Message: $($json.message)" -ForegroundColor Gray
                if ($json.applicationAuthed) {
                    Write-Host "   - Application Authenticated: YES" -ForegroundColor Green
                } else {
                    Write-Host "   - Application Authenticated: NO" -ForegroundColor Red
                }
                if ($json.accountAuthed) {
                    Write-Host "   - Account Authenticated: YES" -ForegroundColor Green
                } else {
                    Write-Host "   - Account Authenticated: NO" -ForegroundColor Red
                }
                if ($json.symbolsLoaded) {
                    Write-Host "   - Symbols Loaded: YES" -ForegroundColor Green
                } else {
                    Write-Host "   - Symbols Loaded: NO" -ForegroundColor Red
                }
            } catch {
                Write-Host "   Could not parse response as JSON" -ForegroundColor Yellow
            }
        } else {
            Write-Host "   WARNING No response received (timeout)" -ForegroundColor Yellow
        }
        
        $ws.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "Done", $cts.Token).Wait()
    } else {
        Write-Host "   ERROR Could not connect (State: $($ws.State))" -ForegroundColor Red
    }
    
    $ws.Dispose()
    $cts.Dispose()
} catch {
    Write-Host "   ERROR WebSocket test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Summary
Write-Host "SUMMARY:" -ForegroundColor Cyan
Write-Host "If all checks passed, the bridge is working correctly." -ForegroundColor Gray
Write-Host "If authentication failed, check your credentials in local.properties" -ForegroundColor Gray
Write-Host "If the bridge is not running, start it with: .\start_ctrader_bridge.ps1" -ForegroundColor Gray
Write-Host ""
