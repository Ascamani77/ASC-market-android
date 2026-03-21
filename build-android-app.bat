@echo off
REM Build script for bundling the React chart app into Android assets

echo.
echo ========================================
echo Building React app for Android...
echo ========================================
echo.

cd newtry

echo Installing dependencies...
call npm install

echo.
echo Building React app...
call npm run build:android

echo.
echo ========================================
echo Build complete!
echo ========================================
echo.
echo Next steps:
echo 1. Open Android Studio
echo 2. Rebuild the Android app (Build > Rebuild Project)
echo 3. Deploy to your phone
echo.
echo The chart will now load from bundled assets without needing a PC!
echo.

pause
