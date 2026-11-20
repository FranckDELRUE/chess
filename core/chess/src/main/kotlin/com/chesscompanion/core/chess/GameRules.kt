package com.chesscompanion.core.chess

enum class DrawReason { THREEFOLD_REPETITION, FIFTY_MOVES, INSUFFICIENT_MATERIAL }

sealed interface GameStatus {
    data object Ongoing : GameStatus

    data class Checkmate(
        val winner: Side,
    ) : GameStatus

    data object Stalemate : GameStatus

    data class Draw(
        val reason: DrawReason,
    ) : GameStatus

    val isOver: Boolean get() = this != Ongoing
}

/**
 * End-of-game rules. Draws are declared automatically, without a claim (S1 planning decision),
 * and a checkmate delivered on the 100th halfmove still wins.
 */
object GameRules {
    const val FIFTY_MOVE_PLIES = 100
    private const val REPETITIONS_FOR_DRAW = 3

    /** [history]: Zobrist keys of the earlier positions of the game, oldest first, current one excluded. */
    fun status(
        position: Position,
        history: List<Long> = emptyList(),
    ): GameStatus =
        when {
            position.legalMoves.isEmpty() ->
                if (position.isCheck) GameStatus.Checkmate(position.sideToMove.opposite) else GameStatus.Stalemate
            isInsufficientMaterial(position.board) -> GameStatus.Draw(DrawReason.INSUFFICIENT_MATERIAL)
            position.halfmoveClock >= FIFTY_MOVE_PLIES -> GameStatus.Draw(DrawReason.FIFTY_MOVES)
            isThreefoldRepetition(position, history) -> GameStatus.Draw(DrawReason.THREEFOLD_REPETITION)
            else -> GameStatus.Ongoing
        }

    /** Only positions since the last pawn move or capture can repeat the current one. */
    fun isThreefoldRepetition(
        position: Position,
        history: List<Long>,
    ): Boolean {
        val earlier = history.takeLast(position.halfmoveClock).count { it == position.zobristKey }
        return earlier + 1 >= REPETITIONS_FOR_DRAW
    }

    /**
     * No sequence of legal moves can mate: bare kings, a single minor piece, or only bishops all on
     * squares of the same colour. King and knight against king and knight is not included (a mate exists).
     */
    fun isInsufficientMaterial(board: Board): Boolean {
        val others = board.pieces().filter { it.second.type != PieceType.KING }
        return when {
            others.isEmpty() -> true
            others.size == 1 -> others[0].second.type == PieceType.KNIGHT || others[0].second.type == PieceType.BISHOP
            others.all { it.second.type == PieceType.BISHOP } ->
                others.map { (square, _) -> (square.file + square.rank) % 2 }.distinct().size == 1
            else -> false
        }
    }
}
