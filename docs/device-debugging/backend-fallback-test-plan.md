# LiteRT-LM backend fallback test plan

## Context

After fixing `max_num_images` from `0` to `1`, Samsung S23+ still failed on the default GPU path:

```text
MainExecutorSettings: backend: GPU
VisionExecutorSettings:
  EncoderBackend: GPU
  AdapterBackend: CPU
OpenCL not supported on this platform. Using OpenGL instead.
CreateSharedMemoryManager is not implemented.
DYNAMIC_UPDATE_SLICE failed to prepare.
STABLEHLO_COMPOSITE failed to prepare.
```

The app should now try the Gallery-aligned default first:

```text
GPU text + GPU vision
```

If initialization fails, it closes any partial LiteRT resources and retries:

```text
CPU text + CPU vision
```

CPU/CPU remains a diagnostic fallback. It is not assumed to be a guaranteed working vision mode.

## Device smoke test

1. Install the APK from this PR.
2. Open the app.
3. Confirm model file is already present or download it.
4. Capture one image.
5. Tap `Bild beschreiben`.
6. Watch the UI card for `Backend:`.
7. Capture logcat around initialization and inference.

## Expected useful outcomes

### Outcome A: GPU works

UI shows:

```text
Backend: GPU text + GPU vision
```

No fallback is needed. Keep this mode.

### Outcome B: GPU fails, CPU initializes and inference works

UI shows:

```text
Backend: CPU text + CPU vision
```

Document logcat. Then consider exposing backend selection for repeatability.

### Outcome C: GPU fails and CPU also fails

UI shows initialization failure. Document both backend attempts from logcat. Next investigation should compare Gallery runtime settings more deeply, including whether Gallery is using additional model/task metadata or experimental flags.

## Logcat strings to search

```text
MainExecutorSettings
VisionExecutorSettings
max_num_images
OpenCL not supported
CreateSharedMemoryManager
DYNAMIC_UPDATE_SLICE
STABLEHLO_COMPOSITE
backend: GPU
backend: CPU
```
