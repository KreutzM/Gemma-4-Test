# Samsung S23+ LiteRT-LM vision debugging

## Observed logcat failure

During a device smoke test on Samsung S23+, the app loaded the model and captured an image, but image description failed without an app crash.

Relevant logcat details:

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

## Interpretation

- The app did not crash; LiteRT-LM initialization/inference failed.
- The GPU delegate path fell back from OpenCL to OpenGL and then failed in shared-memory setup.
- The reported `max_num_images: 0` is suspicious for a vision request and can prevent image support from being configured as intended.
- Google AI Edge Gallery defaults the vision accelerator to GPU, but it maps CPU as an available backend option in its runtime code. CPU is therefore useful as an explicit diagnostic mode, not as an assumed universal fix.

## Current fix direction

- Keep the Gallery-aligned default backend as GPU text + GPU vision.
- Explicitly set `maxImages = 1` in `GemmaInferenceConfig` and pass it into `EngineConfig` as `maxNumImages`.
- Add an explicit `GemmaBackendMode` so CPU/CPU can be tested intentionally if GPU/GPU continues to fail on a device.

## Next device test

1. Install the APK built from the fix PR.
2. Confirm logcat no longer reports `max_num_images: 0`.
3. Test default `GpuTextGpuVision` mode.
4. If GPU still fails, expose or temporarily force `CpuTextCpuVision` and capture logcat again.

## Open questions

- Whether Gemma-4 E2B vision works reliably on S23+ GPU/OpenGL with LiteRT-LM 0.11.0.
- Whether CPU/CPU vision is practically supported for this model on this device or only useful as a diagnostic fallback.
- Whether a newer LiteRT-LM version fixes the OpenGL shared-memory failure.
