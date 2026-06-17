param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

Set-Location $ProjectRoot
Write-Host "Launching RuneLite CV Helper from $ProjectRoot"
powershell -ExecutionPolicy Bypass -File (Join-Path $ProjectRoot "scripts\launch-dev-runelite.ps1")
