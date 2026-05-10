# Gemma 4 vision soft-token / patch-budget debugging

## Why this document exists

Samsung S23+ device tests show that the app now reaches the LiteRT-LM vision path, but inference still fails during tensor preparation:

```text
DYNAMIC_UPDATE_SLICE failed to prepare
Failed to allocate tensors
RunPrefillAsync status: INTERNAL
Failed to invoke the compiled model
Receive callback OnError
```

The failure persists after fixing `max_num_images` and after adding CPU fallback.

## Device observations so far

### Before `maxNumImages = 1`

```text
MainExecutorSettings: backend: GPU
VisionExecutorSettings:
  EncoderBackend: GPU
  AdapterBackend: CPU
max_num_images: 0
OpenCL not supported on this platform. Using OpenGL instead.
CreateSharedMemoryManager is not implemented.
DYNAMIC_UPDATE_SLICE failed to prepare.
```

### After explicit `maxNumImages = 1` and CPU fallback

```text
MainExecutorSettings: backend: CPU
EncoderBackend: CPU
AdapterBackend: CPU
max_num_images: 1
Resize image from 141x250 to 576x1056 which will result in 2376 patches
max_num_patches: 2520
DYNAMIC_UPDATE_SLICE failed to prepare
Failed to allocate tensors
```

### After 512x512 square letterbox

```text
MainExecutorSettings: backend: CPU
EncoderBackend: CPU
AdapterBackend: CPU
max_num_images: 1
Resize image from 512x512 to 768x768 which will result in 2304 patches
DYNAMIC_UPDATE_SLICE failed to prepare
Failed to allocate tensors
```

This shows that reducing or normalizing app-side PNG dimensions does not necessarily reduce LiteRT-LM's internal vision size. The runtime/model image processor can still upscale to a target patch budget.

## Relevant public information

### Gemma 4 vision patch budget

Gemma 4 vision uses a soft-token / patch-budget image preprocessing scheme. Public documentation describes allowed soft-token counts such as 70, 140, 280, 560, and 1120. The default 280 soft tokens corresponds to 2520 image patches and roughly 645k pixels. This matches the device logs showing `max_num_patches: 2520` and internal sizes like `768x768`.

Reference:

- https://huggingface.co/docs/transformers/model_doc/gemma4

### Keras image converter behavior

Keras Hub's Gemma4 image converter documents `max_soft_tokens`. It preserves aspect ratio and chooses valid target resolutions based on soft-token budget. This supports the interpretation that app-side input resolution is not the main control knob once LiteRT-LM's Gemma 4 image processor takes over.

Reference:

- https://keras.io/keras_hub/api/models/gemma4/gemma4_image_converter/

### LiteRT-LM Kotlin API surface

The public LiteRT-LM Kotlin `EngineConfig` exposes:

- `modelPath`
- `backend`
- `visionBackend`
- `audioBackend`
- `maxNumTokens`
- `maxNumImages`
- `cacheDir`

It does not expose an obvious `maxSoftTokens`, `maxNumPatches`, or image sequence length parameter in the inspected source.

Reference:

- https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/java/com/google/ai/edge/litertlm/Config.kt

### Google AI Edge Gallery allowlist

Gallery allowlist `1_0_13.json` lists `Gemma-4-E2B-it` with:

```json
{
  "modelId": "litert-community/gemma-4-E2B-it-litert-lm",
  "modelFile": "gemma-4-E2B-it.litertlm",
  "sizeInBytes": 2588147712,
  "minDeviceMemoryInGb": 8,
  "llmSupportImage": true,
  "llmSupportAudio": true,
  "defaultConfig": {
    "topK": 64,
    "topP": 0.95,
    "temperature": 1.0,
    "maxContextLength": 32000,
    "maxTokens": 4000,
    "accelerators": "gpu,cpu",
    "visionAccelerator": "gpu"
  }
}
```

It also lists both an initial `commitHash` and an `updatableModelFiles` commit hash for the same model file. Device debugging should avoid unpinned model downloads.

Reference:

- https://github.com/google-ai-edge/gallery/blob/main/model_allowlists/1_0_13.json

## Working hypothesis

The current failure is likely caused by the Gemma 4 vision processor and/or LiteRT tensor allocation at the default 280-soft-token / 2520-patch budget, not by the raw app-side PNG dimensions alone.

The best next fix is not to keep shrinking the PNG. Instead, find how Gallery or LiteRT-LM configures one of:

- `max_soft_tokens`
- image sequence length
- max image patches
- vision processor target size
- model metadata that encodes the above
- model variant / revision that changes the above

## Recommended next PRs

### PR A: Gallery parity config

Mirror Gallery's allowlist defaults exactly:

- `topK = 64`
- `temperature = 1.0`
- `maxTokens = 4000`
- `maxContextLength = 32000` if LiteRT-LM API permits it
- default backend GPU/GPU first, CPU fallback only as diagnostic
- pin model download to Gallery's known model file commit hash

This does not directly expose soft-token budget, but it removes obvious config drift.

### PR B: Runtime/API inspection

Add a small checked-in research artifact or local script that records the LiteRT-LM Android API surface from dependency sources/JAR metadata, looking specifically for image/vision knobs.

Search terms:

```text
maxSoftTokens
softTokens
maxNumPatches
numPatches
imageSeq
imageSequence
visionConfig
imageProcessor
Gemma4DataProcessor
```

### PR C: Upstream issue preparation

If no public Android API exposes the vision budget, prepare a minimal upstream issue with:

- device: Samsung S23+
- Android version
- model file revision / hash
- LiteRT-LM version
- exact `EngineConfig`
- logcat excerpts showing `max_num_images: 1`, CPU/CPU backend, `768x768`, `2304 patches`, and `DYNAMIC_UPDATE_SLICE failed to prepare`
- confirmation that Google AI Edge Gallery works on the same device with the same model

## Anti-fixes

Avoid these unless a PR explicitly proves them useful:

- repeatedly shrinking PNG dimensions without checking LiteRT's internal resize logs,
- changing to MediaPipe `.task`,
- removing `visionBackend`,
- increasing image size above 512,
- assuming CPU fallback is a real fix,
- relying on `resolve/main` model downloads for debugging reproducibility.
