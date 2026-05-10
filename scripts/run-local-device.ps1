param(
    [switch]$Clean,
    [switch]$SkipTests,
    [switch]$SkipLint,
    [switch]$NoInstall,
    [switch]$NoStart,
    [switch]$NoBuild,
    [string]$DeviceSerial,
    [string]$GradleUserHome = "D:\GradleHome-Codex"
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

function Find-CachedGradle {
    if (-not $env:GRADLE_USER_HOME) {
        return $null
    }

    $wrapperDists = Join-Path $env:GRADLE_USER_HOME "wrapper\dists"
    if (-not (Test-Path $wrapperDists)) {
        return $null
    }

    return Get-ChildItem -Path $wrapperDists -Recurse -Filter "gradle.bat" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}

function Initialize-GradleEnvironment {
    if ((Test-Path $GradleUserHome) -and ($env:GRADLE_USER_HOME -ne $GradleUserHome)) {
        $env:GRADLE_USER_HOME = $GradleUserHome
        Write-Host "GRADLE_USER_HOME=$GradleUserHome" -ForegroundColor DarkGray
    }

    $cachedGradle = Find-CachedGradle
    if ($cachedGradle) {
        $gradleHome = Split-Path (Split-Path $cachedGradle -Parent) -Parent
        if ($env:GRADLE_HOME -ne $gradleHome) {
            $env:GRADLE_HOME = $gradleHome
            Write-Host "GRADLE_HOME=$gradleHome" -ForegroundColor DarkGray
        }
    }
}

function Get-BootstrapGradle {
    $pathGradle = Get-Command "gradle" -ErrorAction SilentlyContinue
    if ($pathGradle) {
        return $pathGradle.Source
    }

    if ($env:GRADLE_HOME) {
        $gradleHomeCommand = Join-Path $env:GRADLE_HOME "bin\gradle.bat"
        if (Test-Path $gradleHomeCommand) {
            return $gradleHomeCommand
        }
    }

    return Find-CachedGradle
}

function Ensure-GradleWrapper {
    if ((Test-Path ".\gradlew.bat") -and (Test-Path ".\gradle\wrapper\gradle-wrapper.jar")) {
        return
    }

    $bootstrapGradle = Get-BootstrapGradle
    if (-not $bootstrapGradle) {
        throw "gradlew.bat oder gradle-wrapper.jar fehlt, und 'gradle' ist nicht im PATH. Installiere Gradle oder stelle einen Gradle-Cache unter $GradleUserHome bereit."
    }

    Invoke-Step "Generate Gradle Wrapper $GradleVersion" {
        & $bootstrapGradle wrapper --gradle-version $GradleVersion --distribution-type bin
    }

    if ((-not (Test-Path ".\gradlew.bat")) -or (-not (Test-Path ".\gradle\wrapper\gradle-wrapper.jar"))) {
        throw "Gradle Wrapper konnte nicht vollstaendig erzeugt werden: .\gradlew.bat oder .\gradle\wrapper\gradle-wrapper.jar fehlt."
    }
}

function Get-ConnectedDeviceCount {
    $devices = & adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
    return @($devices).Count
}

if (-not (Test-Path ".\settings.gradle.kts")) {
    throw "settings.gradle.kts wurde nicht gefunden. Bitte Skript aus dem Repository-Root ausfuehren."
}

Initialize-GradleEnvironment
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
