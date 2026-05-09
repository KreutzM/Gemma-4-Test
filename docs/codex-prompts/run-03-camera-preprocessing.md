# Codex Run 03: camera capture and image preprocessing

You are working in `KreutzM/Gemma-4-Test`.

## Goal

Add the minimal camera/photo path needed before LiteRT-LM inference.

## Scope

- Request camera permission.
- Capture a preview bitmap with `ActivityResultContracts.TakePicturePreview`.
- Display the captured image in the MVP UI.
- Downscale the captured bitmap to a bounded long edge.
- Encode the processed image as PNG bytes for a later LiteRT-LM `Content.ImageBytes` call.
- Add unit tests for pure resize planning.

## Out of scope

- CameraX.
- Full-resolution file capture.
- FileProvider/external storage.
- EXIF orientation handling.
- LiteRT-LM dependency and inference.
- Real device/instrumented camera tests.

## Validation

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace --no-daemon
```

## Review checklist

- No external storage assumptions.
- The UI remains usable without a captured photo.
- The image long edge is bounded before inference.
- PNG bytes are prepared only after successful capture.
- No LiteRT-LM scope creep.
