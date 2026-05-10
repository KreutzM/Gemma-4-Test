param(
    [switch]$Clean,
    [switch]$SkipTests,
    [switch]$SkipLint,
    [switch]$NoInstall,
    [switch]$NoStart,
    [switch]$NoBuild,
    [string]$DeviceSerial
)

$ErrorActionPreference = "Stop"

$PackageName = "de.kreutzm.gemma4test"
$MainActivity = ".MainActivity"
$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
$GradleVersion = "8.10.2"

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host "`n==> $Name" -ForegroundColor Cyan
    & $Command
}

function Invoke-Gradle {
    param([string[]]$Arguments)

    & .\gradlew.bat @Arguments --stacktrace --no-daemon
}

function Invoke-Adb {
    param([string[]]$Arguments)

    if ($DeviceSerial) {
        & adb -s $DeviceSerial @Arguments
    } else {
        & adb @Arguments
    }
}

function Test-CommandAvailable {
    param([string]$CommandName)

    return [bool](Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Ensure-GradleWrapper {
    if ((Test-Path ".\gradlew.bat") -and (Test-Path ".\gradle\wrapper\gradle-wrapper.jar")) {
        return
    }

    if (-not (Test-CommandAvailable "gradle")) {
        throw "gradlew.bat oder gradle-wrapper.jar fehlt, und 'gradle' ist nicht im PATH. Installiere Gradle oder erzeuge den Wrapper lokal."
    }

    Invoke-Step "Generate Gradle Wrapper $GradleVersion" {
        & gradle wrapper --gradle-version $GradleVersion --distribution-type bin
    }

    if (-not (Test-Path ".\gradlew.bat")) {
        throw "Gradle Wrapper konnte nicht erzeugt werden: .\gradlew.bat fehlt."
    }
}

function Get-ConnectedDeviceCount {
    $devices = & adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
    return @($devices).Count
}

if (-not (Test-Path ".\settings.gradle.kts")) {
    throw "settings.gradle.kts wurde nicht gefunden. Bitte Skript aus dem Repository-Root ausfuehren."
}

Ensure-GradleWrapper

if (-not $NoBuild) {
    if ($Clean) {
        Invoke-Step "Clean" {
            Invoke-Gradle @("clean")
        }
    }

    if (-not $SkipTests) {
        Invoke-Step "Unit tests" {
            Invoke-Gradle @("testDebugUnitTest")
        }
    }

    if (-not $SkipLint) {
        Invoke-Step "Android lint" {
            Invoke-Gradle @("lintDebug")
        }
    }

    Invoke-Step "Assemble debug APK" {
        Invoke-Gradle @("assembleDebug")
    }
} else {
    Write-Host "`nNoBuild gesetzt: Gradle-Tasks werden uebersprungen." -ForegroundColor Yellow
}

if (-not (Test-Path $ApkPath)) {
    throw "APK wurde nicht gefunden: $ApkPath"
}

if ($NoInstall) {
    Write-Host "`nNoInstall gesetzt: Installation wird uebersprungen." -ForegroundColor Yellow
    exit 0
}

if (-not (Test-CommandAvailable "adb")) {
    throw "adb ist nicht im PATH. Installiere Android Platform Tools oder fuege sie zum PATH hinzu."
}

Invoke-Step "Check adb device" {
    Invoke-Adb @("devices")
}

if (-not $DeviceSerial) {
    $deviceCount = Get-ConnectedDeviceCount
    if ($deviceCount -eq 0) {
        throw "Kein freigegebenes ADB-Geraet gefunden. Aktiviere USB-Debugging und bestaetige die RSA-Abfrage auf dem Device."
    }
    if ($deviceCount -gt 1) {
        throw "Mehrere ADB-Geraete gefunden. Bitte -DeviceSerial <serial> angeben."
    }
}

Invoke-Step "Install debug APK" {
    Invoke-Adb @("install", "-r", $ApkPath)
}

if ($NoStart) {
    Write-Host "`nNoStart gesetzt: App-Start wird uebersprungen." -ForegroundColor Yellow
    exit 0
}

Invoke-Step "Start Gemma 4 Test" {
    Invoke-Adb @("shell", "am", "start", "-n", "$PackageName/$MainActivity")
}

Write-Host "`nFertig." -ForegroundColor Green
