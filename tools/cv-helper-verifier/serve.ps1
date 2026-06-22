param(
	[int]$Port = 8765,
	[switch]$OpenBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$toolRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$preferredCvHelperPort = 11777
$verifierSurfaces = @("prayer", "spell", "minimap", "inventory", "equipment", "panels", "combat")

function Add-CommonHeaders {
	param([System.Net.HttpListenerResponse]$Response)

	$Response.Headers["Access-Control-Allow-Origin"] = "*"
	$Response.Headers["Access-Control-Allow-Methods"] = "GET, OPTIONS"
	$Response.Headers["Access-Control-Allow-Headers"] = "Content-Type"
	$Response.Headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
	$Response.Headers["Pragma"] = "no-cache"
	$Response.Headers["Expires"] = "0"
}

function Write-JsonResponse {
	param(
		[System.Net.HttpListenerResponse]$Response,
		[int]$StatusCode,
		$Body
	)

	$json = $Body | ConvertTo-Json -Depth 8
	$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
	Add-CommonHeaders -Response $Response
	$Response.StatusCode = $StatusCode
	$Response.ContentType = "application/json; charset=utf-8"
	$Response.ContentLength64 = $bytes.Length
	$Response.OutputStream.Write($bytes, 0, $bytes.Length)
	$Response.OutputStream.Close()
}

function Write-TextResponse {
	param(
		[System.Net.HttpListenerResponse]$Response,
		[int]$StatusCode,
		[string]$ContentType,
		[byte[]]$Bytes
	)

	Add-CommonHeaders -Response $Response
	$Response.StatusCode = $StatusCode
	$Response.ContentType = $ContentType
	$Response.ContentLength64 = $Bytes.Length
	$Response.OutputStream.Write($Bytes, 0, $Bytes.Length)
	$Response.OutputStream.Close()
}

function ConvertTo-PortNumber {
	param([string]$Value)

	$text = if ($null -eq $Value) { "" } else { $Value.Trim() }
	$parsed = 0
	if ([int]::TryParse($text, [ref]$parsed) -and $parsed -ge 1 -and $parsed -le 65535) {
		return $parsed
	}
	return $null
}

function Parse-KnownPorts {
	param([string]$Known)

	if ([string]::IsNullOrWhiteSpace($Known)) {
		return @()
	}

	return $Known.Split(",") |
		ForEach-Object { ConvertTo-PortNumber $_ } |
		Where-Object { $_ -ne $null }
}

function Get-ProcessNameById {
	param([int]$ProcessId)

	try {
		return (Get-Process -Id $ProcessId -ErrorAction Stop).ProcessName
	} catch {
		return $null
	}
}

function Get-JavaListeningPorts {
	$candidates = @()
	try {
		$connections = Get-NetTCPConnection -State Listen -ErrorAction Stop |
			Where-Object {
				$_.LocalPort -ne $Port -and (
					$_.LocalAddress -eq "127.0.0.1" -or
					$_.LocalAddress -eq "0.0.0.0" -or
					$_.LocalAddress -eq "::1" -or
					$_.LocalAddress -eq "::"
				)
			}

		foreach ($connection in $connections) {
			$processName = Get-ProcessNameById -ProcessId $connection.OwningProcess
			if ($processName -notmatch "^(java|RuneLite)$") {
				continue
			}
			$candidates += [pscustomobject]@{
				port = [int]$connection.LocalPort
				source = "java-listener"
				processName = $processName
				pid = [int]$connection.OwningProcess
			}
		}
	} catch {
	}

	$seen = @{}
	return $candidates | Where-Object {
		if ($seen.ContainsKey($_.port)) {
			return $false
		}
		$seen[$_.port] = $true
		return $true
	}
}

function Get-DiscoveryCandidates {
	param([int[]]$KnownPorts)

	$ordered = New-Object System.Collections.ArrayList
	$seen = @{}
	function Try-AddCandidate {
		param(
			[int]$CandidatePort,
			[string]$Source,
			[string]$ProcessName,
			[int]$ProcessId
		)

		if ($CandidatePort -le 0 -or $seen.ContainsKey($CandidatePort)) {
			return
		}
		$seen[$CandidatePort] = $true
		$null = $ordered.Add([pscustomobject]@{
			port = $CandidatePort
			source = $Source
			processName = $ProcessName
			pid = $ProcessId
		})
	}

	Try-AddCandidate -CandidatePort $preferredCvHelperPort -Source "preferred-port" -ProcessName $null -ProcessId 0
	foreach ($knownPort in $KnownPorts) {
		Try-AddCandidate -CandidatePort $knownPort -Source "known-port" -ProcessName $null -ProcessId 0
	}
	foreach ($listener in Get-JavaListeningPorts) {
		Try-AddCandidate -CandidatePort $listener.port -Source $listener.source -ProcessName $listener.processName -ProcessId $listener.pid
	}

	return @($ordered)
}

function Test-CvHelperPort {
	param([pscustomobject]$Candidate)

	$result = [ordered]@{
		port = [string]$Candidate.port
		source = $Candidate.source
		processName = $Candidate.processName
		pid = $Candidate.pid
		ok = $false
		activePort = $null
		status = $null
		gameState = $null
		loggedIn = $null
		hasMobConfig = $false
		hasSkillFarmers = $false
		hasVerifierDashboard = $false
		error = $null
	}

	try {
		$status = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/status" -f $Candidate.port) -TimeoutSec 1
		if ($status.plugin -ne "CV Helper") {
			throw "unexpected-plugin:$($status.plugin)"
		}
		$activePort = ConvertTo-PortNumber ([string]$status.port)
		$resolvedPort = if ($null -ne $activePort) { $activePort } else { $Candidate.port }
		$result.activePort = [string]$resolvedPort
		$result.status = [string]$status.status
		$result.gameState = [string]$status.player.gameState
		$result.loggedIn = [bool]$status.player.loggedIn
		try {
			$null = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/automation/mob-farmer/config" -f $resolvedPort) -TimeoutSec 1
			$result.hasMobConfig = $true
		} catch {
			$result.hasMobConfig = $false
		}
		try {
			$mining = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/automation/mining/config" -f $resolvedPort) -TimeoutSec 1
			$woodcutting = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/automation/woodcutting/config" -f $resolvedPort) -TimeoutSec 1
			$result.hasSkillFarmers = ($mining.version -eq 1 -and $woodcutting.version -eq 1)
		} catch {
			$result.hasSkillFarmers = $false
		}
		try {
			$null = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/entities" -f $resolvedPort) -TimeoutSec 1
			$null = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/entities/nearest" -f $resolvedPort) -TimeoutSec 1
			foreach ($surface in $verifierSurfaces) {
				$null = Invoke-RestMethod -Uri ("http://127.0.0.1:{0}/targets/{1}" -f $resolvedPort, $surface) -TimeoutSec 1
			}
			$result.hasVerifierDashboard = $true
		} catch {
			$result.hasVerifierDashboard = $false
		}
		$result.ok = $true
	} catch {
		$result.error = $_.Exception.Message
	}

	return [pscustomobject]$result
}

function Invoke-Discovery {
	param([int[]]$KnownPorts)

	$attempts = @()
	$active = $null
	$bestScore = -1
	foreach ($candidate in Get-DiscoveryCandidates -KnownPorts $KnownPorts) {
		$attempt = Test-CvHelperPort -Candidate $candidate
		$attempts += $attempt
		if ($attempt.ok) {
			$score = 0
			if ($attempt.hasMobConfig) {
				$score += 1
			}
			if ($attempt.hasSkillFarmers) {
				$score += 2
			}
			if ($attempt.hasVerifierDashboard) {
				$score += 4
			}
			if ($score -gt $bestScore) {
				$active = $attempt
				$bestScore = $score
			}
			if ($score -ge 7) {
				break
			}
		}
	}

	if ($active) {
		$preferredAttempt = $attempts | Where-Object { $_.port -eq [string]$preferredCvHelperPort } | Select-Object -First 1
		$preferredWasStale = $preferredAttempt -and $preferredAttempt.ok -and (($preferredAttempt.hasVerifierDashboard -ne $active.hasVerifierDashboard) -or ($preferredAttempt.hasSkillFarmers -ne $active.hasSkillFarmers)) -and $active.activePort -ne $preferredAttempt.activePort
		return [ordered]@{
			helperAvailable = $true
			activePort = $active.activePort
			source = $active.source
			sourceLabel = $active.source -replace "-", " "
			summary = if ($preferredWasStale) {
				"Recovered CV Helper on $($active.activePort) because preferred port 11777 is an older/stale export without the newest verifier endpoints."
			} elseif ($active.source -eq "preferred-port") {
				"Connected on the preferred CV Helper port 11777."
			} else {
				"Recovered CV Helper on fallback port $($active.activePort) after 11777 was unavailable."
			}
			error = ""
			attempts = $attempts
		}
	}

	return [ordered]@{
		helperAvailable = $true
		activePort = ""
		source = "helper-scan"
		sourceLabel = "helper scan"
		summary = "No live CV Helper export was found. The helper tried 11777 first, then known ports, then listening local Java ports."
		error = if ($attempts.Count) { $attempts[-1].error } else { "no-attempts" }
		attempts = $attempts
	}
}

function Resolve-StaticPath {
	param([string]$RequestPath)

	$path = if ([string]::IsNullOrWhiteSpace($RequestPath) -or $RequestPath -eq "/") {
		"index.html"
	} else {
		$RequestPath.TrimStart("/")
	}

	$fullPath = [System.IO.Path]::GetFullPath((Join-Path $toolRoot $path))
	if (-not $fullPath.StartsWith($toolRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
		return $null
	}
	if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
		return $null
	}
	return $fullPath
}

function Get-ContentType {
	param([string]$Path)

	switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
		".html" { return "text/html; charset=utf-8" }
		".js" { return "application/javascript; charset=utf-8" }
		".css" { return "text/css; charset=utf-8" }
		".json" { return "application/json; charset=utf-8" }
		".png" { return "image/png" }
		".svg" { return "image/svg+xml" }
		default { return "application/octet-stream" }
	}
}

