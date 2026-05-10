package de.kreutzm.gemma4test.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaInferenceConfigTest {
    @Test
    fun defaultConfigUsesGalleryAlignedVisionValuesWithCpuRetry() {
        val config = GemmaInferenceConfig()

        assertEquals(4000, config.maxTokens)
        assertEquals(1, config.maxImages)
        assertEquals(64, config.topK)
        assertEquals(0.95, config.topP, 0.0)
        assertEquals(1.0, config.temperature, 0.0)
        assertEquals(GemmaBackendMode.GpuTextGpuVision, config.backendMode)
        assertTrue(config.retryCpuOnGpuFailure)
        assertFalse(config.prompt.isBlank())
    }

    @Test
    fun backendModeLabelsAreUsefulForDeviceDebugging() {
        assertEquals("GPU text + GPU vision", GemmaBackendMode.GpuTextGpuVision.label)
        assertEquals("CPU text + CPU vision", GemmaBackendMode.CpuTextCpuVision.label)
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
            GemmaInferenceConfig(maxImages = 0)
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
