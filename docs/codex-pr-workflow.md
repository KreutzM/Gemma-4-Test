# PR-based Codex workflow

Every Codex or ChatGPT connector implementation run should use a pull request.

## Why

- GitHub Actions runs automatically on every pull request.
- The diff is reviewable before merging.
- Failed builds do not pollute `main`.
- Each implementation phase has a clear audit trail.
- ChatGPT can review the PR diff and CI result before the next run starts.

## Standard process

1. Create a branch named `codex/run-XX-short-topic`.
2. Apply one focused change set only.
3. Open a pull request against `main`.
4. Wait for CI:
   - `testDebugUnitTest`
   - `lintDebug`
   - `assembleDebug`
5. Review the PR here in Chat:
   - changed files,
   - CI logs if failing,
   - scope creep,
   - next-step readiness.
6. Merge only when the PR is green and reviewed.
7. Start the next run from updated `main`.

## Branch naming

Examples:

- `codex/run-01b-gradle-wrapper`
- `codex/run-02-model-downloader`
- `codex/run-03-camera-preprocessing`
- `codex/run-04-litertlm-inference`

## PR template checklist

Each PR description should contain:

```markdown
## Summary
- 

## Validation
- [ ] `./gradlew testDebugUnitTest lintDebug assembleDebug --stacktrace`
- [ ] GitHub Actions green

## Scope guard
- [ ] No model binaries committed
- [ ] No secrets committed
- [ ] No unrelated refactors

## Known risks
- 

## Next step
- 
```

## Rule

Do not commit directly to `main` for implementation runs. Direct commits are acceptable only for initial repository bootstrap or emergency fixes explicitly requested by the repository owner.
