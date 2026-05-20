# List all accounts linked to your access token

$ACCESS_TOKEN = $env:CTRADER_ACCESS_TOKEN

if ([string]::IsNullOrEmpty($ACCESS_TOKEN)) {
    Write-Host "[ERROR] ACCESS_TOKEN not set" -ForegroundColor Red
    exit 1
}

Write-Host "Fetching accounts linked to your token..." -ForegroundColor Yellow

$url = "https://openapi.ctrader.com/connect/userinfo"

try {
    $response = curl.exe -s -X GET "$url" -H "Authorization: Bearer $ACCESS_TOKEN"
    
    $data = $response | ConvertFrom-Json
    
    Write-Host "`n[OK] Token is valid!" -ForegroundColor Green
    Write-Host "`nUser Info:" -ForegroundColor Cyan
    Write-Host "User ID: $($data.sub)" -ForegroundColor White
    
    if ($data.trader_accounts) {
        Write-Host "`nLinked Trading Accounts:" -ForegroundColor Cyan
        $data.trader_accounts | ForEach-Object {
            Write-Host "  - Account ID: $_" -ForegroundColor White
        }
        
        Write-Host "`nUse one of these account IDs in your configuration" -ForegroundColor Yellow
    } else {
        Write-Host "`nNo trading accounts found in token" -ForegroundColor Red
        Write-Host "The token might not have been authorized for any accounts" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
}
