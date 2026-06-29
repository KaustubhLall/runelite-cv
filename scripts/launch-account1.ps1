param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
Set-Location $ProjectRoot

$profileName = "account1"
$runeliteDir = Join-Path $env:USERPROFILE ".runelite-$profileName"

# Check if profile directory exists, if not bootstrap full directory
if (!(Test-Path $runeliteDir)) {
    Write-Host "Profile directory not found for $profileName. Bootstrapping full directory..." -ForegroundColor Yellow
    
    $defaultDir = Join-Path $env:USERPROFILE ".runelite"
    if (!(Test-Path $defaultDir)) {
        Write-Host "Default RuneLite directory not found. Please log in to RuneLite first." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    # Copy entire default directory to profile
    Write-Host "Copying full RuneLite directory from default to $profileName..." -ForegroundColor Cyan
    Copy-Item -LiteralPath $defaultDir -Destination $runeliteDir -Recurse -Force
    Write-Host "Full directory copied to: $runeliteDir" -ForegroundColor Green
    Write-Host "Launch this profile and log in with the correct account credentials to update them." -ForegroundColor Cyan
}

Write-Host "Launching $profileName..." -ForegroundColor Cyan

try {
    & (Join-Path $ProjectRoot "scripts\launch-dev-runelite.ps1") -AccountProfile $profileName
    Write-Host "Launch command completed." -ForegroundColor Green
} catch {
    Write-Host "Error launching $profileName" -ForegroundColor Red
    Write-Host $_ -ForegroundColor Red
}

Write-Host "Press Enter to close this window..."
Read-Host
