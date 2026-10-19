package com.chesscompanion.feature.play

import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.BoardAnnotations

/**
 * The "Annoter" mode (S1-8), as an immutable state: while it is on, a tap colours a square and a
 * drag draws an arrow instead of playing. Marks belong to the position shown and are cleared by
 * the ViewModel when it changes.
 */
data class AnnotationInput(
    val enabled: Boolean = false,
    val color: AnnotationColor = AnnotationColor.GREEN,
    val annotations: BoardAnnotations = BoardAnnotations.EMPTY,
) {
    /** The new state when [action] is an annotation gesture; null when it must reach the game. */
    fun consume(action: PlayAction.Board): AnnotationInput? {
        if (!enabled) return null
        return when (action) {
            is PlayAction.SquareTap -> copy(annotations = annotations.toggleSquare(action.square, color))
            is PlayAction.Drop -> copy(annotations = annotations.toggleArrow(action.from, action.to, color))
            is PlayAction.DragStart -> this
            else -> null
        }
    }

    fun on(action: PlayAction.Annotation): AnnotationInput =
        when (action) {
            PlayAction.ToggleAnnotate -> copy(enabled = !enabled)
            is PlayAction.PickAnnotationColor -> copy(enabled = true, color = action.color)
            PlayAction.ClearAnnotations -> copy(annotations = BoardAnnotations.EMPTY)
        }

    fun cleared(): AnnotationInput = if (annotations.isEmpty) this else copy(annotations = BoardAnnotations.EMPTY)
}
