# Fast Token Exchange using curl
# This should be faster than Invoke-WebRequest

$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$REDIRECT_URI = "http://localhost:8888/callback"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Fast Production Token Generator (using curl)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "`nAuthorization URL (copy and open in browser):" -ForegroundColor Yellow
$authUrl = "https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=$CLIENT_ID&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&scope=trading&product=web"
Write-Host $authUrl -ForegroundColor White

Write-Host "`nSteps:" -ForegroundColor Yellow
Write-Host "1. Open the URL above in your browser" -ForegroundColor White
Write-Host "2. Login and select account 5287516" -ForegroundColor White
Write-Host "3. Click 'Allow access'" -ForegroundColor White
Write-Host "4. Copy ONLY the code from the URL (after 'code=')" -ForegroundColor White

Write-Host "`nPaste the code (or full URL) here and press Enter IMMEDIATELY:" -ForegroundColor Yellow
$input = Read-Host "Code or URL"

if ([string]::IsNullOrEmpty($input)) {
    Write-Host "[ERROR] No code provided" -ForegroundColor Red
    exit 1
}

# Extract code from URL if full URL was pasted
if ($input -match 'code=([^&]+)') {
    $CODE = $matches[1]
    Write-Host "[OK] Extracted code from URL" -ForegroundColor Green
} else {
    $CODE = $input
}

Write-Host "`nExchanging code (using curl for speed)..." -ForegroundColor Yellow

$tokenUrl = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$CODE&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"

# Use curl for faster request
$response = curl.exe -s -X GET "$tokenUrl" -H "Accept: application/json" -H "Content-Type: application/json"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] curl failed" -ForegroundColor Red
    exit 1
}

try {
    $data = $response | ConvertFrom-Json
    
    if ($data.errorCode) {
        Write-Host "`n[ERROR] $($data.errorCode): $($data.description)" -ForegroundColor Red
        
        if ($data.errorCode -eq "ACCESS_DENIED") {
            Write-Host "`nThe code expired or was already used." -ForegroundColor Yellow
            Write-Host "Try again and paste the code FASTER (within 60 seconds)!" -ForegroundColor Yellow
        }
        
        exit 1
    }
    
    Write-Host "`n[SUCCESS] Production token received!" -ForegroundColor Green
    
    $accessToken = $data.accessToken
    $refreshToken = $data.refreshToken
    $expiresIn = $data.expiresIn
    
    Write-Host "`nAccess Token:  $($accessToken.Substring(0,30))..." -ForegroundColor White
    Write-Host "Refresh Token: $($refreshToken.Substring(0,30))..." -ForegroundColor White
    Write-Host "Expires In:    $expiresIn seconds (~$([Math]::Floor($expiresIn / 86400)) days)" -ForegroundColor White
    
    # Save to file
    $data | ConvertTo-Json | Out-File "ctrader_tokens.json" -Encoding UTF8
    Write-Host "`n[OK] Saved to ctrader_tokens.json" -ForegroundColor Green
    
    # Update bridge script
    $script = Get-Content "start_ctrader_bridge.ps1" -Raw
    $script = $script -replace 'CTRADER_ACCESS_TOKEN = ".*"', "CTRADER_ACCESS_TOKEN = `"$accessToken`""
    $script | Set-Content "start_ctrader_bridge.ps1" -Encoding UTF8
    Write-Host "[OK] Updated start_ctrader_bridge.ps1" -ForegroundColor Green
    
    # Set environment variables
    [System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', $accessToken, 'User')
    [System.Environment]::SetEnvironmentVariable('CTRADER_REFRESH_TOKEN', $refreshToken, 'User')
    Write-Host "[OK] Environment variables set" -ForegroundColor Green
    
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "SUCCESS! Production token configured" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan
    
    Write-Host "`nThis is a PRODUCTION token (not sandbox)" -ForegroundColor Green
    Write-Host "Valid for 30 days, can be refreshed automatically" -ForegroundColor Gray
    
    Write-Host "`nNext: Run .\start_ctrader_bridge.ps1" -ForegroundColor Yellow
    
} catch {
    Write-Host "`n[ERROR] Failed to parse response: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response: $response" -ForegroundColor Gray
}
