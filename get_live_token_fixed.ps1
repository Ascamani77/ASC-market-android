# Generate Access Token for LIVE Account 1360716

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Live Account Token Generator (Fixed)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Target Account: 1360716 (LIVE)" -ForegroundColor Yellow
Write-Host ""

$CLIENT_ID = "$env:CTRADER_CLIENT_ID"
$CLIENT_SECRET = "$env:CTRADER_CLIENT_SECRET"
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
Write-Host "5. Copy the FULL URL from browser" -ForegroundColor White
Write-Host ""
Write-Host "IMPORTANT: Paste within 60 seconds or code expires!" -ForegroundColor Yellow
Write-Host ""

# Get authorization code from user
$input = Read-Host "Paste the full URL here and press Enter"

# Extract code from URL
$code = $input
if ($input -match "code=([^&]+)") {
    $code = $matches[1]
    Write-Host "[OK] Extracted code: $($code.Substring(0, 20))..." -ForegroundColor Green
} else {
    Write-Host "[ERROR] Could not extract code from URL" -ForegroundColor Red
    exit 1
}

Write-Host "Exchanging code for token..." -ForegroundColor Cyan

# Exchange code for token
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
        Write-Host ""
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
            $scriptContent = $scriptContent -replace '\$env:CTRADER_ACCESS_TOKEN = ".*?"', "`$env:CTRADER_ACCESS_TOKEN = `"$($tokenData.access_token)`""
            
            # Update account ID - NOTE: We'll use 1360716 temporarily, then get real API ID
            $scriptContent = $scriptContent -replace '\$env:CTRADER_ACCOUNT_ID = ".*?"', '$env:CTRADER_ACCOUNT_ID = "1360716"'
            
            # Ensure host type is live
            $scriptContent = $scriptContent -replace '\$env:CTRADER_HOST_TYPE = ".*?"', '$env:CTRADER_HOST_TYPE = "live"'
            
            $scriptContent | Set-Content $scriptPath
            Write-Host "[OK] Updated start_ctrader_bridge.ps1" -ForegroundColor Green
        }
        
        Write-Host ""
        Write-Host "============================================================" -ForegroundColor Cyan
        Write-Host "SUCCESS! Live account token configured" -ForegroundColor Green
        Write-Host "============================================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Next Step: Get the API account ID" -ForegroundColor Yellow
        Write-Host "Run: python get_live_account_id.py" -ForegroundColor White
        Write-Host ""
        
    } else {
        Write-Host "[ERROR] No access token in response" -ForegroundColor Red
        Write-Host "Response:" -ForegroundColor Yellow
        $tokenData | ConvertTo-Json
    }
} catch {
    Write-Host "[ERROR] Request failed: $($_.Exception.Message)" -ForegroundColor Red
    
    # Try to parse error response
    try {
        $errorResponse = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Host ""
        Write-Host "Error Code: $($errorResponse.errorCode)" -ForegroundColor Red
        Write-Host "Description: $($errorResponse.description)" -ForegroundColor Red
        
        if ($errorResponse.errorCode -eq "ACCESS_DENIED") {
            Write-Host ""
            Write-Host "The authorization code has expired (60 second limit)." -ForegroundColor Yellow
            Write-Host "Please run the script again and paste the code faster." -ForegroundColor Yellow
        }
        
        if ($errorResponse.description -match "Too Many Attempts") {
            Write-Host ""
            Write-Host "Rate limited! Wait 5-10 minutes before trying again." -ForegroundColor Yellow
        }
    } catch {
        # Could not parse error
    }
}
