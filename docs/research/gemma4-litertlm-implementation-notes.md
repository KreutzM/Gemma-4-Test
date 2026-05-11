# Gemma 4 E2B LiteRT-LM implementation notes

These notes are guardrails for future Codex/fix runs. Gemma 4 vision is fragile outside Google AI Edge Gallery, so changes should stay close to Gallery unless a device log proves a different path is required.

## Reference implementation

Google AI Edge Gallery is the reference app for this repository.

Relevant Gallery patterns:

- Runtime artifact: `com.google.ai.edge.litertlm:litertlm-android:0.11.0`.
- Runtime type: LiteRT-LM, not MediaPipe `.task` inference.
- `EngineConfig` receives:
  - `modelPath`,
  - text `backend`,
  - non-null `visionBackend` for image input,
  - `maxNumTokens`,
  - cache dir.
- Gallery's default vision accelerator is GPU.
- Gallery maps the vision accelerator to CPU/GPU/NPU backends, so CPU exists as a configurable backend. It should not be assumed to be a guaranteed fallback for Gemma 4 vision on every device.
- Image content is appended before text content:
  - `Content.ImageBytes(...)`,
  - then `Content.Text(...)`.
- `Conversation` and `Engine` are explicitly closed.

## Current target model

- Name: `Gemma-4-E2B-it`
- Repo: `litert-community/gemma-4-E2B-it-litert-lm`
- File: `gemma-4-E2B-it.litertlm`
- Gallery allowlist size: `2,588,147,712` bytes
- Pinned updated model file size from Hugging Face/Xet headers on 2026-05-10: `2,583,085,056` bytes
- Target device: Samsung S23+

## Known device failure from Samsung S23+

The first device smoke test loaded the model and captured an image but failed during LiteRT-LM inference. The app did not crash.

Important logcat excerpts:

```text
LiteRT GPU registered
OpenCL not supported on this platform. Using OpenGL instead.
Failed to create litert::ml_drift::DelegateKernelLiteRt:
UNIMPLEMENTED: CreateSharedMemoryManager is not implemented.
Failed to initialize kernel.
Restored original execution plan after delegate application failure.
DYNAMIC_UPDATE_SLICE failed to prepare.
STABLEHLO_COMPOSITE failed to prepare.

MainExecutorSettings: backend: GPU
VisionExecutorSettings:
  EncoderBackend: GPU
  AdapterBackend: CPU
max_num_images: 0
```

## Interpretation and priorities

1. `max_num_images: 0` is suspicious for a vision request and must be fixed/verified before switching runtimes or models.
2. The GPU path failed through OpenGL fallback, but that does not prove CPU vision is unsupported or supported. It only proves this exact GPU/OpenGL path failed on the tested device/runtime combination.
3. CPU backend exists in Gallery's backend mapping, but CPU/CPU should be treated as a diagnostic mode until device logs prove it works.
4. Do not remove GPU/GPU default merely because the first smoke test failed. Gallery defaults vision to GPU.
5. Do not add multi-image support while single-image support is unresolved.

## Fix-run checklist

Before changing inference code, verify:

- Does the change keep LiteRT-LM, not MediaPipe `.task`?
- Does `EngineConfig` still set a non-null `visionBackend`?
- Does `EngineConfig` explicitly allow at least one image when the API exposes image count/capacity?
- Are image bytes still sent before text?
- Is the model file path app-private and the exact `.litertlm` file?
- Are `Conversation` and `Engine` closed on success and failure?
- Is the UI surfacing errors instead of swallowing them?
- Is any CPU fallback described as diagnostic unless validated on S23+?

## Device smoke-test checklist

After every LiteRT-LM fix PR:

1. Install debug APK on Samsung S23+.
2. Confirm the downloaded model file exists and has the expected byte count.
3. Capture a single photo.
4. Run image description.
5. Check logcat for:
   - backend settings,
   - vision settings,
   - `max_num_images`,
   - delegate/kernel failures,
   - `DYNAMIC_UPDATE_SLICE` or `STABLEHLO_COMPOSITE` failures.
6. Document the exact log excerpts in `docs/device-debugging/`.

## Anti-patterns

Avoid these unless a PR explains why:

- Switching to MediaPipe `.task` inference.
- Treating CPU fallback as guaranteed.
- Sending text before image.
- Sending full-resolution camera images.
- Keeping the engine alive without deterministic cleanup in the MVP.
- Adding multi-image support before one-image inference works.
- Silently changing LiteRT-LM/Kotlin versions without linking to Gallery or official release notes.
