# Samsung S23+ GPU/OpenCL success

## Result

After adding optional OpenCL/vendor GPU native-library declarations to the Android manifest, the app successfully initialized Gemma 4 E2B image description with GPU/OpenCL on Samsung S23+.

Confirmed logcat lines:

```text
backendPolicy=GPU only
Trying LiteRT backend: GPU text + GPU vision
MainExecutorSettings: backend: GPU
EncoderBackend: GPU
AdapterBackend: CPU
Loaded OpenCL library with dlopen.
Created OpenCL device from provided device id and platform id.
Replacing ... with delegate (LITERT_CL)
LiteRT backend initialized: GPU text + GPU vision
Backend: GPU text + GPU vision
Beschreibung abgeschlossen
```

The previous failure path is gone in the successful run:

```text
OpenCL not supported on this platform. Using OpenGL instead.
Failed to load OpenCL library with dlopen: libvndksupport.so not found
Replacing ... with delegate (LITERT_OPENGL)
CreateSharedMemoryManager is not implemented
```

## Root cause

The model file was not the root cause. A local test with the copied Play Store Gallery model file still failed before the manifest fix.

The confirmed fix was declaring optional native GPU/OpenCL libraries in `AndroidManifest.xml` so Android exposes the vendor OpenCL libraries to the app process on Android 12+ devices.

## Remaining note

LiteRT-LM still logged:

```text
GPU sampler unavailable. Falling back to CPU sampling.
```

This is not a blocker for the MVP because the model and vision encoder initialized with GPU/OpenCL and image description completed. It may be investigated later as a performance optimization.

## Compatibility note

PR #26 introduced model `variantId` metadata. Older installed model metadata files may not contain `variantId`. The app now accepts legacy metadata when the stable fields still match and rewrites the metadata in the current format.

## Regression test checklist

After future runtime, manifest, model, or dependency changes:

1. Install debug APK on Samsung S23+.
2. Select `GPU only`.
3. Run one image-description request.
4. Confirm logcat contains `LITERT_CL` and `LiteRT backend initialized: GPU text + GPU vision`.
5. Confirm logcat does not contain `LITERT_OPENGL` or `CreateSharedMemoryManager is not implemented` for the active run.
