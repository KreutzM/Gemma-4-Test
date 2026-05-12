# Samsung S23+ known-good Gemma 4 E2B baseline

Use this as the starting point for future Gemma 4 E2B Android apps.

## Working configuration

- Device: Samsung S23+ with 8 GB RAM.
- Runtime: LiteRT-LM Android `0.11.0`.
- Model: `litert-community/gemma-4-E2B-it-litert-lm`, file `gemma-4-E2B-it.litertlm`.
- Model size: `2,583,085,056` bytes.
- Backend policy for validation: `GPU only`.
- Active backend: `GPU text + GPU vision`.
- Vision capacity: `max_num_images: 1`.
- Sampling defaults aligned with Gallery: `topK=64`, `topP=0.95`, `temperature=1.0`, `maxTokens=4000`.
- Camera input: file-backed `TakePicture()` through `FileProvider`.
- Image preprocessing: preserve aspect ratio, no crop, no letterbox, long edge capped at `1024 px`, PNG bytes sent to LiteRT-LM.

## Required Android manifest detail

Keep the optional native-library declarations:

```xml
<uses-native-library android:name="libOpenCL.so" android:required="false" />
<uses-native-library android:name="libOpenCL-pixel.so" android:required="false" />
<uses-native-library android:name="libGLES_mali.so" android:required="false" />
<uses-native-library android:name="libPVROCL.so" android:required="false" />
```

Without these declarations, this app previously fell back from OpenCL to OpenGL and failed with:

```text
OpenCL not supported on this platform. Using OpenGL instead.
Failed to load OpenCL library with dlopen: libvndksupport.so not found
LITERT_OPENGL
CreateSharedMemoryManager is not implemented
LiteRT backend failed: GPU text + GPU vision
```

## Known-good logcat signature

Search for:

```text
GemmaVisionEngine
Trying LiteRT backend: GPU text + GPU vision
MainExecutorSettings: backend: GPU
EncoderBackend: GPU
AdapterBackend: CPU
Loaded OpenCL library with dlopen.
Created OpenCL device from provided device id and platform id.
LITERT_CL
LiteRT backend initialized: GPU text + GPU vision
activeBackendMode=GPU text + GPU vision
Beschreibung abgeschlossen
```

This non-blocking line may still appear:

```text
GPU sampler unavailable. Falling back to CPU sampling.
```

Treat it as a performance note unless model or vision backend also falls back.

## Image input guardrails

Do:

- use file-backed camera capture,
- keep single-image input,
- preserve aspect ratio,
- cap the long edge at `1024 px`,
- log prepared bitmap dimensions and PNG byte size.

Do not:

- use `TakePicturePreview()` for model input,
- send full-resolution camera images to LiteRT-LM,
- reintroduce square `512 x 512` letterboxing unless a device log proves it is needed,
- raise the long-edge cap above `1024 px` without RAM and logcat comparison.

Confirmed S23+ example:

```text
GemmaImagePreprocessing: capturedPhotoSizeBytes=3118274
GemmaImagePreprocessing: preparedBitmap=1024x577; pngBytes=833529
```

## RAM behavior during GPU inference

Observed with `GPU only` and a prepared `1024 x 577` image:

```text
Peak app TOTAL PSS: about 3.55 GB
Peak app TOTAL RSS: about 3.49 GB
Peak app Graphics: about 2.55 GB
Peak system GPU memory: about 2.84 GB
Free RAM during peak: about 0.8-0.9 GB
Post-inference app TOTAL PSS: about 241 MB
Post-inference app Graphics: about 68 MB
```

The device completed inference and released memory afterward. This confirms the current path works, but it also shows that `1024 px` is already a real GPU memory load.

## Validation commands

Build:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace --no-daemon
```

Install and run:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n de.kreutzm.gemma4test/.MainActivity
```

Check memory during inference:

```powershell
adb shell dumpsys meminfo de.kreutzm.gemma4test
adb shell dumpsys meminfo
adb shell cat /proc/meminfo
```

Check package native-library visibility:

```powershell
adb shell dumpsys package de.kreutzm.gemma4test | findstr /i "OpenCL GLES mali PVROCL uses-native-library"
```

## Decision rules for future apps

- If GPU/OpenCL fails before engine creation, check manifest native-library declarations first.
- If the UI shows tiny image dimensions such as `141 x 250`, the app is still using preview capture.
- If GPU memory gets close to the device limit, lower image long edge before changing model/runtime.
- If changing the model artifact, pin size and revision and repeat GPU-only logcat validation.
- Keep CPU fallback for usability, but validate performance-sensitive work in `GPU only`.
