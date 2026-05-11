param(
    [string]$DeviceSerial,
    [string]$OutputDir = "logs",
    [string]$GalleryPackage = "com.google.ai.edge.gallery",
    [string]$OurPackage = "de.kreutzm.gemma4test",
    [string]$GalleryFileHint = "gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm",
    [string]$OurModelFile = "gemma-4-E2B-it.litertlm"
)

$ErrorActionPreference = "Stop"

function Test-CommandAvailable {
    param([string]$CommandName)
    return [bool](Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Invoke-Adb {
    param([string[]]$Arguments)
    if ($DeviceSerial) {
        & adb -s $DeviceSerial @Arguments
    } else {
        & adb @Arguments
    }
}

function Get-ConnectedDeviceCount {
    $devices = & adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
    return @($devices).Count
}

function Invoke-AdbText {
    param([string[]]$Arguments)
    $output = Invoke-Adb $Arguments 2>&1
    return ($output -join "`n").Trim()
}

function Write-Section {
    param(
        [string]$Title,
        [string]$Text,
        [string]$Path
    )
    Add-Content -Path $Path -Value "`n## $Title"
    if ($Text) {
        Add-Content -Path $Path -Value $Text
    } else {
        Add-Content -Path $Path -Value "<empty>"
    }
}

function Get-ShellFileMetadata {
    param([string]$Path)

    $quoted = $Path.Replace("'", "'\''")
    $command = "if [ -f '$quoted' ]; then ls -l '$quoted'; wc -c < '$quoted'; sha256sum '$quoted' 2>/dev/null || toybox sha256sum '$quoted' 2>/dev/null || echo 'sha256sum unavailable'; else echo 'missing'; fi"
    return Invoke-AdbText @("shell", "sh", "-c", $command)
}

function Get-RunAsFileMetadata {
    param(
        [string]$Package,
        [string]$RelativePath
    )

    $quoted = $RelativePath.Replace("'", "'\''")
    $command = "if [ -f '$quoted' ]; then ls -l '$quoted'; wc -c < '$quoted'; sha256sum '$quoted' 2>/dev/null || toybox sha256sum '$quoted' 2>/dev/null || echo 'sha256sum unavailable'; else echo 'missing'; fi"
    return Invoke-AdbText @("shell", "run-as", $Package, "sh", "-c", $command)
}

function Find-GalleryModelFiles {
    param(
        [string]$Package,
        [string]$FileHint
    )

    $roots = @(
        "/sdcard/Android/data/$Package/files",
        "/storage/emulated/0/Android/data/$Package/files"
    )
    $conditions = @()
    foreach ($root in $roots) {
        $conditions += "if [ -d '$root' ]; then find '$root' -type f -name '*.litertlm' 2>/dev/null; fi"
    }
    $command = ($conditions -join "; ")
    $found = Invoke-AdbText @("shell", "sh", "-c", $command)
    if (-not $found) {
        return @()
    }
    $items = $found -split "`n" | Where-Object { $_.Trim().Length -gt 0 } | Select-Object -Unique
    if ($FileHint) {
        $preferred = $items | Where-Object { $_ -like "*$FileHint*" }
        if (@($preferred).Count -gt 0) {
            return @($preferred)
        }
    }
    return @($items)
}

if (-not (Test-CommandAvailable "adb")) {
    throw "adb ist nicht im PATH. Installiere Android Platform Tools oder fuege sie zum PATH hinzu."
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

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryPath = Join-Path $OutputDir "litert-model-comparison-$timestamp.md"

Set-Content -Path $summaryPath -Value "# LiteRT model file comparison"
Add-Content -Path $summaryPath -Value ""
Add-Content -Path $summaryPath -Value "Timestamp: $timestamp"
Add-Content -Path $summaryPath -Value "Gallery package: $GalleryPackage"
Add-Content -Path $summaryPath -Value "Our package: $OurPackage"
Add-Content -Path $summaryPath -Value "Device serial: $DeviceSerial"

Write-Host "`n==> Device" -ForegroundColor Cyan
$deviceInfo = Invoke-AdbText @("shell", "getprop", "ro.product.manufacturer")
$deviceModel = Invoke-AdbText @("shell", "getprop", "ro.product.model")
$deviceSoc = Invoke-AdbText @("shell", "getprop", "ro.soc.model")
Write-Section -Title "Device" -Path $summaryPath -Text "Manufacturer: $deviceInfo`nModel: $deviceModel`nSoC: $deviceSoc"

Write-Host "`n==> Package versions" -ForegroundColor Cyan
$galleryPkg = Invoke-AdbText @("shell", "dumpsys", "package", $GalleryPackage, "|", "grep", "version")
if (-not $galleryPkg) {
    $galleryPkg = Invoke-AdbText @("shell", "dumpsys", "package", $GalleryPackage)
}
$ourPkg = Invoke-AdbText @("shell", "dumpsys", "package", $OurPackage, "|", "grep", "version")
if (-not $ourPkg) {
    $ourPkg = Invoke-AdbText @("shell", "dumpsys", "package", $OurPackage)
}
Write-Section -Title "Gallery package metadata" -Path $summaryPath -Text $galleryPkg
Write-Section -Title "Our app package metadata" -Path $summaryPath -Text $ourPkg

Write-Host "`n==> Gallery model files" -ForegroundColor Cyan
$galleryFiles = Find-GalleryModelFiles -Package $GalleryPackage -FileHint $GalleryFileHint
if (@($galleryFiles).Count -eq 0) {
    Write-Section -Title "Gallery model files" -Path $summaryPath -Text "No .litertlm files found under /sdcard/Android/data/$GalleryPackage/files. Open Gallery, download Gemma-4-E2B-it, then run this script again."
} else {
    Add-Content -Path $summaryPath -Value "`n## Gallery model files"
    foreach ($file in $galleryFiles) {
        Add-Content -Path $summaryPath -Value "`n### $file"
        $metadata = Get-ShellFileMetadata -Path $file
        Add-Content -Path $summaryPath -Value $metadata
    }
}

Write-Host "`n==> Our app model file" -ForegroundColor Cyan
$ourRelativePath = "files/models/$OurModelFile"
$ourMetadata = Get-RunAsFileMetadata -Package $OurPackage -RelativePath $ourRelativePath
Write-Section -Title "Our app model file via run-as" -Path $summaryPath -Text "Relative path: $ourRelativePath`n$ourMetadata"

Write-Host "`n==> Recent LiteRT/OpenCL log summary" -ForegroundColor Cyan
$patterns = "GemmaVisionEngine|AGLlmChatModelHelper|MainExecutorSettings|VisionExecutorSettings|EncoderBackend|AdapterBackend|OpenCL|OpenGL|LITERT_CL|CreateSharedMemoryManager|Failed to create engine|LiteRT backend|model_path|gemma4_2b|gemma-4-E2B"
$logSummary = Invoke-AdbText @("logcat", "-d", "-v", "time")
$filteredLog = ($logSummary -split "`n" | Select-String -Pattern $patterns -CaseSensitive:$false | ForEach-Object { $_.Line }) -join "`n"
Write-Section -Title "Recent LiteRT/OpenCL log summary" -Path $summaryPath -Text $filteredLog

Write-Host "`nSummary written to $summaryPath" -ForegroundColor Green
Write-Host "Next: compare Gallery and app SHA-256 values. If they differ, test our app with the Gallery file or align to Gallery's exact model revision before changing EngineConfig." -ForegroundColor Yellow
