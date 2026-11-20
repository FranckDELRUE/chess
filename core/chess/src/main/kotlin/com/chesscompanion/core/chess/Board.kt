package com.chesscompanion.core.chess

/**
 * Piece placement on the 8x8 grid. Squares are indexed 0..63 with a1 = 0, b1 = 1, ... h8 = 63.
 * Immutable: the backing array is never exposed nor modified after construction.
 */
class Board internal constructor(
    private val squares: Array<Piece?>,
) {
    constructor(squares: List<Piece?>) : this(squares.toTypedArray())

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

    fun kingSquare(side: Side): Square? {
        val king = Piece(PieceType.KING, side)
        val index = squares.indexOf(king)
        return if (index >= 0) Square(index) else null
    }

    internal fun at(index: Int): Piece? = squares[index]

    internal fun copySquares(): Array<Piece?> = squares.copyOf()

    override fun equals(other: Any?): Boolean = other is Board && other.squares.contentEquals(squares)

    override fun hashCode(): Int = squares.contentHashCode()

    companion object {
        const val SIZE = 8
    }
}
