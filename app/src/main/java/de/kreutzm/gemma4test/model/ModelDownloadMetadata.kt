package de.kreutzm.gemma4test.model

import java.io.File
import java.util.Properties

data class ModelDownloadMetadata(
    val displayName: String,
    val fileName: String,
    val url: String,
    val sourceRevision: String,
    val expectedSizeBytes: Long,
) {
    fun matches(request: ModelDownloadRequest): Boolean =
        displayName == request.displayName &&
            fileName == request.fileName &&
            url == request.url &&
            sourceRevision == request.sourceRevision &&
            expectedSizeBytes == request.expectedSizeBytes

    fun writeTo(file: File) {
        val properties = Properties().apply {
            setProperty(KEY_DISPLAY_NAME, displayName)
            setProperty(KEY_FILE_NAME, fileName)
            setProperty(KEY_URL, url)
            setProperty(KEY_SOURCE_REVISION, sourceRevision)
            setProperty(KEY_EXPECTED_SIZE_BYTES, expectedSizeBytes.toString())
        }
        file.outputStream().use { output ->
            properties.store(output, "Gemma model download metadata")
        }
    }

    companion object {
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_FILE_NAME = "fileName"
        private const val KEY_URL = "url"
        private const val KEY_SOURCE_REVISION = "sourceRevision"
        private const val KEY_EXPECTED_SIZE_BYTES = "expectedSizeBytes"

        fun fromRequest(request: ModelDownloadRequest): ModelDownloadMetadata = ModelDownloadMetadata(
            displayName = request.displayName,
            fileName = request.fileName,
            url = request.url,
            sourceRevision = request.sourceRevision,
            expectedSizeBytes = request.expectedSizeBytes,
        )

        fun readFrom(file: File): ModelDownloadMetadata? {
            if (!file.isFile) return null
            val properties = Properties()
            return try {
                file.inputStream().use { input -> properties.load(input) }
                ModelDownloadMetadata(
                    displayName = properties.getProperty(KEY_DISPLAY_NAME) ?: return null,
                    fileName = properties.getProperty(KEY_FILE_NAME) ?: return null,
                    url = properties.getProperty(KEY_URL) ?: return null,
                    sourceRevision = properties.getProperty(KEY_SOURCE_REVISION) ?: return null,
                    expectedSizeBytes = properties.getProperty(KEY_EXPECTED_SIZE_BYTES)?.toLongOrNull() ?: return null,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
