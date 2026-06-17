param(
    [string] $RuneLiteInstallDir = "$env:LOCALAPPDATA\RuneLite"
)

$ErrorActionPreference = "Stop"

$settingsPath = Join-Path $RuneLiteInstallDir "settings.json"
$credentialsPath = Join-Path $env:USERPROFILE ".runelite\credentials.properties"
$runeliteExe = Join-Path $RuneLiteInstallDir "RuneLite.exe"

if (!(Test-Path $RuneLiteInstallDir)) {
    throw "RuneLite install directory not found: $RuneLiteInstallDir"
}

if (Test-Path $settingsPath) {
    Copy-Item -LiteralPath $settingsPath -Destination "$settingsPath.bak" -Force
    $settings = Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
} else {
    $settings = [pscustomobject]@{
        lastUpdateAttemptTime = 0
        lastUpdateAttemptNum = 0
        debug = $false
        nodiffs = $false
        skipTlsVerification = $false
        noupdates = $false
        safemode = $false
        ipv4 = $false
        clientArguments = @()
        jvmArguments = @()
        hardwareAccelerationMode = "AUTO"
        launchMode = "AUTO"
    }
}

$args = @($settings.clientArguments)
if ($args -notcontains "--insecure-write-credentials") {
    $settings.clientArguments = @($args + "--insecure-write-credentials")
}

$settings | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $settingsPath -Encoding UTF8

Write-Host "RuneLite launcher settings updated: $settingsPath"
Write-Host "Required client argument: --insecure-write-credentials"

if (Test-Path $credentialsPath) {
    Write-Host "Credentials already present: $credentialsPath"
    exit 0
}

Write-Host "Credentials are not present yet: $credentialsPath"
Write-Host "Launch RuneLite from Jagex Launcher for CoreDump/C0REDUMPED. When it opens, RuneLite should write credentials.properties."

$jagexLauncher = Join-Path ${env:ProgramFiles(x86)} "Jagex Launcher\JagexLauncher.exe"
if (Test-Path $jagexLauncher) {
    Start-Process -FilePath $jagexLauncher
} elseif (Test-Path $runeliteExe) {
    Start-Process -FilePath $runeliteExe -WorkingDirectory $RuneLiteInstallDir
}
