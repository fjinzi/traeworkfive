@echo off
echo Starting project...
echo This script will:
echo 1. Clean up ports 5173 and 8080 if they are in use
echo 2. Start the Spring Boot backend
echo 3. Start the Vue.js frontend
echo.
powershell -ExecutionPolicy Bypass -File "d:\aigithub\traework\traeworkthd\traeworkthd\start_project.ps1"
echo.
pause
