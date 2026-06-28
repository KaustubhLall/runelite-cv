param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("list", "create", "bootstrap", "launch", "delete")]
    [string] $Action = "list",
    
    [Parameter(Mandatory=$false)]
    [string] $Profile = ""
)

$ErrorActionPreference = "Stop"

function Get-RuneliteProfiles {
    $profileDirs = Get-ChildItem -Path (Join-Path $env:USERPROFILE ".runelite*") -Directory -ErrorAction SilentlyContinue
    $profiles = @()
    
    foreach ($dir in $profileDirs) {
        if ($dir.Name -eq ".runelite") {
            $profiles += [PSCustomObject]@{
                Name = "default"
                Path = $dir.FullName
                HasCredentials = Test-Path (Join-Path $dir.FullName "credentials.properties")
            }
        } elseif ($dir.Name -match "^\.runelite-(.+)$") {
            $profiles += [PSCustomObject]@{
                Name = $matches[1]
                Path = $dir.FullName
                HasCredentials = Test-Path (Join-Path $dir.FullName "credentials.properties")
            }
        }
    }
    
    return $profiles
}

function Show-ProfileList {
    $profiles = Get-RuneliteProfiles
    
    if ($profiles.Count -eq 0) {
        Write-Host "No RuneLite profiles found." -ForegroundColor Yellow
        return
    }
    
    Write-Host "RuneLite Profiles:" -ForegroundColor Cyan
    Write-Host "----------------" -ForegroundColor Cyan
    
    foreach ($prof in $profiles) {
        $credStatus = if ($prof.HasCredentials) { "[OK]" } else { "[--]" }
        Write-Host "  $credStatus $($prof.Name) - $($prof.Path)" -ForegroundColor White
    }
}

function New-Profile {
    param([string] $ProfileName)
    
    if ([string]::IsNullOrWhiteSpace($ProfileName)) {
        throw "Profile name is required for create action"
    }
    
    $profileDir = Join-Path $env:USERPROFILE ".runelite-$ProfileName"
    
    if (Test-Path $profileDir) {
        Write-Host "Profile '$ProfileName' already exists at: $profileDir" -ForegroundColor Yellow
        return
    }
    
    New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
    Write-Host "Created profile '$ProfileName' at: $profileDir" -ForegroundColor Green
    Write-Host "Run 'credential-manager.ps1 -Action bootstrap -Profile $ProfileName' to set up credentials." -ForegroundColor Cyan
}

function Initialize-ProfileCredentials {
    param([string] $ProfileName)
    
    if ([string]::IsNullOrWhiteSpace($ProfileName)) {
        throw "Profile name is required for bootstrap action"
    }
    
    $bootstrapScript = Join-Path $PSScriptRoot "bootstrap-jagex-credentials.ps1"
    
    if (!(Test-Path $bootstrapScript)) {
        throw "Bootstrap script not found: $bootstrapScript"
    }
    
    Write-Host "Bootstrapping credentials for profile '$ProfileName'..." -ForegroundColor Cyan
    & $bootstrapScript -AccountProfile $ProfileName
}

function Launch-Profile {
    param([string] $ProfileName)
    
    $launchScript = Join-Path $PSScriptRoot "launch-dev-runelite.ps1"
    
    if (!(Test-Path $launchScript)) {
        throw "Launch script not found: $launchScript"
    }
    
    if ([string]::IsNullOrWhiteSpace($ProfileName)) {
        Write-Host "Launching with default profile..." -ForegroundColor Cyan
        & $launchScript
    } else {
        Write-Host "Launching with profile '$ProfileName'..." -ForegroundColor Cyan
        & $launchScript -AccountProfile $ProfileName
    }
}

function Remove-Profile {
    param([string] $ProfileName)
    
    if ([string]::IsNullOrWhiteSpace($ProfileName)) {
        throw "Profile name is required for delete action"
    }
    
    if ($ProfileName -eq "default") {
        throw "Cannot delete the default profile"
    }
    
    $profileDir = Join-Path $env:USERPROFILE ".runelite-$ProfileName"
    
    if (!(Test-Path $profileDir)) {
        Write-Host "Profile '$ProfileName' does not exist." -ForegroundColor Yellow
        return
    }
    
    Write-Host "This will delete the profile directory: $profileDir" -ForegroundColor Yellow
    $confirm = Read-Host "Are you sure? (yes/no)"
    
    if ($confirm -eq "yes") {
        Remove-Item -Path $profileDir -Recurse -Force
        Write-Host "Deleted profile '$ProfileName'." -ForegroundColor Green
    } else {
        Write-Host "Deletion cancelled." -ForegroundColor Yellow
    }
}

switch ($Action) {
    "list" {
        Show-ProfileList
    }
    "create" {
        New-Profile -ProfileName $Profile
    }
    "bootstrap" {
        Initialize-ProfileCredentials -ProfileName $Profile
    }
    "launch" {
        Launch-Profile -ProfileName $Profile
    }
    "delete" {
        Remove-Profile -ProfileName $Profile
    }
    default {
        Write-Host "Unknown action: $Action" -ForegroundColor Red
        Write-Host "Valid actions: list, create, bootstrap, launch, delete" -ForegroundColor Cyan
    }
}
