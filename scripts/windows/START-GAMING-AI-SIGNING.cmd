@echo off
setlocal
cd /d "%~dp0"
where powershell.exe >nul 2>&1
if errorlevel 1 (
  echo Windows PowerShell not found.
  pause
  exit /b 1
)
echo Gaming AI stable signing setup.
echo IMPORTANT: review Setup-GamingAISigning.ps1 before executing.
echo The program keeps your private key on this PC and uses GitHub encrypted secrets.
echo.
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0Setup-GamingAISigning.ps1"
set CODE=%ERRORLEVEL%
echo.
if not "%CODE%"=="0" echo Setup needs attention. Read the error above.
pause
exit /b %CODE%
