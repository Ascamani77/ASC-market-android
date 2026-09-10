# Verify Account and Token Relationship
# This script checks if the access token is valid for the specified account

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader Account/Token Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Load credentials
$CLIENT_ID = "$env:CTRADER_CLIENT_ID"
$CLIENT_SECRET = "$env:CTRADER_CLIENT_SECRET"

# Load token from file
$tokenFile = ".\ctrader_tokens.json"
if (-not (Test-Path $tokenFile)) {
    Write-Host "ERROR: Token file not found: $tokenFile" -ForegroundColor Red
    exit 1
}

$tokenData = Get-Content $tokenFile | ConvertFrom-Json
$ACCESS_TOKEN = $tokenData.accessToken

Write-Host "Credentials loaded:" -ForegroundColor Green
Write-Host "  Client ID: $($CLIENT_ID.Substring(0, 20))..." -ForegroundColor Gray
Write-Host "  Access Token: $($ACCESS_TOKEN.Substring(0, 20))..." -ForegroundColor Gray
Write-Host ""

# Test both endpoints
$endpoints = @(
    @{Name="Demo"; Url="https://demo.ctraderapi.com/apps/token/accounts"},
    @{Name="Live"; Url="https://live.ctraderapi.com/apps/token/accounts"}
)

foreach ($endpoint in $endpoints) {
    Write-Host "Testing $($endpoint.Name) endpoint..." -ForegroundColor Cyan
    Write-Host "  URL: $($endpoint.Url)" -ForegroundColor Gray
    
    try {
        $response = curl -s -X GET "$($endpoint.Url)?access_token=$ACCESS_TOKEN" `
            -H "Accept: application/json"
        
        Write-Host "  Response:" -ForegroundColor Yellow
        Write-Host $response -ForegroundColor White
        
        # Try to parse as JSON
        try {
            $json = $response | ConvertFrom-Json
            if ($json.data) {
                Write-Host ""
                Write-Host "  Accounts found:" -ForegroundColor Green
                foreach ($account in $json.data) {
                    $accountId = $account.ctidTraderAccountId
                    $live = $account.live
                    $broker = $account.brokerName
                    Write-Host "    - Account ID: $accountId (Live: $live, Broker: $broker)" -ForegroundColor White
                }
            }
        } catch {
            Write-Host "  Could not parse as JSON" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Check which endpoint returned your account" -ForegroundColor White
Write-Host "2. Verify the account ID matches 47340965" -ForegroundColor White
Write-Host "3. Use the correct endpoint in start_ctrader_bridge.ps1" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
