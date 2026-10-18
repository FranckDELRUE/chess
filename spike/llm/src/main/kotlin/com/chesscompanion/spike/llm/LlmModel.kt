package com.chesscompanion.spike.llm

/**
 * S0-6 candidates (GGUF Q4_K_M, Apache 2.0), fetched by `scripts/fetch-llm-models.ps1 -Push`
 * into the app's external files dir (`/sdcard/Android/data/com.chesscompanion/files/models`).
 */
enum class LlmModel(
    val label: String,
    val fileName: String,
    /** Appended after the chat template's assistant header. */
    val assistantPrefix: String,
) {
    /** Empty think block: Qwen3's official way to turn reasoning off (`enable_thinking=False`). */
    QWEN3_1_7B("Qwen3-1.7B", "qwen3-1.7b-q4_k_m.gguf", "<think>\n\n</think>\n\n"),
    EUROLLM_1_7B("EuroLLM-1.7B-Instruct", "eurollm-1.7b-instruct-q4_k_m.gguf", ""),
}
