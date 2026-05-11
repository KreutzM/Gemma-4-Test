package de.kreutzm.gemma4test.model

object GemmaModelConfig {
    const val displayName = "Gemma-4-E2B-it"
    const val huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm"
    const val currentVariantId = "hf-gallery-allowlist-updated"
    const val fileName = "gemma-4-E2B-it.litertlm"
    // Hugging Face resolve/Xet headers for the pinned commit report X-Linked-Size/Content-Length
    // 2,583,085,056 bytes as of 2026-05-10.
    const val sizeBytes = 2_583_085_056L
    const val expectedSha256 = ""
    const val minDeviceMemoryGb = 8

    // Google AI Edge Gallery model_allowlists/1_0_13.json lists this updated model file commit
    // for Gemma-4-E2B-it. Keep the URL pinned for reproducible device debugging.
    const val galleryModelCommitHash = "7fa1d78473894f7e736a21d920c3aa80f950c0db"
    const val galleryInitialModelCommitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f"

    // Prefer the Hugging Face Hub API or an authenticated mirror if the file requires license-gated
    // access. Do not hard-code personal tokens in this repo.
    const val downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/7fa1d78473894f7e736a21d920c3aa80f950c0db/gemma-4-E2B-it.litertlm"

    const val observedGalleryVariantId = "observed-play-store-gallery-20260325"
    const val observedGalleryFileName = "gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm"
    const val observedGallerySizeBytes = 2_538_766_336L
    const val observedGallerySha256 = "02957360dbcd67bf4bd629271fa8f9ab318ad6d1c10593bae5e9900611669bc0"
    const val observedGalleryPath =
        "/storage/emulated/0/Android/data/com.google.ai.edge.gallery/files/Gemma_4_E2B_it/20260325/gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm"

    const val defaultPrompt =
        "Beschreibe dieses Foto präzise auf Deutsch. Nenne sichtbare Objekte, Szene, Kontext und Unsicherheiten."
}
