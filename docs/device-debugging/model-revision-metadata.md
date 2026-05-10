# Model revision metadata

## Problem

The app now pins `Gemma-4-E2B-it` to the updated Google AI Edge Gallery model-file revision:

```text
7fa1d78473894f7e736a21d920c3aa80f950c0db
```

Earlier app versions downloaded from `resolve/main`. Because the model file has the same name and expected byte size, a stale app-private file could be treated as complete even when it was downloaded from a different revision.

That makes device debugging non-reproducible.

## Current behavior

A completed download now requires both:

1. the `.litertlm` file exists and has the expected byte count, and
2. a sidecar metadata file exists and matches the current request.

Metadata path:

```text
gemma-4-E2B-it.litertlm.metadata
```

Stored fields:

```text
displayName
fileName
url
sourceRevision
expectedSizeBytes
```

If the model exists but the metadata is missing, stale, malformed, or points at a different URL/revision, the app treats the model as incomplete and downloads the pinned revision again.

## Device-test implication

After installing this PR, an existing model file from an older app run will be downloaded again once because it does not have matching metadata.

This is expected and useful for reproducibility.

## Remaining limitation

This metadata is not a cryptographic integrity check. It proves that the app downloaded through the pinned URL it was configured to use. It does not prove file content by hash.

A future PR should add SHA-256 verification if a stable checksum is available from the model publisher or generated from a trusted Gallery-matching artifact.
