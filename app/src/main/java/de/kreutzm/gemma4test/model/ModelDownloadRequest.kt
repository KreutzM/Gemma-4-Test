package de.kreutzm.gemma4test.model

data class ModelDownloadRequest(
    val displayName: String,
    val url: String,
    val fileName: String,
    val expectedSizeBytes: Long,
) {
    init {
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(url.startsWith("https://")) { "Model downloads must use HTTPS" }
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(expectedSizeBytes > 0L) { "expectedSizeBytes must be positive" }
    }

    companion object {
        fun gemma4E2B(): ModelDownloadRequest = ModelDownloadRequest(
            displayName = GemmaModelConfig.displayName,
            url = GemmaModelConfig.downloadUrl,
            fileName = GemmaModelConfig.fileName,
            expectedSizeBytes = GemmaModelConfig.sizeBytes,
        )
    }
}
