# Image preprocessing and Gemma 4 patch count

## Current preprocessing

The app preserves the captured image aspect ratio and scales only when needed:

- camera capture uses a file-backed `TakePicture()` result, not the low-resolution `TakePicturePreview()` bitmap,
- full-resolution JPEGs are sampled during decode before final scaling,
- no square letterboxing,
- no crop,
- long edge capped at `1024 px`,
- PNG bytes are sent to LiteRT-LM,
- single image only.

Examples:

```text
4000 x 3000 -> 1024 x 768
3000 x 4000 -> 768 x 1024
800 x 600 -> 800 x 600
```

This is based on S23+ GPU/OpenCL testing after optional OpenCL native libraries were declared. Letterboxing is no longer required for the working GPU path.

## Earlier CPU/XNNPack failure

Before GPU/OpenCL initialization was fixed, CPU/XNNPack attempts showed internal image expansion near the model patch limit:

```text
Resize image from 141x250 to 576x1056 which will result in 2376 patches
max_num_patches: 2520
DYNAMIC_UPDATE_SLICE failed to prepare
Failed to allocate tensors
```

The temporary `512 x 512` letterbox path was a conservative diagnostic workaround for that CPU failure mode. It should not be treated as the preferred GPU/OpenCL input format.

## Device smoke test

After installing an image-preprocessing change:

1. Capture one landscape photo and one portrait photo through the app camera button.
2. Confirm the UI shows an aspect-ratio-preserving prepared image, with the longer edge at `1024 px` for normal high-resolution camera photos.
3. If the UI shows very small dimensions such as `141 x 250`, the app is still receiving a preview bitmap instead of the full file-backed camera capture.
4. Run `GPU only`.
5. Check logcat for:

```text
LiteRT backend initialized: GPU text + GPU vision
LITERT_CL
Beschreibung abgeschlossen
```

6. Confirm the old GPU/OpenGL failure does not return:

```text
OpenCL not supported on this platform. Using OpenGL instead.
CreateSharedMemoryManager is not implemented.
```

## Guardrail

Do not reintroduce square letterboxing or raise the long-edge cap above `1024 px` without a Samsung S23+ GPU-only logcat comparison.
