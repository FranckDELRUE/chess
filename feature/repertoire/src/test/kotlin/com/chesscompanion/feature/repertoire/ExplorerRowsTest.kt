package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.engine.Score
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExplorerRowsTest {
    @Test
    fun `scores are shown from the point of view of White`() {
        assertEquals("+0,35", formatEvaluation(Score.Centipawns(35), Side.WHITE))
        assertEquals("-0,35", formatEvaluation(Score.Centipawns(35), Side.BLACK))
        assertEquals("-1,20", formatEvaluation(Score.Centipawns(-120), Side.WHITE))
        assertEquals("+0,00", formatEvaluation(Score.Centipawns(0), Side.BLACK))
        assertEquals("#3", formatEvaluation(Score.Mate(3), Side.WHITE))
        assertEquals("-#3", formatEvaluation(Score.Mate(3), Side.BLACK))
        assertEquals("-#2", formatEvaluation(Score.Mate(-2), Side.WHITE))
    }
}
