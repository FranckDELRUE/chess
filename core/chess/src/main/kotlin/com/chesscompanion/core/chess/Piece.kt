package com.chesscompanion.core.chess

enum class PieceType(
    /** Lower-case letter used by FEN and UCI. */
    val letter: Char,
) {
    KING('k'),
    QUEEN('q'),
    ROOK('r'),
    BISHOP('b'),
    KNIGHT('n'),
    PAWN('p'),
    ;

    companion object {
        /** Pieces a pawn may promote to. */
        val PROMOTIONS = listOf(QUEEN, ROOK, BISHOP, KNIGHT)

        fun fromLetter(letter: Char): PieceType? = entries.firstOrNull { it.letter == letter.lowercaseChar() }
    }
}

data class Piece(
    val type: PieceType,
    val side: Side,
) {
    /** FEN letter: upper case for White, lower case for Black. */
    val fenChar: Char get() = if (side == Side.WHITE) type.letter.uppercaseChar() else type.letter

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val type = PieceType.fromLetter(c) ?: return null
            return Piece(type, if (c.isUpperCase()) Side.WHITE else Side.BLACK)
        }
    }
}
