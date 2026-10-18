package com.chesscompanion.core.chess

/**
 * Piece placement on the 8x8 grid. Squares are indexed 0..63 with a1 = 0, b1 = 1, ... h8 = 63.
 */
class Board(
    private val squares: List<Piece?>,
) {
    init {
        require(squares.size == SQUARE_COUNT) { "A board has $SQUARE_COUNT squares, got ${squares.size}" }
    }

    fun pieceAt(
        file: Int,
        rank: Int,
    ): Piece? {
        require(file in 0 until SIZE && rank in 0 until SIZE) { "Square out of range: $file,$rank" }
        return squares[rank * SIZE + file]
    }

    override fun equals(other: Any?): Boolean = other is Board && other.squares == squares

    override fun hashCode(): Int = squares.hashCode()

    companion object {
        const val SIZE = 8
        private const val SQUARE_COUNT = SIZE * SIZE
    }
}
