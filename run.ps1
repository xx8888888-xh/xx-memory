$env:JAVA_HOME = "D:\software\android studio\jbr"
$env:ANDROID_HOME = "C:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Android\Sdk"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$apk = Join-Path $projectRoot "android\app\build\outputs\apk\debug\app-debug.apk"
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
$emulator = "C:\Android\Sdk\emulator\emulator.exe"
$avd = "Clock3_Test_Device"
$pkg = "com.xxmemory.app/.MainActivity"

$ErrorActionPreference = "SilentlyContinue"

Write-Host "========================================" -ForegroundColor White
Write-Host "  xx memory - Install & Launch" -ForegroundColor White
Write-Host "========================================" -ForegroundColor White

# === Step 1: Check if device is online ===
Write-Host "`n[1] Checking device..." -ForegroundColor Cyan
$devices = & $adb devices
if ($devices -match "emulator-5554\s+device") {
    Write-Host "  Device online" -ForegroundColor Green
} else {
    Write-Host "  Device offline, starting emulator..." -ForegroundColor Yellow

    # Clean lock files
    $avdRoot = "C:\Users\xx\.android\avd\${avd}.avd"
    if (Test-Path $avdRoot) {
        Get-ChildItem -Path $avdRoot -Recurse -Filter "*.lock" -ErrorAction SilentlyContinue | ForEach-Object {
            Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
        }
    }

    # Start emulator
    Start-Process -FilePath $emulator -ArgumentList @("-avd", $avd, "-no-snapshot-load")
    Write-Host "  Waiting for boot (up to 5 min)..." -ForegroundColor Yellow

    # Wait for device
    $waited = 0
    $ready = $false
    while ($waited -lt 300) {
        Start-Sleep -Seconds 5
        $waited += 5
        $devices = & $adb devices
        if ($devices -match "emulator-5554\s+device") {
            $ready = $true
            Write-Host "  Device online after ${waited}s" -ForegroundColor Green
            break
        }
        Write-Host "  ... ${waited}s" -ForegroundColor DarkGray
    }
    if (-not $ready) {
        Write-Host "  ERROR: Device not ready after 300s" -ForegroundColor Red
        Write-Host "  Please start emulator manually from Android Studio" -ForegroundColor Yellow
        exit 1
    }

    # Wait for boot completed
    Write-Host "  Waiting for boot complete..." -ForegroundColor Yellow
    $waited = 0
    while ($waited -lt 120) {
        Start-Sleep -Seconds 3
        $waited += 3
        $boot = & $adb -s emulator-5554 shell getprop sys.boot_completed 2>$null
        $bootStr = "$boot".Trim()
        if ($bootStr -eq "1") {
            Write-Host "  Boot completed after ${waited}s" -ForegroundColor Green
            break
        }
    }
}

# === Step 2: Install APK ===
Write-Host "`n[2] Installing APK..." -ForegroundColor Cyan
if (-not (Test-Path $apk)) {
    Write-Host "  Building APK..." -ForegroundColor Yellow
    Push-Location (Join-Path $projectRoot "android")
    & ".\gradlew.bat" :app:assembleDebug --no-daemon --console=plain --warning-mode=none 2>&1 | Out-Null
    Pop-Location
}
if (-not (Test-Path $apk)) {
    Write-Host "  ERROR: APK not found" -ForegroundColor Red
    exit 1
}
& $adb -s emulator-5554 install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Install failed" -ForegroundColor Red
    exit 1
}
Write-Host "  Install OK" -ForegroundColor Green

# === Step 3: Launch app ===
Write-Host "`n[3] Launching app..." -ForegroundColor Cyan
& $adb -s emulator-5554 shell am start -n $pkg
if ($LASTEXITCODE -eq 0) {
    Write-Host "  App launched" -ForegroundColor Green
} else {
    Write-Host "  ERROR: Launch failed" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor White
Write-Host "  DONE" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor White
