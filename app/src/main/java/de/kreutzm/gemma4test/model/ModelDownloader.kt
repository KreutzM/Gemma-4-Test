package de.kreutzm.gemma4test.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class ModelDownloader(
    private val fileStore: ModelFileStore,
) {
    suspend fun download(
        request: ModelDownloadRequest,
        onState: suspend (ModelDownloadState) -> Unit,
    ): ModelDownloadState = withContext(Dispatchers.IO) {
        onState(ModelDownloadState.Starting)
        fileStore.ensureModelsDir()

        val target = fileStore.modelFile(request)
        if (fileStore.hasCompleteModel(request)) {
            return@withContext ModelDownloadState.AlreadyAvailable(
                absolutePath = target.absolutePath,
                sizeBytes = target.length(),
            ).also { onState(it) }
        }

        fileStore.deletePartial(request)
        val partial = fileStore.partialFile(request)

        try {
            val finalState = downloadToPartialFile(request, partial, target, onState)
            onState(finalState)
            finalState
        } catch (cancellation: CancellationException) {
            partial.delete()
            throw cancellation
        } catch (throwable: Throwable) {
            partial.delete()
            val state = ModelDownloadState.Failed(throwable.message ?: throwable::class.java.simpleName)
            onState(state)
            state
        }
    }

    private suspend fun downloadToPartialFile(
        request: ModelDownloadRequest,
        partial: File,
        target: File,
        onState: suspend (ModelDownloadState) -> Unit,
    ): ModelDownloadState {
        val connection = openConnection(request.url)
        try {
            val responseCode = connection.responseCode
            check(responseCode in 200..299) { "Model download failed with HTTP $responseCode" }

            val reportedLength = connection.contentLengthLong
            if (reportedLength > 0L) {
                check(reportedLength == request.expectedSizeBytes) {
                    "Unexpected model size: server reports $reportedLength bytes, expected ${request.expectedSizeBytes} bytes"
                }
            }

            BufferedInputStream(connection.inputStream).use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE_BYTES)
                    var downloadedBytes = 0L
                    var lastReportedBytes = 0L

                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (downloadedBytes - lastReportedBytes >= PROGRESS_STEP_BYTES || downloadedBytes == request.expectedSizeBytes) {
                            lastReportedBytes = downloadedBytes
                            onState(ModelDownloadState.Downloading(downloadedBytes, request.expectedSizeBytes))
                        }
                    }
                }
            }

            check(partial.length() == request.expectedSizeBytes) {
                "Downloaded ${partial.length()} bytes, expected ${request.expectedSizeBytes} bytes"
            }

            if (target.exists()) {
                check(target.delete()) { "Could not replace existing model file: ${target.absolutePath}" }
            }
            check(partial.renameTo(target)) { "Could not move downloaded model into place" }

            return ModelDownloadState.Completed(
                absolutePath = target.absolutePath,
                sizeBytes = target.length(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        return connection
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE_BYTES = 128 * 1024
        private const val PROGRESS_STEP_BYTES = 16L * 1024L * 1024L
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
