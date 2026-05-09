# Implementation plan

## Goal

Build a minimal, reliable Android MVP that downloads Gemma 4 E2B once and then produces German descriptions for newly captured photos fully offline.

## Phase 0: Repository hygiene

- Keep CI green on every commit.
- Generate and commit Gradle Wrapper locally or in the first Codex run.
- Add ktlint/detekt only after the baseline project compiles.
- Keep model files out of Git.

## Phase 1: Compile-ready Android skeleton

Deliverables:

- Compose main screen with model status, download action, camera action, inference action, output area.
- Runtime permissions for camera.
- App-private model path helper.
- Unit tests for model metadata and file-path helpers.
- CI passing `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.

Acceptance criteria:

- Fresh clone builds on GitHub Actions.
- App launches on Android emulator/device.
- No model download yet required for build.

## Phase 2: Model download

Deliverables:

- `ModelDownloader` with progress state.
- App-private storage target under `context.filesDir/models/gemma-4-E2B-it.litertlm`.
- Byte-count verification against `GemmaModelConfig.sizeBytes`.
- Retry/cancel support.
- Clear error message for network/license/HTTP failures.

Acceptance criteria:

- Download can be started from UI.
- Interrupted downloads do not leave a corrupt file marked ready.
- App reports exact downloaded bytes and final local file path.

## Phase 3: Camera/photo preprocessing

Deliverables:

- Camera permission flow.
- Minimal photo capture, preferably `TakePicturePreview` for first MVP or `TakePicture` with FileProvider if quality is needed.
- Downscale image to bounded long edge.
- Convert bitmap to PNG bytes.
- Preview captured image in UI.

Acceptance criteria:

- Capture works on Samsung S23+.
- Image memory footprint is bounded.
- UI can clear/re-take photo.

## Phase 4: LiteRT-LM inference

Deliverables:

- Add verified `com.google.ai.edge.litertlm:litertlm-android` dependency.
- Implement `GemmaVisionEngine`:
  - initialize `Engine` with model path,
  - set LLM backend and `visionBackend`,
  - create `Conversation`,
  - send `Content.ImageBytes` followed by `Content.Text`,
  - stream results,
  - cleanup/cancel.
- Backend strategy: GPU first, CPU fallback for LLM if supported; expose diagnostic status.

Acceptance criteria:

- With downloaded model and captured image, app streams a German description.
- Airplane mode works after download.
- Errors are visible and actionable instead of crashes/freezes.

## Phase 5: Reliability pass

Deliverables:

- Stress tests on S23+: cold start, repeated photo capture, repeated inference, orientation changes.
- Conservative default prompt and token limit.
- Logcat tags documented.
- Known failure modes documented in README.

Acceptance criteria:

- 5 consecutive image descriptions without app restart.
- Engine cleanup works when leaving the screen.
- No main-thread ANRs during download/init/inference.

## Phase 6: Optional improvements

- Persist last output and last photo URI.
- Add backend selector for CPU/GPU diagnostics.
- Add network constraint for model download.
- Add checksum verification if Google/Hugging Face publishes a stable checksum.
- Add instrumented smoke test for UI navigation.
