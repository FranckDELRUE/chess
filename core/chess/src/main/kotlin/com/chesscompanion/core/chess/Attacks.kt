package com.chesscompanion.core.chess

/**
 * Attack detection on a 0x88 grid (ADR 0006): index88 = rank * 16 + file, and any index with
 * `index and 0x88 != 0` is off the board, so a single test stops rays and jumps at the edges.
 */
internal object Board0x88 {
    const val OFF_BOARD = 0x88
    const val RANK_STEP = 16
    private const val FILE_MASK = 7
    private const val RANK_SHIFT_64 = 3
    private const val RANK_SHIFT_88 = 4

    val KNIGHT = intArrayOf(33, 31, 18, 14, -14, -18, -31, -33)
    val KING = intArrayOf(1, -1, 16, -16, 15, 17, -15, -17)
    val ORTHOGONAL = intArrayOf(1, -1, 16, -16)
    val DIAGONAL = intArrayOf(15, 17, -15, -17)

    /** Offsets from a white pawn to the squares it attacks (negated for Black). */
    val WHITE_PAWN_CAPTURES = intArrayOf(15, 17)

    fun to88(index: Int): Int = (index shr RANK_SHIFT_64) * RANK_STEP + (index and FILE_MASK)

    fun from88(index88: Int): Int = (index88 shr RANK_SHIFT_88) * Board.SIZE + (index88 and FILE_MASK)

    fun rankOf(index: Int): Int = index shr RANK_SHIFT_64

    fun onBoard(index88: Int): Boolean = index88 and OFF_BOARD == 0
}

object Attacks {
    /** True if [square] is attacked by a piece of side [by] (pins are ignored, as in the rules). */
    fun isAttacked(
        board: Board,
        square: Square,
        by: Side,
    ): Boolean {
        val target = Board0x88.to88(square.index)
        return pawnAttacks(board, target, by) ||
            jumpAttacks(board, target, by, Board0x88.KNIGHT, PieceType.KNIGHT) ||
            jumpAttacks(board, target, by, Board0x88.KING, PieceType.KING) ||
            rayAttacks(board, target, by, Board0x88.ORTHOGONAL, PieceType.ROOK) ||
            rayAttacks(board, target, by, Board0x88.DIAGONAL, PieceType.BISHOP)
    }

    /** True if the king of [side] is attacked. */
    fun isInCheck(
        board: Board,
        side: Side,
    ): Boolean {
        val king = board.kingSquare(side) ?: return false
        return isAttacked(board, king, side.opposite)
    }

    private fun pawnAttacks(
        board: Board,
        target: Int,
        by: Side,
    ): Boolean {
        // A white pawn on x attacks x + 15 and x + 17, so it stands on target - 15 or target - 17.
        val direction = if (by == Side.WHITE) -1 else 1
        val pawn = Piece(PieceType.PAWN, by)
        return Board0x88.WHITE_PAWN_CAPTURES.any { offset ->
            val from = target + direction * offset
            Board0x88.onBoard(from) && board.at(Board0x88.from88(from)) == pawn
        }
    }

    private fun jumpAttacks(
        board: Board,
        target: Int,
        by: Side,
        offsets: IntArray,
        type: PieceType,
    ): Boolean {
        val attacker = Piece(type, by)
        return offsets.any { offset ->
            val from = target + offset
            Board0x88.onBoard(from) && board.at(Board0x88.from88(from)) == attacker
        }
    }

    /** [slider] is the rook or the bishop; the queen attacks along both kinds of ray. */
    private fun rayAttacks(
        board: Board,
        target: Int,
        by: Side,
        offsets: IntArray,
        slider: PieceType,
    ): Boolean =
        offsets.any { offset ->
            var from = target + offset
            var blocker: Piece? = null
            while (Board0x88.onBoard(from) && blocker == null) {
                blocker = board.at(Board0x88.from88(from))
                from += offset
            }
            blocker != null && blocker.side == by && (blocker.type == slider || blocker.type == PieceType.QUEEN)
        }
}
