# Quick build script for Pepperstone Demo integration
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Building App with Pepperstone Demo" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Clean and build
Write-Host "Starting build..." -ForegroundColor Yellow
$buildOutput = & .\gradlew assembleDebug 2>&1

# Check for success
if ($LASTEXITCODE -eq 0 -and $buildOutput -match "BUILD SUCCESSFUL") {
    Write-Host "`n✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    
    # Check APK
    if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") {
        $apk = Get-Item "app\build\outputs\apk\debug\app-debug.apk"
        Write-Host "`nAPK Details:" -ForegroundColor Cyan
        Write-Host "  Location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
        Write-Host "  Size: $([math]::Round($apk.Length/1MB,2)) MB" -ForegroundColor White
        Write-Host "  Modified: $($apk.LastWriteTime)" -ForegroundColor White
        
        Write-Host "`nNext Steps:" -ForegroundColor Yellow
        Write-Host "  1. Install: adb install -r app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
        Write-Host "  2. Open app and go to Settings → Chart Feed" -ForegroundColor White
        Write-Host "  3. Select 'Pepperstone Demo'" -ForegroundColor White
        Write-Host "  4. Verify connection to port 8083" -ForegroundColor White
    }
} else {
    Write-Host "`n❌ BUILD FAILED" -ForegroundColor Red
    
    # Extract errors
    $errors = $buildOutput | Select-String -Pattern "error:|FAILED|exhaustive" -Context 1,1
    
    if ($errors) {
        Write-Host "`nErrors found:" -ForegroundColor Yellow
        $errors | ForEach-Object { 
            Write-Host $_.Line -ForegroundColor Red
        }
    }
    
    Write-Host "`nLast 30 lines of output:" -ForegroundColor Yellow
    ($buildOutput -split "`n") | Select-Object -Last 30 | ForEach-Object { 
        Write-Host $_ -ForegroundColor Gray
    }
}

Write-Host "`n========================================`n" -ForegroundColor Cyan
