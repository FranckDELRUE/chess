package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GameRulesTest {
    /** Plays UCI moves and returns the final position with the keys of the earlier ones. */
    private fun playLine(
        start: Position,
        vararg uci: String,
    ): Pair<Position, List<Long>> {
        val history = mutableListOf<Long>()
        var position = start
        for (text in uci) {
            history += position.zobristKey
            position = position.play(requireNotNull(Move.parseUci(text)))
        }
        return position to history
    }

    private fun status(fen: String) = GameRules.status(Fen.parse(fen))

    @Test
    fun `start position is ongoing`() {
        assertEquals(GameStatus.Ongoing, GameRules.status(Position.START))
        assertFalse(GameRules.status(Position.START).isOver)
    }

    @Test
    fun `fool's mate`() {
        val (position, history) = playLine(Position.START, "f2f3", "e7e5", "g2g4", "d8h4")
        assertEquals(GameStatus.Checkmate(Side.BLACK), GameRules.status(position, history))
    }

    @Test
    fun `back rank mate`() {
        assertEquals(GameStatus.Checkmate(Side.WHITE), status("3R2k1/5ppp/8/8/8/8/8/6K1 b - - 0 1"))
    }

    @Test
    fun stalemate() {
        assertEquals(GameStatus.Stalemate, status("k7/2Q5/1K6/8/8/8/8/8 b - - 0 1"))
    }

    @Test
    fun `fifty move rule`() {
        assertEquals(GameStatus.Ongoing, status("4k3/8/8/8/8/8/R7/4K3 w - - 99 80"))
        assertEquals(GameStatus.Draw(DrawReason.FIFTY_MOVES), status("4k3/8/8/8/8/8/R7/4K3 w - - 100 80"))
    }

    @Test
    fun `checkmate on the hundredth halfmove wins`() {
        assertEquals(GameStatus.Checkmate(Side.WHITE), status("3R2k1/5ppp/8/8/8/8/8/6K1 b - - 100 80"))
    }

    @Test
    fun `threefold repetition`() {
        val shuffle = arrayOf("g1f3", "g8f6", "f3g1", "f6g8")
        val (twice, twiceHistory) = playLine(Position.START, *shuffle)
        assertEquals(GameStatus.Ongoing, GameRules.status(twice, twiceHistory))
        val (thrice, history) = playLine(Position.START, *shuffle, *shuffle)
        assertEquals(GameStatus.Draw(DrawReason.THREEFOLD_REPETITION), GameRules.status(thrice, history))
    }

    @Test
    fun `repetition does not count positions before a pawn move`() {
        val shuffle = arrayOf("g1f3", "g8f6", "f3g1", "f6g8")
        val (position, history) = playLine(Position.START, *shuffle, "e2e4", "e7e5", *shuffle)
        // The start position occurred twice, but before the pawn moves: no repetition after them.
        assertFalse(GameRules.isThreefoldRepetition(position, history))
    }

    @Test
    fun `lost en passant right makes positions differ`() {
        // After 1.e4 Nf6 2.e5 d5, White may take en passant: that position differs from later repeats.
        val line = "e2e4 g8f6 e4e5 d7d5 g1f3 b8c6 f3g1 c6b8 g1f3 b8c6 f3g1 c6b8".split(" ")
        val (position, history) = playLine(Position.START, *line.toTypedArray())
        assertEquals(2, (history + position.zobristKey).count { it == position.zobristKey })
        assertFalse(GameRules.isThreefoldRepetition(position, history))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/8/4KN2 w - - 0 1",
            "4k3/8/8/8/8/8/8/4KB2 w - - 0 1",
            "4kb2/8/8/8/8/8/8/2B1K3 w - - 0 1",
            "4k3/8/8/8/8/8/8/B1B1K3 w - - 0 1",
        ],
    )
    fun `insufficient material`(fen: String) {
        assertTrue(GameRules.isInsufficientMaterial(Fen.parse(fen).board))
        assertEquals(GameStatus.Draw(DrawReason.INSUFFICIENT_MATERIAL), status(fen))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
            "4kn2/8/8/8/8/8/8/4KN2 w - - 0 1",
            "4k3/8/8/8/8/8/8/3NKN2 w - - 0 1",
            "4k3/8/8/8/8/8/8/2B1KB2 w - - 0 1",
            "4kb2/8/8/8/8/8/8/3BK3 w - - 0 1",
        ],
    )
    fun `sufficient material`(fen: String) {
        assertFalse(GameRules.isInsufficientMaterial(Fen.parse(fen).board))
    }
}
