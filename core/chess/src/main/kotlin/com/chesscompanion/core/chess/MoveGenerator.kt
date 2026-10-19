package com.chesscompanion.core.chess

import kotlin.math.abs

/**
 * Legal move generation (ADR 0006): pseudo-legal moves on the 0x88 grid, then each move is played
 * on a copy of the board and kept only if its own king is not attacked. This filter covers pins,
 * evasions and the horizontal pin of an en passant capture without special cases.
 */
internal object MoveGenerator {
    private const val QUEEN_FILE = 3
    private const val KING_FILE = 4
    private const val KINGSIDE_ROOK_TARGET_FILE = 5
    private const val KING_KINGSIDE_FILE = 6
    private const val KING_QUEENSIDE_FILE = 2

    fun legalMoves(position: Position): List<Move> =
        pseudoLegalMoves(position).filter { move ->
            !Attacks.isInCheck(Board(applyMove(position.board, move)), position.sideToMove)
        }

    /** Plays a move already known to be legal. */
    fun play(
        position: Position,
        move: Move,
    ): Position {
        val board = position.board
        val piece = checkNotNull(board[move.from]) { "No piece on ${move.from}" }
        val isPawn = piece.type == PieceType.PAWN
        val isCapture = board[move.to] != null || (isPawn && move.from.file != move.to.file)
        val newBoard = Board(applyMove(board, move))
        val side = position.sideToMove
        val castling = updatedCastling(position.castling, move)
        val enPassant =
            if (isPawn && abs(move.to.rank - move.from.rank) == 2) {
                Square.of(move.from.file, (move.from.rank + move.to.rank) / 2)
            } else {
                null
            }

        var key = position.zobristKey xor Zobrist.blackToMove
        for (index in 0 until Square.COUNT) {
            val before = board.at(index)
            val after = newBoard.at(index)
            if (before != after) {
                if (before != null) key = key xor Zobrist.piece(before, Square(index))
                if (after != null) key = key xor Zobrist.piece(after, Square(index))
            }
        }
        key = key xor Zobrist.castling(position.castling) xor Zobrist.castling(castling)
        position.enPassant
            ?.takeIf { position.isEnPassantCapturable }
            ?.let { key = key xor Zobrist.enPassantFile(it.file) }
        enPassant
            ?.takeIf { Position.isEnPassantCapturable(newBoard, side.opposite, it) }
            ?.let { key = key xor Zobrist.enPassantFile(it.file) }

        return Position.trusted(
            board = newBoard,
            sideToMove = side.opposite,
            castling = castling,
            enPassant = enPassant,
            halfmoveClock = if (isPawn || isCapture) 0 else position.halfmoveClock + 1,
            fullmoveNumber = if (side == Side.BLACK) position.fullmoveNumber + 1 else position.fullmoveNumber,
            zobristKey = key,
        )
    }

    /** New placement after [move]: handles en passant, castling (rook move) and promotion. */
    private fun applyMove(
        board: Board,
        move: Move,
    ): Array<Piece?> {
        val squares = board.copySquares()
        val piece = checkNotNull(squares[move.from.index]) { "No piece on ${move.from}" }
        val from = move.from
        val to = move.to
        if (piece.type == PieceType.PAWN && from.file != to.file && squares[to.index] == null) {
            squares[Square.of(to.file, from.rank).index] = null
        }
        if (piece.type == PieceType.KING && abs(to.file - from.file) == 2) {
            val kingside = to.file > from.file
            val rookFrom = Square.of(if (kingside) Board.SIZE - 1 else 0, from.rank)
            val rookTo = Square.of(if (kingside) KINGSIDE_ROOK_TARGET_FILE else QUEEN_FILE, from.rank)
            squares[rookTo.index] = squares[rookFrom.index]
            squares[rookFrom.index] = null
        }
        squares[to.index] = move.promotion?.let { Piece(it, piece.side) } ?: piece
        squares[from.index] = null
        return squares
    }

    private fun updatedCastling(
        rights: CastlingRights,
        move: Move,
    ): CastlingRights {
        var result = rights
        for (square in listOf(move.from, move.to)) {
            result =
                when (square) {
                    Square.E1 -> result.without(Side.WHITE)
                    Square.A1 -> result.without(Side.WHITE, kingside = false)
                    Square.H1 -> result.without(Side.WHITE, kingside = true)
                    Square.E8 -> result.without(Side.BLACK)
                    Square.A8 -> result.without(Side.BLACK, kingside = false)
                    Square.H8 -> result.without(Side.BLACK, kingside = true)
                    else -> result
                }
        }
        return result
    }

