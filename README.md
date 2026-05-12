# Gemma 4 Android MVP

Minimal Android MVP for offline image description with Google Gemma 4 E2B on a Samsung S23+.

The app target is intentionally narrow:

1. download the Gemma 4 E2B LiteRT-LM model from Hugging Face on first use,
2. keep the model file in app-private storage,
3. capture one photo with the Android camera,
4. run local image+text inference with LiteRT-LM,
5. show a German image description without sending image or prompt data to a server after the model has been downloaded.

## Current baseline

The app now has a working Samsung S23+ baseline for Gemma 4 E2B image description:

- LiteRT-LM initializes `GPU text + GPU vision` through OpenCL / `LITERT_CL`.
- The Android manifest declares optional vendor OpenCL/GPU libraries so Android exposes them to the app process.
- Camera capture uses a file-backed `TakePicture()` result through `FileProvider`, not `TakePicturePreview()`.
- Image preprocessing preserves aspect ratio and caps the long edge at `1024 px`.
- The debug UI exposes `GPU only`, `CPU only`, and `GPU then CPU fallback`.
- The UI and logcat show the selected policy and the active initialized backend.

Start new Gemma 4 E2B Android app work from `docs/device-debugging/s23-plus-known-good-baseline.md`.

## Target model

Use `Gemma-4-E2B-it` from the Gallery allowlist:

- Hugging Face repo: `litert-community/gemma-4-E2B-it-litert-lm`
- File: `gemma-4-E2B-it.litertlm`
- Pinned updated model file size: `2,583,085,056` bytes
- Minimum device memory according to Gallery: `8 GB`
- Runtime: LiteRT-LM Android (`com.google.ai.edge.litertlm:litertlm-android`)

## Expected device

Samsung Galaxy S23+ has 8 GB RAM in common configurations, so it matches the Gallery allowlist minimum for E2B but not the E4B minimum. Treat GPU as preferred and CPU as diagnostic/fallback. Keep single-image input and token/image sizes conservative.

Observed S23+ GPU-only inference with a `1024 x 577` prepared image peaked near:

- app `TOTAL PSS`: `3.55 GB`,
- app `Graphics`: `2.55 GB`,
- system GPU memory: `2.84 GB`.

The device completed inference and released memory afterward, but this is already a meaningful memory load. Do not raise image size beyond `1024 px` long edge without a device memory/logcat comparison.

## Developer quick start

```bash
# Clone
 git clone https://github.com/KreutzM/Gemma-4-Test.git
 cd Gemma-4-Test

# Optional: configure local Codex defaults
 codex -c model=gpt-5.5

# Android build once Gradle wrapper is generated or Android Studio syncs the project
 ./gradlew :app:assembleDebug
```

The committed skeleton intentionally does not include a Gradle Wrapper JAR because this repo was initialized through the GitHub connector. Generate it locally with Android Studio or `gradle wrapper` and commit it in the first Codex implementation run if desired.
