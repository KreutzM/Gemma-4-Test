package de.kreutzm.gemma4test.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class GemmaInferenceConfigTest {
    @Test
    fun defaultConfigUsesConservativeMvpValues() {
        val config = GemmaInferenceConfig()

        assertEquals(1024, config.maxTokens)
        assertEquals(40, config.topK)
        assertEquals(0.95, config.topP, 0.0)
        assertEquals(0.2, config.temperature, 0.0)
        assertFalse(config.prompt.isBlank())
    }

    @Test
    fun rejectsBlankPrompt() {
        assertThrows(IllegalArgumentException::class.java) {
            GemmaInferenceConfig(prompt = " ")
        }
    }

    @Test
    fun rejectsInvalidSamplerValues() {
        assertThrows(IllegalArgumentException::class.java) {
            GemmaInferenceConfig(maxTokens = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GemmaInferenceConfig(topK = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GemmaInferenceConfig(topP = 1.1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GemmaInferenceConfig(temperature = -0.1)
        }
    }
}
