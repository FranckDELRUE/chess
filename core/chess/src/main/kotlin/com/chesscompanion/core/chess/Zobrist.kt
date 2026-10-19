package com.chesscompanion.core.chess

/**
 * Zobrist hashing (ADR 0006). The random numbers come from a fixed-seed SplitMix64, so keys are
 * stable across runs and devices; they are not Polyglot keys.
 *
 * The en passant file is hashed only when the capture is possible for the side to move, so two
 * positions that differ only by an unusable en passant square share the same key.
 */
object Zobrist {
    private const val SEED = 0x43484553534C4F47L // "CHESSLOG"
    private const val PIECE_KINDS = 12
    private const val CASTLING_STATES = 16

    private val numbers: LongArray =
        SplitMix64(SEED).let { random ->
            LongArray(PIECE_KINDS * Square.COUNT + 1 + CASTLING_STATES + Board.SIZE) { random.next() }
        }

    private const val SIDE_OFFSET = PIECE_KINDS * Square.COUNT
    private const val CASTLING_OFFSET = SIDE_OFFSET + 1
    private const val EN_PASSANT_OFFSET = CASTLING_OFFSET + CASTLING_STATES

    fun piece(
        piece: Piece,
        square: Square,
    ): Long = numbers[(piece.type.ordinal * 2 + piece.side.ordinal) * Square.COUNT + square.index]

    /** Toggled when Black is to move. */
    val blackToMove: Long get() = numbers[SIDE_OFFSET]

    fun castling(rights: CastlingRights): Long = numbers[CASTLING_OFFSET + rights.bits]

    fun enPassantFile(file: Int): Long = numbers[EN_PASSANT_OFFSET + file]

    fun key(position: Position): Long {
        var key = 0L
        for ((square, piece) in position.board.pieces()) key = key xor piece(piece, square)
        if (position.sideToMove == Side.BLACK) key = key xor blackToMove
        key = key xor castling(position.castling)
        val enPassant = position.enPassant
        if (enPassant != null && position.isEnPassantCapturable) key = key xor enPassantFile(enPassant.file)
        return key
    }

    private class SplitMix64(
        private var state: Long,
    ) {
        fun next(): Long {
            state += GOLDEN_GAMMA
            var z = state
            z = (z xor (z ushr SHIFT_1)) * MIX_1
            z = (z xor (z ushr SHIFT_2)) * MIX_2
            return z xor (z ushr SHIFT_3)
        }

        private companion object {
            const val GOLDEN_GAMMA = -0x61c8864680b583ebL
            const val MIX_1 = -0x40a7b892e31b1a47L
            const val MIX_2 = -0x6b2fb644ecceee15L
            const val SHIFT_1 = 30
            const val SHIFT_2 = 27
            const val SHIFT_3 = 31
        }
    }
}
