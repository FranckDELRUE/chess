package com.chesscompanion.core.chess

/** A square of the board, indexed 0..63 with a1 = 0, b1 = 1, ... h8 = 63. */
@JvmInline
value class Square(
    val index: Int,
) {
    init {
        require(index in 0 until COUNT) { "Square index out of range: $index" }
    }

    /** 0 = file a, 7 = file h. */
    val file: Int get() = index and 7

    /** 0 = rank 1, 7 = rank 8. */
    val rank: Int get() = index shr 3

    val name: String get() = "${'a' + file}${'1' + rank}"

    override fun toString(): String = name

    companion object {
        const val COUNT = 64

        val all: List<Square> = List(COUNT) { Square(it) }

        val A1 = Square(0)
        val E1 = Square(4)
        val H1 = Square(7)
        val A8 = Square(56)
        val E8 = Square(60)
        val H8 = Square(63)

        fun of(
            file: Int,
            rank: Int,
        ): Square {
            require(file in 0 until Board.SIZE && rank in 0 until Board.SIZE) { "Square out of range: $file,$rank" }
            return Square(rank * Board.SIZE + file)
        }

        /** Parses a square name such as `e4`; returns null if the text is not a square. */
        fun parse(name: String): Square? {
            if (name.length != 2) return null
            val file = name[0] - 'a'
            val rank = name[1] - '1'
            return if (file in 0 until Board.SIZE && rank in 0 until Board.SIZE) of(file, rank) else null
        }
    }
}
