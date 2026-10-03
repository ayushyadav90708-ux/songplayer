@echo off
setlocal
where gradle >nul 2>nul
if %errorlevel%==0 (
  gradle build
  exit /b %errorlevel%
)
echo Gradle was not found on PATH.
echo Install Gradle 8.14.3 or open this folder in IntelliJ IDEA and run the Gradle 'build' task.
exit /b 1
