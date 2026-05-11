package de.kreutzm.gemma4test.model

data class ModelDownloadRequest(
    val displayName: String,
    val variantId: String,
    val url: String,
    val fileName: String,
    val expectedSizeBytes: Long,
    val expectedSha256: String?,
    val sourceRevision: String,
) {
    init {
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(variantId.isNotBlank()) { "variantId must not be blank" }
        require(url.startsWith("https://")) { "Model downloads must use HTTPS" }
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(expectedSizeBytes > 0L) { "expectedSizeBytes must be positive" }
        require(expectedSha256 == null || expectedSha256.matches(Regex("[a-fA-F0-9]{64}"))) {
            "expectedSha256 must be null or a 64-character hex string"
        }
        require(sourceRevision.isNotBlank()) { "sourceRevision must not be blank" }
    }

    companion object {
        fun gemma4E2B(): ModelDownloadRequest = gemma4E2BCurrent()

        fun gemma4E2BCurrent(): ModelDownloadRequest = ModelDownloadRequest(
            displayName = GemmaModelConfig.displayName,
            variantId = GemmaModelConfig.currentVariantId,
            url = GemmaModelConfig.downloadUrl,
            fileName = GemmaModelConfig.fileName,
            expectedSizeBytes = GemmaModelConfig.sizeBytes,
            expectedSha256 = GemmaModelConfig.expectedSha256.ifBlank { null },
            sourceRevision = GemmaModelConfig.galleryModelCommitHash,
        )
    }
}
