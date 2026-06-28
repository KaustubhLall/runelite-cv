param(
	[int]$Port = 8770,
	[switch]$OpenBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$toolRoot = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "v3"

function Add-CommonHeaders {
	param([System.Net.HttpListenerResponse]$Response)
	$Response.Headers["Access-Control-Allow-Origin"] = "*"
	$Response.Headers["Access-Control-Allow-Methods"] = "GET, OPTIONS"
	$Response.Headers["Access-Control-Allow-Headers"] = "Content-Type"
	$Response.Headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
	$Response.Headers["Pragma"] = "no-cache"
	$Response.Headers["Expires"] = "0"
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

function Resolve-StaticPath {
	param([string]$RequestPath)
	$path = if ([string]::IsNullOrWhiteSpace($RequestPath) -or $RequestPath -eq "/") {
		"index.html"
	} else {
		$RequestPath.TrimStart("/")
	}
	$fullPath = [System.IO.Path]::GetFullPath((Join-Path $toolRoot $path))
	if (-not $fullPath.StartsWith($toolRoot, [System.StringComparison]::OrdinalIgnoreCase)) { return $null }
	if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) { return $null }
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
		".woff2" { return "font/woff2" }
		default { return "application/octet-stream" }
	}
}

# Try the requested port, then fall back to the next free one if it is already
# registered/listening (e.g. another v3 instance or the in-editor preview).
$listener = $null
$boundPort = $Port
for ($attempt = 0; $attempt -lt 12; $attempt++) {
	$candidate = $Port + $attempt
	$try = [System.Net.HttpListener]::new()
	$try.Prefixes.Add(("http://127.0.0.1:{0}/" -f $candidate))
	try {
		$try.Start()
		$listener = $try
		$boundPort = $candidate
		if ($attempt -gt 0) {
			Write-Host ("Port {0} was busy; using {1} instead." -f $Port, $candidate) -ForegroundColor Yellow
		}
		break
	} catch [System.Net.HttpListenerException] {
		$try.Close()
	}
}

if ($null -eq $listener) {
	Write-Error ("Could not bind any port in {0}..{1}. Close other servers and retry." -f $Port, ($Port + 11))
	exit 1
}
$Port = $boundPort

Write-Host ("CV Helper WebHelper Console v3 available at http://127.0.0.1:{0}/" -f $Port)

if ($OpenBrowser) {
	Start-Process ("http://127.0.0.1:{0}/" -f $Port) | Out-Null
}

try {
	while ($listener.IsListening) {
		$context = $listener.GetContext()
		try {
			$requestPath = $context.Request.Url.AbsolutePath
			$method = $context.Request.HttpMethod

			if ($method -eq "OPTIONS") {
				Add-CommonHeaders -Response $context.Response
				$context.Response.StatusCode = 204
				$context.Response.OutputStream.Close()
				continue
			}

			$staticPath = Resolve-StaticPath -RequestPath $requestPath
			if (-not $staticPath) {
				Add-CommonHeaders -Response $context.Response
				$context.Response.StatusCode = 404
				$context.Response.ContentType = "text/plain; charset=utf-8"
				$message = [System.Text.Encoding]::UTF8.GetBytes("not found")
				$context.Response.ContentLength64 = $message.Length
				$context.Response.OutputStream.Write($message, 0, $message.Length)
				$context.Response.OutputStream.Close()
				continue
			}

			$bytes = [System.IO.File]::ReadAllBytes($staticPath)
			Write-TextResponse -Response $context.Response -StatusCode 200 -ContentType (Get-ContentType -Path $staticPath) -Bytes $bytes
		} catch {
			try { $context.Response.Abort() } catch { }
		}
	}
} finally {
	$listener.Stop()
	$listener.Close()
}
