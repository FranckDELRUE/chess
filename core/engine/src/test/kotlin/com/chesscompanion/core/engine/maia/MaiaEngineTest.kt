package com.chesscompanion.core.engine.maia

import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.LegalMoveProvider
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MaiaEngineTest {
    private val fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"

    /** Prefers the illegal move a1h8, then e2e3 among the legal ones. */
    private val network =
        object : MaiaNetwork {
            override fun policy(planes: FloatArray) =
                FloatArray(Lc0PolicyMap.SIZE).also {
                    it[Lc0PolicyMap.moves.indexOf("a1h8")] = 9f
                    it[Lc0PolicyMap.moves.indexOf("e2e3")] = 3f
                    it[Lc0PolicyMap.moves.indexOf("e2e4")] = 2f
                }
        }
    private val legal = LegalMoveProvider { setOf("e2e3", "e2e4", "e1d1") }

    @Test
    fun `plays the most likely legal move`() =
        runTest {
            val event = MaiaEngine(network, legal).analyze(fen, 1).single()
            assertEquals(EngineEvent.BestMove("e2e3"), event)
        }

    @Test
    fun `reports when the raw arg-max was illegal`() =
        runTest {
            val evaluation = MaiaEngine(network, legal).evaluate(fen)
            assertFalse(evaluation.rawBestIsLegal)
            assertEquals(3, evaluation.priors.size)
        }

    @Test
    fun `no legal move gives none`() =
        runTest {
            val event = MaiaEngine(network, { emptySet() }).analyze(fen, 1).single()
            assertEquals(EngineEvent.BestMove("(none)"), event)
        }
}
