package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlayMoveTest {
    private fun Position.play(vararg uci: String): Position =
        uci.fold(this) { position, text -> position.play(requireNotNull(Move.parseUci(text)) { text }) }

    private fun fen(text: String) = Fen.parse(text)

    @Test
    fun `double pawn push sets the en passant square and counters`() {
        val position = Position.START.play("e2e4")
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", position.toFen())
        val after = position.play("g8f6", "g1f3")
        assertEquals("rnbqkb1r/pppppppp/5n2/8/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 2 2", after.toFen())
    }

    @Test
    fun `en passant capture removes the pawn`() {
        val position = fen("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3").play("e5f6")
        assertEquals("rnbqkbnr/ppp1p1pp/5P2/3p4/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 3", position.toFen())
    }

    @Test
    fun `castling moves the rook and removes the rights`() {
        val start = fen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 3 10")
        assertEquals("r3k2r/8/8/8/8/8/8/R4RK1 b kq - 4 10", start.play("e1g1").toFen())
        assertEquals("2kr3r/8/8/8/8/8/8/R4RK1 w - - 5 11", start.play("e1g1", "e8c8").toFen())
    }

    @Test
    fun `rook moves and rook captures remove one castling right`() {
        val start = fen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertEquals("Kkq", start.play("a1b1").castling.toFen())
        assertEquals("Qq", start.play("h1h8").castling.toFen())
    }

    @Test
    fun `promotion replaces the pawn`() {
        val position = fen("8/4P3/8/8/8/k7/8/4K3 w - - 7 40").play("e7e8n")
        assertEquals("4N3/8/8/8/8/k7/8/4K3 b - - 0 40", position.toFen())
    }

    @Test
    fun `illegal moves are rejected`() {
        assertThrows<IllegalMoveException> { Position.START.play("e2e5") }
        assertThrows<IllegalMoveException> { Position.START.play("e1g1") }
        // Pinned knight.
        val pinned = fen("4k3/4r3/8/8/8/8/4N3/4K3 w - - 0 1")
        assertThrows<IllegalMoveException> { pinned.play("e2c3") }
        // Promotion piece is required on the last rank.
        assertThrows<IllegalMoveException> { fen("8/4P3/8/8/8/k7/8/4K3 w - - 0 1").play("e7e8") }
    }

    @Test
    fun `castling through an attacked square is illegal`() {
        val position = fen("4k3/8/8/8/8/8/5r2/R3K2R w KQ - 0 1")
        assertFalse(position.isLegal(Move.parseUci("e1g1")!!))
        assertTrue(position.isLegal(Move.parseUci("e1c1")!!))
    }

    @Test
    fun `check is detected`() {
        assertTrue(fen("4k3/8/8/8/8/8/8/4R1K1 b - - 0 1").isCheck)
        assertFalse(Position.START.isCheck)
    }

    @Test
    fun `fen with the side not to move in check is rejected`() {
        assertThrows<InvalidFenException> { fen("4k3/8/8/8/8/8/8/4R1K1 w - - 0 1") }
        assertThrows<InvalidFenException> { fen("8/8/8/3kK3/8/8/8/8 w - - 0 1") }
    }

    @Test
    fun `incremental zobrist key matches a full recomputation`() {
        listOf(
            Fen.START,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
        ).forEach { checkKeys(Fen.parse(it), depth = 3) }
    }

    private fun checkKeys(
        position: Position,
        depth: Int,
    ) {
        assertEquals(Zobrist.key(position), position.zobristKey, position.toFen())
        assertEquals(position, Fen.parse(position.toFen()))
        if (depth == 0) return
        position.legalMoves.forEach { checkKeys(position.play(it), depth - 1) }
    }
}
