package de.kreutzm.gemma4test.inference

import de.kreutzm.gemma4test.model.GemmaModelConfig

data class GemmaInferenceConfig(
    val prompt: String = GemmaModelConfig.defaultPrompt,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val maxImages: Int = DEFAULT_MAX_IMAGES,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Double = DEFAULT_TOP_P,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val backendPolicy: GemmaBackendPolicy = GemmaBackendPolicy.GpuThenCpuFallback,
) {
    init {
        require(prompt.isNotBlank()) { "prompt must not be blank" }
        require(maxTokens > 0) { "maxTokens must be positive" }
        require(maxImages > 0) { "maxImages must be positive" }
        require(topK > 0) { "topK must be positive" }
        require(topP in 0.0..1.0) { "topP must be in [0, 1]" }
        require(temperature >= 0.0) { "temperature must be non-negative" }
    }

    companion object {
        // Aligned with Google AI Edge Gallery model_allowlists/1_0_13.json for Gemma-4-E2B-it.
        const val DEFAULT_MAX_TOKENS = 4000
        const val DEFAULT_MAX_IMAGES = 1
        const val DEFAULT_TOP_K = 64
        const val DEFAULT_TOP_P = 0.95
        const val DEFAULT_TEMPERATURE = 1.0
    }
}

enum class GemmaBackendMode(
    val label: String,
) {
    GpuTextGpuVision("GPU text + GPU vision"),
    CpuTextCpuVision("CPU text + CPU vision"),
}

enum class GemmaBackendPolicy(
    val label: String,
) {
    GpuOnly("GPU only"),
    CpuOnly("CPU only"),
    GpuThenCpuFallback("GPU then CPU fallback"),
}

fun GemmaBackendPolicy.backendAttemptOrder(): List<GemmaBackendMode> = when (this) {
    GemmaBackendPolicy.GpuOnly -> listOf(GemmaBackendMode.GpuTextGpuVision)
    GemmaBackendPolicy.CpuOnly -> listOf(GemmaBackendMode.CpuTextCpuVision)
    GemmaBackendPolicy.GpuThenCpuFallback -> listOf(
        GemmaBackendMode.GpuTextGpuVision,
        GemmaBackendMode.CpuTextCpuVision,
    )
}
