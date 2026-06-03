# Get cTrader Access Token via OAuth2 Flow
# This script helps you get an access token using the OAuth2 authorization code flow

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader OAuth2 Token Generator" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Your application credentials
$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"

# You need to have a redirect URI configured in your app
# Common redirect URIs: http://localhost:8080, https://your-domain.com/callback
Write-Host "Enter your redirect URI (configured in cTrader app settings):" -ForegroundColor Yellow
Write-Host "  Example: http://localhost:8080" -ForegroundColor Gray
$REDIRECT_URI = Read-Host "Redirect URI"

if (-not $REDIRECT_URI) {
    Write-Host "❌ Redirect URI is required" -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "Select account type:" -ForegroundColor Yellow
Write-Host "  1. DEMO (for testing)" -ForegroundColor White
Write-Host "  2. LIVE (for real trading)" -ForegroundColor White
$choice = Read-Host "Enter choice (1 or 2)"

$scope = "trading"  # Full access (trading + accounts)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 1: Get Authorization Code" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Build authorization URL
$authUrl = "https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=$CLIENT_ID&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&scope=$scope&product=web"

Write-Host "Opening browser for authorization..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Authorization URL:" -ForegroundColor Gray
Write-Host $authUrl -ForegroundColor DarkGray
Write-Host ""

# Open browser
Start-Process $authUrl

Write-Host "Instructions:" -ForegroundColor Cyan
Write-Host "  1. A browser window will open" -ForegroundColor White
Write-Host "  2. Sign in with your cTrader credentials" -ForegroundColor White
if ($choice -eq "1") {
    Write-Host "     → Use DEMO credentials!" -ForegroundColor Yellow
} else {
    Write-Host "     → Use LIVE credentials!" -ForegroundColor Red
}
Write-Host "  3. Click 'Allow access' to authorize the app" -ForegroundColor White
Write-Host "  4. You'll be redirected to: $REDIRECT_URI" -ForegroundColor White
Write-Host "  5. Copy the FULL redirect URL from your browser" -ForegroundColor White
Write-Host ""
Write-Host "Example redirect URL:" -ForegroundColor Gray
Write-Host "  $REDIRECT_URI/?code=ABC123XYZ..." -ForegroundColor DarkGray
Write-Host ""

$redirectedUrl = Read-Host "Paste the full redirect URL here"

if (-not $redirectedUrl) {
    Write-Host "❌ No URL provided" -ForegroundColor Red
    pause
    exit 1
}

# Extract authorization code from URL
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
Write-Host "✅ Authorization code extracted: $($code.Substring(0, 20))..." -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  STEP 2: Exchange for Access Token" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Exchange authorization code for access token
$tokenUrl = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$code&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"

Write-Host "Requesting access token..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri $tokenUrl -Method GET -Headers @{
        "Accept" = "application/json"
        "Content-Type" = "application/json"
    }
    
    if ($response.accessToken) {
        Write-Host "✅ Access token received!" -ForegroundColor Green
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  TOKEN DETAILS" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Access Token:" -ForegroundColor Cyan
        Write-Host $response.accessToken -ForegroundColor White
        Write-Host ""
        Write-Host "Refresh Token:" -ForegroundColor Cyan
        Write-Host $response.refreshToken -ForegroundColor White
        Write-Host ""
        Write-Host "Expires In: $($response.expiresIn) seconds (~$([math]::Round($response.expiresIn / 86400, 1)) days)" -ForegroundColor Gray
        Write-Host ""
        
        # Save to file
        $tokenData = @{
            accessToken = $response.accessToken
            refreshToken = $response.refreshToken
            expiresIn = $response.expiresIn
            tokenType = $response.tokenType
            generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            accountType = if ($choice -eq "1") { "DEMO" } else { "LIVE" }
        }
        
        $tokenFile = if ($choice -eq "1") { "ctrader_demo_token.json" } else { "ctrader_live_token.json" }
        $tokenData | ConvertTo-Json | Out-File -FilePath $tokenFile -Encoding UTF8
        
        Write-Host "✅ Token saved to: $tokenFile" -ForegroundColor Green
        Write-Host ""
        
        # Offer to start bridge
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "  START BRIDGE?" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        
        $startBridge = Read-Host "Start the bridge now? (y/n)"
        
        if ($startBridge -eq "y" -or $startBridge -eq "Y") {
            Write-Host ""
            if ($choice -eq "1") {
                # Start demo bridge
                $DEMO_ACCOUNT_ID = "47340965"
                
                Write-Host "Starting DEMO bridge..." -ForegroundColor Cyan
                docker stop ctrader-bridge-demo 2>$null | Out-Null
                docker rm ctrader-bridge-demo 2>$null | Out-Null
                
                docker run -d --name ctrader-bridge-demo --restart unless-stopped -p 8083:8083 `
                  -e CTRADER_CLIENT_ID="$CLIENT_ID" `
                  -e CTRADER_CLIENT_SECRET="$CLIENT_SECRET" `
                  -e CTRADER_ACCESS_TOKEN="$($response.accessToken)" `
                  -e CTRADER_ACCOUNT_ID="$DEMO_ACCOUNT_ID" `
                  -e CTRADER_HOST_TYPE="demo" `
                  -e CTRADER_BRIDGE_HOST="0.0.0.0" `
                  -e CTRADER_BRIDGE_PORT="8083" `
                  myrealapp-ctrader-bridge:latest
                
                Write-Host "✅ DEMO bridge started on port 8083" -ForegroundColor Green
                Write-Host "   View logs: docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
            } else {
                # Start live bridge
                $LIVE_ACCOUNT_ID = "47341092"
                
                Write-Host "Starting LIVE bridge..." -ForegroundColor Cyan
                docker stop ctrader-bridge-live 2>$null | Out-Null
                docker rm ctrader-bridge-live 2>$null | Out-Null
                
                docker run -d --name ctrader-bridge-live --restart unless-stopped -p 8082:8082 `
                  -e CTRADER_CLIENT_ID="$CLIENT_ID" `
                  -e CTRADER_CLIENT_SECRET="$CLIENT_SECRET" `
                  -e CTRADER_ACCESS_TOKEN="$($response.accessToken)" `
                  -e CTRADER_ACCOUNT_ID="$LIVE_ACCOUNT_ID" `
                  -e CTRADER_HOST_TYPE="live" `
                  -e CTRADER_BRIDGE_HOST="0.0.0.0" `
                  -e CTRADER_BRIDGE_PORT="8082" `
                  myrealapp-ctrader-bridge:latest
                
                Write-Host "✅ LIVE bridge started on port 8082" -ForegroundColor Green
                Write-Host "   View logs: docker logs -f ctrader-bridge-live" -ForegroundColor Gray
            }
        }
        
    } else {
        Write-Host "❌ Failed to get access token" -ForegroundColor Red
        Write-Host "Error: $($response.errorCode)" -ForegroundColor Red
        Write-Host "Description: $($response.description)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "❌ Error requesting token: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Yellow
    Write-Host $_.Exception.Message -ForegroundColor Gray
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
