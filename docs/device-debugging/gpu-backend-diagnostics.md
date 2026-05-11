# GPU backend diagnostics

## Current state

Samsung S23+ tests showed that the app can run Gemma-4-E2B-it LiteRT-LM image description through CPU/XNNPack, but CPU inference is slow. Earlier GPU attempts failed during LiteRT-LM delegate setup with this signature:

```text
OpenCL not supported on this platform. Using OpenGL instead.
Failed to create litert::ml_drift::DelegateKernelLiteRt:
UNIMPLEMENTED: CreateSharedMemoryManager is not implemented.
Failed to initialize kernel.
```

Later successful CPU initialization logs included:

```text
MainExecutorSettings: backend: CPU
EncoderBackend: CPU
AdapterBackend: CPU
max_num_images: 1
```

This does not prove Google AI Edge Gallery uses GPU on the same S23+. Gallery may initialize GPU, or it may also fall back to CPU. The useful next test is a direct logcat comparison.

## App controls

The debug UI exposes three backend policies:

- `GPU only`: tries `GPU text + GPU vision` and surfaces the initialization error if it fails.
- `CPU only`: tries `CPU text + CPU vision`.
- `GPU then CPU fallback`: tries GPU first, then CPU if GPU initialization fails.

The default remains `GPU then CPU fallback` so the app stays usable during device tests.

The UI shows both:

```text
Backend: GPU text + GPU vision
Policy: GPU only
```

or:

```text
Backend: CPU text + CPU vision
Policy: GPU then CPU fallback
```

## Logcat strings

Search for these strings when testing:

```text
GemmaVisionEngine
Trying LiteRT backend
LiteRT backend failed
LiteRT backend initialized
MainExecutorSettings
VisionExecutorSettings
OpenCL not supported
CreateSharedMemoryManager
backend: GPU
backend: CPU
```

The app emits stable logs through `GemmaVisionEngine`:

```text
Trying LiteRT backend: GPU text + GPU vision
LiteRT backend failed: GPU text + GPU vision
Trying LiteRT backend: CPU text + CPU vision
LiteRT backend initialized: CPU text + CPU vision
LiteRT backend initialized: GPU text + GPU vision
```

Failures are logged with the full throwable so logcat contains the exception message and stacktrace.

## Controlled S23+ tests

1. Install the app and load the already pinned `gemma-4-E2B-it.litertlm` model.
2. Test `GPU only`.
   - If GPU initializes, capture latency and logcat with `Backend: GPU text + GPU vision`.
   - If GPU fails, capture `LiteRT backend failed: GPU text + GPU vision` and the stacktrace.
3. Test `CPU only`.
   - Confirm the UI shows `Backend: CPU text + CPU vision`.
   - Capture latency and `MainExecutorSettings` / `EncoderBackend` / `AdapterBackend`.
4. Test `GPU then CPU fallback`.
   - If GPU fails, confirm logcat shows GPU failure and CPU success.
   - Confirm the UI shows the final active backend.
5. Open Google AI Edge Gallery with the same model and capture logcat for:
   - `Preferred backend`
   - `MainExecutorSettings`
   - `VisionExecutorSettings`
   - `backend: GPU`
   - `backend: CPU`
   - `OpenCL`
   - `OpenGL`
   - `CreateSharedMemoryManager`

The key question is whether Gallery actually initializes GPU on this S23+, or whether it also uses CPU/fallback.

## 2026-05-11 Gallery comparison result

Gallery did initialize GPU/OpenCL successfully on the same S23+ for `Gemma-4-E2B-it` Ask Image. It logged `MainExecutorSettings: backend: GPU`, `EncoderBackend: GPU`, `AdapterBackend: CPU`, `Loaded OpenCL library with dlopen`, and `Replacing ... node(s) with delegate (LITERT_CL)`.

This app still failed GPU initialization with `OpenCL not supported on this platform. Using OpenGL instead` followed by `CreateSharedMemoryManager is not implemented`.

See `docs/device-debugging/gallery-backend-comparison-2026-05-11.md` for the full ADB/logcat comparison. The next investigation should focus on why Gallery obtains the OpenCL path while this app falls into OpenGL, and whether Gallery's dated local model file differs from this app's pinned Hugging Face file.

The locally pulled Play Store Gallery file did differ from this app's pinned Hugging Face artifact:

```text
Gallery file: gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
Gallery size: 2,538,766,336 bytes
Gallery sha256: 02957360dbcd67bf4bd629271fa8f9ab318ad6d1c10593bae5e9900611669bc0
App file: gemma-4-E2B-it.litertlm
App expected size: 2,583,085,056 bytes
```

The public Gallery allowlist has not yet revealed a reproducible URL for the observed Play Store file. See `docs/research/gallery-model-revision.md` before changing model download configuration.

## Speculative decoding note

Gallery checks model capabilities with `Capabilities(modelPath).hasSpeculativeDecodingSupport()` and can set `ExperimentalFlags.enableSpeculativeDecoding` when supported. This app does not enable speculative decoding by default yet. A later PR should add an explicit toggle only after the LiteRT-LM Kotlin API usage is compile-safe in this project.
