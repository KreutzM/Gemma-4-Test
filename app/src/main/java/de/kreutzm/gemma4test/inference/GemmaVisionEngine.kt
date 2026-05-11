package de.kreutzm.gemma4test.inference

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val TAG = "GemmaVisionEngine"

@OptIn(ExperimentalApi::class)
class GemmaVisionEngine(
    private val context: Context,
    private val modelPath: String,
    private val config: GemmaInferenceConfig = GemmaInferenceConfig(),
) : AutoCloseable {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    var activeBackendMode: GemmaBackendMode? = null
        private set

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (engine != null && conversation != null) return@runCatching

            val modesToTry = config.backendPolicy.backendAttemptOrder()
            logRuntimeDiagnostics(modesToTry)

            var lastFailure: Throwable? = null
            for (mode in modesToTry) {
                try {
                    Log.i(TAG, "Trying LiteRT backend: ${mode.label}")
                    initializeWithBackendMode(mode)
                    activeBackendMode = mode
                    Log.i(TAG, "LiteRT backend initialized: ${mode.label}")
                    Log.i(TAG, "activeBackendMode=${mode.label}")
                    return@runCatching
                } catch (throwable: Throwable) {
                    lastFailure = throwable
                    Log.e(TAG, "LiteRT backend failed: ${mode.label}", throwable)
                    close()
                }
            }

            throw checkNotNull(lastFailure) { "No LiteRT-LM backend mode was attempted" }
        }
    }

    suspend fun describeImage(
        pngBytes: ByteArray,
        onPartialText: (String) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(pngBytes.isNotEmpty()) { "pngBytes must not be empty" }
            val activeConversation = checkNotNull(conversation) { "GemmaVisionEngine is not initialized" }
            suspendCancellableCoroutine { continuation ->
                val output = StringBuilder()
                activeConversation.sendMessageAsync(
                    Contents.of(
                        listOf(
                            Content.ImageBytes(pngBytes),
                            Content.Text(config.prompt),
                        ),
                    ),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            val chunk = message.toString()
                            output.append(chunk)
                            onPartialText(output.toString())
                        }

                        override fun onDone() {
                            if (continuation.isActive) {
                                continuation.resume(output.toString())
                            }
                        }

                        override fun onError(throwable: Throwable) {
                            if (continuation.isActive) {
                                continuation.resumeWith(Result.failure(throwable))
                            }
                        }
                    },
                    emptyMap(),
                )

                continuation.invokeOnCancellation {
                    activeConversation.cancelProcess()
                }
            }
        }
    }.recoverCatching { throwable ->
        if (throwable is CancellationException) throw throwable
        throw throwable
    }

    override fun close() {
        try {
            conversation?.close()
        } finally {
            conversation = null
            try {
                engine?.close()
            } finally {
                engine = null
                activeBackendMode = null
            }
        }
    }

    private fun initializeWithBackendMode(mode: GemmaBackendMode) {
        val (textBackend, visionBackend) = mode.toLiteRtBackends()
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = textBackend,
            visionBackend = visionBackend,
            maxNumTokens = config.maxTokens,
            maxNumImages = config.maxImages,
            cacheDir = context.cacheDir.absolutePath,
        )
        val initializedEngine = Engine(engineConfig)
        initializedEngine.initialize()
        val initializedConversation = initializedEngine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = config.topK,
                    topP = config.topP,
                    temperature = config.temperature,
                ),
            ),
        )
        engine = initializedEngine
        conversation = initializedConversation
    }

    private fun logRuntimeDiagnostics(modesToTry: List<GemmaBackendMode>) {
        Log.i(TAG, "modelPath=$modelPath")
        Log.i(TAG, "cacheDir=${context.cacheDir.absolutePath}")
        Log.i(TAG, "context.filesDir=${context.filesDir.absolutePath}")
        Log.i(TAG, "context.externalFilesDir=${context.getExternalFilesDir(null)?.absolutePath ?: "unavailable"}")
        Log.i(TAG, "nativeLibraryDir=${context.applicationInfo.nativeLibraryDir}")
        Log.i(TAG, "backendPolicy=${config.backendPolicy.label}")
        Log.i(TAG, "backend attempt order=${modesToTry.joinToString { it.label }}")
    }

    private fun GemmaBackendMode.toLiteRtBackends(): Pair<Backend, Backend> = when (this) {
        GemmaBackendMode.GpuTextGpuVision -> Backend.GPU() to Backend.GPU()
        GemmaBackendMode.CpuTextCpuVision -> Backend.CPU() to Backend.CPU()
    }
}
