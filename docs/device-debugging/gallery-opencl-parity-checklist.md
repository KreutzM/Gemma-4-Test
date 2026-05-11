# Gallery OpenCL parity checklist

## Known Gallery success path

Gallery on Samsung S23+ initialized `Gemma-4-E2B-it` Ask Image with:

- `MainExecutorSettings: backend: GPU`
- `EncoderBackend: GPU`
- `AdapterBackend: CPU`
- OpenCL loaded through `dlopen`
- `LITERT_CL` delegate applied
- speculative decoding disabled in the captured run

Gallery model path observed in logcat:

```text
/storage/emulated/0/Android/data/com.google.ai.edge.gallery/files/Gemma_4_E2B_it/20260325/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
```

Locally pulled Gallery file:

```text
file name: gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
size: 2,538,766,336 bytes
sha256: 02957360dbcd67bf4bd629271fa8f9ab318ad6d1c10593bae5e9900611669bc0
```

## Known app failure path

This app currently requests GPU but fails before engine creation:

- `MainExecutorSettings: backend: GPU`
- `EncoderBackend: GPU`
- `AdapterBackend: CPU`
- OpenCL unavailable in this app process
- OpenGL fallback
- `CreateSharedMemoryManager is not implemented`
- `LiteRtLmJniException: Failed to create engine`

Current app model path:

```text
/data/user/0/de.kreutzm.gemma4test/files/models/gemma-4-E2B-it.litertlm
```

## Hypotheses in priority order

1. Different `.litertlm` binary or revision.
2. Different storage path: Gallery uses external app files, this app uses internal app files.
3. Different cache directory behavior or serialized GPU artifact reuse.
4. Different native library or OpenCL discovery environment.
5. Different `EngineConfig` fields or experimental flags.
6. Gallery reuses serialized OpenCL/GPU artifacts from a previous successful initialization.

## Model revision status

The public Gallery allowlist points to Hugging Face file `gemma-4-E2B-it.litertlm` with initial commit `6e5c4f1e395deb959c494953478fa5cec4b8008f` and updated commit `7fa1d78473894f7e736a21d920c3aa80f950c0db`.

The observed Play Store Gallery file name `gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm` was not found in the public Gallery repo or allowlist. Until a public source URL is identified, do not replace this app's download URL with the observed Gallery artifact.

See `docs/research/gallery-model-revision.md`.

## Comparison command

After one Gallery Ask Image inference and one Gemma-4-Test GPU-only attempt, run:

```powershell
.\scripts\compare-litert-model-files.ps1
```

The script writes a Markdown report under `logs/`.

Decision rule:

```text
RESULT: model files differ. Align model revision before changing EngineConfig.
RESULT: model files match. Investigate path/cache/runtime initialization differences.
If Gallery-equivalent model works on GPU: root cause was model artifact/revision mismatch.
If Gallery-equivalent model still fails on GPU: investigate path/cache/native OpenCL initialization differences.
If Gallery-equivalent model is not publicly reproducible: build Gallery from source or use a local-only external-file test; do not commit model binaries.
```

## Optional external model path experiment

Do not enable this by default. Use it only as an isolated follow-up after the model-file comparison.

Test internal model path versus external app-files model path and compare `OpenCL`, `OpenGL`, `LITERT_CL`, `modelPath`, `cacheDir`, and `nativeLibraryDir` logcat lines.
