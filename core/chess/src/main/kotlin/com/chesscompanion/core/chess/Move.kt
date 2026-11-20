package com.chesscompanion.core.chess

/**
 * A move from one square to another. Castling is written as the king's move (`e1g1`), as in UCI;
 * [promotion] is set only for a pawn reaching the last rank.
 */
data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
) {
    init {
        require(from != to) { "A move must change square: $from" }
        require(promotion == null || promotion in PieceType.PROMOTIONS) { "Invalid promotion piece: $promotion" }
    }

    /** UCI notation, e.g. `e2e4` or `e7e8q`. */
    val uci: String get() = "$from$to${promotion?.letter ?: ""}"

    override fun toString(): String = uci

    companion object {
        /** Parses UCI notation; returns null if the text is not a well-formed move (legality is not checked). */
        fun parseUci(text: String): Move? {
            if (text.length !in UCI_LENGTH..UCI_LENGTH + 1) return null
            val from = Square.parse(text.substring(0, 2))
            val to = Square.parse(text.substring(2, UCI_LENGTH))
            val promotion =
                text.getOrNull(UCI_LENGTH)?.let { c -> PieceType.fromLetter(c)?.takeIf { it in PieceType.PROMOTIONS } }
            val promotionValid = text.length == UCI_LENGTH || promotion != null
            if (from == null || to == null || !promotionValid) return null
            return if (from == to) null else Move(from, to, promotion)
        }

        private const val UCI_LENGTH = 4
    }
}
