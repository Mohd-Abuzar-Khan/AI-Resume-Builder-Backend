# run-all-services.ps1
# Script to run all backend microservices for Resumade

if (-not (Test-Path "eureka-server")) {
    Write-Error "Please run this script from the 'Backend' directory."
    exit
}

$services = @(
    "eureka-server",
    "api-gateway",
    "auth-service",
    "resume-service",
    "template-service",
    "ai-service",
    "export-service",
    "notification-service",
    "job-match-service"
)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   Resumade Backend Startup Script        " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Start Eureka Server first
Write-Host "Starting Eureka Server..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Write-Host '--- EUREKA SERVER ---' -ForegroundColor Green; mvn spring-boot:run -Dspring-boot.run.jvmArguments='-Xmx256m -Xms128m'" -WorkingDirectory "eureka-server" -WindowStyle Normal

# Wait for Eureka to be ready (Port 8761)
Write-Host "Waiting for Eureka Server to start on port 8761..." -ForegroundColor Gray
$eurekaReady = $false
$timeout = 60 # seconds
$elapsed = 0
while (-not $eurekaReady -and $elapsed -lt $timeout) {
    try {
        $connection = New-Object System.Net.Sockets.TcpClient("localhost", 8761)
        if ($connection.Connected) {
            $eurekaReady = $true
            $connection.Close()
        }
    } catch {
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
}

if (-not $eurekaReady) {
    Write-Warning "Eureka Server didn't start in time. Proceeding anyway..."
} else {
    Write-Host "Eureka Server is UP!" -ForegroundColor Green
}

# 2. Start the rest of the services
foreach ($service in $services) {
    if ($service -eq "eureka-server") { continue }
    
    $memLimit = "-Xmx256m -Xms128m"
    if ($service -eq "ai-service" -or $service -eq "job-match-service" -or $service -eq "resume-service") {
        $memLimit = "-Xmx512m -Xms256m"
    }

    Write-Host "Starting $service..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Write-Host '--- $service ---' -ForegroundColor Green; mvn spring-boot:run -Dspring-boot.run.jvmArguments='$memLimit'" -WorkingDirectory "$service" -WindowStyle Normal
    Start-Sleep -Seconds 4 # Give a bit more delay to avoid CPU/IO spikes
}

Write-Host "`nAll services have been triggered to start." -ForegroundColor Cyan
Write-Host "Individual terminal windows are open for logs." -ForegroundColor White
Write-Host "==========================================" -ForegroundColor Cyan
