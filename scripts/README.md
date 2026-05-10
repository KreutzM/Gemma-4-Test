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

# Skip local unit tests or lint for quick manual smoke tests
.\scripts\run-local-device.ps1 -SkipTests -SkipLint
```

Prerequisites:

- JDK 17 available to Gradle.
- Android SDK / Platform Tools installed.
- `adb` available in `PATH` for install/start.
- USB debugging enabled on the Android device.
- If the Gradle Wrapper is missing, `gradle` must be available in `PATH` so the script can generate it.
