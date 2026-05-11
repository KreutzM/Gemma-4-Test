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

Compares the LiteRT-LM model files and recent GPU/OpenCL logs between Google AI Edge Gallery and this app on a connected Android device.

Run from the repository root after both apps have downloaded/created their Gemma model files:

```powershell
.\scripts\compare-litert-model-files.ps1
```

Useful variants:

```powershell
# Use a specific device when multiple ADB devices are connected
.\scripts\compare-litert-model-files.ps1 -DeviceSerial <serial>

# Write summaries to a custom directory
.\scripts\compare-litert-model-files.ps1 -OutputDir .\logs\s23plus

# Override the expected Gallery file hint if logcat shows a different file name
.\scripts\compare-litert-model-files.ps1 -GalleryFileHint "gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm"
```

The script writes a timestamped Markdown summary with:

- device information,
- package metadata,
- Gallery `.litertlm` files visible under external app files,
- this app's model metadata through `run-as`,
- file sizes and SHA-256 hashes when available,
- recent LiteRT/OpenCL/GPU logcat lines.

Prerequisites:

- JDK 17 available to Gradle.
- Android SDK / Platform Tools installed.
- `adb` available in `PATH` for install/start.
- USB debugging enabled on the Android device.
- If the Gradle Wrapper is missing, `gradle` must be available in `PATH`, or the configured `-GradleUserHome` must contain an unpacked Gradle distribution under `wrapper\dists`.
