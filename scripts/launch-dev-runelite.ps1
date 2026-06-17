param(
    [string] $JavaHome = "C:\Program Files\Android\openjdk\jdk-21.0.8",
    [string] $ServiceVersion = "1.12.28"
)

$ErrorActionPreference = "Stop"

$credentialsPath = Join-Path $env:USERPROFILE ".runelite\credentials.properties"
$jarPath = Join-Path $PSScriptRoot "..\runelite-client\build\libs\client-1.12.29-SNAPSHOT-shaded.jar"
$javaExe = Join-Path $JavaHome "bin\java.exe"

if (!(Test-Path $credentialsPath)) {
    throw "Jagex launcher credentials are missing. Run scripts\bootstrap-jagex-credentials.ps1, then launch RuneLite from Jagex Launcher once for CoreDump/C0REDUMPED."
}

if (!(Test-Path $javaExe)) {
    throw "Java not found: $javaExe"
}

Push-Location (Join-Path $PSScriptRoot "..")
try {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
    .\gradlew.bat :client:shadowJar
} finally {
    Pop-Location
}

if (!(Test-Path $jarPath)) {
    throw "RuneLite shaded jar was not created: $jarPath"
}

$serviceBase = "https://api.runelite.net/runelite-$ServiceVersion"
$arguments = @(
    "-Drunelite.pluginhub.version=$ServiceVersion",
    "-Drunelite.http-service.url=$serviceBase",
    "-jar",
    (Resolve-Path $jarPath).Path
)

Write-Host "Launching custom RuneLite with service compatibility version: $ServiceVersion"
Write-Host "RuneLite API base: $serviceBase"
Start-Process -FilePath $javaExe -ArgumentList $arguments -WorkingDirectory (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
