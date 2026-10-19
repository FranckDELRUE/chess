package com.chesscompanion.core.ui.board

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.Square

/** Callbacks of the interactive board and how it reads a drag. */
internal class BoardInput(
    val onTap: (Square) -> Unit,
    val onDragStart: (Square) -> Unit,
    val onMove: (Square, Square) -> Unit,
    val dragMode: DragMode,
    val arrowColor: Color,
)

/** A drag in progress: the square it started from and the finger position on the board. */
internal data class Drag(
    val from: Square,
    val position: Offset,
)

/**
 * Turns pointer events into board callbacks. The only state kept is the drag in progress, which
 * is UI state (where the finger is), not game state.
 */
internal class BoardGestures(
    private val flipped: Boolean,
    private val cellPx: Float,
) {
    /** Latest callbacks and board, refreshed at each composition. */
    var input: BoardInput? = null
    var board: Board? = null

    var drag: Drag? by mutableStateOf(null)
        private set

    private val arrowMode: Boolean get() = input?.dragMode == DragMode.ARROW

    fun tap(offset: Offset) {
        squareUnder(offset)?.let { input?.onTap(it) }
    }

    /** In piece mode a drag starts only on a piece; in arrow mode, on any square. */
    fun start(offset: Offset) {
        val from = squareUnder(offset) ?: return
        if (!arrowMode && board?.get(from) == null) return
        drag = Drag(from, offset)
        if (!arrowMode) input?.onDragStart(from)
    }

    fun move(amount: Offset) {
        drag = drag?.let { it.copy(position = it.position + amount) }
    }

    /** A piece dropped back on its square does nothing; an arrow to its own square marks the square. */
    fun end() {
        drag?.let { d ->
            val to = squareUnder(d.position)
            if (to != null && (arrowMode || to != d.from)) input?.onMove(d.from, to)
        }
        drag = null
    }

    fun cancel() {
        drag = null
    }

    private fun squareUnder(offset: Offset): Square? = squareAtOffset(offset.x, offset.y, cellPx, flipped)
}

internal fun Modifier.boardGestures(gestures: BoardGestures): Modifier =
    pointerInput(gestures) { detectTapGestures(onTap = gestures::tap) }
        .pointerInput(gestures) {
            detectDragGestures(
                onDragStart = gestures::start,
                onDrag = { change, amount ->
                    change.consume()
                    gestures.move(amount)
                },
                onDragEnd = gestures::end,
                onDragCancel = gestures::cancel,
            )
        }
