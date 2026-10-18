package com.chesscompanion.core.engine.maia

import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

/** A legal move and its probability under the network's policy. */
data class MovePrior(
    val move: String,
    val prior: Float,
)

/** Turns the network's 1858 policy logits into a distribution over the legal moves. */
object PolicyDecoder {
    /** Softmax over the logits of [legalMoves] only, sorted by decreasing prior. */
    fun decode(
        logits: FloatArray,
        fen: String,
        legalMoves: Collection<String>,
    ): List<MovePrior> {
        require(logits.size == Lc0PolicyMap.SIZE) { "Expected ${Lc0PolicyMap.SIZE} logits, got ${logits.size}" }
        if (legalMoves.isEmpty()) return emptyList()
        val moveLogits = policyIndices(fen, legalMoves).map { (move, index) -> move to logits[index] }
        val max = moveLogits.maxOf { it.second }
        val weights = moveLogits.map { (move, logit) -> move to exp((logit - max).toDouble()) }
        val total = weights.sumOf { it.second }
        return weights.map { (move, w) -> MovePrior(move, (w / total).toFloat()) }.sortedByDescending { it.prior }
    }

    /** lc0 policy index of each of [legalMoves] in the position [fen]. */
    fun policyIndices(
        fen: String,
        legalMoves: Collection<String>,
    ): Map<String, Int> {
        val position = FenFields.parse(fen)
        val black = position.sideToMove == Side.BLACK
        return legalMoves.associateWith { move ->
            val king = position.board.pieceAt(move[0] - 'a', move[1] - '1')?.type == PieceType.KING
            Lc0PolicyMap.indexOf(move, black, king)
        }
    }

    /**
     * Picks a move: the most likely at [temperature] 0 (Maia's usual play), otherwise a draw with
     * probabilities proportional to prior^(1 / temperature). Null when there is no move.
     */
    fun choose(
        priors: List<MovePrior>,
        temperature: Double,
        random: Random = Random.Default,
    ): String? {
        if (priors.isEmpty() || temperature <= 0.0) return priors.maxByOrNull { it.prior }?.move
        val weights = priors.map { it.prior.toDouble().pow(1.0 / temperature) }
        var draw = random.nextDouble() * weights.sum()
        val index = weights.indexOfFirst { w -> (draw - w).also { draw = it } <= 0.0 }
        return priors[if (index < 0) priors.lastIndex else index].move
    }
}
