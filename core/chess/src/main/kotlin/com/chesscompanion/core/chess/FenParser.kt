package com.chesscompanion.core.chess

class InvalidFenException(
    message: String,
) : IllegalArgumentException(message)

/** Minimal FEN reader: only the piece placement field is interpreted for now. */
object FenParser {
    const val START_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    private val pieceByLetter =
        mapOf(
            'k' to PieceType.KING,
            'q' to PieceType.QUEEN,
            'r' to PieceType.ROOK,
            'b' to PieceType.BISHOP,
            'n' to PieceType.KNIGHT,
            'p' to PieceType.PAWN,
        )

    fun parseBoard(fen: String): Board {
        val ranks = fen.trim().substringBefore(' ').split('/')
        if (ranks.size != Board.SIZE) throw InvalidFenException("Expected 8 ranks, got ${ranks.size}")

        val squares = arrayOfNulls<Piece>(Board.SIZE * Board.SIZE)
        // FEN lists rank 8 first.
        ranks.forEachIndexed { index, rankText ->
            parseRank(rankText, squares, (Board.SIZE - 1 - index) * Board.SIZE)
        }
        return Board(squares.toList())
    }

    private fun parseRank(
        rankText: String,
        squares: Array<Piece?>,
        offset: Int,
    ) {
        var file = 0
        for (c in rankText) {
            if (c in '1'..'8') {
                file += c - '0'
            } else {
                val type = pieceByLetter[c.lowercaseChar()] ?: throw InvalidFenException("Unknown piece '$c'")
                if (file <
                    Board.SIZE
                ) {
                    squares[offset + file] = Piece(type, if (c.isUpperCase()) Side.WHITE else Side.BLACK)
                }
                file++
            }
        }
        if (file != Board.SIZE) throw InvalidFenException("Rank '$rankText' does not have 8 files")
    }
}
