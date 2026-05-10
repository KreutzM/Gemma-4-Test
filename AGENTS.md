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
- Follow Google AI Edge Gallery as the reference architecture whenever possible.
- Do not invent a parallel runtime abstraction unless a device log proves Gallery's pattern cannot work.
- Initialize image support with a non-null `visionBackend`.
- Pass an explicit image capacity to LiteRT-LM when the API supports it. Device logs showing `max_num_images: 0` are a blocker for image-description work.
- Send image content before text content.
- Close `Conversation` and `Engine` deterministically.
- Keep the MVP single-image until S23+ inference is verified.
- Keep image preprocessing bounded; do not send full-resolution camera images into LiteRT-LM.
- For the current MVP, send a conservative `512 x 512` square letterboxed image to LiteRT-LM. Do not raise this size without a successful S23+ logcat smoke test.

## Gemma 4 / LiteRT-LM guardrails

Gemma 4 E2B vision is known to be difficult to run reliably outside Google AI Edge Gallery. Future Codex runs must preserve the following constraints unless a PR explicitly documents why they are changed:

- LiteRT-LM version is currently aligned with Gallery: `com.google.ai.edge.litertlm:litertlm-android:0.11.0`.
- Kotlin version was raised to `2.2.0` because LiteRT-LM 0.11.0 uses newer Kotlin metadata.
- Gallery's default vision accelerator is GPU; CPU is available in Gallery's backend mapping, but it must be treated as a diagnostic mode, not as a guaranteed vision fallback.
- On Samsung S23+, observed logs showed GPU registration, OpenCL unavailable, OpenGL fallback, `CreateSharedMemoryManager is not implemented`, `DYNAMIC_UPDATE_SLICE failed to prepare`, and `max_num_images: 0`.
- After `max_num_images` was fixed to `1`, S23+ CPU/XNNPack logs showed internal resize from `141x250` to `576x1056`, `2376 patches` near `max_num_patches: 2520`, followed by `DYNAMIC_UPDATE_SLICE failed to prepare` and tensor allocation failure.
- After 512x512 letterboxing, S23+ CPU/XNNPack logs still showed internal resize to `768x768`, `2304 patches`, and the same `DYNAMIC_UPDATE_SLICE`/tensor allocation failure. Do not assume smaller PNG dimensions alone will fix this.
- Gemma 4 vision uses a soft-token / patch-budget image processor. Before lowering or raising image size again, investigate whether LiteRT-LM exposes `max_soft_tokens`, `maxNumPatches`, image sequence length, or equivalent configuration.
- Public LiteRT-LM Kotlin `EngineConfig` currently exposes `maxNumTokens` and `maxNumImages`, but not an obvious `maxSoftTokens` setting. Treat this as an open API gap until proven otherwise.
- Gallery allowlist 1.0.13 lists Gemma-4-E2B-it with `topK=64`, `topP=0.95`, `temperature=1.0`, `maxTokens=4000`, `maxContextLength=32000`, `accelerators=gpu,cpu`, and `visionAccelerator=gpu`. Preserve or explicitly document deviations from these defaults.
- Gallery allowlist 1.0.13 includes a specific initial `commitHash` and an updated model file commit hash for `gemma-4-E2B-it.litertlm`. Avoid relying on unpinned `resolve/main` downloads for reproducible device debugging.
- Fixes should first remove configuration mismatches versus Gallery, especially model revision, image capacity, backend setup, sampler/config defaults, and vision-token budget, before changing the model or runtime.
- Do not switch back to MediaPipe LLM Inference or `.task` files for this target model.
- Do not add multi-image support until single-image inference succeeds on device.
- Do not hide inference failures. Surface the exact error in UI and preserve logcat-relevant context in docs.

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
