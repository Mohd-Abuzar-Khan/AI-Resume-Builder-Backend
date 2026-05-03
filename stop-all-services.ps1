# stop-all-services.ps1
# Script to stop all backend microservices for Resumade

$ports = @(8761, 9090, 9091, 9092, 9093, 9094, 9095, 9096, 9097)

Write-Host "Stopping Resumade Backend Services..." -ForegroundColor Cyan

foreach ($port in $ports) {
    Write-Host "Checking port $port..." -ForegroundColor Gray
    $process = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    
    if ($process) {
        $procId = $process.OwningProcess
        Write-Host "Found process $procId on port $port. Killing it..." -ForegroundColor Yellow
        Stop-Process -Id $procId -Force
        Write-Host "Port $port is now free." -ForegroundColor Green
    }
}

# Also kill any remaining Maven/Java processes that might be related if ports didn't catch them
# This is more aggressive and might kill other java apps, so we use it with caution or skip it.
# Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host "All services stopped." -ForegroundColor Cyan
