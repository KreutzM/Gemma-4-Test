package de.kreutzm.gemma4test.model

sealed interface ModelDownloadState {
    data object Idle : ModelDownloadState
    data object Starting : ModelDownloadState

    data class AlreadyAvailable(
        val absolutePath: String,
        val sizeBytes: Long,
    ) : ModelDownloadState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : ModelDownloadState {
        val progressPercent: Int = if (totalBytes > 0L) {
            ((downloadedBytes.coerceAtMost(totalBytes) * 100L) / totalBytes).toInt()
        } else {
            0
        }
    }

    data class Completed(
        val absolutePath: String,
        val sizeBytes: Long,
    ) : ModelDownloadState

    data class Failed(
        val message: String,
    ) : ModelDownloadState
}
