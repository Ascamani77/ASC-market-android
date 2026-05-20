# cTrader Token Generator (Using Playground URI)
# This uses the default playground redirect URI that's already registered

$CLIENT_ID = $env:CTRADER_CLIENT_ID
$CLIENT_SECRET = $env:CTRADER_CLIENT_SECRET
$REDIRECT_URI = "https://openapi.ctrader.com/apps/playground"  # Default URI - already registered
$SCOPE = "trading"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Pepperstone cTrader Token Generator (Playground URI)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if ([string]::IsNullOrEmpty($CLIENT_ID) -or [string]::IsNullOrEmpty($CLIENT_SECRET)) {
    Write-Host "`n[ERROR] Missing credentials!" -ForegroundColor Red
    Write-Host "Please set CTRADER_CLIENT_ID and CTRADER_CLIENT_SECRET first" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n[OK] Client ID: $($CLIENT_ID.Substring(0, 20))..." -ForegroundColor Green
Write-Host "[OK] Client Secret: $($CLIENT_SECRET.Substring(0, 20))..." -ForegroundColor Green
Write-Host "[OK] Using Playground redirect URI (already registered)" -ForegroundColor Green

# Build authorization URL
$authUrl = "https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=$CLIENT_ID&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&scope=$SCOPE&product=web"

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
Write-Host "4. You'll be redirected to the Playground page" -ForegroundColor White
Write-Host "5. Look for 'Authorization Code' on the page" -ForegroundColor White
Write-Host "6. Copy the code" -ForegroundColor White

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
    
    # Set environment variables
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "STEP 3: Set Environment Variables" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    
    [System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', $accessToken, 'User')
    [System.Environment]::SetEnvironmentVariable('CTRADER_REFRESH_TOKEN', $refreshToken, 'User')
    
    Write-Host "`n[OK] Environment variables set!" -ForegroundColor Green
    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "1. Close and reopen PowerShell" -ForegroundColor White
    Write-Host "2. Run: .\start_ctrader_bridge.ps1" -ForegroundColor White
    
} catch {
    Write-Host "`n[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`nFull error:" -ForegroundColor Gray
    Write-Host $_.Exception -ForegroundColor Gray
    exit 1
}
