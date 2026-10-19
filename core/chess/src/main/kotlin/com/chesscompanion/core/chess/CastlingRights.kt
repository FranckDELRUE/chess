package com.chesscompanion.core.chess

/** Castling rights still available, stored as four bits (White O-O, White O-O-O, Black O-O, Black O-O-O). */
@JvmInline
value class CastlingRights(
    val bits: Int,
) {
    init {
        require(bits in 0..ALL_BITS) { "Invalid castling bits: $bits" }
    }

    fun has(
        side: Side,
        kingside: Boolean,
    ): Boolean = bits and bit(side, kingside) != 0

    fun without(
        side: Side,
        kingside: Boolean,
    ): CastlingRights = CastlingRights(bits and bit(side, kingside).inv())

    fun without(side: Side): CastlingRights = without(side, kingside = true).without(side, kingside = false)

    /** FEN field, in the canonical order `KQkq`, or `-`. */
    fun toFen(): String =
        FEN_LETTERS
            .filter { (letter, _) -> bits and bitOf(letter) != 0 }
            .joinToString("") { it.first.toString() }
            .ifEmpty { "-" }

    override fun toString(): String = toFen()

    companion object {
        val NONE = CastlingRights(0)
        val ALL = CastlingRights(15)
        private const val ALL_BITS = 15

        private val FEN_LETTERS =
            listOf(
                'K' to (Side.WHITE to true),
                'Q' to (Side.WHITE to false),
                'k' to (Side.BLACK to true),
                'q' to (Side.BLACK to false),
            )

        private fun bit(
            side: Side,
            kingside: Boolean,
        ): Int = 1 shl (side.ordinal * 2 + if (kingside) 0 else 1)

        private fun bitOf(letter: Char): Int {
            val (side, kingside) = FEN_LETTERS.first { it.first == letter }.second
            return bit(side, kingside)
        }

        /** Parses the FEN castling field; returns null if it is malformed (unknown or repeated letter). */
        fun parseFen(field: String): CastlingRights? {
            if (field == "-") return NONE
            val bits = field.map { c -> if (FEN_LETTERS.any { it.first == c }) bitOf(c) else null }
            val valid = field.isNotEmpty() && field.length == field.toSet().size && null !in bits
            return if (valid) CastlingRights(bits.filterNotNull().sum()) else null
        }
    }
}
