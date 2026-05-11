param(
    [string]$DeviceSerial = "",
    [string]$OutputDir = "logs",
    [int]$HashTimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

$GalleryPackage = "com.google.ai.edge.gallery"
$AppPackage = "de.kreutzm.gemma4test"
$AppModelRelativePath = "files/models/gemma-4-E2B-it.litertlm"
$AppModelPath = "/data/user/0/$AppPackage/$AppModelRelativePath"
$KnownGalleryModelPath = "/storage/emulated/0/Android/data/com.google.ai.edge.gallery/files/Gemma_4_E2B_it/20260325/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm"

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $adbArgs = @()
    if ($DeviceSerial.Trim().Length -gt 0) { $adbArgs += @("-s", $DeviceSerial) }
    $adbArgs += $Arguments
    & adb @adbArgs
}

function Invoke-AdbText {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $output = Invoke-Adb @Arguments 2>&1
    return ($output | Out-String).Trim()
}

function Escape-ShellSingleQuoted {
    param([string]$Value)
    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Split-ParentPath {
    param([string]$Path)
    $index = $Path.LastIndexOf("/")
    if ($index -lt 0) { return "" }
    return $Path.Substring(0, $index)
}

function Assert-AdbReady {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb ist nicht im PATH. Installiere Android Platform Tools oder fuege sie zum PATH hinzu."
    }
    if ($DeviceSerial.Trim().Length -eq 0) {
        $devices = (& adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" })
        $count = @($devices).Count
        if ($count -eq 0) { throw "Kein freigegebenes ADB-Geraet gefunden." }
        if ($count -gt 1) { throw "Mehrere ADB-Geraete gefunden. Bitte -DeviceSerial <serial> angeben." }
    }
}

function Get-PackageVersion {
    param([string]$PackageName)
    $dump = Invoke-AdbText shell dumpsys package $PackageName
    if ($dump.Length -eq 0) { return "unavailable" }
    $versionName = (($dump -split "`r?`n") | Where-Object { $_ -match "versionName=" } | Select-Object -First 1).Trim()
    $versionCode = (($dump -split "`r?`n") | Where-Object { $_ -match "versionCode=" } | Select-Object -First 1).Trim()
    $parts = @()
    if ($versionName) { $parts += $versionName }
    if ($versionCode) { $parts += $versionCode }
    if ($parts.Count -eq 0) { return "unavailable" }
    return ($parts -join "; ")
}

function Get-RecentLogLines {
    $patterns = @(
        "GemmaVisionEngine", "AGLlmChatModelHelper", "MainExecutorSettings", "VisionExecutorSettings",
        "EncoderBackend", "AdapterBackend", "backend: GPU", "backend: CPU", "OpenCL", "OpenGL",
        "LITERT_CL", "CreateSharedMemoryManager", "Failed to create engine", "LiteRT backend",
        "model_path", "gemma4_2b", "gemma-4-E2B", "XNNPack", "Exception", "Error"
    ) -join "|"
    $lines = Invoke-Adb logcat -d -v time |
        Select-String -Pattern $patterns -CaseSensitive:$false |
        Select-Object -Last 120 |
        ForEach-Object { $_.Line }
    return @($lines)
}

function Get-GalleryModelPath {
    $logs = Get-RecentLogLines
    $fromLogs = $logs |
        ForEach-Object { if ($_ -match "model_path:\s*(/storage/[^\s]+\.litertlm)") { $Matches[1] } } |
        Select-Object -First 1
    if ($fromLogs) { return $fromLogs }

    $found = Invoke-AdbText shell find "/storage/emulated/0/Android/data/$GalleryPackage/files" -name "*.litertlm" -type f
    if ($LASTEXITCODE -eq 0 -and $found.Length -gt 0) {
        return (($found -split "`r?`n") | Where-Object { $_.Trim().Length -gt 0 } | Select-Object -First 1).Trim()
    }
    return $KnownGalleryModelPath
}

function Get-ExternalFileMetadata {
    param([string]$Path)
    $quotedPath = Escape-ShellSingleQuoted $Path
    $exists = Invoke-AdbText shell sh -c "test -f $quotedPath && echo yes || echo no"
    $size = "unavailable"; $sha = "unavailable"; $warning = ""
    if ($exists -eq "yes") {
        $sizeValue = Invoke-AdbText shell stat -c "%s" $Path
        if ($LASTEXITCODE -eq 0 -and $sizeValue -match "^\d+$") { $size = $sizeValue }
        $shaValue = Invoke-AdbText shell sh -c "timeout $HashTimeoutSeconds sha256sum $quotedPath"
        if ($LASTEXITCODE -eq 0 -and $shaValue -match "^[a-fA-F0-9]{64}") {
            $sha = $Matches[0].ToLowerInvariant()
        } else {
            $warning = "Could not hash external file within $HashTimeoutSeconds seconds."
        }
    } else {
        $warning = "File was not readable at expected external app-files path."
    }
    return [pscustomobject]@{ Path = $Path; FileName = [System.IO.Path]::GetFileName($Path); ParentDirectory = Split-ParentPath $Path; SizeBytes = $size; Sha256 = $sha; Warning = $warning }
}

function Get-InternalAppFileMetadata {
    param([string]$PackageName, [string]$DisplayPath, [string]$RelativePath)
    $size = "unavailable"; $sha = "unavailable"; $warning = ""
    $exists = Invoke-AdbText shell run-as $PackageName sh -c "test -f '$RelativePath' && echo yes || echo no"
    if ($LASTEXITCODE -eq 0 -and $exists -eq "yes") {
        $sizeValue = Invoke-AdbText shell run-as $PackageName stat -c "%s" $RelativePath
        if ($LASTEXITCODE -eq 0 -and $sizeValue -match "^\d+$") { $size = $sizeValue }
        $shaValue = Invoke-AdbText shell run-as $PackageName sh -c "timeout $HashTimeoutSeconds sha256sum '$RelativePath'"
        if ($LASTEXITCODE -eq 0 -and $shaValue -match "^[a-fA-F0-9]{64}") {
            $sha = $Matches[0].ToLowerInvariant()
        } else {
            $warning = "Could not hash internal app file within $HashTimeoutSeconds seconds."
        }
    } else {
        $warning = "Could not read internal app file via run-as $PackageName. Install debug build and download model first."
    }
    return [pscustomobject]@{ Path = $DisplayPath; FileName = [System.IO.Path]::GetFileName($DisplayPath); ParentDirectory = Split-ParentPath $DisplayPath; SizeBytes = $size; Sha256 = $sha; Warning = $warning }
}

function Get-GpuPathObserved {
    param([string[]]$Lines)
    $joined = $Lines -join "`n"
    if ($joined -match "LITERT_CL|Loaded OpenCL library|Created OpenCL device") { return "OpenCL / LITERT_CL" }
    if ($joined -match "CreateSharedMemoryManager|OpenGL instead|LiteRT backend failed") { return "OpenGL fallback / failure" }
    if ($joined -match "backend: CPU") { return "CPU observed" }
    return "not observed in current logcat"
}

function New-MarkdownTableRow {
    param([string]$Item, [string]$Gallery, [string]$App)
    $safeGallery = $Gallery -replace "\|", "\|"
    $safeApp = $App -replace "\|", "\|"
    return "| $Item | $safeGallery | $safeApp |"
}

Assert-AdbReady
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputPath = Join-Path $OutputDir "litert-model-comparison-$timestamp.md"
$galleryModelPath = Get-GalleryModelPath
$galleryMetadata = Get-ExternalFileMetadata -Path $galleryModelPath
$appMetadata = Get-InternalAppFileMetadata -PackageName $AppPackage -DisplayPath $AppModelPath -RelativePath $AppModelRelativePath
$galleryVersion = Get-PackageVersion -PackageName $GalleryPackage
$appVersion = Get-PackageVersion -PackageName $AppPackage
$logs = Get-RecentLogLines
$galleryLogs = @($logs | Where-Object { $_ -match "AGLlmChatModelHelper|gemma4_2b|LITERT_CL|Loaded OpenCL|Created OpenCL|MainExecutorSettings|VisionExecutorSettings" })
$appLogs = @($logs | Where-Object { $_ -match "GemmaVisionEngine|gemma-4-E2B|CreateSharedMemoryManager|OpenGL instead|LiteRT backend" })
$galleryGpuPath = Get-GpuPathObserved -Lines $galleryLogs
$appGpuPath = Get-GpuPathObserved -Lines $appLogs

$result = "RESULT: unable to compare hashes. Preserve available metadata and capture readable files."
if ($galleryMetadata.Sha256 -match "^[a-f0-9]{64}$" -and $appMetadata.Sha256 -match "^[a-f0-9]{64}$") {
    if ($galleryMetadata.Sha256 -eq $appMetadata.Sha256) {
        $result = "RESULT: model files match. Investigate path/cache/runtime initialization differences."
    } else {
        $result = "RESULT: model files differ. Align model revision before changing EngineConfig."
    }
}

$content = New-Object System.Collections.Generic.List[string]
$content.Add("# LiteRT model comparison")
$content.Add("")
$content.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
$content.Add("Device serial: $(if ($DeviceSerial.Trim().Length -gt 0) { $DeviceSerial } else { 'default adb device' })")
$content.Add("")
$content.Add("## Comparison result")
$content.Add("")
$content.Add("| Item | Gallery | Gemma-4-Test |")
$content.Add("| --- | --- | --- |")
$content.Add((New-MarkdownTableRow "Model path" $galleryMetadata.Path $appMetadata.Path))
$content.Add((New-MarkdownTableRow "File name" $galleryMetadata.FileName $appMetadata.FileName))
$content.Add((New-MarkdownTableRow "Size bytes" $galleryMetadata.SizeBytes $appMetadata.SizeBytes))
$content.Add((New-MarkdownTableRow "SHA-256" $galleryMetadata.Sha256 $appMetadata.Sha256))
$content.Add((New-MarkdownTableRow "Parent directory" $galleryMetadata.ParentDirectory $appMetadata.ParentDirectory))
$content.Add((New-MarkdownTableRow "Storage location" "external app files" "internal app files"))
$content.Add((New-MarkdownTableRow "Package version info" $galleryVersion $appVersion))
$content.Add((New-MarkdownTableRow "GPU path observed" $galleryGpuPath $appGpuPath))
$content.Add("")
$content.Add($result)
$content.Add("")
$warnings = @($galleryMetadata.Warning, $appMetadata.Warning) | Where-Object { $_ -and $_.Trim().Length -gt 0 }
if ($warnings.Count -gt 0) {
    $content.Add("## Warnings"); $content.Add("")
    foreach ($warning in $warnings) { $content.Add("- $warning") }
    $content.Add("")
}
$content.Add("## Recent Gallery LiteRT/OpenCL log lines")
$content.Add(""); $content.Add('```text')
if ($galleryLogs.Count -gt 0) { $galleryLogs | ForEach-Object { $content.Add($_) } } else { $content.Add("No matching Gallery LiteRT/OpenCL log lines found.") }
$content.Add('```'); $content.Add("")
$content.Add("## Recent Gemma-4-Test LiteRT/OpenCL log lines")
$content.Add(""); $content.Add('```text')
if ($appLogs.Count -gt 0) { $appLogs | ForEach-Object { $content.Add($_) } } else { $content.Add("No matching Gemma-4-Test LiteRT/OpenCL log lines found.") }
$content.Add('```'); $content.Add("")
$content | Set-Content -Path $outputPath -Encoding UTF8

Write-Host "Wrote $outputPath"
Write-Host $result
if ($warnings.Count -gt 0) { Write-Warning ($warnings -join " ") }
