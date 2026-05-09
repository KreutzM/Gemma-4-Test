# Codex Run 04: LiteRT-LM runtime integration

You are working in `KreutzM/Gemma-4-Test`.

## Goal

Wire the MVP to LiteRT-LM so a downloaded Gemma 4 E2B `.litertlm` file and prepared PNG image bytes can produce a local German image description.

## Source alignment

Google AI Edge Gallery main uses:

- `com.google.ai.edge.litertlm:litertlm-android:0.11.0`
- `EngineConfig(modelPath = ..., backend = ..., visionBackend = ...)`
- `Content.ImageBytes(...)` before `Content.Text(...)`
- `conversation.sendMessageAsync(...)` with `MessageCallback`

## Scope

- Add the LiteRT-LM dependency.
- Add a small `GemmaVisionEngine` wrapper.
- Initialize `Engine` and `Conversation`.
- Send PNG bytes before the text prompt.
- Stream partial text into the UI.
- Close `Conversation` and `Engine` after each MVP run.
- Keep conservative sampler defaults.

## Out of scope

- Backend selector UI.
- CPU fallback retry after GPU failure.
- Long-lived retained engine/ViewModel lifecycle.
- Multi-image support.
- Prompt editing UI.
- Instrumented/device tests.

## Validation

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace --no-daemon
```

## Device smoke test

After CI is green and the PR is merged, test on Samsung S23+:

1. Install debug APK.
2. Download model.
3. Switch to airplane mode.
4. Capture one photo.
5. Tap `Bild beschreiben`.
6. Confirm output streams and no crash occurs.

## Review checklist

- Image bytes are sent before text.
- `visionBackend` is non-null.
- No model files or secrets are committed.
- Engine and conversation are closed.
- Errors are surfaced in UI.
