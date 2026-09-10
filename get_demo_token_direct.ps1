# Get cTrader Demo Token - Direct Method (No Portal Access Needed)
# This method uses a simple local HTTP server to receive the OAuth callback

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader DEMO Token - Direct Method" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This method doesn't require accessing the cTrader portal." -ForegroundColor Yellow
Write-Host "We'll use a local HTTP server to receive the OAuth callback." -ForegroundColor Yellow
Write-Host ""

$CLIENT_ID = "$env:CTRADER_CLIENT_ID"
$CLIENT_SECRET = "$env:CTRADER_CLIENT_SECRET"
$DEMO_ACCOUNT_ID = "47340965"
$REDIRECT_URI = "http://localhost:8080/callback"

Write-Host "Demo Account:" -ForegroundColor Cyan
Write-Host "  Account ID: $DEMO_ACCOUNT_ID" -ForegroundColor White
Write-Host "  Account Number: 5288664" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 1: Start Local Server" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Starting local HTTP server on port 8080..." -ForegroundColor Yellow

# Create a simple HTTP listener
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:8080/")

try {
    $listener.Start()
    Write-Host "✅ Server started on http://localhost:8080" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to start server on port 8080" -ForegroundColor Red
    Write-Host "   Port may be in use. Close any apps using port 8080 and try again." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 2: Authorize Application" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Build authorization URL
$authUrl = "https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=$CLIENT_ID&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&scope=trading&product=web"

Write-Host "I will now open a browser for authorization." -ForegroundColor Yellow
Write-Host ""
Write-Host "In the browser:" -ForegroundColor Cyan
Write-Host "  1. Sign in with your DEMO credentials" -ForegroundColor White
Write-Host "  2. Click 'Allow access' or 'Grant access'" -ForegroundColor White
Write-Host "  3. You'll be redirected back automatically" -ForegroundColor White
Write-Host ""
Write-Host "⏳ Waiting for authorization..." -ForegroundColor Yellow
Write-Host ""

# Open browser
Start-Process $authUrl

# Wait for callback
$code = $null
$timeout = 300  # 5 minutes
$elapsed = 0

Write-Host "Listening for callback (timeout: 5 minutes)..." -ForegroundColor Gray
Write-Host ""

while ($elapsed -lt $timeout -and $null -eq $code) {
    if ($listener.IsListening) {
        $contextTask = $listener.GetContextAsync()
        
        # Wait for 1 second or until request arrives
        $completed = $contextTask.AsyncWaitHandle.WaitOne(1000)
        
        if ($completed) {
            $context = $contextTask.Result
            $request = $context.Request
            $response = $context.Response
            
            # Check if this is the callback
            if ($request.Url.AbsolutePath -eq "/callback") {
                $queryParams = $request.Url.Query
                
                if ($queryParams -match '[?&]code=([^&]+)') {
                    $code = $matches[1]
                    
                    # Send success response to browser
                    $html = @"
<!DOCTYPE html>
<html>
<head>
    <title>Authorization Successful</title>
    <style>
        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
        .success { background: #4CAF50; color: white; padding: 20px; border-radius: 10px; display: inline-block; }
        h1 { margin: 0 0 10px 0; }
    </style>
</head>
<body>
    <div class="success">
        <h1>✅ Authorization Successful!</h1>
        <p>You can close this window and return to PowerShell.</p>
    </div>
</body>
</html>
"@
                    $buffer = [System.Text.Encoding]::UTF8.GetBytes($html)
                    $response.ContentLength64 = $buffer.Length
                    $response.ContentType = "text/html"
                    $response.OutputStream.Write($buffer, 0, $buffer.Length)
                    $response.OutputStream.Close()
                    
                    Write-Host "✅ Authorization code received!" -ForegroundColor Green
                    break
                } else {
                    # Error in callback
                    $html = @"
<!DOCTYPE html>
<html>
<head>
    <title>Authorization Failed</title>
    <style>
        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0f0f0; }
        .error { background: #f44336; color: white; padding: 20px; border-radius: 10px; display: inline-block; }
        h1 { margin: 0 0 10px 0; }
    </style>
</head>
<body>
    <div class="error">
        <h1>❌ Authorization Failed</h1>
        <p>No authorization code found. Please try again.</p>
    </div>
</body>
</html>
"@
                    $buffer = [System.Text.Encoding]::UTF8.GetBytes($html)
                    $response.ContentLength64 = $buffer.Length
                    $response.ContentType = "text/html"
                    $response.OutputStream.Write($buffer, 0, $buffer.Length)
                    $response.OutputStream.Close()
                }
            }
        }
    }
    
    $elapsed++
}

$listener.Stop()
$listener.Close()

if ($null -eq $code) {
    Write-Host ""
    Write-Host "❌ Timeout: No authorization received" -ForegroundColor Red
    Write-Host "   Please try again and complete the authorization quickly." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 3: Exchange for Access Token" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$tokenUrl = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$code&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"

Write-Host "Requesting access token..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri $tokenUrl -Method GET -Headers @{
        "Accept" = "application/json"
    }
    
    if ($response.accessToken) {
        Write-Host "✅ Access token received!" -ForegroundColor Green
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  SUCCESS!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Access Token:" -ForegroundColor Cyan
        Write-Host $response.accessToken -ForegroundColor White
        Write-Host ""
        Write-Host "Expires In: $($response.expiresIn) seconds (~$([math]::Round($response.expiresIn / 3600, 1)) hours)" -ForegroundColor Gray
        Write-Host ""
        
        # Save to file
        $tokenData = @{
            accessToken = $response.accessToken
            refreshToken = $response.refreshToken
            expiresIn = $response.expiresIn
            tokenType = $response.tokenType
            generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            accountType = "DEMO"
            accountId = $DEMO_ACCOUNT_ID
        }
        
        $tokenFile = "ctrader_demo_token.json"
        $tokenData | ConvertTo-Json | Out-File -FilePath $tokenFile -Encoding UTF8
        
        Write-Host "✅ Token saved to: $tokenFile" -ForegroundColor Green
        Write-Host ""
        
        # Ask to start bridge
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "  STEP 4: Start Demo Bridge" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        
        $startBridge = Read-Host "Start the demo bridge now? (y/n)"
        
        if ($startBridge -eq "y" -or $startBridge -eq "Y") {
            Write-Host ""
            Write-Host "Starting demo bridge..." -ForegroundColor Cyan
            
            # Stop existing container
            docker stop ctrader-bridge-demo 2>$null | Out-Null
            docker rm ctrader-bridge-demo 2>$null | Out-Null
            
            # Start new container
            docker run -d --name ctrader-bridge-demo --restart unless-stopped -p 8083:8083 `
              -e CTRADER_CLIENT_ID="$CLIENT_ID" `
              -e CTRADER_CLIENT_SECRET="$CLIENT_SECRET" `
              -e CTRADER_ACCESS_TOKEN="$($response.accessToken)" `
              -e CTRADER_ACCOUNT_ID="$DEMO_ACCOUNT_ID" `
              -e CTRADER_HOST_TYPE="demo" `
              -e CTRADER_BRIDGE_HOST="0.0.0.0" `
              -e CTRADER_BRIDGE_PORT="8083" `
              myrealapp-ctrader-bridge:latest
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ Demo bridge started!" -ForegroundColor Green
                Write-Host ""
                Write-Host "Waiting for connection..." -ForegroundColor Yellow
                Start-Sleep -Seconds 5
                
                Write-Host ""
                Write-Host "Checking logs..." -ForegroundColor Yellow
                $logs = docker logs ctrader-bridge-demo --tail 20 2>&1
                
                if ($logs -match "account_authenticated") {
                    Write-Host "✅ Bridge authenticated successfully!" -ForegroundColor Green
                } else {
                    Write-Host "⏳ Still connecting... Check logs:" -ForegroundColor Yellow
                    Write-Host "   docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
                }
                
                Write-Host ""
                Write-Host "========================================" -ForegroundColor Green
                Write-Host "  DEMO BRIDGE RUNNING" -ForegroundColor Green
                Write-Host "========================================" -ForegroundColor Green
                Write-Host ""
                Write-Host "WebSocket URL: ws://localhost:8083" -ForegroundColor White
                Write-Host "Account: $DEMO_ACCOUNT_ID (DEMO)" -ForegroundColor White
                Write-Host ""
                Write-Host "View logs: docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
                Write-Host ""
            } else {
                Write-Host "❌ Failed to start container" -ForegroundColor Red
            }
        } else {
            Write-Host ""
            Write-Host "To start the bridge later, run:" -ForegroundColor Yellow
            Write-Host "  .\start_demo_bridge.ps1" -ForegroundColor Gray
            Write-Host ""
        }
        
    } else {
        Write-Host "❌ Failed to get access token" -ForegroundColor Red
        Write-Host "Response: $($response | ConvertTo-Json)" -ForegroundColor Gray
    }
    
} catch {
    Write-Host "❌ Error requesting token" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Gray
    Write-Host ""
    
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "The authorization code may have expired." -ForegroundColor Yellow
        Write-Host "Please run this script again." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
