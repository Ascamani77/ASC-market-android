# Get cTrader Demo Token - Manual Method (No Local Server)
# This method doesn't require any local ports

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader DEMO Token - Manual Method" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This method doesn't require a local server." -ForegroundColor Yellow
Write-Host "You'll copy the authorization code from the browser URL." -ForegroundColor Yellow
Write-Host ""

$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$DEMO_ACCOUNT_ID = "47340965"

# Use a redirect URI that doesn't need to work
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

Write-Host "I will open a browser for authorization." -ForegroundColor Yellow
Write-Host ""
Write-Host "IMPORTANT INSTRUCTIONS:" -ForegroundColor Red
Write-Host "  1. Sign in with your DEMO credentials (NOT live!)" -ForegroundColor White
Write-Host "  2. Click 'Allow access' or 'Grant access'" -ForegroundColor White
Write-Host "  3. The page will fail to load (that's OK!)" -ForegroundColor Yellow
Write-Host "  4. Look at the browser address bar" -ForegroundColor White
Write-Host "  5. Copy the FULL URL from the address bar" -ForegroundColor White
Write-Host ""
Write-Host "The URL will look like:" -ForegroundColor Gray
Write-Host "  http://localhost:9999/callback?code=ABC123XYZ..." -ForegroundColor DarkGray
Write-Host ""
Write-Host "You need to copy the part after 'code='" -ForegroundColor Yellow
Write-Host ""

$ready = Read-Host "Ready to open browser? (y/n)"

if ($ready -ne "y" -and $ready -ne "Y") {
    Write-Host "Cancelled." -ForegroundColor Yellow
    pause
    exit 0
}

Write-Host ""
Write-Host "Opening browser..." -ForegroundColor Yellow
Write-Host ""

# Open browser
Start-Process $authUrl

Write-Host "After authorizing:" -ForegroundColor Cyan
Write-Host "  1. The page will show an error (that's normal)" -ForegroundColor White
Write-Host "  2. Look at the browser address bar" -ForegroundColor White
Write-Host "  3. Find the 'code=' part in the URL" -ForegroundColor White
Write-Host "  4. Copy everything after 'code=' (until the end or '&')" -ForegroundColor White
Write-Host ""
Write-Host "Example:" -ForegroundColor Gray
Write-Host "  URL: http://localhost:9999/callback?code=ABC123XYZ456" -ForegroundColor DarkGray
Write-Host "  Copy: ABC123XYZ456" -ForegroundColor DarkGray
Write-Host ""

$code = Read-Host "Paste the authorization code here"

if (-not $code) {
    Write-Host "❌ No code provided" -ForegroundColor Red
    pause
    exit 1
}

# Clean up the code (remove any URL parts)
$code = $code.Trim()
if ($code -match 'code=([^&]+)') {
    $code = $matches[1]
}

Write-Host ""
Write-Host "✅ Authorization code received!" -ForegroundColor Green
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
    Write-Host "Error: $_" -ForegroundColor Gray
    Write-Host ""
    
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "Possible reasons:" -ForegroundColor Yellow
        Write-Host "  1. Authorization code expired (they expire in ~10 minutes)" -ForegroundColor White
        Write-Host "  2. Code was already used (can only use once)" -ForegroundColor White
        Write-Host "  3. Code was copied incorrectly" -ForegroundColor White
        Write-Host ""
        Write-Host "Please run this script again and get a fresh code." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
