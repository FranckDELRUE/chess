package com.chesscompanion.core.ui.board

import androidx.annotation.StringRes
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.R

/** Colours of the marks drawn by the player (S1-8). */
enum class AnnotationColor { GREEN, RED, BLUE }

/** Spoken colour name, e.g. "vert". */
@StringRes
fun AnnotationColor.nameRes(): Int =
    when (this) {
        AnnotationColor.GREEN -> R.string.annotation_green
        AnnotationColor.RED -> R.string.annotation_red
        AnnotationColor.BLUE -> R.string.annotation_blue
    }

data class Arrow(
    val from: Square,
    val to: Square,
)

/**
 * Coloured squares and arrows drawn on the board by the player. Immutable. Marking again with the
 * same colour erases the mark, with another colour replaces it (as on Lichess).
 */
data class BoardAnnotations(
    val squares: Map<Square, AnnotationColor> = emptyMap(),
    val arrows: Map<Arrow, AnnotationColor> = emptyMap(),
) {
    val isEmpty: Boolean get() = squares.isEmpty() && arrows.isEmpty()

    fun toggleSquare(
        square: Square,
        color: AnnotationColor,
    ): BoardAnnotations = copy(squares = squares.toggled(square, color))

    /** An arrow from a square to itself marks the square. */
    fun toggleArrow(
        from: Square,
        to: Square,
        color: AnnotationColor,
    ): BoardAnnotations =
        if (from == to) toggleSquare(from, color) else copy(arrows = arrows.toggled(Arrow(from, to), color))

    fun clear(): BoardAnnotations = EMPTY

    private fun <K> Map<K, AnnotationColor>.toggled(
        key: K,
        color: AnnotationColor,
    ): Map<K, AnnotationColor> = if (this[key] == color) this - key else this + (key to color)

    companion object {
        val EMPTY = BoardAnnotations()
    }
}
