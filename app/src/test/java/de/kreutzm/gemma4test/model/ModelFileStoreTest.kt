package de.kreutzm.gemma4test.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelFileStoreTest {
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
    fun createsModelDirectory() {
        val modelsDir = temporaryFolder.root.resolve("models")
        val store = ModelFileStore.fromDirectory(modelsDir)

        assertFalse(modelsDir.exists())
        store.ensureModelsDir()
        assertTrue(modelsDir.isDirectory)
    }

    @Test
    fun detectsCompleteModelByExpectedSizeAndMetadata() {
        val store = ModelFileStore.fromDirectory(temporaryFolder.root)
        store.ensureModelsDir()
        store.modelFile(request).writeBytes(byteArrayOf(1, 2, 3, 4))
        store.writeMetadata(request)

        assertTrue(store.hasCompleteModel(request))
    }

    @Test
    fun rejectsModelWithoutMetadata() {
        val store = ModelFileStore.fromDirectory(temporaryFolder.root)
        store.ensureModelsDir()
        store.modelFile(request).writeBytes(byteArrayOf(1, 2, 3, 4))

        assertFalse(store.hasCompleteModel(request))
    }

    @Test
    fun rejectsModelWithStaleMetadata() {
        val store = ModelFileStore.fromDirectory(temporaryFolder.root)
        store.ensureModelsDir()
        store.modelFile(request).writeBytes(byteArrayOf(1, 2, 3, 4))
        store.writeMetadata(request.copy(sourceRevision = "revision-old"))

        assertFalse(store.hasCompleteModel(request))
    }

    @Test
    fun rejectsWrongSizedModel() {
        val store = ModelFileStore.fromDirectory(temporaryFolder.root)
        store.ensureModelsDir()
        store.modelFile(request).writeBytes(byteArrayOf(1, 2, 3))
        store.writeMetadata(request)

        assertFalse(store.hasCompleteModel(request))
    }

    @Test
    fun rejectsPathSegmentsInModelFileName() {
        val unsafeRequest = request.copy(fileName = "../model.litertlm")
        val store = ModelFileStore.fromDirectory(temporaryFolder.root)

        assertThrows(IllegalArgumentException::class.java) {
            store.modelFile(unsafeRequest)
        }
    }
}
