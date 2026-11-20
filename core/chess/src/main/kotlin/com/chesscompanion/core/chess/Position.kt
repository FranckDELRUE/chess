package com.chesscompanion.core.chess

open class InvalidPositionException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

class IllegalMoveException(
    message: String,
) : IllegalArgumentException(message)

/**
 * A complete, immutable chess position: what a FEN describes. [of] rejects inconsistent
 * positions (see [PositionValidator]); [play] only ever produces legal successors.
 */
class Position private constructor(
    val board: Board,
    val sideToMove: Side,
    val castling: CastlingRights,
    /** Set after every two-square pawn advance, even when no capture is possible (FEN standard). */
    val enPassant: Square?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
    key: Long?,
) {
    /** Zobrist key: equal for positions that are the same for the repetition rule. */
    val zobristKey: Long = key ?: Zobrist.key(this)

    /** Legal moves, computed once on first use. */
    val legalMoves: List<Move> by lazy(LazyThreadSafetyMode.PUBLICATION) { MoveGenerator.legalMoves(this) }

    /** True if the side to move is in check. */
    val isCheck: Boolean get() = Attacks.isInCheck(board, sideToMove)

    /** True if a pawn of the side to move stands next to the pawn that just advanced two squares. */
    val isEnPassantCapturable: Boolean get() = isEnPassantCapturable(board, sideToMove, enPassant)

    fun isLegal(move: Move): Boolean = move in legalMoves

    /** Plays a legal move; throws [IllegalMoveException] otherwise. */
    fun play(move: Move): Position {
        if (move !in legalMoves) throw IllegalMoveException("Illegal move $move in ${toFen()}")
        return MoveGenerator.play(this, move)
    }

    fun toFen(): String = Fen.write(this)

    override fun equals(other: Any?): Boolean =
        other is Position &&
            other.board == board &&
            other.sideToMove == sideToMove &&
            other.castling == castling &&
            other.enPassant == enPassant &&
            other.halfmoveClock == halfmoveClock &&
            other.fullmoveNumber == fullmoveNumber

    override fun hashCode(): Int = zobristKey.hashCode() * 31 + halfmoveClock * 7 + fullmoveNumber

    override fun toString(): String = toFen()

    companion object {
        val START: Position by lazy { Fen.parse(Fen.START) }

        /** Builds a position; throws [InvalidPositionException] if it is inconsistent. */
        fun of(
            board: Board,
            sideToMove: Side,
            castling: CastlingRights,
            enPassant: Square?,
            halfmoveClock: Int,
            fullmoveNumber: Int,
        ): Position {
            val position = Position(board, sideToMove, castling, enPassant, halfmoveClock, fullmoveNumber, key = null)
            PositionValidator.firstError(position)?.let { throw InvalidPositionException(it) }
            return position
        }

        /** Successor built by [MoveGenerator.play]: already consistent, key computed incrementally. */
        internal fun trusted(
            board: Board,
            sideToMove: Side,
            castling: CastlingRights,
            enPassant: Square?,
            halfmoveClock: Int,
            fullmoveNumber: Int,
            zobristKey: Long,
        ): Position = Position(board, sideToMove, castling, enPassant, halfmoveClock, fullmoveNumber, zobristKey)

        internal fun isEnPassantCapturable(
            board: Board,
            sideToMove: Side,
            enPassant: Square?,
        ): Boolean {
            val target = enPassant ?: return false
            val pawnRank = if (sideToMove == Side.WHITE) target.rank - 1 else target.rank + 1
            val ownPawn = Piece(PieceType.PAWN, sideToMove)
            return (target.file > 0 && board.pieceAt(target.file - 1, pawnRank) == ownPawn) ||
                (target.file < Board.SIZE - 1 && board.pieceAt(target.file + 1, pawnRank) == ownPawn)
        }
    }
}

internal object PositionValidator {
    private const val MAX_PIECES_PER_SIDE = 16
    private const val MAX_PAWNS_PER_SIDE = 8

    fun firstError(position: Position): String? =
        countersError(position)
            ?: piecesError(position.board)
            ?: castlingError(position)
            ?: enPassantError(position)
            ?: checkError(position)

    private fun countersError(position: Position): String? =
        when {
            position.halfmoveClock < 0 -> "Halfmove clock must not be negative: ${position.halfmoveClock}"
            position.fullmoveNumber < 1 -> "Fullmove number must be at least 1: ${position.fullmoveNumber}"
            else -> null
        }

    private fun piecesError(board: Board): String? {
        val pieces = board.pieces()
        val misplacedPawn =
            pieces.firstOrNull { (square, piece) ->
                piece.type == PieceType.PAWN && (square.rank == 0 || square.rank == Board.SIZE - 1)
            }
        return Side.entries.firstNotNullOfOrNull { sideError(pieces, it) }
            ?: misplacedPawn?.let { "Pawn on the first or last rank: ${it.first}" }
    }

    private fun sideError(
        pieces: List<Pair<Square, Piece>>,
        side: Side,
    ): String? {
        val own = pieces.filter { it.second.side == side }
        val kings = own.count { it.second.type == PieceType.KING }
        val pawns = own.count { it.second.type == PieceType.PAWN }
        val name = side.name.lowercase()
        return when {
            kings != 1 -> "Expected one $name king, found $kings"
            own.size > MAX_PIECES_PER_SIDE -> "Too many $name pieces: ${own.size}"
            pawns > MAX_PAWNS_PER_SIDE -> "Too many $name pawns: $pawns"
            else -> null
        }
    }

    private fun castlingError(position: Position): String? {
        val board = position.board
        for (side in Side.entries) {
            val backRank = if (side == Side.WHITE) 0 else Board.SIZE - 1
            for (kingside in listOf(true, false)) {
                if (!position.castling.has(side, kingside)) continue
                val king = board[Square.of(KING_FILE, backRank)]
                val rook = board[Square.of(if (kingside) Board.SIZE - 1 else 0, backRank)]
                val label = "${side.name.lowercase()} ${if (kingside) "kingside" else "queenside"}"
                if (king != Piece(PieceType.KING, side)) return "Castling right $label without the king on its square"
                if (rook != Piece(PieceType.ROOK, side)) return "Castling right $label without the rook on its square"
            }
        }
        return null
    }

    private fun enPassantError(position: Position): String? {
        val target = position.enPassant ?: return null
        val mover = position.sideToMove.opposite
        // White to move: Black just played e.g. e7-e5, so the target is e6 (rank index 5).
        val expectedRank = if (mover == Side.BLACK) 5 else 2
        val step = if (mover == Side.BLACK) -1 else 1
        val board = position.board
        return when {
            target.rank != expectedRank -> "En passant square $target is not on the expected rank"
            board[target] != null -> "En passant square $target is occupied"
            board.pieceAt(target.file, target.rank - step) != null ->
                "En passant square $target: the pawn's starting square is occupied"
            board.pieceAt(target.file, target.rank + step) != Piece(PieceType.PAWN, mover) ->
                "En passant square $target without the pawn that just advanced"
            else -> null
        }
    }

    private fun checkError(position: Position): String? {
        val waiting = position.sideToMove.opposite
        return if (Attacks.isInCheck(position.board, waiting)) {
            "The ${waiting.name.lowercase()} king is in check but it is not ${waiting.name.lowercase()}'s move"
        } else {
            null
        }
    }

    private const val KING_FILE = 4
}
