package com.chesscompanion.core.chess

/** Counts leaf nodes of the legal move tree: the reference test of a move generator. */
object Perft {
    fun count(
        position: Position,
        depth: Int,
    ): Long {
        require(depth >= 0) { "Depth must not be negative: $depth" }
        return when (depth) {
            0 -> 1L
            1 -> position.legalMoves.size.toLong()
            else -> position.legalMoves.sumOf { count(MoveGenerator.play(position, it), depth - 1) }
        }
    }

    /** Node count below each legal move, keyed by UCI: compare with Stockfish `go perft` to locate a bug. */
    fun divide(
        position: Position,
        depth: Int,
    ): Map<String, Long> {
        require(depth >= 1) { "Depth must be at least 1: $depth" }
        return position.legalMoves.associate { it.uci to count(MoveGenerator.play(position, it), depth - 1) }
    }
}
