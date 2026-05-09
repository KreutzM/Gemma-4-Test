# Codex Run 01b: finalize Gradle Wrapper and CI baseline

You are working in `KreutzM/Gemma-4-Test` on branch `codex/run-01b-gradle-wrapper` or a fresh branch from updated `main`.

## Goal

Finalize the Gradle Wrapper and make Android CI green without implementing app features.

## Context

Read first:

- `AGENTS.md`
- `docs/gradle-wrapper.md`
- `docs/quality.md`
- `.github/workflows/android-ci.yml`

## Tasks

1. Generate and commit the standard Gradle Wrapper files:

   ```bash
   gradle wrapper --gradle-version 8.10.2 --distribution-type bin
   ```

2. Run:

   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace --no-daemon
   ```

3. Fix only build, lint, or resource issues found by those commands.
4. Add ktlint only if the baseline build is already green and the plugin can be configured without widening scope.
5. Do not implement model download, camera capture, or LiteRT-LM inference.

## Expected handoff summary

Return:

- commands run and whether they passed,
- changed files,
- CI status or local build status,
- any follow-up required before Run 02.