    private fun pseudoLegalMoves(position: Position): List<Move> {
        val moves = ArrayList<Move>(INITIAL_CAPACITY)
        val board = position.board
        for (index in 0 until Square.COUNT) {
            val piece = board.at(index)
            if (piece == null || piece.side != position.sideToMove) continue
            when (piece.type) {
                PieceType.PAWN -> pawnMoves(position, index, moves)
                PieceType.KNIGHT -> jumpMoves(board, index, piece.side, Board0x88.KNIGHT, moves)
                PieceType.KING -> jumpMoves(board, index, piece.side, Board0x88.KING, moves)
                PieceType.BISHOP -> rayMoves(board, index, piece.side, Board0x88.DIAGONAL, moves)
                PieceType.ROOK -> rayMoves(board, index, piece.side, Board0x88.ORTHOGONAL, moves)
                PieceType.QUEEN -> {
                    rayMoves(board, index, piece.side, Board0x88.DIAGONAL, moves)
                    rayMoves(board, index, piece.side, Board0x88.ORTHOGONAL, moves)
                }
            }
        }
        castlingMoves(position, moves)
        return moves
    }

    private fun pawnMoves(
        position: Position,
        from: Int,
        moves: MutableList<Move>,
    ) {
        val board = position.board
        val side = position.sideToMove
        val forward = if (side == Side.WHITE) Board0x88.RANK_STEP else -Board0x88.RANK_STEP
        val startRank = if (side == Side.WHITE) 1 else Board.SIZE - 2
        val from88 = Board0x88.to88(from)

        val one = from88 + forward
        if (Board0x88.onBoard(one) && board.at(Board0x88.from88(one)) == null) {
            addPawnMove(from, Board0x88.from88(one), moves)
            val two = one + forward
            if (Board0x88.rankOf(from) == startRank && board.at(Board0x88.from88(two)) == null) {
                moves += Move(Square(from), Square(Board0x88.from88(two)))
            }
        }
        for (capture in intArrayOf(forward - 1, forward + 1)) {
            val target88 = from88 + capture
            if (!Board0x88.onBoard(target88)) continue
            val target = Board0x88.from88(target88)
            val victim = board.at(target)
            if (victim != null && victim.side != side) {
                addPawnMove(from, target, moves)
            } else if (victim == null && position.enPassant?.index == target) {
                moves += Move(Square(from), Square(target))
            }
        }
    }

    private fun addPawnMove(
        from: Int,
        to: Int,
        moves: MutableList<Move>,
    ) {
        val lastRank = Board0x88.rankOf(to) == 0 || Board0x88.rankOf(to) == Board.SIZE - 1
        if (lastRank) {
            PieceType.PROMOTIONS.forEach { moves += Move(Square(from), Square(to), it) }
        } else {
            moves += Move(Square(from), Square(to))
        }
    }

    private fun jumpMoves(
        board: Board,
        from: Int,
        side: Side,
        offsets: IntArray,
        moves: MutableList<Move>,
    ) {
        val from88 = Board0x88.to88(from)
        for (offset in offsets) {
            val target88 = from88 + offset
            if (!Board0x88.onBoard(target88)) continue
            val target = Board0x88.from88(target88)
            val occupant = board.at(target)
            if (occupant == null || occupant.side != side) moves += Move(Square(from), Square(target))
        }
    }

    private fun rayMoves(
        board: Board,
        from: Int,
        side: Side,
        offsets: IntArray,
        moves: MutableList<Move>,
    ) {
        val from88 = Board0x88.to88(from)
        for (offset in offsets) {
            var target88 = from88 + offset
            while (Board0x88.onBoard(target88)) {
                val target = Board0x88.from88(target88)
                val occupant = board.at(target)
                if (occupant == null || occupant.side != side) moves += Move(Square(from), Square(target))
                if (occupant != null) break
                target88 += offset
            }
        }
    }

    /** Castling: rights, empty squares between king and rook, king neither in check nor crossing an attack. */
    private fun castlingMoves(
        position: Position,
        moves: MutableList<Move>,
    ) {
        val side = position.sideToMove
        val board = position.board
        val rank = if (side == Side.WHITE) 0 else Board.SIZE - 1
        val king = Square.of(KING_FILE, rank)
        val wings = CASTLING_WINGS.filter { position.castling.has(side, it.kingside) }
        if (wings.isEmpty() || Attacks.isAttacked(board, king, side.opposite)) return
        for (wing in wings) {
            val pathClear = wing.emptyFiles.all { board.pieceAt(it, rank) == null }
            // The king's destination square is checked by the legality filter.
            if (pathClear && !Attacks.isAttacked(board, Square.of(wing.crossedFile, rank), side.opposite)) {
                moves += Move(king, Square.of(wing.kingTargetFile, rank))
            }
        }
    }

    private class CastlingWing(
        val kingside: Boolean,
        val kingTargetFile: Int,
        val crossedFile: Int,
        val emptyFiles: List<Int>,
    )

    private val CASTLING_WINGS =
        listOf(
            CastlingWing(true, KING_KINGSIDE_FILE, KINGSIDE_ROOK_TARGET_FILE, listOf(5, 6)),
            CastlingWing(false, KING_QUEENSIDE_FILE, QUEEN_FILE, listOf(1, 2, 3)),
        )

    private const val INITIAL_CAPACITY = 48
}
