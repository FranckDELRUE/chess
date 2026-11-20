package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Square

/** What an input gesture produced: the new state and, when a move was completed, that move. */
data class InputStep(
    val input: EditorInput,
    val played: Move? = null,
)

/**
 * Entering a move on the board of the editor, as an immutable state machine: a tap selects a
 * piece of the side to move or plays to a target, a drop plays directly, a pawn reaching the last
 * rank waits for its promotion piece. It never changes [position]: the caller applies the move.
 */
data class EditorInput(
    val position: Position,
    val selected: Square? = null,
    /** From and to squares of a promotion waiting for its piece. */
    val pendingPromotion: Pair<Square, Square>? = null,
) {
    fun tap(square: Square): InputStep {
        val from = selected
        return when {
            pendingPromotion != null -> InputStep(this)
            from != null && square in targetsOf(from) -> moveOrPromote(from, square)
            square == from -> InputStep(copy(selected = null))
            ownsPiece(square) -> InputStep(copy(selected = square))
            else -> InputStep(copy(selected = null))
        }
    }

    fun dragStart(square: Square): InputStep =
        if (pendingPromotion != null || !ownsPiece(square)) InputStep(this) else InputStep(copy(selected = square))

    fun drop(
        from: Square,
        to: Square,
    ): InputStep = if (pendingPromotion != null) InputStep(this) else moveOrPromote(from, to)

    fun promote(type: PieceType): InputStep {
        val (from, to) = pendingPromotion ?: return InputStep(this)
        return InputStep(copy(selected = null, pendingPromotion = null), Move(from, to, type))
    }

    fun cancelPromotion(): EditorInput = copy(selected = null, pendingPromotion = null)

    /** Squares the piece on [from] can move to. */
    fun targetsOf(from: Square): Set<Square> =
        position.legalMoves
            .filter { it.from == from }
            .mapTo(HashSet()) { it.to }

    private fun moveOrPromote(
        from: Square,
        to: Square,
    ): InputStep {
        val candidates = position.legalMoves.filter { it.from == from && it.to == to }
        return when (candidates.size) {
            0 -> InputStep(copy(selected = null))
            1 -> InputStep(copy(selected = null), candidates.single())
            else -> InputStep(copy(pendingPromotion = from to to))
        }
    }

    private fun ownsPiece(square: Square): Boolean = position.board[square]?.side == position.sideToMove
}
