# PowerShell script to clean specified ports
$ports = 5173, 8080

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
            Write-Host "Found process on port $port, PID: $pids"
            foreach ($pid in $pids) {
                try {
                    Stop-Process -Id $pid -Force -ErrorAction Stop
                    Write-Host "Stopped process PID: $pid"
                }
                catch {
                    Write-Host "Cannot stop process PID: $pid - $_"
                }
            }
        }
    }
    else {
        Write-Host "Port $port is not in use"
    }
}

Write-Host ""
Write-Host "Port cleanup completed!"
