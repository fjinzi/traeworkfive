@echo off
powershell -Command "$ports = 5173, 8080; foreach($port in $ports) { $pids = netstat -ano | Select-String ":$port" | ForEach-Object { ($_ -split '\s+')[-1] } | Where-Object { $_ -ne '0' } | Select-Object -Unique; if($pids) { Write-Host \"Killing processes on port $port : $pids\"; Stop-Process -Id $pids -Force -ErrorAction SilentlyContinue } else { Write-Host \"Port $port is free\" } }"
echo Port cleanup completed!
