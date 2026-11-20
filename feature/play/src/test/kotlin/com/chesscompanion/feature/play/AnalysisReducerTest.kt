package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnalysisReducerTest {
    private val start = Fen.parse(Fen.START)
    private val blackToMove = Fen.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")

    private fun info(
        depth: Int,
        rank: Int,
        score: Score,
        vararg pv: String,
    ) = EngineEvent.Info(EngineInfo(depth = depth, multiPv = rank, score = score, pv = pv.toList()))

    private fun reduce(
        vararg events: EngineEvent,
        position: Position = start,
    ) = events.fold(AnalysisState(enabled = true, fen = position.toFen())) { s, e ->
        AnalysisReducer.reduce(s, e, position)
    }

    @Test
    fun `lines are kept by rank and written in french san`() {
        val state =
            reduce(
                info(10, 2, Score.Centipawns(20), "d2d4", "d7d5"),
                info(10, 1, Score.Centipawns(35), "g1f3", "g8f6", "b1c3"),
            )
        assertEquals(listOf(1, 2), state.lines.map { it.rank })
        assertEquals(listOf("Cf3", "Cf6", "Cc3"), state.best!!.moves)
        assertEquals(Move.parseUci("g1f3"), state.best!!.firstMove)
        assertEquals("1.Cf3 Cf6 2.Cc3", state.best!!.text())
        assertEquals(10, state.depth)
    }

    @Test
    fun `a shallower line never replaces a deeper one of the same rank`() {
        val state =
            reduce(
                info(12, 1, Score.Centipawns(30), "e2e4"),
                info(8, 1, Score.Centipawns(-90), "a2a3"),
            )
        assertEquals(12, state.best!!.depth)
        assertEquals(listOf("e4"), state.best!!.moves)
    }

    @Test
    fun `an equal or deeper line replaces it`() {
        val state =
            reduce(
                info(12, 1, Score.Centipawns(30), "e2e4"),
                info(12, 1, Score.Centipawns(33), "d2d4"),
                info(13, 1, Score.Centipawns(35), "g1f3"),
            )
        assertEquals(listOf("Cf3"), state.best!!.moves)
        assertEquals(1, state.lines.size)
    }

    @Test
    fun `scores are seen from white whoever is to move`() {
        val state = reduce(info(10, 1, Score.Centipawns(40), "e7e5"), position = blackToMove)
        assertEquals(Score.Centipawns(-40), state.best!!.score)
        assertTrue(state.whiteShare < 0.5f)
        val mate = reduce(info(10, 1, Score.Mate(2), "d7d5"), position = blackToMove)
        assertEquals(Score.Mate(-2), mate.best!!.score)
    }

    @Test
    fun `the first move of black is numbered with three dots`() {
        val state = reduce(info(10, 1, Score.Centipawns(0), "e7e5", "g1f3", "b8c6"), position = blackToMove)
        assertEquals("1...e5 2.Cf3 Cc6", state.best!!.text())
    }

    @Test
    fun `a variation is cut at the first illegal move`() {
        val state = reduce(info(10, 1, Score.Centipawns(0), "e2e4", "e7e5", "e4e6", "a7a6"))
        assertEquals(listOf("e4", "e5"), state.best!!.moves)
    }

    @Test
    fun `only the plies shown are converted to san`() {
        val pv = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6", "e1g1", "f8e7")
        val state = reduce(info(10, 1, Score.Centipawns(0), *pv.toTypedArray()))
        assertEquals(AnalysisLine.MAX_PLIES, state.best!!.moves.size)
    }

    @Test
    fun `an illegal or missing first move gives no line`() {
        val state = reduce(info(10, 1, Score.Centipawns(0), "e2e5"), info(10, 2, Score.Centipawns(0)))
        assertTrue(state.lines.isEmpty())
        assertEquals(AnalysisState.NEUTRAL, state.whiteShare)
    }

    @Test
    fun `best move finishes the search and a restart reopens it keeping the lines`() {
        val done = reduce(info(10, 1, Score.Centipawns(10), "e2e4"), EngineEvent.BestMove("e2e4"))
        assertTrue(done.finished)
        val restarted = AnalysisReducer.reduce(done, EngineEvent.Restarted, start)
        assertFalse(restarted.finished)
        assertEquals(done.lines, restarted.lines)
        // After the restart, shallow results do not replace the deeper lines already shown.
        val after = AnalysisReducer.reduce(restarted, info(3, 1, Score.Centipawns(-50), "a2a3"), start)
        assertEquals(10, after.depth)
    }

    @Test
    fun `scores are formatted for display`() {
        assertEquals("+0,4", formatScore(Score.Centipawns(40)))
        assertEquals("-1,2", formatScore(Score.Centipawns(-120)))
        assertEquals("+0,0", formatScore(Score.Centipawns(0)))
        assertEquals("M3", formatScore(Score.Mate(3)))
        assertEquals("-M2", formatScore(Score.Mate(-2)))
    }
}
