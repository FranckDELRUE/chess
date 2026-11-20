package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Square

/** What an input gesture produced: the new state and, when a move was completed, that move. */
internal data class DrillInputStep(
    val input: DrillInput,
    val played: Move? = null,
)

/**
 * Entering the answer on the board, as an immutable state machine: a tap selects a piece of the side to
 * move or plays to a target, a drop plays directly, a pawn reaching the last rank waits for its piece.
 * It never changes [position]. (Same behaviour as the editor's input; to extract once a third screen needs it.)
 */
internal data class DrillInput(
    val position: Position,
    val selected: Square? = null,
    val pendingPromotion: Pair<Square, Square>? = null,
) {
    fun tap(square: Square): DrillInputStep {
        val from = selected
        return when {
            pendingPromotion != null -> DrillInputStep(this)
            from != null && square in targetsOf(from) -> moveOrPromote(from, square)
            square == from -> DrillInputStep(copy(selected = null))
            ownsPiece(square) -> DrillInputStep(copy(selected = square))
            else -> DrillInputStep(copy(selected = null))
        }
    }

    fun dragStart(square: Square): DrillInputStep =
        if (pendingPromotion != null || !ownsPiece(square)) {
            DrillInputStep(this)
        } else {
            DrillInputStep(copy(selected = square))
        }

    fun drop(
        from: Square,
        to: Square,
    ): DrillInputStep = if (pendingPromotion != null) DrillInputStep(this) else moveOrPromote(from, to)

    fun promote(type: PieceType): DrillInputStep {
        val (from, to) = pendingPromotion ?: return DrillInputStep(this)
        return DrillInputStep(copy(selected = null, pendingPromotion = null), Move(from, to, type))
    }

    fun cancelPromotion(): DrillInput = copy(selected = null, pendingPromotion = null)

    fun targetsOf(from: Square): Set<Square> = position.legalMoves.filter { it.from == from }.mapTo(HashSet()) { it.to }

    private fun moveOrPromote(
        from: Square,
        to: Square,
    ): DrillInputStep {
        val candidates = position.legalMoves.filter { it.from == from && it.to == to }
        return when (candidates.size) {
            0 -> DrillInputStep(copy(selected = null))
            1 -> DrillInputStep(copy(selected = null), candidates.single())
            else -> DrillInputStep(copy(pendingPromotion = from to to))
        }
    }

    private fun ownsPiece(square: Square): Boolean = position.board[square]?.side == position.sideToMove
}
