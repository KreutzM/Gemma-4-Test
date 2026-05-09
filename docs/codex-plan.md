# Codex CLI workflow plan

## What Codex CLI is

Codex CLI is OpenAI's local coding agent. It runs in the developer checkout, can inspect and edit files, run commands, and produce commits/patches depending on the configured approval and sandbox settings.

Install options documented by OpenAI include:

```bash
npm install -g @openai/codex
# or
brew install --cask codex
```

Run from the repository root:

```bash
codex
```

For this project, use model `gpt-5.5` when available:

```bash
codex -c model=gpt-5.5
```

## Repo files that make Codex reliable

- `AGENTS.md`: project-specific rules Codex should obey.
- `.codex/config.toml`: local defaults for model/sandbox/approval profile. Treat as a suggested project profile, not a secret.
- `docs/implementation-plan.md`: phase plan and acceptance criteria.
- `docs/research/google-ai-edge-gallery.md`: Gallery-specific model/runtime notes.
- `docs/codex-prompts/`: one prompt file per Codex run.
- `.github/workflows/android-ci.yml`: CI gate for build/test/lint.
- `.gitignore`: avoid committing model binaries, APK outputs, IDE caches, and secrets.

## Planned Codex run sequence

### Run 1: Make skeleton build green

Prompt file: `docs/codex-prompts/run-01-build-green.md`

Scope:

- Generate or fix Gradle Wrapper if missing.
- Fix Android resources/icons/manifests if needed.
- Add minimal unit tests.
- Run `./gradlew testDebugUnitTest lintDebug assembleDebug`.
- Commit only build/test/lint-related changes.

Review here in Chat:

- Inspect diff.
- Inspect CI result.
- Check whether any dependency versions were changed without reason.

### Run 2: Model downloader

Scope:

- Implement robust file download with progress, retry/cancel, byte-count verification.
- Add unit tests for state transitions and path handling.
- Do not implement inference yet.

Review here in Chat:

- Ensure no token/secrets are committed.
- Ensure corrupt partial files are not marked ready.
- Verify UI state semantics.

### Run 3: Camera and image preprocessing

Scope:

- Implement capture and preview.
- Downscale bitmap and PNG conversion.
- Add tests for image scaling helpers where practical.

Review here in Chat:

- Check permissions and memory behavior.
- Verify no external storage assumptions.

### Run 4: LiteRT-LM integration

Scope:

- Verify latest LiteRT-LM Android artifact/version from official Google docs or Gallery build files.
- Implement `GemmaVisionEngine`.
- Use `Content.ImageBytes` followed by `Content.Text`.
- Add backend diagnostics and cleanup.

Review here in Chat:

- Confirm dependency/version source.
- Confirm lifecycle cleanup and error handling.
- Device-test on S23+.

## Review checklist for every Codex run

1. Does CI pass?
2. Is the diff limited to the requested scope?
3. Are model binaries, tokens, APKs, and local IDE files absent?
4. Are long-running tasks off the main thread?
5. Are errors shown in UI and logs without crashing?
6. Are new assumptions documented?
