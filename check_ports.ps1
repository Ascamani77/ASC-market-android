# Check Port Availability and Manage Processes

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Port Availability Checker" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$portsToCheck = @(3000, 8080, 8082, 8083)

foreach ($port in $portsToCheck) {
    Write-Host "Checking port $port..." -ForegroundColor Yellow
    
    $connections = netstat -ano | findstr ":$port"
    
    if ($connections) {
        Write-Host "  ❌ Port $port is IN USE" -ForegroundColor Red
        
        # Extract PID
        $lines = $connections -split "`n"
        foreach ($line in $lines) {
            if ($line -match "LISTENING\s+(\d+)") {
                $pid = $matches[1]
                
                try {
                    $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
                    if ($process) {
                        Write-Host "     Process: $($process.ProcessName) (PID: $pid)" -ForegroundColor Gray
                        Write-Host "     Path: $($process.Path)" -ForegroundColor DarkGray
                    } else {
                        Write-Host "     Process: Unknown (PID: $pid)" -ForegroundColor Gray
                    }
                } catch {
                    Write-Host "     Process: Unknown (PID: $pid)" -ForegroundColor Gray
                }
            }
        }
    } else {
        Write-Host "  ✅ Port $port is AVAILABLE" -ForegroundColor Green
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Port Usage Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Port 3000: For OAuth callback (alternative)" -ForegroundColor White
Write-Host "Port 8080: For OAuth callback (default)" -ForegroundColor White
Write-Host "Port 8082: For cTrader LIVE bridge" -ForegroundColor White
Write-Host "Port 8083: For cTrader DEMO bridge" -ForegroundColor White
Write-Host ""

# Check if port 8080 is in use
$port8080 = netstat -ano | findstr ":8080"
if ($port8080 -match "LISTENING\s+(\d+)") {
    $pid = $matches[1]
    
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "  Port 8080 Management" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Port 8080 is currently in use by PID: $pid" -ForegroundColor Yellow
    Write-Host ""
    
    try {
        $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Process Details:" -ForegroundColor Cyan
            Write-Host "  Name: $($process.ProcessName)" -ForegroundColor White
            Write-Host "  PID: $pid" -ForegroundColor White
            Write-Host "  Path: $($process.Path)" -ForegroundColor White
            Write-Host ""
            
            $kill = Read-Host "Do you want to kill this process? (y/n)"
            
            if ($kill -eq "y" -or $kill -eq "Y") {
                try {
                    Stop-Process -Id $pid -Force
                    Write-Host "✅ Process killed successfully" -ForegroundColor Green
                    Write-Host ""
                    Write-Host "Port 8080 should now be available." -ForegroundColor Green
                    Write-Host "You can now run: .\get_demo_token_direct.ps1" -ForegroundColor Gray
                } catch {
                    Write-Host "❌ Failed to kill process: $_" -ForegroundColor Red
                    Write-Host ""
                    Write-Host "You may need to run PowerShell as Administrator." -ForegroundColor Yellow
                    Write-Host "Or use the alternative script: .\get_demo_token_port3000.ps1" -ForegroundColor Yellow
                }
            } else {
                Write-Host ""
                Write-Host "Process not killed." -ForegroundColor Yellow
                Write-Host "Use the alternative script: .\get_demo_token_port3000.ps1" -ForegroundColor Gray
            }
        }
    } catch {
        Write-Host "Could not get process details." -ForegroundColor Yellow
    }
} else {
    Write-Host "✅ Port 8080 is available!" -ForegroundColor Green
    Write-Host "You can run: .\get_demo_token_direct.ps1" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Recommendations" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$port3000Available = -not (netstat -ano | findstr ":3000")
$port8080Available = -not (netstat -ano | findstr ":8080")

if ($port8080Available) {
    Write-Host "✅ Use: .\get_demo_token_direct.ps1 (port 8080)" -ForegroundColor Green
} elseif ($port3000Available) {
    Write-Host "✅ Use: .\get_demo_token_port3000.ps1 (port 3000)" -ForegroundColor Green
} else {
    Write-Host "⚠️  Both ports 3000 and 8080 are in use" -ForegroundColor Yellow
    Write-Host "   Close apps using these ports or use manual method" -ForegroundColor Gray
    Write-Host "   See: ALTERNATIVE_TOKEN_METHODS.md" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
