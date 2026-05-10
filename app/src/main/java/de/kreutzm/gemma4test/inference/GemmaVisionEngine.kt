package de.kreutzm.gemma4test.inference

import android.content.Context
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

@OptIn(ExperimentalApi::class)
class GemmaVisionEngine(
    private val context: Context,
    private val modelPath: String,
    private val config: GemmaInferenceConfig = GemmaInferenceConfig(),
) : AutoCloseable {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (engine != null && conversation != null) return@runCatching

            val (textBackend, visionBackend) = config.backendMode.toLiteRtBackends()
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
            engine?.close()
            engine = null
        }
    }

    private fun GemmaBackendMode.toLiteRtBackends(): Pair<Backend, Backend> = when (this) {
        GemmaBackendMode.GpuTextGpuVision -> Backend.GPU() to Backend.GPU()
        GemmaBackendMode.CpuTextCpuVision -> Backend.CPU() to Backend.CPU()
    }
}
