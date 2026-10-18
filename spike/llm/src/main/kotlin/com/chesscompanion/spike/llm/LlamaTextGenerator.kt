package com.chesscompanion.spike.llm

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** llama.cpp on the CPU behind [TextGenerator]. One model and one context per instance. */
class LlamaTextGenerator private constructor(
    private val native: LlamaNative,
    private val handle: Long,
    private val assistantPrefix: String,
    private val dispatcher: CoroutineDispatcher,
) : TextGenerator {
    private val lock = Mutex()

    override fun generate(
        messages: List<ChatMessage>,
        params: GenerationParams,
    ): Flow<GenerationEvent> =
        callbackFlow {
            launch(dispatcher) {
                lock.withLock {
                    val decoder = Utf8StreamDecoder()
                    val raw =
                        native.nativeGenerate(
                            handle,
                            messages.map { it.role.wire }.toTypedArray(),
                            messages.map { it.content }.toTypedArray(),
                            assistantPrefix,
                            params.maxTokens,
                            params.temperature,
                            params.seed,
                        ) { bytes ->
                            val text = decoder.decode(bytes)
                            text.isEmpty() || trySend(GenerationEvent.Token(text)).isSuccess
                        }
                    val tail = decoder.finish()
                    if (tail.isNotEmpty()) send(GenerationEvent.Token(tail))
                    if (raw == null) {
                        close(IllegalStateException("Generation failed (see Logcat, tag LLM_JNI)"))
                    } else {
                        send(GenerationEvent.Done(GenerationStats.fromNative(raw)))
                        close()
                    }
                }
            }
            awaitClose { native.nativeCancel(handle) }
        }.buffer(Channel.UNLIMITED)

    suspend fun setThreads(threads: Int) = lock.withLock { native.nativeSetThreads(handle, threads) }

    override fun close() = native.nativeFree(handle)

    companion object {
        const val DEFAULT_THREADS = 4
        const val DEFAULT_CONTEXT = 1024

        fun systemInfo(): String = LlamaNative().nativeSystemInfo()

        /** Loads [model] from [dir]; the file is memory-mapped, not copied. */
        suspend fun load(
            dir: File,
            model: LlmModel,
            threads: Int = DEFAULT_THREADS,
            contextTokens: Int = DEFAULT_CONTEXT,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): LlamaTextGenerator {
            val file = File(dir, model.fileName)
            require(file.isFile) { "Missing ${file.absolutePath} (run scripts/fetch-llm-models.ps1 -Push)" }
            val native = LlamaNative()
            val handle = withContext(Dispatchers.IO) { native.nativeLoad(file.absolutePath, threads, contextTokens) }
            check(handle != 0L) { "llama.cpp cannot load ${model.fileName}" }
            return LlamaTextGenerator(native, handle, model.assistantPrefix, dispatcher)
        }
    }
}
