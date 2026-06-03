# Check if everything is ready for demo bridge setup

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Demo Bridge Readiness Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allGood = $true

# Check 1: Docker
Write-Host "[1/5] Checking Docker..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Docker is installed: $dockerVersion" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Docker is not installed or not running" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "  ❌ Docker is not installed or not running" -ForegroundColor Red
    $allGood = $false
}

# Check 2: Docker image
Write-Host "[2/5] Checking Docker image..." -ForegroundColor Yellow
$imageExists = docker images myrealapp-ctrader-bridge:latest --format "{{.Repository}}" 2>$null
if ($imageExists) {
    Write-Host "  ✅ Docker image exists: myrealapp-ctrader-bridge:latest" -ForegroundColor Green
} else {
    Write-Host "  ❌ Docker image not found: myrealapp-ctrader-bridge:latest" -ForegroundColor Red
    Write-Host "     Run: docker build -f Dockerfile.ctrader -t myrealapp-ctrader-bridge:latest ." -ForegroundColor Yellow
    $allGood = $false
}

# Check 3: Port 8083 availability
Write-Host "[3/5] Checking port 8083..." -ForegroundColor Yellow
$portInUse = netstat -ano | findstr ":8083"
if ($portInUse) {
    Write-Host "  ⚠️  Port 8083 is in use" -ForegroundColor Yellow
    Write-Host "     This might be the demo bridge already running" -ForegroundColor Gray
    
    # Check if it's our container
    $demoContainer = docker ps --filter "name=ctrader-bridge-demo" --format "{{.Names}}" 2>$null
    if ($demoContainer) {
        Write-Host "     ✅ Demo bridge container is running" -ForegroundColor Green
    }
} else {
    Write-Host "  ✅ Port 8083 is available" -ForegroundColor Green
}

# Check 4: Scripts exist
Write-Host "[4/5] Checking scripts..." -ForegroundColor Yellow
$scriptsExist = $true

if (Test-Path ".\get_demo_token_simple.ps1") {
    Write-Host "  ✅ get_demo_token_simple.ps1 exists" -ForegroundColor Green
} else {
    Write-Host "  ❌ get_demo_token_simple.ps1 not found" -ForegroundColor Red
    $scriptsExist = $false
}

if (Test-Path ".\start_demo_bridge.ps1") {
    Write-Host "  ✅ start_demo_bridge.ps1 exists" -ForegroundColor Green
} else {
    Write-Host "  ❌ start_demo_bridge.ps1 not found" -ForegroundColor Red
    $scriptsExist = $false
}

if (-not $scriptsExist) {
    $allGood = $false
}

# Check 5: Token file
Write-Host "[5/5] Checking for existing token..." -ForegroundColor Yellow
if (Test-Path ".\ctrader_demo_token.json") {
    Write-Host "  ✅ Demo token file exists" -ForegroundColor Green
    
    try {
        $tokenData = Get-Content ".\ctrader_demo_token.json" | ConvertFrom-Json
        $generatedAt = [DateTime]::Parse($tokenData.generatedAt)
        $age = (Get-Date) - $generatedAt
        
        Write-Host "     Generated: $($tokenData.generatedAt)" -ForegroundColor Gray
        Write-Host "     Age: $([math]::Round($age.TotalHours, 1)) hours" -ForegroundColor Gray
        
        if ($age.TotalHours -gt 24) {
            Write-Host "     ⚠️  Token may be expired (>24 hours old)" -ForegroundColor Yellow
        } else {
            Write-Host "     ✅ Token should still be valid" -ForegroundColor Green
        }
    } catch {
        Write-Host "     ⚠️  Could not parse token file" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ℹ️  No token file found (you'll need to get one)" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($allGood) {
    Write-Host "✅ Everything looks good!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    
    if (Test-Path ".\ctrader_demo_token.json") {
        Write-Host "  1. You have a token file - try starting the bridge:" -ForegroundColor White
        Write-Host "     .\start_demo_bridge.ps1" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  2. If the token is expired, get a new one:" -ForegroundColor White
        Write-Host "     .\get_demo_token_simple.ps1" -ForegroundColor Gray
    } else {
        Write-Host "  1. Get a demo access token:" -ForegroundColor White
        Write-Host "     .\get_demo_token_simple.ps1" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  2. Or if you have a token, start the bridge:" -ForegroundColor White
        Write-Host "     .\start_demo_bridge.ps1" -ForegroundColor Gray
    }
} else {
    Write-Host "❌ Some issues need to be fixed first" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please address the issues marked with ❌ above" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Demo Account Info:" -ForegroundColor Cyan
Write-Host "  Account ID: 47340965" -ForegroundColor White
Write-Host "  Account Number: 5288664" -ForegroundColor White
Write-Host "  Balance: $50,000 (demo)" -ForegroundColor White
Write-Host "  Bridge Port: 8083" -ForegroundColor White
Write-Host ""

# Check if demo bridge is running
$demoContainer = docker ps --filter "name=ctrader-bridge-demo" --format "{{.Names}}" 2>$null
if ($demoContainer) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  DEMO BRIDGE STATUS" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "✅ Demo bridge is currently running" -ForegroundColor Green
    Write-Host ""
    Write-Host "View logs:" -ForegroundColor Cyan
    Write-Host "  docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Check authentication:" -ForegroundColor Cyan
    Write-Host "  docker logs ctrader-bridge-demo | findstr authenticated" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Restart:" -ForegroundColor Cyan
    Write-Host "  docker restart ctrader-bridge-demo" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
