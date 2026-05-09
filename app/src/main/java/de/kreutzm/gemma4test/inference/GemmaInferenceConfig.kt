package de.kreutzm.gemma4test.inference

import de.kreutzm.gemma4test.model.GemmaModelConfig

data class GemmaInferenceConfig(
    val prompt: String = GemmaModelConfig.defaultPrompt,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Double = DEFAULT_TOP_P,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val preferGpu: Boolean = true,
) {
    init {
        require(prompt.isNotBlank()) { "prompt must not be blank" }
        require(maxTokens > 0) { "maxTokens must be positive" }
        require(topK > 0) { "topK must be positive" }
        require(topP in 0.0..1.0) { "topP must be in [0, 1]" }
        require(temperature >= 0.0) { "temperature must be non-negative" }
    }

    companion object {
        const val DEFAULT_MAX_TOKENS = 1024
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_TOP_P = 0.95
        const val DEFAULT_TEMPERATURE = 0.2
    }
}
