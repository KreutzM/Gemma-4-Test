# Gemma 4 Android MVP

Minimal Android MVP for offline image description with Google Gemma 4 E2B on a Samsung S23+.

The app target is intentionally narrow:

1. download the Gemma 4 E2B LiteRT-LM model from Hugging Face on first use,
2. keep the model file in app-private storage,
3. capture one photo with the Android camera,
4. run local image+text inference with LiteRT-LM,
5. show a German image description without sending image or prompt data to a server after the model has been downloaded.

## Current baseline

This repository is a Codex-ready starter repo. It contains:

- Android project skeleton in `app/` with a Compose UI placeholder and permissions/manifests.
- Model metadata and download target documented in `docs/research/google-ai-edge-gallery.md`.
- Codex workflow instructions in `AGENTS.md`, `.codex/config.toml`, and `docs/codex-plan.md`.
- A phased implementation plan in `docs/implementation-plan.md`.

## Target model

Use `Gemma-4-E2B-it` from the Gallery allowlist:

- Hugging Face repo: `litert-community/gemma-4-E2B-it-litert-lm`
- File: `gemma-4-E2B-it.litertlm`
- Size: `2,588,147,712` bytes
- Minimum device memory according to Gallery: `8 GB`
- Runtime: LiteRT-LM Android (`com.google.ai.edge.litertlm:litertlm-android`)

## Expected device

Samsung Galaxy S23+ has 8 GB RAM in common configurations, so it matches the Gallery allowlist minimum for E2B but not the E4B minimum. Treat GPU as preferred, CPU as fallback, and keep token/image sizes conservative.

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
