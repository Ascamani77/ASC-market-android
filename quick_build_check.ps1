# Quick build check for Pepperstone Demo integration
Write-Host "Starting quick build check..." -ForegroundColor Cyan

# Run Gradle build and capture output
$output = & .\gradlew assembleDebug --no-daemon 2>&1 | Out-String

# Check for specific errors
if ($output -match "BUILD SUCCESSFUL") {
    Write-Host "`n✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "`nAPK Location:" -ForegroundColor Yellow
    Write-Host "app\build\outputs\apk\debug\app-debug.apk"
    exit 0
}

# Extract and display errors
$errors = $output | Select-String -Pattern "error:|FAILED|'when' expression must be exhaustive" -Context 2,2

if ($errors) {
    Write-Host "`n❌ BUILD FAILED - Errors found:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host $_.Line -ForegroundColor Yellow }
} else {
    Write-Host "`n⚠️ Build status unclear" -ForegroundColor Yellow
    Write-Host "Last 20 lines of output:" -ForegroundColor Cyan
    ($output -split "`n") | Select-Object -Last 20 | ForEach-Object { Write-Host $_ }
}

exit 1
