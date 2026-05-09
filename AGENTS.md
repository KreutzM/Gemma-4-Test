# AGENTS.md

Guidance for Codex and other coding agents working in this repository.

## Project goal

Build a minimal Android app that downloads Gemma 4 E2B and then performs local/offline image description on a Samsung S23+.

## Hard constraints

- Do not commit model binaries, APK outputs, personal access tokens, Hugging Face tokens, local signing keys, or Android Studio user files.
- Keep the first MVP single-image and conservative on memory.
- Do not add a backend server. After the model is downloaded, image inference must be local/offline.
- Prefer small, reviewable commits.
- Keep CI green: unit tests, Android lint, and debug build.
- Document any dependency version chosen for LiteRT-LM with a source and date.

## Android conventions

- Kotlin and Jetpack Compose.
- App package: `de.kreutzm.gemma4test`.
- Minimum SDK: 26.
- Compile/target SDK: use the latest stable value configured in Gradle unless CI requires adjustment.
- Use app-private storage for model files.
- Run long operations in coroutines off the main thread.
- All user-visible text in the MVP may be German.

## Runtime conventions

- Target model: `gemma-4-E2B-it.litertlm` from `litert-community/gemma-4-E2B-it-litert-lm`.
- Use LiteRT-LM, not MediaPipe `.task` inference.
- Initialize image support with a non-null `visionBackend`.
- Send image content before text content.
- Close `Conversation` and `Engine` deterministically.
- Prefer GPU first for S23+, but keep CPU fallback and clear diagnostics.

## Quality gates

Run before handing off:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

If ktlint/detekt are configured, run those too. If a task cannot be run locally, state exactly why and what command should be run in CI.

## Communication

When opening a PR or producing a summary, include:

- what changed,
- commands run,
- known risks,
- next recommended step.
