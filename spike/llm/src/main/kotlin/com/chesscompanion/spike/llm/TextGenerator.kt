package com.chesscompanion.spike.llm

import kotlinx.coroutines.flow.Flow

/** On-device text generation, kept apart from [com.chesscompanion.core.engine.ChessEngine]: it never plays. */
interface TextGenerator : AutoCloseable {
    /** Streams one answer to [messages]; the flow ends with a single [GenerationEvent.Done]. */
    fun generate(
        messages: List<ChatMessage>,
        params: GenerationParams = GenerationParams(),
    ): Flow<GenerationEvent>
}

data class ChatMessage(
    val role: Role,
    val content: String,
) {
    enum class Role(
        val wire: String,
    ) {
        SYSTEM("system"),
        USER("user"),
    }
}

/** [temperature] 0 means greedy decoding. */
data class GenerationParams(
    val maxTokens: Int = 96,
    val temperature: Float = 0.7f,
    val seed: Int = 42,
)

sealed interface GenerationEvent {
    data class Token(
        val text: String,
    ) : GenerationEvent

    data class Done(
        val stats: GenerationStats,
    ) : GenerationEvent
}

/** Timings of one answer. Prefill = prompt processing; decode = the single-token steps after the first token. */
data class GenerationStats(
    val promptTokens: Int,
    val prefillMs: Double,
    val generatedTokens: Int,
    val decodeSteps: Int,
    val decodeMs: Double,
    val firstTokenMs: Double,
) {
    val prefillTokensPerSecond: Double get() = perSecond(promptTokens, prefillMs)
    val decodeTokensPerSecond: Double get() = perSecond(decodeSteps, decodeMs)

    companion object {
        /** Builds the stats from the native `[promptTokens, prefillUs, generated, decodeSteps, decodeUs, firstUs]`. */
        fun fromNative(raw: LongArray): GenerationStats {
            require(raw.size == NATIVE_FIELDS) { "Expected $NATIVE_FIELDS fields, got ${raw.size}" }
            return GenerationStats(
                promptTokens = raw[0].toInt(),
                prefillMs = raw[1] / US_PER_MS,
                generatedTokens = raw[2].toInt(),
                decodeSteps = raw[3].toInt(),
                decodeMs = raw[4] / US_PER_MS,
                firstTokenMs = raw[5] / US_PER_MS,
            )
        }

        private fun perSecond(
            count: Int,
            ms: Double,
        ): Double = if (ms > 0) count * MS_PER_S / ms else 0.0

        private const val NATIVE_FIELDS = 6
        private const val US_PER_MS = 1000.0
        private const val MS_PER_S = 1000.0
    }
}
