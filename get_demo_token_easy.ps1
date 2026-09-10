# Get cTrader Demo Token - Easy Method
# This script helps you extract the code correctly

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader DEMO Token - Easy Method" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$CLIENT_ID = "$env:CTRADER_CLIENT_ID"
$CLIENT_SECRET = "$env:CTRADER_CLIENT_SECRET"
$DEMO_ACCOUNT_ID = "47340965"
$REDIRECT_URI = "http://localhost:9999/callback"

Write-Host "Demo Account:" -ForegroundColor Cyan
Write-Host "  Account ID: $DEMO_ACCOUNT_ID" -ForegroundColor White
Write-Host "  Account Number: 5288664" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 1: Get Authorization Code" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Build authorization URL
$authUrl = "https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=$CLIENT_ID&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&scope=trading&product=web"

Write-Host "INSTRUCTIONS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. I will open a browser" -ForegroundColor White
Write-Host "2. Sign in with DEMO credentials" -ForegroundColor White
Write-Host "3. Click 'Allow access'" -ForegroundColor White
Write-Host "4. Page will fail to load (that's OK!)" -ForegroundColor White
Write-Host "5. Copy the ENTIRE URL from browser address bar" -ForegroundColor White
Write-Host "6. Paste it here - I'll extract the code for you" -ForegroundColor White
Write-Host ""

$ready = Read-Host "Ready? (y/n)"

if ($ready -ne "y" -and $ready -ne "Y") {
    Write-Host "Cancelled." -ForegroundColor Yellow
    pause
    exit 0
}

Write-Host ""
Write-Host "Opening browser..." -ForegroundColor Yellow
Start-Process $authUrl
Start-Sleep -Seconds 2

Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  PASTE THE FULL URL HERE" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "After authorizing, the browser will show an error page." -ForegroundColor White
Write-Host "That's normal! Just copy the FULL URL from the address bar." -ForegroundColor White
Write-Host ""
Write-Host "Example of what to copy:" -ForegroundColor Gray
Write-Host "http://localhost:9999/callback?code=ABC123XYZ&client_id=..." -ForegroundColor DarkGray
Write-Host ""
Write-Host "Paste the full URL here:" -ForegroundColor Cyan

$fullUrl = Read-Host

if (-not $fullUrl) {
    Write-Host "❌ No URL provided" -ForegroundColor Red
    pause
    exit 1
}

# Extract code from URL
$code = $null

# Try different patterns
if ($fullUrl -match '[?&]code=([^&]+)') {
    $code = $matches[1]
    Write-Host ""
    Write-Host "✅ Found authorization code!" -ForegroundColor Green
    Write-Host "   Code: $($code.Substring(0, [Math]::Min(20, $code.Length)))..." -ForegroundColor Gray
} else {
    Write-Host ""
    Write-Host "❌ Could not find 'code=' in the URL" -ForegroundColor Red
    Write-Host ""
    Write-Host "The URL you pasted:" -ForegroundColor Yellow
    Write-Host $fullUrl -ForegroundColor Gray
    Write-Host ""
    Write-Host "Make sure you:" -ForegroundColor Yellow
    Write-Host "  1. Copied the FULL URL from the browser address bar" -ForegroundColor White
    Write-Host "  2. The URL contains '?code=' or '&code='" -ForegroundColor White
    Write-Host "  3. You authorized the application first" -ForegroundColor White
    pause
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 2: Exchange for Access Token" -ForegroundColor Cyan
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
        Write-Host "Refresh Token:" -ForegroundColor Cyan
        Write-Host $response.refreshToken -ForegroundColor White
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
        Write-Host "  STEP 3: Start Demo Bridge" -ForegroundColor Cyan
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
                    
                    # Show symbols loaded
                    $symbolsLine = $logs | Select-String "symbols_loaded"
                    if ($symbolsLine) {
                        Write-Host $symbolsLine -ForegroundColor Green
                    }
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
                Write-Host "Commands:" -ForegroundColor Cyan
                Write-Host "  View logs: docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
                Write-Host "  Stop: docker stop ctrader-bridge-demo" -ForegroundColor Gray
                Write-Host "  Restart: docker restart ctrader-bridge-demo" -ForegroundColor Gray
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
    Write-Host ""
    
    # Try to get error details
    $errorDetails = $_.ErrorDetails.Message
    if ($errorDetails) {
        try {
            $errorJson = $errorDetails | ConvertFrom-Json
            Write-Host "Error Code: $($errorJson.errorCode)" -ForegroundColor Red
            Write-Host "Description: $($errorJson.description)" -ForegroundColor Red
        } catch {
            Write-Host "Error: $_" -ForegroundColor Gray
        }
    } else {
        Write-Host "Error: $_" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  1. Authorization code expired (valid for ~10 minutes)" -ForegroundColor White
    Write-Host "  2. Code was already used (can only use once)" -ForegroundColor White
    Write-Host "  3. Wrong credentials (make sure you used DEMO account)" -ForegroundColor White
    Write-Host ""
    Write-Host "Please run this script again to get a fresh code." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
