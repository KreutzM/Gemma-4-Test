# Gallery model revision research

Date: 2026-05-11

## Question

Can this app download the exact `Gemma-4-E2B-it` file that Google AI Edge Gallery used on the Samsung S23+ GPU/OpenCL success path?

## Local Gallery artifact

The Play Store Gallery app stored this file on the S23+:

```text
/storage/emulated/0/Android/data/com.google.ai.edge.gallery/files/Gemma_4_E2B_it/20260325/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
```

Local pull/hash result:

```text
file name: gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
size: 2,538,766,336 bytes
sha256: 02957360dbcd67bf4bd629271fa8f9ab318ad6d1c10593bae5e9900611669bc0
```

This differs from the app's currently pinned Hugging Face artifact:

```text
file name: gemma-4-E2B-it.litertlm
expected size: 2,583,085,056 bytes
source revision: 7fa1d78473894f7e736a21d920c3aa80f950c0db
```

## Public Gallery source evidence

The public Google AI Edge Gallery allowlist `model_allowlists/1_0_13.json` lists `Gemma-4-E2B-it` as:

```text
modelId: litert-community/gemma-4-E2B-it-litert-lm
modelFile: gemma-4-E2B-it.litertlm
sizeInBytes: 2,588,147,712
commitHash: 6e5c4f1e395deb959c494953478fa5cec4b8008f
updatableModelFiles[0].fileName: gemma-4-E2B-it.litertlm
updatableModelFiles[0].commitHash: 7fa1d78473894f7e736a21d920c3aa80f950c0db
```

Gallery's public `AllowedModel.toModel()` logic builds a Hugging Face URL from `modelId`, `commitHash`, and `modelFile` unless an explicit `url` or SoC-specific model file overrides it.

Searches in the public Gallery repo for these observed Play Store strings returned no matching source entry:

```text
gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
Gemma_4_E2B_it
20260325
```

## Result

No public, reproducible Gallery-equivalent download URL was identified for the exact observed file:

```text
gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
```

Because the source URL is not identified, this app must not switch defaults or invent a URL. The app keeps the current pinned Hugging Face allowlist artifact and logs explicit model metadata before LiteRT-LM initialization.

## Decision rule

If a Gallery-equivalent model source is later found and GPU-only works:

```text
Root cause was model artifact/revision mismatch.
```

If the Gallery-equivalent file still fails on GPU:

```text
Investigate path/cache/native OpenCL initialization differences.
```

If the Gallery-equivalent model is not publicly reproducible:

```text
Build Gallery from source or use a local-only external-file test; do not commit model binaries.
```

## Local-only follow-up

Use the ignored local artifact only for controlled device testing:

```text
models/Edge-Gallery/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm
```

Do not commit `.litertlm`, `.task`, `.tflite`, APKs, generated logs, or model binaries.
