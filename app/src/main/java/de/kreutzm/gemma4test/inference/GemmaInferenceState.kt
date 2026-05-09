package de.kreutzm.gemma4test.inference

sealed interface GemmaInferenceState {
    data object Idle : GemmaInferenceState
    data object Initializing : GemmaInferenceState
    data object Running : GemmaInferenceState

    data class Streaming(
        val text: String,
    ) : GemmaInferenceState

    data class Completed(
        val text: String,
    ) : GemmaInferenceState

    data class Failed(
        val message: String,
    ) : GemmaInferenceState
}
