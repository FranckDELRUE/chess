package com.chesscompanion.spike.llm

/**
 * Decodes a UTF-8 byte stream that arrives in arbitrary chunks (one per token): a character split
 * across two tokens is held back until its last byte arrives. Invalid sequences become U+FFFD.
 */
class Utf8StreamDecoder {
    private var pending = ByteArray(0)

    fun decode(chunk: ByteArray): String {
        val bytes = pending + chunk
        val complete = completeLength(bytes)
        pending = bytes.copyOfRange(complete, bytes.size)
        return String(bytes, 0, complete, Charsets.UTF_8)
    }

    /** Flushes what is left at end of stream (an incomplete character becomes U+FFFD). */
    fun finish(): String = String(pending, Charsets.UTF_8).also { pending = ByteArray(0) }

    /** Length of the longest prefix that does not end inside a multi-byte character. */
    private fun completeLength(bytes: ByteArray): Int {
        var i = bytes.size - 1
        var continuation = 0
        while (i >= 0 && continuation < MAX_CONTINUATION && isContinuation(bytes[i])) {
            i--
            continuation++
        }
        if (i < 0) return bytes.size
        val needed = sequenceLength(bytes[i])
        return if (needed > continuation + 1) i else bytes.size
    }

    private fun isContinuation(b: Byte): Boolean = b.toInt() and CONT_MASK == CONT_BITS

    /** Byte count announced by a lead byte (110xxxxx: 2, 1110xxxx: 3, 11110xxx: 4), else 1. */
    private fun sequenceLength(lead: Byte): Int {
        val v = lead.toInt() and BYTE_MASK
        return LEADS.firstOrNull { (mask, bits, _) -> v and mask == bits }?.third ?: 1
    }

    private companion object {
        const val MAX_CONTINUATION = 3
        const val BYTE_MASK = 0xFF
        const val CONT_MASK = 0xC0
        const val CONT_BITS = 0x80
        val LEADS = listOf(Triple(0xE0, 0xC0, 2), Triple(0xF0, 0xE0, 3), Triple(0xF8, 0xF0, 4))
    }
}
