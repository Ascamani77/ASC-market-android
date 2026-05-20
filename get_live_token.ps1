# Generate Access Token for LIVE Account 1360716
# Fast token generation using curl

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Live Account Token Generator" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Target Account: 1360716 (LIVE)" -ForegroundColor Yellow
Write-Host ""

$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$REDIRECT_URI = "http://localhost:8888/callback"
$SCOPE = "trading"

# Build authorization URL
$authUrl = "https://id.ctrader.com/my/settings/openapi/grantingaccess/?" +
    "client_id=$CLIENT_ID&" +
    "redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&" +
    "scope=$SCOPE&" +
    "product=web"

Write-Host "Authorization URL (copy and open in browser):" -ForegroundColor Green
Write-Host $authUrl -ForegroundColor White
Write-Host ""
Write-Host "Steps:" -ForegroundColor Yellow
Write-Host "1. Open the URL above in your browser" -ForegroundColor White
Write-Host "2. Login to cTrader" -ForegroundColor White
Write-Host "3. SELECT ACCOUNT 1360716 (LIVE ACCOUNT)" -ForegroundColor Red
Write-Host "4. Click 'Allow access'" -ForegroundColor White
Write-Host "5. Copy the FULL URL from browser (or just the code)" -ForegroundColor White
Write-Host ""

# Get authorization code from user
$input = Read-Host "Paste the code (or full URL) here and press Enter IMMEDIATELY"

# Extract code from URL if full URL was pasted
$code = $input
if ($input -match "code=([^&]+)") {
    $code = $matches[1]
    Write-Host "[OK] Extracted code from URL" -ForegroundColor Green
}

Write-Host "Exchanging code for token..." -ForegroundColor Cyan

# Exchange code for token using Invoke-RestMethod
$tokenUrl = "https://openapi.ctrader.com/apps/token"
$body = @{
    grant_type = "authorization_code"
    code = $code
    redirect_uri = $REDIRECT_URI
    client_id = $CLIENT_ID
    client_secret = $CLIENT_SECRET
}

try {
    $tokenData = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    
    if ($tokenData.access_token) {
        Write-Host "[SUCCESS] Live account token received!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Access Token:  $($tokenData.access_token.Substring(0, 30))..." -ForegroundColor White
        Write-Host "Refresh Token: $($tokenData.refresh_token.Substring(0, 30))..." -ForegroundColor White
        Write-Host "Expires In:    $($tokenData.expires_in) seconds (~30 days)" -ForegroundColor White
        Write-Host ""
        
        # Save to file
        $tokenData | ConvertTo-Json | Set-Content "ctrader_tokens_live.json"
        Write-Host "[OK] Saved to ctrader_tokens_live.json" -ForegroundColor Green
        
        # Update start_ctrader_bridge.ps1
        $scriptPath = ".\start_ctrader_bridge.ps1"
        if (Test-Path $scriptPath) {
            $scriptContent = Get-Content $scriptPath -Raw
            
            # Update access token
            $scriptContent = $scriptContent -replace 'CTRADER_ACCESS_TOKEN = ".*?"', "CTRADER_ACCESS_TOKEN = `"$($tokenData.access_token)`""
            
            # Update account ID to live account
            $scriptContent = $scriptContent -replace 'CTRADER_ACCOUNT_ID = ".*?"', 'CTRADER_ACCOUNT_ID = "1360716"'
            
            # Ensure host type is live
            $scriptContent = $scriptContent -replace 'CTRADER_HOST_TYPE = ".*?"', 'CTRADER_HOST_TYPE = "live"'
            
            $scriptContent | Set-Content $scriptPath
            Write-Host "[OK] Updated start_ctrader_bridge.ps1" -ForegroundColor Green
        }
        
        # Set environment variables for immediate use
        $env:CTRADER_ACCESS_TOKEN = $tokenData.access_token
        $env:CTRADER_ACCOUNT_ID = "1360716"
        $env:CTRADER_HOST_TYPE = "live"
        Write-Host "[OK] Environment variables set" -ForegroundColor Green
        
        Write-Host ""
        Write-Host "============================================================" -ForegroundColor Cyan
        Write-Host "SUCCESS! Live account token configured" -ForegroundColor Green
        Write-Host "============================================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Configuration:" -ForegroundColor Yellow
        Write-Host "  Account ID: 1360716 (LIVE)" -ForegroundColor White
        Write-Host "  Host Type: live" -ForegroundColor White
        Write-Host "  Token Valid: 30 days" -ForegroundColor White
        Write-Host ""
        Write-Host "Next: Run .\start_ctrader_bridge.ps1" -ForegroundColor Green
        
    } else {
        Write-Host "[ERROR] Failed to get token" -ForegroundColor Red
        Write-Host "Response: $response" -ForegroundColor Yellow
        
        if ($tokenData.message) {
            Write-Host "Error: $($tokenData.message)" -ForegroundColor Red
            if ($tokenData.message -match "Too Many Attempts") {
                Write-Host ""
                Write-Host "Rate limited! Wait 5-10 minutes before trying again." -ForegroundColor Yellow
            }
        }
    }
} catch {
    Write-Host "[ERROR] Request failed: $($_.Exception.Message)" -ForegroundColor Red
}
