# Pepperstone cTrader Credentials Verification Script
# Run this to check if your environment variables are properly set

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Pepperstone cTrader Credentials Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allGood = $true

# Check Client ID
Write-Host "Checking CTRADER_CLIENT_ID..." -NoNewline
$clientId = $env:CTRADER_CLIENT_ID
if ([string]::IsNullOrEmpty($clientId)) {
    Write-Host " [X] NOT SET" -ForegroundColor Red
    $allGood = $false
} elseif ($clientId.Length -ne 56) {
    Write-Host " [!] SET but wrong length ($($clientId.Length) chars, expected 56)" -ForegroundColor Yellow
    Write-Host "   Value: $($clientId.Substring(0, [Math]::Min(20, $clientId.Length)))..." -ForegroundColor Gray
    $allGood = $false
} else {
    Write-Host " [OK] (56 chars)" -ForegroundColor Green
    Write-Host "   Value: $($clientId.Substring(0, 20))..." -ForegroundColor Gray
}

# Check Client Secret
Write-Host "Checking CTRADER_CLIENT_SECRET..." -NoNewline
$clientSecret = $env:CTRADER_CLIENT_SECRET
if ([string]::IsNullOrEmpty($clientSecret)) {
    Write-Host " [X] NOT SET" -ForegroundColor Red
    $allGood = $false
} elseif ($clientSecret.Length -ne 50) {
    Write-Host " [!] SET but wrong length ($($clientSecret.Length) chars, expected 50)" -ForegroundColor Yellow
    Write-Host "   Value: $($clientSecret.Substring(0, [Math]::Min(20, $clientSecret.Length)))..." -ForegroundColor Gray
    $allGood = $false
} else {
    Write-Host " [OK] (50 chars)" -ForegroundColor Green
    Write-Host "   Value: $($clientSecret.Substring(0, 20))..." -ForegroundColor Gray
}

# Check Access Token
Write-Host "Checking CTRADER_ACCESS_TOKEN..." -NoNewline
$accessToken = $env:CTRADER_ACCESS_TOKEN
if ([string]::IsNullOrEmpty($accessToken)) {
    Write-Host " [X] NOT SET" -ForegroundColor Red
    $allGood = $false
} elseif ($accessToken.Length -lt 40) {
    Write-Host " [!] SET but too short ($($accessToken.Length) chars, expected 43+)" -ForegroundColor Yellow
    Write-Host "   Value: $($accessToken.Substring(0, [Math]::Min(20, $accessToken.Length)))..." -ForegroundColor Gray
    $allGood = $false
} else {
    Write-Host " [OK] ($($accessToken.Length) chars)" -ForegroundColor Green
    Write-Host "   Value: $($accessToken.Substring(0, 20))..." -ForegroundColor Gray
}

# Check Account ID
Write-Host "Checking CTRADER_ACCOUNT_ID..." -NoNewline
$accountId = $env:CTRADER_ACCOUNT_ID
if ([string]::IsNullOrEmpty($accountId)) {
    Write-Host " [X] NOT SET" -ForegroundColor Red
    $allGood = $false
} elseif ($accountId -notmatch '^\d+$') {
    Write-Host " [!] SET but not numeric: $accountId" -ForegroundColor Yellow
    $allGood = $false
} else {
    Write-Host " [OK]" -ForegroundColor Green
    Write-Host "   Value: $accountId" -ForegroundColor Gray
}

# Check Host Type
Write-Host "Checking CTRADER_HOST_TYPE..." -NoNewline
$hostType = $env:CTRADER_HOST_TYPE
if ([string]::IsNullOrEmpty($hostType)) {
    Write-Host " [X] NOT SET" -ForegroundColor Red
    $allGood = $false
} elseif ($hostType -notin @('demo', 'live')) {
    Write-Host " [!] SET but invalid value: $hostType (expected 'demo' or 'live')" -ForegroundColor Yellow
    $allGood = $false
} else {
    Write-Host " [OK]" -ForegroundColor Green
    Write-Host "   Value: $hostType" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

if ($allGood) {
    Write-Host "[OK] All credentials are properly configured!" -ForegroundColor Green
    Write-Host ""
    Write-Host "You can now run: .\start_ctrader_bridge.ps1" -ForegroundColor Cyan
} else {
    Write-Host "[X] Some credentials are missing or incorrect" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please follow these steps:" -ForegroundColor Yellow
    Write-Host "1. Go to https://openapi.ctrader.com/" -ForegroundColor White
    Write-Host "2. Login with your Pepperstone credentials" -ForegroundColor White
    Write-Host "3. Get your Client ID, Client Secret, and Access Token" -ForegroundColor White
    Write-Host "4. Set them using the commands in PEPPERSTONE_TOKEN_SETUP.md" -ForegroundColor White
    Write-Host ""
    Write-Host "See PEPPERSTONE_TOKEN_SETUP.md for detailed instructions" -ForegroundColor Cyan
}

Write-Host "========================================" -ForegroundColor Cyan
