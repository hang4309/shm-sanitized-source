param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$DataDir = "",
    [switch]$Once,
    [int]$MaxRecords = 0,
    [switch]$DryRun,
    [switch]$NoVerify
)

$ErrorActionPreference = "Stop"

$CollectorScript = Join-Path $PSScriptRoot "os265_file_to_mysql_collector.py"
$LogPath = Join-Path $PSScriptRoot "logs\os265_file_to_mysql.log"
$DefaultDataDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\..\..\..\sample-data\os265"))

function Write-Step {
    param([string]$Message)
    Write-Host "[OS265 File Collector] $Message"
}

function Test-PythonCandidate {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $false
    }

    try {
        $output = & $Path --version 2>&1
        if ($LASTEXITCODE -eq 0 -and ($output -join " ") -match "Python\s+3\.") {
            return $true
        }
    } catch {
        return $false
    }

    return $false
}

function Find-Python {
    $candidates = @()

    if ($env:OS265_COLLECTOR_PYTHON) {
        $candidates += $env:OS265_COLLECTOR_PYTHON
    }

    $candidates += @(
        (Join-Path $env:LOCALAPPDATA "Programs\Python\Python313\python.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\Python\Python312\python.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\Python\Python311\python.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\Python\Python310\python.exe"),
        "C:\Python313\python.exe",
        "C:\Python312\python.exe",
        "C:\Python311\python.exe",
        "C:\Python310\python.exe"
    )

    foreach ($commandName in @("python3", "python")) {
        $command = Get-Command $commandName -ErrorAction SilentlyContinue
        if ($command) {
            $candidates += $command.Source
        }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ((Test-Path -LiteralPath $candidate) -and (Test-PythonCandidate -Path $candidate)) {
            return $candidate
        }
    }

    return $null
}

function Test-BackendReachable {
    param([string]$Url)

    try {
        Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 | Out-Null
        return $true
    } catch {
        if ($_.Exception.Response) {
            return $true
        }
        return $false
    }
}

if (-not (Test-Path -LiteralPath $CollectorScript)) {
    throw "Collector script not found: $CollectorScript"
}

$python = Find-Python
if (-not $python) {
    Write-Step "Python 3 is not available. Set OS265_COLLECTOR_PYTHON to a valid python.exe path."
    exit 1
}

Write-Step "Python: $python"

if (-not (Test-BackendReachable -Url $BaseUrl)) {
    Write-Step "Backend is not reachable at $BaseUrl. Please start the backend first."
    exit 1
}

Write-Step "Backend reachable: $BaseUrl"
if ([string]::IsNullOrWhiteSpace($DataDir)) {
    $EffectiveDataDir = $DefaultDataDir
} else {
    $EffectiveDataDir = $DataDir
}

Write-Step "Listening data record directory: $EffectiveDataDir"
Write-Step "Log file: $LogPath"

$args = @(
    $CollectorScript,
    "--base-url", $BaseUrl
)

if (-not [string]::IsNullOrWhiteSpace($DataDir)) {
    $args += @("--data-dir", $DataDir)
}

if ($Once) {
    $args += "--once"
}

if ($MaxRecords -gt 0) {
    $args += @("--max-records", [string]$MaxRecords)
}

if ($DryRun) {
    $args += "--dry-run"
}

if ($NoVerify) {
    $args += "--no-verify"
}

& $python @args
