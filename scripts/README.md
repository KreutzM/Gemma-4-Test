# Local Windows scripts

## `run-local-device.ps1`

Runs the local Android CI-equivalent tasks on Windows and optionally installs/starts the debug APK on a connected Android device via ADB.

Run from the repository root:

```powershell
.\scripts\run-local-device.ps1
```

Default behavior:

1. ensures the Gradle Wrapper exists,
2. runs unit tests,
3. runs Android lint,
4. builds the debug APK,
5. installs the APK via ADB,
6. starts `de.kreutzm.gemma4test/.MainActivity`.

Useful variants:

```powershell
# Clean build, then install and start
.\scripts\run-local-device.ps1 -Clean

# Build/test/lint only, no device install
.\scripts\run-local-device.ps1 -NoInstall

# Install existing APK without rebuilding
.\scripts\run-local-device.ps1 -NoBuild

# Use a specific device when multiple ADB devices are connected
.\scripts\run-local-device.ps1 -DeviceSerial <serial>

# Use a custom Gradle user home/cache
.\scripts\run-local-device.ps1 -GradleUserHome D:\GradleHome-Codex

# Skip local unit tests or lint for quick manual smoke tests
.\scripts\run-local-device.ps1 -SkipTests -SkipLint
```

## `compare-litert-model-files.ps1`

Compares the Google AI Edge Gallery Gemma-4-E2B-it `.litertlm` file with this app's downloaded model and writes a Markdown report under `logs/`.

Run after one Gallery Ask Image inference and one Gemma-4-Test GPU-only attempt:

```powershell
.\scripts\compare-litert-model-files.ps1
```

Useful variants:

```powershell
# Use a specific device when multiple ADB devices are connected
.\scripts\compare-litert-model-files.ps1 -DeviceSerial <serial>

# Write summaries to a custom directory
.\scripts\compare-litert-model-files.ps1 -OutputDir .\logs\s23plus

# Bound on-device hashing time for multi-GB files
.\scripts\compare-litert-model-files.ps1 -HashTimeoutSeconds 60
```

The generated `logs/litert-model-comparison-*.md` report includes:

- model paths,
- file names,
- sizes,
- SHA-256 values when readable,
- storage location,
- package versions,
- recent LiteRT/OpenCL log lines.

Prerequisites:

- JDK 17 available to Gradle.
- Android SDK / Platform Tools installed.
- `adb` available in `PATH` for install/start.
- USB debugging enabled on the Android device.
- If the Gradle Wrapper is missing, `gradle` must be available in `PATH`, or the configured `-GradleUserHome` must contain an unpacked Gradle distribution under `wrapper\dists`.
