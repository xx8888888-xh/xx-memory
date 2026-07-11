param(
    [string]$Command,
    [string]$CommandFile = "ai_cmd.txt",
    [string]$OutputFile = "ai_out.txt",
    [string]$ErrFile = "ai_err.txt",
    [string]$ExitFile = "ai_exit.txt",
    [string]$BackupDir = "ai_backup",
    [int]$TimeoutSeconds = 600,
    [string]$BackupTargets = "",
    [switch]$Cleanup,
    [switch]$NoBackup,
    [switch]$DryRun,
    [switch]$WriteToFile
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $scriptDir) { $scriptDir = (Get-Location).Path }

$CommandFile = Join-Path $scriptDir $CommandFile
$OutputFile = Join-Path $scriptDir $OutputFile
$ErrFile = Join-Path $scriptDir $ErrFile
$ExitFile = Join-Path $scriptDir $ExitFile
$BackupDir = Join-Path $scriptDir $BackupDir

$MARK_START = "===AI-RELAY-START==="
$MARK_END   = "===AI-RELAY-END==="

# ---------- Cleanup Mode ----------
if ($Cleanup) {
    foreach ($f in @($CommandFile, $OutputFile, $ErrFile, $ExitFile)) {
        if (Test-Path $f) { Remove-Item $f -Force -ErrorAction SilentlyContinue }
    }
    Write-Output "Cleanup done"
    exit 0
}

# ---------- Backup Utility ----------
function Backup-IfExists {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return }
    if ($NoBackup) { return }
    if (-not (Test-Path $BackupDir)) {
        New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    }
    $name = [System.IO.Path]::GetFileName($Path)
    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $dest = Join-Path $BackupDir "${stamp}_${name}"
    Copy-Item -Path $Path -Destination $dest -Force
}

# ---------- Resolve Command ----------
$cmd = ""
if ($Command) {
    $cmd = $Command
} elseif (Test-Path $CommandFile) {
    $cmd = (Get-Content -Path $CommandFile -Raw -Encoding utf8).Trim()
}

if ([string]::IsNullOrWhiteSpace($cmd)) {
    Write-Output $MARK_START
    Write-Output "[EXIT-CODE] 1"
    Write-Output "[STDERR] No command. Pass -Command or write to $CommandFile"
    Write-Output $MARK_END
    exit 1
}

# ---------- Pre-Backup Affected Targets ----------
$targets = @()
if ($BackupTargets) {
    foreach ($raw in $BackupTargets.Split(';')) {
        $p = $raw.Trim().Trim('"',"'")
        if ($p -and (Test-Path $p)) { $targets += $p }
    }
}
if ($cmd -match "(?i)\b(Remove-Item|del|rm|rd|rmdir|Remove-ItemProperty|Clear-Content)\b") {
    $regex = [regex]'"([^"]+)"|''([^'']+)'''
    foreach ($m in $regex.Matches($cmd)) {
        $p = if ($m.Groups[1].Success) { $m.Groups[1].Value } else { $m.Groups[2].Value }
        if (Test-Path $p) { $targets += $p }
    }
}
foreach ($t in $targets) { Backup-IfExists -Path $t }

# ---------- Dry-Run ----------
if ($DryRun) {
    Write-Output $MARK_START
    Write-Output "[EXIT-CODE] 0"
    Write-Output "[STDOUT] DRY-RUN: $cmd"
    Write-Output $MARK_END
    if (Test-Path $CommandFile) { Remove-Item $CommandFile -Force -ErrorAction SilentlyContinue }
    exit 0
}

# ---------- Execute ----------
$env:PATH = "C:\Windows\System32;C:\Windows\SysWOW64;C:\Windows;C:\Windows\System32\Wbem;" + $env:PATH

$lastExit = 0
$stdout = ""
$stderr = ""

try {
    $output = Invoke-Expression $cmd 2>&1
    $lastExit = if ($null -ne $LASTEXITCODE) { $LASTEXITCODE } else { 0 }

    foreach ($line in $output) {
        if ($line -is [System.Management.Automation.ErrorRecord]) {
            $stderr += $line.ToString() + "`n"
        } else {
            $stdout += $line.ToString() + "`n"
        }
    }
} catch {
    $stderr += "ERROR: $($_.Exception.Message)`n"
    $lastExit = 1
}

# ---------- Emit (default: console, AI sees directly) ----------
Write-Output $MARK_START
Write-Output "[EXIT-CODE] $lastExit"
if ($stdout) { Write-Output "[STDOUT]"; Write-Output $stdout }
if ($stderr) { Write-Output "[STDERR]"; Write-Output $stderr }
Write-Output $MARK_END

# ---------- Optional File Dump (off by default) ----------
if ($WriteToFile) {
    $stdout | Out-File -FilePath $OutputFile -Encoding utf8 -NoNewline
    $stderr | Out-File -FilePath $ErrFile -Encoding utf8 -NoNewline
    "$lastExit" | Out-File -FilePath $ExitFile -Encoding utf8
}

# ---------- Auto-clean input ----------
if (Test-Path $CommandFile) { Remove-Item $CommandFile -Force -ErrorAction SilentlyContinue }