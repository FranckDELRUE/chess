package com.chesscompanion.spike.llm

/** JNI bridge to `libllm_jni.so` (see `src/main/cpp/llm_jni.cpp`). Calls on a handle must not overlap. */
internal class LlamaNative {
    fun interface TokenCallback {
        /** Raw UTF-8 bytes of one token (a character may span two tokens). Returns false to stop. */
        fun onToken(bytes: ByteArray): Boolean
    }

    external fun nativeSystemInfo(): String

    /** Returns a session handle, or 0 if the model cannot be loaded. */
    external fun nativeLoad(
        path: String,
        threads: Int,
        nCtx: Int,
    ): Long

    external fun nativeSetThreads(
        handle: Long,
        threads: Int,
    )

    /** Thread-safe: asks a running [nativeGenerate] to stop after the current token. */
    external fun nativeCancel(handle: Long)

    /**
     * Blocks until the answer is complete. Returns
     * `[promptTokens, prefillUs, generatedTokens, decodeSteps, decodeUs, firstTokenUs]`, or null on error.
     */
    @Suppress("LongParameterList")
    external fun nativeGenerate(
        handle: Long,
        roles: Array<String>,
        contents: Array<String>,
        assistantPrefix: String,
        maxTokens: Int,
        temperature: Float,
        seed: Int,
        callback: TokenCallback,
    ): LongArray?

    external fun nativeFree(handle: Long)

    companion object {
        init {
            System.loadLibrary("llm_jni")
        }
    }
}
