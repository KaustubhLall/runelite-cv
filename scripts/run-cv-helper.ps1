param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $AccountProfile = ""
)

$ErrorActionPreference = "Stop"

Set-Location $ProjectRoot
Write-Host "Launching RuneLite CV Helper from $ProjectRoot"
$launchArgs = @{}
if (-not [string]::IsNullOrWhiteSpace($AccountProfile)) {
    $launchArgs['AccountProfile'] = $AccountProfile
}
& (Join-Path $ProjectRoot "scripts\launch-dev-runelite.ps1") @launchArgs
