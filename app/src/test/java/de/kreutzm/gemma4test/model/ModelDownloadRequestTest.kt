package de.kreutzm.gemma4test.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelDownloadRequestTest {
    @Test
    fun gemma4E2BUsesConfiguredMetadata() {
        val request = ModelDownloadRequest.gemma4E2B()

        assertEquals(GemmaModelConfig.displayName, request.displayName)
        assertEquals(GemmaModelConfig.downloadUrl, request.url)
        assertEquals(GemmaModelConfig.fileName, request.fileName)
        assertEquals(GemmaModelConfig.sizeBytes, request.expectedSizeBytes)
        assertEquals(GemmaModelConfig.galleryModelCommitHash, request.sourceRevision)
    }

    @Test
    fun rejectsNonHttpsDownloadUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelDownloadRequest(
                displayName = "test",
                url = "http://example.invalid/model.litertlm",
                fileName = "model.litertlm",
                expectedSizeBytes = 1L,
                sourceRevision = "revision",
            )
        }
    }

    @Test
    fun rejectsNonPositiveExpectedSize() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelDownloadRequest(
                displayName = "test",
                url = "https://example.invalid/model.litertlm",
                fileName = "model.litertlm",
                expectedSizeBytes = 0L,
                sourceRevision = "revision",
            )
        }
    }

    @Test
    fun rejectsBlankSourceRevision() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelDownloadRequest(
                displayName = "test",
                url = "https://example.invalid/model.litertlm",
                fileName = "model.litertlm",
                expectedSizeBytes = 1L,
                sourceRevision = " ",
            )
        }
    }
}
