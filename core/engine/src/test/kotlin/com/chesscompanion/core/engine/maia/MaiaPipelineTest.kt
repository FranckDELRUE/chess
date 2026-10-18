package com.chesscompanion.core.engine.maia

import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.LegalMoveProvider
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs

/**
 * End to end against lc0: our encoder + ONNX Runtime + decoder must give lc0's own priors.
 * Skipped when the models are missing (CI): run `scripts/fetch-maia-nets.ps1` then
 * `python tools/maia2onnx/maia2onnx.py`.
 */
class MaiaPipelineTest {
    @ParameterizedTest
    @ValueSource(strings = ["1100", "1500", "1900"])
    fun `priors match lc0 on the 20 test positions`(net: String) =
        runTest {
            val file = MaiaReference.model(net)
            assumeTrue(file.exists(), "${file.path} missing")
            val reference = MaiaReference.rows.filter { it.net == net }.groupBy { it.fen }
            val legal = LegalMoveProvider { fen -> reference.getValue(fen).map { it.move }.toSet() }
            MaiaEngine(OnnxMaiaNetwork(file.readBytes()), legal).use { engine ->
                reference.forEach { (fen, rows) ->
                    val priors = engine.evaluate(fen).priors.associate { it.move to it.prior }
                    rows.forEach { row ->
                        // lc0 prints priors as percentages with 2 decimals.
                        val diff = abs(priors.getValue(row.move) - row.prior)
                        assert(diff < PRIOR_TOLERANCE) { "$net $fen ${row.move}: ${priors[row.move]} vs ${row.prior}" }
                    }
                    val best = (engine.analyze(fen, 1).single() as EngineEvent.BestMove).move
                    assertEquals(rows.maxBy { it.prior }.move, best, "$net $fen")
                }
            }
        }

    private suspend fun <T> MaiaEngine.use(block: suspend (MaiaEngine) -> T): T =
        try {
            block(this)
        } finally {
            close()
        }

    private companion object {
        const val PRIOR_TOLERANCE = 0.001f
    }
}
