package de.kreutzm.gemma4test.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Properties

class ModelDownloadMetadataTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val request = ModelDownloadRequest(
        displayName = "test",
        variantId = "test-variant",
        url = "https://example.invalid/model.litertlm",
        fileName = "model.litertlm",
        expectedSizeBytes = 4L,
        expectedSha256 = null,
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
    fun readsLegacyMetadataWithoutVariantId() {
        val metadataFile = temporaryFolder.newFile("legacy.metadata")
        Properties().apply {
            setProperty("displayName", request.displayName)
            setProperty("fileName", request.fileName)
            setProperty("url", request.url)
            setProperty("sourceRevision", request.sourceRevision)
            setProperty("expectedSizeBytes", request.expectedSizeBytes.toString())
        }.store(metadataFile.outputStream(), "legacy")

        val metadata = ModelDownloadMetadata.readFrom(metadataFile)

        assertEquals("legacy-metadata", metadata?.variantId)
        assertTrue(metadata?.matchesLegacyRequest(request) == true)
        assertFalse(metadata?.matches(request) == true)
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
    fun metadataRejectsDifferentVariant() {
        val metadata = ModelDownloadMetadata.fromRequest(request)

        assertFalse(metadata.matches(request.copy(variantId = "other-variant")))
    }

    @Test
    fun malformedMetadataReturnsNull() {
        val metadataFile = temporaryFolder.newFile("malformed.metadata")
        metadataFile.writeText("not=complete")

        assertNull(ModelDownloadMetadata.readFrom(metadataFile))
    }
}
