package com.chesscompanion.core.chess

/**
 * Piece placement on the 8x8 grid. Squares are indexed 0..63 with a1 = 0, b1 = 1, ... h8 = 63.
 */
class Board(
    private val squares: List<Piece?>,
) {
    init {
        require(squares.size == Square.COUNT) { "A board has ${Square.COUNT} squares, got ${squares.size}" }
    }

    operator fun get(square: Square): Piece? = squares[square.index]

    fun pieceAt(
        file: Int,
        rank: Int,
    ): Piece? = get(Square.of(file, rank))

    /** Occupied squares with their piece, from a1 to h8. */
    fun pieces(): List<Pair<Square, Piece>> = Square.all.mapNotNull { square -> get(square)?.let { square to it } }

    override fun equals(other: Any?): Boolean = other is Board && other.squares == squares

    override fun hashCode(): Int = squares.hashCode()

    companion object {
        const val SIZE = 8
    }
}
