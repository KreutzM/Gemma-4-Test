# Image preprocessing and Gemma 4 patch count

## Observed CPU/XNNPack failure

After the backend fallback PR, the app no longer failed on the GPU/OpenGL delegate path. LiteRT-LM initialized with CPU/XNNPack and vision enabled:

```text
MainExecutorSettings: backend: CPU
EncoderBackend: CPU
AdapterBackend: CPU
max_num_images: 1
number_of_threads: 4
Creating Gemma4DataProcessor
Resize image from 141x250 to 576x1056
encoder_signature_index: 1 name: vision_280
adapter_signature_index: 1 name: vision_adapter_280
```

However, inference still failed:

```text
DYNAMIC_UPDATE_SLICE failed to prepare
Failed to allocate tensors
RunPrefillAsync status: INTERNAL
Failed to invoke the compiled model
Receive callback OnError
```

The internal resize was especially important:

```text
Resize image from 141x250 to 576x1056 which will result in 2376 patches
max_num_patches: 2520
```

The patch count was close to the model/runtime limit. Very tall or very wide preview images can be internally upscaled into many patches even when the source bitmap is small.

## Current mitigation

For the MVP, captured images are now converted into a conservative square letterbox image before being encoded as PNG and sent to LiteRT-LM:

- canvas: `512 x 512`
- image is aspect-fit into the square,
- background is black,
- output is PNG bytes,
- single image only.

This is intentionally more conservative than the previous long-edge `1024 px` preprocessing.

## Why letterbox instead of crop

Cropping would reduce patches but can remove important image content. Letterboxing preserves the whole scene while bounding the input geometry.

## Device smoke test

After installing this change:

1. Capture one photo.
2. Confirm UI shows `512 x 512 px` for the prepared image.
3. Run image description.
4. Check logcat for LiteRT-LM's internal resize and patch count.
5. Verify whether `DYNAMIC_UPDATE_SLICE` and `Failed to allocate tensors` still occur.

## Future knobs

If 512 still fails, try lower square sizes in follow-up PRs:

- `448 x 448`
- `384 x 384`
- `336 x 336`

Do not increase above `512 x 512` until a device smoke test proves the patch/memory path is stable on Samsung S23+.
