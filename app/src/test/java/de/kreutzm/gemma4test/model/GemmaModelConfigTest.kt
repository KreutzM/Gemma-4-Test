package de.kreutzm.gemma4test.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaModelConfigTest {
    @Test
    fun modelMetadataMatchesExpectedLiteRtLmBundle() {
        assertEquals("Gemma-4-E2B-it", GemmaModelConfig.displayName)
        assertEquals("gemma-4-E2B-it.litertlm", GemmaModelConfig.fileName)
        assertEquals(2_588_147_712L, GemmaModelConfig.sizeBytes)
        assertTrue(GemmaModelConfig.downloadUrl.endsWith("/gemma-4-E2B-it.litertlm"))
    }
}
