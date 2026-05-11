# Model file comparison plan

## Why this matters

The Samsung S23+ comparison showed that Google AI Edge Gallery initializes Gemma-4-E2B-it with GPU/OpenCL, while this app falls from OpenCL to OpenGL and fails during GPU engine creation.

Gallery logcat showed a local model path such as:

```text
/storage/emulated/0/Android/data/com.google.ai.edge.gallery/files/Gemma_4_E2B_it/20260325/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
```

This app downloads:

```text
/data/user/0/de.kreutzm.gemma4test/files/models/gemma-4-E2B-it.litertlm
```

Both are labeled `Gemma-4-E2B-it`, but they may not be byte-identical. Before changing LiteRT-LM runtime code, compare the actual model files.

## Script

Use:

```powershell
.\scripts\compare-litert-model-files.ps1
```

With a specific device:

```powershell
.\scripts\compare-litert-model-files.ps1 -DeviceSerial <serial>
```

The script writes a timestamped summary under `logs/`, for example:

```text
logs/litert-model-comparison-20260511-153000.md
```

It records:

- device manufacturer/model/SoC,
- Gallery package metadata,
- this app's package metadata,
- Gallery `.litertlm` files visible under external app files,
- size and SHA-256 for Gallery model files when `sha256sum` is available on-device,
- size and SHA-256 for this app's model file via `run-as`,
- recent LiteRT/OpenCL/GPU logcat lines.

## Interpreting results

### Case A: File hashes match

If Gallery and this app use the same bytes, then the GPU/OpenCL difference is likely due to runtime initialization, environment, or cache behavior rather than model content.

Next investigation:

- Compare `EngineConfig` usage.
- Compare cache directory behavior.
- Check whether Gallery has prebuilt or reused GPU/OpenCL artifacts.
- Look for environment flags or native library paths.

### Case B: File sizes or hashes differ

If the files differ, do not assume runtime code is wrong yet.

Next investigation:

- Identify Gallery's exact model revision and file source.
- Determine whether Gallery uses a dated packaged asset not equivalent to Hugging Face's pinned `gemma-4-E2B-it.litertlm`.
- Test this app against the Gallery file only if this can be done without committing model binaries or violating distribution assumptions.
- Prefer aligning to the exact Gallery model revision through a documented download URL if available.

### Case C: Gallery file is not visible

If Gallery stores the file in private internal storage, Play Store Gallery cannot be inspected with `run-as`. In that case:

- rely on Gallery logcat model path,
- use Android external files if visible,
- or build Gallery from source as a debug app for deeper inspection.

## Guardrails

- Do not commit model binaries.
- Do not copy Gallery files into the repository.
- Do not assume `/data/data/com.google.ai.edge.gallery` is readable.
- Do not change this app's model URL or runtime until file comparison is documented.
- Keep the GPU diagnostics UI/logging in place until Gallery parity is achieved or the remaining gap is explained.

## Relevant logcat strings

```text
GemmaVisionEngine
AGLlmChatModelHelper
MainExecutorSettings
VisionExecutorSettings
EncoderBackend
AdapterBackend
OpenCL
OpenGL
LITERT_CL
CreateSharedMemoryManager
model_path
gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
gemma-4-E2B-it.litertlm
```
