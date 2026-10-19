package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ZobristTest {
    private fun key(fen: String) = Fen.parse(fen).zobristKey

    @Test
    fun `same position gives the same key whatever the counters`() {
        assertEquals(key(Fen.START), key("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 12 40"))
    }

    @Test
    fun `side to move changes the key`() {
        assertNotEquals(key("4k3/8/8/8/8/8/8/4K3 w - - 0 1"), key("4k3/8/8/8/8/8/8/4K3 b - - 0 1"))
    }

    @Test
    fun `castling rights change the key`() {
        assertNotEquals(key(Fen.START), key("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Kkq - 0 1"))
    }

    @Test
    fun `placement changes the key`() {
        assertNotEquals(key("4k3/8/8/8/8/8/8/4K3 w - - 0 1"), key("3k4/8/8/8/8/8/8/4K3 w - - 0 1"))
    }

    @Test
    fun `unusable en passant square does not change the key`() {
        assertEquals(
            key("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"),
            key("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"),
        )
    }

    @Test
    fun `capturable en passant square changes the key`() {
        val withEnPassant = Fen.parse("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3")
        assertEquals(true, withEnPassant.isEnPassantCapturable)
        assertNotEquals(
            key("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 3"),
            withEnPassant.zobristKey,
        )
    }

    @Test
    fun `keys are stable across runs`() {
        // Pinned value: a change here invalidates any stored key (repetition history, caches).
        assertEquals(key(Fen.START), Zobrist.key(Position.START))
        assertEquals(KNOWN_START_KEY, key(Fen.START))
    }

    private companion object {
        const val KNOWN_START_KEY = -3139988857650081677L
    }
}
