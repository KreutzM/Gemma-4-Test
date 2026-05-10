# Gallery parity configuration

## Goal

Gemma 4 LiteRT-LM vision is fragile outside Google AI Edge Gallery. This app should remove avoidable configuration drift before deeper runtime work.

## Gallery reference

Google AI Edge Gallery `model_allowlists/1_0_13.json` lists `Gemma-4-E2B-it` with:

```json
{
  "modelId": "litert-community/gemma-4-E2B-it-litert-lm",
  "modelFile": "gemma-4-E2B-it.litertlm",
  "sizeInBytes": 2588147712,
  "minDeviceMemoryInGb": 8,
  "commitHash": "6e5c4f1e395deb959c494953478fa5cec4b8008f",
  "llmSupportImage": true,
  "llmSupportAudio": true,
  "defaultConfig": {
    "topK": 64,
    "topP": 0.95,
    "temperature": 1.0,
    "maxContextLength": 32000,
    "maxTokens": 4000,
    "accelerators": "gpu,cpu",
    "visionAccelerator": "gpu"
  },
  "updatableModelFiles": [
    {
      "fileName": "gemma-4-E2B-it.litertlm",
      "commitHash": "7fa1d78473894f7e736a21d920c3aa80f950c0db"
    }
  ]
}
```

Gallery's LiteRT-LM helper passes `maxTokens` to `EngineConfig.maxNumTokens`, sets `visionBackend` when image support is enabled, and sends `Content.ImageBytes(...)` before `Content.Text(...)`.

## Changes made for parity

- Pin the model download URL to Gallery's updated model file commit hash:
  - `7fa1d78473894f7e736a21d920c3aa80f950c0db`
- Preserve the initial Gallery model commit hash for reference:
  - `6e5c4f1e395deb959c494953478fa5cec4b8008f`
- Align default inference config:
  - `topK = 64`
  - `topP = 0.95`
  - `temperature = 1.0`
  - `maxTokens = 4000`
- Keep existing Android app-specific diagnostic behavior:
  - GPU text + GPU vision first,
  - CPU text + CPU vision retry after GPU initialization failure,
  - `maxImages = 1`.

## Known non-parity

The app currently does not expose Gallery's full task/config model and does not pass `maxContextLength = 32000` because the inspected public LiteRT-LM Kotlin `EngineConfig` exposes `maxNumTokens`, not a separate `maxContextLength` parameter.

The app still uses a single hard-coded German prompt and one-image MVP flow.

## Device-test expectations

After installing this PR:

1. If the old model file was already downloaded from `resolve/main`, delete app data or delete the model file before testing the pinned download path.
2. Confirm the model is downloaded from the pinned commit URL.
3. Capture one image.
4. Run image description.
5. Check logcat for:
   - `max_num_images: 1`,
   - backend mode,
   - internal resize / patch count,
   - whether `DYNAMIC_UPDATE_SLICE failed to prepare` persists.

## Important caveat

This PR is not expected to directly solve the Gemma 4 vision soft-token / patch-budget failure. It removes avoidable drift from Gallery before the next deeper investigation.
