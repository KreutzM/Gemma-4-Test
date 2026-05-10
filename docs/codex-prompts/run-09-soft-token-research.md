# Codex Run 09: Gemma 4 vision soft-token research

You are working in `KreutzM/Gemma-4-Test`.

## Goal

Do not change runtime behavior yet. Research and document the correct next fix for the Samsung S23+ Gemma 4 LiteRT-LM vision failure.

## Current device facts

The app reaches LiteRT-LM vision inference and does not crash, but inference fails:

```text
MainExecutorSettings: backend: CPU
EncoderBackend: CPU
AdapterBackend: CPU
max_num_images: 1
Resize image from 512x512 to 768x768 which will result in 2304 patches
DYNAMIC_UPDATE_SLICE failed to prepare
Failed to allocate tensors
RunPrefillAsync status: INTERNAL
Failed to invoke the compiled model
```

Earlier logs also showed:

```text
max_num_patches: 2520
Resize image from 141x250 to 576x1056 which will result in 2376 patches
```

## Hypothesis

App-side PNG size is not the primary knob. Gemma 4's image processor appears to resize based on a soft-token / patch budget. The relevant next fix likely involves Gallery parity, model revision pinning, or a hidden/not-yet-used LiteRT-LM image budget setting.

## Required research

1. Inspect Google AI Edge Gallery for Gemma-4-E2B-it model defaults and model revision pins.
2. Inspect LiteRT-LM Kotlin API sources for any image/vision knobs:
   - `maxSoftTokens`
   - `softTokens`
   - `maxNumPatches`
   - `numPatches`
   - `imageSeq`
   - `imageSequence`
   - `visionConfig`
   - `imageProcessor`
   - `Gemma4DataProcessor`
3. Verify whether `EngineConfig` exposes only `maxNumImages` and `maxNumTokens` or whether another config path exists.
4. Compare this app's config to Gallery allowlist 1.0.13:
   - topK
   - topP
   - temperature
   - maxTokens
   - maxContextLength
   - accelerators
   - visionAccelerator
   - model commit hash / updated model file commit hash
5. Decide the next code PR:
   - Gallery parity config,
   - model revision pinning,
   - soft-token configuration if API exists,
   - or upstream issue prep if API does not exist.

## Scope guard

- Do not lower PNG size again unless LiteRT internal logs prove it changes patch count.
- Do not switch away from LiteRT-LM.
- Do not remove `visionBackend`.
- Do not add multi-image support.
- Do not hide errors.

## Validation

Docs-only changes still require:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace --no-daemon
```
