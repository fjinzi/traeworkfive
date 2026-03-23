# Complete project startup script
# Clean ports first
$ports = 5173, 8080

Write-Host "=== Cleaning up ports ===" -ForegroundColor Cyan
foreach ($port in $ports) {
    $connections = netstat -ano | Select-String ":$port"
    
    if ($connections) {
        $processIds = $connections | ForEach-Object {
            $parts = $_ -split '\s+'
            $procId = $parts[-1]
            if ($procId -ne '0' -and $procId -ne '') {
                $procId
            }
        } | Select-Object -Unique

        if ($processIds) {
            Write-Host "Found process on port $port, PID: $processIds" -ForegroundColor Yellow
            foreach ($procId in $processIds) {
                try {
                    Stop-Process -Id $procId -Force -ErrorAction Stop
                    Write-Host "Stopped process PID: $procId" -ForegroundColor Green
                }
                catch {
                    Write-Host "Cannot stop process PID: $procId - $_" -ForegroundColor Red
                }
            }
        }
    }
    else {
        Write-Host "Port $port is not in use" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "=== Port cleanup completed! ===" -ForegroundColor Green
Write-Host ""

# Start backend (Spring Boot)
Write-Host "=== Starting Backend (Spring Boot) ===" -ForegroundColor Cyan
Write-Host "This will run in the background..." -ForegroundColor Gray
Write-Host ""

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'd:\aigithub\traework\traeworkfour'; mvn spring-boot:run" -WindowStyle Normal

Write-Host ""
Write-Host "=== Starting Frontend (Vue.js) ===" -ForegroundColor Cyan
Write-Host "This will run in the background..." -ForegroundColor Gray
Write-Host ""

# Start frontend (Vue)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'd:\aigithub\traework\traeworkfour\frontend'; npm run dev" -WindowStyle Normal

Write-Host ""
Write-Host "=== Project startup initiated! ===" -ForegroundColor Green
Write-Host "Backend will be available at: http://localhost:8080" -ForegroundColor Yellow
Write-Host "Frontend will be available at: http://localhost:5173" -ForegroundColor Yellow
Write-Host ""
Write-Host "Two separate PowerShell windows have been opened for backend and frontend." -ForegroundColor Gray
