package com.chesscompanion.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.exp

class EvaluationTest {
    private val start = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    /** Lichess formula, recomputed independently: 2 / (1 + e^(-0.00368208 * cp)) - 1. */
    private fun lichess(cp: Int) = 2 / (1 + exp(-0.00368208 * cp)) - 1

    @Test
    fun `score is seen from white`() {
        assertEquals(Score.Centipawns(35), Score.Centipawns(35).forWhite(whiteToMove = true))
        assertEquals(Score.Centipawns(-35), Score.Centipawns(35).forWhite(whiteToMove = false))
        assertEquals(Score.Mate(-3), Score.Mate(3).forWhite(whiteToMove = false))
    }

    @Test
    fun `side to move accounts for the moves of the request`() {
        assertTrue(SearchRequest(start, SearchLimit.Depth(1)).whiteToMove)
        assertFalse(SearchRequest(start, SearchLimit.Depth(1), moves = listOf("e2e4")).whiteToMove)
        assertTrue(SearchRequest(start, SearchLimit.Depth(1), moves = listOf("e2e4", "e7e5")).whiteToMove)
        assertFalse(SearchRequest(start.replace(" w ", " b "), SearchLimit.Depth(1)).whiteToMove)
    }

    @Test
    fun `winning chances follow the lichess sigmoid`() {
        assertEquals(0.0, Score.Centipawns(0).winningChances(), 1e-12)
        assertEquals(lichess(150), Score.Centipawns(150).winningChances(), 1e-12)
        assertEquals(-lichess(150), Score.Centipawns(-150).winningChances(), 1e-12)
        assertEquals(0.5, Score.Centipawns(0).winProbability(), 1e-12)
    }

    @Test
    fun `centipawns are capped at 1000`() {
        assertEquals(lichess(1000), Score.Centipawns(5000).winningChances(), 1e-12)
        assertEquals(lichess(-1000), Score.Centipawns(-5000).winningChances(), 1e-12)
    }

    @Test
    fun `mate scores beat any centipawn score`() {
        assertEquals(lichess(2000), Score.Mate(1).winningChances(), 1e-12)
        assertEquals(lichess(1100), Score.Mate(15).winningChances(), 1e-12)
        assertEquals(lichess(-2000), Score.Mate(-1).winningChances(), 1e-12)
        // Stockfish reports `mate 0` for the side that is checkmated.
        assertTrue(Score.Mate(0).winProbability() < 0.01)
        assertTrue(Score.Mate(15).winningChances() > Score.Centipawns(1000).winningChances())
    }

    @Test
    fun `strength bounds come from stockfish`() {
        EngineStrength.Elo(1320)
        EngineStrength.Elo(3190)
        EngineStrength.Skill(0)
        assertThrows<IllegalArgumentException> { EngineStrength.Elo(1319) }
        assertThrows<IllegalArgumentException> { EngineStrength.Skill(21) }
        assertThrows<IllegalArgumentException> { SearchRequest(start, SearchLimit.Depth(1), multiPv = 0) }
    }
}
