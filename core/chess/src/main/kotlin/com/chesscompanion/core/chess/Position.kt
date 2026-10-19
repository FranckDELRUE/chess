package com.chesscompanion.core.chess

open class InvalidPositionException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/**
 * A complete, immutable chess position: what a FEN describes. The constructor rejects
 * inconsistent positions (see [PositionValidator]).
 *
 * Not yet checked (S1-2, needs attack detection): the side not to move must not be in check.
 */
data class Position(
    val board: Board,
    val sideToMove: Side,
    val castling: CastlingRights,
    val enPassant: Square?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
) {
    init {
        PositionValidator.firstError(this)?.let { throw InvalidPositionException(it) }
    }

    /** Zobrist key: equal for positions that are the same for the repetition rule. */
    val zobristKey: Long by lazy { Zobrist.key(this) }

    /** True if a pawn of the side to move stands next to the pawn that just advanced two squares. */
    val isEnPassantCapturable: Boolean
        get() {
            val target = enPassant ?: return false
            val pawnRank = if (sideToMove == Side.WHITE) target.rank - 1 else target.rank + 1
            val ownPawn = Piece(PieceType.PAWN, sideToMove)
            return listOf(target.file - 1, target.file + 1)
                .filter { it in 0 until Board.SIZE }
                .any { board.pieceAt(it, pawnRank) == ownPawn }
        }

    fun toFen(): String = Fen.write(this)

    override fun toString(): String = toFen()

    companion object {
        val START: Position by lazy { Fen.parse(Fen.START) }
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

    private const val KING_FILE = 4
}
