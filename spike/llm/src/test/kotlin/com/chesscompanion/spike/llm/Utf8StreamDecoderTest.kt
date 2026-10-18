package com.chesscompanion.spike.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Utf8StreamDecoderTest {
    private fun decodeChunks(chunks: List<ByteArray>): List<String> {
        val decoder = Utf8StreamDecoder()
        return chunks.map { decoder.decode(it) } + decoder.finish()
    }

    @Test
    fun `ascii chunks pass through`() {
        assertEquals(listOf("Le ", "cavalier", ""), decodeChunks(listOf("Le ".toByteArray(), "cavalier".toByteArray())))
    }

    @Test
    fun `accented letter split across two tokens is held back`() {
        val bytes = "é".toByteArray() // C3 A9
        val out = decodeChunks(listOf("d".toByteArray() + bytes[0], byteArrayOf(bytes[1]) + "fendu".toByteArray()))
        assertEquals(listOf("d", "éfendu", ""), out)
    }

    @Test
    fun `four-byte character split in three chunks`() {
        val bytes = "♞🙂".toByteArray() // 3 bytes + 4 bytes
        val out = decodeChunks(listOf(bytes.copyOfRange(0, 4), bytes.copyOfRange(4, 6), bytes.copyOfRange(6, 7)))
        assertEquals(listOf("♞", "", "🙂", ""), out)
    }

    @Test
    fun `incomplete character at end of stream becomes replacement char`() {
        val decoder = Utf8StreamDecoder()
        assertEquals("a", decoder.decode(byteArrayOf('a'.code.toByte(), 0xC3.toByte())))
        assertEquals("�", decoder.finish())
    }

    @Test
    fun `stray continuation bytes are not held back`() {
        val decoder = Utf8StreamDecoder()
        assertEquals("a�", decoder.decode(byteArrayOf('a'.code.toByte(), 0xA9.toByte())))
        assertEquals("", decoder.finish())
    }
}
