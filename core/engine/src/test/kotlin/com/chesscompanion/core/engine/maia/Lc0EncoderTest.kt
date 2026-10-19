package com.chesscompanion.core.engine.maia

import com.chesscompanion.core.chess.Fen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Lc0EncoderTest {
    private fun FloatArray.plane(p: Int): List<Float> = toList().subList(p * 64, (p + 1) * 64)

    private fun FloatArray.squares(p: Int): Set<Int> =
        plane(p)
            .withIndex()
            .filter {
                it.value == 1f
            }.map { it.index }
            .toSet()

    private fun FloatArray.isFilled(
        p: Int,
        value: Float = 1f,
    ) = plane(p).all { it == value }

    @Test
    fun `start position has only the current board and no history`() {
        val planes = Lc0Encoder.encode(Fen.START)
        assertEquals(Lc0Encoder.PLANES * 64, planes.size)
        assertEquals((8..15).toSet(), planes.squares(0)) // our pawns on rank 2
        assertEquals(setOf(4), planes.squares(5)) // our king e1
        assertEquals((48..55).toSet(), planes.squares(6)) // their pawns on rank 7
        assertEquals(setOf(60), planes.squares(11)) // their king e8
        (13 until 104).forEach { assertEquals(emptySet<Int>(), planes.squares(it), "plane $it") }
        (104..107).forEach { assert(planes.isFilled(it)) }
        assert(planes.isFilled(108, 0f)) // White to move
        assert(planes.isFilled(110, 0f))
        assert(planes.isFilled(111))
    }

    @Test
    fun `black to move is mirrored and other positions fill the history`() {
        val fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R b KQkq - 0 5"
        val planes = Lc0Encoder.encode(fen)
        assertEquals(setOf(4), planes.squares(5)) // our (black) king e8 seen as e1
        assertEquals(setOf(60), planes.squares(11)) // their king e1 seen as e8
        assertEquals(setOf(2 * 8 + 2, 2 * 8 + 5), planes.squares(1)) // knights c6 f6 → c3 f3
        (1 until 8).forEach { i -> (0 until 12).forEach { assertEquals(planes.plane(it), planes.plane(i * 13 + it)) } }
        assert(planes.isFilled(108))
    }

    @Test
    fun `castling rights are ours then theirs, queenside first`() {
        val planes = Lc0Encoder.encode("r3k2r/8/8/8/8/8/8/R3K2R b Kq - 0 1")
        assert(planes.isFilled(104)) // our (black) O-O-O
        assert(planes.isFilled(105, 0f))
        assert(planes.isFilled(106, 0f))
        assert(planes.isFilled(107)) // their (white) O-O
    }

    @Test
    fun `rule 50 plane holds the raw half-move count`() {
        assert(Lc0Encoder.encode("8/8/3k4/8/2Q5/8/5K2/1q6 w - - 10 60").isFilled(109, 10f))
    }

    @Test
    fun `history undoes the double pawn push behind an en passant square`() {
        val planes = Lc0Encoder.encode("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        assert(4 * 8 + 4 in planes.squares(6)) // current: white e4 pawn seen as e5
        val history = planes.squares(13 + 6)
        assert(4 * 8 + 4 !in history && 6 * 8 + 4 in history) // history: back on e2, seen as e7
    }
}
