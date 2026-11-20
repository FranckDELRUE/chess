package com.chesscompanion.core.engine.maia

import kotlin.math.abs

/**
 * lc0's 1858-move policy layout (`kMoveStrs` in lc0's `encoder.cc`), rebuilt by rule:
 * for each from-square a1..h8, every queen-line or knight destination in square order, then the
 * 66 promotions from rank 7 to Q, R or B. Moves are in the mover's perspective (Black's are
 * mirrored), a knight promotion uses the plain move's index and castling is "king takes rook".
 */
object Lc0PolicyMap {
    const val SIZE = 1858

    private const val BOARD = 8
    private const val MIRROR = 56
    private const val SEVENTH_RANK = 6
    private val promotions = listOf('q', 'r', 'b')

    /** Index → move string in lc0's notation (`a1b1`, …, `h7h8b`). */
    val moves: List<String> = buildMoves()

    private val indexByMove: Map<String, Int> = moves.withIndex().associate { (i, m) -> m to i }

    /**
     * Policy index of a legal UCI [move]. [kingMove] tells whether the moving piece is a king,
     * which is how castling (`e1g1`) is told apart from a rook or queen move.
     */
    fun indexOf(
        move: String,
        blackToMove: Boolean,
        kingMove: Boolean,
    ): Int {
        var from = squareOf(move, 0)
        var to = squareOf(move, 2)
        if (blackToMove) {
            from = from xor MIRROR
            to = to xor MIRROR
        }
        if (kingMove && abs(to % BOARD - from % BOARD) == 2) {
            to = if (to > from) to + 1 else to - 2 // castling: g1 → h1, c1 → a1
        }
        val promotion =
            move
                .getOrNull(4)
                ?.takeIf { it != 'n' }
                ?.toString()
                .orEmpty()
        return indexByMove[name(from) + name(to) + promotion] ?: error("Move $move has no lc0 policy index")
    }

    private fun buildMoves(): List<String> {
        val result = mutableListOf<String>()
        for (from in 0 until BOARD * BOARD) {
            for (to in 0 until BOARD * BOARD) {
                if (to != from && (isQueenLine(from, to) || isKnightJump(from, to))) result += name(from) + name(to)
            }
        }
        for (file in 0 until BOARD) {
            val from = SEVENTH_RANK * BOARD + file
            for (toFile in file - 1..file + 1) {
                if (toFile !in 0 until BOARD) continue
                promotions.forEach { p -> result += name(from) + name(from + BOARD + toFile - file) + p }
            }
        }
        check(result.size == SIZE) { "lc0 policy map has ${result.size} moves" }
        return result
    }

    private fun isQueenLine(
        from: Int,
        to: Int,
    ): Boolean {
        val df = abs(to % BOARD - from % BOARD)
        val dr = abs(to / BOARD - from / BOARD)
        return df == 0 || dr == 0 || df == dr
    }

    private fun isKnightJump(
        from: Int,
        to: Int,
    ): Boolean {
        val df = abs(to % BOARD - from % BOARD)
        val dr = abs(to / BOARD - from / BOARD)
        return (df == 1 && dr == 2) || (df == 2 && dr == 1)
    }

    private fun squareOf(
        move: String,
        at: Int,
    ): Int {
        require(move.length >= at + 2 && move[at] in 'a'..'h' && move[at + 1] in '1'..'8') { "Bad UCI move $move" }
        return (move[at + 1] - '1') * BOARD + (move[at] - 'a')
    }

    private fun name(square: Int): String = "${'a' + square % BOARD}${'1' + square / BOARD}"
}
