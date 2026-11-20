package com.chesscompanion.core.chess

class InvalidFenException(
    message: String,
    cause: Throwable? = null,
) : InvalidPositionException(message, cause)

/** Forsyth-Edwards Notation: strict reader and canonical writer (ADR 0006). */
object Fen {
    const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    private const val FIELDS = 6
    private const val EPD_FIELDS = 4

    /**
     * Parses a full FEN (6 fields) or its first 4 fields (counters default to 0 and 1).
     * Throws [InvalidFenException] if the text is malformed or the position is inconsistent.
     */
    fun parse(fen: String): Position {
        val fields = fen.trim().split(Regex("\\s+"))
        if (fields.size != FIELDS && fields.size != EPD_FIELDS) {
            throw InvalidFenException("Expected $FIELDS fields (or $EPD_FIELDS), got ${fields.size}: '$fen'")
        }
        val board = parseBoard(fields[0])
        val side = parseSide(fields[1])
        val castling = parseCastling(fields[2])
        val enPassant = parseEnPassant(fields[3])
        val halfmove = if (fields.size == FIELDS) parseCounter(fields[4], "halfmove clock") else 0
        val fullmove = if (fields.size == FIELDS) parseCounter(fields[5], "fullmove number") else 1
        return try {
            Position.of(board, side, castling, enPassant, halfmove, fullmove)
        } catch (e: InvalidPositionException) {
            throw InvalidFenException(e.message ?: "Invalid position: '$fen'", e)
        }
    }

    /** Parses the piece placement field only (any following fields are ignored and not validated). */
    fun parseBoard(fen: String): Board {
        val ranks = fen.trim().substringBefore(' ').split('/')
        if (ranks.size != Board.SIZE) throw InvalidFenException("Expected 8 ranks, got ${ranks.size}")

        val squares = arrayOfNulls<Piece>(Square.COUNT)
        // FEN lists rank 8 first.
        ranks.forEachIndexed { index, rankText ->
            parseRank(rankText, squares, (Board.SIZE - 1 - index) * Board.SIZE)
        }
        return Board(squares.toList())
    }

    fun write(position: Position): String =
        listOf(
            writeBoard(position.board),
            if (position.sideToMove == Side.WHITE) "w" else "b",
            position.castling.toFen(),
            position.enPassant?.name ?: "-",
            position.halfmoveClock.toString(),
            position.fullmoveNumber.toString(),
        ).joinToString(" ")

    fun writeBoard(board: Board): String =
        (Board.SIZE - 1 downTo 0).joinToString("/") { rank ->
            buildString {
                var empty = 0
                for (file in 0 until Board.SIZE) {
                    val piece = board.pieceAt(file, rank)
                    if (piece == null) {
                        empty++
                    } else {
                        if (empty > 0) append(empty)
                        empty = 0
                        append(piece.fenChar)
                    }
                }
                if (empty > 0) append(empty)
            }
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
                val piece = Piece.fromFenChar(c) ?: throw InvalidFenException("Unknown piece '$c'")
                if (file < Board.SIZE) squares[offset + file] = piece
                file++
            }
        }
        if (file != Board.SIZE) throw InvalidFenException("Rank '$rankText' does not have 8 files")
    }

    private fun parseSide(field: String): Side =
        when (field) {
            "w" -> Side.WHITE
            "b" -> Side.BLACK
            else -> throw InvalidFenException("Side to move must be 'w' or 'b', got '$field'")
        }

    private fun parseCastling(field: String): CastlingRights =
        CastlingRights.parseFen(field) ?: throw InvalidFenException("Invalid castling field '$field'")

    private fun parseEnPassant(field: String): Square? =
        if (field == "-") {
            null
        } else {
            Square.parse(field) ?: throw InvalidFenException("Invalid en passant field '$field'")
        }

    private fun parseCounter(
        text: String,
        name: String,
    ): Int =
        text.takeIf { it.all(Char::isDigit) }?.toIntOrNull()
            ?: throw InvalidFenException("Invalid $name '$text'")
}
