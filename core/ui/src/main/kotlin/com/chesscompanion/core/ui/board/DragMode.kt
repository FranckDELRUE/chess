package com.chesscompanion.core.ui.board

/** How a drag on the interactive board is read. */
enum class DragMode {
    /** The piece under the finger follows it and is dropped on a square (playing). */
    PIECE,

    /** A drag from any square draws an arrow to the square under the finger (annotating). */
    ARROW,
}
