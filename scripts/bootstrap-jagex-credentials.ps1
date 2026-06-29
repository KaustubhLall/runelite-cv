param(
    [string] $RuneLiteInstallDir = "$env:LOCALAPPDATA\RuneLite",
    [string] $AccountProfile = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AccountProfile)) {
    $runeliteDir = Join-Path $env:USERPROFILE ".runelite"
} else {
    $runeliteDir = Join-Path $env:USERPROFILE ".runelite-$AccountProfile"
}

# Create profile directory if it doesn't exist
if (!(Test-Path $runeliteDir)) {
    New-Item -ItemType Directory -Path $runeliteDir -Force | Out-Null
    Write-Host "Created profile directory: $runeliteDir" -ForegroundColor Green
}

$settingsPath = Join-Path $RuneLiteInstallDir "settings.json"
$credentialsPath = Join-Path $runeliteDir "credentials.properties"
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

$clientArgs = @($settings.clientArguments)
if ($clientArgs -notcontains "--insecure-write-credentials") {
    $settings.clientArguments = @($clientArgs + "--insecure-write-credentials")
}

$settings | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $settingsPath -Encoding UTF8

Write-Host "RuneLite launcher settings updated: $settingsPath"
Write-Host "Required client argument: --insecure-write-credentials"

if (Test-Path $credentialsPath) {
    if ([string]::IsNullOrWhiteSpace($AccountProfile)) {
        Write-Host "Credentials already present: $credentialsPath"
    } else {
        Write-Host "Credentials already present for profile '$AccountProfile': $credentialsPath"
    }
    exit 0
}

if ([string]::IsNullOrWhiteSpace($AccountProfile)) {
    Write-Host "Credentials are not present yet: $credentialsPath"
    Write-Host "Launching RuneLite from Jagex Launcher. Please log in with your account credentials."
    Write-Host "When RuneLite opens, it will write credentials.properties to: $runeliteDir"

    $jagexLauncher = Join-Path ${env:ProgramFiles(x86)} "Jagex Launcher\JagexLauncher.exe"
    if (Test-Path $jagexLauncher) {
        Start-Process -FilePath $jagexLauncher
    } elseif (Test-Path $runeliteExe) {
        Start-Process -FilePath $runeliteExe -WorkingDirectory $RuneLiteInstallDir
    }
} else {
    Write-Host "Credentials are not present yet for profile '$AccountProfile': $credentialsPath"
    Write-Host "For profile-specific credentials, we need to copy from the default profile first."
    Write-Host ""
    
    $defaultCreds = Join-Path $env:USERPROFILE ".runelite\credentials.properties"
    
    if (!(Test-Path $defaultCreds)) {
        Write-Host "Default credentials not found at: $defaultCreds" -ForegroundColor Yellow
        Write-Host "Please first bootstrap the default profile (without -AccountProfile parameter)"
        Write-Host "Then run this bootstrap again for your profile."
        exit 1
    }
    
    Write-Host "Copying credentials from default profile to '$AccountProfile' profile..."
    Copy-Item -LiteralPath $defaultCreds -Destination $credentialsPath -Force
    Write-Host "Credentials copied to: $credentialsPath" -ForegroundColor Green
    Write-Host ""
    Write-Host "IMPORTANT: The copied credentials are for your default account."
    Write-Host "To use this profile with a different account, you need to:"
    Write-Host "1. Launch this profile: .\scripts\credential-manager.ps1 -Action launch -Profile '$AccountProfile'"
    Write-Host "2. Log out from the current account in RuneLite"
    Write-Host "3. Log in with the new account credentials"
    Write-Host "4. The new credentials will be saved to this profile"
}
