# Gallery backend comparison on Samsung S23+

Date: 2026-05-11  
Device: Samsung S23+ over ADB (`192.168.178.61:34071`)  
Gallery package: `com.google.ai.edge.gallery`  
Gallery activity: `com.google.ai.edge.gallery/.MainActivity`

## Goal

Compare this app's LiteRT-LM backend behavior with Google AI Edge Gallery on the same S23+. The key question was whether Gallery actually initializes GPU for Gemma-4-E2B-it image analysis or silently uses CPU/fallback.

## Our app result

This app attempted GPU text + GPU vision and failed during GPU delegate setup:

```text
I/GemmaVisionEngine: Trying LiteRT backend: GPU text + GPU vision
I/native:   MainExecutorSettings: backend: GPU
I/native:   VisionExecutorSettings: VisionExecutorSettings:
I/native:   EncoderBackend: GPU
I/native:   AdapterBackend: CPU
I/litert: OpenCL not supported on this platform. Using OpenGL instead.
E/native: Failed to create litert::ml_drift::DelegateKernelLiteRt:
UNIMPLEMENTED: CreateSharedMemoryManager is not implemented.
E/GemmaVisionEngine: LiteRT backend failed: GPU text + GPU vision
E/GemmaVisionEngine: com.google.ai.edge.litertlm.LiteRtLmJniException:
Failed to create engine: INTERNAL
```

Interpretation:

- Our app requested GPU for text and vision.
- LiteRT-LM did not create an OpenCL path for this process.
- It fell back internally to OpenGL.
- OpenGL delegate creation failed at `CreateSharedMemoryManager`.
- In the captured run, no CPU fallback logs appeared afterward, which matches a likely `GPU only` policy test.

## Gallery result

Gallery was installed and already configured for:

```text
Ask Image
Current model is Gemma-4-E2B-it
Downloaded
```

During image inference, Gallery initialized GPU successfully:

```text
I/native:   MainExecutorSettings: backend: GPU
I/native: model_assets: model_path:
/storage/emulated/0/Android/data/com.google.ai.edge.gallery/files/Gemma_4_E2B_it/20260325/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
I/native: enable_speculative_decoding: false
I/native:   VisionExecutorSettings: VisionExecutorSettings:
I/native:   EncoderBackend: GPU
I/native:   AdapterBackend: CPU
I/tflite: Loaded OpenCL library with dlopen.
I/litert: Created OpenCL device from provided device id and platform id.
I/litert: Reusing provided EGL environment.
I/litert: Created LiteRT GpuEnvironment.
I/tflite: Replacing 2068 out of 2068 node(s) with delegate (LITERT_CL) node
I/native: Initializing OpenCL-based API from serialized data.
I/tflite: Replacing 2245 out of 2245 node(s) with delegate (LITERT_CL) node
I/native: Creating Gemma4DataProcessor
```

Gallery also logged:

```text
I/native: The Main backend constraint is not set.
I/native: The Vision backend constraint is not set.
I/native: enable_speculative_decoding: false
W/native: OpenCL sampler not available, falling back to statically linked C API
```

Interpretation:

- Gallery really used GPU/OpenCL on the same S23+.
- Gallery did not silently fall back to CPU for the main model or vision encoder.
- Gallery's vision adapter remained CPU, which matches the model section constraint and our observed `AdapterBackend: CPU`.
- Gallery did not enable speculative decoding in this run.
- Gallery loaded OpenCL successfully; our app did not.

## Important difference to investigate

Gallery's local file path was:

```text
Gemma_4_E2B_it/20260325/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
```

Our app currently downloads:

```text
gemma-4-E2B-it.litertlm
```

The Gallery UI labels both as `Gemma-4-E2B-it`, but the on-device Gallery file name and directory indicate a dated Gallery-packaged model asset. Before changing runtime code, verify whether the binary content, metadata, or bundled GPU cache behavior differs from the pinned Hugging Face file used by this app.

## Next debugging direction

The failure is no longer "S23+ cannot run Gemma-4-E2B-it GPU." Gallery proves it can. The likely gap is one of:

- Gallery uses a different packaged `.litertlm` binary or revision than this app.
- Gallery provides or reuses an OpenCL/EGL/GPU environment option that this app does not.
- Gallery has model files or cache artifacts under its app-private external directory that avoid the OpenGL fallback path.
- Gallery initialization path sets LiteRT-LM options that are not exposed by this app's current `EngineConfig` usage.

## Repro commands

Start Gallery:

```powershell
adb shell am start -n com.google.ai.edge.gallery/.MainActivity
```

Find Gallery PID:

```powershell
adb shell pidof com.google.ai.edge.gallery
```

Filter Gallery backend logs:

```powershell
adb logcat -d -v time --pid=<gallery-pid> |
  Select-String -Pattern 'MainExecutorSettings|VisionExecutorSettings|backend: GPU|backend: CPU|OpenCL|OpenGL|CreateSharedMemoryManager|EncoderBackend|AdapterBackend|LiteRT|litert|litertlm|Gemma|gemma|Capabilities|Speculative|ExperimentalFlags|Failed|Exception|Error|backend'
```

Filter this app's backend logs:

```powershell
adb logcat -d -v time --pid=<app-pid> |
  Select-String -Pattern 'GemmaVisionEngine|Trying LiteRT backend|LiteRT backend failed|LiteRT backend initialized|MainExecutorSettings|VisionExecutorSettings|OpenCL|OpenGL|CreateSharedMemoryManager|EncoderBackend|AdapterBackend|Failed|Exception|Error'
```
