package de.kreutzm.gemma4test.model

object GemmaModelConfig {
    const val displayName = "Gemma-4-E2B-it"
    const val huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm"
    const val fileName = "gemma-4-E2B-it.litertlm"
    const val sizeBytes = 2_588_147_712L
    const val minDeviceMemoryGb = 8

    // Google AI Edge Gallery model_allowlists/1_0_13.json lists this updated model file commit
    // for Gemma-4-E2B-it. Keep the URL pinned for reproducible device debugging.
    const val galleryModelCommitHash = "7fa1d78473894f7e736a21d920c3aa80f950c0db"
    const val galleryInitialModelCommitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f"

    // Prefer the Hugging Face Hub API or an authenticated mirror if the file requires license-gated
    // access. Do not hard-code personal tokens in this repo.
    const val downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/7fa1d78473894f7e736a21d920c3aa80f950c0db/gemma-4-E2B-it.litertlm"

    const val defaultPrompt =
        "Beschreibe dieses Foto präzise auf Deutsch. Nenne sichtbare Objekte, Szene, Kontext und Unsicherheiten."
}
