# Complete project startup script
# Clean ports first
$ports = 5173, 8080

Write-Host "=== Cleaning up ports ===" -ForegroundColor Cyan
foreach ($port in $ports) {
    $connections = netstat -ano | Select-String ":$port"
    
    if ($connections) {
        $pids = $connections | ForEach-Object {
            $parts = $_ -split '\s+'
            $pid = $parts[-1]
            if ($pid -ne '0' -and $pid -ne '') {
                $pid
            }
        } | Select-Object -Unique

        if ($pids) {
            Write-Host "Found process on port $port, PID: $pids" -ForegroundColor Yellow
            foreach ($pid in $pids) {
                try {
                    Stop-Process -Id $pid -Force -ErrorAction Stop
                    Write-Host "Stopped process PID: $pid" -ForegroundColor Green
                }
                catch {
                    Write-Host "Cannot stop process PID: $pid - $_" -ForegroundColor Red
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

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'd:\aigithub\traework\traeworkthd\traeworkthd'; mvn spring-boot:run" -WindowStyle Normal

Write-Host ""
Write-Host "=== Starting Frontend (Vue.js) ===" -ForegroundColor Cyan
Write-Host "This will run in the background..." -ForegroundColor Gray
Write-Host ""

# Start frontend (Vue)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location 'd:\aigithub\traework\traeworkthd\traeworkthd\frontend'; npm run dev" -WindowStyle Normal

Write-Host ""
Write-Host "=== Project startup initiated! ===" -ForegroundColor Green
Write-Host "Backend will be available at: http://localhost:8080" -ForegroundColor Yellow
Write-Host "Frontend will be available at: http://localhost:5173" -ForegroundColor Yellow
Write-Host ""
Write-Host "Two separate PowerShell windows have been opened for backend and frontend." -ForegroundColor Gray
Write-Host "Press Enter to exit this script..." -ForegroundColor Gray
Read-Host