$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add(("http://127.0.0.1:{0}/" -f $Port))
$listener.Start()

Write-Host ("CV Helper verifier available at http://127.0.0.1:{0}/" -f $Port)
Write-Host "Discovery helper probes 11777 first, then known ports, then live local Java listeners."

if ($OpenBrowser) {
	Start-Process ("http://127.0.0.1:{0}/" -f $Port) | Out-Null
}

try {
	while ($listener.IsListening) {
		$context = $listener.GetContext()
		$requestPath = $context.Request.Url.AbsolutePath

		if ($context.Request.HttpMethod -eq "OPTIONS") {
			Add-CommonHeaders -Response $context.Response
			$context.Response.StatusCode = 204
			$context.Response.OutputStream.Close()
			continue
		}

		if ($requestPath -eq "/api/discover") {
			$knownPorts = Parse-KnownPorts -Known $context.Request.QueryString["known"]
			$payload = Invoke-Discovery -KnownPorts $knownPorts
			Write-JsonResponse -Response $context.Response -StatusCode 200 -Body $payload
			continue
		}

		$staticPath = Resolve-StaticPath -RequestPath $requestPath
		if (-not $staticPath) {
			Write-JsonResponse -Response $context.Response -StatusCode 404 -Body @{ error = "not-found"; path = $requestPath }
			continue
		}

		$bytes = [System.IO.File]::ReadAllBytes($staticPath)
		Write-TextResponse -Response $context.Response -StatusCode 200 -ContentType (Get-ContentType -Path $staticPath) -Bytes $bytes
	}
} finally {
	$listener.Stop()
	$listener.Close()
}
