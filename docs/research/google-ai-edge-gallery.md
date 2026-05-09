# Research: Google AI Edge Gallery for Gemma 4 E2B image analysis

Source repository: `google-ai-edge/gallery`.

## Model file used by Gallery

The Gallery Gemma 4 E2B entry uses a LiteRT-LM bundle, not a legacy MediaPipe `.task` file.

- Model name: `Gemma-4-E2B-it`
- Hugging Face repository: `litert-community/gemma-4-E2B-it-litert-lm`
- File: `gemma-4-E2B-it.litertlm`
- Size: `2,588,147,712` bytes
- Runtime type: `LiteRT_LM`
- Minimum device memory: `8 GB`
- Recommended max tokens for MVP: start at `1024`, then test `2048`.

For this MVP, avoid E4B on S23+ because it has a much higher memory requirement and multiple Gallery reports indicate slow or failed initialization on larger Gemma 4 variants.

## Implementation observations from Gallery

Gallery abstracts model runtime through `LlmModelHelper`. The LiteRT-LM implementation creates an `Engine`, then a `Conversation`, and sends multimodal content as ordered `Content` items.

Important behaviors to copy:

1. Keep the model file in app-private storage and pass its absolute path into `EngineConfig`.
2. Use `EngineConfig(modelPath = ..., backend = ..., visionBackend = ...)`.
3. Enable image support by setting a non-null `visionBackend`.
4. Convert input `Bitmap` to PNG bytes and add it as `Content.ImageBytes(...)` before the prompt text.
5. Use `conversation.sendMessageAsync(...)` and stream partial responses through `MessageCallback`.
6. Always close `Conversation` and `Engine` when leaving the screen or switching models.
7. Prefer conservative sampler settings for image description: low temperature, limited max output tokens, one image for the first MVP.

## Image-analysis pitfalls

- Vision support is not automatic. The engine must be initialized with a `visionBackend`; otherwise image bytes will not be processed correctly.
- Gallery comments note that Gemma multimodal variants may require GPU for vision while audio is CPU-only in their runtime wiring.
- Large images increase memory pressure. Downscale to a bounded long edge before PNG conversion, e.g. 1024 px or lower for the first MVP.
- Put image content before text content; Gallery comments mention this is used for accurate last-token handling.
- Keep the first MVP single-image. Multi-image plus long prompts has user-reported truncation and continuation issues.
- Avoid long structured JSON prompts initially. Use a concise German prompt and plain text output.
- Add CPU fallback for text backend, but test whether vision on CPU works acceptably before exposing it.
- Initialize and infer off the main thread. Large model compilation can freeze the UI if incorrectly dispatched.
- Treat download integrity as part of runtime reliability: verify byte count and resume/retry robustly.

## Known issues to consider

From open Gallery issues as of 2026-05-09:

- `Gemma-4-E2B-it` can fail to create the LiteRT engine on some devices, including a Pixel 9 Pro XL report with `INVALID_ARGUMENT` in `model_resources_litert_lm.cc`.
- `Gemma-4-E4B-it` has reported `Failed to create engine` errors and very long load times on some devices.
- Ask Image mode with Gemma 4 E2B, multiple images, and a very long prompt can stop generation mid-output; continuing may produce empty output or `<|channel>`.
- GPU can be slower or unstable on some non-Snapdragon devices; CPU fallback and clear backend selection are valuable.
- Some models can hallucinate their execution context, e.g. claiming they run remotely even when actually local. The app UI and system prompt should state local/offline execution but not rely on the model to verify privacy.

## MVP stance

For a Samsung S23+ target, start with:

- one image,
- GPU preferred for both LLM and vision if initialization succeeds,
- CPU fallback for LLM initialization failures,
- low memory image preprocessing,
- max output tokens at 1024,
- visible initialization/download states,
- robust cleanup and cancellation.
