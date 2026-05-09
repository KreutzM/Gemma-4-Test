package de.kreutzm.gemma4test.model

object GemmaModelConfig {
    const val displayName = "Gemma-4-E2B-it"
    const val huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm"
    const val fileName = "gemma-4-E2B-it.litertlm"
    const val sizeBytes = 2_588_147_712L
    const val minDeviceMemoryGb = 8

    // Final implementation should prefer the Hugging Face Hub API or an authenticated mirror if
    // the file requires license-gated access. Do not hard-code personal tokens in this repo.
    const val downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

    const val defaultPrompt =
        "Beschreibe dieses Foto präzise auf Deutsch. Nenne sichtbare Objekte, Szene, Kontext und Unsicherheiten."
}
