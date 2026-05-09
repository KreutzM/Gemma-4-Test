# Codex Run 01: make Android skeleton build green

You are working in `KreutzM/Gemma-4-Test`.

## Goal

Make the committed Android MVP skeleton build, test, lint, and assemble reliably in CI.

## Context

Read first:

- `AGENTS.md`
- `README.md`
- `docs/implementation-plan.md`
- `docs/research/google-ai-edge-gallery.md`
- `.github/workflows/android-ci.yml`

## Tasks

1. Inspect the Gradle project and fix any build issues.
2. Generate and commit a Gradle Wrapper if missing, using a Gradle version compatible with Android Gradle Plugin 8.8.2.
3. Add any missing Android resources needed for the manifest, such as launcher icons or safer placeholder resources.
4. Add a minimal unit test for `GemmaModelConfig`.
5. Add ktlint or a comparable Kotlin formatting gate only if it can be configured quickly without destabilizing CI. If added, wire it into CI.
6. Run:

   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace
   ```

7. Commit all changes in one or more small commits.

## Constraints

- Do not implement model download or LiteRT-LM inference in this run.
- Do not add model binaries or APK outputs.
- Do not change the app package unless required to fix a build issue.
- Keep changes minimal and focused on build/quality infrastructure.

## Expected handoff summary

Return:

- commands run and result,
- files changed,
- any unresolved warnings,
- next recommended Codex prompt/run.
