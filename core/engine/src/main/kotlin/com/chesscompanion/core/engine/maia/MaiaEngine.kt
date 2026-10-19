package com.chesscompanion.core.engine.maia

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.engine.ChessEngine
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.LegalMoveProvider
import com.chesscompanion.core.engine.RulesLegalMoveProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Policy of one position, with the time spent on each stage. */
data class MaiaEvaluation(
    val priors: List<MovePrior>,
    /** Whether the raw arg-max of the 1858 logits is legal, i.e. masking was not needed. */
    val rawBestIsLegal: Boolean,
    val legalMovesMs: Double,
    val inferenceMs: Double,
)

/**
 * Maia as a [ChessEngine]: one network evaluation, no search ("policy only", like lc0 `go nodes 1`).
 * `depth` is ignored. Option `Temperature` (default 0): 0 plays the most likely move, higher values
 * sample among the legal moves (see [PolicyDecoder.choose]). The FEN is validated by `:core:chess`
 * and the legal moves come from the rules by default (S1-5).
 */
class MaiaEngine(
    private val network: MaiaNetwork,
    private val legalMoves: LegalMoveProvider = RulesLegalMoveProvider,
    private val random: Random = Random.Default,
) : ChessEngine {
    @Volatile
    private var temperature = 0.0

    override suspend fun start(options: Map<String, String>) {
        options.forEach { (name, value) -> setOption(name, value) }
    }

    override suspend fun setOption(
        name: String,
        value: String,
    ) {
        if (name == TEMPERATURE) temperature = value.toDouble()
    }

    override suspend fun newGame() = Unit

    override fun analyze(
        fen: String,
        depth: Int,
    ): Flow<EngineEvent> =
        flow {
            val move = PolicyDecoder.choose(evaluate(fen).priors, temperature, random)
            emit(EngineEvent.BestMove(move ?: "(none)"))
        }

    /** Throws `InvalidFenException` for an invalid FEN (rule 2). */
    suspend fun evaluate(fen: String): MaiaEvaluation {
        val position = Fen.parse(fen)
        val t0 = System.nanoTime()
        val legal = legalMoves.legalMoves(fen)
        val t1 = System.nanoTime()
        val logits = withContext(Dispatchers.Default) { network.policy(Lc0Encoder.encode(position)) }
        val t2 = System.nanoTime()
        val raw = logits.indices.maxBy { logits[it] }
        return MaiaEvaluation(
            priors = PolicyDecoder.decode(logits, position, legal),
            rawBestIsLegal = raw in PolicyDecoder.policyIndices(position, legal).values,
            legalMovesMs = (t1 - t0) / NS_PER_MS,
            inferenceMs = (t2 - t1) / NS_PER_MS,
        )
    }

    override suspend fun close() = network.close()

    companion object {
        const val TEMPERATURE = "Temperature"
        private const val NS_PER_MS = 1e6
    }
}
