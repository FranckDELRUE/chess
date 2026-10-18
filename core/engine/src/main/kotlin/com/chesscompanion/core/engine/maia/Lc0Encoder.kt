package com.chesscompanion.core.engine.maia

import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.FenParser
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side

/**
 * Encodes a FEN into lc0's `INPUT_CLASSICAL_112_PLANE` tensor (shape 112 x 8 x 8, flattened),
 * the input format of the Maia-1 networks. Mirrors `EncodePositionForNN` in lc0's `encoder.cc`:
 *
 * - the board is seen from the side to move ("ours"); for Black, ranks are mirrored;
 * - planes 13i..13i+12 hold history position i (6 of our pieces, 6 of theirs, repetition);
 * - only a FEN is known, so history is filled like lc0's default `fen_only`: the current position
 *   repeated (with the last double pawn push undone when there is an en passant square),
 *   except for the start position, whose history stays empty;
 * - planes 104-107 castling rights (our O-O-O, our O-O, their O-O-O, their O-O),
 *   108 Black to move, 109 rule-50 ply count (raw), 110 zeros, 111 ones.
 *
 * Square index in a plane: rank * 8 + file, a1 = 0, in the mover's perspective.
 */
object Lc0Encoder {
    const val PLANES = 112
    const val SQUARES = 64

    private const val HISTORY = 8
    private const val PLANES_PER_POSITION = 13
    private const val THEIR_PAWNS = 6
    private const val OUR_QUEENSIDE = 104
    private const val OUR_KINGSIDE = 105
    private const val THEIR_QUEENSIDE = 106
    private const val THEIR_KINGSIDE = 107
    private const val BLACK_TO_MOVE = 108
    private const val RULE_50 = 109
    private const val ONES = 111
    private const val MIRROR = 56
    private const val RANK_5 = 32
    private const val RANK_7 = 48
    private const val ALL = -1L
    private val pieceOrder =
        listOf(PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN, PieceType.KING)
    private val startBoard = FenParser.parseBoard(FenParser.START_POSITION)

    fun encode(fen: String): FloatArray {
        val position = FenFields.parse(fen)
        val planes = LongArray(PLANES)
        writeHistory(position, planes)
        writeAuxiliary(position, planes)

        val out = FloatArray(PLANES * SQUARES)
        planes.forEachIndexed { p, mask ->
            if (mask != 0L) for (sq in 0 until SQUARES) if (mask and bit(sq) != 0L) out[p * SQUARES + sq] = 1f
        }
        out.fill(position.halfMoves.toFloat(), RULE_50 * SQUARES, (RULE_50 + 1) * SQUARES)
        return out
    }

    private fun writeHistory(
        position: FenFields,
        planes: LongArray,
    ) {
        val current = piecePlanes(position.board, position.sideToMove)
        current.copyInto(planes)
        val isStart = position.board == startBoard && position.castling == "KQkq" && position.sideToMove == Side.WHITE
        if (isStart) return
        val filled = current.copyOf()
        position.enPassantFile?.let { file ->
            // The opponent just pushed a pawn from (their) rank 7 to rank 5: undo it.
            filled[THEIR_PAWNS] = (filled[THEIR_PAWNS] and bit(RANK_5 + file).inv()) or bit(RANK_7 + file)
        }
        for (i in 1 until HISTORY) filled.copyInto(planes, i * PLANES_PER_POSITION)
    }

    private fun writeAuxiliary(
        position: FenFields,
        planes: LongArray,
    ) {
        val white = position.sideToMove == Side.WHITE
        val rights = position.castling
        if ((if (white) 'Q' else 'q') in rights) planes[OUR_QUEENSIDE] = ALL
        if ((if (white) 'K' else 'k') in rights) planes[OUR_KINGSIDE] = ALL
        if ((if (white) 'q' else 'Q') in rights) planes[THEIR_QUEENSIDE] = ALL
        if ((if (white) 'k' else 'K') in rights) planes[THEIR_KINGSIDE] = ALL
        if (!white) planes[BLACK_TO_MOVE] = ALL
        planes[ONES] = ALL
    }

    /** 12 bitboards: our P N B R Q K then their P N B R Q K, mirrored when [us] is Black. */
    private fun piecePlanes(
        board: Board,
        us: Side,
    ): LongArray {
        val planes = LongArray(PLANES_PER_POSITION - 1)
        for (rank in 0 until Board.SIZE) {
            for (file in 0 until Board.SIZE) {
                val piece = board.pieceAt(file, rank) ?: continue
                val square = rank * Board.SIZE + file
                val ours = piece.side == us
                val index = pieceOrder.indexOf(piece.type) + if (ours) 0 else THEIR_PAWNS
                planes[index] = planes[index] or bit(if (us == Side.WHITE) square else square xor MIRROR)
            }
        }
        return planes
    }

    private fun bit(square: Int): Long = 1L shl square
}

/** The FEN fields the encoder needs. Only the placement is validated by `:core:chess` for now. */
internal class FenFields(
    val board: Board,
    val sideToMove: Side,
    val castling: String,
    val enPassantFile: Int?,
    val halfMoves: Int,
) {
    companion object {
        private const val SIDE = 1
        private const val CASTLING = 2
        private const val EN_PASSANT = 3
        private const val HALF_MOVES = 4

        fun parse(fen: String): FenFields {
            val fields = fen.trim().split(Regex("\\s+"))
            val side = if (fields.getOrNull(SIDE) == "b") Side.BLACK else Side.WHITE
            val enPassant = fields.getOrNull(EN_PASSANT)?.takeIf { it.length == 2 && it[0] in 'a'..'h' }
            return FenFields(
                board = FenParser.parseBoard(fen),
                sideToMove = side,
                castling = fields.getOrNull(CASTLING)?.takeIf { it != "-" }.orEmpty(),
                enPassantFile = enPassant?.let { it[0] - 'a' },
                halfMoves = fields.getOrNull(HALF_MOVES)?.toIntOrNull() ?: 0,
            )
        }
    }
}
