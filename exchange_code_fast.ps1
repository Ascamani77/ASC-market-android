# Fast Code Exchange - Run this immediately after getting the code

$CLIENT_ID = "$env:CTRADER_CLIENT_ID"
$CLIENT_SECRET = "$env:CTRADER_CLIENT_SECRET"
$REDIRECT_URI = "http://localhost:8888/callback"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Fast Token Exchange" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host "`nPaste the FULL URL from your browser (including the code):" -ForegroundColor Yellow
Write-Host "Example: http://localhost:8888/callback?code=ABC123..." -ForegroundColor Gray

$fullUrl = Read-Host "`nPaste URL here"

# Extract code from URL
if ($fullUrl -match 'code=([^&]+)') {
    $CODE = $matches[1]
    Write-Host "`n[OK] Code extracted: $($CODE.Substring(0,20))..." -ForegroundColor Green
} else {
    Write-Host "`n[ERROR] Could not find code in URL" -ForegroundColor Red
    Write-Host "Make sure you pasted the full URL including '?code=...'" -ForegroundColor Yellow
    exit 1
}

Write-Host "`nExchanging code for token (this should be fast)..." -ForegroundColor Yellow

$url = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$CODE&redirect_uri=$([System.Uri]::EscapeDataString($REDIRECT_URI))&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"

try {
    $response = Invoke-WebRequest -Uri $url -Method Get -UseBasicParsing -Headers @{
        "Accept" = "application/json"
        "Content-Type" = "application/json"
    }
    
    $data = $response.Content | ConvertFrom-Json
    
    if ($data.errorCode) {
        Write-Host "`n[ERROR] $($data.errorCode): $($data.description)" -ForegroundColor Red
        
        if ($data.errorCode -eq "ACCESS_DENIED") {
            Write-Host "`nThe code expired (1 minute limit) or was already used." -ForegroundColor Yellow
            Write-Host "Get a new code and run this script again immediately!" -ForegroundColor Yellow
        }
        
        exit 1
    }
    
    Write-Host "`n[OK] Token received!" -ForegroundColor Green
    
    $accessToken = $data.accessToken
    $refreshToken = $data.refreshToken
    
    Write-Host "Access Token:  $($accessToken.Substring(0,30))..." -ForegroundColor White
    Write-Host "Refresh Token: $($refreshToken.Substring(0,30))..." -ForegroundColor White
    
    # Save to file
    $data | ConvertTo-Json | Out-File "ctrader_tokens.json" -Encoding UTF8
    Write-Host "`n[OK] Saved to ctrader_tokens.json" -ForegroundColor Green
    
    # Update bridge script
    $script = Get-Content "start_ctrader_bridge.ps1" -Raw
    $script = $script -replace 'CTRADER_ACCESS_TOKEN = ".*"', "CTRADER_ACCESS_TOKEN = `"$accessToken`""
    $script | Set-Content "start_ctrader_bridge.ps1" -Encoding UTF8
    Write-Host "[OK] Updated start_ctrader_bridge.ps1" -ForegroundColor Green
    
    # Set environment variable
    [System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', $accessToken, 'User')
    [System.Environment]::SetEnvironmentVariable('CTRADER_REFRESH_TOKEN', $refreshToken, 'User')
    Write-Host "[OK] Environment variables set" -ForegroundColor Green
    
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "SUCCESS! Token is ready" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan
    
    Write-Host "`nNext: Run .\start_ctrader_bridge.ps1" -ForegroundColor Yellow
    
} catch {
    Write-Host "`n[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Message -like "*could not be resolved*") {
        Write-Host "`nDNS/Network issue - cannot reach openapi.ctrader.com" -ForegroundColor Yellow
        Write-Host "Check your internet connection or firewall settings" -ForegroundColor Yellow
    }
}
