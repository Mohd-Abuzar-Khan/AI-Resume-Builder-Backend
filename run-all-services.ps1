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

# 0. Start Docker dependencies (Redis, RabbitMQ)
Write-Host "Checking Docker dependencies..." -ForegroundColor Yellow
if (Get-Command "docker" -ErrorAction SilentlyContinue) {
    Write-Host "Starting MySQL, Redis, RabbitMQ, and SonarQube via Docker Compose..." -ForegroundColor Gray
    docker compose up -d mysql redis rabbitmq sonarqube
} else {
    Write-Warning "Docker not found. Please ensure Redis and RabbitMQ are running manually."
}

# Environment Variables (Load from .env file if it exists)
if (Test-Path ".env") {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Gray
    Get-Content ".env" | Where-Object { $_ -match "=" -and -not $_.StartsWith("#") } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        Set-Item -Path "env:$($name.Trim())" -Value $value.Trim()
    }
} else {
    Write-Warning ".env file not found. Falling back to default values."
}

# Memory limits (using MAVEN_OPTS avoids all quoting issues with Start-Process)
$defaultMem = "-Xmx256m -Xms128m"
$heavyMem   = "-Xmx512m -Xms256m"

# 1. Start Eureka Server first
Write-Host "Starting Eureka Server..." -ForegroundColor Yellow
$eurekaCmd = "`$env:MAVEN_OPTS='$defaultMem'; Write-Host '--- EUREKA SERVER ---' -ForegroundColor Green; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $eurekaCmd -WorkingDirectory "eureka-server" -WindowStyle Normal

# Wait for Eureka to be ready (Port 8761)
Write-Host "Waiting for Eureka Server to start on port 8761..." -ForegroundColor Gray
$eurekaReady = $false
$timeout = 60
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

    $memLimit = $defaultMem
    if ($service -eq "ai-service" -or $service -eq "job-match-service" -or $service -eq "resume-service") {
        $memLimit = $heavyMem
    }

    Write-Host "Starting $service..." -ForegroundColor Yellow
    $serviceCmd = "`$env:MAVEN_OPTS='$memLimit'; Write-Host '--- $service ---' -ForegroundColor Green; mvn spring-boot:run"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $serviceCmd -WorkingDirectory "$service" -WindowStyle Normal
    Start-Sleep -Seconds 4
}

Write-Host "`nAll services have been triggered to start." -ForegroundColor Cyan
Write-Host "Individual terminal windows are open for logs." -ForegroundColor White
Write-Host "==========================================" -ForegroundColor Cyan