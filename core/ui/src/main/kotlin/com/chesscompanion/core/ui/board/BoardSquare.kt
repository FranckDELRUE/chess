package com.chesscompanion.core.ui.board

import com.chesscompanion.core.chess.Board

/** File (0 = a) and rank (0 = rank 1) shown at a screen cell; row 0 / column 0 is the top-left. */
internal data class BoardSquare(
    val file: Int,
    val rank: Int,
)

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

internal fun isLightSquare(square: BoardSquare): Boolean = (square.file + square.rank) % 2 == 1
