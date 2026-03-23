# 停止占用指定端口的进程
function Stop-ProcessOnPort {
    param([int]$port)
    
    $process = netstat -ano | Where-Object { $_ -match ":$port\s" } | ForEach-Object {
        $parts = $_ -split '\s+'
        $pid = $parts[-1]
        if ($pid -ne '0' -and $pid -ne '') {
            Get-Process -Id $pid -ErrorAction SilentlyContinue
        }
    } | Select-Object -First 1

    if ($process) {
        Write-Host "正在停止端口 $port 上的进程: $($process.Name) (PID: $($process.Id))"
        Stop-Process -Id $process.Id -Force
        Write-Host "进程已停止"
    }
    else {
        Write-Host "端口 $port 没有被占用"
    }
}

# 停止5173（前端）和8080（后端）端口的进程
Write-Host "=== 检查并停止端口占用 ==="
Stop-ProcessOnPort 5173
Stop-ProcessOnPort 8080

Write-Host ""
Write-Host "=== 端口清理完成，开始启动项目 ==="
Write-Host ""

# 启动后端（Spring Boot）
Write-Host "=== 启动后端服务 ==="
$backendJob = Start-Job -ScriptBlock {
    Set-Location "d:\aigithub\traework\traeworkfive"
    .\mvnw.cmd spring-boot:run
}
Write-Host "后端服务正在启动中..."

Write-Host ""
Write-Host "=== 等待后端服务启动 ==="
Start-Sleep -Seconds 30

# 启动前端（Vue）
Write-Host ""
Write-Host "=== 启动前端服务 ==="
$frontendJob = Start-Job -ScriptBlock {
    Set-Location "d:\aigithub\traework\traeworkfive\frontend"
    npm run dev
}
Write-Host "前端服务正在启动中..."

Write-Host ""
Write-Host "=== 项目启动完成 ==="
Write-Host "后端服务：http://localhost:8080"
Write-Host "前端服务：http://localhost:5173"
Write-Host ""
Write-Host "按 Ctrl+C 停止所有服务"
Write-Host ""

# 等待作业
Wait-Job -Job $backendJob, $frontendJob
