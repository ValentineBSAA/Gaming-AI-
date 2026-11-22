@echo off
setlocal
cd /d "%~dp0"
where powershell.exe >nul 2>&1
if errorlevel 1 (
  echo Windows PowerShell not found. Use Windows 11 or PowerShell 5.1+.
  pause
  exit /b 1
)
if not exist "%~dp0setup-signing.ps1" (
  echo Cannot find setup-signing.ps1 beside this launcher.
  echo Extract BOTH files into the same Windows folder.
  pause
  exit /b 1
)
echo GAMING AI ^| Secure Windows APK Signing
echo.
echo IMPORTANT: Review setup-signing.ps1 before continuing.
echo The private key remains on your PC and is saved to encrypted GitHub Actions secrets.
echo You must back it up and keep the password safe.
echo.
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-signing.ps1"
set STATUS=%ERRORLEVEL%
echo.
if not "%STATUS%"=="0" echo Setup stopped. Review the message above before retrying.
pause
exit /b %STATUS%
