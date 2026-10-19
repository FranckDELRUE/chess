package com.chesscompanion.core.chess

enum class Side {
    WHITE,
    BLACK,
    ;

    val opposite: Side get() = if (this == WHITE) BLACK else WHITE
}
