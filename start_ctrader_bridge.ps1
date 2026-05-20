# cTrader Bridge Startup Script
# This script starts the Pepperstone cTrader bridge with the correct credentials

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Pepperstone cTrader Bridge Startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Set environment variables from local.properties
$env:CTRADER_CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$env:CTRADER_CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$env:CTRADER_ACCESS_TOKEN = "bz5h7SrzyS_xb8udgcckRcGJU2D9FgIMCAkegWiRFeE"
$env:CTRADER_ACCOUNT_ID = "47341092"
$env:CTRADER_HOST_TYPE = "live"
$env:CTRADER_BRIDGE_PORT = "8082"

Write-Host "OK Environment variables configured" -ForegroundColor Green
Write-Host "  - Host Type: $($env:CTRADER_HOST_TYPE)" -ForegroundColor Gray
Write-Host "  - Account ID: $($env:CTRADER_ACCOUNT_ID)" -ForegroundColor Gray
Write-Host "  - Bridge Port: $($env:CTRADER_BRIDGE_PORT)" -ForegroundColor Gray
Write-Host ""

# Check if Python is installed
$pythonCheck = Get-Command python -ErrorAction SilentlyContinue
if (-not $pythonCheck) {
    Write-Host "ERROR Python not found. Please install Python 3.x" -ForegroundColor Red
    Write-Host "  Download from: https://www.python.org/downloads/" -ForegroundColor Yellow
    pause
    exit 1
}

$pythonVersion = python --version 2>&1
Write-Host "OK Python found: $pythonVersion" -ForegroundColor Green

# Check if port 8082 is already in use
$portInUse = Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "WARNING Port 8082 is already in use" -ForegroundColor Yellow
    Write-Host "  Attempting to stop existing process..." -ForegroundColor Yellow
    $processId = $portInUse.OwningProcess | Select-Object -First 1
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "OK Port cleared" -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting cTrader bridge..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop the bridge" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Start the bridge
python .\ctrader_bridge.py



