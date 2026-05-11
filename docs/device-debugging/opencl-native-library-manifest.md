# OpenCL native-library manifest experiment

## Context

Google AI Edge Gallery initializes `Gemma-4-E2B-it` on the Samsung S23+ with GPU/OpenCL:

```text
Loaded OpenCL library with dlopen.
Created OpenCL device from provided device id and platform id.
Replacing ... with delegate (LITERT_CL)
```

This app requested the same GPU text + GPU vision backend but failed before engine creation:

```text
OpenCL not supported on this platform. Using OpenGL instead.
Failed to load OpenCL library with dlopen: libvndksupport.so not found
Replacing ... with delegate (LITERT_OPENGL)
CreateSharedMemoryManager is not implemented
LiteRT backend failed: GPU text + GPU vision
LiteRtLmJniException: Failed to create engine
```

A local test with the exact copied Play Store Gallery model file still failed in this app with the same OpenCL-to-OpenGL path. That strongly suggests the remaining issue is not the model binary alone, but native-library visibility, packaging, cache/runtime setup, or Gallery-specific initialization.

## Change under test

The app manifest now declares optional vendor GPU/OpenCL libraries:

```xml
<uses-native-library android:name="libOpenCL.so" android:required="false" />
<uses-native-library android:name="libOpenCL-pixel.so" android:required="false" />
<uses-native-library android:name="libGLES_mali.so" android:required="false" />
<uses-native-library android:name="libPVROCL.so" android:required="false" />
```

They are optional so devices without a specific vendor library remain installable.

## Device validation

After installing the APK from this PR:

1. Open Gemma 4 Test.
2. Select `GPU only`.
3. Use the current pinned app model first.
4. Capture one image.
5. Tap `Bild beschreiben`.
6. Capture logcat.

Search for:

```text
GemmaVisionEngine
MainExecutorSettings: backend: GPU
EncoderBackend: GPU
AdapterBackend: CPU
Loaded OpenCL library with dlopen
Created OpenCL device
LITERT_CL
LITERT_OPENGL
OpenCL not supported
libvndksupport.so not found
CreateSharedMemoryManager
LiteRT backend initialized: GPU text + GPU vision
LiteRT backend failed: GPU text + GPU vision
```

## Expected outcomes

### Success

```text
Loaded OpenCL library with dlopen
Created OpenCL device
LITERT_CL
LiteRT backend initialized: GPU text + GPU vision
```

This would indicate the missing manifest declarations were blocking OpenCL visibility.

### Partial progress

If OpenCL loads but inference fails later, keep the manifest change and debug the new failure separately.

### No change

If the app still logs:

```text
OpenCL not supported on this platform. Using OpenGL instead.
libvndksupport.so not found
LITERT_OPENGL
CreateSharedMemoryManager is not implemented
```

then the manifest entries were not sufficient. The next investigation should compare Gallery's APK manifest/native-library declarations, app package ABI/native libs, cache directory behavior, and whether Gallery initializes/reuses a GPU environment differently.

## Verification command

After install, inspect package native-library declarations:

```powershell
adb shell dumpsys package de.kreutzm.gemma4test | findstr /i "OpenCL GLES mali PVROCL uses-native-library"
```
