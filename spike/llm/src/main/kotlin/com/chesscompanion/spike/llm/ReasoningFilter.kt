package com.chesscompanion.spike.llm

/** Removes the `<think>…</think>` blocks of reasoning models (Qwen3), even an unclosed one. */
object ReasoningFilter {
    private val closed = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)
    private val open = Regex("<think>.*", RegexOption.DOT_MATCHES_ALL)

    fun strip(text: String): String = text.replace(closed, "").replace(open, "").trim()
}
