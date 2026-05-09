# Codex Run 02: model downloader foundation

You are working in `KreutzM/Gemma-4-Test`.

## Goal

Implement a robust model download foundation without adding camera capture or LiteRT-LM inference.

## Scope

- Model download request metadata.
- App-private model file storage helper.
- Atomic download into `*.part`, then rename into final file after byte-count verification.
- UI state for idle, starting, downloading, completed, already available, and failed.
- Unit tests for metadata, path safety, expected-size checks, and progress calculation.

## Out of scope

- Camera capture.
- Image preprocessing.
- LiteRT-LM dependency and engine integration.
- Real network tests in CI.
- Hugging Face authentication handling beyond clear failure messages.

## Validation

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace --no-daemon
```

## Review checklist

- No model binaries committed.
- No secrets or tokens committed.
- Downloads use HTTPS only.
- Partial files are cleaned up on failure/cancellation.
- A completed model is recognized only by exact expected byte count.
- UI remains responsive during download.
