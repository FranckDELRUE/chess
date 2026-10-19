package com.chesscompanion.core.ui.board

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Square

/** What the board marks on top of the pieces; computed by the screen's ViewModel. */
data class BoardHighlights(
    val selected: Square? = null,
    /** Squares the selected piece can reach: a dot when empty, a ring on a capture. */
    val targets: Set<Square> = emptySet(),
    val lastMove: Move? = null,
    /** Square of a king in check. */
    val check: Square? = null,
)
