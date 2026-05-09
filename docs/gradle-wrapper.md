# Gradle Wrapper policy

The Android CI can bootstrap the Gradle Wrapper on the runner when the wrapper is missing or incomplete. This keeps the repository buildable while the initial skeleton is being stabilized.

## Preferred committed state

The repository should eventually contain the standard Gradle Wrapper files generated from a trusted local environment:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Recommended command:

```bash
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
```

Then run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace --no-daemon
```

## Why the wrapper JAR is not committed by this connector run

This ChatGPT GitHub connector can reliably create and update UTF-8 text files. It should not synthesize or paste a binary `gradle-wrapper.jar`. The next local Codex CLI run with shell access, or a developer workstation, should generate the wrapper and commit the exact Gradle-produced files.

## CI behavior until then

`.github/workflows/android-ci.yml` installs Gradle 8.10.2, generates the wrapper if it is missing or incomplete, and then uses `./gradlew` for tests, lint, and the debug build.
