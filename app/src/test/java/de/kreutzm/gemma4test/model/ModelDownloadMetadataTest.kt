package de.kreutzm.gemma4test.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelDownloadMetadataTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val request = ModelDownloadRequest(
        displayName = "test",
        url = "https://example.invalid/model.litertlm",
        fileName = "model.litertlm",
        expectedSizeBytes = 4L,
        sourceRevision = "revision-a",
    )

    @Test
    fun writesAndReadsMetadata() {
        val metadataFile = temporaryFolder.newFile("model.litertlm.metadata")
        val metadata = ModelDownloadMetadata.fromRequest(request)

        metadata.writeTo(metadataFile)

        assertEquals(metadata, ModelDownloadMetadata.readFrom(metadataFile))
    }

    @Test
    fun metadataMatchesOriginalRequest() {
        val metadata = ModelDownloadMetadata.fromRequest(request)

        assertTrue(metadata.matches(request))
    }

    @Test
    fun metadataRejectsDifferentRevision() {
        val metadata = ModelDownloadMetadata.fromRequest(request)

        assertFalse(metadata.matches(request.copy(sourceRevision = "revision-b")))
    }

    @Test
    fun metadataRejectsDifferentUrl() {
        val metadata = ModelDownloadMetadata.fromRequest(request)

        assertFalse(metadata.matches(request.copy(url = "https://example.invalid/other.litertlm")))
    }

    @Test
    fun malformedMetadataReturnsNull() {
        val metadataFile = temporaryFolder.newFile("malformed.metadata")
        metadataFile.writeText("not=complete")

        assertNull(ModelDownloadMetadata.readFrom(metadataFile))
    }
}
