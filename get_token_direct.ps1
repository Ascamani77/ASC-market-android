# Direct Token Generator with Embedded Credentials
# This script has the credentials built-in

$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$REDIRECT_URI = "http://localhost:8888/callback"
$SCOPE = "trading"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Pepperstone cTrader Token Generator" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "`n[OK] Using embedded credentials" -ForegroundColor Green

# Build authorization URL
$authUrl = "https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=$CLIENT_ID&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&scope=$SCOPE&product=web"

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "IMPORTANT: Add Redirect URI First!" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "`nBefore continuing, you MUST add this redirect URI:" -ForegroundColor Yellow
Write-Host "  http://localhost:8888/callback" -ForegroundColor White
Write-Host "`nSteps:" -ForegroundColor Yellow
Write-Host "1. Go to: https://openapi.ctrader.com/" -ForegroundColor White
Write-Host "2. Click 'Edit' on your application" -ForegroundColor White
Write-Host "3. Scroll to 'Redirect URIs' section" -ForegroundColor White
Write-Host "4. Add: http://localhost:8888/callback" -ForegroundColor White
Write-Host "5. Click 'Save'" -ForegroundColor White
Write-Host "6. Wait 30 seconds" -ForegroundColor White

$continue = Read-Host "`nHave you added the redirect URI? (yes/no)"

if ($continue -ne "yes") {
    Write-Host "`nPlease add the redirect URI first, then run this script again." -ForegroundColor Yellow
    exit 0
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "STEP 1: Get Authorization Code" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "`nCopy this URL and paste it in your browser:" -ForegroundColor Yellow
Write-Host ""
Write-Host $authUrl -ForegroundColor White
Write-Host ""

Write-Host "Then:" -ForegroundColor Yellow
Write-Host "1. Login with your Pepperstone credentials" -ForegroundColor White
Write-Host "2. Select your demo account (5287516)" -ForegroundColor White
Write-Host "3. Click 'Allow access'" -ForegroundColor White
Write-Host "4. Browser will show 'ERR_CONNECTION_REFUSED' - that's OK!" -ForegroundColor White
Write-Host "5. Copy the 'code' from the URL bar" -ForegroundColor White

$authCode = Read-Host "`nPaste the authorization code here"

if ([string]::IsNullOrEmpty($authCode)) {
    Write-Host "`n[ERROR] No code provided" -ForegroundColor Red
    exit 1
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "STEP 2: Exchange for Access Token" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$tokenUrl = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$authCode&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"

Write-Host "`nExchanging code for token..." -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $tokenUrl -Method Get -Headers @{
        "Accept" = "application/json"
        "Content-Type" = "application/json"
    }
    
    if ($response.errorCode) {
        Write-Host "`n[ERROR] $($response.errorCode): $($response.description)" -ForegroundColor Red
        
        if ($response.errorCode -eq "ACCESS_DENIED") {
            Write-Host "`nPossible causes:" -ForegroundColor Yellow
            Write-Host "1. Redirect URI not added to application" -ForegroundColor White
            Write-Host "2. Authorization code expired (1 minute limit)" -ForegroundColor White
            Write-Host "3. Code already used" -ForegroundColor White
            Write-Host "`nTry again and paste the code faster!" -ForegroundColor Yellow
        }
        
        exit 1
    }
    
    Write-Host "[OK] Token received!" -ForegroundColor Green
    
    $accessToken = $response.accessToken
    $refreshToken = $response.refreshToken
    $expiresIn = $response.expiresIn
    
    Write-Host "`nAccess Token:  $($accessToken.Substring(0, 30))..." -ForegroundColor White
    Write-Host "Refresh Token: $($refreshToken.Substring(0, 30))..." -ForegroundColor White
    Write-Host "Expires In:    $expiresIn seconds (~$([Math]::Floor($expiresIn / 86400)) days)" -ForegroundColor White
    
    # Save to file
    $response | ConvertTo-Json | Out-File -FilePath "ctrader_tokens.json" -Encoding UTF8
    Write-Host "`n[OK] Saved to: ctrader_tokens.json" -ForegroundColor Green
    
    # Update start_ctrader_bridge.ps1 with new token
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "STEP 3: Update Bridge Script" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    
    $bridgeScript = Get-Content "start_ctrader_bridge.ps1" -Raw
    $bridgeScript = $bridgeScript -replace 'CTRADER_ACCESS_TOKEN = ".*"', "CTRADER_ACCESS_TOKEN = `"$accessToken`""
    $bridgeScript | Set-Content "start_ctrader_bridge.ps1" -Encoding UTF8
    
    Write-Host "[OK] Updated start_ctrader_bridge.ps1 with new token" -ForegroundColor Green
    
    # Set environment variables
    [System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', $accessToken, 'User')
    [System.Environment]::SetEnvironmentVariable('CTRADER_REFRESH_TOKEN', $refreshToken, 'User')
    
    Write-Host "[OK] Environment variables set" -ForegroundColor Green
    
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "SUCCESS!" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan
    
    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "1. Run: .\start_ctrader_bridge.ps1" -ForegroundColor White
    Write-Host "2. Bridge should connect successfully" -ForegroundColor White
    Write-Host "3. Open your Android app and check Pepperstone charts" -ForegroundColor White
    
} catch {
    Write-Host "`n[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`nFull error:" -ForegroundColor Gray
    Write-Host $_.Exception -ForegroundColor Gray
    exit 1
}
