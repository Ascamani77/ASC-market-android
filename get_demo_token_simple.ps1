# Simple Demo Token Generator for cTrader
# This script helps you get a demo access token step-by-step

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader DEMO Token Generator" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$DEMO_ACCOUNT_ID = "47340965"

Write-Host "Demo Account Info:" -ForegroundColor Yellow
Write-Host "  Account ID: $DEMO_ACCOUNT_ID" -ForegroundColor White
Write-Host "  Account Number: 5288664" -ForegroundColor White
Write-Host "  Balance: $50,000 (demo)" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 1: Configure Redirect URI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "First, you need to add a redirect URI to your cTrader app." -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Go to: https://id.ctrader.com/my/settings/openapi/applications" -ForegroundColor White
Write-Host "2. Sign in with your DEMO credentials" -ForegroundColor White
Write-Host "3. Find your application (Client ID: 27391_...)" -ForegroundColor White
Write-Host "4. Add a redirect URI (e.g., http://localhost:8080)" -ForegroundColor White
Write-Host "5. Save the changes" -ForegroundColor White
Write-Host ""

$continue = Read-Host "Have you added a redirect URI? (y/n)"
if ($continue -ne "y" -and $continue -ne "Y") {
    Write-Host "Please add a redirect URI first, then run this script again." -ForegroundColor Yellow
    pause
    exit 0
}

Write-Host ""
Write-Host "Enter the redirect URI you configured:" -ForegroundColor Yellow
Write-Host "  Example: http://localhost:8080" -ForegroundColor Gray
$REDIRECT_URI = Read-Host "Redirect URI"

if (-not $REDIRECT_URI) {
    Write-Host "❌ Redirect URI is required" -ForegroundColor Red
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

Write-Host "I will now open a browser window for authorization." -ForegroundColor Yellow
Write-Host ""
Write-Host "In the browser:" -ForegroundColor Cyan
Write-Host "  1. Sign in with your DEMO credentials (NOT live!)" -ForegroundColor White
Write-Host "  2. Click 'Allow access' to authorize the app" -ForegroundColor White
Write-Host "  3. You'll be redirected to: $REDIRECT_URI" -ForegroundColor White
Write-Host "  4. Copy the FULL URL from the browser address bar" -ForegroundColor White
Write-Host ""
Write-Host "Example redirect URL:" -ForegroundColor Gray
Write-Host "  $REDIRECT_URI/?code=ABC123XYZ..." -ForegroundColor DarkGray
Write-Host ""

$ready = Read-Host "Ready to open browser? (y/n)"
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
Write-Host "After authorizing, paste the full redirect URL here:" -ForegroundColor Yellow
$redirectedUrl = Read-Host "Redirect URL"

if (-not $redirectedUrl) {
    Write-Host "❌ No URL provided" -ForegroundColor Red
    pause
    exit 1
}

# Extract authorization code
$code = $null
if ($redirectedUrl -match '[?&]code=([^&]+)') {
    $code = $matches[1]
} else {
    Write-Host "❌ Could not find authorization code in URL" -ForegroundColor Red
    Write-Host "   Make sure you copied the full URL including ?code=..." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host ""
Write-Host "✅ Authorization code found!" -ForegroundColor Green
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
    
    # Check if it's a 400 error (common with expired codes)
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "The authorization code may have expired." -ForegroundColor Yellow
        Write-Host "Authorization codes are only valid for a few minutes." -ForegroundColor Yellow
        Write-Host "Please run this script again and complete the process quickly." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
