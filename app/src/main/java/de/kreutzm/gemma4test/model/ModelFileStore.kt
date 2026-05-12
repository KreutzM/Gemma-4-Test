package de.kreutzm.gemma4test.model

import android.content.Context
import java.io.File

class ModelFileStore private constructor(
    private val modelsDir: File,
) {
    fun modelFile(request: ModelDownloadRequest): File = fileForSafeName(request.fileName)

    fun partialFile(request: ModelDownloadRequest): File = fileForSafeName("${request.fileName}.part")

    fun metadataFile(request: ModelDownloadRequest): File = fileForSafeName("${request.fileName}.metadata")

    fun ensureModelsDir(): File = modelsDir.also { directory ->
        if (!directory.exists()) {
            check(directory.mkdirs()) { "Could not create model directory: ${directory.absolutePath}" }
        }
        check(directory.isDirectory) { "Model path is not a directory: ${directory.absolutePath}" }
    }

    fun hasCompleteModel(request: ModelDownloadRequest): Boolean {
        val file = modelFile(request)
        if (!file.isFile || file.length() != request.expectedSizeBytes) return false

        val metadata = ModelDownloadMetadata.readFrom(metadataFile(request)) ?: return false
        if (metadata.matches(request)) return true
        if (metadata.matchesLegacyRequest(request)) {
            writeMetadata(request)
            return true
        }
        return false
    }

    fun writeMetadata(request: ModelDownloadRequest) {
        ModelDownloadMetadata.fromRequest(request).writeTo(metadataFile(request))
    }

    fun deleteMetadata(request: ModelDownloadRequest) {
        val metadata = metadataFile(request)
        if (metadata.exists()) {
            check(metadata.delete()) { "Could not delete model metadata file: ${metadata.absolutePath}" }
        }
    }

    fun deletePartial(request: ModelDownloadRequest) {
        val partial = partialFile(request)
        if (partial.exists()) {
            check(partial.delete()) { "Could not delete partial model file: ${partial.absolutePath}" }
        }
    }

    private fun fileForSafeName(fileName: String): File {
        require(fileName == File(fileName).name) { "Model file name must not contain path segments" }
        require(fileName.isNotBlank()) { "Model file name must not be blank" }
        return File(modelsDir, fileName)
    }

    companion object {
        fun fromContext(context: Context): ModelFileStore = fromDirectory(File(context.filesDir, "models"))

        fun fromDirectory(modelsDir: File): ModelFileStore = ModelFileStore(modelsDir)
    }
}
