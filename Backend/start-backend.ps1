# CLIMS Backend Startup Script (PowerShell)
# This script starts the CLIMS backend server

Write-Host "🚀 Starting CLIMS Backend..." -ForegroundColor Green
Write-Host ""

# Navigate to backend directory
$BackendPath = Join-Path $PSScriptRoot "backend"
Set-Location $BackendPath

Write-Host "📂 Working directory: $BackendPath" -ForegroundColor Cyan
Write-Host ""

# Check if Maven wrapper exists
if (-not (Test-Path ".\mvnw.cmd")) {
    Write-Host "❌ Error: Maven wrapper (mvnw.cmd) not found!" -ForegroundColor Red
    Write-Host "   Please ensure you're running this script from the Backend folder." -ForegroundColor Yellow
    exit 1
}

# Start the application
Write-Host "⏳ Starting Spring Boot application..." -ForegroundColor Yellow
Write-Host "   This may take a minute on first run (downloading dependencies)..." -ForegroundColor Gray
Write-Host ""

.\mvnw.cmd spring-boot:run

Write-Host ""
Write-Host "✅ Backend stopped." -ForegroundColor Green
