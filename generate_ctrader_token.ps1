# Pepperstone cTrader Access Token Generator (PowerShell)
# Automates the OAuth 2.0 flow to generate production access tokens

param(
    [string]$Scope = "trading"  # 'accounts' for read-only, 'trading' for full access
)

$ErrorActionPreference = "Stop"

# Configuration
$CLIENT_ID = $env:CTRADER_CLIENT_ID
$CLIENT_SECRET = $env:CTRADER_CLIENT_SECRET
$REDIRECT_URI = "http://localhost:8888/callback"
$AUTH_URL = "https://id.ctrader.com/my/settings/openapi/grantingaccess/"
$TOKEN_URL = "https://openapi.ctrader.com/apps/token"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Pepperstone cTrader Access Token Generator" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# Check credentials
if ([string]::IsNullOrEmpty($CLIENT_ID) -or [string]::IsNullOrEmpty($CLIENT_SECRET)) {
    Write-Host "`n[ERROR] Missing credentials!" -ForegroundColor Red
    Write-Host "`nPlease set these environment variables first:" -ForegroundColor Yellow
    Write-Host "  CTRADER_CLIENT_ID" -ForegroundColor White
    Write-Host "  CTRADER_CLIENT_SECRET" -ForegroundColor White
    Write-Host "`nRun:" -ForegroundColor Yellow
    Write-Host "  [System.Environment]::SetEnvironmentVariable('CTRADER_CLIENT_ID', 'your_id', 'User')" -ForegroundColor White
    Write-Host "  [System.Environment]::SetEnvironmentVariable('CTRADER_CLIENT_SECRET', 'your_secret', 'User')" -ForegroundColor White
    exit 1
}

Write-Host "`n[OK] Client ID: $($CLIENT_ID.Substring(0, 20))..." -ForegroundColor Green
Write-Host "[OK] Client Secret: $($CLIENT_SECRET.Substring(0, 20))..." -ForegroundColor Green
Write-Host "[OK] Redirect URI: $REDIRECT_URI" -ForegroundColor Green
Write-Host "[OK] Scope: $Scope" -ForegroundColor Green

# Build authorization URL
$authParams = @{
    client_id = $CLIENT_ID
    redirect_uri = $REDIRECT_URI
    scope = $Scope
    product = "web"
}
$queryString = ($authParams.GetEnumerator() | ForEach-Object { "$($_.Key)=$([System.Uri]::EscapeDataString($_.Value))" }) -join "&"
$authUrl = "$AUTH_URL?$queryString"

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "STEP 1: User Authorization" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "`nOpening browser for authorization..." -ForegroundColor Yellow
Write-Host "`nPlease:" -ForegroundColor White
Write-Host "1. Login with your Pepperstone credentials" -ForegroundColor White
Write-Host "2. Select your demo account (5287516)" -ForegroundColor White
Write-Host "3. Click 'Allow access'" -ForegroundColor White
Write-Host "`nAfter authorization, you'll be redirected to a page showing the code." -ForegroundColor Yellow
Write-Host "Copy the 'code' parameter from the URL." -ForegroundColor Yellow

# Open browser
try {
    Start-Process $authUrl
} catch {
    # Fallback: use cmd to open browser
    cmd /c start $authUrl
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "Waiting for Authorization Code" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "`nThe browser will redirect to: $REDIRECT_URI" -ForegroundColor Gray
Write-Host "The page won't load (that's normal), but the URL will contain the code." -ForegroundColor Gray
Write-Host "`nExample URL:" -ForegroundColor Gray
Write-Host "  http://localhost:8888/callback?code=ABC123XYZ..." -ForegroundColor Gray
Write-Host "`nCopy everything after 'code=' from the URL" -ForegroundColor Yellow

$authCode = Read-Host "`nPaste the authorization code here"

if ([string]::IsNullOrEmpty($authCode)) {
    Write-Host "`n[ERROR] No authorization code provided" -ForegroundColor Red
    exit 1
}

Write-Host "`n[OK] Authorization code received: $($authCode.Substring(0, [Math]::Min(20, $authCode.Length)))..." -ForegroundColor Green

# Exchange code for token
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "STEP 2: Exchange Code for Access Token" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$tokenParams = @{
    grant_type = "authorization_code"
    code = $authCode
    redirect_uri = $REDIRECT_URI
    client_id = $CLIENT_ID
    client_secret = $CLIENT_SECRET
}

$tokenQueryString = ($tokenParams.GetEnumerator() | ForEach-Object { "$($_.Key)=$([System.Uri]::EscapeDataString($_.Value))" }) -join "&"
$tokenRequestUrl = "$TOKEN_URL?$tokenQueryString"

Write-Host "`nSending request to: $TOKEN_URL" -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $tokenRequestUrl -Method Get -Headers @{
        "Accept" = "application/json"
        "Content-Type" = "application/json"
    }
    
    if ($response.errorCode) {
        Write-Host "`n[ERROR] $($response.errorCode): $($response.description)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Access token received successfully!" -ForegroundColor Green
    
    # Save tokens
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "STEP 3: Save Tokens" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    
    $accessToken = $response.accessToken
    $refreshToken = $response.refreshToken
    $expiresIn = $response.expiresIn
    $expiresInDays = [Math]::Floor($expiresIn / 86400)
    
    Write-Host "`nAccess Token:  $($accessToken.Substring(0, 30))..." -ForegroundColor White
    Write-Host "Refresh Token: $($refreshToken.Substring(0, 30))..." -ForegroundColor White
    Write-Host "Expires In:    $expiresIn seconds (~$expiresInDays days)" -ForegroundColor White
    
    # Save to file
    $tokenFile = "ctrader_tokens.json"
    $response | ConvertTo-Json | Out-File -FilePath $tokenFile -Encoding UTF8
    Write-Host "`n[OK] Tokens saved to: $tokenFile" -ForegroundColor Green
    
    # Set environment variables
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "STEP 4: Set Environment Variables" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    
    Write-Host "`nSetting environment variables..." -ForegroundColor Yellow
    
    [System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', $accessToken, 'User')
    [System.Environment]::SetEnvironmentVariable('CTRADER_REFRESH_TOKEN', $refreshToken, 'User')
    
    Write-Host "[OK] Environment variables set!" -ForegroundColor Green
    
    # Update current session
    $env:CTRADER_ACCESS_TOKEN = $accessToken
    $env:CTRADER_REFRESH_TOKEN = $refreshToken
    
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "SUCCESS! Token generation complete" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan
    
    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "1. Close and reopen PowerShell (to load new env vars)" -ForegroundColor White
    Write-Host "2. Run: .\verify_ctrader_credentials.ps1" -ForegroundColor White
    Write-Host "3. Run: .\start_ctrader_bridge.ps1" -ForegroundColor White
    Write-Host "============================================================" -ForegroundColor Cyan
    
} catch {
    Write-Host "`n[ERROR] Request failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
