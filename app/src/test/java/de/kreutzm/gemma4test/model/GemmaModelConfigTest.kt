package de.kreutzm.gemma4test.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaModelConfigTest {
    @Test
    fun modelMetadataMatchesExpectedLiteRtLmBundle() {
        assertEquals("Gemma-4-E2B-it", GemmaModelConfig.displayName)
        assertEquals("hf-gallery-allowlist-updated", GemmaModelConfig.currentVariantId)
        assertEquals("gemma-4-E2B-it.litertlm", GemmaModelConfig.fileName)
        assertEquals(2_583_085_056L, GemmaModelConfig.sizeBytes)
        assertTrue(GemmaModelConfig.downloadUrl.endsWith("/gemma-4-E2B-it.litertlm"))
    }

    @Test
    fun observedGalleryModelMetadataMatchesLocalPull() {
        assertEquals(
            "gemma4_2b_v09_obfus_fix_all_modalities_thinking.litertlm",
            GemmaModelConfig.observedGalleryFileName,
        )
        assertEquals(2_538_766_336L, GemmaModelConfig.observedGallerySizeBytes)
        assertEquals(
            "02957360dbcd67bf4bd629271fa8f9ab318ad6d1c10593bae5e9900611669bc0",
            GemmaModelConfig.observedGallerySha256,
        )
    }
}
