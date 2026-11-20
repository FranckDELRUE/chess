package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.GameSession

/**
 * Entering moves on the board, as an immutable state machine: a tap selects a piece of the side
 * to move or plays to a target, a drop plays directly, a pawn reaching the last rank waits for its
 * promotion piece. Nothing happens once the game is over or while a promotion is pending.
 */
data class MoveInput(
    val session: GameSession,
    val selected: Square? = null,
    /** From and to squares of a promotion waiting for its piece. */
    val pendingPromotion: Pair<Square, Square>? = null,
) {
    private val blocked: Boolean get() = pendingPromotion != null || session.status().isOver

    fun tap(square: Square): MoveInput {
        if (blocked) return this
        val from = selected
        return when {
            from != null && square in targetsOf(from) -> moveOrPromote(from, square)
            square == from -> copy(selected = null)
            ownsPiece(square) -> copy(selected = square)
            else -> copy(selected = null)
        }
    }

    fun dragStart(square: Square): MoveInput = if (blocked || !ownsPiece(square)) this else copy(selected = square)

    fun drop(
        from: Square,
        to: Square,
    ): MoveInput = if (blocked) this else moveOrPromote(from, to)

    fun promote(type: PieceType): MoveInput {
        val (from, to) = pendingPromotion ?: return this
        return MoveInput(session.play(Move(from, to, type)))
    }

    fun cancelPromotion(): MoveInput = copy(selected = null, pendingPromotion = null)

    /** Squares the piece on [from] can move to. */
    fun targetsOf(from: Square): Set<Square> =
        session.position.legalMoves
            .filter { it.from == from }
            .mapTo(HashSet()) { it.to }

    private fun moveOrPromote(
        from: Square,
        to: Square,
    ): MoveInput {
        val candidates = session.position.legalMoves.filter { it.from == from && it.to == to }
        return when (candidates.size) {
            0 -> copy(selected = null)
            1 -> MoveInput(session.play(candidates.single()))
            else -> copy(pendingPromotion = from to to)
        }
    }

    private fun ownsPiece(square: Square): Boolean = session.position.board[square]?.side == session.position.sideToMove
}
