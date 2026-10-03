@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 ( gradle %* & exit /b %ERRORLEVEL% )
set VER=8.10.2
set CACHE=%USERPROFILE%\.gradle\note-block-songs-bootstrap\%VER%
set DIST=%CACHE%\gradle-%VER%\bin\gradle.bat
if not exist "%DIST%" (
  if not exist "%CACHE%" mkdir "%CACHE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%VER%-bin.zip' -OutFile '%CACHE%\gradle.zip'"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%CACHE%\gradle.zip' '%CACHE%'"
)
call "%DIST%" %*
