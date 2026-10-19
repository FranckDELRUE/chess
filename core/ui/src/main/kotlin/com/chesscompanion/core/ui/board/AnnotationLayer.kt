package com.chesscompanion.core.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.Square
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val ARROW_WIDTH_RATIO = 0.16f
private const val HEAD_LENGTH_RATIO = 0.42f
private const val HEAD_ANGLE = 0.5
private const val HALF = 0.5f

/** Centre of [square] in pixels from the board's top-left corner. */
internal fun squareCenter(
    square: Square,
    cellPx: Float,
    flipped: Boolean,
): Offset {
    val column = if (flipped) Board.SIZE - 1 - square.file else square.file
    val row = if (flipped) square.rank else Board.SIZE - 1 - square.rank
    return Offset((column + HALF) * cellPx, (row + HALF) * cellPx)
}

/** Arrows over the pieces, plus the arrow being drawn ([preview], from a square centre to the finger). */
@Composable
internal fun AnnotationLayer(
    annotations: BoardAnnotations,
    preview: Pair<Square, Offset>?,
    previewColor: Color,
    cellPx: Float,
    flipped: Boolean,
    colors: BoardColors,
) {
    if (annotations.arrows.isEmpty() && preview == null) return
    Canvas(Modifier.fillMaxSize()) {
        annotations.arrows.forEach { (arrow, color) ->
            drawArrow(
                squareCenter(arrow.from, cellPx, flipped),
                squareCenter(arrow.to, cellPx, flipped),
                colors.of(color),
                cellPx,
            )
        }
        preview?.let { (from, finger) -> drawArrow(squareCenter(from, cellPx, flipped), finger, previewColor, cellPx) }
    }
}

/** A straight arrow (knight moves included) whose head ends at [end]. */
private fun DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    cellPx: Float,
) {
    val length = hypot(end.x - start.x, end.y - start.y)
    val head = cellPx * HEAD_LENGTH_RATIO
    if (length <= head) return
    val angle = atan2(end.y - start.y, end.x - start.x).toDouble()
    val shaftEnd = Offset(end.x - (head * cos(angle)).toFloat(), end.y - (head * sin(angle)).toFloat())
    drawLine(color, start, shaftEnd, strokeWidth = cellPx * ARROW_WIDTH_RATIO, cap = StrokeCap.Round)
    val left =
        Offset(
            end.x - (head * cos(angle - HEAD_ANGLE)).toFloat(),
            end.y - (head * sin(angle - HEAD_ANGLE)).toFloat(),
        )
    val right =
        Offset(
            end.x - (head * cos(angle + HEAD_ANGLE)).toFloat(),
            end.y - (head * sin(angle + HEAD_ANGLE)).toFloat(),
        )
    val path =
        Path().apply {
            moveTo(end.x, end.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
    drawPath(path, color)
}
