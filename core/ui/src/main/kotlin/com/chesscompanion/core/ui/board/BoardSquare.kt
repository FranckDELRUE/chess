package com.chesscompanion.core.ui.board

import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.Square

/** File (0 = a) and rank (0 = rank 1) shown at a screen cell; row 0 / column 0 is the top-left. */
internal data class BoardSquare(
    val file: Int,
    val rank: Int,
) {
    fun toSquare(): Square = Square.of(file, rank)
}

internal fun squareAt(
    row: Int,
    column: Int,
    flipped: Boolean,
): BoardSquare =
    if (flipped) {
        BoardSquare(file = Board.SIZE - 1 - column, rank = row)
    } else {
        BoardSquare(file = column, rank = Board.SIZE - 1 - row)
    }

/** Square under a point of the board (pixels from its top-left corner), or null outside the board. */
internal fun squareAtOffset(
    x: Float,
    y: Float,
    cellPx: Float,
    flipped: Boolean,
): Square? {
    if (cellPx <= 0f) return null
    val column = (x / cellPx).toInt()
    val row = (y / cellPx).toInt()
    val inside = x >= 0f && y >= 0f && column < Board.SIZE && row < Board.SIZE
    return if (inside) squareAt(row, column, flipped).toSquare() else null
}

internal fun isLightSquare(square: BoardSquare): Boolean = (square.file + square.rank) % 2 == 1
